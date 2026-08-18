package com.pol.memento.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pol.memento.MainViewModel
import com.pol.memento.data.Note
import com.pol.memento.ui.components.NoteCard
import com.pol.memento.ui.components.SwipeToDismissWrapper
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val archivedNotes by viewModel.archivedNotesList.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val foldersWithNotes by viewModel.foldersWithNotesList.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    if (isSearchActive) {
        androidx.activity.compose.BackHandler {
            isSearchActive = false
            searchQuery = ""
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Archivio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isSearchActive = !isSearchActive 
                        if (!isSearchActive) searchQuery = ""
                    }) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search, 
                            contentDescription = "Cerca"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (archivedNotes.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessuna nota archiviata", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            }
        } else {
            val displayedNotes = if (searchQuery.isBlank()) {
                archivedNotes
            } else {
                archivedNotes.filter {
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
                            .padding(top = 16.dp, bottom = 12.dp),
                        placeholder = { Text("Cerca nell'archivio...") },
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = if (isSearchActive) 0.dp else 16.dp, bottom = 16.dp)
                    ) {
                        items(displayedNotes, key = { it.id }) { note ->
                            val folderName = foldersWithNotes.find { f -> f.notes.any { it.id == note.id } }?.folder?.name
                            Box(modifier = Modifier.animateItem()) {
                                ArchiveNoteItem(note, viewModel, snackbarHostState, coroutineScope, isGridView, folderName)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = if (isSearchActive) 0.dp else 16.dp, bottom = 16.dp)
                    ) {
                        items(displayedNotes, key = { it.id }) { note ->
                            val folderName = foldersWithNotes.find { f -> f.notes.any { it.id == note.id } }?.folder?.name
                            Box(modifier = Modifier.animateItem()) {
                                ArchiveNoteItem(note, viewModel, snackbarHostState, coroutineScope, isGridView, folderName)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArchiveNoteItem(
    note: Note,
    viewModel: MainViewModel,
    snackbarHostState: SnackbarHostState,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    isGridView: Boolean,
    folderName: String?
) {
    val deleteNoteAction: (Note) -> Unit = { deletedNote ->
        viewModel.deleteNote(deletedNote)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Nota eliminata definitivamente",
                actionLabel = "ANNULLA",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreNote(deletedNote)
            }
        }
    }

    val unarchiveNoteAction: (Note) -> Unit = { unarchivedNote ->
        viewModel.unarchiveNote(unarchivedNote)
        coroutineScope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = "Nota ripristinata",
                actionLabel = "ANNULLA",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.archiveNote(unarchivedNote)
            }
        }
    }

    SwipeToDismissWrapper(
        note = note,
        onDelete = deleteNoteAction,
        onArchive = null,
        onUnarchive = unarchiveNoteAction
    ) {
        NoteCard(
            modifier = Modifier.shadow(0.dp, shape = RoundedCornerShape(12.dp)),
            dragHandleModifier = Modifier,
            showDragHandle = false,
            isGridView = isGridView,
            isSelectedForShare = false,
            note = note,
            folderName = folderName,
            onClick = {},
            onTogglePin = { viewModel.togglePin(note) },
            onToggleCheckbox = { updatedNote ->
                viewModel.updateNote(updatedNote, updatedNote.title, updatedNote.description, updatedNote.priority, updatedNote.isPinned)
            }
        )
    }
}
