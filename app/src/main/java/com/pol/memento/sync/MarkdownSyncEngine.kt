package com.pol.memento.sync

import android.content.Context
import com.pol.memento.data.GitSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

class MarkdownSyncEngine(
    private val context: Context,
    private val gitSettings: GitSettingsRepository
) {
    // La cartella locale dove risiederà il repository clonato
    private val repoDir = File(context.filesDir, "memento_sync")

    private fun getSanitizedFilename(title: String): String {
        val safeTitle = title.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
        return if (safeTitle.isEmpty()) "Senza Titolo" else safeTitle
    }

    private fun findFileByNoteId(noteId: String): File? {
        if (!repoDir.exists()) return null
        val files = repoDir.walkTopDown().filter { it.isFile && it.name.endsWith(".md") }.toList()
        for (file in files) {
            try {
                // Leggiamo solo l'intestazione YAML per essere super veloci
                val reader = file.bufferedReader()
                var line = reader.readLine()
                if (line?.trim() == "---") {
                    var foundId = false
                    for (i in 0..10) {
                        line = reader.readLine() ?: break
                        if (line.trim() == "---") break
                        if (line.startsWith("id:") && line.substringAfter(":").trim().removeSurrounding("\"") == noteId) {
                            foundId = true
                            break
                        }
                    }
                    if (foundId) {
                        reader.close()
                        return file
                    }
                }
                reader.close()
            } catch (e: Exception) { /* ignore */ }
        }
        return null
    }

    /**
     * Salva una singola nota nel file system clonato, nella cartella specificata dal suo primo tag (o root).
     */
    suspend fun saveNote(note: com.pol.memento.data.Note, folderNames: List<String>, isUpdate: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!repoDir.exists()) return@withContext Result.failure(Exception("Repo non clonato"))

            val markdownContent = MarkdownSerializer.serializeNote(note, folderNames)
            
            // Trova l'eventuale file vecchio (se il titolo è stato cambiato)
            val oldFile = findFileByNoteId(note.id)
            oldFile?.delete()
            
            val subDir = if (folderNames.isNotEmpty()) {
                val dir = File(repoDir, getSanitizedFilename(folderNames.first()))
                if (!dir.exists()) dir.mkdirs()
                dir
            } else {
                repoDir
            }
            
            val filename = "${getSanitizedFilename(note.title)}.md"
            val file = File(subDir, filename)
            
            file.writeText(markdownContent)
            
            val actionName = if (isUpdate) "Update note" else "Create note"
            commitAndPush("$actionName: ${note.title}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Errore salvataggio nota: ${e.message}"))
        }
    }

    /**
     * Elimina una nota dal file system e fa commit.
     */
    suspend fun deleteNote(note: com.pol.memento.data.Note): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!repoDir.exists()) return@withContext Result.failure(Exception("Repo non clonato"))

            val oldFile = findFileByNoteId(note.id)
            oldFile?.delete()
            
            commitAndPush("Delete note: ${note.title}")
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Errore eliminazione nota: ${e.message}"))
        }
    }

    /**
     * Clona il repository specificato. Se la cartella esiste già, la svuota prima di clonare.
     */
    suspend fun cloneRepo(url: String, username: String, pat: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (repoDir.exists()) {
                repoDir.deleteRecursively()
            }
            repoDir.mkdirs()

            val credentials = UsernamePasswordCredentialsProvider(username, pat)

            Git.cloneRepository()
                .setURI(url)
                .setDirectory(repoDir)
                .setCredentialsProvider(credentials)
                .call()
                .use { git ->
                    // La clonazione è completata e il puntatore Git è stato chiuso (grazie a .use)
                }
            
            Result.success(Unit)
        } catch (e: GitAPIException) {
            e.printStackTrace()
            Result.failure(Exception("Errore Git durante la clonazione: ${e.message}"))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Errore di sistema durante la clonazione: ${e.message}"))
        }
    }

    /**
     * Esegue `git pull --rebase` per scaricare le ultime modifiche dal cloud.
     */
    suspend fun pullRebase(): Result<Unit> = withContext(Dispatchers.IO) {
        val url = gitSettings.getRepoUrl()
        val username = gitSettings.getUsername()
        val pat = gitSettings.getPat()

        if (url.isNullOrBlank() || username.isNullOrBlank() || pat.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Credenziali Git non configurate."))
        }

        try {
            val credentials = UsernamePasswordCredentialsProvider(username, pat)
            Git.open(repoDir).use { git ->
                val pullResult = git.pull()
                    .setCredentialsProvider(credentials)
                    .setRebase(true) // Git pull --rebase
                    .call()
                
                if (pullResult.isSuccessful) {
                    return@withContext Result.success(Unit)
                }
                
                // Gestione conflitti: se c'è un conflitto, abortiamo il rebase e favoriamo la versione remota (più recente sul cloud)
                val status = pullResult.rebaseResult?.status
                if (status != null && status != org.eclipse.jgit.api.RebaseResult.Status.OK) {
                    try {
                        git.rebase().setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT).call()
                        // Resetta hard alla versione remota scaricata
                        git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("FETCH_HEAD").call()
                        return@withContext Result.success(Unit)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        return@withContext Result.failure(Exception("Impossibile risolvere il conflitto: ${e.message}"))
                    }
                }

                return@withContext Result.failure(Exception("Errore nel pull rebase: ${status?.name}"))
            }
        } catch (e: Exception) {
            // Se eravamo in rebasing e c'è stata un'eccezione, proviamo ad abortire
            try {
                Git.open(repoDir).use { git ->
                    if (git.repository.repositoryState != org.eclipse.jgit.lib.RepositoryState.SAFE) {
                        git.rebase().setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT).call()
                        git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("FETCH_HEAD").call()
                    }
                }
            } catch (ignore: Exception) {}
            
            e.printStackTrace()
            Result.failure(Exception("Errore durante il pull: ${e.message}"))
        }
    }

    /**
     * Crea un commit locale e poi effettua un push.
     */
    suspend fun commitAndPush(commitMessage: String): Result<Unit> = withContext(Dispatchers.IO) {
        val username = gitSettings.getUsername()
        val pat = gitSettings.getPat()

        if (username.isNullOrBlank() || pat.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Credenziali Git non configurate."))
        }

        try {
            val credentials = UsernamePasswordCredentialsProvider(username, pat)
            Git.open(repoDir).use { git ->
                // git add .
                git.add().addFilepattern(".").call()
                // stage deletions
                git.add().setUpdate(true).addFilepattern(".").call()
                
                // git commit -m
                git.commit().setMessage(commitMessage).call()
                
                // git push
                git.push()
                    .setCredentialsProvider(credentials)
                    .call()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Errore durante commit e push: ${e.message}"))
        }
    }

    /**
     * Esegue solo un `git push` per caricare le modifiche locali sul remoto.
     */
    suspend fun pushOnly(): Result<Unit> = withContext(Dispatchers.IO) {
        val username = gitSettings.getUsername()
        val pat = gitSettings.getPat()

        if (username.isNullOrBlank() || pat.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Credenziali Git non configurate."))
        }

        try {
            val credentials = UsernamePasswordCredentialsProvider(username, pat)
            Git.open(repoDir).use { git ->
                git.push()
                    .setCredentialsProvider(credentials)
                    .call()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Errore durante push: ${e.message}"))
        }
    }

    /**
     * Esegue l'intero ciclo di sincronizzazione: Pull Rebase -> Push.
     */
    suspend fun syncAll(): Result<Unit> = withContext(Dispatchers.IO) {
        val pullRes = pullRebase()
        if (pullRes.isFailure) return@withContext pullRes
        
        return@withContext pushOnly()
    }
    
    /**
     * Legge tutti i file markdown nella cartella clonato, deserializza e aggiorna il DB locale.
     * NB: Chiamalo DOPO un pull, per aggiornare l'interfaccia.
     */
    suspend fun syncDatabaseWithFiles(
        noteDao: com.pol.memento.data.NoteDao, 
        folderDao: com.pol.memento.data.FolderDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!repoDir.exists()) return@withContext Result.failure(Exception("Repo non clonato"))

            val mdFiles = repoDir.walkTopDown().filter { it.isFile && it.name.endsWith(".md") }.toList()
            
            // Per gestire cancellazioni (file rimossi dal PC), leggiamo tutte le note attuali
            val dbNotes = noteDao.getAllNotes().first()
            val existingIds = mdFiles.mapNotNull { file ->
                val parsed = MarkdownDeserializer.deserialize(file.readText())
                if (parsed != null) {
                    // Update/Insert note
                    noteDao.insertNote(parsed.note) // insertNote has OnConflictStrategy.REPLACE
                    
                    // Manage folders (create them if they don't exist)
                    folderDao.removeNoteFromAllFolders(parsed.note.id)
                    
                    // Read current folders to find matching names
                    val allFolders = folderDao.getAllFolders().first()
                    for (fName in parsed.folderNames) {
                        var folder = allFolders.find { it.name == fName }
                        if (folder == null) {
                            folder = com.pol.memento.data.Folder(name = fName)
                            folderDao.insertFolder(folder)
                        }
                        folderDao.insertFolderNoteCrossRef(
                            com.pol.memento.data.FolderNoteCrossRef(folder.id, parsed.note.id)
                        )
                    }
                    parsed.note.id
                } else null
            }.toSet()
            
            // Delete notes from DB that no longer exist in Git
            // Safeguard: non cancelliamo nulla se la cartella Git non contiene alcun markdown
            // Questo previene di cancellare tutto il DB locale se cloniamo un repo vuoto appena creato su Github.
            if (mdFiles.isNotEmpty()) {
                for (dbNote in dbNotes) {
                    if (!existingIds.contains(dbNote.id)) {
                        noteDao.deleteNote(dbNote)
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Errore durante la deserializzazione: ${e.message}"))
        }
    }

    fun getMarkdownFilesCount(): Int {
        if (!repoDir.exists()) return 0
        return repoDir.walkTopDown().count { it.isFile && it.name.endsWith(".md") }
    }

    /**
     * Esporta tutte le note del database nel repository Git e fa un singolo commit/push.
     * Utile subito dopo aver clonato un repository vuoto.
     */
    suspend fun exportAllToGit(
        noteDao: com.pol.memento.data.NoteDao, 
        folderDao: com.pol.memento.data.FolderDao
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (!repoDir.exists()) return@withContext Result.failure(Exception("Repo non clonato"))

            val dbNotes = noteDao.getAllNotes().first()
            val allFolders = folderDao.getFoldersWithNotes().first()

            for (note in dbNotes) {
                val folderNames = allFolders.filter { it.notes.any { n -> n.id == note.id } }.map { it.folder.name }
                val markdownContent = MarkdownSerializer.serializeNote(note, folderNames)
                
                val oldFile = findFileByNoteId(note.id)
                oldFile?.delete()
                
                val subDir = if (folderNames.isNotEmpty()) {
                    val dir = File(repoDir, getSanitizedFilename(folderNames.first()))
                    if (!dir.exists()) dir.mkdirs()
                    dir
                } else {
                    repoDir
                }
                
                val filename = "${getSanitizedFilename(note.title)}.md"
                val file = File(subDir, filename)
                file.writeText(markdownContent)
            }

            if (dbNotes.isNotEmpty()) {
                commitAndPush("Initial export: ${dbNotes.size} notes")
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(Exception("Errore durante l'esportazione di massa: ${e.message}"))
        }
    }
}
