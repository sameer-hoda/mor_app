package dev.mor.app.ui

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dev.mor.app.LaunchState
import dev.mor.app.R
import dev.mor.app.ui.LocalV2SheetTop
import kotlinx.coroutines.delay

/* SPEC §2 tokens — night/ivory/gold/sindoor palette for the welcome journey.
   The door + video intro is unchanged; the questions after "Let's begin" now
   live in V2Onboarding's steps (final-build package 02-onboarding-layout). */
private val JNight = Color(0xFF0E0B0A)
private val JIvory = Color(0xFFF7F1E6)
private val JInk = Color(0xFF1F1A14)
private val JInk2 = Color(0xFF4A4238)
private val JInk3 = Color(0xFF8A7F6D)
private val JGold = Color(0xFFC99A3B)
private val JCream2 = Color(0xFFE5D9C4)

private val JourneySerif = FontFamily(
    Font(R.font.cormorant_500, FontWeight.Medium),
    Font(R.font.cormorant_600, FontWeight.SemiBold),
    Font(R.font.cormorant_700, FontWeight.Bold),
)
private val JourneySans = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
)

private val EaseBloom = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
private val EaseSheet = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val EaseRise = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)

@Composable
fun WelcomeJourney(draft: V2Draft, sharedPlayer: Player? = null, onBegin: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val view = LocalView.current
    val reduced = rememberReducedMotion()
    var stage by remember { mutableIntStateOf(0) }
    var origin by remember { mutableStateOf(Offset(195f, 530f)) }
    val topA = remember { Animatable(844f) }

    LaunchedEffect(Unit) {
        MorSound.ensure(ctx)
        draft.stepKey = "journey"
        draft.save()
        // v4 #1: the door art is the first-launch destination; release the
        // splash as soon as this composable's first frame is up.
        LaunchState.destinationReady.value = true
    }
    val exoA = remember { ExoPlayer.Builder(ctx).build() }
    val exoB = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) {
        onDispose { exoA.release(); exoB.release() }
    }
    // v5 #1: the journey keeps its own palace clips through the greeting; the
    // hoisted background takes over only at the TLDR sheet (stage 5), so the
    // entrance and the "Namaste" caption always sit on the palace interior.
    val heroOn = stage >= 5
    // v4 #1: the tap pill and ring fade in only after the splash iris ends.
    val splashGone by LaunchState.splashGone.collectAsState()

    LaunchedEffect(Unit) {
        if (!reduced) {
            exoB.repeatMode = Player.REPEAT_MODE_ONE
            exoB.setMediaItem(rawMi(ctx, R.raw.journey_c3))
            exoB.prepare()
            exoA.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(ps: Int) {
                    if (ps != Player.STATE_ENDED) return
                    when (stage) {
                        2 -> stage = 3   // doors open -> reveal
                        3 -> stage = 4   // reveal -> bird in the arch + greeting
                    }
                }
            })
        }
    }
    LaunchedEffect(stage) {
        when (stage) {
            0 -> if (!reduced) {
                exoA.repeatMode = Player.REPEAT_MODE_ONE
                exoA.setMediaItem(rawMi(ctx, R.raw.journey_c0_bw))
                exoA.prepare()
                exoA.playWhenReady = true
            }
            1 -> {
                delay(if (reduced) 300L else 1500L)
                stage = 2
            }
            2 -> if (reduced) {
                delay(300); stage = 3
            } else {
                exoA.repeatMode = Player.REPEAT_MODE_OFF
                exoA.setMediaItem(rawMi(ctx, R.raw.journey_c1))
                exoA.prepare()
                exoA.playWhenReady = true
            }
            3 -> if (reduced) {
                delay(300); stage = 4
            } else {
                // v5 #1: the reveal — the doors are open and Neelu walks in.
                // This clip has no caption; the greeting waits for C3.
                exoA.repeatMode = Player.REPEAT_MODE_OFF
                exoA.setMediaItem(rawMi(ctx, R.raw.journey_c2))
                exoA.prepare()
                exoA.playWhenReady = true
            }
            4 -> if (!reduced) {
                // v5 #1: Neelu is now on screen, so the "Namaste" caption
                // lands on the palace interior instead of an empty doorway.
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                exoA.repeatMode = Player.REPEAT_MODE_ONE
                exoA.setMediaItem(rawMi(ctx, R.raw.journey_c3))
                exoA.prepare()
                exoA.playWhenReady = true
            }
        }
    }
    LaunchedEffect(heroOn) {
        if (heroOn && !reduced) {
            // v4 #3+8: the greeting reveals the shared hoisted player when it
            // exists, so the loop keeps playing into the steps without a restart.
            if (sharedPlayer != null) sharedPlayer.playWhenReady = true
            else exoB.playWhenReady = true
            exoA.pause()
        }
    }

    BackHandler(enabled = stage in 1..4) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        MorSound.back()
        stage -= 1
    }

    BoxWithConstraints(
        Modifier.fillMaxSize().background(
            // v4 #3+8: at the TLDR sheet the journey's own video layers step
            // aside and the hoisted background (with the band/focal logic)
            // shows through unchanged.
            if (stage >= 5 && sharedPlayer != null) Color.Transparent else JNight
        )
    ) {
        val den = LocalDensity.current
        val wpx = with(den) { maxWidth.toPx() }
        val hpx = with(den) { maxHeight.toPx() }
        val sg = maxOf(wpx / 948f, hpx / 1688f)
        val ogx = (wpx - 948f * sg) / 2f
        val ogy = (hpx - 1688f * sg) / 2f

        CompositionLocalProvider(LocalDensity provides Density(2f * sg, den.fontScale)) {
            val dimA = jAnim(if (stage == 4) 0.28f else 0f, tween(900), reduced)
            val heroA = jAnim(if (heroOn) 1f else 0f, tween(500), reduced)

            if (reduced) {
                Box(Modifier.fillMaxSize()) {
                    if (stage < 1) {
                        Image(
                            painterResource(R.drawable.journey_kf0), null,
                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                            colorFilter = ColorFilter.colorMatrix(greyMatrix()),
                        )
                    }
                    if (stage >= 2) {
                        Image(
                            painterResource(R.drawable.journey_kf2), null,
                            Modifier.fillMaxSize().graphicsLayer { alpha = if (stage == 2) 1f else 0f },
                            contentScale = ContentScale.Crop,
                        )
                    }
                    if (stage >= 3) {
                        Image(
                            painterResource(R.drawable.journey_kf3), null,
                            Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                        )
                    }
                }
            } else {
                // v4 #3+8: with the shared player hoisted, the TLDR stage is
                // rendered by the root NeeluVideoBackground instead, so the
                // same player instance continues into the steps.
                if (stage <= 4 || sharedPlayer == null) {
                    AndroidView(
                        factory = { c -> inflatePlayer(c).apply { player = exoA } },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                if (sharedPlayer == null) {
                    Box(Modifier.fillMaxSize().graphicsLayer { alpha = heroA.value }) {
                        AndroidView(
                            factory = { c -> inflatePlayer(c).apply { player = exoB } },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            BloomLayer(stage, reduced, origin)
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = dimA.value }
                    .background(Color(0xFF0E0806))
            )

            if (stage == 0) GateOverlay(reduced, splashGone) { o ->
                origin = Offset((o.x - ogx) / (2f * sg), (o.y - ogy) / (2f * sg))
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                MorSound.ensure(ctx)
                MorSound.next()
                stage = 1
            }
            if (stage == 4) NeeluOverlay(reduced) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                MorSound.next()
                stage = 5
            }
            TldrSheet(stage == 5, reduced) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                MorSound.next()
                // Hand off to the step system: Goal first (02-onboarding-layout).
                onBegin()
            }
        }
    }
}

private fun rawMi(ctx: android.content.Context, res: Int): MediaItem =
    MediaItem.fromUri("android.resource://${ctx.packageName}/$res")

private fun inflatePlayer(ctx: android.content.Context): PlayerView =
    LayoutInflater.from(ctx).inflate(R.layout.journey_player, null, false) as PlayerView

@Composable
private fun jAnim(target: Float, spec: FiniteAnimationSpec<Float>, reduced: Boolean): Animatable<Float, AnimationVector1D> {
    val a = remember { Animatable(target) }
    LaunchedEffect(target, reduced) {
        if (reduced) a.animateTo(target, tween(300)) else a.animateTo(target, spec)
    }
    return a
}

private fun Modifier.jrise(order: Int, reduced: Boolean): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(reduced) {
        if (reduced) a.snapTo(1f)
        else {
            delay(order * 250L)
            a.animateTo(1f, tween(800, easing = EaseRise))
        }
    }
    graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 16.dp.toPx() }
}

