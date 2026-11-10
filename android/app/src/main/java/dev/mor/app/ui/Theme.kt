package dev.mor.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import dev.mor.app.R

// Palette mirrors web styles.css :root.
val Teal = Color(0xFF0B4A57)
val TealDark = Color(0xFF08343D)
val Neel = Color(0xFF1F4FB5)
val Pankh = Color(0xFF19A39A)
val Panna = Color(0xFF1E8A5A)
val Genda = Color(0xFFF2A93B)
val Gold = Color(0xFFF2B544)
val Jamun = Color(0xFF2D2A6E)
val Kumkum = Color(0xFFD9483B)
val Chandan = Color(0xFFF8F5EE)
val Ink = Color(0xFF142027)
val Sub = Color(0xFF4A5A63)
val Line = Color(0xFFEDE7DA)
val Line2 = Color(0xFFD9D2C2)
val Mint = Color(0xFFE3F2EF)
val Mint2 = Color(0xFFD3EBE6)
val AmberBg = Color(0xFFFDEBC8)
val Amber = Color(0xFF7A4B05)
val GreenBg = Color(0xFFDDF1E6)
val Green = Color(0xFF155E3D)
val RedBg = Color(0xFFFBE1DE)
val Red = Color(0xFF9A2A20)
val LavBg = Color(0xFFE6E4F6)
val Disabled = Color(0xFFE3DDD0)

private val MorColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    secondary = Pankh,
    tertiary = Gold,
    background = Chandan,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    error = Kumkum,
)

private val certProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

val MorDisplay = FontFamily(
    Font(GoogleFont("Baloo 2"), certProvider, FontWeight.SemiBold),
    Font(GoogleFont("Baloo 2"), certProvider, FontWeight.Bold),
    Font(GoogleFont("Baloo 2"), certProvider, FontWeight.ExtraBold),
)

val MorBody = FontFamily(
    Font(GoogleFont("Mukta"), certProvider, FontWeight.Normal),
    Font(GoogleFont("Mukta"), certProvider, FontWeight.Medium),
    Font(GoogleFont("Mukta"), certProvider, FontWeight.SemiBold),
    Font(GoogleFont("Mukta"), certProvider, FontWeight.Bold),
)

/** Mukta for all body/label type; Baloo 2 (MorDisplay) for display, headline
 *  and titleLarge slots (sizes/weights unchanged, font family only). Raw
 *  screen-level hero Texts also apply MorDisplay at call sites. */
private fun morTypo(): Typography {
    val base = Typography()
    fun body(s: androidx.compose.ui.text.TextStyle) = s.copy(fontFamily = MorBody)
    fun head(s: androidx.compose.ui.text.TextStyle) = s.copy(fontFamily = MorDisplay)
    return Typography(
        displayLarge = head(base.displayLarge),
        displayMedium = head(base.displayMedium),
        displaySmall = head(base.displaySmall),
        headlineLarge = head(base.headlineLarge),
        headlineMedium = head(base.headlineMedium),
        headlineSmall = head(base.headlineSmall),
        titleLarge = head(base.titleLarge),
        titleMedium = body(base.titleMedium),
        titleSmall = body(base.titleSmall),
        bodyLarge = body(base.bodyLarge),
        bodyMedium = body(base.bodyMedium),
        bodySmall = body(base.bodySmall),
        labelLarge = body(base.labelLarge),
        labelMedium = body(base.labelMedium),
        labelSmall = body(base.labelSmall),
    )
}

@Composable
fun MorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MorColors, typography = morTypo(), content = content)
}
