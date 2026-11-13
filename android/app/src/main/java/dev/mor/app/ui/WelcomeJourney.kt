package dev.mor.app.ui

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import dev.mor.app.R
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/* SPEC §2 tokens — night/ivory/gold/sindoor palette for the welcome journey. */
private val JNight = Color(0xFF0E0B0A)
private val JIvory = Color(0xFFF7F1E6)
private val JIvoryHi = Color(0xFFFFFBF2)
private val JOptBg = Color(0xFFFFFDF8)
private val JInk = Color(0xFF1F1A14)
private val JInk2 = Color(0xFF4A4238)
private val JInk3 = Color(0xFF8A7F6D)
private val JSub = Color(0xFF5A4E3C)
private val JGold = Color(0xFFC99A3B)
private val JGoldInk = Color(0xFF8F6E2B)
private val JGoldDisc = Color(0xFFE2B65A)
private val JGoldRipple = Color(0xFFFFE2A6)
private val JLine = Color(0xFFE6D9C2)
private val JOptBorder = Color(0xFFE3D5BC)
private val JRadioRing = Color(0xFFCDBFA4)
private val JSindoor = Color(0xFF7A1D16)
private val JCheckInk = Color(0xFF3A1208)
private val JCreamText = Color(0xFFFFF8EA)
private val JCream2 = Color(0xFFE5D9C4)
private val JSelSub = Color(0xFFF1D9C6)
private val JMedBg = Color(0xFFF3E8D5)
private val JMedSel = Color(0x12FFF8EA)

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
private val EaseCam = CubicBezierEasing(0.3f, 0.7f, 0.2f, 1f)
private val EaseSheet = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
private val EaseRise = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
private val EasePop = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)

/* §3a goal rows — ids match the V2 contract. */
private data class GoalOpt(val id: String, val title: String, val sub: String, val main: String, val sec: String)
private val GOALS = listOf(
    GoalOpt("lose", "Lose weight", "Steady, no crash diets", "M3 7l6 6 4-4 8 8", "M21 11v6h-6"),
    GoalOpt("maintain", "Maintain weight", "Stay here, eat better", "M4 9h16", "M4 15h16"),
    GoalOpt("gain", "Gain weight", "Build up gently", "M3 17l6-6 4 4 8-8", "M15 7h6v6"),
    GoalOpt("healthy", "Eat healthier", "Same weight, better thalis", "M5 19c0-8 6-14 15-14 0 9-6 15-14 15", "M5 19l8-8"),
)
private val GOAL_LINES = mapOf(
    "lose" to "Steady wins. No crash diets, promise.",
    "maintain" to "Keeping you right where you are. Easy.",
    "gain" to "Building up gently. Extra roti approved.",
    "healthy" to "Better choices, same you. Love that.",
)
private const val GOAL_ASK = "Let’s start with the big one."

