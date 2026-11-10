package dev.mor.app.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap

/**
 * Mosaic Neelu pose bitmaps, decoded once at app start (Phase A/B cold-load rule:
 * every pose paints from an in-memory bitmap, faded in ~220ms when decoded late —
 * a grey or empty frame is a regression).
 */
object PoseArt {
    val poses = listOf("happy", "wave", "cool", "thinking", "joy", "love", "hungry", "sleepy")
    const val COLD_LOAD_CAP_MS = 1200
    const val FADE_IN_MS = 220

    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    fun decodeAllAsync(ctx: Context) {
        val appCtx = ctx.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            poses.forEach { p ->
                if (!cache.containsKey(p)) {
                    runCatching {
                        val res = appCtx.resources
                        val id = res.getIdentifier("neelu_$p", "drawable", appCtx.packageName)
                        if (id != 0) {
                            val bmp = BitmapFactory.decodeResource(res, id)
                            if (bmp != null) cache.put(p, bmp.asImageBitmap())
                        }
                    }
                }
            }
        }
    }

    /** Suspend until decoded, capped at 1200ms. */
    suspend fun await(pose: String): ImageBitmap? = withTimeoutOrNull(COLD_LOAD_CAP_MS.toLong()) {
        while (!cache.containsKey(pose)) delay(30)
        cache[pose]
    }

    fun peek(pose: String): ImageBitmap? = cache[pose]
}

/** State that updates at decode time so recomposition can fade the pose in. */
@Composable
fun rememberPose(pose: String): ImageBitmap? {
    var bmp by remember(pose) { mutableStateOf(PoseArt.peek(pose)) }
    LaunchedEffect(pose) {
        if (bmp == null) {
            val loaded = PoseArt.await(pose)
            if (loaded != null) bmp = loaded
        }
    }
    return bmp
}

/**
 * Paints a mosaic Neelu pose. Never empty/grey: while decoding the slot holds
 * layout (parent provides the teal card behind); when the bitmap lands the
 * first paint fades in over ~220ms.
 */
@Composable
fun NeeluBitmap(
    pose: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val bmp = rememberPose(pose)
    var painted by remember(pose) { mutableStateOf(bmp != null) }
    if (bmp != null && !painted) painted = true
    val a by animateFloatAsState(
        targetValue = if (painted) 1f else 0f,
        animationSpec = tween(PoseArt.FADE_IN_MS), label = "poseFade"
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(a)
            )
        }
    }
}
