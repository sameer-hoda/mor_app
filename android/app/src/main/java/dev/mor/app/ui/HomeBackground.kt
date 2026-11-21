package dev.mor.app.ui

// Mor · Home background: day "Morning gradient" (06:00–19:00), night "Dusk plum"
// (19:00–06:00), device local time. The leaf mosaic is gone in both modes.
// Status-bar icons are handled by MorSystemBars(dark = night) on Home's root.

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

val DayBackgroundBrush = Brush.verticalGradient(
    0.00f to Color(0xFFF6F0E4),   // brand cream
    0.55f to Color(0xFFF3E8CF),
    1.00f to Color(0xFFEFDFB8),   // soft gold wash
)
val NightBackgroundBrush = Brush.verticalGradient(
    0.00f to Color(0xFF43222D),
    0.55f to Color(0xFF301820),
    1.00f to Color(0xFF1D0F14),
)

/** Colours for everything that sits directly on the background (not inside a card). */
data class HomeOnBackground(
    val dayLabel: Color,        // "SATURDAY"
    val greeting: Color,        // "Good evening"
    val lightStatusBarIcons: Boolean,
)

val DayOnBackground = HomeOnBackground(
    dayLabel = Color(0xFF5B6A5E), greeting = Color(0xFF0B3A42),
    lightStatusBarIcons = false,
)
val NightOnBackground = HomeOnBackground(
    dayLabel = Color(0xFFE3B23C),       // brand gold, 7.1:1 on #43222D
    greeting = Color(0xFFF6F0E4),       // cream, 12.3:1
    lightStatusBarIcons = true,
)

fun isNight(now: LocalTime = LocalTime.now()): Boolean =
    now < LocalTime.of(6, 0) || now >= LocalTime.of(19, 0)

/** Greeting by time of day (applies in both modes). */
fun greetingFor(now: LocalTime = LocalTime.now()): String = when {
    now >= LocalTime.of(5, 0) && now < LocalTime.of(12, 0) -> "Good morning"
    now >= LocalTime.of(12, 0) && now < LocalTime.of(17, 0) -> "Good afternoon"
    else -> "Good evening"
}

/** Re-checks on resume and at the next 06:00 / 19:00 boundary while Home is open. */
@Composable
fun rememberIsNight(): Boolean {
    var night by remember { mutableStateOf(isNight()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { night = isNight() }
    LaunchedEffect(night) {
        val now = LocalDateTime.now()
        val next = if (night) {
            val six = now.toLocalDate().atTime(6, 0)
            if (now.toLocalTime() < LocalTime.of(6, 0)) six else six.plusDays(1)
        } else now.toLocalDate().atTime(19, 0)
        delay(Duration.between(now, next).toMillis().coerceAtLeast(1_000))
        night = isNight()
    }
    return night
}

/** Full-bleed background behind Home; cross-fades 600 ms, never flashes.
 *  `dim` is the next-version item 18 evening state: late at night with nothing
 *  logged, the stage dims so Neelu's single quiet reminder reads as a whisper. */
@Composable
fun HomeBackground(night: Boolean, modifier: Modifier = Modifier, dim: Boolean = false) {
    AnimatedContent(
        targetState = night,
        transitionSpec = { fadeIn(tween(600)) togetherWith fadeOut(tween(600)) },
        label = "homeBg",
        modifier = modifier.fillMaxSize(),
    ) { isNight ->
        Box(Modifier.fillMaxSize().background(if (isNight) NightBackgroundBrush else DayBackgroundBrush)) {
            if (dim) Box(Modifier.fillMaxSize().background(Color(0x1A140C08)))
        }
    }
}