@Composable
fun WelcomeJourney(draft: V2Draft, onFinished: () -> Unit) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val reduced = rememberReducedMotion()
    var stage by remember { mutableIntStateOf(if (draft.answers.goal.isNotEmpty()) 5 else 0) }
    var origin by remember { mutableStateOf(Offset(195f, 530f)) }
    var captions by remember { mutableStateOf(false) }
    var reactOn by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        MorSound.ensure(ctx)
        draft.stepKey = "journey"
        draft.save()
    }

    val exoA = remember { ExoPlayer.Builder(ctx).build() }
    val exoB = remember { ExoPlayer.Builder(ctx).build() }
    val exoC = remember { ExoPlayer.Builder(ctx).build() }
    DisposableEffect(Unit) {
        onDispose { exoA.release(); exoB.release(); exoC.release() }
    }
    val heroOn = stage >= 4 || (stage == 3 && captions)

    LaunchedEffect(Unit) {
        if (!reduced) {
            exoB.repeatMode = Player.REPEAT_MODE_ONE
            exoB.setMediaItem(rawMi(ctx, R.raw.journey_c3))
            exoB.prepare()
            exoA.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(ps: Int) {
                    if (ps != Player.STATE_ENDED) return
                    when (stage) {
                        2 -> stage = 3
                        3 -> {
                            captions = true
                            exoB.playWhenReady = true
                        }
                    }
                }
            })
            exoC.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(ps: Int) {
                    if (ps == Player.STATE_ENDED) reactOn = false
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
            3 -> if (!reduced) {
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                exoA.repeatMode = Player.REPEAT_MODE_OFF
                exoA.setMediaItem(rawMi(ctx, R.raw.journey_c2))
                exoA.prepare()
                exoA.playWhenReady = true
            }
        }
    }
    LaunchedEffect(heroOn) {
        if (heroOn && !reduced) {
            exoB.playWhenReady = true
            exoA.pause()
        }
    }

    BackHandler(enabled = stage == 5) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        MorSound.back()
        stage = 4
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(JNight)) {
        val den = LocalDensity.current
        val wpx = with(den) { maxWidth.toPx() }
        val hpx = with(den) { maxHeight.toPx() }
        val sg = maxOf(wpx / 948f, hpx / 1688f)
        val bgx = ((wpx - 948f * sg) / 2f) / (2f * sg)
        val bgy = ((hpx - 1688f * sg) / 2f) / (2f * sg)
        val ogx = (wpx - 948f * sg) / 2f
        val ogy = (hpx - 1688f * sg) / 2f

        CompositionLocalProvider(LocalDensity provides Density(2f * sg, den.fontScale)) {
            val heroScale = jAnim(if (stage >= 5) 1.04f else 1f, tween(1100, easing = EaseCam), reduced)
            val dimA = jAnim(if (stage >= 5) 0.28f else 0f, tween(900), reduced)
            val heroA = jAnim(if (heroOn) 1f else 0f, tween(500), reduced)
            val reactA = remember { Animatable(0f) }
            LaunchedEffect(reactOn) {
                reactA.animateTo(if (reactOn) 1f else 0f, tween(if (reactOn) 120 else 300))
            }

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
                        Box(
                            Modifier.fillMaxSize().graphicsLayer {
                                alpha = 1f
                                scaleX = heroScale.value
                                scaleY = heroScale.value
                            }
                        ) {
                            Image(
                                painterResource(R.drawable.journey_kf3), null,
                                Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
            } else {
                AndroidView(
                    factory = { c -> inflatePlayer(c).apply { player = exoA } },
                    modifier = Modifier.fillMaxSize(),
                )
                    Box(Modifier.fillMaxSize().graphicsLayer {
                        alpha = heroA.value
                        scaleX = heroScale.value
                        scaleY = heroScale.value
                    }) {
                        AndroidView(
                            factory = { c -> inflatePlayer(c).apply { player = exoB } },
                            modifier = Modifier.fillMaxSize(),
                        )
                        AndroidView(
                            factory = { c -> inflatePlayer(c).apply { player = exoC } },
                            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = reactA.value },
                        )
                    }
            }

            BloomLayer(stage, reduced, origin)
            Box(
                Modifier.fillMaxSize().graphicsLayer { alpha = dimA.value }
                    .background(Color(0xFF0E0806))
            )

            if (stage == 0) GateOverlay(reduced) { o ->
                origin = Offset((o.x - ogx) / (2f * sg), (o.y - ogy) / (2f * sg))
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                MorSound.ensure(ctx)
                MorSound.next()
                stage = 1
            }
            if (stage == 3 && (captions || reduced)) NeeluOverlay(reduced) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                MorSound.next()
                stage = 4
            }
            TldrSheet(stage == 4, reduced) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                MorSound.next()
                stage = 5
            }
            GoalSheet(stage == 5, reduced, draft, onPicked = { id ->
                MorSound.ensure(ctx)
                MorSound.select(GOALS.indexOfFirst { it.id == id })
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                draft.answers = draft.answers.copy(goal = id)
                draft.save()
                if (!reduced) {
                    exoC.setMediaItem(rawMi(ctx, R.raw.journey_c4))
                    exoC.seekTo(0)
                    exoC.prepare()
                    exoC.playWhenReady = true
                    reactOn = true
                }
            }) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onFinished()
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
private fun GateOverlay(reduced: Boolean, onEnter: (Offset) -> Unit) {
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
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
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
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) JSindoor else Color(0xFFE3D9C6))
            .clickable(enabled = enabled, onClickLabel = label) { onClick() }
            .semantics { role = Role.Button; contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontFamily = JourneySans, fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = if (enabled) JCreamText else JInk3,
        )
    }
}

