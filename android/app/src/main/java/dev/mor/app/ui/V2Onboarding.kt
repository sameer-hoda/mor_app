@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package dev.mor.app.ui

import android.Manifest
import android.content.Context
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import dev.mor.app.data.AppVm
import dev.mor.app.data.Plan
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.roundToInt

/** Phase A: first-time onboarding v2, ported natively from morzip.js. */

private val V2_STEPN = mapOf("goal" to 1, "diet" to 2, "body" to 3, "target" to 4, "activity" to 5, "meals" to 6)
private val V2_NEELU = mapOf(
    "welcome" to "happy", "meet" to "wave", "goal" to "happy", "diet" to "hungry",
    "body" to "cool", "target" to "thinking", "activity" to "wave", "meals" to "hungry",
    "building" to "thinking", "plan" to "joy", "notify" to "wave", "splash" to "joy"
)
private val V2_QUICK = mapOf(
    "breakfast" to listOf("07:30", "08:00", "08:30", "09:00"),
    "lunch" to listOf("12:30", "13:00", "13:30", "14:00"),
    "snack" to listOf("16:00", "16:30", "17:00", "18:00"),
    "dinner" to listOf("20:00", "20:30", "21:00", "21:30")
)

data class V2Answers(
    val goal: String = "",
    val diet: String = "",
    val sex: String = "",
    val age: Int = 30,
    val heightCm: Int = 170,
    val weightKg: Float = 72f,
    val targetKg: Float = 66f,
    val activity: String = "light",
    val times: Map<String, String> = mapOf(
        "breakfast" to "08:30", "lunch" to "13:30", "snack" to "17:00", "dinner" to "21:00"
    ),
    val mealOn: Map<String, Boolean> = mapOf(
        "breakfast" to true, "lunch" to true, "snack" to true, "dinner" to true
    )
)

class V2Draft(private val ctx: Context) {
    var answers by mutableStateOf(V2Answers())
    var confettiShown by mutableStateOf(false)
    var soundOn by mutableStateOf(!MorSound.isMuted(ctx))
    var stepKey by mutableStateOf("splash")

    fun save() {
        runCatching {
            val a = answers
            val j = org.json.JSONObject()
                .put("step", stepKey)
                .put("goal", a.goal).put("diet", a.diet).put("sex", a.sex).put("age", a.age)
                .put("height_cm", a.heightCm).put("weight_kg", a.weightKg)
                .put("target_weight_kg", a.targetKg).put("activity", a.activity)
                .put("times", org.json.JSONObject(a.times))
                .put("mealOn", org.json.JSONObject(a.mealOn))
            ctx.getSharedPreferences("mor", Context.MODE_PRIVATE).edit()
                .putString("v2draft", j.toString())
                .putBoolean("v2confetti", confettiShown)
                .apply()
        }
    }

    fun load() {
        runCatching {
            val s = ctx.getSharedPreferences("mor", Context.MODE_PRIVATE).getString("v2draft", null)
                ?: return
            val j = org.json.JSONObject(s)
            val times = V2Answers().times.toMutableMap()
            val jt = j.optJSONObject("times")
            if (jt != null) jt.keys().forEach { k -> times[k] = jt.optString(k) }
            val mealOn = V2Answers().mealOn.toMutableMap()
            val jo = j.optJSONObject("mealOn")
            if (jo != null) jo.keys().forEach { k -> mealOn[k] = jo.optBoolean(k, true) }
            answers = V2Answers(
                goal = j.optString("goal", ""), diet = j.optString("diet", ""),
                sex = j.optString("sex", ""), age = j.optInt("age", 30),
                heightCm = j.optInt("height_cm", 170),
                weightKg = j.optDouble("weight_kg", 72.0).toFloat(),
                targetKg = j.optDouble("target_weight_kg", 66.0).toFloat(),
                activity = j.optString("activity", "light"),
                times = times, mealOn = mealOn
            )
            confettiShown = ctx.getSharedPreferences("mor", Context.MODE_PRIVATE)
                .getBoolean("v2confetti", false)
            stepKey = j.optString("step", "splash")
        }
    }

    fun clear() {
        ctx.getSharedPreferences("mor", Context.MODE_PRIVATE).edit()
            .remove("v2draft").remove("v2confetti").apply()
    }
}

/* motion helpers ------------------------------------------------------ */

private val ExitEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
private val MacroEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

private fun fmtKg(v: Float): String =
    if (abs(v % 1f) < 0.05f) v.roundToInt().toString() else "%.1f".format(v)

private fun kgStr(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()

@Composable
private fun rememberHaptics(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) } }
}

private fun Modifier.v2Stagger(order: Int, startMs: Int = 300, gapMs: Int = 50): Modifier = composed {
    val a = remember { Animatable(0f) }
    val reduced = rememberReducedMotion()
    LaunchedEffect(Unit) {
        delay((startMs + order * gapMs).toLong())
        if (reduced) a.animateTo(1f, tween(160, easing = LinearEasing))
        else a.animateTo(1f, M.RiseSpring)
    }
    this.then(
        Modifier.graphicsLayer {
            alpha = a.value
            translationY = (1f - a.value) * 20.dp.toPx()
        }
    )
}

private fun Modifier.v2Bob(): Modifier = composed {
    if (rememberReducedMotion()) {
        this
    } else {
        val inf = rememberInfiniteTransition(label = "bob")
        val t by inf.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
            label = "bobT"
        )
        val density = LocalDensity.current
        this.then(
            Modifier.graphicsLayer {
                val eased = if (t < 0.5f) 2f * t * t else 1f - (-2f * t + 2f) * (-2f * t + 2f) / 2f
                translationY = with(density) { -(eased * 6).dp.toPx() }
            }
        )
    }
}

/* shared scaffolding -------------------------------------------------- */

@Composable
private fun GuideNeelu(step: String, size: Dp, bob: Boolean = true) {
    NeeluBitmap(
        V2_NEELU[step] ?: "happy",
        if (bob) Modifier.size(size).v2Bob() else Modifier.size(size),
        contentDescription = null
    )
}

