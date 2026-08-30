package com.pol.memento.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel
import com.pol.memento.ui.util.parseMarkdown

@Composable
fun NoteCard(
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
    showDragHandle: Boolean = true,
    isGridView: Boolean = false,
    isCompactMode: Boolean = false,
    isSelectedForShare: Boolean = false,
    note: Note,
    folderName: String? = null,
    onClick: () -> Unit,
    onTogglePin: (() -> Unit)? = null,
    onRemoveFromFolder: (() -> Unit)? = null,
    onToggleCheckbox: ((Note) -> Unit)? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val priorityColor = when (note.priority) {
        PriorityLevel.HIGH -> Color(0xFFE53935)   // Rosso
        PriorityLevel.MEDIUM -> Color(0xFF4CAF50) // Verde
        PriorityLevel.LOW -> Color(0xFF2979FF)    // Blu
    }

    val cardBorder = if (isSelectedForShare) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else null

    Card(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        border = cardBorder
    ) {
        if (isGridView) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(priorityColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title, 
                        fontWeight = FontWeight.Bold, 
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (onTogglePin != null) {
                        IconButton(
                            onClick = onTogglePin,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Fissa nota",
                                modifier = Modifier.size(16.dp),
                                tint = if (note.isPinned) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }
                    } else if (note.isPinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Fissa nota",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (folderName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = folderName, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Maniglia di trascinamento (3 barrette)
                if (showDragHandle) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Trascina",
                        modifier = dragHandleModifier.padding(end = 8.dp),
                        tint = Color.Gray
                    )
                }

                // Pallino colorato per indicare la priorità
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(priorityColor)
                )
                Spacer(modifier = Modifier.width(16.dp))

                // Testi della nota
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = note.title, fontWeight = FontWeight.Bold)
                    if (!isCompactMode && note.description.isNotBlank()) {
                        val annotatedText = parseMarkdown(note.description)
                        var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
                        Text(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current),
                            onTextLayout = { textLayoutResult = it },
                            modifier = Modifier.pointerInput(Unit) {
                                detectTapGestures { pos ->
                                    textLayoutResult?.let { layoutResult ->
                                        val offset = layoutResult.getOffsetForPosition(pos)
                                        val textStr = note.description
                                        
                                        // Handle URL clicks
                                        val annotations = annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                        if (annotations.isNotEmpty()) {
                                            val url = annotations.first().item
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                                context.startActivity(intent)
                                            } catch (e: Exception) {}
                                            return@detectTapGestures
                                        }

                                        if (offset < textStr.length) {
                                            val charClicked = textStr[offset]
                                            if (charClicked == '☐') {
                                                val newDesc = textStr.substring(0, offset) + "☑" + textStr.substring(offset + 1)
                                                onToggleCheckbox?.invoke(note.copy(description = newDesc))
                                                return@detectTapGestures
                                            } else if (charClicked == '☑') {
                                                val newDesc = textStr.substring(0, offset) + "☐" + textStr.substring(offset + 1)
                                                onToggleCheckbox?.invoke(note.copy(description = newDesc))
                                                return@detectTapGestures
                                            }
                                        }
                                        onClick()
                                    }
                                }
                            }
                        )
                    }
                    if (folderName != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = folderName, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                        }
                    }
                }

                // Pulsante rapido per mettere/togliere la puntina
                if (onTogglePin != null) {
                    IconButton(onClick = onTogglePin) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Fissa nota",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                // Il pulsante per eliminare la nota è stato rimosso per supportare lo swipe
                if (onRemoveFromFolder != null) {
                    IconButton(onClick = onRemoveFromFolder) {
                        Icon(Icons.Default.Close, contentDescription = "Rimuovi da cartella")
                    }
                }
            }
        }
    }
}
