package com.atruedev.kmpnfc.sample

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors =
    lightColorScheme(
        primary =
            androidx.compose.ui.graphics
                .Color(0xFF1565C0),
        secondary =
            androidx.compose.ui.graphics
                .Color(0xFF00838F),
    )

private val DarkColors =
    darkColorScheme(
        primary =
            androidx.compose.ui.graphics
                .Color(0xFF90CAF9),
        secondary =
            androidx.compose.ui.graphics
                .Color(0xFF4DD0E1),
    )

@Composable
fun SampleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
