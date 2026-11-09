package com.jetbrains.kmpapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import com.jetbrains.kmpapp.data.model.PageAppearance

val QuranTealPrimary = Color(0xFF1B6B52)
val QuranTealDark = Color(0xFF0E4936)
val QuranTealLight = Color(0xFF43987C)
val QuranGoldAccent = Color(0xFFD4AF37)

// Highlight colors matching quran_android
val AyahSelectionHighlight = Color(0x4D388E3C) // Semi-transparent green
val AyahAudioHighlight = Color(0x4DFFA000)     // Semi-transparent amber
val AyahBookmarkHighlight = Color(0x4D1976D2)  // Semi-transparent blue

data class PageThemeColors(
    val backgroundColor: Color,
    val primaryTextColor: Color,
    val secondaryTextColor: Color,
    val headerFooterColor: Color,
    val cardSurfaceColor: Color,
    val isNight: Boolean
)

fun resolvePageThemeColors(
    appearance: PageAppearance,
    nightTextBrightness: Int = 255,
    nightBgBrightness: Int = 0
): PageThemeColors {
    return when (appearance) {
        PageAppearance.LIGHT_CREAM -> PageThemeColors(
            backgroundColor = Color(0xFFFFF8E8),
            primaryTextColor = Color(0xFF1A1A1A),
            secondaryTextColor = Color(0xFF4A4A4A),
            headerFooterColor = Color(0xFF5D4037),
            cardSurfaceColor = Color(0xFFF7EED8),
            isNight = false
        )
        PageAppearance.WHITE -> PageThemeColors(
            backgroundColor = Color(0xFFFFFFFF),
            primaryTextColor = Color(0xFF141414),
            secondaryTextColor = Color(0xFF505050),
            headerFooterColor = Color(0xFF37474F),
            cardSurfaceColor = Color(0xFFF4F6F8),
            isNight = false
        )
        PageAppearance.SEPIA -> PageThemeColors(
            backgroundColor = Color(0xFFF4ECD8),
            primaryTextColor = Color(0xFF3B2E1E),
            secondaryTextColor = Color(0xFF5C4B37),
            headerFooterColor = Color(0xFF5A4632),
            cardSurfaceColor = Color(0xFFEADFC5),
            isNight = false
        )
        PageAppearance.GREEN -> PageThemeColors(
            backgroundColor = Color(0xFFE8F5E9),
            primaryTextColor = Color(0xFF1B3B22),
            secondaryTextColor = Color(0xFF3E6347),
            headerFooterColor = Color(0xFF2E5736),
            cardSurfaceColor = Color(0xFFDCECDD),
            isNight = false
        )
        PageAppearance.NIGHT -> {
            val bg = nightBgBrightness.coerceIn(0, 150) / 255f
            val fg = nightTextBrightness.coerceIn(50, 255) / 255f
            PageThemeColors(
                backgroundColor = Color(bg, bg, bg, 1f),
                primaryTextColor = Color(fg, fg, fg, 1f),
                secondaryTextColor = Color((fg * 0.78f).coerceAtLeast(0.2f), (fg * 0.78f).coerceAtLeast(0.2f), (fg * 0.78f).coerceAtLeast(0.2f), 1f),
                headerFooterColor = Color((fg * 0.7f).coerceAtLeast(0.2f), (fg * 0.7f).coerceAtLeast(0.2f), (fg * 0.7f).coerceAtLeast(0.2f), 1f),
                cardSurfaceColor = Color((bg + 0.08f).coerceAtMost(0.4f), (bg + 0.08f).coerceAtMost(0.4f), (bg + 0.08f).coerceAtMost(0.4f), 1f),
                isNight = true
            )
        }
    }
}

/**
 * Builds the ColorFilter applied to Mushaf PNG images in Night Mode,
 * matching quran_android's ImageGrayscaleInvertFilter + brightness settings.
 */
fun buildMushafImageColorFilter(
    appearance: PageAppearance,
    nightTextBrightness: Int,
    nightBgBrightness: Int
): ColorFilter? {
    if (appearance != PageAppearance.NIGHT) return null
    val fg = nightTextBrightness.coerceIn(50, 255).toFloat()
    val bg = nightBgBrightness.coerceIn(0, 150).toFloat()
    // Transparent Mushaf PNGs have dark glyphs (RGB ~ 0) with opaque alpha on transparent background.
    // Invert RGB channels and scale to nightTextBrightness while adding nightBgBrightness offset:
    val scale = -(fg - bg) / 255f
    val offset = fg
    val matrix = ColorMatrix(
        floatArrayOf(
            scale, 0f, 0f, 0f, offset,
            0f, scale, 0f, 0f, offset,
            0f, 0f, scale, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
    return ColorFilter.colorMatrix(matrix)
}

private val LightColorScheme = lightColorScheme(
    primary = QuranTealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5EFE6),
    onPrimaryContainer = QuranTealDark,
    secondary = QuranGoldAccent,
    onSecondary = Color.Black,
    background = Color(0xFFF9FBF9),
    surface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = QuranTealLight,
    onPrimary = Color(0xFF06291D),
    primaryContainer = QuranTealDark,
    onPrimaryContainer = Color(0xFFD5EFE6),
    secondary = QuranGoldAccent,
    onSecondary = Color.Black,
    background = Color(0xFF101412),
    surface = Color(0xFF181E1B)
)

/**
 * Compact type scale in line with Material 3 / iOS HIG guidance for dense reading apps
 * (body 14sp / 15pt-ish, secondary 12-13sp, captions 11sp).
 */
private val QuranTypography = Typography(
    displayLarge = TextStyle(fontSize = 48.sp, lineHeight = 56.sp),
    displayMedium = TextStyle(fontSize = 38.sp, lineHeight = 46.sp),
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun QuranTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = QuranTypography
    ) {
        CompositionLocalProvider(LocalTextStyle provides QuranTypography.bodyLarge) {
            content()
        }
    }
}
