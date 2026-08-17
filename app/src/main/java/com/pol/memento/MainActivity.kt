package com.pol.memento

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.BorderStroke
import com.pol.memento.data.FolderWithNotes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.toArgb
import com.pol.memento.data.Note
import com.pol.memento.data.PriorityLevel
import sh.calvin.reorderable.*

class MainActivity : ComponentActivity() {
    // Colleghiamo il "cervello" (ViewModel) all'interfaccia
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Launcher per richiedere il permesso delle notifiche
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }

            val isDarkModeState by viewModel.isDarkMode.collectAsState()
            
            // Se il DataStore sta ancora caricando dal disco, non renderizziamo nulla
            // per evitare il "flash" bianco.
            if (isDarkModeState == null) {
                return@setContent
            }

            // Ora sappiamo con certezza quale tema usare
            val isDarkMode = isDarkModeState!!
            val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

            val view = androidx.compose.ui.platform.LocalView.current
            if (!view.isInEditMode) {
                androidx.compose.runtime.SideEffect {
                    val window = (view.context as android.app.Activity).window
                    window.statusBarColor = colorScheme.background.toArgb()
                    window.navigationBarColor = colorScheme.background.toArgb()
                    androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
                    androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkMode
                }
            }

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        onDarkModeChange = { viewModel.setDarkMode(it) }
                    )
                }
            }
        }
    }
}

enum class Screen { HOME, FOLDERS, ARCHIVE }