/* ---- Stage 1: colour bloom over the monochrome gate ---- */

@Composable
private fun BloomLayer(stage: Int, reduced: Boolean, origin: Offset) {
    if (stage < 1 || stage > 2) return
    val bloomR = jAnim(1100f, tween(1500, easing = EaseBloom), reduced)
    val fadeA = jAnim(if (stage >= 2) 0f else 1f, tween(400), reduced)
    if (fadeA.value <= 0.01f) return
    Image(
        painterResource(R.drawable.journey_kf0), null,
        Modifier.fillMaxSize().graphicsLayer { alpha = fadeA.value }.drawWithContent {
            val c = Offset(origin.x.dp.toPx(), origin.y.dp.toPx())
            clipPath(Path().apply { addOval(Rect(c, bloomR.value.dp.toPx())) }) {
                this@drawWithContent.drawContent()
            }
        },
        contentScale = ContentScale.Crop,
    )
}

/* ---- Stage 0 overlay ---- */

@Composable
private fun GateOverlay(reduced: Boolean, armed: Boolean, onEnter: (Offset) -> Unit) {
    // v4 #1: pill + pulsing ring hidden until the splash iris is done, then
    // a 500 ms fade. With animations off they appear immediately.
    val revealA = remember { Animatable(if (armed) 1f else 0f) }
    LaunchedEffect(armed, reduced) {
        if (reduced) revealA.snapTo(if (armed) 1f else 0f)
        else if (armed) revealA.animateTo(1f, tween(500))
    }
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxWidth().height(320.dp).background(
                Brush.verticalGradient(
                    listOf(Color(0xD10A0808), Color(0x590A0808), Color(0x000A0808))
                )
            )
        )
        Column(
            Modifier.fillMaxWidth().jrise(0, reduced).offset(y = 74.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "MOR",
                fontFamily = JourneySerif, fontWeight = FontWeight.Medium,
                fontSize = 50.sp, letterSpacing = 17.sp,
                color = JIvory,
                modifier = Modifier.padding(start = 17.dp),
            )
            Canvas(Modifier.size(120.dp, 10.dp)) {
                val y = 5.dp.toPx()
                drawLine(
                    Color(0xFFE9DCC2), Offset(0f, y), Offset(48.dp.toPx(), y),
                    strokeWidth = 0.8.dp.toPx(),
                )
                drawLine(
                    Color(0xFFE9DCC2), Offset(72.dp.toPx(), y), Offset(120.dp.toPx(), y),
                    strokeWidth = 0.8.dp.toPx(),
                )
                val d = Path().apply {
                    moveTo(60.dp.toPx(), 1.dp.toPx())
                    lineTo(64.dp.toPx(), 5.dp.toPx())
                    lineTo(60.dp.toPx(), 9.dp.toPx())
                    lineTo(56.dp.toPx(), 5.dp.toPx())
                    close()
                }
                drawPath(d, Color(0xFFE9DCC2), style = Stroke(0.8.dp.toPx()))
            }
            Text(
                "Eat well, the Indian way.",
                fontFamily = JourneySerif, fontSize = 19.sp, color = Color(0xFFEADFC9),
                style = TextStyle(fontStyle = FontStyle.Italic, fontSynthesis = FontSynthesis.All),
            )
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = revealA.value }) {
            TapRing()
            Box(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp)
                    .background(Color(0x990E0B0A), RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            ) {
                Text(
                    "TAP THE DOOR TO ENTER",
                    fontFamily = JourneySans, fontSize = 13.sp, letterSpacing = 2.1.sp,
                    color = JIvory,
                )
            }
        }
        Box(
            Modifier.fillMaxSize()
                .pointerInput(Unit) { detectTapGestures(onTap = { onEnter(it) }) }
                .clickable(onClickLabel = "Enter the palace") {
                    onEnter(Offset(195f, 530f))
                }
                .semantics {
                    role = Role.Button
                    contentDescription = "Enter the palace"
                }
        )
    }
}

