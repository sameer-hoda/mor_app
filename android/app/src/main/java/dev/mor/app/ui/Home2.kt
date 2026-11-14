package dev.mor.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import dev.mor.app.LaunchState
import dev.mor.app.R
import dev.mor.app.data.AppVm
import dev.mor.app.data.Insights
import dev.mor.app.data.Notif
import dev.mor.app.data.Profile
import dev.mor.app.data.friendlyErr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Phase 2 Home (integration kit MASTER-PROMPT Part A) + overlays:
 * trends sheet (Part C), notifications sheet, and the full-screen Profile
 * (Part B) circle-revealing from the avatar. The Add-meal M1 sheet stays in
 * Home.kt (AddSheet). Kit screens use the approved preview stacks (Part F):
 * sans = system default family, serif = FontFamily.Serif.
 */

// ---------- Part F tokens (app.html :root) ----------

val H2Teal = Color(0xFF0F5560)
val H2Ink = Color(0xFF0B3A42)
val H2Ink2 = Color(0xFF2C4549)
val H2Muted = Color(0xFF4A5C5E)
val H2Mute2 = Color(0xFF6B6A5A)
val H2Gold = Color(0xFFE0A22E)
val H2GoldPlus = Color(0xFFF1C565)
val H2Water = Color(0xFFD6ECF1)
val H2WaterInk = Color(0xFF0B4A60)
val H2WaterIcon = Color(0xFF0B5A73)
val H2WaterSeg = Color(0xFF3A8EA0)
val H2Cream = Color(0xFFF6EFDC)
val H2Cream2 = Color(0xFFFFFAF0)
val H2Cream3 = Color(0xFFFBF5E6)
val H2Mint = Color(0xFFE4F0EE)
val H2Badge = Color(0xFFB5321D)
val H2Line = Color(0x240F5560)      // rgba(15,85,96,.14)
val H2Hair = Color(0x1A0F5560)      // rgba(15,85,96,.1)
val H2Border = Color(0x330F5560)    // rgba(15,85,96,.2)
val H2Track = Color(0x120F5560)     // rgba(15,85,96,.07)
val H2IconBtnBg = Color(0xD9FFFCF3) // rgba(255,252,243,.85)
val H2BubbleBg = Color(0xF0FFFCF3)  // rgba(255,252,243,.94)
val H2TrackerBg = Color(0xE0FBF6E6) // rgba(251,246,230,.88)
val H2WeekBg = Color(0xE6FBF6E6)    // rgba(251,246,230,.9)
val H2WaterBorder = Color(0xFF8CC3D1)
val H2WaterEmpty = Color(0x333A8EA0) // rgba(58,142,160,.2)
val H2QaBg = Color(0xFFFFF8E6)
val H2GoalLbl = Color(0xFFA37419)
val H2DetailBg = Color(0x24E0A22E)  // rgba(224,162,46,.14)
val H2Danger = Color(0xFF9A2A20)
val H2Ease = CubicBezierEasing(0.2f, 0.75f, 0.2f, 1f)
val H2OutCubic = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
val H2RevealEase = CubicBezierEasing(0.3f, 1.08f, 0.4f, 1f)

private val h2NumberFormat = java.text.NumberFormat.getIntegerInstance(Locale("en", "IN"))

/** 1,900-style grouping exactly as the preview renders it (en-IN). */
fun h2fmt(n: Double): String = h2NumberFormat.format(kotlin.math.round(n).toLong())

@Composable
private fun h2ScreenW(): Int {
    val cfg = LocalConfiguration.current
    return cfg.screenWidthDp.takeIf { it > 0 } ?: 360
}

/** Press scale 0.94 (Part F) driven off the caller's interaction source. */
@Composable
private fun h2PressScale(inter: MutableInteractionSource): Float {
    val pressed by inter.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = 0.6f, stiffness = 900f),
        label = "h2press",
    )
    return scale
}

@Composable
internal fun H2Grab() {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 4.dp)
            .size(40.dp, 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(Color(0x400F5560)),
    )
}

// ---------- Part A: HOME ----------

