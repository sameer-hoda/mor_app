@file:OptIn(
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package dev.mor.app.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.mor.app.R
import dev.mor.app.data.AppVm
import dev.mor.app.data.Plan
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberUpdatedState

/** Phase A onboarding: palace door journey (intro) then the step system from
 *  the final-build package (02-onboarding-layout, 04-option-icons,
 *  05-about-you-8c, 06-target-weight-9a, 07-plan-ready-flow). */

private val V2_STEPN = mapOf("goal" to 1, "diet" to 2, "body" to 3, "target" to 4, "activity" to 5, "meals" to 6)
private val V2_NEELU = mapOf(
    "journey" to "happy", "goal" to "cool", "diet" to "hungry", "body" to "cool",
    "target" to "thinking", "activity" to "wave", "meals" to "hungry",
    "building" to "thinking", "plan" to "joy", "allset" to "happy"
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
    val paceIdx: Int = 1,
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
                .put("target_weight_kg", a.targetKg).put("pace_idx", a.paceIdx)
                .put("activity", a.activity)
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
            var sex = j.optString("sex", "")
            if (sex == "other") sex = "" // item 11: "Other" removed everywhere; ask again
            answers = V2Answers(
                goal = j.optString("goal", ""), diet = j.optString("diet", ""),
                sex = sex, age = j.optInt("age", 30),
                heightCm = j.optInt("height_cm", 170),
                weightKg = j.optDouble("weight_kg", 72.0).toFloat(),
                targetKg = j.optDouble("target_weight_kg", 66.0).toFloat(),
                paceIdx = j.optInt("pace_idx", 1),
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
private val CubicOut = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)

private fun fmtKg(v: Float): String =
    if (abs(v % 1f) < 0.05f) v.roundToInt().toString() else "%.1f".format(v)

@Composable
private fun rememberHaptics(): () -> Unit {
    val view = androidx.compose.ui.platform.LocalView.current
    return remember(view) {
        { view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) }
    }
}

/* shared scaffolding ----------------------------------------------------- */

@Composable
private fun V2Top(step: String, draft: V2Draft) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp).height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) { }
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

