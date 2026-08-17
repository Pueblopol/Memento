package com.pol.memento.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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
