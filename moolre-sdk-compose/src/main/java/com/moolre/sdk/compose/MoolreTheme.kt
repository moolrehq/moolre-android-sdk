package com.moolre.sdk.compose

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal object MoolreBrandColors {
    val primary = Color(0xFF142A4A)
    val orange = Color(0xFFFDB93C)
    val buttonBackground = Color.White
    val buttonContent = Color.Black
    val buttonDisabledBackground = Color(0xFFD3D3D3)
    val buttonDisabledContent = Color.Black
}

private val MoolreLightColors = lightColorScheme(
    primary = MoolreBrandColors.primary,
    onPrimary = Color.White,
    secondary = MoolreBrandColors.orange,
    onSecondary = Color.Black
)

private val MoolreDarkColors = darkColorScheme(
    primary = MoolreBrandColors.primary,
    onPrimary = Color.White,
    secondary = MoolreBrandColors.orange,
    onSecondary = Color.Black
)

/**
 * Optional SDK theme for applications that want Moolre's brand color scheme.
 */
@Composable
fun MoolreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) MoolreDarkColors else MoolreLightColors,
        content = content
    )
}
