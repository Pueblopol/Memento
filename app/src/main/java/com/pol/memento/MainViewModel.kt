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

    fun createFolder(name: String, noteIds: Set<Int>) {
        viewModelScope.launch {
            val folderId = folderDao.insertFolder(com.pol.memento.data.Folder(name = name)).toInt()
            noteIds.forEach { noteId ->
                folderDao.insertFolderNoteCrossRef(com.pol.memento.data.FolderNoteCrossRef(folderId, noteId))
            }
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

    fun removeNoteFromFolder(folderId: Int, noteId: Int) {
        viewModelScope.launch {
            folderDao.removeNoteFromFolder(folderId, noteId)
        }
    }

    fun updateFolderNotes(folderId: Int, noteIds: Set<Int>) {
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
    fun addNote(title: String, description: String, priority: PriorityLevel, isPinned: Boolean) {
        viewModelScope.launch {
            val newNote = Note(
                title = title,
                description = description,
                priority = priority,
                isPinned = isPinned
            )
            // Salviamo nel database e otteniamo l'ID generato
            val generatedId = dao.insertNote(newNote).toInt()

            // Creiamo la notifica associata a quell'ID
            val noteWithId = newNote.copy(id = generatedId)
            notificationHelper.showNotification(noteWithId)
        }
    }

    // Funzione per aggiornare una nota esistente
    fun updateNote(note: Note, title: String, description: String, priority: PriorityLevel, isPinned: Boolean) {
        viewModelScope.launch {
            val updatedNote = note.copy(
                title = title,
                description = description,
                priority = priority,
                isPinned = isPinned
            )
            dao.updateNote(updatedNote)
            notificationHelper.showNotification(updatedNote)
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
        }
    }

    // Funzione per ripristinare una nota eliminata
    fun restoreNote(note: Note) {
        viewModelScope.launch {
            dao.insertNote(note)
            if (!note.isCompleted && (note.isPinned || note.priority == PriorityLevel.HIGH)) {
                notificationHelper.showNotification(note)
            }
        }
    }

    // Funzione per attivare/disattivare la puntina (Pinned)
    fun togglePin(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isPinned = !note.isPinned)
            dao.updateNote(updatedNote)
            if (updatedNote.isCompleted) {
                // Le note archiviate non mostrano notifiche anche se pinnate
                notificationHelper.cancelNotification(updatedNote.id)
            } else if (updatedNote.isPinned || updatedNote.priority == PriorityLevel.HIGH) {
                notificationHelper.showNotification(updatedNote)
            } else {
                notificationHelper.cancelNotification(updatedNote.id)
            }
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
        }
    }

    fun unarchiveNote(note: Note) {
        viewModelScope.launch {
            val updatedNote = note.copy(isCompleted = false)
            dao.updateNote(updatedNote)
            notificationHelper.showNotification(updatedNote)
        }
    }
}