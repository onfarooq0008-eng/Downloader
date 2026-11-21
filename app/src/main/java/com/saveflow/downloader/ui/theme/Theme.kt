package com.saveflow.downloader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.saveflow.downloader.domain.models.ThemeMode

val Success = Color(0xFF22A06B)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B5BF0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E9FF),
    onPrimaryContainer = Color(0xFF1B1B6B),
    secondary = Color(0xFFFF5C8A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3EB),
    onSecondaryContainer = Color(0xFF5C0F27),
    background = Color(0xFFF6F6FB),
    onBackground = Color(0xFF16161D),
    surface = Color.White,
    onSurface = Color(0xFF16161D),
    surfaceVariant = Color(0xFFEDEDF6),
    onSurfaceVariant = Color(0xFF6A6A7A),
    outline = Color(0xFFDADAE8),
    outlineVariant = Color(0xFFE6E6F0),
    error = Color(0xFFD93A4A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8E8BFF),
    onPrimary = Color(0xFF14134A),
    primaryContainer = Color(0xFF2A2A6E),
    onPrimaryContainer = Color(0xFFE0DFFF),
    secondary = Color(0xFFFF7FA3),
    onSecondary = Color(0xFF4D0B20),
    secondaryContainer = Color(0xFF5C1F34),
    onSecondaryContainer = Color(0xFFFFD9E3),
    background = Color(0xFF0E0F14),
    onBackground = Color(0xFFEDEDF5),
    surface = Color(0xFF171822),
    onSurface = Color(0xFFEDEDF5),
    surfaceVariant = Color(0xFF222433),
    onSurfaceVariant = Color(0xFF9A9BB0),
    outline = Color(0xFF30324A),
    outlineVariant = Color(0xFF262839),
    error = Color(0xFFFF6B79)
)

private val Sans = FontFamily.SansSerif

private val AppTypography = Typography(
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp)
)

private val AppShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp)
)

@Composable
fun SaveFlowTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