@Composable
fun HomeScreen2(nav: NavController, vm: AppVm) {
    val home by vm.home.collectAsState()
    val profile by vm.profile.collectAsState()
    LaunchedEffect(Unit) {
        vm.refreshProfile()
        vm.refreshHome()
    }
    // v4 #1: release the launch splash once today's totals are on screen.
    LaunchedEffect(home, profile) {
        if (home != null && profile != null) LaunchState.destinationReady.value = true
    }

    var trendsOpen by remember { mutableStateOf(false) }
    var notesOpen by remember { mutableStateOf(false) }
    var profileOpen by remember { mutableStateOf(false) }

    if (home == null || profile == null) {
        Box(Modifier.fillMaxSize().background(H2Cream), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = H2Teal)
        }
        return
    }
    val h = home!!
    val p = profile!!
    // A6 mood: night sleeps, an empty plate begs, over thinks it over,
    // goal day dances, otherwise a friendly wave.
    val neeluAnim = when {
        h.state == "night" -> "sleep"
        h.kcal_eaten <= 0.0 -> "hungry"
        h.kcal_eaten > h.kcal_target -> "think"
        h.state == "goal" -> "dance"
        else -> "wave"
    }

    // Part G: system back closes the top overlay first, then leaves the app.
    BackHandler(enabled = profileOpen || trendsOpen || notesOpen) {
        when {
            profileOpen -> profileOpen = false
            trendsOpen -> trendsOpen = false
            notesOpen -> notesOpen = false
        }
    }

    var avatarCenter by remember { mutableStateOf(Offset(297f, 58f)) }
    val scope = rememberCoroutineScope()

    // Item 7: content starts at the status-bar inset + 8dp; the day/night
    // gradient draws full-bleed behind the system bars (item 12): 06:00–18:59
    // morning cream, from 19:00 dusk plum. Status-bar icons flip with it.
    val night = rememberIsNight()
    val onBg = if (night) NightOnBackground else DayOnBackground
    MorScreen(dark = night, background = { HomeBackground(night) }) {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(10.dp))
            // A2: top bar, 68 tall, side padding 16.
            Row(
                Modifier.fillMaxWidth().padding(16.dp, 0.dp).height(68.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        LocalDate.now().dayOfWeek
                            .getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH).uppercase(),
                        fontSize = 12.sp, letterSpacing = (12 * 0.08).sp,
                        color = onBg.dayLabel, fontFamily = FontFamily.Default,
                    )
                    Text(
                        greetingFor(), fontSize = 22.sp, lineHeight = (22 * 1.15).sp,
                        color = onBg.greeting, fontFamily = FontFamily.Serif,
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val aInter = remember { MutableInteractionSource() }
                    val aScale = h2PressScale(aInter)
                    Box(
                        Modifier
                            .size(46.dp)
                            .graphicsLayer { scaleX = aScale; scaleY = aScale }
                            .background(H2IconBtnBg, CircleShape)
                            .border(1.dp, H2Border, CircleShape)
                            .onGloballyPositioned { avatarCenter = it.boundsInRoot().center }
                            .clickable(
                                interactionSource = aInter, indication = null,
                            ) { profileOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_person_line), null,
                            tint = H2Teal, modifier = Modifier.size(24.dp),
                        )
                    }
                    val bInter = remember { MutableInteractionSource() }
                    val bScale = h2PressScale(bInter)
                    Box(
                        Modifier
                            .size(46.dp)
                            .graphicsLayer { scaleX = bScale; scaleY = bScale }
                            .background(H2IconBtnBg, CircleShape)
                            .border(1.dp, H2Border, CircleShape)
                            .clickable(
                                interactionSource = bInter, indication = null,
                            ) { notesOpen = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_bell), null,
                            tint = H2Teal, modifier = Modifier.size(22.dp),
                        )
                        if (h.unread > 0) UnreadBadge(h.unread, Modifier.align(Alignment.TopEnd))
                    }
                }
            }

            Bubble(bubbleText(h))

            Tracker(h, neeluAnim)

            // 07-plan-ready-flow item 5: reminders were left off, so after the
            // first logged meal offer them once from here instead of a screen.
            if (h.kcal_eaten > 0 && p.reminders_on != 1) ReminderNudgeCard(vm, p)

            Spacer(Modifier.weight(1f).heightIn(min = 150.dp))

            // A8 week strip data: the app's existing streak API lights the days.
            var weekFlags by remember { mutableStateOf<List<Boolean?>?>(null) }
            LaunchedEffect(h.streak, h.date) {
                scope.launch(Dispatchers.IO) {
                    val flags: List<Boolean?>? = try {
                        val s = vm.api().streak()
                        val byDate = s.days.associateBy { it.date }
                        val monday = LocalDate.now()
                            .with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        (0..6).map { i -> byDate[monday.plusDays(i.toLong()).toString()]?.lit }
                    } catch (_: Exception) { null }
                    withContext(Dispatchers.Main) { weekFlags = flags }
                }
            }
            WeekStrip(h, weekFlags) { trendsOpen = true }

            Spacer(Modifier.height(14.dp))
            ActionsRow(nav, vm, h)
        }

        if (trendsOpen) TrendsSheet(h, vm) { trendsOpen = false }
        if (notesOpen) NotesSheet(nav, vm) { notesOpen = false }
        ProfileOverlay(profileOpen, avatarCenter, nav, vm, h, p) { profileOpen = false }
    }
}

/**
 * 07-plan-ready-flow item 5 — the only place reminders get offered after the
 * "Remind you at mealtimes?" screen was removed. Shows after the first logged
 * meal while reminders are off. Turn on asks for POST_NOTIFICATIONS on API 33+
 * and then enables them (denied → a note with a link to system Settings);
 * Not now hides the card for 3 days, and it never comes back after 2 dismissals.
 */
