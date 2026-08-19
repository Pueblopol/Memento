package com.pol.memento.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel

class NotificationRestorerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra("note_id")
        if (noteId != null) {
            val title = intent.getStringExtra("note_title") ?: ""
            val desc = intent.getStringExtra("note_desc") ?: ""
            val priorityName = intent.getStringExtra("note_priority") ?: PriorityLevel.MEDIUM.name
            val isPinned = intent.getBooleanExtra("note_is_pinned", false)
            val isPersistent = intent.getBooleanExtra("note_is_persistent", false)
            
            val note = Note(
                id = noteId,
                title = title,
                description = desc,
                priority = PriorityLevel.valueOf(priorityName),
                isPinned = isPinned,
                isPersistent = isPersistent
            )
            
            if (note.isPersistent) {
                val notificationHelper = NotificationHelper(context)
                notificationHelper.showNotification(note)
            }
        }
    }
}
