package com.pol.memento

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.pol.memento.ui.screens.ArchiveScreen
import com.pol.memento.ui.screens.FoldersScreen
import com.pol.memento.ui.screens.MainScreen

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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
            if (isDarkModeState == null) {
                return@setContent
            }

            val isDarkMode = isDarkModeState!!

            com.pol.memento.ui.theme.MementoTheme(
                darkTheme = isDarkMode,
                dynamicColor = true
            ) {
                val colorScheme = MaterialTheme.colorScheme
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as android.app.Activity).window
                        window.statusBarColor = colorScheme.background.toArgb()
                        window.navigationBarColor = colorScheme.background.toArgb()
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkMode
                        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkMode
                    }
                }

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

@Composable
fun AppNavigation(viewModel: MainViewModel, isDarkMode: Boolean, onDarkModeChange: (Boolean) -> Unit) {
    var showGitSyncScreen by androidx.compose.runtime.remember { mutableStateOf(false) }

    if (showGitSyncScreen) {
        com.pol.memento.ui.screens.GitSyncScreen(
            viewModel = viewModel,
            onNavigateBack = { showGitSyncScreen = false }
        )
    } else {
        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = 1,
            pageCount = { 3 }
        )
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

        androidx.activity.compose.BackHandler(enabled = pagerState.currentPage != 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(1)
            }
        }

        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> FoldersScreen(viewModel, onNavigateBack = { 
                    coroutineScope.launch { pagerState.animateScrollToPage(1) } 
                })
                1 -> MainScreen(
                    viewModel, 
                    isDarkMode, 
                    onDarkModeChange, 
                    onNavigateToFolders = { 
                        coroutineScope.launch { pagerState.animateScrollToPage(0) } 
                    }, 
                    onNavigateToArchive = { 
                        coroutineScope.launch { pagerState.animateScrollToPage(2) } 
                    },
                    onNavigateToGitSync = {
                        showGitSyncScreen = true
                    }
                )
                2 -> ArchiveScreen(viewModel, onNavigateBack = { 
                    coroutineScope.launch { pagerState.animateScrollToPage(1) } 
                })
            }
        }
    }
}