@Composable
private fun ReminderNudgeCard(vm: AppVm, p: dev.mor.app.data.Profile) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("mor", Context.MODE_PRIVATE) }
    var gone by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    val eligible = remember {
        prefs.getInt("v2_rcard_count", 0) < 2 &&
            System.currentTimeMillis() >= prefs.getLong("v2_rcard_hidden_until", 0L)
    }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            vm.saveProfile(mapOf("reminders_on" to true))
            gone = true
        } else {
            denied = true
        }
    }

    fun dismiss() {
        prefs.edit()
            .putInt("v2_rcard_count", prefs.getInt("v2_rcard_count", 0) + 1)
            .putLong("v2_rcard_hidden_until", System.currentTimeMillis() + 3L * 24 * 60 * 60 * 1000)
            .apply()
        gone = true
    }

    fun turnOn() {
        val hasIt = ContextCompat.checkSelfPermission(
            ctx, Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (hasIt || Build.VERSION.SDK_INT < 33) {
            vm.saveProfile(mapOf("reminders_on" to true))
            gone = true
        } else {
            perm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (gone || !eligible) return

    val times = p.meal_times.orEmpty().values.sorted()
    val body = "We'll remind you at " + when (times.size) {
        0 -> "your meal times"
        1 -> times[0]
        2 -> "${times[0]} and ${times[1]}"
        else -> times.dropLast(1).joinToString(", ") + " and " + times.last()
    } + "."

    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(MorShape.Card))
            .background(MorColor.Card)
            .border(1.dp, MorColor.CardBorder, RoundedCornerShape(MorShape.Card))
            .padding(14.dp, 13.dp),
    ) {
        if (denied) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeeluBitmap("thinking", Modifier.size(30.dp), contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Notifications are off for Mor. You can turn them on in Settings.",
                    fontSize = 13.sp, lineHeight = 18.sp, color = H2Ink,
                    fontFamily = FontFamily.Default,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "Settings", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = H2Teal, fontFamily = FontFamily.Default,
                    modifier = Modifier
                        .clickable {
                            ctx.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName),
                            )
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
                Text(
                    "Got it", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                    color = H2Mute2, fontFamily = FontFamily.Default,
                    modifier = Modifier
                        .clickable { dismiss() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                )
            }
        } else {
            Column {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Want a nudge at your meal times?",
                            fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                            color = H2Ink, fontFamily = FontFamily.Default,
                        )
                        Spacer(Modifier.height(3.dp))
                        Text(
                            body, fontSize = 12.5.sp, lineHeight = 17.sp,
                            color = H2Mute2, fontFamily = FontFamily.Default,
                        )
                    }
                    NeeluBitmap("wave", Modifier.size(30.dp), contentDescription = null)
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MorColor.Maroon)
                            .clickable {
                                MorSound.ensure(ctx)
                                MorSound.select(0)
                                turnOn()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "Turn on", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = MorColor.CreamText, fontFamily = FontFamily.Default,
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        "Not now", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        color = H2Mute2, fontFamily = FontFamily.Default,
                        modifier = Modifier
                            .clickable {
                                MorSound.ensure(ctx)
                                MorSound.select(1)
                                dismiss()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}
private fun bubbleText(h: dev.mor.app.data.Home): String = when {
    h.kcal_eaten <= 0.0 -> "Eaten anything lately? Add a meal."
    h.kcal_eaten > h.kcal_target -> "Over today. Tomorrow's a fresh plate."
    else -> "${h2fmt(h.kcal_target - h.kcal_eaten)} kcal left today. Nice going."
}

/** Red badge, 20 tall, overhanging the bell circle, never clipped (A2). */
@Composable
private fun UnreadBadge(count: Int, modifier: Modifier) {
    Box(
        modifier
            .offset(x = 4.dp, y = (-4).dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(H2Badge)
            .border(2.dp, H2Cream3, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            if (count > 99) "99+" else "$count",
            fontSize = 11.sp, fontWeight = FontWeight.Bold,
            color = Color.White, fontFamily = FontFamily.Default,
        )
    }
}

/** A4: Neelu's speech bubble, max 62% wide, with the small down tail. */
@Composable
private fun Bubble(text: String) {
    val maxW = (h2ScreenW() * 0.62f).dp
    Box(Modifier.padding(start = 16.dp, top = 4.dp)) {
        Box(
            Modifier
                .widthIn(max = maxW)
                .background(H2BubbleBg, RoundedCornerShape(18.dp))
                .padding(14.dp, 10.dp),
        ) {
            AnimatedContent(
                text,
                transitionSpec = {
                    (fadeIn(tween(400, easing = H2Ease)) +
                        slideInVertically(tween(400, easing = H2Ease)) { it / 3 })
                        .togetherWith(fadeOut(tween(150)))
                },
                label = "bubble",
            ) { t ->
                Text(
                    t, fontSize = 14.sp, lineHeight = (14 * 1.35).sp,
                    color = H2Ink2, fontFamily = FontFamily.Default,
                )
            }
        }
        // Tail: a 16x8 triangle 26px from the bubble's left edge, pointing down.
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .offset(x = 18.dp, y = 6.dp)
                .size(16.dp, 8.dp),
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val path = Path()
                path.moveTo(0f, 0f)
                path.lineTo(16.dp.toPx(), 0f)
                path.lineTo(8.dp.toPx(), 8.dp.toPx())
                path.close()
                drawPath(path, H2BubbleBg)
            }
        }
    }
}