@Composable
private fun TapRing() {
    val ring = rememberInfiniteTransition(label = "tapring")
    val rs by ring.animateFloat(
        0.7f, 1.9f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ringScale",
    )
    val ra by ring.animateFloat(
        0.9f, 0f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "ringAlpha",
    )
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.align(Alignment.TopCenter).offset(y = 576.dp).size(48.dp, 48.dp)) {
            drawCircle(
                JIvory,
                radius = 24.dp.toPx() * rs,
                style = Stroke(1.5.dp.toPx()),
                alpha = ra,
            )
        }
        Canvas(Modifier.align(Alignment.TopCenter).offset(y = 576.dp).size(48.dp, 48.dp)) {
            drawRect(
                Brush.radialGradient(
                    listOf(Color(0xCCFFFFFF), Color(0x00FFFFFF)),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.width / 2f,
                )
            )
            drawCircle(JIvory, radius = 7.dp.toPx())
        }
    }
}

/* ---- Stage 3 overlay ---- */

@Composable
private fun NeeluOverlay(reduced: Boolean, onNext: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(220.dp).background(
                Brush.verticalGradient(listOf(Color(0x000E0806), Color(0xC00E0806)))
            )
        )
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(160.dp).background(
                Brush.verticalGradient(listOf(Color(0x730E0806), Color(0x000E0806)))
            )
        )
        Text(
            "THE RED COURT",
            fontFamily = JourneySans, fontSize = 12.sp, letterSpacing = 3.6.sp,
            color = Color(0xFFF7EBD0),
            modifier = Modifier.align(Alignment.TopCenter).offset(y = 66.dp).jrise(1, reduced),
        )
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .padding(bottom = 46.dp)
                .jrise(1, reduced),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Namaste! I’m Neelu.",
                fontFamily = JourneySerif, fontWeight = FontWeight.SemiBold,
                fontSize = 34.sp, color = Color(0xFFFFF8EA),
            )
            Text(
                "I’ll walk you through. It takes about a minute.",
                fontFamily = JourneySans, fontSize = 14.5.sp, color = JCream2,
                modifier = Modifier.jrise(1, reduced),
            )
            Text(
                "TAP TO CONTINUE",
                fontFamily = JourneySans, fontSize = 11.sp, letterSpacing = 2.2.sp,
                color = Color(0xFFD9C9A8),
                modifier = Modifier.padding(top = 10.dp).jrise(2, reduced),
            )
        }
        Box(
            Modifier.fillMaxSize()
                .clickable(onClickLabel = "Continue") { onNext() }
                .semantics {
                    role = Role.Button
                    contentDescription = "Continue"
                }
        )
    }
}

