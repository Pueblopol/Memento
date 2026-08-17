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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
            val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

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