/** A5: calorie tracker panel, max 78% wide, with Neelu leaning on its
 *  bottom right — her feet 14dp below the panel's bottom edge (A6). */
@Composable
private fun Tracker(h: dev.mor.app.data.Home, anim: String) {
    val reduced = rememberReducedMotion()
    val fs = if (h2ScreenW() <= 320) 74 else 90
    val target = h.kcal_target.coerceAtLeast(1.0)
    val frac = (h.kcal_eaten / target).coerceIn(0.0, 1.0).toFloat()

    // Count-up ~700ms (A9): the shown number eases from the last painted value.
    var shownK by rememberSaveable { mutableStateOf(h.kcal_eaten) }
    LaunchedEffect(h.kcal_eaten, h.date) {
        val from = shownK
        val to = h.kcal_eaten
        if (reduced || from == to) {
            shownK = to
        } else {
            val anim = Animatable(from.toFloat())
            anim.animateTo(to.toFloat(), tween(700, easing = H2OutCubic)) {
                shownK = value.toDouble()
            }
        }
    }
    val barFrac by animateFloatAsState(frac, tween(700, easing = H2OutCubic), label = "kbar")

    Box(Modifier.padding(start = 16.dp, top = 16.dp)) {
        Column(
            Modifier
                .widthIn(max = (h2ScreenW() * 0.78f).dp)
                .background(H2TrackerBg, RoundedCornerShape(22.dp))
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 14.dp),
        ) {
            Text(
                h2fmt(shownK), fontSize = fs.sp,
                lineHeight = (fs * 0.98f).sp, letterSpacing = (fs * -0.03f).sp,
                color = H2Ink, fontFamily = FontFamily.Serif,
                style = TextStyle(fontFeatureSettings = "tnum"),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (h.kcal_eaten > h.kcal_target)
                    "of ${h2fmt(h.kcal_target)} kcal, over by ${h2fmt(h.kcal_eaten - h.kcal_target)}"
                else
                    "of ${h2fmt(h.kcal_target)} kcal, ${h2fmt(h.kcal_target - h.kcal_eaten)} left",
                fontSize = 14.sp, color = H2Ink2, fontFamily = FontFamily.Default,
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(H2Line),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(barFrac)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(H2Teal),
                )
            }
            Spacer(Modifier.height(8.dp))
            // W2: the second line — one segment per glass goal, then the caption.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(h.water_goal.coerceAtLeast(1)) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(if (i < h.water) H2WaterSeg else H2WaterEmpty),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Water, ${h.water} of ${h.water_goal} glasses",
                fontSize = 12.5.sp, color = H2Ink2, fontFamily = FontFamily.Default,
            )
        }
        // Prototype: .neelu sits in a zero-height anchor right after the card
        // (right:4px, top:-14px, width:184px) so the bird hangs BELOW the card,
        // not inside it. Neelu paints ContentScale.Fit centered in a square box,
        // so y = 78 + 92*aspect puts the painted top 14dp above the card bottom.
        val neeluA = when (poseFor(anim)) {
            "cool" -> 1.132f; "happy" -> 1.003f; "hungry" -> 0.963f
            "love" -> 1.108f; "joy" -> 1.017f; "thinking" -> 0.99f
            else -> 1.05f // sleepy
        }
        Neelu(
            anim, 184.dp,
            Modifier.align(Alignment.BottomEnd)
                .offset(x = (-4).dp, y = (78f + 92f * neeluA).dp),
        )
    }
}

private val WEEK_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

