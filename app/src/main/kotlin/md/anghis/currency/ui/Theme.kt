package md.anghis.currency.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Палитра как в Лаб 2 и Лаб 3: светлый фон, белые карточки, тёмно-синяя шапка, синий акцент. */
object Palette {
    val bg = Color(0xFFF4F6FB)
    val card = Color(0xFFFFFFFF)
    val border = Color(0xFFE3E7F0)
    val header = Color(0xFF1E2A4A)
    val headerText = Color(0xFFFFFFFF)
    val headerMuted = Color(0xFFAEB8D6)
    val text = Color(0xFF1F2430)
    val muted = Color(0xFF6B7280)
    val accent = Color(0xFF3B5BDB)
    val accentDisabled = Color(0xFFB9C4EE)
    val badgeBg = Color(0xFFEDF1FF)
    val warnBg = Color(0xFFFFF4E5)
    val warnText = Color(0xFF9A5B00)
    val error = Color(0xFFC92A2A)
    val errorBg = Color(0xFFFFF0F0)
    val okBg = Color(0xFFEBFBEE)
    val okText = Color(0xFF2B8A3E)
}

private val Colors = lightColorScheme(
    primary = Palette.accent,
    onPrimary = Color.White,
    primaryContainer = Palette.badgeBg,
    onPrimaryContainer = Palette.accent,
    secondaryContainer = Palette.badgeBg,
    onSecondaryContainer = Palette.accent,
    background = Palette.bg,
    onBackground = Palette.text,
    surface = Palette.card,
    onSurface = Palette.text,
    surfaceVariant = Palette.bg,
    onSurfaceVariant = Palette.muted,
    surfaceContainer = Palette.card,
    outline = Palette.border,
    outlineVariant = Palette.border,
    error = Palette.error,
    errorContainer = Palette.errorBg,
    onErrorContainer = Palette.error,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}
