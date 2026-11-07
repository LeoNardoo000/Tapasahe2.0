package com.example.rfidpaymentsys.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE9FF),
    onPrimaryContainer = Color(0xFF06244F),
    secondary = BrandBlueDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E4FA),
    onSecondaryContainer = Color(0xFF061F4A),
    tertiary = BrandAmber,
    onTertiary = BrandAmberDark,
    tertiaryContainer = Color(0xFFFFE8BF),
    onTertiaryContainer = Color(0xFF3B2600),
    background = Color(0xFFF3F7FE),
    onBackground = Color(0xFF101828),
    surface = Color.White,
    onSurface = Color(0xFF101828),
    surfaceVariant = Color(0xFFE4ECF8),
    onSurfaceVariant = Color(0xFF435068),
    outline = Color(0xFF7685A0),
    outlineVariant = Color(0xFFC9D6EA),
    error = DangerRed,
    surfaceDim = Color(0xFFD9E1EF),
    surfaceBright = Color.White,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F9FF),
    surfaceContainer = Color(0xFFEEF3FC),
    surfaceContainerHigh = Color(0xFFE8EFFA),
    surfaceContainerHighest = Color(0xFFE2EAF7)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF8FB9FF),
    onPrimary = Color(0xFF002F6C),
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFFA9C2F2),
    onSecondary = Color(0xFF0A2A66),
    secondaryContainer = Color(0xFF1C3A73),
    onSecondaryContainer = Color(0xFFD6E4FF),
    tertiary = Color(0xFFFFC857),
    onTertiary = Color(0xFF3B2600),
    tertiaryContainer = Color(0xFF5C3B00),
    onTertiaryContainer = Color(0xFFFFE8BF),
    background = Color(0xFF0B1220),
    onBackground = Color(0xFFE6ECF8),
    surface = Color(0xFF111A2B),
    onSurface = Color(0xFFE6ECF8),
    surfaceVariant = Color(0xFF243049),
    onSurfaceVariant = Color(0xFFB9C4DB),
    outline = Color(0xFF7F8CA8),
    outlineVariant = Color(0xFF33415E),
    error = Color(0xFFFF8A80),
    surfaceDim = Color(0xFF0B1220),
    surfaceBright = Color(0xFF243049),
    surfaceContainerLowest = Color(0xFF0A101C),
    surfaceContainerLow = Color(0xFF0F1727),
    surfaceContainer = Color(0xFF141D30),
    surfaceContainerHigh = Color(0xFF1A253A),
    surfaceContainerHighest = Color(0xFF212D45)
)

/** Rounded corners everywhere, like the icon. */
private val TapasaheShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun RFIDPaymentSysTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the app always shows its own brand colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = TapasaheShapes,
        content = content
    )
}
