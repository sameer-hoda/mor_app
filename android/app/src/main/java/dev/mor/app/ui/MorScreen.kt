package dev.mor.app.ui

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** Item 7 (final-build package): per-screen system-bar appearance.
 *  dark = light status-bar icons over dark art (onboarding, camera, Home at
 *  night); light screens (Home by day, profile, sheets) use dark icons. */
@Composable
fun MorSystemBars(dark: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            runCatching {
                val window = (view.context as Activity).window
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
        }
    }
}

/** Item 7 wrapper: the background slot draws full-bleed under the system bars;
 *  content respects the status-bar/cutout top inset plus horizontal screen
 *  margins plus 8dp, and never the bottom — bottom-pinned controls apply
 *  WindowInsets.navigationBarsPadding() themselves. */
@Composable
fun MorScreen(
    dark: Boolean = false,
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    MorSystemBars(dark)
    Box(Modifier.fillMaxSize()) {
        background()
        Box(
            Modifier.fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)
                )
                .padding(top = 8.dp),
            content = content,
        )
    }
}
