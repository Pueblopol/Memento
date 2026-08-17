package com.pol.memento.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel

class NotificationRestorerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("note_id", -1)
        if (noteId != -1) {
            val title = intent.getStringExtra("note_title") ?: ""
            val desc = intent.getStringExtra("note_desc") ?: ""
            val priorityName = intent.getStringExtra("note_priority") ?: PriorityLevel.MEDIUM.name
            val isPinned = intent.getBooleanExtra("note_is_pinned", false)
            
            val note = Note(
                id = noteId,
                title = title,
                description = desc,
                priority = PriorityLevel.valueOf(priorityName),
                isPinned = isPinned
            )
            
            // Se la nota è ancora configurata per essere fissa, la ricreiamo!
            if (note.isPinned || note.priority == PriorityLevel.HIGH) {
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification(note)
            }
        }
    }
}
