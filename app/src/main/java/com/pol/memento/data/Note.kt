package com.pol.memento.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val priority: PriorityLevel, // HIGH, MEDIUM, LOW
    val isPinned: Boolean,       // true = notifica fissa
    val isNotificationActive: Boolean = true,
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val position: Double = 0.0
)

enum class PriorityLevel {
    HIGH,   // Rosso
    MEDIUM, // Verde acqua
    LOW     // Blu
}