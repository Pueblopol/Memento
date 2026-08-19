package com.pol.memento.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pol.memento.MainViewModel
import com.pol.memento.data.FolderWithNotes
import com.pol.memento.data.Note
import com.pol.memento.ui.components.NoteCard
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FoldersScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val foldersWithNotes by viewModel.foldersWithNotesList.collectAsState()
    val allNotes by viewModel.allNotesList.collectAsState()
    val lazyListState = rememberLazyListState()
    var localFolders by remember { mutableStateOf<List<FolderWithNotes>>(foldersWithNotes) }

    LaunchedEffect(foldersWithNotes) {
        localFolders = foldersWithNotes
    }

    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val newList = localFolders.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        localFolders = newList
    }

    var hasDragged by remember { mutableStateOf(false) }
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (reorderState.isAnyItemDragging) {
            hasDragged = true
        } else if (hasDragged && localFolders.isNotEmpty()) {
            viewModel.updateFoldersOrder(localFolders.map { it.folder })
            hasDragged = false
        }
    }

    var isCreatingFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var selectedNotesForNewFolder by remember { mutableStateOf(setOf<String>()) }
    var expandedFolderId by remember { mutableStateOf<String?>(null) }
    
    var folderBeingEdited by remember { mutableStateOf<FolderWithNotes?>(null) }
    var selectedNotesForEditingFolder by remember { mutableStateOf(setOf<String>()) }

    var showFolderMenuId by remember { mutableStateOf<String?>(null) }
    var isRenamingFolder by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<com.pol.memento.data.Folder?>(null) }
    var renameFolderText by remember { mutableStateOf("") }

    var isSheetOpen by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    val defaultPriority by viewModel.defaultPriority.collectAsState()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cartelle", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { isCreatingFolder = true }) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = "Nuova cartella")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(localFolders, key = { it.folder.id }) { folderWithNotes ->
                ReorderableItem(reorderState, key = folderWithNotes.folder.id) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                    val dismissState = rememberSwipeToDismissBoxState(
                        positionalThreshold = { it * .25f }
                    )

                    LaunchedEffect(dismissState.currentValue) {
                        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
                            viewModel.deleteFolder(folderWithNotes.folder)
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Cartella eliminata",
                                    actionLabel = "Annulla"
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreFolder(folderWithNotes)
                                }
                            }
                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                        }
                    }

                    LaunchedEffect(Unit) {
                        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                            dismissState.reset()
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                                    else -> Color.Transparent
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, shape = RoundedCornerShape(12.dp))
                                    .padding(horizontal = 20.dp)
                            ) {
                                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.White)
                                }
                            }
                        }
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation, shape = RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = {
                                        expandedFolderId = if (expandedFolderId == folderWithNotes.folder.id) null else folderWithNotes.folder.id
                                    },
                                    onLongClick = {
                                        showFolderMenuId = folderWithNotes.folder.id
                                    }
                                )
                        ) {
                            Box {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Menu, contentDescription = "Trascina cartella", modifier = Modifier.draggableHandle().padding(end = 8.dp), tint = Color.Gray)
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(folderWithNotes.folder.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                        IconButton(
                                            onClick = {
                                                folderBeingEdited = folderWithNotes
                                                selectedNotesForEditingFolder = folderWithNotes.notes.map { it.id }.toSet()
                                            }
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "Aggiungi note")
                                        }
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showFolderMenuId == folderWithNotes.folder.id,
                                        onDismissRequest = { showFolderMenuId = null }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Rinomina") },
                                            onClick = {
                                                showFolderMenuId = null
                                                folderToRename = folderWithNotes.folder
                                                renameFolderText = folderWithNotes.folder.name
                                                isRenamingFolder = true
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Elimina", color = Color.Red) },
                                            onClick = {
                                                showFolderMenuId = null
                                                viewModel.deleteFolder(folderWithNotes.folder)
                                                coroutineScope.launch {
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Cartella eliminata",
                                                        actionLabel = "Annulla"
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.restoreFolder(folderWithNotes)
                                                    }
                                                }
                                            }
                                        )
                                    }
                                AnimatedVisibility(visible = expandedFolderId == folderWithNotes.folder.id) {
                                    Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        folderWithNotes.notes.forEach { note ->
                                            NoteCard(
                                                note = note,
                                                showDragHandle = false,
                                                isGridView = false,
                                                isCompactMode = true,
                                                onClick = { 
                                                    noteToEdit = note
                                                    isSheetOpen = true
                                                },
                                                onRemoveFromFolder = { viewModel.removeNoteFromFolder(folderWithNotes.folder.id, note.id) },
                                                onToggleCheckbox = { updatedNote ->
                                                    viewModel.updateNote(updatedNote, updatedNote.title, updatedNote.description, updatedNote.priority, updatedNote.isPinned)
                                                }
                                            )
                                        }
                                    }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (isRenamingFolder) {
        AlertDialog(
            onDismissRequest = { isRenamingFolder = false },
            title = { Text("Rinomina Cartella") },
            text = {
                OutlinedTextField(
                    value = renameFolderText,
                    onValueChange = { renameFolderText = it },
                    label = { Text("Nome cartella") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameFolderText.isNotBlank() && folderToRename != null) {
                        viewModel.renameFolder(folderToRename!!, renameFolderText)
                        isRenamingFolder = false
                    }
                }) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { isRenamingFolder = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (isCreatingFolder) {
        AlertDialog(
            onDismissRequest = { isCreatingFolder = false },
            title = { Text("Nuova Cartella") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Nome cartella") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Seleziona note da aggiungere:", style = MaterialTheme.typography.titleMedium)
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allNotes) { note ->
                            NoteCard(
                                note = note,
                                showDragHandle = false,
                                isGridView = true,
                                isCompactMode = true,
                                isSelectedForShare = note.id in selectedNotesForNewFolder,
                                onClick = {
                                    selectedNotesForNewFolder = if (note.id in selectedNotesForNewFolder) {
                                        selectedNotesForNewFolder - note.id
                                    } else {
                                        selectedNotesForNewFolder + note.id
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.createFolder(newFolderName, selectedNotesForNewFolder)
                        isCreatingFolder = false
                        newFolderName = ""
                        selectedNotesForNewFolder = emptySet()
                    }
                }) {
                    Text("Crea")
                }
            },
            dismissButton = {
                TextButton(onClick = { isCreatingFolder = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (folderBeingEdited != null) {
        AlertDialog(
            onDismissRequest = { folderBeingEdited = null },
            title = { Text("Aggiungi note a ${folderBeingEdited!!.folder.name}") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Seleziona le note da includere:", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalItemSpacing = 8.dp,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allNotes) { note ->
                            NoteCard(
                                note = note,
                                showDragHandle = false,
                                isGridView = true,
                                isCompactMode = true,
                                isSelectedForShare = note.id in selectedNotesForEditingFolder,
                                onClick = {
                                    selectedNotesForEditingFolder = if (note.id in selectedNotesForEditingFolder) {
                                        selectedNotesForEditingFolder - note.id
                                    } else {
                                        selectedNotesForEditingFolder + note.id
                                    }
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateFolderNotes(folderBeingEdited!!.folder.id, selectedNotesForEditingFolder)
                    folderBeingEdited = null
                    selectedNotesForEditingFolder = emptySet()
                }) {
                    Text("Salva")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderBeingEdited = null }) {
                    Text("Annulla")
                }
            }
        )
    }

    if (isSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState
        ) {
            AddNoteContent(
                noteToEdit = noteToEdit,
                initialPriority = defaultPriority,
                folders = foldersWithNotes.map { it.folder },
                initialFolderId = foldersWithNotes.find { f -> f.notes.any { it.id == noteToEdit?.id } }?.folder?.id,
                onSave = { title, desc, prio, pinned, persistent, folderId ->
                    if (noteToEdit == null) {
                        viewModel.addNoteFromSheet(title, desc, prio, pinned, persistent, folderId)
                    } else {
                        viewModel.updateNoteFromSheet(noteToEdit!!, title, desc, prio, pinned, persistent, folderId)
                    }
                    isSheetOpen = false
                },
                onCancel = { isSheetOpen = false }
            )
        }
    }
}