/** A8: S1 — 24 circles, tick fill when done, gold ring today, dashed-faint past. */
@Composable
private fun WeekStrip(
    h: dev.mor.app.data.Home,
    weekFlags: List<Boolean?>?,
    onOpen: () -> Unit,
) {
    val todayIdx = (LocalDate.now().dayOfWeek.value + 6) % 7
    val loggedToday = h.meals.values.any { it.logged }
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(H2WeekBg, RoundedCornerShape(22.dp))
            .clickable(onClick = onOpen)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 8.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "${h.streak} day streak", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    color = H2Ink, fontFamily = FontFamily.Default,
                )
                Spacer(Modifier.weight(1f))
                Text("Trends ›", fontSize = 13.sp, color = H2Teal, fontFamily = FontFamily.Default)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                (0..6).forEach { i ->
                    val flag = weekFlags?.getOrNull(i)
                    val isToday = i == todayIdx
                    val done = if (isToday && flag == null) loggedToday else flag == true
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                            if (done) {
                                Box(
                                    Modifier.fillMaxSize().clip(CircleShape).background(H2Teal),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        painterResource(R.drawable.ic_tick), null,
                                        tint = Color.White, modifier = Modifier.size(13.dp),
                                    )
                                }
                            } else if (isToday) {
                                Box(Modifier.fillMaxSize().border(2.dp, H2Gold, CircleShape))
                            } else {
                                val dashed = i < todayIdx
                                Canvas(
                                    Modifier.fillMaxSize()
                                        .alpha(if (dashed) 0.6f else 1f),
                                ) {
                                    drawCircle(
                                        H2Border,
                                        style = Stroke(
                                            width = 1.5.dp.toPx(),
                                            pathEffect = if (dashed)
                                                PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                                            else null,
                                        ),
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            WEEK_LETTERS[i], fontSize = 12.sp,
                            color = if (isToday) H2Ink else H2Muted,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Default,
                        )
                    }
                }
            }
        }
    }
}

/** A9 v4: the one action pair — water + Add meal, now exactly equal width. */
private fun waterLabel(text: String, size: TextUnit) = buildAnnotatedString {
    withStyle(SpanStyle(color = H2WaterIcon, fontWeight = FontWeight.SemiBold, fontSize = size)) {
        append("+ ")
    }
    withStyle(SpanStyle(color = H2WaterInk, fontWeight = FontWeight.SemiBold, fontSize = size)) {
        append(text)
    }
}

@Composable
private fun ActionsRow(nav: NavController, vm: AppVm, h: dev.mor.app.data.Home) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var addingWater by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        val wInter = remember { MutableInteractionSource() }
        val wScale = h2PressScale(wInter)
        BoxWithConstraints(
            Modifier
                .weight(1f)
                .height(54.dp)
                .graphicsLayer { scaleX = wScale; scaleY = wScale }
                .background(H2Water, RoundedCornerShape(27.dp))
                .border(1.5.dp, H2WaterBorder, RoundedCornerShape(27.dp))
                .clickable(interactionSource = wInter, indication = null) {
                    if (!addingWater) {
                        addingWater = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        scope.launch(Dispatchers.IO) {
                            try {
                                val j = vm.api().water(mapOf("delta" to 1))
                                vm.refreshHome()
                                vm.showSnack("Water logged · ${j.glasses}/${h.water_goal}")
                            } catch (e: Exception) {
                                vm.showSnack("Water failed: ${friendlyErr(e)}", true)
                            }
                            addingWater = false
                        }
                    }
                }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            // v4 #9: auto-size 15 → 13 sp, then the short "+ Add water" label.
            val tm = rememberTextMeasurer()
            val availPx = with(LocalDensity.current) {
                (maxWidth - 20.dp - 26.dp - 5.dp).toPx()
            }
            fun fit(s: AnnotatedString, size: TextUnit) = tm.measure(
                s,
                TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = size),
                maxLines = 1,
            ).size.width <= availPx
            val level = remember(availPx) {
                when {
                    fit(waterLabel("Add glass of water", 15.sp), 15.sp) -> 0
                    fit(waterLabel("Add glass of water", 13.sp), 13.sp) -> 1
                    else -> 2
                }
            }
            val labelSize = if (level == 0) 15.sp else 13.sp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_glass_water), null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(width = 26.dp, height = 33.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    if (level == 2) waterLabel("Add water", 13.sp) else waterLabel("Add glass of water", labelSize),
                    lineHeight = (labelSize.value * 1.12f).sp, maxLines = 1,
                )
            }
        }
        val mInter = remember { MutableInteractionSource() }
        val mScale = h2PressScale(mInter)
        Box(
            Modifier
                .weight(1f)
                .height(54.dp)
                .graphicsLayer { scaleX = mScale; scaleY = mScale }
                .background(H2Teal, RoundedCornerShape(27.dp))
                .clickable(interactionSource = mInter, indication = null) { nav.navigate("add") }
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "+", fontSize = 22.sp, color = H2GoldPlus,
                    fontFamily = FontFamily.Default,
                    modifier = Modifier.offset(y = (-2).dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "Add meal", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = Color.White, fontFamily = FontFamily.Default,
                )
            }
        }
    }
}

// ---------- Part C: TRENDS SHEET ----------