@Composable
private fun V2Top(step: String, onBack: (() -> Unit)?, draft: V2Draft) {
    val n = V2_STEPN[step]
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp).height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconCapsule { MorSound.back(); onBack() }
        } else {
            Spacer(Modifier.width(44.dp))
        }
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
            if (n != null) {
                (1..6).forEach { i ->
                    val cur = i == n
                    val w by animateDpAsState(if (cur) 22.dp else 8.dp, tween(300), label = "dot")
                    Box(
                        Modifier.height(8.dp).width(w)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i <= n) Teal else Line2)
                    )
                    if (i < 6) Spacer(Modifier.width(6.dp))
                }
            }
        }
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.75f))
                .clickable {
                    MorSound.ensure(ctx)
                    val m = MorSound.toggleMute(ctx)
                    draft.soundOn = !m
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(20.dp)) {
                val ink = Color(0xFF142027)
                val c = center
                val p = density
                fun rel(dx: Float, dy: Float) = Offset(c.x + dx * p, c.y + dy * p)
                drawLine(ink, rel(-9f, -3f), rel(-9f, 3f), 1.4.dp.toPx())
                drawLine(ink, rel(-4f, -6f), rel(-4f, 6f), 1.4.dp.toPx())
                drawLine(ink, rel(-9f, -3f), rel(-4f, -3f), 1.4.dp.toPx())
                drawLine(ink, rel(-9f, 3f), rel(-4f, 3f), 1.4.dp.toPx())
                if (draft.soundOn) {
                    drawArc(ink, -55f, 110f, false, topLeft = rel(-1f, -6f), size = Size(7f * p, 12.dp.toPx()), style = Stroke(1.6.dp.toPx()))
                    drawArc(ink, -55f, 110f, false, topLeft = rel(-1f, -9f), size = Size(13f * p, 18.dp.toPx()), style = Stroke(1.6.dp.toPx()))
                } else {
                    drawLine(ink, rel(0f, -4f), rel(7f, 4f), 1.6.dp.toPx())
                    drawLine(ink, rel(7f, -4f), rel(0f, 4f), 1.6.dp.toPx())
                }
            }
        }
    }
}

@Composable
private fun IconCapsule(onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .background(Color.White.copy(alpha = 0.75f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(22.dp)) {
            val ink = Color(0xFF142027)
            val c = center
            val p = density
            fun rel(dx: Float, dy: Float) = Offset(c.x + dx * p, c.y + dy * p)
            drawLine(ink, rel(2f, -7f), rel(-4f, 0f), 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(ink, rel(-4f, 0f), rel(2f, 7f), 2.2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

/** Speech bubble: pops in (bubble spring), title rises out of a 5px blur,
 *  subline follows one beat later (rise spring + 150ms). */
@Composable
private fun BubbleHeading(title: String, sub: String?, eyebrow: String?, center: Boolean) {
    val reduced = rememberReducedMotion()
    val bubble = remember { Animatable(if (reduced) 1f else 0f) }
    val head = remember { Animatable(if (reduced) 1f else 0f) }
    val subA = remember { Animatable(if (reduced || sub == null) 1f else 0f) }
    LaunchedEffect(title) {
        if (!reduced) {
            bubble.animateTo(1f, M.BubbleSpring)
            head.animateTo(1f, M.RiseSpring)
            if (sub != null) {
                delay(150)
                subA.animateTo(1f, M.RiseSpring)
            }
        }
    }
    Column(
        Modifier
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = TealDark.copy(alpha = 0.10f))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 13.dp)
            .graphicsLayer {
                val s = 0.86f + 0.14f * bubble.value
                scaleX = s; scaleY = s
                alpha = bubble.value
            },
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start
    ) {
        if (eyebrow != null) {
            Text(
                eyebrow, fontFamily = MorBody, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold,
                color = Sub, letterSpacing = 0.9.sp
            )
        }
        Text(
            title, fontFamily = MorDisplay, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
            color = Ink, lineHeight = 26.5.sp,
            textAlign = if (center) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.graphicsLayer {
                alpha = head.value
                translationY = (1f - head.value) * 10.dp.toPx()
            }.blur(if (reduced) 0.dp else ((1f - head.value) * 5).dp)
        )
        if (sub != null) {
            Text(
                sub, fontFamily = MorBody, fontSize = 14.5.sp, color = Sub, lineHeight = 20.sp,
                textAlign = if (center) TextAlign.Center else TextAlign.Start,
                modifier = Modifier.padding(top = 4.dp).graphicsLayer {
                    alpha = subA.value
                    translationY = (1f - subA.value) * 6.dp.toPx()
                }
            )
        }
    }
}

/** Standard step screen: top bar, guide (Neelu 104 + bubble), body, CTA. */
@Composable
private fun V2Step(
    step: String, title: String, sub: String?, draft: V2Draft,
    onBack: (() -> Unit)?, cta: String = "Continue", enabled: Boolean = true,
    onNext: () -> Unit,
    body: @Composable ColumnScope.() -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        V2Top(step, onBack, draft)
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(Modifier.align(Alignment.Bottom)) { GuideNeelu(step, 104.dp) }
            Box(Modifier.weight(1f)) {
                BubbleHeading(
                    title, sub,
                    if (V2_STEPN.containsKey(step)) "STEP ${V2_STEPN[step]} OF 6" else null,
                    center = false
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().weight(1f)
                .padding(start = 20.dp, end = 20.dp, top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) { body() }
        Column(
            Modifier.fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 10.dp)
                .navigationBarsPadding().padding(bottom = 22.dp)
        ) {
            V2Button(cta, enabled, onClick = onNext)
        }
    }
}

@Composable
private fun V2Button(
    text: String, enabled: Boolean = true, secondary: Boolean = false, onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        border = if (secondary) BorderStroke(2.dp, Teal) else null,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) Color.White else Teal,
            contentColor = if (secondary) Teal else Color.White,
            disabledContainerColor = Disabled,
            disabledContentColor = Color(0xFF5A564D)
        ),
        modifier = Modifier.fillMaxWidth().height(if (secondary) 52.dp else 56.dp)
    ) {
        Text(
            text, fontFamily = MorBody,
            fontSize = if (secondary) 16.sp else 18.sp, fontWeight = FontWeight.SemiBold
        )
    }
}

/* choice pieces -------------------------------------------------------- */

private data class V2Opt(val v: String, val label: String, val sub: String? = null, val mark: Color? = null, val bars: Int = 0)

@Composable
private fun Choicex(opts: List<V2Opt>, sel: String, onPick: (String, Int) -> Unit, grid: Boolean) {
    if (grid) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            opts.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { o ->
                        Box(Modifier.weight(1f)) {
                            OptCard(o, sel == o.v, grid = true) { onPick(o.v, opts.indexOf(o)) }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            opts.forEach { o -> OptCard(o, sel == o.v, grid = false) { onPick(o.v, opts.indexOf(o)) } }
        }
    }
}

