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
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.TransformOrigin
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

/** Issue 13: one date style across target and plan ("12 Mar"). */
private fun shortPlanDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return runCatching {
        java.time.LocalDate.parse(iso)
            .format(DateTimeFormatter.ofPattern("d MMM", java.util.Locale.ENGLISH))
    }.getOrDefault(iso)
}

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
    OptionSpec("lose", "Lose weight", "A gentle deficit", R.drawable.ic_opt_lose, R.drawable.ic_opt_lose_selected, R.drawable.avd_opt_lose),
    OptionSpec("maintain", "Maintain weight", "Hold where you are", R.drawable.ic_opt_maintain, R.drawable.ic_opt_maintain_selected, R.drawable.avd_opt_maintain),
    OptionSpec("gain", "Gain weight", "A steady surplus", R.drawable.ic_opt_gain, R.drawable.ic_opt_gain_selected, R.drawable.avd_opt_gain),
    OptionSpec("healthy", "Eat healthier", "Quality first", R.drawable.ic_opt_healthy, R.drawable.ic_opt_healthy_selected, R.drawable.avd_opt_healthy),
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
private fun WheelCard(
    spec: WheelSpec, onValue: (Float) -> Unit, onTapCentre: () -> Unit,
    showTypeHint: Boolean = true,
) {
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
        if (showTypeHint) {
            Text(
                "Tap to type", fontFamily = MorInter, fontSize = 11.sp,
                color = Color(0xFF8A7A62),
            )
        }
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

/* next-version flow (pack S2) -------------------------------------------- */
// About you first, then Objective, then Target only for reduce/gain, then
// Diet. Activity and Meals stay after Diet because the plan maths uses them.
private val FLOW_BASE = listOf("body", "goal", "target", "diet", "activity", "meals")
private val V2_STEP_KEYS = FLOW_BASE.toSet()

private fun flowFor(goal: String): List<String> =
    FLOW_BASE.filter { it != "target" || (goal != "maintain" && goal != "healthy") }

private fun stepNoFor(key: String, goal: String): Int =
    (flowFor(goal).indexOf(key) + 1).coerceAtLeast(1)

private fun totalFor(goal: String): Int = flowFor(goal).size

@Composable
private fun GoalStep(draft: V2Draft, dir: Int, onNext: () -> Unit) {
    OnboardingScaffold(
        step = stepNoFor("goal", draft.answers.goal),
        total = totalFor(draft.answers.goal),
        bubble = "Let’s start with the big why.",
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
        step = stepNoFor("diet", draft.answers.goal),
        total = totalFor(draft.answers.goal),
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
        step = stepNoFor("body", a.goal), total = totalFor(a.goal),
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
private const val BUILDING_CARD_FRAC = 0.68f

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
    val gain = a.goal == "gain"
    val paces = if (gain) GAIN_PACES else PACES
    val pace = paces[a.paceIdx.coerceIn(0, paces.lastIndex)]
    val start = a.weightKg
    val projected = (if (gain) start + pace.rate * HORIZON_WEEKS else start - pace.rate * HORIZON_WEEKS)
        .coerceIn(35f, 160f)

    // One date for the headline, the chart axis and the stored target.
    val horizon = LocalDateTime.now().plusDays((HORIZON_WEEKS * 7).roundToInt().toLong())
    val horizonDate = horizon.format(DateTimeFormatter.ofPattern("d MMM"))
    val axis = remember {
        val now = LocalDateTime.now()
        val m = DateTimeFormatter.ofPattern("MMM")
        listOf(
            "Today",
            now.plusDays(28).format(m),
            now.plusDays(56).format(m),
            now.plusDays(84).format(m),
            horizon.format(DateTimeFormatter.ofPattern("d MMM")),
        )
    }

    LaunchedEffect(Unit) {
        // Issue 13: one decimal before it is stored or sent; the server used to
        // print three (70.102 kg) because this value went over unrounded.
        val rounded = (projected * 10f).roundToInt() / 10f
        draft.answers = draft.answers.copy(targetKg = rounded)
        draft.save()
    }

    val animTarget by animateFloatAsState(
        projected, tween(if (reduced) 1 else 450, easing = CubicOut), label = "targetNum",
    )
    val minKcal = if (a.sex == "male") 1500 else 1200
    val planKcal = (((tdeeOf(a).roundToInt() + pace.dailyDelta) + 5) / 10 * 10)
        .coerceAtLeast(minKcal)
    val change = projected - start
    // Issue 5: the curve's end point follows the chosen pace (0.18..0.54 kg/wk).
    val curveFrac by animateFloatAsState(
        (pace.rate / 0.54f).coerceIn(0f, 1f),
        tween(if (reduced) 1 else 300, easing = CubicOut),
        label = "curveFrac",
    )

    OnboardingScaffold(
        step = stepNoFor("target", a.goal), total = totalFor(a.goal),
        bubble = "Three paces. Pick what feels right.",
        title = "Set your target weight",
        widePadding = true,
        primaryText = "Continue", primaryEnabled = true,
        dir = dir, onPrimary = onNext,
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "BY ${horizonDate.uppercase()} YOU COULD BE",
                fontFamily = MorInter, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp, color = MorColor.GoldText,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    "%.1f".format(animTarget), fontFamily = Cormorant,
                    fontSize = 48.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Ink,
                    lineHeight = 50.sp,
                )
                Text(
                    " kg", fontFamily = MorInter, fontSize = 16.sp,
                    color = MorColor.Muted, modifier = Modifier.padding(bottom = 7.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill(
                    "${if (change < 0) "\u2212" else "+"}${"%.1f".format(abs(change))} kg",
                    bg = Color(0xFFF1E4D3), fg = Color(0xFF5E3A0E),
                )
                Pill(
                    "${"%,d".format(planKcal)} kcal a day",
                    bg = MorColor.Track, fg = Color(0xFF4A3F33),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                paces.forEach { p ->
                    val on = p.key == pace.key
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) MorColor.Maroon else Color(0xFFF1E4D3))
                            .clickable {
                                val idx = paces.indexOf(p).coerceAtLeast(0)
                                MorSound.select(idx)
                                val nt = (if (gain) start + p.rate * HORIZON_WEEKS
                                else start - p.rate * HORIZON_WEEKS).coerceIn(35f, 160f)
                                draft.answers = draft.answers.copy(
                                    paceIdx = idx,
                                    targetKg = (nt * 10f).roundToInt() / 10f,
                                )
                                draft.save()
                            }
                            .padding(vertical = 9.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            p.label, fontFamily = MorInter, fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (on) Color(0xFFFFFDF6) else MorColor.Ink,
                        )
                        Text(
                            "${"%.2f".format(p.rate)} kg/wk ${if (gain) "gain" else "loss"}",
                            fontFamily = MorInter, fontSize = 10.sp,
                            color = if (on) Color(0xFFFFFDF6).copy(alpha = 0.85f) else MorColor.Muted,
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            SpecCurve(gain, curveFrac)
            Row(
                Modifier.fillMaxWidth().padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                axis.forEach { l ->
                    Text(l, fontFamily = MorInter, fontSize = 10.sp, color = Color(0xFF8A7F68))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Illustrative figures only.", fontFamily = MorInter, fontSize = 10.sp,
                color = MorColor.NoteFaint,
            )
        }
    }
}

/** The approved target curve (viewBox 220x84), now pace-aware (issue 5):
 *  `frac` 0.33 / 0.67 / 1.0 for Gradual / Steady / Faster. Gain rises, loss falls. */
@Composable
private fun SpecCurve(gain: Boolean, frac: Float) {
    Canvas(Modifier.fillMaxWidth().height(84.dp)) {
        val sx = size.width / 220f
        val sy = size.height / 84f
        val startY = if (gain) 54f else 12f
        val endY = if (gain) 54f - 42f * frac else 12f + 42f * frac
        val d = endY - startY
        val path = Path().apply {
            moveTo(14f * sx, startY * sy)
            cubicTo(
                70f * sx, (startY + d * 0.12f) * sy,
                150f * sx, (startY + d * 0.70f) * sy,
                206f * sx, endY * sy,
            )
        }
        drawPath(path, MorColor.Maroon, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(MorColor.Maroon, 4.dp.toPx(), Offset(14f * sx, startY * sy))
        drawCircle(MorColor.Gold, 4.dp.toPx(), Offset(206f * sx, endY * sy))
    }
}

@Composable
private fun ActivityStep(draft: V2Draft, dir: Int, onNext: () -> Unit) {
    OnboardingScaffold(
        step = stepNoFor("activity", draft.answers.goal), total = totalFor(draft.answers.goal),
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
        step = stepNoFor("meals", draft.answers.goal), total = totalFor(draft.answers.goal),
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
            step = totalFor(draft.answers.goal), total = totalFor(draft.answers.goal),
            bubble = "Built around your plate.",
            title = "Your plan is ready!",
            primaryText = "Go to Home", primaryEnabled = true,
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
                // Issue 13: no goal pill when there is no weight change; the
                // date uses the same short style as the target screen.
                val g = draft.answers.goal
                val line = plan.goal_line
                if (line != null && (g == "lose" || g == "gain")) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    ) {
                        Pill("$line · by ${shortPlanDate(plan.target_date)}", bg = MorColor.Track, fg = MorColor.Maroon)
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
    // Issue 11: at t=1 every dot must be gone. The old denominator (/2) left
    // ~57% of the dots frozen on screen forever.
    if (t <= 0f || t >= 1f) return
    Canvas(Modifier.fillMaxSize()) {
        val colors = listOf(MorColor.Maroon, MorColor.Gold, MorColor.GoldLight, Neel, Genda)
        val r = size
        fun burst(seed: Int, count: Int, spread: Float, vel: Float, at: Float, scale: Float) {
            val rel = ((t - at) / (1f - at)).coerceIn(0f, 1f)
            if (rel <= 0f || rel >= 1f) return
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
    // Issue 9 (settled): option B — the About roller, reused as three WheelCards.
    // One source of truth; Save converts once to 24h HH:MM.
    val h24 = initial.substringBefore(":").toIntOrNull()?.coerceIn(0, 23) ?: 8
    val m0 = initial.substringAfter(":").toIntOrNull()?.coerceIn(0, 59) ?: 30
    var hour12 by remember { mutableIntStateOf(if (h24 % 12 == 0) 12 else h24 % 12) }
    var minute by remember { mutableIntStateOf(m0) }
    var pm by remember { mutableIntStateOf(if (h24 >= 12) 1 else 0) }
    var keypad by remember { mutableStateOf<String?>(null) }

    val hourItems = (1..12).map { WheelItem(it.toFloat(), "$it", "hr") }
    val minuteItems = (0..59).map { WheelItem(it.toFloat(), "%02d".format(it), "min") }
    val ampmItems = listOf(WheelItem(0f, "AM", ""), WheelItem(1f, "PM", ""))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MorColor.Ivory,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
        ),
    ) {
        Column(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Edit $slot", fontFamily = Cormorant, fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold, color = MorColor.Ink,
                modifier = Modifier.align(Alignment.Start).padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(Modifier.weight(1f)) {
                    WheelCard(
                        WheelSpec(
                            "Hour", hourItems, hour12 - 1, "Hour", null, null,
                            false, 1f..12f, "1–12",
                        ),
                        onValue = { hour12 = it.roundToInt().coerceIn(1, 12) },
                        onTapCentre = { keypad = "hour" },
                    )
                }
                Box(Modifier.weight(1f)) {
                    WheelCard(
                        WheelSpec(
                            "Minute", minuteItems, minute, "Minute", null, null,
                            false, 0f..59f, "0–59",
                        ),
                        onValue = { minute = it.roundToInt().coerceIn(0, 59) },
                        onTapCentre = { keypad = "minute" },
                    )
                }
                Box(Modifier.weight(1f)) {
                    WheelCard(
                        WheelSpec(
                            "AM/PM", ampmItems, pm, "AM or PM", null, null,
                            false, 0f..1f, "AM or PM",
                        ),
                        onValue = { pm = if (it >= 0.5f) 1 else 0 },
                        onTapCentre = { },
                        showTypeHint = false,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 20.dp)) {
                ObPrimaryButton("Save", enabled = true) {
                    val out = (hour12 % 12) + if (pm == 1) 12 else 0
                    onSave("%02d:%02d".format(out, minute))
                }
            }
        }
    }

    when (keypad) {
        "hour" -> ObKeypad(
            "Enter hour", "$hour12", "hr", false, 1f..12f, "1–12",
            onSet = { hour12 = it.roundToInt().coerceIn(1, 12) },
            onDismiss = { keypad = null },
        )
        "minute" -> ObKeypad(
            "Enter minute", "%02d".format(minute), "min", false, 0f..59f, "0–59",
            onSet = { minute = it.roundToInt().coerceIn(0, 59) },
            onDismiss = { keypad = null },
        )
    }
}

@Composable
private fun SlotIcon(slot: String) {
    // Issue 6 (settled): the Snack row uses the chai cup from the mock. The other
    // three meal icons are untouched.
    if (slot == "snack") {
        Box(Modifier.size(34.dp), contentAlignment = Alignment.Center) {
            Image(
                painterResource(R.drawable.ic_snack_chai), null,
                Modifier.size(28.dp),
            )
        }
        return
    }
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
    // Issue 13: Maintain and Eat healthier skip the Target step, so the draft
    // still holds an old target. Send a clean value instead of a stale one:
    // maintain = current weight, healthier = no target at all.
    val target: Any? = when (a.goal) {
        "maintain" -> a.weightKg
        "healthy" -> null
        else -> (a.targetKg * 10f).roundToInt() / 10f
    }
    return mapOf(
        "goal" to a.goal, "diet" to a.diet, "sex" to a.sex, "age" to a.age,
        "height_cm" to a.heightCm, "weight_kg" to a.weightKg, "target_weight_kg" to target,
        "activity" to a.activity, "meal_times" to meals, "complete" to true,
        // next: the Meal times step owns this now — on when any slot is on.
        "reminders_on" to a.mealOn.values.any { it },
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

    Box(Modifier.fillMaxSize().background(MorColor.FrameCream)) {
        Image(
            painterResource(R.drawable.court), null,
            Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        )
        // Issue 10 (assumption): card centre at 68% of screen height, one constant.
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val frac = BUILDING_CARD_FRAC
            Column(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = maxHeight * (frac - 0.5f))
                    .padding(horizontal = 20.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MorColor.ScreenCream.copy(alpha = 0.97f))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                ) {
                    Text(
                        "Building your plan…", fontFamily = Cormorant, fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold, color = MorColor.Ink,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        lines.forEachIndexed { i, line ->
                            if (i <= doneCount) {
                                CascadeRow(
                                    line + if (failed && i == lines.lastIndex) " — that took too long." else "",
                                    failed && i == lines.lastIndex,
                                )
                            }
                        }
                    }
                }
                if (failed) {
                    Spacer(Modifier.height(14.dp))
                    ObPrimaryButton("Try again", enabled = true) { attempt += 1 }
                }
            }
        }
    }
}

@Composable
private fun CascadeRow(text: String, fail: Boolean) {
    val reduced = rememberReducedMotion()
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(
            1f, tween(if (reduced) 1 else 450, easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1.2f)),
        )
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 8.dp.toPx()
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(20.dp)) {
            drawCircle(if (fail) Kumkum else MorColor.Fat, 10.dp.toPx(), center)
            if (!fail) {
                drawLine(Color.White, Offset(center.x - 5.dp.toPx(), center.y + 0.5.dp.toPx()),
                    Offset(center.x - 1.dp.toPx(), center.y + 4.5.dp.toPx()), 2.2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(Color.White, Offset(center.x - 1.dp.toPx(), center.y + 4.5.dp.toPx()),
                    Offset(center.x + 5.dp.toPx(), center.y - 4.dp.toPx()), 2.2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(text, fontFamily = MorInter, fontSize = 13.sp, color = MorColor.Ink)
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

/** The peel-with-curl easing and transform (pack A1 / S7). */
private val PeelEase = CubicBezierEasing(0.45f, 0f, 0.65f, 0.4f)

private fun Modifier.peelOut(p: Float): Modifier = this.graphicsLayer {
    val tip = 0.35f
    val ty = if (p <= tip) (p / tip) * 0.12f
    else 0.12f + ((p - tip) / (1f - tip)) * 0.94f
    val rot = if (p <= tip) -2.5f * (p / tip)
    else -2.5f - 2.5f * ((p - tip) / (1f - tip))
    transformOrigin = TransformOrigin(0.12f, 1f)
    translationY = size.height * ty
    rotationZ = rot
    alpha = 1f - 0.1f * p
    shadowElevation = 18.dp.toPx()
    shape = RoundedCornerShape(20.dp)
    clip = false
}

@Composable
private fun V2Screen(
    key: String, draft: V2Draft, vm: AppVm, nav: androidx.navigation.NavController,
    plan: Plan?, onPlan: (Plan?) -> Unit, onNext: () -> Unit,
) {
    when (key) {
        "journey" -> WelcomeJourney(draft) { onNext() }
        "body" -> BodyStep(draft, 1, onNext)
        "goal" -> GoalStep(draft, 1, onNext)
        "target" -> TargetStep(draft, 1, onNext)
        "diet" -> DietStep(draft, 1, onNext)
        "activity" -> ActivityStep(draft, 1, onNext)
        "meals" -> MealsStep(draft, 1, onNext)
        "building" -> V2BuildingScreen(draft, vm, onPlan = onPlan, advance = onNext)
        "plan" -> PlanStep(plan ?: Plan(), draft) {
            // Issue 12: the plan screen is the last step; it goes straight Home.
            vm.refreshProfile()
            vm.refreshHome()
            nav.navigate("home") { popUpTo("boot") { inclusive = true } }
            draft.clear()
            MorSound.done()
        }
    }
}

/** Route host: palace intro, then the pack step cards, then building / plan /
 *  all-set. Step-to-step moves use the approved peel-with-curl; the incoming
 *  card is mounted first and never moves. */
@Composable
fun V2Onboarding(nav: androidx.navigation.NavController, vm: AppVm) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val draft = remember { V2Draft(ctx).apply { load() } }
    val reduced = rememberReducedMotion()
    val density = LocalDensity.current

    fun routeList(): List<String> =
        listOf("journey") + flowFor(draft.answers.goal) + listOf("building", "plan")

    var step by remember {
        mutableStateOf(routeList().let { if (draft.stepKey in it) draft.stepKey else "journey" })
    }
    var busy by remember { mutableStateOf(false) }
    var planShown by remember { mutableStateOf<Plan?>(null) }
    var leaving by remember { mutableStateOf<String?>(null) }
    val peel = remember { Animatable(1f) }

    fun go(d: Int) {
        if (busy) return
        val keys = routeList()
        val i = keys.indexOf(step)
        if (i < 0) return
        var j = (i + d).coerceIn(0, keys.lastIndex)
        // Back from Plan never re-runs Building.
        if (d < 0 && keys[j] == "building") j--
        val next = keys[j]
        if (next == step) return

        val animatePeel = step in V2_STEP_KEYS && next in V2_STEP_KEYS && !reduced
        MorSound.ensure(ctx)
        if (d > 0) MorSound.next() else MorSound.back()

        leaving = if (animatePeel) step else null
        step = next
        draft.stepKey = next
        draft.save()

        if (animatePeel) {
            busy = true
            scope.launch {
                peel.snapTo(0f)
                peel.animateTo(1f, tween(550, easing = PeelEase))
                leaving = null
                busy = false
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(MorColor.FrameCream)) {
        val videoPlayer = rememberNeeluPlayer()
        val sheetTop = remember { mutableStateOf(0.dp) }
        val bandTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
        CompositionLocalProvider(LocalV2SheetTop provides sheetTop) {
            NeeluVideoBackground(
                videoPlayer, maxWidth, maxHeight, bandTop,
                if (step == "building") maxHeight else sheetTop.value,
                Modifier.fillMaxSize(),
            )
            // System back walks the reverse direction through the steps.
            BackHandler(enabled = step != "journey" && !busy) { go(-1) }
            Box(Modifier.fillMaxSize()) {
                V2Screen(step, draft, vm, nav, planShown, { planShown = it }) { go(1) }
                leaving?.let { old ->
                    Box(Modifier.fillMaxSize().peelOut(peel.value)) {
                        V2Screen(old, draft, vm, nav, planShown, { planShown = it }) { go(1) }
                    }
                }
            }
        }
    }
}