@Composable
fun AppNavigation(viewModel: MainViewModel, isDarkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    BackHandler(enabled = currentScreen != Screen.HOME) {
        currentScreen = Screen.HOME
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            val animSpec = tween<IntOffset>(300)
            when (targetState) {
                Screen.FOLDERS -> {
                    if (initialState == Screen.HOME) slideInHorizontally(animationSpec = animSpec) { width -> -width } togetherWith slideOutHorizontally(animationSpec = animSpec) { width -> width }
                    else slideInHorizontally(animationSpec = animSpec) { width -> width } togetherWith slideOutHorizontally(animationSpec = animSpec) { width -> -width }
                }
                Screen.HOME -> {
                    when (initialState) {
                        Screen.FOLDERS -> slideInHorizontally(animationSpec = animSpec) { width -> width } togetherWith slideOutHorizontally(animationSpec = animSpec) { width -> -width }
                        Screen.ARCHIVE -> slideInHorizontally(animationSpec = animSpec) { width -> -width } togetherWith slideOutHorizontally(animationSpec = animSpec) { width -> width }
                        else -> slideInHorizontally(animationSpec = animSpec) { width -> width } togetherWith slideOutHorizontally(animationSpec = animSpec) { width -> -width }
                    }
                }
                Screen.ARCHIVE -> {
                    if (initialState == Screen.HOME) slideInHorizontally(animationSpec = animSpec) { width -> width } togetherWith slideOutHorizontally(animationSpec = animSpec) { width -> -width }
                    else slideInHorizontally(animationSpec = animSpec) { width -> width } togetherWith slideOutHorizontally(animationSpec = animSpec) { width -> -width }
                }
            }
        }, label = "AppNavigation"
    ) { screen ->
        when (screen) {
            Screen.HOME -> MainScreen(viewModel, isDarkMode, onDarkModeChange, onNavigateToFolders = { currentScreen = Screen.FOLDERS }, onNavigateToArchive = { currentScreen = Screen.ARCHIVE })
            Screen.FOLDERS -> FoldersScreen(viewModel, onNavigateBack = { currentScreen = Screen.HOME })
            Screen.ARCHIVE -> ArchiveScreen(viewModel, onNavigateBack = { currentScreen = Screen.HOME })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, isDarkMode: Boolean, onDarkModeChange: (Boolean) -> Unit, onNavigateToFolders: () -> Unit, onNavigateToArchive: () -> Unit) {
    // Raccogliamo la lista delle note dal database in tempo reale
    val notes by viewModel.notesList.collectAsState()
    val foldersWithNotes by viewModel.foldersWithNotesList.collectAsState()

    // Variabili per mostrare o nascondere i popup
    var isSheetOpen by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var activeFilters by remember { mutableStateOf(setOf<PriorityLevel>()) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Variabili di stato "in memoria" sostituite dal DataStore!
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
        // Spostiamo l'elemento
        var newList = localNotes.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        
        // Ri-ordiniamo la lista forzando le note fissate in cima e per priorità.
        // In questo modo le note rimbalzano al loro posto corretto se trascinate nel gruppo sbagliato.
        newList = newList.sortedWith(
            compareByDescending<Note> { it.isPinned }
                .thenBy { 
                    when (it.priority) {
                        PriorityLevel.HIGH -> 1
                        PriorityLevel.MEDIUM -> 2
                        PriorityLevel.LOW -> 3
                    }
                }
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

    // Lo Scaffold è la struttura base dello schermo (contiene il pulsante fluttuante e la barra superiore)
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text("Memento", fontWeight = FontWeight.Bold) 
                },
                navigationIcon = {
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(
                            Icons.Default.FilterAlt, 
                            contentDescription = "Filtra",
                            tint = if (activeFilters.isNotEmpty()) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                },
                actions = {
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
                // Tasto Cartelle (sinistra)
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

                // Tasto Archivio (destra)
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
        val displayedNotes = if (activeFilters.isEmpty()) localNotes else localNotes.filter { it.priority in activeFilters }
        
        if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
            // LazyColumn è una lista scorrevole (simile alla RecyclerView)
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
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
                                    dragHandleModifier = if (activeFilters.isEmpty()) Modifier.draggableHandle() else Modifier,
                                    showDragHandle = activeFilters.isEmpty(),
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

    // Se la variabile diventa 'true', mostriamo il Bottom Sheet
    if (isSheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { isSheetOpen = false },
            sheetState = sheetState
        ) {
            AddNoteContent(
                noteToEdit = noteToEdit,
                initialPriority = defaultPriority,
                onSave = { title, desc, prio, pinned ->
                    if (noteToEdit == null) {
                        viewModel.addNote(title, desc, prio, pinned)
                    } else {
                        viewModel.updateNote(noteToEdit!!, title, desc, prio, pinned)
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

// Interfaccia della singola Nota (Dashboard)
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
                    if (note.isPinned) {
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
                        ClickableText(
                            text = annotatedText,
                            style = MaterialTheme.typography.bodyMedium.copy(color = LocalContentColor.current),
                            onClick = { offset ->
                                val text = note.description
                                if (offset < text.length) {
                                    val charClicked = text[offset]
                                    if (charClicked == '☐') {
                                        val newDesc = text.substring(0, offset) + "☑" + text.substring(offset + 1)
                                        onToggleCheckbox?.invoke(note.copy(description = newDesc))
                                        return@ClickableText
                                    } else if (charClicked == '☑') {
                                        val newDesc = text.substring(0, offset) + "☐" + text.substring(offset + 1)
                                        onToggleCheckbox?.invoke(note.copy(description = newDesc))
                                        return@ClickableText
                                    }
                                }
                                onClick()
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

// Interfaccia del Modale a Scomparsa (Add Note)
@Composable
fun AddNoteContent(
    noteToEdit: Note?,
    initialPriority: PriorityLevel,
    onSave: (String, String, PriorityLevel, Boolean) -> Unit,
    onCancel: () -> Unit
) {
    // Variabili temporanee in cui l'utente scrive prima di salvare
    var title by remember { mutableStateOf(noteToEdit?.title ?: "") }
    var description by remember { mutableStateOf(TextFieldValue(noteToEdit?.description ?: "")) }
    var priority by remember { mutableStateOf(noteToEdit?.priority ?: initialPriority) }
    var isPinned by remember { mutableStateOf(noteToEdit?.isPinned ?: false) }
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

                    if (newText.length == oldText.length + 1 && newValue.selection.min == newValue.selection.max) {
                        val cursor = newValue.selection.min
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
                                    finalValue = TextFieldValue(newStr, TextRange(startOfPrevLine))
                                } else {
                                    val prefix = "${num + 1}. "
                                    val newStr = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                                    finalValue = TextFieldValue(newStr, TextRange(cursor + prefix.length))
                                }
                            } else if (bulletMatch != null) {
                                val content = bulletMatch.groupValues[2]
                                if (content.isEmpty()) {
                                    val startOfPrevLine = textBeforeNewline.lastIndexOf('\n') + 1
                                    val newStr = newText.substring(0, startOfPrevLine) + newText.substring(cursor)
                                    finalValue = TextFieldValue(newStr, TextRange(startOfPrevLine))
                                } else {
                                    val prefix = "• "
                                    val newStr = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                                    finalValue = TextFieldValue(newStr, TextRange(cursor + prefix.length))
                                }
                            } else if (checkMatch != null) {
                                val content = checkMatch.groupValues[2]
                                if (content.isEmpty()) {
                                    val startOfPrevLine = textBeforeNewline.lastIndexOf('\n') + 1
                                    val newStr = newText.substring(0, startOfPrevLine) + newText.substring(cursor)
                                    finalValue = TextFieldValue(newStr, TextRange(startOfPrevLine))
                                } else {
                                    val prefix = "☐ "
                                    val newStr = newText.substring(0, cursor) + prefix + newText.substring(cursor)
                                    finalValue = TextFieldValue(newStr, TextRange(cursor + prefix.length))
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
                        description = TextFieldValue(
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
                            description = TextFieldValue(newStr, currentSelection)
                        } else if (line.startsWith("☑ ")) {
                            val newStr = text.substring(0, lineStart) + "☐ " + text.substring(lineStart + 2)
                            description = TextFieldValue(newStr, currentSelection)
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
                            description = TextFieldValue(
                                text = before + "**" + selected + "**" + after,
                                selection = TextRange(currentSelection.max + 4)
                            )
                        } else {
                            val before = currentText.substring(0, currentSelection.min)
                            val after = currentText.substring(currentSelection.max)
                            description = TextFieldValue(
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

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Fissa come notifica persistente")
            Spacer(modifier = Modifier.weight(1f))
            Switch(checked = isPinned, onCheckedChange = { isPinned = it })
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
                onClick = { onSave(title, description.text, priority, isPinned) },
                enabled = title.isNotBlank()
            ) {
                Text(if (noteToEdit == null) "Aggiungi Nota" else "Aggiorna Nota")
            }
        }
    }
}

// Piccolo componente per disegnare i pulsanti colorati della priorità
@Composable
fun PriorityButton(text: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val backgroundColor = if (isSelected) color else Color.Transparent
    val contentColor = if (isSelected) Color.White else color

    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = backgroundColor,
            contentColor = contentColor
        )
    ) {
        Text(text)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
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
    var selectedNotesForNewFolder by remember { mutableStateOf(setOf<Int>()) }
    var expandedFolderId by remember { mutableStateOf<Int?>(null) }
    
    var folderBeingEdited by remember { mutableStateOf<FolderWithNotes?>(null) }
    var selectedNotesForEditingFolder by remember { mutableStateOf(setOf<Int>()) }

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
                                .clickable {
                                    expandedFolderId = if (expandedFolderId == folderWithNotes.folder.id) null else folderWithNotes.folder.id
                                }
                        ) {
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
                } // Ends SwipeToDismissBox
            }
        }
    }
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 400.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                onSave = { title, desc, prio, pinned ->
                    if (noteToEdit == null) {
                        viewModel.addNote(title, desc, prio, pinned)
                    } else {
                        viewModel.updateNote(noteToEdit!!, title, desc, prio, pinned)
                    }
                    isSheetOpen = false
                },
                onCancel = { isSheetOpen = false }
            )
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDismissWrapper(
    note: Note,
    onDelete: (Note) -> Unit,
    onArchive: ((Note) -> Unit)? = null,
    onUnarchive: ((Note) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState.currentValue) {
        when (dismissState.currentValue) {
            SwipeToDismissBoxValue.EndToStart -> {
                onDelete(note)
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.StartToEnd -> {
                if (onArchive != null) {
                    onArchive(note)
                } else if (onUnarchive != null) {
                    onUnarchive(note)
                }
                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
            }
            SwipeToDismissBoxValue.Settled -> {}
        }
    }

    // Reset automatico dello stato quando il componente viene ri-composto (es. ripristino con undo)
    LaunchedEffect(Unit) {
        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
            dismissState.reset()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = onArchive != null || onUnarchive != null,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                    SwipeToDismissBoxValue.StartToEnd -> if (onArchive != null) Color(0xFF4CAF50) else Color(0xFF2196F3)
                    else -> Color.Transparent
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp)
            ) {
                // Icona completamento/ripristino (sinistra)
                if (onArchive != null || onUnarchive != null) {
                    Box(modifier = Modifier.align(Alignment.CenterStart)) {
                        Icon(if (onArchive != null) Icons.Default.Archive else Icons.Default.Unarchive, contentDescription = if (onArchive != null) "Archivia" else "Estrai da archivio", tint = Color.White)
                    }
                }
                
                // Icona elimina (destra)
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = Color.White)
                }
            }
        }
    ) {
        content()
    }
}
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun ArchiveScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val archivedNotes by viewModel.archivedNotesList.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val foldersWithNotes by viewModel.foldersWithNotesList.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Archivio", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
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
            val contentModifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)

            if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(archivedNotes, key = { it.id }) { note ->
                        val folderName = foldersWithNotes.find { f -> f.notes.any { it.id == note.id } }?.folder?.name
                        Box(modifier = Modifier.animateItem()) {
                            ArchiveNoteItem(note, viewModel, snackbarHostState, coroutineScope, isGridView, folderName)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = contentModifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(archivedNotes, key = { it.id }) { note ->
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
            onClick = {}, // Le note archiviate magari non si modificano a meno che non le si estrae
            onTogglePin = { viewModel.togglePin(note) },
            onToggleCheckbox = { updatedNote ->
                viewModel.updateNote(updatedNote, updatedNote.title, updatedNote.description, updatedNote.priority, updatedNote.isPinned)
            }
        )
    }
}

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text.text)
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        boldRegex.findAll(text.text).forEach { matchResult ->
            builder.addStyle(
                SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                matchResult.range.first,
                matchResult.range.last + 1
            )
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    val builder = AnnotatedString.Builder(text)
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    boldRegex.findAll(text).forEach { matchResult ->
        builder.addStyle(
            SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
            matchResult.range.first,
            matchResult.range.last + 1
        )
    }
    return builder.toAnnotatedString()
}