@Composable
private fun OptCard(o: V2Opt, on: Boolean, grid: Boolean, onPick: () -> Unit) {
    val view = LocalView.current
    val h = rememberHaptics()
    val pop = remember { Animatable(1f) }
    LaunchedEffect(on) {
        if (on) {
            pop.snapTo(1.04f)
            pop.animateTo(1f, tween(180))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .height(if (grid) 96.dp else 64.dp)
            .graphicsLayer {
                val s = pop.value
                scaleX = s; scaleY = s
            }
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Ink.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(20.dp))
            .background(if (on) Mint else Color.White)
            .border(2.dp, if (on) Teal else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable {
                MorSound.ensure(view.context)
                h()
                onPick()
            }
            .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = if (grid) Alignment.Top else Alignment.CenterVertically
    ) {
        LensRadio(on)
        Column(Modifier.weight(1f)) {
            Text(o.label, fontFamily = MorBody, fontSize = 16.5.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            if (o.sub != null) {
                Text(
                    o.sub, fontFamily = MorBody, fontSize = 13.5.sp, color = Sub,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
        if (o.mark != null) VegMark(o.mark)
        if (o.bars > 0) BarsGlyph(o.bars)
    }
}

@Composable
private fun LensRadio(on: Boolean) {
    if (on) {
        Canvas(Modifier.size(16.dp, 20.dp)) {
            drawOval(Color(0xFF19A39A), Offset(center.x - 6.dp.toPx(), center.y - 8.6.dp.toPx()), Size(12.dp.toPx(), 17.2.dp.toPx()))
            drawOval(Color(0xFF1F4FB5), Offset(center.x - 3.4.dp.toPx(), center.y - 4.8.dp.toPx()), Size(6.8.dp.toPx(), 9.6.dp.toPx()))
            drawOval(Color(0xFF142027), Offset(center.x - 1.5.dp.toPx(), center.y - 2.1.dp.toPx()), Size(3.dp.toPx(), 4.2.dp.toPx()))
        }
    } else {
        Box(Modifier.size(26.dp).clip(CircleShape).border(2.dp, Line2, CircleShape).background(Color.White))
    }
}

@Composable
private fun VegMark(color: Color) {
    Box(Modifier.size(20.dp).border(2.dp, color, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun BarsGlyph(n: Int) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..4).forEach { i ->
            Box(
                Modifier.width(5.dp).height((6 + i * 4).dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (i <= n) Teal else Line2)
            )
        }
    }
}

/* wheel picker (snap column, tilt, tick, settle) ------------------------ */

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Wheel(
    label: String, values: List<Float>, initial: Float,
    format: (Float) -> String, unit: String?, sub: ((Float) -> String)?,
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit
) {
    val itemDp = 46.dp
    val itemPx = with(LocalDensity.current) { itemDp.toPx() }
    val padPx = (with(LocalDensity.current) { 92.dp.toPx() })
    val padDp = 92.dp
    val list = rememberLazyListState()
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val idx0 = values.indexOf(initial).coerceAtLeast(0)
    var currentIdx by remember { mutableStateOf(idx0) }
    var lastTick by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) { list.scrollToItem(idx0 + 1, -padPx.roundToInt()) }

    LaunchedEffect(list) {
        snapshotFlow { list.layoutInfo }
            .map { info ->
                val center = info.viewportSize.height / 2f
                val row = info.visibleItemsInfo.firstOrNull {
                    center >= it.offset && center < it.offset + it.size
                }
                (row?.index ?: idx0 + 1) - 1
            }
            .distinctUntilChanged()
            .collectLatest { v ->
                if (v in values.indices && v != currentIdx) {
                    currentIdx = v
                    onChange(values[v])
                    val n = System.currentTimeMillis()
                    if (n - lastTick > 45) { MorSound.tickWheel(); lastTick = n }
                }
            }
    }
    LaunchedEffect(list) {
        snapshotFlow { list.isScrollInProgress }
            .collectLatest { scrolling ->
                if (!scrolling) {
                    delay(140)
                    MorSound.settle()
                }
            }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontFamily = MorBody, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
        Box(Modifier.fillMaxWidth().height(230.dp)) {
            Box(
                Modifier.align(Alignment.Center).fillMaxWidth().height(46.dp)
                    .background(Pankh.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            )
            Box(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().height(56.dp)
                    .background(Brush.verticalGradient(listOf(Chandan, Chandan.copy(alpha = 0f))))
            )
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(56.dp)
                    .background(Brush.verticalGradient(listOf(Chandan.copy(alpha = 0f), Chandan)))
            )
            LazyColumn(
                state = list,
                flingBehavior = rememberSnapFlingBehavior(lazyListState = list),
                modifier = Modifier.fillMaxSize()
            ) {
                item(key = 0) { Spacer(Modifier.height(padDp)) }
                itemsIndexed(values, key = { i, _ -> i + 1 }) { i, v ->
                    val info = list.layoutInfo
                    val center = info.viewportSize.height / 2f
                    val row = info.visibleItemsInfo.firstOrNull {
                        center >= it.offset && center < it.offset + it.size
                    }
                    val centeredIdx = (row?.index ?: i + 1) - 1
                    val d = i - centeredIdx
                    Box(
                        Modifier
                            .fillMaxWidth().height(itemDp)
                            .graphicsLayer {
                                rotationX = -(d * 30f).coerceIn(-30f, 30f)
                                alpha = 1f - 0.55f * abs(d).coerceAtMost(1)
                                cameraDistance = 8f * density
                            }
                            .clickable {
                                MorSound.ensure(view.context)
                                MorSound.select(1)
                                scope.launch { list.animateScrollToItem(i + 1, -padPx.roundToInt()) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            format(v),
                            fontFamily = MorBody,
                            fontSize = if (abs(d) < 0.5f) 23.sp else 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (abs(d) < 0.5f) Teal else Sub
                        )
                    }
                }
                item(key = values.size + 1) { Spacer(Modifier.height(padDp)) }
            }
        }
        Text(unit ?: "", fontFamily = MorBody, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Sub, modifier = Modifier.height(15.dp))
        if (sub != null) {
            Text(
                sub(values[currentIdx]),
                fontFamily = MorBody, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Sub
            )
        }
    }
}

/* times ---------------------------------------------------------------- */

@Composable
private fun TimeSheet(
    slot: String, initial: String,
    onSave: (String) -> Unit, onDismiss: () -> Unit
) {
    var pick by remember { mutableStateOf(initial) }
    val st = rememberTimePickerState(
        initialHour = initial.substringBefore(":").toIntOrNull() ?: 8,
        initialMinute = initial.substringAfter(":").toIntOrNull() ?: 30,
        is24Hour = true
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Chandan) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                .navigationBarsPadding().padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                    .background(Line2)
            )
            Text(
                "Edit $slot", fontFamily = MorDisplay, fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold, color = Ink,
                modifier = Modifier.align(Alignment.Start).padding(top = 10.dp)
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.Start)
            ) {
                (V2_QUICK[slot] ?: emptyList()).forEach { t ->
                    TimeChip(t, t == pick) { pick = t }
                }
            }
            Spacer(Modifier.height(12.dp))
            TimePicker(state = st)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onSave("%02d:%02d".format(st.hour, st.minute))
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save", fontFamily = MorBody, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun TimeChip(text: String, on: Boolean, onClick: () -> Unit) {
    val view = LocalView.current
    Box(
        Modifier
            .height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (on) Teal else Color.White)
            .border(1.5.dp, if (on) Teal else Line2, RoundedCornerShape(20.dp))
            .clickable { MorSound.ensure(view.context); MorSound.select(1); onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text, fontFamily = MorBody, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (on) Color.White else Ink
        )
    }
}

/* body / target / meals bodies ------------------------------------------ */

@Composable
private fun BodyBody(draft: V2Draft) {
    val a = draft.answers
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SexCard("female", "Female", a.sex == "female", modifier = Modifier.weight(1f)) {
                draft.answers = draft.answers.copy(sex = "female"); draft.save()
            }
            SexCard("male", "Male", a.sex == "male", modifier = Modifier.weight(1f)) {
                draft.answers = draft.answers.copy(sex = "male"); draft.save()
            }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.5.dp, if (a.sex == "other") Teal else Line2, RoundedCornerShape(50))
                    .background(if (a.sex == "other") Mint else Color.Transparent)
                    .clickable {
                        MorSound.select(2)
                        draft.answers = draft.answers.copy(sex = "other"); draft.save()
                    }
                    .padding(horizontal = 22.dp, vertical = 7.dp)
            ) {
                Text(
                    "Other", fontFamily = MorBody, fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (a.sex == "other") Teal else Sub
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Wheel(
                "Age", (18..100).map { it.toFloat() }, a.age.toFloat(),
                format = { "${it.roundToInt()}" }, unit = "", sub = null,
                modifier = Modifier.weight(1f)
            ) { v -> draft.answers = draft.answers.copy(age = v.roundToInt()); draft.save() }
            Wheel(
                "Height", (130..210).map { it.toFloat() }, a.heightCm.toFloat(),
                format = { "${it.roundToInt()}" }, unit = "cm",
                sub = { v ->
                    val ft = (v / 30.48).toInt()
                    val inch = Math.round(v / 2.54 - ft * 12).toInt()
                    "$ft′$inch″"
                },
                modifier = Modifier.weight(1f)
            ) { v -> draft.answers = draft.answers.copy(heightCm = v.roundToInt()); draft.save() }
            Wheel(
                "Weight", (70..300).map { it / 2f }, a.weightKg,
                format = { fmtKg(it) }, unit = "kg", sub = null,
                modifier = Modifier.weight(1f)
            ) { v -> draft.answers = draft.answers.copy(weightKg = v); draft.save() }
        }
    }
}

