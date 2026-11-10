package dev.mor.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.mor.app.R

/**
 * The mosaic wall backdrop (Phase A onboarding v2 + Phase B whole-app reskin).
 * Phase A §4.2 / REDESIGN-SPEC item 1: only the wall moves —
 * 26s breeze, ease-in-out alternate loop, keyframes
 *   0%   translate(0px, 0px)   scale(1.02)
 *   50%  translate(-12px, 7px) scale(1.05)
 *   100% translate(9px, -9px)  scale(1.03)
 * Cover-drawn at 1.10 base scale (the inset:-5% overdraw), `alpha` opacity
 * (0.60 in onboarding v2, 0.40-0.45 in the app reskin). Transform-only.
 */
@Composable
fun MosaicWall(
    alpha: Float,
    modifier: Modifier = Modifier
) {
    val reduced = rememberReducedMotion()
    val inf = rememberInfiniteTransition(label = "breeze")
    val phase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 26000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breezePhase"
    )
    val t = if (reduced) 0f else phase
    val (tx, ty, s) = breezeKeyframe(t)

    Box(modifier.fillMaxSize()) {
        // soft token blobs behind the wall (onboarding style.css .b1/.b2)
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-140).dp, y = (-260).dp)
                .size(680.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFD3EBE6).copy(alpha = 0.85f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 30.dp, y = 30.dp)
                .size(600.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFFDEBC8).copy(alpha = 0.70f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.bg_feathereye),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = alpha,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = 1.10f * s
                        scaleY = 1.10f * s
                        translationX = tx
                        translationY = ty
                    }
            )
        }
    }
}

/** Piecewise interpolation of the three keyframe stops, ease-in-out per segment. */
private fun breezeKeyframe(t: Float): Triple<Float, Float, Float> {
    fun ease(u: Float): Float {
        val x = u.coerceIn(0f, 1f)
        return if (x < 0.5f) 4f * x * x * x
        else 1f - (-2f * x + 2f).let { it * it * it } / 2f
    }
    fun lerp(a: Float, b: Float, u: Float) = a + (b - a) * u
    fun mix(ax: Float, ay: Float, as_: Float, bx: Float, by: Float, bs: Float, u: Float) =
        Triple(lerp(ax, bx, u), lerp(ay, by, u), lerp(as_, bs, u))
    return if (t <= 0.5f)
        mix(0f, 0f, 1.02f, -12f, 7f, 1.05f, ease(t / 0.5f))
    else
        mix(-12f, 7f, 1.05f, 9f, -9f, 1.03f, ease((t - 0.5f) / 0.5f))
}