private data class TrendDay(val letter: String, val kcal: Double?, val isToday: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsSheet(h: dev.mor.app.data.Home, vm: AppVm, onClose: () -> Unit) {
    var ins by remember { mutableStateOf<Insights?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try { ins = vm.api().insights("week") } catch (_: Exception) { }
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose, containerColor = H2Cream2,
        dragHandle = { H2Grab() }, sheetState = sheetState,
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 20.dp)) {
            val d = ins
            val today = LocalDate.now()
            val todayIdx = (today.dayOfWeek.value + 6) % 7
            val monday = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val daysByKey = d?.days?.associateBy { it.date } ?: emptyMap()
            val goal = d?.kcal_target?.toDouble() ?: h.kcal_target
            val trend = (0..6).map { i ->
                val dk = daysByKey[monday.plusDays(i.toLong()).toString()]
                val kcal = when {
                    i > todayIdx -> null
                    i < todayIdx -> dk?.kcal ?: 0.0
                    else -> if (dk != null && dk.kcal > 0.0) dk.kcal else h.kcal_eaten
                }
                TrendDay(WEEK_LETTERS[i], kcal, i == todayIdx)
            }
            var sel by remember { mutableStateOf(todayIdx) }
            val max = maxOf(
                goal * 1.15,
                trend.mapNotNull { it.kcal }.maxOrNull() ?: 0.0,
            )

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "This week", fontSize = 24.sp, color = H2Ink,
                    fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f),
                )
                Text(
                    "${h.streak} day streak", fontSize = 13.sp, color = H2Teal,
                    fontFamily = FontFamily.Default,
                )
            }
            Spacer(Modifier.height(14.dp))
            TrendChart(goal, max, trend, sel) { sel = it }
            Spacer(Modifier.height(14.dp))
            val day = trend[sel]
            val dayDate = monday.plusDays(sel.toLong())
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(H2DetailBg, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    dayDate.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH),
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = H2Ink,
                    fontFamily = FontFamily.Default,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    if (day.kcal == null) ""
                    else (if (day.isToday) "Today, " else "") +
                        "${h2fmt(day.kcal!!)} kcal" +
                        if (day.isToday) " · ${h.water} glasses" else "",
                    fontSize = 14.sp, color = H2Ink2, fontFamily = FontFamily.Default,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                StatCell(h2fmt((d?.avg ?: 0).toDouble()), "avg kcal", Modifier.weight(1f))
                StatCell("${d?.logged_days ?: 0}", "logged days", Modifier.weight(1f))
                StatCell("${h.streak}", "day streak", Modifier.weight(1f))
            }
            Spacer(Modifier.height(14.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .border(1.5.dp, Color(0x590F5560), RoundedCornerShape(25.dp))
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Close", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = H2Teal, fontFamily = FontFamily.Default,
                )
            }
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 26.sp, color = H2Ink, fontFamily = FontFamily.Serif)
        Text(label, fontSize = 12.sp, color = H2Muted, fontFamily = FontFamily.Default)
    }
}