@Composable
private fun SexCard(
    kind: String, label: String, on: Boolean,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    val view = LocalView.current
    val h = rememberHaptics()
    Column(
        modifier
            .shadow(2.dp, RoundedCornerShape(20.dp), spotColor = Ink.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(20.dp))
            .background(if (on) Mint else Color.White)
            .border(2.dp, if (on) Teal else Color.Transparent, RoundedCornerShape(20.dp))
            .clickable {
                MorSound.ensure(view.context)
                MorSound.select(if (kind == "female") 0 else 1)
                h()
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(Modifier.size(34.dp)) {
            val ink = if (on) Teal else Color(0xFF8A94A0)
            val sw = 2.4.dp.toPx()
            if (kind == "female") {
                drawCircle(ink, 8.dp.toPx(), center, style = Stroke(sw))
                drawLine(ink, Offset(center.x, center.y + 8.dp.toPx()), Offset(center.x, center.y + 17.dp.toPx()), sw, cap = StrokeCap.Round)
                drawLine(ink, Offset(center.x - 4.5.dp.toPx(), center.y + 13.dp.toPx()), Offset(center.x + 4.5.dp.toPx(), center.y + 13.dp.toPx()), sw, cap = StrokeCap.Round)
            } else {
                drawCircle(ink, 8.dp.toPx(), Offset(center.x - 2.dp.toPx(), center.y + 2.dp.toPx()), style = Stroke(sw))
                drawLine(ink, Offset(center.x + 3.7.dp.toPx(), center.y - 3.7.dp.toPx()), Offset(center.x + 10.dp.toPx(), center.y - 10.dp.toPx()), sw, cap = StrokeCap.Round)
                drawLine(ink, Offset(center.x + 4.5.dp.toPx(), center.y - 10.dp.toPx()), Offset(center.x + 10.dp.toPx(), center.y - 10.dp.toPx()), sw, cap = StrokeCap.Round)
                drawLine(ink, Offset(center.x + 10.dp.toPx(), center.y - 10.dp.toPx()), Offset(center.x + 10.dp.toPx(), center.y - 4.5.dp.toPx()), sw, cap = StrokeCap.Round)
            }
        }
        Text(
            label, fontFamily = MorBody, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            color = if (on) Teal else Sub, modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun TargetBody(draft: V2Draft) {
    val a = draft.answers
    val view = LocalView.current
    var tickLast by remember { mutableLongStateOf(0L) }
    val max = maxOf(80f, a.weightKg + 10f)
    val loss = a.weightKg - a.targetKg
    val amb = loss > a.weightKg * 0.1f
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            Modifier.fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = Ink.copy(alpha = 0.06f))
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White)
                .padding(16.dp, 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    fmtKg(a.targetKg), fontFamily = MorDisplay, fontSize = 58.sp,
                    fontWeight = FontWeight.ExtraBold, color = Ink
                )
                Text(" kg", fontFamily = MorBody, fontSize = 22.sp, color = Sub)
            }
            if (abs(loss) >= 0.05f) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Pill(
                        "${if (loss > 0) "−" else "+"}${fmtKg(abs(loss))} kg · ${if (amb) "Ambitious" else "Realistic"}",
                        bg = if (amb) AmberBg else GreenBg, fg = if (amb) Amber else Green
                    )
                    Pill("By ${targetDate(loss)}", bg = Mint, fg = Teal)
                }
            }
            Slider(
                value = a.targetKg,
                onValueChange = {
                    draft.answers = draft.answers.copy(targetKg = it)
                    draft.save()
                    val n = System.currentTimeMillis()
                    if (n - tickLast > 45) { MorSound.tickWheel(); tickLast = n }
                },
                valueRange = 35f..max,
                steps = (((max - 35f) / 0.5f).roundToInt() - 1).coerceAtLeast(0),
                colors = SliderDefaults.colors(
                    thumbColor = Teal, activeTrackColor = Teal,
                    inactiveTrackColor = Line2, activeTickColor = Teal,
                    inactiveTickColor = Line2
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Slide to your target · now ${fmtKg(a.weightKg)} kg",
                fontFamily = MorBody, fontSize = 13.5.sp, color = Sub, textAlign = TextAlign.Center
            )
        }
        if (amb) {
            val ms = ((a.weightKg - a.weightKg * 0.1f) * 10).roundToInt() / 10f
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                    .background(AmberBg).padding(12.dp, 14.dp)
            ) {
                Text(
                    "Big jump. Go in steps?", fontFamily = MorBody, fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold, color = Amber
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color.White)
                        .clickable {
                            MorSound.ensure(view.context)
                            MorSound.select(2)
                            draft.answers = draft.answers.copy(targetKg = ms)
                            draft.save()
                        }
                        .padding(horizontal = 16.dp, vertical = 11.dp)
                ) {
                    Text(
                        "Start with ${fmtKg(ms)} kg", fontFamily = MorBody,
                        fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = Amber
                    )
                }
            }
        }
    }
}