/* ---- Stage 5: question 1 (§3a option rows) ---- */

@Composable
private fun GoalSheet(
    visible: Boolean,
    reduced: Boolean,
    draft: V2Draft,
    onPicked: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val sel = draft.answers.goal
    AnimatedVisibility(
        visible,
        enter = slideInVertically(
            tween(if (reduced) 1 else 750, easing = EaseSheet)
        ) { it },
        exit = fadeOut(tween(1)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxWidth().height(696.dp).background(JIvory).drawBehind {
                    drawLine(JGold, Offset(0f, 0.6f.dp.toPx()), Offset(size.width, 0.6f.dp.toPx()), 1.2f.dp.toPx())
                }.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "STEP 1 OF 6",
                        fontFamily = JourneySans, fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp, letterSpacing = 1.76.sp, color = JGoldInk,
                    )
                    Spacer(Modifier.size(10.dp))
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        repeat(6) { i ->
                            Box(
                                Modifier.weight(1f).height(3.dp)
                                    .background(if (i == 0) JGold else JLine, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    HopAvatar(sel)
                    ReplyBubble(if (sel.isEmpty()) GOAL_ASK else GOAL_LINES[sel] ?: GOAL_ASK, sel)
                }
                Text(
                    "What’s your goal?",
                    fontFamily = JourneySerif, fontWeight = FontWeight.Bold,
                    fontSize = 32.sp, lineHeight = 32.sp, color = JInk,
                    modifier = Modifier.jrise(1, reduced),
                )
                Column(Modifier.jrise(2, reduced), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GOALS.forEach { g ->
                        OptRow(
                            g, sel == g.id, sel.isNotEmpty(),
                        ) { off ->
                            onPicked(g.id)
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Cta(
                    if (sel.isEmpty()) "Choose a goal to continue" else "Continue",
                    enabled = sel.isNotEmpty(),
                ) {
                    onContinue()
                }
            }
            Apex(Modifier.align(Alignment.TopCenter).offset(y = (-13).dp))
            }
        }
    }
}

@Composable
private fun OptRow(g: GoalOpt, isSel: Boolean, anySel: Boolean, onPick: (Offset) -> Unit) {
    var playKey by remember { mutableIntStateOf(0) }
    var rippleAt by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(isSel) { if (isSel) playKey++ }

    val press = remember { Animatable(1f) }
    val bg by animateColorAsState(if (isSel) JSindoor else JOptBg, tween(350), label = "rowBg")
    val bc by animateColorAsState(if (isSel) JSindoor else JOptBorder, tween(350), label = "rowBorder")
    val rowAlpha = if (isSel || !anySel) 1f else 0.72f

    Box(
        Modifier.fillMaxWidth().height(66.dp)
            .graphicsLayer {
                scaleX = press.value
                scaleY = press.value
                alpha = rowAlpha
            }
            .shadow(if (isSel) 10.dp else 1.dp, RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.5.dp, bc, RoundedCornerShape(16.dp))
            .pointerInput(g.id) {
                detectTapGestures(
                    onPress = {
                        coroutineScope {
                            launch { press.animateTo(0.975f, tween(150)) }
                        }
                        tryAwaitRelease()
                        coroutineScope {
                            launch { press.animateTo(1f, tween(150)) }
                        }
                    },
                    onTap = { off ->
                        rippleAt = off
                        onPick(off)
                    },
                )
            }
            .semantics {
                role = Role.RadioButton
                selected = isSel
                stateDescription = if (isSel) "Selected" else "Not selected"
                contentDescription = "${g.title}. ${g.sub}"
            }
    ) {
        GoldRipple(playKey, rippleAt)
        Row(
            Modifier.fillMaxSize().padding(start = 12.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Medallion(g, isSel, playKey)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                TitleText(g.title, isSel)
                SubText(g.sub, isSel)
            }
            Spacer(Modifier.size(10.dp))
            RadioDot(isSel, playKey)
        }
    }
}

@Composable
private fun GoldRipple(key: Int, at: Offset) {
    if (key == 0) return
    val a = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        a.snapTo(0f)
        a.animateTo(1f, tween(700, easing = LinearEasing))
    }
    Canvas(Modifier.fillMaxSize()) {
        val c = if (at == Offset.Zero) Offset(40.dp.toPx(), 33.dp.toPx()) else at
        drawCircle(
            JGoldRipple,
            radius = 12.dp.toPx() * a.value * 16f,
            alpha = 0.55f * (1f - a.value),
        )
    }
}

@Composable
private fun Medallion(g: GoalOpt, isSel: Boolean, playKey: Int) {
    val bg by animateColorAsState(if (isSel) JMedSel else JMedBg, tween(350), label = "medBg")
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(bg)
            .border(1.5.dp, if (isSel) JGoldDisc else Color.Transparent, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        GoalIcon(g, isSel, playKey)
    }
}

@Composable
private fun GoalIcon(g: GoalOpt, isSel: Boolean, playKey: Int) {
    val draw = remember(playKey) { Animatable(if (playKey == 0) 1f else 0f) }
    val mv = remember(playKey) { Animatable(0f) }
    val mv2 = remember(playKey) { Animatable(0f) }
    LaunchedEffect(playKey) {
        if (playKey == 0) return@LaunchedEffect
        draw.snapTo(0f)
        launch { draw.animateTo(1f, tween(550, easing = LinearEasing)) }
        when (g.id) {
            "lose", "gain" -> mv.animateTo(1f, tween(700, delayMillis = 100, easing = LinearEasing))
            "maintain" -> mv.animateTo(
                1f,
                keyframes { durationMillis = 800; -14f at 0; 7f at 360; -3f at 600; 0f at 800 },
            )
            "healthy" -> {
                mv.animateTo(
                    1f,
                    keyframes { durationMillis = 700; 0.4f at 0; 1.15f at 420; 1f at 700 },
                )
                mv2.animateTo(
                    1f,
                    keyframes { durationMillis = 700; -30f at 0; 6f at 420; 0f at 700 },
                )
            }
        }
    }
    val t = draw.value
    val sPre = with(LocalDensity.current) { 24.dp.toPx() / 24f }
    val fullMain = remember(g.main, sPre) { parsePath(g.main, sPre) }
    val fullSec = remember(g.sec, sPre) { parsePath(g.sec, sPre) }
    Canvas(
        Modifier.size(24.dp).graphicsLayer {
            when (g.id) {
                "lose" -> {
                    val f = kotlin.math.sin(Math.PI * mv.value).toFloat()
                    translationX = 2.dp.toPx() * f
                    translationY = 4.dp.toPx() * f
                }
                "gain" -> {
                    val f = kotlin.math.sin(Math.PI * mv.value).toFloat()
                    translationX = 2.dp.toPx() * f
                    translationY = -4.dp.toPx() * f
                }
                "maintain" -> rotationZ = -14f * (1f - mv.value)
                "healthy" -> {
                    val sc = if (playKey == 0) 1f else mv.value
                    scaleX = sc
                    scaleY = sc
                    rotationZ = if (playKey == 0) 0f else -30f * (1f - mv2.value)
                    transformOrigin = TransformOrigin(0.2f, 0.8f)
                }
            }
        }
    ) {
        val s = size.width / 24f
        val pm = PathMeasure()
        fun stroke(full: Path) {
            val path = if (t >= 0.999f) full else Path().apply {
                pm.setPath(full, false)
                pm.getSegment(0f, pm.length * t, this, true)
            }
            drawPath(
                path, if (isSel) JGoldRipple else JSindoor,
                style = Stroke(2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        stroke(fullMain)
        stroke(fullSec)
    }
}
@Composable
private fun TitleText(text: String, isSel: Boolean) {
    val c by animateColorAsState(if (isSel) JCreamText else JInk, tween(300), label = "t")
    Text(text, fontFamily = JourneySans, fontWeight = FontWeight.SemiBold, fontSize = 16.5.sp, color = c)
}

@Composable
private fun SubText(text: String, isSel: Boolean) {
    val c by animateColorAsState(if (isSel) JSelSub else JSub, tween(300), label = "s")
    Text(text, fontFamily = JourneySans, fontSize = 13.sp, color = c)
}

@Composable
private fun RadioDot(isSel: Boolean, playKey: Int) {
    val bc by animateColorAsState(if (isSel) Color.Transparent else JRadioRing, tween(300), label = "radio")
    Box(
        Modifier.size(26.dp).clip(CircleShape)
            .border(2.dp, bc, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (isSel) {
            val pop = remember(playKey) { Animatable(0.4f) }
            LaunchedEffect(playKey) {
                pop.snapTo(0.4f)
                pop.animateTo(
                    1f,
                    keyframes { durationMillis = 350; 1.18f at 230; 1f at 350 },
                )
            }
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                }.background(JGoldDisc, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(14.dp)) {
                    val s = size.width / 24f
                    val p = Path().apply {
                        moveTo(5 * s, 12 * s)
                        lineTo(10 * s, 17 * s)
                        lineTo(19 * s, 7 * s)
                    }
                    drawPath(
                        p, JCheckInk,
                        style = Stroke(3.2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}

@Composable
private fun HopAvatar(sel: String) {
    val hop = remember(sel) { Animatable(0f) }
    LaunchedEffect(sel) {
        if (sel.isNotEmpty()) {
            hop.snapTo(0f)
            hop.animateTo(
                1f,
                keyframes { durationMillis = 500; -1f at 200; 0f at 500 },
            )
        }
    }
    Box(Modifier.graphicsLayer { translationY = -6.dp.toPx() * hop.value }) {
        Avatar()
    }
}

@Composable
private fun ReplyBubble(text: String, sel: String) {
    val rise = remember(text) { Animatable(if (text == GOAL_ASK) 1f else 0f) }
    LaunchedEffect(text) {
        if (text != GOAL_ASK) {
            rise.snapTo(0f)
            rise.animateTo(1f, tween(350, easing = EaseRise))
        }
    }
    Box(
        Modifier.graphicsLayer {
            alpha = rise.value
            translationY = (1f - rise.value) * 16.dp.toPx()
        }
            .background(JIvoryHi, RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
            .border(1.dp, Color(0xFFE6D6B4), RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
            .padding(horizontal = 13.dp, vertical = 9.dp)
    ) {
        Text(text, fontFamily = JourneySans, fontSize = 13.5.sp, color = JInk2,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

/* ---- path + colour helpers ---- */

private fun parsePath(d: String, s: Float): Path {
    val p = Path()
    var cx = 0f
    var cy = 0f
    Regex("([A-Za-z])([^A-Za-z]*)").findAll(d).forEach { m ->
        val cmd = m.groupValues[1]
        val n = Regex("[+-]?(?:\\d+\\.?\\d*|\\.\\d+)(?:[eE][+-]?\\d+)?")
            .findAll(m.groupValues[2]).map { it.value.toFloat() * s }.toList()
        when (cmd) {
            "M" -> { cx = n[0]; cy = n[1]; p.moveTo(cx, cy) }
            "l" -> {
                var i = 0
                while (i < n.size) { cx += n[i]; cy += n[i + 1]; p.lineTo(cx, cy); i += 2 }
            }
            "h" -> { cx += n[0]; p.lineTo(cx, cy) }
            "v" -> { cy += n[0]; p.lineTo(cx, cy) }
            "c" -> {
                var i = 0
                while (i + 5 < n.size) {
                    p.cubicTo(cx + n[i], cy + n[i + 1], cx + n[i + 2], cy + n[i + 3], cx + n[i + 4], cy + n[i + 5])
                    cx += n[i + 4]; cy += n[i + 5]
                    i += 6
                }
            }
            "C" -> {
                var i = 0
                while (i + 5 < n.size) {
                    p.cubicTo(n[i], n[i + 1], n[i + 2], n[i + 3], n[i + 4], n[i + 5])
                    cx = n[i + 4]; cy = n[i + 5]
                    i += 6
                }
            }
        }
    }
    return p
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