/* ---- Stage 4: the short version ---- */

@Composable
private fun TldrSheet(visible: Boolean, reduced: Boolean, onBegin: () -> Unit) {
    AnimatedVisibility(
        visible,
        enter = slideInVertically(
            tween(if (reduced) 1 else 750, easing = EaseSheet)
        ) { it },
        exit = fadeOut(tween(1)),
    ) {
        Box(Modifier.fillMaxSize()) {
            // v4 #3+8: aim the background video at the band above the sheet.
            val density = LocalDensity.current
            val sheetTop = LocalV2SheetTop.current
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        if (visible) {
                            sheetTop.value = with(density) { coords.positionInRoot().y.toDp() }
                        }
                    },
            ) {
                Column(
                    Modifier.fillMaxWidth().height(596.dp).background(JIvory).drawBehind {
                        drawLine(JGold, Offset(0f, 0.6f.dp.toPx()), Offset(size.width, 0.6f.dp.toPx()), 1.2f.dp.toPx())
                    }.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Avatar()
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = JInk)) {
                                    append("Neelu")
                                }
                                withStyle(SpanStyle(color = JInk3)) { append(" · the short version") }
                            },
                            fontFamily = JourneySans, fontSize = 13.sp,
                        )
                    }
                    Text(
                        "Snap your meal.\nKnow your calories.",
                        fontFamily = JourneySerif, fontWeight = FontWeight.Bold,
                        fontSize = 29.sp, lineHeight = 29.6.sp, color = JInk,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Bullet("Any Indian meal, read in seconds")
                        Bullet("A plan built for your goal, diet and day")
                        Bullet("Gentle nudges at mealtimes, if you want them")
                    }
                    Spacer(Modifier.weight(1f))
                    Cta("Let’s begin", enabled = true, onClick = onBegin)
                    Text(
                        "6 quick questions · No sign-up needed",
                        fontFamily = JourneySans, fontSize = 12.5.sp, color = JInk3,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                }
                Apex(Modifier.align(Alignment.TopCenter).offset(y = (-13).dp))
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).graphicsLayer { rotationZ = 45f }.background(JGold))
        Spacer(Modifier.size(10.dp))
        Text(text, fontFamily = JourneySans, fontSize = 14.sp, color = JInk2)
    }
}

