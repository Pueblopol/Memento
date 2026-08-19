package com.pol.memento.sync

import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object MarkdownSerializer {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Converte una nota e i nomi delle sue cartelle in un file Markdown con YAML frontmatter.
     */
    fun serializeNote(note: Note, folderNames: List<String>): String {
        val sb = java.lang.StringBuilder()
        sb.appendLine("---")
        sb.appendLine("id: \"${note.id}\"")
        
        // Escape quotes in title
        val safeTitle = note.title.replace("\"", "\\\"")
        sb.appendLine("title: \"$safeTitle\"")
        
        // Folders list
        if (folderNames.isNotEmpty()) {
            sb.appendLine("folders:")
            folderNames.forEach { folder ->
                val safeFolder = folder.replace("\"", "\\\"")
                sb.appendLine("  - \"$safeFolder\"")
            }
        } else {
            sb.appendLine("folders: []")
        }
        
        sb.appendLine("priority: ${note.priority.name}")
        sb.appendLine("pinned: ${note.isPinned}")
        sb.appendLine("persistent_notification: ${note.isPersistent}")
        sb.appendLine("is_completed: ${note.isCompleted}")
        sb.appendLine("created_at: ${dateFormat.format(Date(note.createdAt))}")
        sb.appendLine("position: ${note.position}")
        sb.appendLine("---")
        sb.appendLine()
        
        if (note.title.isNotBlank()) {
            sb.appendLine("# ${note.title}")
            sb.appendLine()
        }
        
        sb.append(note.description)
        
        return sb.toString()
    }
}
