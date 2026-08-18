package com.pol.memento.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel

class NotificationHelper(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        // I canali di notifica sono obbligatori da Android 8.0 (API 26) in poi
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Canale Fissato
            // Torniamo a IMPORTANCE_DEFAULT. Purtroppo Android blocca l'inamovibilità
            // se la priorità è minore di DEFAULT.
            val stickyChannel = NotificationChannel(
                "channel_sticky_silent_v2",
                "Note Fisse",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Mostra le note in modo permanente"
                setSound(null, null)
                enableVibration(false)
            }

            notificationManager.createNotificationChannel(stickyChannel)
        }
    }

    fun showNotification(note: Note) {
        val channelId = "channel_sticky_silent_v2"

        // Usiamo un'icona di sistema di default per ora (una piccola matita)
        val icon = android.R.drawable.ic_menu_edit

        val restoreIntent = Intent(context, NotificationRestorerReceiver::class.java).apply {
            putExtra("note_id", note.id)
            putExtra("note_title", note.title)
            putExtra("note_description", note.description)
            putExtra("note_priority", note.priority.name)
            putExtra("note_is_pinned", note.isPinned)
            putExtra("note_is_persistent", note.isPersistent)
        }
        
        val pendingRestoreIntent = PendingIntent.getBroadcast(
            context, 
            note.id, 
            restoreIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(icon)
            .setContentTitle(note.title)
            .setDeleteIntent(pendingRestoreIntent) // L'arma segreta contro Android 14
            // Dobbiamo usare PRIORITÀ DEFAULT altrimenti Android sblocca lo swipe
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setSilent(true)
            // IL CUORE DELL'APP: Solo se la nota è "Persistent" non può essere rimossa con lo swipe
            .setOngoing(note.isPersistent)

        // Mostra la notifica usando l'ID univoco della nota
        notificationManager.notify(note.id, builder.build())
    }

    fun cancelNotification(noteId: Int) {
        notificationManager.cancel(noteId)
    }
}