private fun targetDate(loss: Float): String {
    val weeks = maxOf(1, abs((loss / 0.5f).roundToInt()))
    return LocalDateTime.now().plusDays(weeks * 7L)
        .format(DateTimeFormatter.ofPattern("d MMM uuuu"))
}

@Composable
private fun MealsBody(draft: V2Draft) {
    val a = draft.answers
    val view = LocalView.current
    var editing by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("breakfast", "lunch", "snack", "dinner").forEach { s ->
            Row(
                Modifier
                    .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = Ink.copy(alpha = 0.05f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .padding(12.dp, 8.dp, 12.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SlotIcon(s)
                Spacer(Modifier.width(12.dp))
                Text(
                    s.replaceFirstChar { it.uppercase() }, fontFamily = MorBody,
                    fontSize = 15.5.sp, fontWeight = FontWeight.Bold, color = Ink,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.5.dp, Line2, RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .clickable { editing = s }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        fmtTime(a.times[s] ?: "08:30"), fontFamily = MorBody,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = a.mealOn[s] == true,
                    onCheckedChange = {
                        MorSound.ensure(view.context)
                        MorSound.select(if (it) 3 else 0)
                        draft.answers = draft.answers.copy(mealOn = a.mealOn + (s to it))
                        draft.save()
                    },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Teal,
                        uncheckedTrackColor = Line2,
                        checkedThumbColor = Color.White,
                        uncheckedThumbColor = Color.White
                    )
                )
            }
        }
        Text(
            "Neelu reminds you at these times. Max 4 a day, never at night.",
            fontFamily = MorBody, fontSize = 13.5.sp, color = Sub,
            modifier = Modifier.padding(4.dp, 0.dp)
        )
    }
    editing?.let { slot ->
        TimeSheet(
            slot, a.times[slot] ?: "08:30",
            onSave = { t ->
                draft.answers = draft.answers.copy(times = a.times + (slot to t))
                draft.save()
                editing = null
            },
            onDismiss = { editing = null }
        )
    }
}

