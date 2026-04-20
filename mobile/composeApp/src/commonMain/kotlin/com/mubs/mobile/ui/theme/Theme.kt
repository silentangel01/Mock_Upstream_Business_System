package com.mubs.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MunicipalBlue = Color(0xFF1565C0)
private val MunicipalBlueLight = Color(0xFF1E88E5)
private val MunicipalBlueDark = Color(0xFF0D47A1)
private val OnPrimary = Color.White
private val Surface = Color(0xFFF5F5F5)
private val Error = Color(0xFFD32F2F)

private val LightColors = lightColorScheme(
    primary = MunicipalBlue,
    onPrimary = OnPrimary,
    primaryContainer = Color(0xFFBBDEFB),
    onPrimaryContainer = MunicipalBlueDark,
    secondary = Color(0xFF546E7A),
    onSecondary = OnPrimary,
    surface = Surface,
    onSurface = Color(0xFF212121),
    error = Error,
    onError = OnPrimary,
    background = Color.White,
    onBackground = Color(0xFF212121)
)

@Composable
fun MubsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