/** Speech bubble used by the building screen. */
@Composable
private fun BubbleHeading(title: String, sub: String?) {
    Column(
        Modifier
            .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = TealDark.copy(alpha = 0.10f))
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 13.dp)
    ) {
        Text(
            title, fontFamily = MorDisplay, fontSize = 23.sp, fontWeight = FontWeight.ExtraBold,
            color = Ink, lineHeight = 26.5.sp,
        )
        if (sub != null) {
            Text(
                sub, fontFamily = MorBody, fontSize = 14.5.sp, color = Sub,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/* option groups (04-option-icons) --------------------------------------- */

private val GOAL_OPTS = listOf(
    OptionSpec("lose", "Lose weight", "Steady, no crash diets", R.drawable.ic_opt_lose, R.drawable.ic_opt_lose_selected, R.drawable.avd_opt_lose),
    OptionSpec("maintain", "Maintain weight", "Stay here, eat better", R.drawable.ic_opt_maintain, R.drawable.ic_opt_maintain_selected, R.drawable.avd_opt_maintain),
    OptionSpec("gain", "Gain weight", "Build up gently", R.drawable.ic_opt_gain, R.drawable.ic_opt_gain_selected, R.drawable.avd_opt_gain),
    OptionSpec("healthy", "Eat healthier", "Same weight, better thalis", R.drawable.ic_opt_healthy, R.drawable.ic_opt_healthy_selected, R.drawable.avd_opt_healthy),
)
private val DIET_OPTS = listOf(
    OptionSpec("vegan", "Vegan", "No animal products", R.drawable.ic_opt_vegan, R.drawable.ic_opt_vegan_selected, R.drawable.avd_opt_vegan),
    OptionSpec("veg", "Vegetarian", "Dairy, no meat or eggs", R.drawable.ic_opt_veg, R.drawable.ic_opt_veg_selected, R.drawable.avd_opt_veg),
    OptionSpec("egg", "Eggetarian", "Vegetarian, plus eggs", R.drawable.ic_opt_egg, R.drawable.ic_opt_egg_selected, R.drawable.avd_opt_egg),
    OptionSpec("nonveg", "Non-veg", "Meat, fish and eggs", R.drawable.ic_opt_nonveg, R.drawable.ic_opt_nonveg_selected, R.drawable.avd_opt_nonveg),
)
private val ACT_OPTS = listOf(
    OptionSpec("sed", "Sedentary", "Mostly sitting during the day", R.drawable.ic_opt_sed, R.drawable.ic_opt_sed_selected, R.drawable.avd_opt_sed),
    OptionSpec("light", "Lightly active", "Light walks or exercise 1–3 days a week", R.drawable.ic_opt_light, R.drawable.ic_opt_light_selected, R.drawable.avd_opt_light),
    OptionSpec("mod", "Moderately active", "Exercise 3–5 days a week", R.drawable.ic_opt_mod, R.drawable.ic_opt_mod_selected, R.drawable.avd_opt_mod),
    OptionSpec("very", "Very active", "Hard exercise 6–7 days a week", R.drawable.ic_opt_very, R.drawable.ic_opt_very_selected, R.drawable.avd_opt_very),
)

// Server contract values (backend/src/plan.js): activity sit/light/active/very,
// diet non is the old "non-veg" id.
private fun actKey(v: String) = when (v) { "sit" -> "sed"; "active" -> "mod"; else -> v }
private fun actVal(k: String) = when (k) { "sed" -> "sit"; "mod" -> "active"; else -> k }
private fun dietKey(v: String) = if (v == "non") "nonveg" else v
private fun dietVal(k: String) = if (k == "nonveg") "non" else k

/* wheel cards (05-about-you-8c) ------------------------------------------ */

private val V2_PREFS = "mor"

private fun cmToIn(cm: Int): Int = (cm / 2.54).roundToInt().coerceIn(51, 83)
private fun inToCm(inch: Int): Int = (inch * 2.54).roundToInt().coerceIn(130, 210)
private fun ftinLabel(inch: Int): String = "${inch / 12}′${inch % 12}″"

private fun kgToLb(kg: Float): Int = (kg * 2.20462f).roundToInt().coerceIn(77, 353)
private fun lbToKg(lb: Int): Float = (lb / 2.20462f * 2).roundToInt() / 2f

private data class WheelSpec(
    val label: String,
    val items: List<WheelItem>,
    val initialIndex: Int,
    val description: String,
    val unitPill: String?,
    val onUnit: (() -> Unit)?,
    val allowDecimal: Boolean,
    val range: ClosedFloatingPointRange<Float>,
    val rangeText: String,
)

@Composable
private fun WheelCard(spec: WheelSpec, onValue: (Float) -> Unit, onTapCentre: () -> Unit) {
    Column(
        Modifier
            .clip(RoundedCornerShape(MorShape.Tile))
            .background(MorColor.Card)
            .border(1.dp, MorColor.SheetBorder, RoundedCornerShape(MorShape.Tile))
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth().height(28.dp).padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                spec.label.uppercase(), fontFamily = MorInter, fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em,
                color = MorColor.Muted,
            )
            Spacer(Modifier.weight(1f))
            if (spec.unitPill != null && spec.onUnit != null) {
                Row(
                    Modifier
                        .height(26.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(MorColor.Ivory)
                        .border(1.dp, Color(0xFFD9C9AA), RoundedCornerShape(13.dp))
                        .clickable(onClick = spec.onUnit)
                        .padding(horizontal = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        spec.unitPill, fontFamily = MorInter, fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold, color = MorColor.Maroon,
                    )
                    Canvas(Modifier.padding(start = 4.dp).size(6.dp, 4.dp)) {
                        val p = Path().apply {
                            moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height); close()
                        }
                        drawPath(p, MorColor.Maroon)
                    }
                }
            }
        }
        WheelPicker(
            items = spec.items,
            selectedIndex = spec.initialIndex,
            onSelected = { i -> spec.items.getOrNull(i)?.let { onValue(it.value) } },
            onTapCentre = onTapCentre,
            contentDescription = spec.description,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Tap to type", fontFamily = MorInter, fontSize = 11.sp,
            color = Color(0xFF8A7A62),
        )
    }
}

/** Keypad sheet: type a value, validated against the wheel's range. */
@Composable
private fun ObKeypad(
    label: String, initial: String, unit: String,
    allowDecimal: Boolean, range: ClosedFloatingPointRange<Float>, rangeText: String,
    onSet: (Float) -> Unit, onDismiss: () -> Unit,
) {
    var typed by remember { mutableStateOf(initial) }
    var err by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MorColor.Ivory,
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                label.uppercase(), fontFamily = MorInter, fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em,
                color = MorColor.Eyebrow,
            )
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    typed.ifEmpty { "0" }, fontFamily = Cormorant, fontSize = 54.sp,
                    fontWeight = FontWeight.SemiBold, color = MorColor.Ink, lineHeight = 58.sp,
                )
                if (unit.isNotEmpty()) {
                    Text(
                        " $unit", fontFamily = MorInter, fontSize = 20.sp,
                        color = MorColor.Muted,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            err?.let {
                Text(it, fontFamily = MorInter, fontSize = 13.sp, color = MorColor.Error,
                    modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            val keys = listOf(
                listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"),
                listOf(if (allowDecimal) "." else "", "0", "⌫"),
            )
            keys.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { k ->
                        if (k.isEmpty()) {
                            Spacer(Modifier.width(86.dp))
                        } else {
                            Box(
                                Modifier
                                    .size(width = 86.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MorColor.Card)
                                    .border(1.dp, MorColor.SheetBorder, RoundedCornerShape(14.dp))
                                    .clickable {
                                        err = null
                                        typed = when (k) {
                                            "⌫" -> typed.dropLast(1)
                                            "." -> if (!typed.contains('.')) "$typed." else typed
                                            else -> if (typed.length < 6) typed + k else typed
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (k == "⌫") "⌫" else k,
                                    fontFamily = if (k == "⌫") MorInter else Cormorant,
                                    fontSize = if (k == "⌫") 20.sp else 26.sp,
                                    fontWeight = FontWeight.SemiBold, color = MorColor.Ink,
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(4.dp))
            val parsed = typed.toFloatOrNull()
            ObPrimaryButton(
                "Set",
                enabled = parsed != null && parsed in range,
                disabledText = if (err == null && parsed != null && parsed !in range) rangeText else null,
            ) {
                val v = typed.toFloatOrNull()
                if (v == null || v !in range) {
                    err = "Enter a value between $rangeText"
                } else {
                    onSet(v); onDismiss()
                }
            }
        }
    }
}

/* steps ------------------------------------------------------------------ */

private fun stepPose(step: String): String = V2_NEELU[step] ?: "happy"

@Composable
private fun GoalStep(draft: V2Draft, dir: Int, onNext: () -> Unit) {
    OnboardingScaffold(
        step = 1, artKey = "goal",
        bubble = "Let’s start with the big one.",
        title = "What’s your goal?",
        primaryText = "Continue", primaryEnabled = draft.answers.goal.isNotEmpty(),
        primaryDisabledText = "Choose one to continue",
        dir = dir, onPrimary = onNext,
    ) {
        OptionGroup(GOAL_OPTS, draft.answers.goal) { k ->
            MorSound.select(GOAL_OPTS.indexOfFirst { it.key == k }.coerceAtLeast(0))
            draft.answers = draft.answers.copy(goal = k)
            draft.save()
        }
    }
}

@Composable
private fun DietStep(draft: V2Draft, dir: Int, onNext: () -> Unit) {
    OnboardingScaffold(
        step = 2, artKey = "diet",
        bubble = "Now, the important bit. Food!",
        title = "What do you eat?",
        primaryText = "Continue", primaryEnabled = draft.answers.diet.isNotEmpty(),
        primaryDisabledText = "Choose one to continue",
        dir = dir, onPrimary = onNext,
    ) {
        OptionGroup(DIET_OPTS, dietKey(draft.answers.diet)) { k ->
            MorSound.select(DIET_OPTS.indexOfFirst { it.key == k }.coerceAtLeast(0))
            draft.answers = draft.answers.copy(diet = dietVal(k))
            draft.save()
        }
    }
}

@Composable
private fun BodyStep(draft: V2Draft, dir: Int, onNext: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(V2_PREFS, Context.MODE_PRIVATE) }
    var hUnit by remember { mutableStateOf(prefs.getString("v2_hunit", "cm") ?: "cm") }
    var wUnit by remember { mutableStateOf(prefs.getString("v2_wunit", "kg") ?: "kg") }
    var keypadFor by remember { mutableStateOf<String?>(null) }
    val a = draft.answers

    fun setH(cm: Int) { draft.answers = draft.answers.copy(heightCm = cm); draft.save() }
    fun setW(kg: Float) { draft.answers = draft.answers.copy(weightKg = kg); draft.save() }

    val ageItems = (16..90).map { WheelItem(it.toFloat(), "$it", "yrs") }
    val hItems = if (hUnit == "cm") {
        (130..210).map { WheelItem(it.toFloat(), "$it", "cm") }
    } else {
        (51..83).map { i -> WheelItem(inToCm(i).toFloat(), ftinLabel(i), "") }
    }
    val wItems = if (wUnit == "kg") {
        (70..320).map { i -> WheelItem(i / 2f, fmtKg(i / 2f), "kg") }
    } else {
        (77..353).map { i -> WheelItem(lbToKg(i), "$i", "lb") }
    }
    val ageIdx = (a.age - 16).coerceIn(0, ageItems.lastIndex)
    val hIdx = if (hUnit == "cm") (a.heightCm - 130).coerceIn(0, hItems.lastIndex)
    else (cmToIn(a.heightCm) - 51).coerceIn(0, hItems.lastIndex)
    val wIdx = if (wUnit == "kg") {
        ((a.weightKg * 2).roundToInt() - 70).coerceIn(0, wItems.lastIndex)
    } else (kgToLb(a.weightKg) - 77).coerceIn(0, wItems.lastIndex)

    val summary = listOf(
        "${a.age} yrs",
        if (hUnit == "cm") "${a.heightCm} cm" else ftinLabel(cmToIn(a.heightCm)),
        if (wUnit == "kg") "${fmtKg(a.weightKg)} kg" else "${kgToLb(a.weightKg)} lb",
    )

    OnboardingScaffold(
        step = 3, artKey = "body",
        bubble = "A few numbers, so the maths is yours.",
        title = "About you",
        widePadding = true,
        primaryText = "Continue", primaryEnabled = a.sex.isNotEmpty(),
        primaryDisabledText = "Pick one to continue",
        dir = dir, onPrimary = onNext,
    ) {
        // v4 #4: S1 sliding-pill control; nothing selected until first tap.
        PillSegmentedControl(
            listOf(
                Segment(
                    "female", "Female",
                    icon = R.drawable.ic_opt_female, iconAnim = R.drawable.avd_opt_female,
                ),
                Segment(
                    "male", "Male",
                    icon = R.drawable.ic_opt_male, iconAnim = R.drawable.avd_opt_male,
                ),
            ),
            selectedKey = a.sex.ifEmpty { null },
            height = 56.dp,
            contentDescription = "Sex",
            onSelect = { k ->
                MorSound.select(if (k == "female") 0 else 1)
                draft.answers = draft.answers.copy(sex = k)
                draft.save()
            },
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) {
                WheelCard(
                    WheelSpec(
                        "Age", ageItems, ageIdx, "Age, ${a.age} years", null, null,
                        false, 16f..90f, "16–90",
                    ),
                    onValue = { v -> draft.answers = draft.answers.copy(age = v.roundToInt()); draft.save() },
                    onTapCentre = { keypadFor = "age" },
                )
            }
            Box(Modifier.weight(1f)) {
                WheelCard(
                    WheelSpec(
                        "Height", hItems, hIdx, "Height", if (hUnit == "cm") "cm" else "ft/in",
                        { hUnit = if (hUnit == "cm") "ftin" else "cm"; prefs.edit().putString("v2_hunit", hUnit).apply() },
                        false, 51f..83f, if (hUnit == "cm") "130–210" else "4′3″–6′11″",
                    ),
                    onValue = { v -> setH(v.roundToInt()) },
                    onTapCentre = { keypadFor = "height" },
                )
            }
            Box(Modifier.weight(1f)) {
                WheelCard(
                    WheelSpec(
                        "Weight", wItems, wIdx, "Weight", if (wUnit == "kg") "kg" else "lb",
                        { wUnit = if (wUnit == "kg") "lb" else "kg"; prefs.edit().putString("v2_wunit", wUnit).apply() },
                        wUnit == "kg", if (wUnit == "kg") 35f..160f else 77f..353f,
                        if (wUnit == "kg") "35–160" else "77–353",
                    ),
                    onValue = { v -> if (wUnit == "kg") setW(v) else setW(lbToKg(v.roundToInt())) },
                    onTapCentre = { keypadFor = "weight" },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            summary.forEachIndexed { i, s ->
                if (i > 0) {
                    Text("  ·  ", fontFamily = MorInter, fontSize = 15.sp, color = MorColor.Gold, fontWeight = FontWeight.Bold)
                }
                Text(s, fontFamily = MorInter, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Ink)
            }
        }
    }

    keypadFor?.let { which ->
        val cfg = when (which) {
            "age" -> KeypadCfg("Enter age", "${a.age}", "yrs", false, 16f..90f, "16–90")
            "height" -> if (hUnit == "cm") KeypadCfg("Enter height", "${a.heightCm}", "cm", false, 130f..210f, "130–210")
            else KeypadCfg("Enter height", "${cmToIn(a.heightCm)}", "", false, 51f..83f, "4′3″–6′11″")
            else -> KeypadCfg(
                // lb keypad gets lb values: kg initial/range made typed weights unusable
                if (wUnit == "kg") "Enter weight" else "Enter weight",
                if (wUnit == "kg") fmtKg(a.weightKg) else "${kgToLb(a.weightKg)}",
                wUnit,
                wUnit == "kg",
                if (wUnit == "kg") 35f..160f else 77f..353f,
                if (wUnit == "kg") "35–160" else "77–353",
            )
        }
        ObKeypad(
            cfg.label, cfg.initial, cfg.unit, cfg.decimal, cfg.range, cfg.rangeText,
            onSet = { v ->
                when (which) {
                    "age" -> draft.answers = draft.answers.copy(age = v.roundToInt())
                    "height" -> if (hUnit == "cm") setH(v.roundToInt()) else setH(inToCm(v.roundToInt()))
                    else -> setW(if (wUnit == "kg") ((v * 2).roundToInt() / 2f) else lbToKg(v.roundToInt()))
                }
                draft.save()
            },
            onDismiss = { keypadFor = null },
        )
    }
}

private data class KeypadCfg(
    val label: String, val initial: String, val unit: String,
    val decimal: Boolean, val range: ClosedFloatingPointRange<Float>, val rangeText: String,
)

/* target weight (06-target-weight-9a) ------------------------------------ */

private data class Pace(val key: String, val label: String, val rate: Float, val dailyDelta: Int, val guidance: String)

private val PACES = listOf(
    Pace("gradual", "Gradual", 0.18f, -300, "Gentle and easy to keep up. Ideal if you have had diets fail before."),
    Pace("steady", "Steady", 0.36f, -500, "Clear progress every month, with room for treats."),
    Pace("faster", "Faster", 0.54f, -700, "Quicker results. Keep protein up and sleep well."),
)
private val GAIN_PACES = listOf(
    Pace("gradual", "Gradual", 0.18f, 150, "Gentle and easy to keep up. Small extra portions, done."),
    Pace("steady", "Steady", 0.36f, 300, "Steady build-up every month, without feeling stuffed."),
    Pace("faster", "Faster", 0.54f, 450, "Quicker results. Keep protein up and sleep well."),
)
private val HORIZON_WEEKS = 16.1f

private fun tdeeOf(a: V2Answers): Float {
    val bmr = if (a.sex == "male") {
        10f * a.weightKg + 6.25f * a.heightCm - 5f * a.age + 5f
    } else {
        10f * a.weightKg + 6.25f * a.heightCm - 5f * a.age - 161f
    }
    val mult = when (a.activity) { "sit" -> 1.2f; "light" -> 1.375f; "active" -> 1.55f; else -> 1.725f }
    return bmr * mult
}

private fun paceTarget(a: V2Answers, pace: Pace): Float {
    val sign = if (a.goal == "gain") 1f else -1f
    return a.weightKg + sign * pace.rate * HORIZON_WEEKS
}

/** One cubic Bézier for a pace: start → dip → target, rebuilt per frame. */
private fun pacePath(w: Float, h: Float, startKg: Float, targetKg: Float, rate: Float, y: (Float) -> Float): Path {
    val sign = if (targetKg < startKg) -1f else 1f
    val p = Path()
    p.moveTo(0f, y(startKg))
    p.cubicTo(
        0.35f * w, y(startKg - sign * rate * 4f),
        0.65f * w, y(targetKg + sign * rate * 3f),
        w, y(targetKg),
    )
    return p
}

private fun bezierPoint(p0: Offset, p1: Offset, p2: Offset, p3: Offset, t: Float): Offset {
    val u = 1f - t
    val x = u * u * u * p0.x + 3f * u * u * t * p1.x + 3f * u * t * t * p2.x + t * t * t * p3.x
    val yy = u * u * u * p0.y + 3f * u * u * t * p1.y + 3f * u * t * t * p2.y + t * t * t * p3.y
    return Offset(x, yy)
}

@Composable
private fun TargetStep(draft: V2Draft, dir: Int, onNext: () -> Unit) {
    val a = draft.answers
    val reduced = rememberReducedMotion()
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences(V2_PREFS, Context.MODE_PRIVATE) }
    val lb = prefs.getString("v2_wunit", "kg") == "lb"
    val haptics = LocalHapticFeedback.current

    // v5 #5: every goal gets the same pace-driven target screen (Gradual /
    // Steady / Faster). The goal word is still stored for meal suggestions.
    val start = a.weightKg
    val maintaining = false
    val gain = a.goal == "gain"
    val losing = a.goal == "lose"
    val paces = if (gain) GAIN_PACES else PACES
    val pace = paces[a.paceIdx.coerceIn(0, paces.lastIndex)]
    val projected = (if (gain) start + pace.rate * HORIZON_WEEKS else start - pace.rate * HORIZON_WEEKS)
        .coerceIn(35f, 160f)
    val shown = if (maintaining) start else projected

    // One date for everything: the headline, the chart's end and the goal line.
    val horizonDate = LocalDateTime.now()
        .plusDays((HORIZON_WEEKS * 7).roundToInt().toLong())
        .format(DateTimeFormatter.ofPattern("d MMMM"))

    LaunchedEffect(Unit) {
        draft.answers = draft.answers.copy(targetKg = shown)
        draft.save()
    }

    var scrubX by remember { mutableStateOf<Float?>(null) }
    val animTarget by animateFloatAsState(shown, tween(if (reduced) 1 else 450, easing = CubicOut), label = "targetNum")
    val animRate by animateFloatAsState(pace.rate, tween(if (reduced) 1 else 450, easing = CubicOut), label = "rate")

    // The kcal chip shows exactly what the plan will deliver (pace-driven delta,
    // rounded to 10 like the server's plan).
    val minKcal = if (a.sex == "male") 1500 else 1200
    val planKcal = when {
        maintaining -> tdeeOf(a).roundToInt()
        else -> (((tdeeOf(a).roundToInt() + pace.dailyDelta) + 5) / 10 * 10)
            .coerceAtLeast(minKcal)
    }
    val change = shown - start

    OnboardingScaffold(
        step = 4, artKey = "target",
        bubble = "Three paces. Pick what feels right.",
        title = "Set your target weight",
        artHeight = ArtHeight.Compact,
        widePadding = true,
        primaryText = "Continue", primaryEnabled = true,
        dir = dir, onPrimary = onNext,
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (maintaining) {
                Text(
                    "YOUR TARGET",
                    fontFamily = MorInter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.14.em, color = MorColor.Eyebrow,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "Stay at", fontFamily = Cormorant,
                        fontSize = 34.sp, fontWeight = FontWeight.Medium, color = MorColor.Ink,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    Text(
                        "%.1f".format(animTarget), fontFamily = Cormorant,
                        fontSize = 84.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Ink,
                        lineHeight = 86.sp,
                    )
                    Text(
                        " kg", fontFamily = MorInter, fontSize = 20.sp,
                        color = MorColor.Muted, modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Pill(
                    "${"%,d".format(planKcal)} kcal a day",
                    bg = MorColor.Track, fg = Color(0xFF4A3F33),
                )
            } else {
                Text(
                    "BY ${horizonDate.uppercase()} YOU COULD BE",
                    fontFamily = MorInter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.14.em, color = MorColor.Eyebrow,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%.1f".format(animTarget), fontFamily = Cormorant,
                        fontSize = 84.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Ink,
                        lineHeight = 86.sp,
                    )
                    Text(
                        " kg", fontFamily = MorInter, fontSize = 20.sp,
                        color = MorColor.Muted, modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill(
                        "${if (change < 0) "−" else "+"}${"%.1f".format(abs(change))} kg",
                        bg = Color(0xFFF3E3C6), fg = Color(0xFF5E3A0E),
                    )
                    Pill(
                        "${"%,d".format(planKcal)} kcal a day",
                        bg = MorColor.Track, fg = Color(0xFF4A3F33),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (maintaining) {
            // v5 #3: a flat projection line, no pace buttons.
            Canvas(Modifier.fillMaxWidth().height(110.dp).padding(horizontal = 8.dp)) {
                val y = size.height / 2f
                drawLine(
                    MorColor.Maroon, Offset(0f, y), Offset(size.width, y), 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(MorColor.Maroon, 4.5.dp.toPx(), Offset(0f, y))
                drawCircle(Color(0xFFF7F1E6), 8.5.dp.toPx(), Offset(size.width, y))
                drawCircle(MorColor.Gold, 6.dp.toPx(), Offset(size.width, y))
            }
        } else {
            Spacer(Modifier.height(14.dp))
            // v4 #4+#6: S1 sliding-pill pace control; hidden while maintaining.
            PillSegmentedControl(
                paces.map { p ->
                    Segment(p.key, p.label, "%.2f kg/wk".format(p.rate))
                },
                selectedKey = pace.key,
                onSelect = { k ->
                    val idx = paces.indexOfFirst { it.key == k }.coerceAtLeast(0)
                    MorSound.select(idx)
                    val p = paces[idx]
                    val newTarget = (if (gain) start + p.rate * HORIZON_WEEKS else start - p.rate * HORIZON_WEEKS)
                        .coerceIn(35f, 160f)
                    draft.answers = draft.answers.copy(paceIdx = idx, targetKg = newTarget)
                    draft.save()
                },
                height = 62.dp,
                contentDescription = "Pace",
            )
            Spacer(Modifier.height(14.dp))
            WeightChart(
                start = start, targetAnim = animTarget, rateAnim = animRate, gain = gain,
                scrubX = scrubX,
                onScrub = { scrubX = it },
                onScrubEnd = { scrubX = null },
            )
            Text(
                "Drag across the chart to see any day",
                fontFamily = MorInter, fontSize = 11.5.sp, color = Color(0xFF8A7A62),
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            AnimatedContent(
                targetState = pace.guidance,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                    } else {
                        fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                    }
                },
                label = "guidance",
            ) { g ->
                Text(
                    g, fontFamily = MorInter, fontSize = 15.sp, lineHeight = (15 * 1.45).sp,
                    color = MorColor.Ink2, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    LaunchedEffect(scrubX) {
        if (scrubX != null) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }
}

/** 110dp chart with morphing pace curves, ghost lines and drag scrubbing. */
@Composable
private fun WeightChart(
    start: Float, targetAnim: Float, rateAnim: Float, gain: Boolean,
    scrubX: Float?, onScrub: (Float?) -> Unit, onScrubEnd: () -> Unit,
) {
    val chartH = 110.dp
    val density = LocalDensity.current
    var widthPx by remember { mutableStateOf(0f) }
    val minKg = if (gain) start - 1f else targetAnim - 1f
    val maxKg = if (gain) targetAnim + 2f else start + 2f
    val span = (maxKg - minKg).coerceAtLeast(1f)
    val horizonDate = LocalDateTime.now().plusDays((HORIZON_WEEKS * 7).roundToInt().toLong())
    val monthFmt = DateTimeFormatter.ofPattern("MMM")
    val dateFmt = DateTimeFormatter.ofPattern("d MMM")
    val labels = remember {
        val now = LocalDateTime.now()
        listOf(
            "Today",
            now.plusDays((HORIZON_WEEKS * 7 / 4).roundToInt().toLong()).format(monthFmt),
            now.plusDays((HORIZON_WEEKS * 7 / 2).roundToInt().toLong()).format(monthFmt),
            now.plusDays((HORIZON_WEEKS * 7 * 3 / 4).roundToInt().toLong()).format(monthFmt),
            horizonDate.format(dateFmt),
        )
    }
    val scrubValue = scrubX?.let { sx ->
        // Find t for the finger's x by binary search on the Bézier's x(t).
        val w = widthPx
        if (w <= 0f) null
        else {
            val yFn: (Float) -> Float = { v -> (maxKg - v) / span * with(density) { chartH.toPx() } }
            val path = pacePath(w, with(density) { chartH.toPx() }, start, targetAnim, rateAnim, yFn)
            val p0 = Offset(0f, yFn(start))
            val p1 = Offset(0.35f * w, yFn(start - (if (gain) 1f else -1f) * rateAnim * 4f))
            val p2 = Offset(0.65f * w, yFn(targetAnim + (if (gain) 1f else -1f) * rateAnim * 3f))
            val p3 = Offset(w, yFn(targetAnim))
            var lo = 0f; var hi = 1f
            repeat(24) {
                val mid = (lo + hi) / 2f
                val pt = bezierPoint(p0, p1, p2, p3, mid)
                if (pt.x < sx) lo = mid else hi = mid
            }
            val t = (lo + hi) / 2f
            val pt = bezierPoint(p0, p1, p2, p3, t)
            val kg = maxKg - pt.y / with(density) { chartH.toPx() } * span
            Triple(sx.coerceIn(0f, w), pt.y, kg)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(chartH)
                .pointerInput(start, gain) {
                    detectDragGestures(
                        onDragStart = { onScrub(it.x) },
                        onDrag = { c, _ -> onScrub(c.position.x) },
                        onDragEnd = onScrubEnd,
                        onDragCancel = onScrubEnd,
                    )
                }
        ) {
            Canvas(Modifier.fillMaxSize().onSizeChanged { widthPx = it.width.toFloat() }) {
                val w = size.width
                val h = size.height
                fun yFn(v: Float): Float = (maxKg - v) / span * h

                // Ghost lines for the other paces.
                val allPaces = if (gain) GAIN_PACES else PACES
                allPaces.filter { "%.2f".format(it.rate) != "%.2f".format(rateAnim) }.forEach { p ->
                    drawPath(
                        pacePath(w, h, start, paceTarget(V2Answers(weightKg = start, goal = if (gain) "gain" else "lose"), p), p.rate, ::yFn),
                        Color(0xFFE2D4BA),
                        style = Stroke(
                            1.5.dp.toPx(), cap = StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
                        ),
                    )
                }
                // Active path: maroon with a gradient fill below.
                val active = pacePath(w, h, start, targetAnim, rateAnim, ::yFn)
                val fill = Path().apply {
                    addPath(active)
                    lineTo(w, h); lineTo(0f, h); close()
                }
                drawPath(
                    fill,
                    Brush.verticalGradient(
                        listOf(MorColor.Maroon.copy(alpha = 0.12f), MorColor.Maroon.copy(alpha = 0f)),
                        startY = 0f, endY = h,
                    ),
                )
                drawPath(active, MorColor.Maroon, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                // Start + end dots.
                drawCircle(MorColor.Maroon, 4.5.dp.toPx(), Offset(0f, yFn(start)))
                drawCircle(MorColor.Gold, 6.dp.toPx(), Offset(w, yFn(targetAnim)))
                drawCircle(MorColor.Ivory, 6.dp.toPx() + 2.5.dp.toPx(), Offset(w, yFn(targetAnim)), style = Stroke(2.5.dp.toPx()))
                // Scrub hairline + tooltip dot.
                scrubValue?.let { (sx, sy, _) ->
                    drawLine(
                        MorColor.Ink.copy(alpha = 0.35f), Offset(sx, 0f), Offset(sx, h), 1.dp.toPx(),
                    )
                    drawCircle(MorColor.Maroon, 4.dp.toPx(), Offset(sx, sy))
                    drawCircle(MorColor.Ivory, 4.dp.toPx() + 2.dp.toPx(), Offset(sx, sy), style = Stroke(2.dp.toPx()))
                }
            }
            scrubValue?.let { (sx, _, kg) ->
                val date = LocalDateTime.now().plusDays(
                    (HORIZON_WEEKS * 7 * (sx / widthPx.coerceAtLeast(1f))).roundToInt().toLong()
                ).format(dateFmt)
                val text = "$date · ${"%.1f".format(kg)} kg"
                val tooltipW = with(density) { 150.dp.toPx() }
                val tx = (sx - tooltipW / 2f).coerceIn(0f, widthPx - tooltipW)
                Box(
                    Modifier
                        .offset(x = with(density) { tx.toDp() }, y = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MorColor.Ink.copy(alpha = 0.92f))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(text, fontFamily = MorInter, fontSize = 11.sp, color = MorColor.Ivory)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            labels.forEach { l ->
                Text(l, fontFamily = MorInter, fontSize = 11.sp, color = Color(0xFF7D6F5A))
            }
        }
    }
}

@Composable
private fun ActivityStep(draft: V2Draft, dir: Int, onNext: () -> Unit) {
    OnboardingScaffold(
        step = 5, artKey = "activity",
        bubble = "Almost there. How do your days go?",
        title = "How active are you?",
        primaryText = "Continue", primaryEnabled = draft.answers.activity.isNotEmpty(),
        primaryDisabledText = "Choose one to continue",
        dir = dir, onPrimary = onNext,
    ) {
        OptionGroup(ACT_OPTS, actKey(draft.answers.activity)) { k ->
            MorSound.select(ACT_OPTS.indexOfFirst { it.key == k }.coerceAtLeast(0))
            draft.answers = draft.answers.copy(activity = actVal(k))
            draft.save()
        }
    }
}

@Composable
private fun MealsStep(draft: V2Draft, dir: Int, onNext: () -> Unit) {
    OnboardingScaffold(
        step = 6, artKey = "meals",
        bubble = "When do you usually eat?",
        title = "Your meal times",
        primaryText = "Build my plan", primaryEnabled = true,
        dir = dir, onPrimary = onNext,
    ) {
        MealsBody(draft)
    }
}

@Composable
private fun PlanStep(plan: Plan, draft: V2Draft, onStart: () -> Unit) {
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
        OnboardingScaffold(
            step = 6, artKey = "plan",
            bubble = "Built around your plate.",
            title = "Your plan is ready!",
            primaryText = "Start", primaryEnabled = true,
            dir = 1, onPrimary = onStart,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MorShape.Card))
                    .background(MorColor.Card)
                    .border(1.dp, MorColor.CardBorder, RoundedCornerShape(MorShape.Card))
                    .padding(16.dp, 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "DAILY TARGET", fontFamily = MorInter, fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em,
                    color = MorColor.Muted,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "%,d".format(plan.kcal_target), fontFamily = Cormorant,
                        fontSize = 54.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Maroon,
                        lineHeight = 56.sp,
                    )
                    Text(
                        " kcal", fontFamily = MorInter, fontSize = 20.sp, color = MorColor.Muted,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
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
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Pill("$line · by ${plan.target_date?.let { prettyDate(it) } ?: ""}", bg = MorColor.Track, fg = MorColor.Maroon)
                    }
                }
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
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, fontFamily = MorInter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Ink, modifier = Modifier.width(62.dp))
        Box(Modifier.weight(1f).height(9.dp).clip(RoundedCornerShape(5.dp)).background(MorColor.Track)) {
            Box(
                Modifier.fillMaxWidth(frac * shown).height(9.dp)
                    .clip(RoundedCornerShape(5.dp)).background(color)
            )
        }
        Text(
            "${grams.roundToInt()} g", fontFamily = MorInter, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, color = MorColor.Ink, textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp),
        )
    }
}

/** Small confetti burst for the plan reveal (two volleys, Mor palette colors). */
@Composable
private fun Confetti(t: Float) {
    if (t <= 0f) return
    Canvas(Modifier.fillMaxSize()) {
        val colors = listOf(MorColor.Maroon, MorColor.Gold, MorColor.GoldLight, Neel, Genda)
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

/** "You're all set" — the final onboarding step (07-plan-ready-flow). */
@Composable
private fun AllSetScreen(nav: androidx.navigation.NavController, vm: AppVm, draft: V2Draft) {
    OnboardingScaffold(
        step = 6, artKey = "allset",
        bubble = "Welcome to Mor.",
        title = "You’re all set",
        subtitle = "Welcome to Mor. Let’s eat well.",
        primaryText = "Go to Home", primaryEnabled = true,
        dir = 1, onPrimary = {
            vm.refreshProfile()
            vm.refreshHome()
            // popUpTo the graph root, so Back from Home exits the app.
            nav.navigate("home") { popUpTo("boot") { inclusive = true } }
            draft.clear()
            MorSound.done()
        },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MorShape.Card))
                .background(MorColor.Card)
                .border(1.dp, MorColor.CardBorder, RoundedCornerShape(MorShape.Card))
                .padding(14.dp, 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(20.dp)) {
                drawCircle(MorColor.Muted, 3.dp.toPx(), Offset(center.x, center.y - 5.dp.toPx()))
                drawLine(MorColor.Muted, Offset(center.x, center.y - 1.dp.toPx()), Offset(center.x, center.y + 6.dp.toPx()), 2.dp.toPx(), cap = StrokeCap.Round)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                "Meal reminders are off. You can turn them on from settings.",
                fontFamily = MorInter, fontSize = 13.5.sp, color = MorColor.Ink2,
            )
        }
    }
}

/* meal times body (restyled to the palace sheet) -------------------------- */

@Composable
private fun MealsBody(draft: V2Draft) {
    val a = draft.answers
    val view = androidx.compose.ui.platform.LocalView.current
    var editing by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("breakfast", "lunch", "snack", "dinner").forEach { s ->
            Row(
                Modifier
                    .clip(RoundedCornerShape(MorShape.Tile))
                    .background(MorColor.Card)
                    .border(1.dp, MorColor.SheetBorder, RoundedCornerShape(MorShape.Tile))
                    .padding(12.dp, 8.dp, 12.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SlotIcon(s)
                Spacer(Modifier.width(12.dp))
                Text(
                    s.replaceFirstChar { it.uppercase() }, fontFamily = MorInter,
                    fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Ink,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .border(1.dp, Color(0xFFD9C9AA), RoundedCornerShape(20.dp))
                        .background(MorColor.Ivory)
                        .clickable { editing = s }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    Text(
                        fmtTime(a.times[s] ?: "08:30"), fontFamily = MorInter,
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Maroon,
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
                        checkedTrackColor = MorColor.Maroon,
                        uncheckedTrackColor = Color(0xFFD9C9AA),
                        checkedThumbColor = MorColor.CreamText,
                        uncheckedThumbColor = MorColor.Card,
                    ),
                )
            }
        }
        Text(
            "Neelu can nudge you at these times once you turn reminders on.",
            fontFamily = MorInter, fontSize = 13.sp, color = MorColor.Muted,
            modifier = Modifier.padding(4.dp, 2.dp),
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
            onDismiss = { editing = null },
        )
    }
}

@Composable
private fun TimeSheet(
    slot: String, initial: String,
    onSave: (String) -> Unit, onDismiss: () -> Unit,
) {
    var pick by remember { mutableStateOf(initial) }
    val st = rememberTimePickerState(
        initialHour = initial.substringBefore(":").toIntOrNull() ?: 8,
        initialMinute = initial.substringAfter(":").toIntOrNull() ?: 30,
        is24Hour = true,
    )
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MorColor.Ivory) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Edit $slot", fontFamily = Cormorant, fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold, color = MorColor.Ink,
                modifier = Modifier.align(Alignment.Start).padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.align(Alignment.Start).padding(horizontal = 20.dp),
            ) {
                (V2_QUICK[slot] ?: emptyList()).forEach { t ->
                    TimeChip(t, t == pick) { pick = t }
                }
            }
            Spacer(Modifier.height(12.dp))
            TimePicker(state = st)
            Spacer(Modifier.height(8.dp))
            ObPrimaryButton("Save", enabled = true) {
                onSave("%02d:%02d".format(st.hour, st.minute))
            }
        }
    }
}

@Composable
private fun TimeChip(text: String, on: Boolean, onClick: () -> Unit) {
    val view = androidx.compose.ui.platform.LocalView.current
    Box(
        Modifier
            .height(40.dp).clip(RoundedCornerShape(20.dp))
            .background(if (on) MorColor.Maroon else MorColor.Card)
            .border(1.dp, if (on) MorColor.Maroon else Color(0xFFD9C9AA), RoundedCornerShape(20.dp))
            .clickable { MorSound.ensure(view.context); MorSound.select(1); onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text, fontFamily = MorInter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (on) MorColor.CreamText else MorColor.Ink,
        )
    }
}

@Composable
private fun SlotIcon(slot: String) {
    Canvas(Modifier.size(34.dp)) {
        when (slot) {
            "breakfast" -> {
                drawCircle(MorColor.Gold, 9.4.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                (0..2).forEach { k ->
                    drawLine(
                        Color(0xFFB98A55), Offset(center.x - 6.dp.toPx(), center.y + (k - 1) * 3.dp.toPx()),
                        Offset(center.x + 6.dp.toPx(), center.y + (k - 1) * 3.dp.toPx()), 1.dp.toPx(),
                    )
                }
            }
            "lunch" -> {
                drawCircle(MorColor.Gold, 9.4.dp.toPx(), center)
                drawCircle(MorColor.GoldPale, 2.6.dp.toPx(), Offset(center.x - 3.dp.toPx(), center.y - 2.4.dp.toPx()))
                drawCircle(MorColor.Ivory, 2.6.dp.toPx(), Offset(center.x + 3.dp.toPx(), center.y - 2.4.dp.toPx()))
                drawCircle(Color(0xFFD9B48A), 2.6.dp.toPx(), Offset(center.x, center.y + 2.6.dp.toPx()))
            }
            "snack" -> {
                drawCircle(Color(0xFF8A5A2B), 5.5.dp.toPx(), Offset(center.x - 0.5.dp.toPx(), center.y + 1.dp.toPx()))
                drawCircle(MorColor.Ivory, 5.dp.toPx(), Offset(center.x - 0.5.dp.toPx(), center.y - 2.dp.toPx()))
                drawLine(Color(0xFF9B9B9B), Offset(center.x - 4.dp.toPx(), center.y - 9.dp.toPx()), Offset(center.x - 6.dp.toPx(), center.y - 4.dp.toPx()), 0.9.dp.toPx())
                drawLine(Color(0xFF9B9B9B), Offset(center.x + 3.dp.toPx(), center.y - 9.dp.toPx()), Offset(center.x + 5.dp.toPx(), center.y - 4.dp.toPx()), 0.9.dp.toPx())
            }
            else -> {
                drawCircle(MorColor.GoldLight, 8.dp.toPx(), Offset(center.x, center.y + 1.dp.toPx()))
                drawCircle(MorColor.GoldPale, 8.dp.toPx(), Offset(center.x, center.y - 1.dp.toPx()))
                drawCircle(MorColor.Gold, 0.9.dp.toPx(), Offset(center.x - 2.dp.toPx(), center.y - 0.6.dp.toPx()))
                drawCircle(MorColor.Gold, 0.9.dp.toPx(), Offset(center.x + 2.dp.toPx(), center.y + 0.2.dp.toPx()))
            }
        }
    }
}

/* building (unchanged flow) ---------------------------------------------- */

internal fun v2CommitMap(draft: V2Draft): Map<String, Any?> {
    val a = draft.answers
    val meals = a.times.filterKeys { a.mealOn[it] == true }
    return mapOf(
        "goal" to a.goal, "diet" to a.diet, "sex" to a.sex, "age" to a.age,
        "height_cm" to a.heightCm, "weight_kg" to a.weightKg, "target_weight_kg" to a.targetKg,
        "activity" to a.activity, "meal_times" to meals, "complete" to true,
        // v4 07: the reminder question is gone, so reminders default to off.
        "reminders_on" to false,
    )
}

@Composable
fun V2BuildingScreen(
    draft: V2Draft, vm: AppVm,
    onPlan: (Plan?) -> Unit, advance: () -> Unit,
) {
    val a = draft.answers
    // v4 #12: all strings come from the saved goal and diet through one mapping.
    val lines = planChecklist(a.diet, a.goal)
    var attempt by remember { mutableIntStateOf(0) }
    var progress by remember(attempt) { mutableIntStateOf(0) }
    var doneCount by remember(attempt) { mutableStateOf(-1) }
    var failed by remember(attempt) { mutableStateOf(false) }
    var planReady by remember(attempt) { mutableStateOf(false) }
    val planDef = remember(attempt) { CompletableDeferred<Plan?>() }
    val reduced = rememberReducedMotion()
    // v4 #3+8: the video is Neelu now — no mascot illustration, and the video
    // aims at the band above the checklist.
    val density = LocalDensity.current
    val sheetTop = LocalV2SheetTop.current
    // v5 #4: the palace runs behind the whole building screen — no black band.
    val fullHeight = LocalConfiguration.current.screenHeightDp.dp
    LaunchedEffect(Unit) { sheetTop.value = fullHeight }

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
                progress = (((i + 1) / lines.size.toFloat()) * 92).roundToInt()
                doneCount = i
                MorSound.select(i)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        V2Top("building", draft)
        Box(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 4.dp),
        ) {
            BubbleHeading(
                "Building your plan…",
                planSubtitle(a.diet, a.goal),
            )
        }
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                lines.forEachIndexed { i, line ->
                    if (i > doneCount) return@forEachIndexed
                    Row(
                        Modifier
                            .shadow(2.dp, RoundedCornerShape(18.dp), spotColor = Ink.copy(alpha = 0.06f))
                            .clip(RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp))
                            .background(MorColor.Card)
                            .padding(10.dp, 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusDot(if (failed && i == lines.lastIndex) "fail" else "done")
                        Spacer(Modifier.width(10.dp))
                        Text(
                            line + if (failed && i == lines.lastIndex) " — that took too long." else "",
                            fontFamily = MorBody, fontSize = 15.sp, color = Ink,
                        )
                    }
                }
            }
            LinearProgressIndicator(
                progress = { progress / 100f },
                color = MorColor.Maroon, trackColor = MorColor.Track,
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            )
        }
        if (failed) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp)
                    .navigationBarsPadding().padding(bottom = 22.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    V2Button("Try again") { attempt += 1 }
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
private fun V2Button(
    text: String, enabled: Boolean = true, onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Teal,
            contentColor = Color.White,
            disabledContainerColor = Disabled,
            disabledContentColor = Color(0xFF5A564D),
        ),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, fontFamily = MorBody, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
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
    "journey", "goal", "diet", "body", "target", "activity", "meals", "building", "plan", "allset",
)

/** Route host: step system after the palace door; parallel enter/exit spring
 *  transitions; pose hold. */
@Composable
fun V2Onboarding(nav: androidx.navigation.NavController, vm: AppVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val draft = remember { V2Draft(ctx).apply { load() } }
    var step by remember { mutableStateOf(if (draft.stepKey in V2_ORDER) draft.stepKey else "journey") }
    var dir by remember { mutableIntStateOf(1) }
    var busy by remember { mutableStateOf(false) }
    var planShown by remember { mutableStateOf<Plan?>(null) }
    val reduced = rememberReducedMotion()
    val density = LocalDensity.current

    fun go(d: Int) {
        if (busy) return
        val i = V2_ORDER.indexOf(step) + d
        var j = i.coerceIn(0, V2_ORDER.lastIndex)
        if (d < 0 && V2_ORDER[j] == "building") j-- // back from Plan never re-runs Building
        // v4 #5: every goal reaches the target-weight step; no skipping.
        if (j == V2_ORDER.indexOf(step)) return
        busy = true
        dir = d
        scope.launch {
            MorSound.ensure(ctx)
            if (d > 0) MorSound.next() else MorSound.back()
            PoseArt.await(V2_NEELU[V2_ORDER[j]] ?: "happy")
            draft.stepKey = V2_ORDER[j]
            draft.save()
            step = V2_ORDER[j]
            busy = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Chandan)) {
        // v4 #3+8: one looping Red Court player for the whole onboarding graph,
        // behind every step; steps never restart or reload it.
        val videoPlayer = rememberNeeluPlayer()
        val sheetTop = remember { mutableStateOf(0.dp) }
        val bandTop = with(density) {
            WindowInsets.statusBars.getTop(density).toDp()
        }
        CompositionLocalProvider(LocalV2SheetTop provides sheetTop) {
            NeeluVideoBackground(
                videoPlayer, maxWidth, maxHeight, bandTop,
                // v5 #4: the building screen has no ivory sheet — run the palace
                // behind the whole screen instead of leaving a black band.
                if (step == "building") maxHeight else sheetTop.value,
                Modifier.fillMaxSize(),
            )
            // System back walks the reverse direction through the steps.
            BackHandler(enabled = step !in listOf("journey", "allset") && !busy) { go(-1) }
            AnimatedContent(
            targetState = step,
            contentKey = { it },
            transitionSpec = {
                if (reduced) {
                    (fadeIn(tween(180)) togetherWith fadeOut(tween(150)))
                } else {
                    val enter = fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                        slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { full -> (full * 0.135f).toInt() * dir }
                    enter togetherWith
                        (fadeOut(tween(200, easing = ExitEasing)) + slideOutHorizontally(tween(200, easing = ExitEasing)) { full -> (-full * 0.1f).toInt() * dir })
                }
            },
            label = "v2journey",
        ) { s ->
            Box(Modifier.fillMaxSize()) {
                when (s) {
                    "journey" -> WelcomeJourney(draft, videoPlayer) { go(1) }
                    "goal" -> GoalStep(draft, dir) { go(1) }
                    "diet" -> DietStep(draft, dir) { go(1) }
                    "body" -> BodyStep(draft, dir) { go(1) }
                    "target" -> TargetStep(draft, dir) { go(1) }
                    "activity" -> ActivityStep(draft, dir) { go(1) }
                    "meals" -> MealsStep(draft, dir) { go(1) }
                    "building" -> V2BuildingScreen(
                        draft, vm,
                        onPlan = { planShown = it },
                        advance = { go(1) },
                    )
                    "plan" -> PlanStep(planShown ?: Plan(), draft) { go(1) }
                    "allset" -> AllSetScreen(nav, vm, draft)
                }
            }
        }
        }
    }
}
