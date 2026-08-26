package com.canni.runpod.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RunPodPurple = Color(0xFF5F4CFE)
val RunPodPurpleSoft = Color(0xFFBDB6FF)

private val DarkColors = darkColorScheme(
    primary = RunPodPurpleSoft,
    onPrimary = Color(0xFF241A66),
    primaryContainer = Color(0xFF3B2FBF),
    onPrimaryContainer = Color(0xFFE4E0FF),
    secondary = Color(0xFFB0BEC5),
    background = Color(0xFF0E1013),
    onBackground = Color(0xFFE6E8EB),
    surface = Color(0xFF17191E),
    onSurface = Color(0xFFE6E8EB),
    surfaceVariant = Color(0xFF22252C),
    onSurfaceVariant = Color(0xFF9AA3AF),
    outline = Color(0xFF3A3F47),
    error = Color(0xFFFF6B6B),
)

private val LightColors = lightColorScheme(
    primary = RunPodPurple,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E0FF),
    onPrimaryContainer = Color(0xFF241A66),
)

@Composable
fun RunPodTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