@Composable
private fun TrendChart(
    goal: Double,
    max: Double,
    days: List<TrendDay>,
    sel: Int,
    onSel: (Int) -> Unit,
) {
    val chartH = if (h2ScreenW() <= 320) 116.dp else 150.dp
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    Box(Modifier.fillMaxWidth().height(chartH)) {
        Row(Modifier.fillMaxSize()) {
            days.forEachIndexed { i, day ->
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(H2Track)
                            .border(2.dp, if (i == sel) H2Ink else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable(
                                enabled = day.kcal != null,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSel(i) },
                    ) {
                        if (day.kcal != null) {
                            val target = (day.kcal / max).toFloat().coerceIn(0f, 1f)
                            val f by animateFloatAsState(
                                if (shown) target else 0f,
                                tween(600, easing = H2Ease), label = "bar",
                            )
                            Box(
                                Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .fillMaxHeight(f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (day.isToday) H2Gold else H2WaterSeg),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        day.letter, fontSize = 12.sp,
                        color = if (day.isToday) H2Ink else H2Muted,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Default,
                    )
                }
            }
        }
        // Dashed goal line + the goal number label (right aligned, above the line).
        Canvas(Modifier.fillMaxSize()) {
            val letterZone = 26.dp.toPx()
            val trackH = size.height - letterZone
            val frac = (goal / max).toFloat().coerceIn(0.02f, 1f)
            val y = letterZone + trackH * (1f - frac)
            drawLine(
                H2Gold, Offset(0f, y), Offset(size.width, y),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            )
            drawIntoCanvas { c ->
                val paint = android.graphics.Paint().apply {
                    color = 0xFFA37419.toInt()
                    textSize = 10.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
                c.nativeCanvas.drawText(h2fmt(goal), size.width, y - 6.dp.toPx(), paint)
            }
        }
    }
}

// ---------- NOTIFICATIONS SHEET ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesSheet(nav: NavController, vm: AppVm, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Notif>?>(null) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            items = try { vm.api().notifications().items } catch (_: Exception) { emptyList() }
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose, containerColor = H2Cream2,
        dragHandle = { H2Grab() }, sheetState = sheetState,
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 18.dp, end = 18.dp, bottom = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Notifications", fontSize = 24.sp, color = H2Ink,
                    fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0x140F5560))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_close_x), null,
                        tint = H2Ink, modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            val list = items
            if (list == null) {
                Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = H2Teal)
                }
            } else if (list.isEmpty()) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Neelu("sleep", 120.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "All caught up", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        color = H2Ink, fontFamily = FontFamily.Default,
                    )
                    Text(
                        "No new reminders. Neelu is napping.", fontSize = 13.sp,
                        color = H2Muted, fontFamily = FontFamily.Default,
                    )
                }
            } else {
                Column {
                    list.forEach { n ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch(Dispatchers.IO) {
                                        try { vm.api().markRead(mapOf("id" to n.id)) } catch (_: Exception) { }
                                        vm.refreshHome()
                                        withContext(Dispatchers.Main) {
                                            onClose()
                                            nav.navigate(when (n.type) {
                                                "badge" -> "challenges"
                                                "missed_meal" -> "add"
                                                else -> "home"
                                            })
                                        }
                                    }
                                }
                                .padding(vertical = 14.dp)
                                .drawBehind {
                                    drawLine(
                                        H2Hair, Offset(0f, size.height),
                                        Offset(size.width, size.height), strokeWidth = 1.dp.toPx(),
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(H2Mint),
                                contentAlignment = Alignment.Center,
                            ) {
                                val (res, w, hgt) = notifIcon(n.type)
                                Icon(
                                    painterResource(res), null, tint = H2Teal,
                                    modifier = Modifier.size(width = w, height = hgt),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        n.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                        color = H2Ink, fontFamily = FontFamily.Default,
                                    )
                                    if (!n.read) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(Modifier.size(8.dp).clip(CircleShape).background(H2Badge))
                                    }
                                }
                                Text(
                                    n.body, fontSize = 13.sp, color = H2Ink2,
                                    fontFamily = FontFamily.Default,
                                )
                                Text(
                                    timeAgo(n.created_at), fontSize = 12.sp, color = H2Muted,
                                    fontFamily = FontFamily.Default,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun notifIcon(type: String): Triple<Int, androidx.compose.ui.unit.Dp, androidx.compose.ui.unit.Dp> =
    when (type) {
        "water" -> Triple(R.drawable.ic_glass_water, 15.dp, 19.dp)
        "streak" -> Triple(R.drawable.ic_tick, 18.dp, 18.dp)
        else -> Triple(R.drawable.ic_bell, 18.dp, 18.dp)
    }

// ---------- Part B: PROFILE (circle reveal from the avatar) ----------

/** Grows an oval from the avatar button until it covers the screen (B2). */
private class CircleReveal(
    private val center: Offset,
    private val frac: Float,
    private val maxR: Float,
) : Shape {
    override fun createOutline(size: Size, direction: LayoutDirection, density: Density): Outline {
        val path = Path()
        if (frac > 0.002f) {
            val r = maxR * frac
            path.addOval(Rect(center - Offset(r, r), Size(r * 2f, r * 2f)))
        }
        return Outline.Generic(path)
    }
}

@Composable
fun ProfileOverlay(
    open: Boolean,
    origin: Offset,
    nav: NavController,
    vm: AppVm,
    h: dev.mor.app.data.Home,
    p: Profile,
    onClose: () -> Unit,
) {
    val reduced = rememberReducedMotion()
    val reveal = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    var maxR by remember { mutableStateOf(1f) }
    LaunchedEffect(open) {
        if (open) {
            closing = false
            if (reduced) reveal.snapTo(1f)
            else reveal.animateTo(1f, tween(600, easing = H2RevealEase))
        } else if (reveal.value > 0f) {
            closing = true
            if (reduced) reveal.snapTo(0f)
            else reveal.animateTo(0f, tween(600, easing = H2RevealEase))
            closing = false
        }
    }
    if (open || closing) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val wPx = with(density) { maxWidth.toPx() }
            val hPx = with(density) { maxHeight.toPx() }
            LaunchedEffect(origin, wPx, hPx) {
                val dx = maxOf(origin.x, wPx - origin.x)
                val dy = maxOf(origin.y, hPx - origin.y)
                maxR = sqrt(dx * dx + dy * dy)
            }
            val shape = CircleReveal(origin, reveal.value, maxR)
            Box(
                Modifier
                    .fillMaxSize()
                    .then(if (reduced) Modifier.alpha(reveal.value) else Modifier.clip(shape)),
            ) {
                ProfileContent(nav, vm, h, p, onClose)
            }
        }
    }
}

@Composable
private fun ProfileContent(
    nav: NavController,
    vm: AppVm,
    h: dev.mor.app.data.Home,
    p: Profile,
    onClose: () -> Unit,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(H2Cream3)
            .statusBarsPadding(),
    ) {
        // B3: back arrow left, title "Profile", no close X.
        Row(
            Modifier.fillMaxWidth().padding(16.dp, 14.dp, 16.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.ArrowBack, null, tint = H2Ink, modifier = Modifier.size(22.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Profile", fontSize = 21.sp, color = H2Ink,
                    fontFamily = FontFamily.Serif, fontWeight = FontWeight.Normal,
                )
            }
            Spacer(Modifier.width(42.dp))
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp)
                .navigationBarsPadding(),
        ) {
            // B4 header: 64 avatar, name, real goal line.
            Row(Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(64.dp).clip(CircleShape).background(H2Mint),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_person_line), null,
                        tint = H2Teal, modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        p.name ?: "You", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        color = H2Ink, fontFamily = FontFamily.Default,
                    )
                    Text(
                        profileGoalLine(p, h), fontSize = 13.sp, color = H2Muted,
                        fontFamily = FontFamily.Default,
                    )
                }
            }

            // B5: the app's existing profile items, grouped under headings.
            val plan = p.plan
            val kcal = plan?.kcal_target?.toDouble() ?: h.kcal_target
            PHeading("YOUR PLAN")
            PRow("Goal", value = if (plan != null)
                "${fmtKg(p.weight_kg)} → ${fmtKg(p.target_weight_kg)} kg" else "—") { nav.navigate("weight") }
            PRow("Daily target", value = "${h2fmt(kcal)} kcal")
            PRow("Diet", value = dietDisplay(p.diet))

            PHeading("REMINDERS")
            PRow(
                "Reminders",
                value = if (p.reminders_on == 1) "${p.meal_times?.size ?: 0} on" else "Off",
            ) { nav.navigate("reminders") }

            PHeading("APP")
            PRow("Units", value = if (p.units == "kg-ft") "kg · ft" else p.units ?: "—")

            PHeading("ACCOUNT")
            PRow("Export my data", sub = "A copy of your meals and plan") {
                scope.launch(Dispatchers.IO) {
                    try {
                        val prof = vm.api().profile()
                        val insD = vm.api().insights()
                        val not = vm.api().notifications()
                        val json = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                            .toJson(mapOf("profile" to prof, "insights" to insD, "notifications" to not))
                        withContext(Dispatchers.Main) {
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_SUBJECT, "mor-data-export.json")
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            ctx.startActivity(Intent.createChooser(send, "Export Mor data"))
                            vm.showSnack("Data exported")
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            vm.showSnack("Export failed: ${friendlyErr(e)}", true)
                        }
                    }
                }
            }
            PRow("Delete my data", sub = "Removes your account and everything in it", danger = true) {
                confirmDelete = true
            }

            Text(
                "Mor v0.1 · android build", fontSize = 12.sp, color = H2Muted,
                fontFamily = FontFamily.Default, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 8.dp),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = H2Cream2,
            title = { Text("Delete ALL your data?", color = H2Ink, fontFamily = FontFamily.Default) },
            text = { Text("This cannot be undone.", color = H2Ink2, fontFamily = FontFamily.Default) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch(Dispatchers.IO) {
                        try { vm.api().reset() } catch (_: Exception) { }
                        withContext(Dispatchers.Main) {
                            vm.showSnack("Data cleared")
                            V2Draft(ctx).clear()
                            vm.refreshProfile()
                            vm.refreshHome()
                            nav.navigate("v2") { popUpTo(0) }
                        }
                    }
                }) { Text("Delete", color = H2Danger, fontFamily = FontFamily.Default) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Keep", color = H2Teal, fontFamily = FontFamily.Default)
                }
            },
        )
    }
}

