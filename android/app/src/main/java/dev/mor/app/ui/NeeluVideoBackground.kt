package dev.mor.app.ui

// Mor · Onboarding background: Neelu at the palace gate, looping (v4 #3 + #8).
// One hoisted Media3 player (created in V2Onboarding, released when onboarding
// finishes) renders behind every onboarding step. Positioned so the peacock's
// fan and head sit in the visible band above the ivory sheet; a slow Ken Burns
// hides the loop point. Falls back to the static poster when animations are
// off, Battery Saver is on, or the device is low on RAM.

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.OptIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import dev.mor.app.R

/** Source video facts: measured from neelu_gate_poster.webp (720×1280). */
object NeeluVideo {
    const val ASPECT = 0.5625f      // width / height of the loop file
    const val FOCAL_FAN = 0.68f     // centre of the spread fan, as a fraction of the video's height
    const val FOCAL_HEAD = 0.74f    // the head, used when the visible band is short
}

/** The Red Court shot's backdrop, so any pre-first-frame gap stays in scene. */
private val CourtInk = Color(0xFF1A0C08)

/** Reported by every onboarding card (measured with onGloballyPositioned) so the
 *  video can aim the peacock at the visible band above it. Provided once in V2Onboarding. */
val LocalV2SheetTop = androidx.compose.runtime.staticCompositionLocalOf {
    androidx.compose.runtime.mutableStateOf(0.dp)
}

fun neeluVideoAllowed(ctx: Context): Boolean {
    val anim = Settings.Global.getFloat(
        ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
    ) != 0f
    val saver = (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
    val lowRam = (ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).isLowRamDevice
    return anim && !saver && !lowRam
}

/** Create once in the onboarding root; null means "poster only". Released by the caller. */
@Composable
fun rememberNeeluPlayer(): Player? {
    val ctx = LocalContext.current
    val useVideo = remember { neeluVideoAllowed(ctx) }
    var decodeFailed by remember { mutableStateOf(false) }
    val player = remember(useVideo) {
        if (!useVideo) null else ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri("android.resource://${ctx.packageName}/${R.raw.neelu_gate_loop}"))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    decodeFailed = true
                }
            })
            prepare()
        }
    }
    DisposableEffect(player) { onDispose { player?.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { player?.pause() }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { player?.play() }
    return if (decodeFailed) null else player
}

/**
 * The looping Red Court behind all of onboarding.
 *
 * @param player the hoisted player, or null for poster-only fallback
 * @param screenWidth / screenHeight full screen size, edge-to-edge
 * @param bandTop the status-bar inset
 * @param sheetTop the card/sheet's current top edge (measured with onGloballyPositioned)
 */
@OptIn(UnstableApi::class)
@Composable
fun NeeluVideoBackground(
    player: Player?,
    screenWidth: Dp,
    screenHeight: Dp,
    bandTop: Dp,
    sheetTop: Dp,
    modifier: Modifier = Modifier,
) {
    // Fill: scale so the video covers the screen.
    val videoH = maxOf(screenHeight, screenWidth / NeeluVideo.ASPECT)
    val band = sheetTop - bandTop
    val focal = if (band < 180.dp) NeeluVideo.FOCAL_HEAD else NeeluVideo.FOCAL_FAN
    val rawOffset = (bandTop + sheetTop) / 2 - videoH * focal
    // Clamp: never reveal space above the video; always run at least 24dp under the sheet's top edge.
    // During sheet slide-ins sheetTop can momentarily exceed the video, so the
    // low bound is floored at 0 — the range must never be empty.
    val minOffset = sheetTop + 24.dp - videoH
    val target = rawOffset.coerceIn(minOf(minOffset, 0.dp), 0.dp)
    val offsetY by animateDpAsState(
        target, spring(dampingRatio = 0.85f, stiffness = 300f), label = "neeluOffset",
    )

    // Slow Ken Burns: 1.00 → 1.03 over 20 s, ping-pong, so the loop point never shows.
    val kb = rememberInfiniteTransition(label = "kb")
    val kbScale by kb.animateFloat(
        1f, if (player != null) 1.03f else 1f,
        infiniteRepeatable(tween(20_000, easing = LinearEasing), RepeatMode.Reverse), label = "kbScale",
    )

    var firstFrame by remember { mutableStateOf(false) }
    val firstFrameListener = remember {
        object : Player.Listener {
            override fun onRenderedFirstFrame() { firstFrame = true }
        }
    }
    DisposableEffect(player) {
        player?.addListener(firstFrameListener)
        onDispose { player?.removeListener(firstFrameListener) }
    }
    val posterAlpha by animateFloatAsState(if (firstFrame) 0f else 1f, tween(200), label = "poster")

    Box(modifier.fillMaxSize().background(CourtInk)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(videoH)
                .offset(y = offsetY)
                .graphicsLayer {
                    scaleX = kbScale
                    scaleY = kbScale
                    transformOrigin = TransformOrigin(0.5f, focal)
                }
        ) {
            if (player != null) {
                AndroidView(
                    factory = { c ->
                        PlayerView(c).apply {
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            this.player = player
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { if (it.player != player) it.player = player },
                )
            }
            if (posterAlpha > 0.01f) {
                Image(
                    painterResource(R.drawable.neelu_gate_poster), null,
                    Modifier.fillMaxSize().graphicsLayer { alpha = posterAlpha },
                    contentScale = ContentScale.Crop,
                )
            }
        }
        // Status-bar legibility scrim: 35% → 0 over the status bar + 24dp.
        Box(
            Modifier.fillMaxWidth().height(bandTop + 24.dp).background(
                Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent))
            )
        )
    }
}
