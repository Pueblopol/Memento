package com.pol.memento.sync

import android.content.Context
import com.pol.memento.data.GitSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("Conflitti o errore nel pull rebase."))
                }
            }
        } catch (e: Exception) {
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
}