@Composable
private fun SlotIcon(slot: String) {
    Canvas(Modifier.size(34.dp)) {
        when (slot) {
            "breakfast" -> {
                drawCircle(Color(0xFFDDB06A), 9.4.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                (0..2).forEach { k ->
                    drawLine(
                        Color(0xFFB98A55), Offset(center.x - 6.dp.toPx(), center.y + (k - 1) * 3.dp.toPx()),
                        Offset(center.x + 6.dp.toPx(), center.y + (k - 1) * 3.dp.toPx()), 1.dp.toPx()
                    )
                }
            }
            "lunch" -> {
                drawCircle(Color(0xFFC98A1E), 9.4.dp.toPx(), center)
                drawCircle(Color(0xFFE8A23C), 2.6.dp.toPx(), Offset(center.x - 3.dp.toPx(), center.y - 2.4.dp.toPx()))
                drawCircle(Color(0xFFF5F0E4), 2.6.dp.toPx(), Offset(center.x + 3.dp.toPx(), center.y - 2.4.dp.toPx()))
                drawCircle(Color(0xFFD9B48A), 2.6.dp.toPx(), Offset(center.x, center.y + 2.6.dp.toPx()))
            }
            "snack" -> {
                drawCircle(Color(0xFF8A5A2B), 5.5.dp.toPx(), Offset(center.x - 0.5.dp.toPx(), center.y + 1.dp.toPx()))
                drawCircle(Color(0xFFF5EDE0), 5.dp.toPx(), Offset(center.x - 0.5.dp.toPx(), center.y - 2.dp.toPx()))
                drawLine(Color(0xFF9B9B9B), Offset(center.x - 4.dp.toPx(), center.y - 9.dp.toPx()), Offset(center.x - 6.dp.toPx(), center.y - 4.dp.toPx()), 0.9.dp.toPx())
                drawLine(Color(0xFF9B9B9B), Offset(center.x + 3.dp.toPx(), center.y - 9.dp.toPx()), Offset(center.x + 5.dp.toPx(), center.y - 4.dp.toPx()), 0.9.dp.toPx())
            }
            else -> {
                drawCircle(Color(0xFFE8A23C), 8.dp.toPx(), Offset(center.x, center.y + 1.dp.toPx()))
                drawCircle(Color(0xFFEFC06A), 8.dp.toPx(), Offset(center.x, center.y - 1.dp.toPx()))
                drawCircle(Color(0xFFC98A1E), 0.9.dp.toPx(), Offset(center.x - 2.dp.toPx(), center.y - 0.6.dp.toPx()))
                drawCircle(Color(0xFFC98A1E), 0.9.dp.toPx(), Offset(center.x + 2.dp.toPx(), center.y + 0.2.dp.toPx()))
            }
        }
    }
}

/* building / plan / notify --------------------------------------------- */

private fun v2CommitMap(draft: V2Draft): Map<String, Any?> {
    val a = draft.answers
    val meals = a.times.filterKeys { a.mealOn[it] == true }
    return mapOf(
        "goal" to a.goal, "diet" to a.diet, "sex" to a.sex, "age" to a.age,
        "height_cm" to a.heightCm, "weight_kg" to a.weightKg, "target_weight_kg" to a.targetKg,
        "activity" to a.activity, "meal_times" to meals, "complete" to true
    )
}

@Composable
fun V2BuildingScreen(
    draft: V2Draft, vm: AppVm,
    onPlan: (Plan?) -> Unit, advance: () -> Unit, allowSkip: Boolean
) {
    val a = draft.answers
    val dietWord = when (a.diet) {
        "vegan" -> "vegan"; "veg" -> "vegetarian"; "egg" -> "eggetarian"; else -> "meals"
    }
    val goalWord = when (a.goal) {
        "lose" -> "weight-loss"; "gain" -> "weight-gain"; "healthy" -> "eat-healthier"; else -> "maintain"
    }
    val lines = listOf(
        "Reading your $goalWord goal",
        "Matching $dietWord Indian meals",
        "Setting your daily calories",
        "Splitting protein, carbs and fat"
    )
    var attempt by remember { mutableIntStateOf(0) }
    var progress by remember(attempt) { mutableIntStateOf(0) }
    var doneCount by remember(attempt) { mutableStateOf(-1) }
    var failed by remember(attempt) { mutableStateOf(false) }
    var planReady by remember(attempt) { mutableStateOf(false) }
    val planDef = remember(attempt) { CompletableDeferred<Plan?>() }
    val reduced = rememberReducedMotion()
    val ctx = LocalContext.current

    fun commit() = vm.saveProfile(v2CommitMap(draft)) { r -> planDef.complete(r?.plan) }

    LaunchedEffect(attempt) {
        delay(380)
        commit()
    }

    LaunchedEffect(Unit) {
        val stage = if (reduced) 120L else 420L
        for (i in lines.indices) {
            delay(stage)
            if (i == lines.lastIndex) {
                val p = withTimeoutOrNull(30000) { planDef.await() }
                if (p == null) {
                    failed = true
                    MorSound.back()
                    return@LaunchedEffect
                }
                progress = 100
                doneCount = i
                MorSound.select(i)
                planReady = true
                onPlan(p)
                delay(450)
                advance()
                return@LaunchedEffect
            } else {
                progress = (((i + 1) / 4f) * 92).roundToInt()
                doneCount = i
                MorSound.select(i)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        V2Top("building", null, draft)
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            GuideNeelu("building", 104.dp)
            Box(Modifier.weight(1f)) {
                BubbleHeading(
                    "Building your plan…",
                    "Balancing your $dietWord and ${a.goal.ifEmpty { "health" }} goal.",
                    null, center = false
                )
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lines.forEachIndexed { i, line ->
                    if (i > doneCount) return@forEachIndexed
                    Row(
                        Modifier
                            .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = Ink.copy(alpha = 0.06f))
                            .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                            .background(Color.White)
                            .padding(10.dp, 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusDot(if (failed && i == lines.lastIndex) "fail" else "done")
                        Spacer(Modifier.width(10.dp))
                        Text(
                            line + if (failed && i == lines.lastIndex) " — that took too long." else "",
                            fontFamily = MorBody, fontSize = 15.sp, color = Ink
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress / 100f },
                color = Teal, trackColor = Line,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp)
                .navigationBarsPadding().padding(bottom = 22.dp)
        ) {
            if (failed) {
                Box(Modifier.weight(1f)) {
                    V2Button("Try again") {
                        attempt += 1
                    }
                }
            } else if (allowSkip) {
                Box(Modifier.weight(1f)) {
                    V2Button("Skip", enabled = planReady) { advance() }
                }
            }
        }
    }
}

@Composable
private fun StatusDot(state: String) {
    Canvas(Modifier.size(20.dp)) {
        if (state == "fail") {
            drawCircle(Kumkum, 10.dp.toPx(), center)
        } else {
            drawCircle(Pankh, 10.dp.toPx(), center)
            drawLine(Color.White, Offset(center.x - 5.dp.toPx(), center.y + 0.5.dp.toPx()), Offset(center.x - 1.dp.toPx(), center.y + 4.5.dp.toPx()), 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.White, Offset(center.x - 1.dp.toPx(), center.y + 4.5.dp.toPx()), Offset(center.x + 5.dp.toPx(), center.y - 4.dp.toPx()), 2.2.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
fun V2PlanScreen(
    draft: V2Draft, plan: Plan, onStart: () -> Unit
) {
    val reduced = rememberReducedMotion()
    val cardStagger = remember { Animatable(if (reduced) 1f else 0f) }
    val confetti = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (!draft.confettiShown && !reduced) {
            draft.confettiShown = true
            draft.save()
            MorSound.done()
            confetti.animateTo(1f, tween(2700, easing = LinearEasing))
        }
    }
    LaunchedEffect(Unit) {
        delay(450)
        cardStagger.animateTo(1f, tween(900, easing = MacroEasing))
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            V2Top("plan", null, draft)
            Column(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GuideNeelu("plan", 150.dp)
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(300.dp)) {
                    BubbleHeading("Your plan is ready!", null, null, center = true)
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp)
            ) {
                Column(
                    Modifier.fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = Ink.copy(alpha = 0.06f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(16.dp, 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "DAILY TARGET", fontFamily = MorBody, fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold, color = Sub, letterSpacing = 1.sp
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "%,d".format(plan.kcal_target), fontFamily = MorDisplay,
                            fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = Teal
                        )
                        Text(" kcal", fontFamily = MorBody, fontSize = 20.sp, color = Sub)
                    }
                    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val mx = maxOf(plan.protein_g * 4, plan.carbs_g * 4, plan.fat_g * 9).toFloat()
                        MacroRow("Protein", plan.protein_g, Neel, (plan.protein_g * 4 / mx).toFloat(), cardStagger.value)
                        MacroRow("Carbs", plan.carbs_g, Genda, (plan.carbs_g * 4 / mx).toFloat(), cardStagger.value)
                        MacroRow("Fat", plan.fat_g, Pankh, (plan.fat_g * 9 / mx).toFloat(), cardStagger.value)
                    }
                    val line = plan.goal_line
                    if (line != null) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                        ) {
                            Pill("$line · by ${plan.target_date?.let { prettyDate(it) } ?: ""}", bg = Mint, fg = Teal)
                        }
                    }
                }
            }
            Box(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 22.dp).navigationBarsPadding()) {
                val view = LocalView.current
                val h = rememberHaptics()
                V2Button("Start", onClick = { h(); onStart() })
            }
        }
        Confetti(confetti.value)
    }
}

@Composable
private fun MacroRow(label: String, grams: Double, color: Color, frac: Float, shown: Float) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(label, fontFamily = MorBody, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Ink, modifier = Modifier.width(62.dp))
        Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(Line)) {
            Box(
                Modifier.fillMaxWidth(frac * shown).height(9.dp)
                    .clip(RoundedCornerShape(5.dp)).background(color)
            )
        }
        Text(
            "${grams.roundToInt()} g", fontFamily = MorBody, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, color = Ink, textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp)
        )
    }
}

