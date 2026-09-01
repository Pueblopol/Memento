package com.pol.memento.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel
import com.pol.memento.ui.components.PriorityButton
import com.pol.memento.ui.util.MarkdownVisualTransformation

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip

// Interfaccia del Modale a Scomparsa (Add Note)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNoteContent(
    noteToEdit: Note?,
    initialPriority: PriorityLevel,
    folders: List<com.pol.memento.data.Folder>,
    initialFolderId: String?,
    onSave: (String, String, PriorityLevel, Boolean, Boolean, String?) -> Unit,
    onCancel: () -> Unit
) {
    // Variabili temporanee in cui l'utente scrive prima di salvare
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var description by remember { mutableStateOf(TextFieldValue(noteToEdit?.description ?: "")) }
    var priority by remember { mutableStateOf(noteToEdit?.priority ?: initialPriority) }
    var isPinned by remember { mutableStateOf(noteToEdit?.isPinned ?: false) }
    var isPersistent by remember { mutableStateOf(noteToEdit?.isPersistent ?: false) }
    var selectedFolderId by remember { mutableStateOf(initialFolderId) }
    var isDescriptionFocused by remember { mutableStateOf(false) }

    val scrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Consuma tutto lo scroll residuo verticale per impedire al BottomSheet di chiudersi
                return available.copy(x = 0f) 
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .nestedScroll(scrollConnection)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = if (noteToEdit == null) "Nuova Nota" else "Modifica Nota", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            
            val context = LocalContext.current
            IconButton(
                onClick = {
                    val priorityIcon = when (priority) {
                        PriorityLevel.HIGH -> "🔴"
                        PriorityLevel.MEDIUM -> "🟢"
                        PriorityLevel.LOW -> "🔵"
                    }
                    val pin = if (isPinned) "📌 " else ""
                    val textToShare = "$pin$priorityIcon $title\n${description.text}".trimEnd()
                    
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, textToShare)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Condividi nota con...")
                    context.startActivity(shareIntent)
                }
            ) {
                Icon(Icons.Default.Share, contentDescription = "Condividi nota")
            }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Titolo") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = description,
                onValueChange = { newValue ->
                    var finalValue = newValue
                    val oldText = description.text
                    val newText = newValue.text

                    val cursor = newValue.selection.min
                    val newlinesInOld = oldText.count { it == '\n' }
                    val newlinesInNew = newText.count { it == '\n' }
                    val isNewlineInserted = cursor > 0 && newlinesInNew > newlinesInOld && newText[cursor - 1] == '\n'
                    
                    if (isNewlineInserted && newValue.selection.min == newValue.selection.max) {
                        if (cursor > 0 && newText[cursor - 1] == '\n') {
                            val textBeforeNewline = newText.substring(0, cursor - 1)
                            val previousLine = textBeforeNewline.substringAfterLast('\n')
                            
                            val numberRegex = Regex("^(\\d+)\\.\\s(.*)$")
                            val bulletRegex = Regex("^(•)\\s(.*)$")
                            val checkboxRegex = Regex("^(☐|☑)\\s(.*)$")
                            
                            val numMatch = numberRegex.find(previousLine)
                            val bulletMatch = bulletRegex.find(previousLine)
                            val checkMatch = checkboxRegex.find(previousLine)
                            
                            if (numMatch != null) {
                                val num = numMatch.groupValues[1].toInt()
                                val content = numMatch.groupValues[2]
                                if (content.isEmpty()) {
                                    val startOfPrevLine = textBeforeNewline.lastIndexOf('\n') + 1
                                    val newStr = newText.substring(0, startOfPrevLine) + newText.substring(cursor)
                                    finalValue = newValue.copy(text = newStr, selection = TextRange(startOfPrevLine))
                                } else {
                                    val prefix = "${num + 1}. "
                                    val newStr = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                                    finalValue = newValue.copy(text = newStr, selection = TextRange(cursor + prefix.length))
                                }
                            } else if (bulletMatch != null) {
                                val content = bulletMatch.groupValues[2]
                                if (content.isEmpty()) {
                                    val startOfPrevLine = textBeforeNewline.lastIndexOf('\n') + 1
                                    val newStr = newText.substring(0, startOfPrevLine) + newText.substring(cursor)
                                    finalValue = newValue.copy(text = newStr, selection = TextRange(startOfPrevLine))
                                } else {
                                    val prefix = "• "
                                    val newStr = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                                    finalValue = newValue.copy(text = newStr, selection = TextRange(cursor + prefix.length))
                                }
                            } else if (checkMatch != null) {
                                val content = checkMatch.groupValues[2]
                                if (content.isEmpty()) {
                                    val startOfPrevLine = textBeforeNewline.lastIndexOf('\n') + 1
                                    val newStr = newText.substring(0, startOfPrevLine) + newText.substring(cursor)
                                    finalValue = newValue.copy(text = newStr, selection = TextRange(startOfPrevLine))
                                } else {
                                    val prefix = "☐ "
                                    val newStr = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                                    finalValue = newValue.copy(text = newStr, selection = TextRange(cursor + prefix.length))
                                }
                            }
                        }
                    }
                    description = finalValue
                },
                label = { Text("Descrizione (Opzionale)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isDescriptionFocused = it.isFocused },
                minLines = 4,
                maxLines = 10,
                shape = RoundedCornerShape(16.dp),
                visualTransformation = MarkdownVisualTransformation()
            )
            
            AnimatedVisibility(visible = isDescriptionFocused) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val currentSelection = description.selection
                    val currentText = description.text
                    
                    val insertAtCursor = { prefix: String ->
                        val before = currentText.substring(0, currentSelection.min)
                        val after = currentText.substring(currentSelection.max)
                        description = description.copy(
                            text = before + prefix + after,
                            selection = TextRange(currentSelection.min + prefix.length)
                        )
                    }

                    IconButton(onClick = { insertAtCursor("• ") }) {
                        Icon(Icons.Default.FormatListBulleted, contentDescription = "Elenco puntato")
                    }
                    IconButton(onClick = { insertAtCursor("1. ") }) {
                        Icon(Icons.Default.FormatListNumbered, contentDescription = "Elenco numerato")
                    }
                    IconButton(onClick = { 
                        val cursor = currentSelection.min
                        val text = currentText
                        val lineStart = text.substring(0, cursor).lastIndexOf('\n') + 1
                        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
                        val line = text.substring(lineStart, lineEnd)
                        
                        if (line.startsWith("☐ ")) {
                            val newStr = text.substring(0, lineStart) + "☑ " + text.substring(lineStart + 2)
                            description = description.copy(text = newStr, selection = currentSelection)
                        } else if (line.startsWith("☑ ")) {
                            val newStr = text.substring(0, lineStart) + "☐ " + text.substring(lineStart + 2)
                            description = description.copy(text = newStr, selection = currentSelection)
                        } else {
                            insertAtCursor("☐ ") 
                        }
                    }) {
                        Icon(Icons.Default.CheckBox, contentDescription = "Casella di controllo")
                    }
                    IconButton(onClick = { 
                        if (currentSelection.min != currentSelection.max) {
                            val before = currentText.substring(0, currentSelection.min)
                            val selected = currentText.substring(currentSelection.min, currentSelection.max)
                            val after = currentText.substring(currentSelection.max)
                            description = description.copy(
                                text = before + "**" + selected + "**" + after,
                                selection = TextRange(currentSelection.max + 4)
                            )
                        } else {
                            val before = currentText.substring(0, currentSelection.min)
                            val after = currentText.substring(currentSelection.max)
                            description = description.copy(
                                text = before + "****" + after,
                                selection = TextRange(currentSelection.min + 2)
                            )
                        }
                    }) {
                        Icon(Icons.Default.FormatBold, contentDescription = "Grassetto")
                    }
                }
            }
        }

        Text(text = "Priorità")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityButton("Alta", Color(0xFFE53935), priority == PriorityLevel.HIGH) { priority = PriorityLevel.HIGH }
            PriorityButton("Media", Color(0xFF4CAF50), priority == PriorityLevel.MEDIUM) { priority = PriorityLevel.MEDIUM }
            PriorityButton("Bassa", Color(0xFF2979FF), priority == PriorityLevel.LOW) { priority = PriorityLevel.LOW }
        }

        if (folders.isNotEmpty()) {
            Text(text = "Aggiungi a cartella")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFolderId == null,
                        onClick = { selectedFolderId = null },
                        label = { Text("Nessuna") }
                    )
                }
                items(folders, key = { it.id }) { folder ->
                    FilterChip(
                        selected = selectedFolderId == folder.id,
                        onClick = { selectedFolderId = folder.id },
                        label = { Text(folder.name) }
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Fissa come notifica persistente")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = isPersistent, onCheckedChange = { isPersistent = it })
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Annulla")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSave(title, description.text, priority, isPinned, isPersistent, selectedFolderId) },
                enabled = title.isNotBlank()
            ) {
                Text(if (noteToEdit == null) "Aggiungi Nota" else "Aggiorna Nota")
            }
        }
    }
}
