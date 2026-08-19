package com.pol.memento.sync

import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

object MarkdownDeserializer {
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    data class ParsedNoteData(
        val note: Note,
        val folderNames: List<String>,
        val needsRewrite: Boolean = false
    )

    /**
     * Legge il contenuto di un file markdown e ricostruisce una Nota e le sue cartelle.
     * defaultTitle viene preso dal nome del file (senza .md).
     */
    fun deserialize(content: String, defaultTitle: String): ParsedNoteData? {
        try {
            val lines = content.lines()
            if (lines.isEmpty()) return null

            var id = UUID.randomUUID().toString()
            var title = defaultTitle
            var priority = PriorityLevel.LOW
            var isPinned = false
            var isPersistent = false
            var isCompleted = false
            var createdAt = System.currentTimeMillis()
            var position = 0.0
            val folderNames = mutableListOf<String>()

            var i = 0
            var hasYaml = false
            var needsRewrite = false

            if (lines[i].trim() == "---") {
                // Look ahead to see if there is a closing ---
                var hasClosingYaml = false
                for (j in (i + 1) until lines.size) {
                    if (lines[j].trim() == "---") {
                        hasClosingYaml = true
                        break
                    }
                }

                if (hasClosingYaml) {
                    hasYaml = true
                    i++
                    var inFoldersArray = false
                    var foundId = false
                    var foundTitle = false
                    
                    // Parsa YAML Frontmatter
                    while (i < lines.size) {
                        val line = lines[i]
                        if (line.trim() == "---") {
                            i++
                            break
                        }
                        
                        if (inFoldersArray) {
                            if (line.startsWith("  - ")) {
                                val folderName = line.substringAfter("-").trim().removeSurrounding("\"")
                                folderNames.add(folderName)
                                i++
                                continue
                            } else {
                                inFoldersArray = false
                            }
                        }

                        if (line.startsWith("id:")) {
                            val parsedId = line.substringAfter(":").trim().removeSurrounding("\"")
                            if (parsedId.isNotEmpty()) {
                                id = parsedId
                                foundId = true
                            }
                        } else if (line.startsWith("title:")) {
                            title = line.substringAfter(":").trim().removeSurrounding("\"")
                            foundTitle = true
                        } else if (line.startsWith("priority:")) {
                            val p = line.substringAfter(":").trim()
                            priority = try { PriorityLevel.valueOf(p) } catch (e: Exception) { PriorityLevel.LOW }
                        } else if (line.startsWith("pinned:")) {
                            isPinned = line.substringAfter(":").trim().toBoolean()
                        } else if (line.startsWith("persistent_notification:")) {
                            isPersistent = line.substringAfter(":").trim().toBoolean()
                        } else if (line.startsWith("is_completed:")) {
                            isCompleted = line.substringAfter(":").trim().toBoolean()
                        } else if (line.startsWith("created_at:")) {
                            val dateStr = line.substringAfter(":").trim()
                            try {
                                createdAt = dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                            } catch (e: Exception) { }
                        } else if (line.startsWith("position:")) {
                            position = line.substringAfter(":").trim().toDoubleOrNull() ?: 0.0
                        } else if (line.startsWith("folders:")) {
                            val arrStr = line.substringAfter(":").trim()
                            if (arrStr == "[]") {
                                // Empty folders
                            } else if (arrStr.isEmpty()) {
                                inFoldersArray = true
                            }
                        }
                        i++
                    }
                    if (!foundId || !foundTitle) {
                        needsRewrite = true
                    }
                } else {
                    needsRewrite = true
                }
            } else {
                needsRewrite = true
            }
            
            // Parsa il corpo Markdown (saltando eventuali # Titolo iniziali per evitare doppioni se aveva lo yaml)
            val descriptionBuilder = StringBuilder()
            var firstHeadingSkipped = false
            
            while (i < lines.size) {
                val line = lines[i]
                if (hasYaml && !firstHeadingSkipped && line.startsWith("# ") && line.substringAfter("# ").trim() == title) {
                    firstHeadingSkipped = true
                    i++
                    // salta linea vuota dopo titolo
                    if (i < lines.size && lines[i].isBlank()) i++
                    continue
                }
                descriptionBuilder.appendLine(line)
                i++
            }
            
            val note = Note(
                id = id,
                title = title,
                description = descriptionBuilder.toString().trimEnd(),
                priority = priority,
                isPinned = isPinned,
                isPersistent = isPersistent,
                isCompleted = isCompleted,
                createdAt = createdAt,
                position = position
            )
            
            return ParsedNoteData(note, folderNames, needsRewrite)
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