/** Small confetti burst for the plan reveal (two volleys, Mor palette colors). */
@Composable
private fun Confetti(t: Float) {
    if (t <= 0f) return
    Canvas(Modifier.fillMaxSize()) {
        val colors = listOf(Color(0xFF0B4A57), Color(0xFFF2B544), Color(0xFF19A39A), Color(0xFF1F4FB5), Color(0xFFD9483B))
        val r = size
        fun burst(seed: Int, count: Int, spread: Float, vel: Float, at: Float, scale: Float) {
            val rel = (t - at) / 2f
            if (rel <= 0f || rel > 1f) return
            val rnd = java.util.Random(seed.toLong())
            for (i in 0 until count) {
                val ang = (-90f + (rnd.nextFloat() - 0.5f) * spread) * (Math.PI / 180).toFloat()
                val sp = vel * (0.55f + rnd.nextFloat() * 0.45f) * r.height * 0.75f
                val ox = r.width * (0.12f + rnd.nextFloat() * 0.76f)
                val oy = r.height * 0.32f
                val x = ox + Math.cos(ang.toDouble()).toFloat() * sp * rel
                val y = oy + Math.sin(ang.toDouble()).toFloat() * sp * rel + 0.55f * r.height * rel * rel
                val s = 6f * scale * (0.7f + rnd.nextFloat() * 0.6f)
                drawCircle(colors[i % colors.size], s, Offset(x, y), alpha = (1f - rel).coerceIn(0f, 1f))
            }
        }
        burst(7, 90, 150f, 0.9f, 380f / 2700f, 0.9f)
        burst(13, 50, 220f, 0.6f, 600f / 2700f, 0.8f)
    }
}

@Composable
fun V2NotifyScreen(
    nav: androidx.navigation.NavController, vm: AppVm, draft: V2Draft
) {
    val ctx = LocalContext.current
    var leaving by remember { mutableStateOf(false) }
    val view = LocalView.current
    val h = rememberHaptics()

    fun finish(on: Boolean) {
        if (leaving) return
        leaving = true
        vm.saveProfile(mapOf("reminders_on" to on)) {
            vm.refreshProfile()
            vm.refreshHome()
            if (!on) vm.showSnack("Reminders are off. Turn them on in Settings anytime.")
            nav.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        }
        draft.clear()
        if (on) MorSound.chime() else MorSound.next()
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        finish(ok)
    }

    fun yes() {
        MorSound.ensure(ctx)
        h()
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (granted) finish(true) else launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            finish(true)
        }
    }

    Column(Modifier.fillMaxSize()) {
        V2Top("notify", null, draft)
        Column(
            Modifier.fillMaxWidth().padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                GuideNeelu("notify", 200.dp)
                Box(
                    Modifier
                        .padding(top = 6.dp, end = 8.dp)
                        .size(46.dp).clip(CircleShape).background(Gold)
                        .shadow(4.dp, CircleShape, spotColor = Color(0x1E000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.size(24.dp)) {
                        val ink = Color(0xFF142027)
                        drawArc(
                            ink, 180f, 180f, false,
                            topLeft = Offset(center.x - 6.dp.toPx(), center.y - 6.dp.toPx()),
                            size = Size(12.dp.toPx(), 12.dp.toPx()),
                            style = Stroke(2.dp.toPx())
                        )
                        drawLine(ink, Offset(center.x - 6.dp.toPx(), center.y + 0.5.dp.toPx()), Offset(center.x + 6.dp.toPx(), center.y + 0.5.dp.toPx()), 2.dp.toPx())
                        drawArc(
                            ink, 0f, 180f, false,
                            topLeft = Offset(center.x - 3.5.dp.toPx(), center.y + 0.5f),
                            size = Size(7.dp.toPx(), 5.dp.toPx()),
                            style = Stroke(2.dp.toPx())
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(Modifier.width(300.dp)) {
                BubbleHeading("Remind you at mealtimes?", "Max 4 a day. Never at night.", null, center = true)
            }
        }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { }
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp)
                .navigationBarsPadding().padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            V2Button("Yes, remind me") { yes() }
            V2Button("Not now", secondary = true) { finish(false) }
        }
    }
}

/* splash / welcome / meet ----------------------------------------------- */

@Composable
private fun V2SplashScreen(onStart: () -> Unit) {
    val splashTap = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier.fillMaxSize().background(Teal).clickable(
            indication = null,
            interactionSource = splashTap
        ) { onStart() }
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Just the peacock artwork on the teal — no ring, no badge circle.
            NeeluBitmap("joy", Modifier.size(156.dp), contentDescription = null)
            Text(
                "Mor", fontFamily = MorDisplay, fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold, color = Color.White,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                "Eat well, the Indian way.", fontFamily = MorBody, fontSize = 15.sp,
                color = Color(0xFFCFE7E1)
            )
            Spacer(Modifier.height(42.dp))
            Text(
                "Tap anywhere to start your journey",
                fontFamily = MorBody, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold,
                color = Gold, letterSpacing = 0.2.sp,
                textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
            )
        }
    }
}

