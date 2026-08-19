package com.pol.memento.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

import androidx.room.Update

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY position ASC, createdAt DESC")
    fun getAllFolders(): Flow<List<Folder>>

    @Transaction
    @Query("SELECT * FROM folders ORDER BY position ASC, createdAt DESC")
    fun getFoldersWithNotes(): Flow<List<FolderWithNotes>>

    @Update
    suspend fun updateFolders(folders: List<Folder>)

    @Update
    suspend fun updateFolder(folder: Folder)

    @Insert
    suspend fun insertFolder(folder: Folder)

    @Insert
    suspend fun insertFolderNoteCrossRef(crossRef: FolderNoteCrossRef)

    @Query("DELETE FROM folder_note_cross_ref WHERE folderId = :folderId")
    suspend fun clearNotesForFolder(folderId: String)

    @Query("DELETE FROM folder_note_cross_ref WHERE folderId = :folderId AND noteId = :noteId")
    suspend fun removeNoteFromFolder(folderId: String, noteId: String)

    @Query("DELETE FROM folder_note_cross_ref WHERE noteId = :noteId")
    suspend fun removeNoteFromAllFolders(noteId: String)

    @Delete
    suspend fun deleteFolder(folder: Folder)
}
