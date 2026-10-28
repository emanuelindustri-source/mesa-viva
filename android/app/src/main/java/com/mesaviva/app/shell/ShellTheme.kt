package com.mesaviva.app.shell

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val MesaDark = darkColorScheme(
    primary = Color(0xFFE0A63A),
    onPrimary = Color(0xFF2B1A04),
    primaryContainer = Color(0xFF4A3410),
    onPrimaryContainer = Color(0xFFFFE4A8),
    secondary = Color(0xFFE7C66A),
    onSecondary = Color(0xFF2B1A04),
    tertiary = Color(0xFF7FD8B4),
    onTertiary = Color(0xFF052018),
    background = Color(0xFF061713),
    onBackground = Color(0xFFF5F1E6),
    surface = Color(0xFF0B241D),
    onSurface = Color(0xFFF5F1E6),
    surfaceVariant = Color(0xFF14352C),
    onSurfaceVariant = Color(0xFFB9CDC5),
    outline = Color(0xFF3D5C52),
    error = Color(0xFFE57373),
)

private val MesaLight = lightColorScheme(
    primary = Color(0xFF7A5200),
    onPrimary = Color(0xFFFFF8E8),
    primaryContainer = Color(0xFFFFDFA1),
    onPrimaryContainer = Color(0xFF271900),
    secondary = Color(0xFF6A5A2E),
    tertiary = Color(0xFF1D6B52),
    background = Color(0xFFF7F2E7),
    onBackground = Color(0xFF201C14),
    surface = Color(0xFFFFFBF2),
    onSurface = Color(0xFF201C14),
    surfaceVariant = Color(0xFFEDE4D3),
    onSurfaceVariant = Color(0xFF4E4638),
    outline = Color(0xFF807663),
)

private val MesaTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
    ),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 20.sp),
    labelSmall = TextStyle(fontSize = 11.sp, letterSpacing = 0.6.sp),
)

@Composable
fun MesaVivaTheme(state: ShellState, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val base = when {
        state.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> MesaDark
        else -> MesaLight
    }
    val accent = Color(state.festival.accent)
    val accent2 = Color(state.festival.accent2)
    val onAccent = if (accent.luminance() > 0.58f) Color(0xFF2B1A04) else Color(0xFFFFFBF2)
    val onAccent2 = if (accent2.luminance() > 0.58f) Color(0xFF211604) else Color(0xFFFFFBF2)
    val scheme = base.copy(
        primary = accent,
        onPrimary = onAccent,
        secondary = accent2,
        onSecondary = onAccent2,
    )
    MaterialTheme(colorScheme = scheme, typography = MesaTypography, content = content)
}
