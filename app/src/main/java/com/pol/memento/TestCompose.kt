package com.pol.memento
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TestCompose() {
    LazyColumn {
        items(listOf(1)) {
            Modifier.animateItem()
        }
    }
}
