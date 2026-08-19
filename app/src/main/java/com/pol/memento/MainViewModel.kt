package com.pol.memento

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pol.memento.data.AppDatabase
import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel
import com.pol.memento.notifications.NotificationHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Colleghiamo il database, notifiche e impostazioni
    private val dao = AppDatabase.getDatabase(application).noteDao()
    private val folderDao = AppDatabase.getDatabase(application).folderDao()
    private val notificationHelper = NotificationHelper(application)
    private val settingsRepo = com.pol.memento.data.SettingsRepository(application)
    val gitSettingsRepo = com.pol.memento.data.GitSettingsRepository(application)
    val syncEngine = com.pol.memento.sync.MarkdownSyncEngine(application, gitSettingsRepo)

    // Flussi di stato per le impostazioni
    val isDarkMode: StateFlow<Boolean?> = settingsRepo.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
    val isGridView: StateFlow<Boolean> = settingsRepo.isGridView
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
        
    val defaultPriority: StateFlow<PriorityLevel> = settingsRepo.defaultPriority
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PriorityLevel.MEDIUM)

    val notesList: StateFlow<List<Note>> = dao.getActiveNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotesList: StateFlow<List<Note>> = dao.getArchivedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotesList: StateFlow<List<Note>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foldersWithNotesList: StateFlow<List<com.pol.memento.data.FolderWithNotes>> = folderDao.getFoldersWithNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createFolder(name: String, noteIds: Set<String>) {
        viewModelScope.launch {
            val folder = com.pol.memento.data.Folder(name = name)
            folderDao.insertFolder(folder)
            noteIds.forEach { noteId ->
                folderDao.insertFolderNoteCrossRef(com.pol.memento.data.FolderNoteCrossRef(folder.id, noteId))
            }
            syncEngine.createPhysicalFolder(name)
        }
    }

    fun deleteFolder(folder: com.pol.memento.data.Folder) {
        viewModelScope.launch {
            folderDao.clearNotesForFolder(folder.id)
            folderDao.deleteFolder(folder)
        }
    }

    fun restoreFolder(folderWithNotes: com.pol.memento.data.FolderWithNotes) {
        viewModelScope.launch {
            folderDao.insertFolder(folderWithNotes.folder)
            folderWithNotes.notes.forEach { note ->
                folderDao.insertFolderNoteCrossRef(
                    com.pol.memento.data.FolderNoteCrossRef(folderWithNotes.folder.id, note.id)
                )
            }
        }
    }

    fun removeNoteFromFolder(folderId: String, noteId: String) {
        viewModelScope.launch {
            folderDao.removeNoteFromFolder(folderId, noteId)
        }
    }

    fun updateFolderNotes(folderId: String, noteIds: Set<String>) {
        viewModelScope.launch {
            folderDao.clearNotesForFolder(folderId)
            noteIds.forEach { noteId ->
                folderDao.insertFolderNoteCrossRef(com.pol.memento.data.FolderNoteCrossRef(folderId, noteId))
            }
        }
    }
    fun updateFoldersOrder(reorderedFolders: List<com.pol.memento.data.Folder>) {
        viewModelScope.launch {
            val updated = reorderedFolders.mapIndexed { index, folder ->
                folder.copy(position = index.toDouble())
            }
            folderDao.updateFolders(updated)
        }
    }

    // Funzione per salvare una nuova nota
    fun addNote(title: String, description: String, priority: PriorityLevel, isPinned: Boolean, isPersistent: Boolean = false) {
        viewModelScope.launch {
            val newNote = Note(
                title = title,
                description = description,
                priority = priority,
                isPinned = isPinned,
                isPersistent = isPersistent
            )
            dao.insertNote(newNote)
            notificationHelper.showNotification(newNote)
            syncNoteSave(newNote, false)
        }
    }

    // Funzione per salvare una nuova nota dal foglio (con cartella)
    fun addNoteFromSheet(title: String, description: String, priority: PriorityLevel, isPinned: Boolean, isPersistent: Boolean, folderId: String?) {
        viewModelScope.launch {
            val newNote = Note(
                title = title,
                description = description,
                priority = priority,
                isPinned = isPinned,
                isPersistent = isPersistent
            )
            dao.insertNote(newNote)

            if (folderId != null) {
                folderDao.insertFolderNoteCrossRef(com.pol.memento.data.FolderNoteCrossRef(folderId, newNote.id))
            }

            notificationHelper.showNotification(newNote)
            syncNoteSave(newNote, false, folderId)
        }
    }

    // Folder management
    fun renameFolder(folder: com.pol.memento.data.Folder, newName: String) {
        viewModelScope.launch {
            folderDao.updateFolder(folder.copy(name = newName))
        }
    }

    // Funzione per aggiornare una nota esistente
    fun updateNote(note: Note, title: String, description: String, priority: PriorityLevel, isPinned: Boolean, isPersistent: Boolean = note.isPersistent) {
        viewModelScope.launch {
            val updatedNote = note.copy(
                title = title,
                description = description,
                priority = priority,
                isPinned = isPinned,
                isPersistent = isPersistent
            )
            dao.updateNote(updatedNote)
            notificationHelper.showNotification(updatedNote)
            syncNoteSave(updatedNote, true)
        }
    }

    // Funzione per aggiornare una nota esistente dal foglio (con cartella)
    fun updateNoteFromSheet(note: Note, title: String, description: String, priority: PriorityLevel, isPinned: Boolean, isPersistent: Boolean, folderId: String?) {
        viewModelScope.launch {
            val updatedNote = note.copy(
                title = title,
                description = description,
                priority = priority,
                isPinned = isPinned,
                isPersistent = isPersistent
            )
            dao.updateNote(updatedNote)
            
            folderDao.removeNoteFromAllFolders(note.id)
            if (folderId != null) {
                folderDao.insertFolderNoteCrossRef(com.pol.memento.data.FolderNoteCrossRef(folderId, note.id))
            }
            
            notificationHelper.showNotification(updatedNote)
            syncNoteSave(updatedNote, true, folderId)
        }
    }

    // Salva il nuovo ordine dopo un trascinamento
    fun updateNotesOrder(reorderedNotes: List<Note>) {
        viewModelScope.launch {
            val updated = reorderedNotes.mapIndexed { index, note ->
                note.copy(position = index.toDouble())
            }
            dao.updateNotes(updated)
        }
    }

    // Funzione per eliminare una nota
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            dao.deleteNote(note)
            notificationHelper.cancelNotification(note.id)
            syncEngine.deleteNote(note)
        }
    }

    // Funzione per ripristinare una nota eliminata
    fun restoreNote(note: Note) {
        viewModelScope.launch {
            dao.insertNote(note)
            if (!note.isCompleted && note.isPersistent) {
                notificationHelper.showNotification(note)
            }
            syncNoteSave(note, false)
        }
    }

    // Funzione per attivare/disattivare la puntina (Pinned)
    fun togglePin(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isPinned = !note.isPinned)
            dao.updateNote(updatedNote)
            syncNoteSave(updatedNote, true)
        }
    }
    
    // Funzioni per aggiornare le impostazioni
    fun setDarkMode(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setDarkMode(isEnabled)
        }
    }
    fun setGridView(isEnabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setGridView(isEnabled)
        }
    }
    fun setDefaultPriority(priority: PriorityLevel) {
        viewModelScope.launch {
            settingsRepo.setDefaultPriority(priority)
        }
    }
    
    // Funzioni per l'archiviazione
    fun archiveNote(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isCompleted = true)
            dao.updateNote(updatedNote)
            notificationHelper.cancelNotification(updatedNote.id)
            syncNoteSave(updatedNote, true)
        }
    }

    fun unarchiveNote(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isCompleted = false)
            dao.updateNote(updatedNote)
            notificationHelper.showNotification(updatedNote)
            syncNoteSave(updatedNote, true)
        }
    }

    private suspend fun syncNoteSave(note: Note, isUpdate: Boolean, folderIdOverride: String? = null) {
        val folderNames = mutableListOf<String>()
        if (folderIdOverride != null) {
            val folder = folderDao.getFolderById(folderIdOverride)
            folder?.let { folderNames.add(it.name) }
        } else {
            val currentFolders = foldersWithNotesList.value
                .filter { it.notes.any { n -> n.id == note.id } }
                .map { it.folder.name }
            folderNames.addAll(currentFolders)
        }
        syncEngine.saveNote(note, folderNames, isUpdate)
    }

    suspend fun initialSyncAfterClone() {
        val mdFiles = syncEngine.getMarkdownFilesCount()
        if (mdFiles == 0) {
            // Repo vuoto: esporta tutte le note locali
            syncEngine.exportAllToGit(dao, folderDao)
        } else {
            // Repo con dati: importa le note in locale
            syncEngine.syncDatabaseWithFiles(dao, folderDao)
        }
    }
}