@Composable
private fun V2WelcomeScreen(draft: V2Draft, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        V2Top("welcome", null, draft)
        Column(
            Modifier.fillMaxWidth().padding(top = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GuideNeelu("welcome", 210.dp)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.width(320.dp)) {
                BubbleHeading(
                    "Snap your meal. Know your calories.",
                    "Point the camera at your thali. Neelu reads it in seconds — English-first, Indian food, minimal words.",
                    null, center = true
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Column(
            Modifier.padding(start = 20.dp, end = 20.dp)
                .navigationBarsPadding().padding(bottom = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            V2Button("Get started") { onNext() }
            Spacer(Modifier.height(10.dp))
            Text(
                "No sign-up needed", fontFamily = MorBody, fontSize = 13.5.sp, color = Sub
            )
        }
    }
}

@Composable
private fun V2MeetScreen(draft: V2Draft, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        V2Top("meet", null, draft)
        Column(
            Modifier.fillMaxWidth().padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GuideNeelu("meet", 230.dp)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.width(320.dp)) {
                BubbleHeading(
                    "Hi, I'm Neelu.",
                    "I'm happiest when you eat well. I'll count, you enjoy.",
                    null, center = true
                )
            }
        }
        Column(
            Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) { }
        Box(Modifier.padding(start = 20.dp, end = 20.dp).navigationBarsPadding().padding(bottom = 22.dp)) {
            V2Button("Let's go") { onNext() }
        }
    }
}

/* goal / diet / activity step wrappers ----------------------------------- */

@Composable
private fun V2GoalScreen(draft: V2Draft, onBack: () -> Unit, onNext: () -> Unit) {
    V2Step("goal", "What's your goal?", null, draft, onBack,
        enabled = draft.answers.goal.isNotEmpty(), onNext = onNext) {
        Choicex(
            listOf(
                V2Opt("lose", "Lose weight", "Steady, sustainable pace"),
                V2Opt("maintain", "Maintain weight", "Stay where you are"),
                V2Opt("gain", "Gain weight", "Build up gently"),
                V2Opt("healthy", "Eat healthier", "Better choices, same weight")
            ),
            draft.answers.goal,
            { v, i ->
                MorSound.select(i)
                draft.answers = draft.answers.copy(goal = v); draft.save()
            },
            grid = false
        )
    }
}

@Composable
private fun V2DietScreen(draft: V2Draft, onBack: () -> Unit, onNext: () -> Unit) {
    V2Step("diet", "What do you eat?", null, draft, onBack,
        enabled = draft.answers.diet.isNotEmpty(), onNext = onNext) {
        Choicex(
            listOf(
                V2Opt("vegan", "Vegan", mark = Green),
                V2Opt("veg", "Vegetarian", mark = Green),
                V2Opt("egg", "Eggetarian", mark = Color(0xFFC98A1E)),
                V2Opt("non", "Non-veg", mark = Kumkum)
            ),
            draft.answers.diet,
            { v, i ->
                MorSound.select(i)
                draft.answers = draft.answers.copy(diet = v); draft.save()
            },
            grid = true
        )
    }
}

@Composable
private fun V2BodyScreen(draft: V2Draft, onBack: () -> Unit, onNext: () -> Unit) {
    V2Step("body", "About you", null, draft, onBack,
        enabled = draft.answers.sex.isNotEmpty(), onNext = onNext) {
        BodyBody(draft)
    }
}

@Composable
private fun V2TargetScreen(draft: V2Draft, onBack: () -> Unit, onNext: () -> Unit) {
    V2Step("target", "Your target weight", null, draft, onBack,
        enabled = draft.answers.targetKg >= 35f, onNext = onNext) {
        TargetBody(draft)
    }
}

@Composable
private fun V2ActivityScreen(draft: V2Draft, onBack: () -> Unit, onNext: () -> Unit) {
    V2Step("activity", "How active are you?", null, draft, onBack,
        enabled = draft.answers.activity.isNotEmpty(), onNext = onNext) {
        Choicex(
            listOf(
                V2Opt("sit", "Mostly sitting", "Desk job, little movement", bars = 1),
                V2Opt("light", "Lightly active", "Walks, chores some days", bars = 2),
                V2Opt("active", "Active", "Exercise most days", bars = 3),
                V2Opt("very", "Very active", "Physical work or daily training", bars = 4)
            ),
            draft.answers.activity,
            { v, i ->
                MorSound.select(i)
                draft.answers = draft.answers.copy(activity = v); draft.save()
            },
            grid = false
        )
    }
}

@Composable
private fun V2MealsScreen(draft: V2Draft, onNext: () -> Unit) {
    V2Step("meals", "Your meal times", null, draft, null,
        cta = "Build my plan", enabled = true, onNext = onNext) {
        MealsBody(draft)
    }
}

/* journey plan ----------------------------------------------------------- */

fun fmtTime(t: String): String {
    val p = t.split(":")
    val h = p.getOrNull(0)?.toIntOrNull() ?: 0
    val m = p.getOrNull(1)?.toIntOrNull() ?: 0
    val ap = if (h >= 12) "PM" else "AM"
    val hh = if (h % 12 == 0) 12 else h % 12
    return "$hh:${m.toString().padStart(2, '0')} $ap"
}

private val V2_ORDER = listOf(
    "splash", "welcome", "meet", "goal", "diet", "body", "target",
    "activity", "meals", "building", "plan", "notify"
)

/** Route host: Backdrop + drift; parallel enter/exit spring transitions
 *  (enter +48px * dir, exit fade and settles -36px * dir); pose hold. */
@Composable
fun V2Onboarding(nav: androidx.navigation.NavController, vm: AppVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val draft = remember { V2Draft(ctx).apply { load() } }
    var step by remember { mutableStateOf(if (draft.stepKey in V2_ORDER) draft.stepKey else "splash") }
    var dir by remember { mutableIntStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var planShown by remember { mutableStateOf<Plan?>(null) }
    val reduced = rememberReducedMotion()

    fun go(d: Int) {
        if (busy) return
        val i = V2_ORDER.indexOf(step) + d
        var j = i.coerceIn(0, V2_ORDER.lastIndex)
        if (d < 0 && V2_ORDER[j] == "building") j-- // back from Plan never re-runs Building
        if (j == V2_ORDER.indexOf(step)) return
        busy = true
        dir = d
        scope.launch {
            MorSound.ensure(ctx)
            if (d > 0) MorSound.next() else MorSound.back()
            PoseArt.await(V2_NEELU[V2_ORDER[j]] ?: V2_NEELU["splash"] ?: "happy")
            draft.stepKey = V2_ORDER[j]
            draft.save()
            step = V2_ORDER[j]
            busy = false
        }
    }

    Box(Modifier.fillMaxSize().background(Chandan)) {
        MosaicWall(alpha = 0.6f)
        AnimatedContent(
            targetState = step,
            contentKey = { it },
            transitionSpec = {
                val d = if (targetState != initialState) dir else dir
                if (reduced) {
                    (fadeIn(tween(180)) togetherWith fadeOut(tween(150)))
                } else {
                    // Welcome/meet enter in place (fade + slight scale) so the
                    // centred assets never slide horizontally during the hop.
                    val enter = if (targetState == "welcome" || targetState == "meet")
                        fadeIn(M.ScreenSpring) + scaleIn(M.ScreenSpring, initialScale = 0.94f)
                    else
                        fadeIn(M.ScreenSpring) + slideInHorizontally(M.ScreenSpringIntOffset) { full -> (full * 0.135f).toInt() * d }
                    enter togetherWith
                        (fadeOut(tween(200, easing = ExitEasing)) + slideOutHorizontally(tween(200, easing = ExitEasing)) { full -> (-full * 0.1f).toInt() * d })
                }
            },
            label = "v2journey"
        ) { s ->
            Box(Modifier.fillMaxSize()) {
                when (s) {
                    "splash" -> V2SplashScreen { go(1) }
                    "welcome" -> V2WelcomeScreen(draft) { go(1) }
                    "meet" -> V2MeetScreen(draft) { go(1) }
                    "goal" -> V2GoalScreen(draft, { go(-1) }) { go(1) }
                    "diet" -> V2DietScreen(draft, { go(-1) }) { go(1) }
                    "body" -> V2BodyScreen(draft, { go(-1) }) { go(1) }
                    "target" -> V2TargetScreen(draft, { go(-1) }) { go(1) }
                    "activity" -> V2ActivityScreen(draft, { go(-1) }) { go(1) }
                    "meals" -> V2MealsScreen(draft) { go(1) }
                    "building" -> V2BuildingScreen(
                        draft, vm,
                        onPlan = { planShown = it },
                        advance = { go(1) },
                        allowSkip = true
                    )
                    "plan" -> V2PlanScreen(
                        draft, planShown ?: Plan(),
                        onStart = { go(1) }
                    )
                    "notify" -> V2NotifyScreen(nav, vm, draft)
                }
            }
        }
    }
}
