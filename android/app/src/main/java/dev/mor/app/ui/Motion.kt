package dev.mor.app.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring

/**
 * Motion port of the onboarding source springs (WAAPI spring stiffness/damping,
 * mass 1 — identical physics to Compose springs):
 *   screen  spring(260, 32)  critically damped  -> dr .992
 *   rise    spring(300, 22)  small settle       -> dr .635
 *   bubble  spring(380, 20)  springy            -> dr .513
 */
object M {
    private fun dr(c: Float, k: Float) = c / (2f * kotlin.math.sqrt(k))

    val ScreenSpring = spring<Float>(dampingRatio = dr(32f, 260f), stiffness = 260f)
    val RiseSpring = spring<Float>(dampingRatio = dr(22f, 300f), stiffness = 300f)
    val BubbleSpring = spring<Float>(dampingRatio = dr(20f, 380f), stiffness = 380f)

    /** Same physics as ScreenSpring, typed for AnimatedContent slide transitions. */
    val ScreenSpringIntOffset = spring<IntOffset>(
        dampingRatio = dr(32f, 260f), stiffness = 260f, visibilityThreshold = IntOffset.VisibilityThreshold
    )

    /** Phase A §4.3: heading/subline entrance stagger. */
    const val HEAD_STAGGER_MS = 150

    /** Exit drift per §4.4: fade + translate(-36px * dir), 200ms cubic-bezier(.4,0,1,1). */
    const val EXIT_MS = 200
    const val ENTER_CONTENT_MS = 90

    fun exitFloat(t: Float) = t * t * (3f - 2f * t) // smoothstep approx of (.4,0,1,1)
}

/** Reduced motion = system animator scale 0 (the spec's "reduced motion / animator scale 0"). */
@Composable
fun rememberReducedMotion(): Boolean {
    val ctx = LocalContext.current
    return remember(ctx) { isReducedMotion(ctx) }
}

fun isReducedMotion(ctx: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        ctx.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE, 1f
    ) == 0f
}.getOrDefault(false)
