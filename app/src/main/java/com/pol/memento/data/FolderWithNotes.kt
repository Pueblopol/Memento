package com.pol.memento.data

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class FolderWithNotes(
    @Embedded val folder: Folder,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(FolderNoteCrossRef::class, parentColumn = "folderId", entityColumn = "noteId")
    )
    val notes: List<Note>
)
