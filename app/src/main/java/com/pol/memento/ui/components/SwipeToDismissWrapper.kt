package com.pol.memento.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pol.memento.data.Note

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
                        Icon(
                            if (onArchive != null) Icons.Default.Archive else Icons.Default.Unarchive,
                            contentDescription = if (onArchive != null) "Archivia" else "Estrai da archivio",
                            tint = Color.White
                        )
                    }
                }
                
                // Icona elimina (destra)
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Elimina",
                        tint = Color.White
                    )
                }
            }
        }
    ) {
        content()
    }
}
