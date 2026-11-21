package dev.mor.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.mor.app.R

/** Palace design tokens from the final-build package (00-design-tokens),
 *  adapted to this package + the font files already in res/font. */
object MorColor {
    // Onboarding (palace)
    val Maroon = Color(0xFF7A1D16)
    val MaroonDark = Color(0xFF5E140F)
    val Gold = Color(0xFFC99A3B)
    val GoldLight = Color(0xFFE2B65A)
    val GoldPale = Color(0xFFF2C86A)
    val Ivory = Color(0xFFF7F1E6)
    val Card = Color(0xFFFFFBF3)
    val CardBorder = Color(0xFFE3D5BC)
    val SheetBorder = Color(0xFFE8DCC6)
    val Track = Color(0xFFEFE6D6)
    val Ink = Color(0xFF1F1A14)
    val Ink2 = Color(0xFF3A2F25)
    val Muted = Color(0xFF6B5E4E)
    val Eyebrow = Color(0xFF8A6A2A)
    val CtaDisabledBg = Color(0xFFE3D8C5)
    val Error = Color(0xFF9B2C1F)
    val CreamText = Color(0xFFFFF6E6)
    // Brand / Home
    val BrandCream = Color(0xFFF6F0E4)
    // Next-version onboarding (pack tokens)
    val Cream = Color(0xFFFAF4E6)
    val ScreenCream = Color(0xFFF6F1E5)
    val FrameCream = Color(0xFFF4EEDF)
    val Sand = Color(0xFFE8DFC8)
    val GoldText = Color(0xFFA8963F)
    val NoteFaint = Color(0xFFB9AE93)
    val Protein = Color(0xFF3E5F8A)
    val Carbs = Color(0xFFC9A227)
    val Fat = Color(0xFF1D6F64)
    val NightGold = Color(0xFFE3B23C)
    val NightPlum = Color(0xFF43222D)
    val HomeCream = Color(0xFFF6EFDC)
}

object MorShape {
    val Card = 16.dp
    val Tile = 18.dp
    val Sheet = 24.dp
    val Button = 16.dp
    val Segment = 14.dp
}

val Cormorant = FontFamily(
    Font(R.font.cormorant_500, FontWeight.Medium),
    Font(R.font.cormorant_600, FontWeight.SemiBold),
    Font(R.font.cormorant_700, FontWeight.Bold),
)

val MorInter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
)