@Composable
private fun Avatar() {
    Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(JGold, radius = size.minDimension / 2f - 0.5.dp.toPx(), style = Stroke(1.dp.toPx()))
            drawCircle(JIvory, radius = size.minDimension / 2f - 1.5.dp.toPx(), style = Stroke(2.dp.toPx()))
        }
        Image(
            painterResource(R.drawable.journey_avatar), null,
            Modifier.size(34.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun Apex(modifier: Modifier = Modifier) {
    Canvas(modifier.size(90.dp, 15.dp)) {
        val sw = size.width / 90f
        val sh = size.height / 15f
        val p = Path().apply {
            moveTo(0f, 14.5f * sh)
            cubicTo(30f * sw, 14.5f * sh, 35f * sw, 1f * sh, 45f * sw, 1f * sh)
            cubicTo(55f * sw, 1f * sh, 60f * sw, 14.5f * sh, 90f * sw, 14.5f * sh)
            close()
        }
        drawPath(p, JIvory)
        drawPath(p, JGold, style = Stroke(1.2f.dp.toPx()))
    }
}

@Composable
private fun Cta(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) Color(0xFF7A1D16) else Color(0xFFE3D8C5))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontFamily = JourneySans, fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = if (enabled) Color(0xFFFFF6E6) else Color(0xFF6B5E4E),
            textAlign = TextAlign.Center,
        )
    }
}

private fun greyMatrix(): ColorMatrix {
    val m = ColorMatrix()
    m.setToSaturation(0f)
    for (i in 0..2) {
        val r = i * 5
        for (j in 0..3) m.values[r + j] *= 0.8424f
        m.values[r + 4] = -0.04f
    }
    return m
}