private fun fmtKg(v: Double?): String =
    if (v == null) "—"
    else if (v % 1.0 == 0.0) "${v.roundToInt()}" else "${(v * 10).roundToInt() / 10.0}"

private fun profileGoalLine(p: Profile, h: dev.mor.app.data.Home): String {
    val label = goalDisplay(p.goal)
    val kcal = p.plan?.kcal_target?.toDouble() ?: h.kcal_target
    return buildString {
        if (label.isNotBlank()) append("$label, ")
        append("${h2fmt(kcal)} kcal a day")
    }
}

@Composable
private fun PHeading(text: String) {
    Text(
        text, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
        letterSpacing = (11.5 * 0.14).sp, color = H2Muted,
        fontFamily = FontFamily.Default,
        modifier = Modifier.padding(top = 22.dp, bottom = 2.dp),
    )
}

@Composable
private fun PRow(
    title: String,
    sub: String? = null,
    value: String? = null,
    danger: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .drawBehind {
                drawLine(
                    H2Hair, Offset(0f, size.height), Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = if (danger) H2Badge else H2Ink, fontFamily = FontFamily.Default,
            )
            if (sub != null) {
                Text(
                    sub, fontSize = 12.5.sp, color = H2Muted, lineHeight = (12.5 * 1.35).sp,
                    fontFamily = FontFamily.Default,
                )
            }
        }
        if (value != null) {
            Text(value, fontSize = 14.sp, color = H2Ink2, fontFamily = FontFamily.Default)
            Spacer(Modifier.width(4.dp))
        }
        if (onClick != null) {
            Icon(painterResource(R.drawable.ic_chev), null, tint = H2Ink2, modifier = Modifier.size(14.dp))
        }
    }
}
