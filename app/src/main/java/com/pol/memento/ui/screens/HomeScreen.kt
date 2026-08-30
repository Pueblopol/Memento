package com.pol.memento.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel
import com.pol.memento.ui.components.NoteCard
import com.pol.memento.ui.components.PriorityButton
import com.pol.memento.ui.components.SwipeToDismissWrapper
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onNavigateToFolders: () -> Unit,
    onNavigateToArchive: () -> Unit,
    onNavigateToGitSync: () -> Unit
) {
    // Raccogliamo la lista delle note dal database in tempo reale
    val notes by viewModel.notesList.collectAsState()
    val foldersWithNotes by viewModel.foldersWithNotesList.collectAsState()

    // Variabili per mostrare o nascondere i popup
    var isSheetOpen by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var activeFilters by remember { mutableStateOf(setOf<PriorityLevel>()) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    if (isSearchActive) {
        androidx.activity.compose.BackHandler {
            isSearchActive = false
            searchQuery = ""
        }
    }

    // Variabili di stato DataStore
    val defaultPriority by viewModel.defaultPriority.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    // Lista locale modificabile per animare subito il drag&drop
    var localNotes by remember { mutableStateOf(notes) }
    
    // Aggiorniamo la lista locale quando il DB cambia
    LaunchedEffect(notes) {
        localNotes = notes
    }

    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        var newList = localNotes.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        
        newList = newList.sortedWith(
            compareBy<Note> { 
                when (it.priority) {
                    PriorityLevel.HIGH -> 1
                    PriorityLevel.MEDIUM -> 2
                    PriorityLevel.LOW -> 3
                }
            }.thenByDescending { it.isPinned }
        ).toMutableList()
        localNotes = newList
    }

    var hasDragged by remember { mutableStateOf(false) }
    LaunchedEffect(reorderState.isAnyItemDragging) {
        if (reorderState.isAnyItemDragging) {
            hasDragged = true
        } else if (hasDragged) {
            viewModel.updateNotesOrder(localNotes)
            hasDragged = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("Memento", fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = { viewModel.setGridView(!isGridView) }) {
                            Icon(
                                if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Cambia layout"
                            )
                        }
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(
                                Icons.Default.FilterAlt, 
                                contentDescription = "Filtra",
                                tint = if (activeFilters.isNotEmpty()) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                    }
                },
                actions = {
                    var isSyncing by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            isSyncing = true
                            viewModel.manualSync { message ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(message)
                                    isSyncing = false
                                }
                            }
                        },
                        enabled = !isSyncing
                    ) {
                        if (isSyncing) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sincronizza Git")
                        }
                    }
                    IconButton(onClick = { 
                        isSearchActive = !isSearchActive 
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search, 
                            contentDescription = "Cerca"
                        )
                    }
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Impostazioni")
                    }
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
        floatingActionButton = {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = onNavigateToFolders,
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Folder, contentDescription = "Cartelle")
                }
                
                FloatingActionButton(
                    onClick = { 
                        noteToEdit = null
                        isSheetOpen = true 
                    },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi Nota")
                }

                SmallFloatingActionButton(
                    onClick = onNavigateToArchive,
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Archive, contentDescription = "Archivio")
                }
            }
        }
    ) { paddingValues ->
        val filteredByPriority = if (activeFilters.isEmpty()) localNotes else localNotes.filter { it.priority in activeFilters }
        val displayedNotes = if (searchQuery.isBlank()) {
            filteredByPriority
        } else {
            filteredByPriority.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            AnimatedVisibility(visible = isSearchActive) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    placeholder = { Text("Cerca nelle note...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancella ricerca")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )
            }

            if (isGridView) {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedNotes, key = { it.id }) { note ->
                        val deleteNoteAction: (Note) -> Unit = { deletedNote ->
                            viewModel.deleteNote(deletedNote)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Nota eliminata",
                                    actionLabel = "ANNULLA",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.restoreNote(deletedNote)
                                }
                            }
                        }
                        val archiveNoteAction: (Note) -> Unit = { archivedNote ->
                            viewModel.archiveNote(archivedNote)
                            coroutineScope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()
                                val result = snackbarHostState.showSnackbar(
                                    message = "Nota archiviata",
                                    actionLabel = "ANNULLA",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.unarchiveNote(archivedNote)
                                }
                            }
                        }
                        Box(modifier = Modifier.animateItem()) {
                            val folderName = foldersWithNotes.find { f -> f.notes.any { it.id == note.id } }?.folder?.name
                            SwipeToDismissWrapper(note = note, onDelete = deleteNoteAction, onArchive = archiveNoteAction) {
                                NoteCard(
                                    modifier = Modifier.shadow(0.dp, shape = RoundedCornerShape(12.dp)),
                                    dragHandleModifier = Modifier,
                                    showDragHandle = false,
                                    isGridView = true,
                                    isSelectedForShare = false,
                                    note = note,
                                    folderName = folderName,
                                    onClick = {
                                        noteToEdit = note
                                        isSheetOpen = true
                                    },
                                    onTogglePin = { viewModel.togglePin(note) },
                                    onToggleCheckbox = { updatedNote ->
                                        viewModel.updateNote(updatedNote, updatedNote.title, updatedNote.description, updatedNote.priority, updatedNote.isPinned)
                                    }
                                )
                            }
                        }
                    }
                }
            } else {
                val canReorder = activeFilters.isEmpty() && searchQuery.isBlank()
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedNotes, key = { it.id }) { note ->
                        ReorderableItem(reorderState, key = note.id) { isDragging ->
                            val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp)
                            val deleteNoteAction: (Note) -> Unit = { deletedNote ->
                                viewModel.deleteNote(deletedNote)
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Nota eliminata",
                                        actionLabel = "ANNULLA",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.restoreNote(deletedNote)
                                    }
                                }
                            }
                            val archiveNoteAction: (Note) -> Unit = { archivedNote ->
                                viewModel.archiveNote(archivedNote)
                                coroutineScope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Nota archiviata",
                                        actionLabel = "ANNULLA",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.unarchiveNote(archivedNote)
                                    }
                                }
                            }
                            Box(modifier = Modifier.animateItem()) {
                                val folderName = foldersWithNotes.find { f -> f.notes.any { it.id == note.id } }?.folder?.name
                                SwipeToDismissWrapper(note = note, onDelete = deleteNoteAction, onArchive = archiveNoteAction) {
                                    NoteCard(
                                        modifier = Modifier.shadow(elevation, shape = RoundedCornerShape(12.dp)),
                                        dragHandleModifier = if (canReorder) Modifier.draggableHandle() else Modifier,
                                        showDragHandle = canReorder,
                                        isGridView = false,
                                        isSelectedForShare = false,
                                        note = note,
                                        folderName = folderName,
                                        onClick = {
                                            noteToEdit = note
                                            isSheetOpen = true
                                        },
                                        onTogglePin = { viewModel.togglePin(note) },
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

    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title = { Text("Filtra per priorità") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Il drag & drop è disabilitato mentre un filtro è attivo.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    PriorityButton("Tutte le note", Color.Gray, activeFilters.isEmpty()) { activeFilters = emptySet() }
                    PriorityButton("Alta", Color(0xFFE53935), PriorityLevel.HIGH in activeFilters) { 
                        activeFilters = if (PriorityLevel.HIGH in activeFilters) activeFilters - PriorityLevel.HIGH else activeFilters + PriorityLevel.HIGH 
                    }
                    PriorityButton("Media", Color(0xFF4CAF50), PriorityLevel.MEDIUM in activeFilters) { 
                        activeFilters = if (PriorityLevel.MEDIUM in activeFilters) activeFilters - PriorityLevel.MEDIUM else activeFilters + PriorityLevel.MEDIUM 
                    }
                    PriorityButton("Bassa", Color(0xFF2979FF), PriorityLevel.LOW in activeFilters) { 
                        activeFilters = if (PriorityLevel.LOW in activeFilters) activeFilters - PriorityLevel.LOW else activeFilters + PriorityLevel.LOW 
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFilterDialog = false }) {
                    Text("Chiudi")
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

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Impostazioni") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Dark Mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tema scuro", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = isDarkMode, onCheckedChange = onDarkModeChange)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Visualizzazione a griglia", style = MaterialTheme.typography.bodyLarge)
                        Switch(checked = isGridView, onCheckedChange = { viewModel.setGridView(it) })
                    }

                    HorizontalDivider()

                    // Priorità Predefinita
                    Text("Priorità predefinita", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PriorityButton("Alta", Color(0xFFE53935), defaultPriority == PriorityLevel.HIGH) { viewModel.setDefaultPriority(PriorityLevel.HIGH) }
                        PriorityButton("Media", Color(0xFF4CAF50), defaultPriority == PriorityLevel.MEDIUM) { viewModel.setDefaultPriority(PriorityLevel.MEDIUM) }
                        PriorityButton("Bassa", Color(0xFF2979FF), defaultPriority == PriorityLevel.LOW) { viewModel.setDefaultPriority(PriorityLevel.LOW) }
                    }

                    HorizontalDivider()

                    // Sincronizzazione Git
                    Text("Cloud Sync", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { 
                            showSettingsDialog = false
                            onNavigateToGitSync()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Configura Sincronizzazione Git")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSettingsDialog = false }) {
                    Text("Fatto")
                }
            }
        )
    }
}
