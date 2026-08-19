package com.pol.memento.data

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "folder_note_cross_ref",
    primaryKeys = ["folderId", "noteId"],
    indices = [Index(value = ["noteId"])]
)
data class FolderNoteCrossRef(
    val folderId: String,
    val noteId: String
)
