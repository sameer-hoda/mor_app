package dev.mor.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import dev.mor.app.data.AppVm
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SplashScreen(nav: NavController) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Teal)
            .clickable { nav.navigate("welcome") },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Just the peacock artwork on the teal — no ring, no badge circle.
            Neelu("splash", 150.dp)
            Spacer(Modifier.height(18.dp))
            Text("Mor", fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Eat well, the Indian way.", fontSize = 15.sp, color = Color(0xFFCFE7E1))
            Spacer(Modifier.height(40.dp))
            // Explicit invitation instead of ambiguous dots (item 11).
            Text("Tap anywhere to start your journey", fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold, color = Gold,
                modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun WelcomeScreen(nav: NavController) {
    Column(Modifier.fillMaxSize().background(Chandan)) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(380.dp)
                .background(Color(0xFFEAF5F1)),
            contentAlignment = Alignment.BottomCenter,
        ) {
            // Feather-filled band instead of flat color or stray chips (A4, C7).
            FeatherBackdrop(Modifier.matchParentSize(), alpha = 0.5f)
            Neelu("wave", 240.dp, enter = true, idle = true)
        }
        Column(
            Modifier.padding(24.dp, 6.dp, 24.dp, 0.dp).weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            H1("Snap your meal. Know your calories.", 32, TextAlign.Center)
            Spacer(Modifier.height(6.dp))
            Muted(
                "Point the camera at your thali. Neelu reads it in seconds — English-first, Indian food, minimal words.",
                align = TextAlign.Center,
            )
            Spacer(Modifier.weight(1f))
            MorButton("Get started") { nav.navigate("meet") }
            Text("No sign-up needed", fontSize = 13.sp, color = Sub,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                    .padding(top = 10.dp, bottom = 26.dp),
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun MeetNeeluScreen(nav: NavController) {
    Column(Modifier.fillMaxSize().background(Chandan)) {
        MorTopBar(onBack = { nav.popBackStack() })
        Column(
            Modifier.weight(1f).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Compact enough to stay truly centered on small screens (A5).
            Box(
                Modifier.size(176.dp).clip(CircleShape).background(Mint2),
                contentAlignment = Alignment.Center,
            ) { Neelu("wave", 160.dp, enter = true, idle = true) }
            Spacer(Modifier.height(18.dp))
            H1("Hi, I'm Neelu.", 34, TextAlign.Center)
            Muted("I'm happiest when you eat well. I'll count, you enjoy.", 17, TextAlign.Center)
        }
        Column(Modifier.navigationBarsPadding().padding(20.dp, 0.dp, 20.dp, 26.dp)) {
            MorButton("Let's go") { nav.navigate("goal") }
        }
    }
}

@Composable
fun GoalScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    var v by remember(profile?.goal) { mutableStateOf(profile?.goal ?: "") }
    val opts = listOf(
        Triple("lose", "Lose weight" to "Steady, sustainable pace", "📉"),
        Triple("maintain", "Maintain weight" to "Stay where you are", "⚖️"),
        Triple("gain", "Gain weight" to "Build up gently", "💪"),
        Triple("healthy", "Eat healthier" to "Better choices, same weight", "🥗"),
    )
    StepScaffold(1, "What's your goal?", "STEP 1 OF 6",
        onBack = { nav.navigate("meet") }, actionEnabled = v.isNotEmpty(),
        onAction = { nav.navigate("diet") }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            opts.forEach { (key, labels, art) ->
                OptRow(labels.first, labels.second, v == key, trailing = {
                    Text(art, fontSize = 34.sp)
                }) {
                    v = key; vm.saveProfile(mapOf("goal" to key))
                }
            }
        }
    }
}

@Composable
fun DietScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    var v by remember(profile?.diet) { mutableStateOf(profile?.diet ?: "") }
    // Item 10: Vegan above Vegetarian; plant-based first, Non-veg last.
    val opts = listOf(
        Triple("vegan", "Vegan", "🌱"),
        Triple("veg", "Vegetarian", "🥬"),
        Triple("egg", "Eggetarian", "🥚"),
        Triple("non", "Non-veg", "🍗"),
    )
    StepScaffold(2, "What do you eat?", "STEP 2 OF 6",
        onBack = { nav.popBackStack() }, actionEnabled = v.isNotEmpty(),
        onAction = { nav.navigate("body") }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            opts.forEach { (key, label, art) ->
                OptRow(label, if (key == "vegan") "No animal products" else if (key == "veg") "No meat, no eggs" else if (key == "non") "Meat, fish & more" else null,
                    v == key,
                    leading = {
                        Text(vegBadge(if (key == "non") "non" else if (key == "egg") "egg" else "veg"), fontSize = 20.sp)
                    },
                    trailing = { Text(art, fontSize = 34.sp) }) {
                    v = key; vm.saveProfile(mapOf("diet" to key))
                }
            }
        }
    }
}

@Composable
fun BodyScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    // Explicit choice only: Male/Female, nothing preselected. A stored "other"
    // value is shown unselected, never silently mapped (item 17).
    var sex by remember { mutableStateOf(if (profile?.sex == "male" || profile?.sex == "female") profile?.sex!! else "") }
    var age by remember { mutableIntStateOf(profile?.age ?: 30) }
    var heightCm by remember { mutableDoubleStateOf(profile?.height_cm ?: 170.0) }
    var weightKg by remember { mutableDoubleStateOf(profile?.weight_kg ?: 72.0) }
    var useCm by remember { mutableStateOf(false) }
    val ft = (heightCm / 30.48).toInt()
    val inch = ((heightCm / 2.54 - ft * 12) + 0.5).toInt()
    var savingBody by remember { mutableStateOf(false) }
    StepScaffold(3, "About you", "STEP 3 OF 6",
        onBack = { nav.popBackStack() },
        actionLabel = if (savingBody) "Saving…" else "Continue",
        actionEnabled = sex.isNotEmpty() && !savingBody,
        onAction = {
            if (!savingBody) {
                savingBody = true
                // Await the save so Target/Plan read the fresh weight (C1, C2).
                vm.saveProfile(mapOf("sex" to sex, "age" to age,
                    "height_cm" to Math.round(heightCm * 10) / 10.0,
                    "weight_kg" to Math.round(weightKg * 10) / 10.0)) { p ->
                    savingBody = false
                    if (p != null) nav.navigate("target")
                    else vm.showSnack("Couldn't save. Check your connection and try again.", true)
                }
            }
        }) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    OptRow("♀ Female", null, sex == "female", big = true) { sex = "female" }
                }
                Box(Modifier.weight(1f)) {
                    OptRow("♂ Male", null, sex == "male", big = true) { sex = "male" }
                }
            }
            SliderCard("AGE", "$age years", age.toFloat(), 18f..100f,
                { age = it.toInt() })
            SliderCard(
                label = "HEIGHT",
                valueText = if (useCm) "${heightCm.toInt()} cm" else "$ft′ $inch″",
                sliderValue = heightCm.toFloat(),
                range = 130f..210f,
                onValue = { heightCm = it.toDouble() },
                labelAction = {
                    Text(if (useCm) "· Use ft/in" else "· Use cm", fontSize = 12.sp, color = Teal,
                        modifier = Modifier.clickable { useCm = !useCm })
                },
            )
            SliderCard("CURRENT WEIGHT", "${Math.round(weightKg * 10) / 10.0} kg",
                weightKg.toFloat(), 35f..150f,
                { weightKg = (it * 2).toInt() / 2.0 })
        }
    }
}

@Composable
fun TargetScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    val w = profile?.weight_kg ?: 72.0
    // Item 17: keyed on the loaded profile so a late-arriving profile replaces
    // the 72/66 defaults instead of the plan inheriting stale numbers.
    var tgt by remember(profile?.weight_kg, profile?.target_weight_kg) {
        mutableDoubleStateOf(profile?.target_weight_kg ?: 66.0)
    }
    var savingTgt by remember { mutableStateOf(false) }
    val loss = w - tgt
    val ambitious = loss > w * 0.1
    val weeks = maxOf(1, Math.ceil(Math.abs(loss) / 0.5).toInt())
    val date = remember(weeks) {
        LocalDate.now().plusWeeks(weeks.toLong())
            .format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
    }
    val milestone = Math.round((w - w * 0.1) * 10) / 10.0
    // Goal/target contradiction must be confirmed, never silently changed (C3).
    val goal = profile?.goal
    val mismatch = (goal == "gain" && tgt < w) || (goal == "lose" && tgt > w)
    var confirmed by remember(tgt) { mutableStateOf(false) }
    StepScaffold(4, "Your target weight", "STEP 4 OF 6",
        onBack = { nav.popBackStack() },
        actionLabel = if (savingTgt) "Saving…" else "Continue",
        actionEnabled = tgt >= 35 && (!mismatch || confirmed) && !savingTgt,
        onAction = {
            // Item 17: await the save — the plan must be built from this
            // target, never from a value still in flight.
            if (!savingTgt) {
                savingTgt = true
                vm.saveProfile(mapOf("target_weight_kg" to tgt)) { p ->
                    savingTgt = false
                    if (p != null) nav.navigate("activity")
                    else vm.showSnack("Couldn't save. Check your connection and try again.", true)
                }
            }
        }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SliderCard(
                label = "TARGET WEIGHT",
                valueText = "${if (tgt % 1 == 0.0) tgt.toInt() else tgt} kg",
                sliderValue = tgt.toFloat(),
                range = 35f..maxOf(80f, (w + 10).toFloat()),
                onValue = { tgt = (it * 2).toInt() / 2.0 },
                bottom = {
                    if (loss != 0.0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Pill("${if (loss > 0) "−" else "+"}${Math.abs(Math.round(loss * 10) / 10.0)} kg · ${if (ambitious) "Ambitious" else "Realistic"}",
                                if (ambitious) AmberBg else GreenBg, if (ambitious) Amber else Green)
                            Pill("By $date", Mint2, Teal)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    Muted("Slide to your target · now $w kg", 13)
                },
            )
            if (mismatch && !confirmed) {
                Box(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(AmberBg)
                        .border(1.5.dp, Color(0xFFC98A1E), RoundedCornerShape(22.dp))
                        .padding(14.dp),
                ) {
                    Column {
                        Text(
                            if (goal == "gain") "Gain weight, but your target is below your current weight?"
                            else "Lose weight, but your target is above your current weight?",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Amber)
                        Spacer(Modifier.height(4.dp))
                        Text("Your goal stays ${goal}. Either move the slider, or confirm you mean it.",
                            fontSize = 13.sp, color = Sub)
                        Spacer(Modifier.height(8.dp))
                        MorSecondaryButton("Keep this target anyway") { confirmed = true }
                    }
                }
            }
            if (ambitious) {
                MorCard {
                    Column {
                        Text("Big jump. Go in steps?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Amber)
                        Spacer(Modifier.height(8.dp))
                        MorSecondaryButton("Start with $milestone kg") { tgt = milestone }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    var v by remember { mutableStateOf(profile?.activity ?: "light") }
    var savingAct by remember { mutableStateOf(false) }
    val opts = listOf(
        Triple("sit", "Mostly sitting", "Desk job, little movement"),
        Triple("light", "Lightly active", "Walks, chores some days"),
        Triple("active", "Active", "Exercise most days"),
        Triple("very", "Very active", "Physical work or daily training"),
    )
    StepScaffold(5, "How active are you?", "STEP 5 OF 6",
        onBack = { nav.popBackStack() },
        actionLabel = if (savingAct) "Saving…" else "Continue",
        actionEnabled = v.isNotEmpty() && !savingAct,
        onAction = {
            // Same await pattern as the other steps: the plan must see this.
            if (!savingAct) {
                savingAct = true
                vm.saveProfile(mapOf("activity" to v)) { p ->
                    savingAct = false
                    if (p != null) nav.navigate("mealtimes")
                    else vm.showSnack("Couldn't save. Check your connection and try again.", true)
                }
            }
        }) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            opts.forEachIndexed { idx, (key, label, sub) ->
                OptRow(label, sub, v == key, trailing = {
                    // Upright ascending bars, baseline-aligned: more filled = more active.
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        repeat(4) { i ->
                            Box(
                                Modifier
                                    .size(7.dp, (8 + i * 7).dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (i <= idx) Teal else Line2),
                            )
                        }
                    }
                }) { v = key; vm.saveProfile(mapOf("activity" to key)) }
            }
        }
    }
}

private val DEFAULT_TIMES = mapOf("breakfast" to "08:30", "lunch" to "13:30", "snack" to "17:00", "dinner" to "21:00")
private val QUICK = mapOf(
    "breakfast" to listOf("07:30", "08:00", "08:30", "09:00"),
    "lunch" to listOf("12:30", "13:00", "13:30", "14:00"),
    "snack" to listOf("16:00", "16:30", "17:00", "18:00"),
    "dinner" to listOf("20:00", "20:30", "21:00", "21:30"),
)

fun fmtTime(t: String): String {
    val p = t.split(":")
    val h = p.getOrNull(0)?.toIntOrNull() ?: 0
    val m = p.getOrNull(1)?.toIntOrNull() ?: 0
    val ap = if (h >= 12) "PM" else "AM"
    val hh = if (h % 12 == 0) 12 else h % 12
    return "$hh:${m.toString().padStart(2, '0')} $ap"
}

@Composable
fun MealTimesScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    var times by remember { mutableStateOf(DEFAULT_TIMES + (profile?.meal_times ?: emptyMap())) }
    var on by remember { mutableStateOf(mapOf("breakfast" to true, "lunch" to true, "snack" to true, "dinner" to true)) }
    var edit by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val slots = listOf("breakfast" to "🫓", "lunch" to "🍽️", "snack" to "🍵", "dinner" to "🍲")
    StepScaffold(6, "Your meal times", "STEP 6 OF 6",
        onBack = { nav.navigate("activity") },
        actionLabel = if (saving) "Saving…" else "Build my plan",
        actionEnabled = !saving,
        onAction = {
            if (!saving) {
                saving = true
                saveError = null
                val keep = times.filterKeys { on[it] == true }
                vm.saveProfile(mapOf("meal_times" to keep, "complete" to true)) { p ->
                    saving = false
                    if (p?.plan != null) nav.navigate("building")
                    else saveError = "Couldn't save your plan. Check your connection and try again."
                }
            }
        }) {
        Column {
            saveError?.let {
                Text(it, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Kumkum,
                    modifier = Modifier.padding(bottom = 8.dp))
            }
            slots.forEach { (s, emoji) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(emoji, fontSize = 30.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(s.replaceFirstChar { it.uppercase() }, fontSize = 15.sp,
                        fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Box(Modifier.clip(RoundedCornerShape(999.dp)).background(Color.White)
                        .clickable { edit = s }.padding(14.dp, 10.dp)) {
                        Text(fmtTime(times[s] ?: "08:30"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.width(8.dp))
                    MorToggle(on[s] == true) { on = on + (s to it) }
                }
            }
            Muted("Neelu reminds you at these times. Max 4 a day, never at night.", 13)
        }
    }
    edit?.let { slot ->
        MorSheet({ edit = null }) {
            Text("Edit ${slot.replaceFirstChar { it.uppercase() }}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QUICK[slot]?.forEach { t ->
                    Chip(fmtTime(t), times[slot] == t) { times = times + (slot to t) }
                }
            }
            Spacer(Modifier.height(12.dp))
            MorButton("Save") { edit = null }
        }
    }
}

@Composable
fun BuildingScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    var p by remember { mutableIntStateOf(4) }
    var failed by remember { mutableStateOf(false) }
    var retrying by remember { mutableStateOf(false) }
    // Ambience only: navigation waits for the real saved plan, never the timer.
    LaunchedEffect(Unit) {
        while (p < 97) { delay(200); p = minOf(97, p + (1..9).random()) }
    }
    // Item 17: only accept a plan built from the CURRENT answers. A plan
    // left over from an earlier run (or a save that raced) shows stale
    // weight/target/date — rebuild it instead of displaying it.
    fun kgStr(v: Double) = if (v % 1 == 0.0) v.toInt().toString() else v.toString()
    var rebuilding by remember { mutableStateOf(false) }
    LaunchedEffect(profile) {
        val pr = profile ?: return@LaunchedEffect
        val pl = pr.plan
        val w = pr.weight_kg ?: 0.0
        val t = pr.target_weight_kg ?: w
        if (pl != null && pl.goal_line == "${kgStr(w)} → ${kgStr(t)} kg") {
            p = 100
            delay(400)
            nav.navigate("plan") { popUpTo("building") { inclusive = true } }
        } else if (!rebuilding) {
            rebuilding = true
            vm.saveProfile(mapOf("complete" to true)) { r ->
                if (r?.plan == null) {
                    rebuilding = false
                    failed = true
                }
                // On success the profile flow re-triggers this effect, which
                // now sees a matching plan and navigates.
            }
        } else {
            // A rebuild already ran yet the plan still doesn't match these
            // answers: say so instead of spinning forever.
            failed = true
        }
    }
    // Truthful timeout: the plan POST should take seconds, not minutes.
    LaunchedEffect(Unit) {
        delay(30000)
        if (profile?.plan == null) failed = true
    }
    Column(
        Modifier.fillMaxSize().background(Chandan).navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Neelu("think", 190.dp)
        Spacer(Modifier.height(18.dp))
        H1(if (failed) "Plan isn't ready yet" else "Building your plan…", align = TextAlign.Center)
        Muted(
            if (failed) "The plan couldn't be saved. Check your connection, then retry — your answers are kept."
            else "Balancing your ${if (profile?.diet == "non") "meals" else profile?.diet ?: "meals"} and ${profile?.goal ?: "health"} goal.",
            align = TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        if (!failed) {
            LinearProgressIndicator(progress = { p / 100f }, modifier = Modifier.width(240.dp),
                color = Teal, trackColor = Line2)
            Muted("$p%", 13, TextAlign.Center)
        } else {
            MorButton(if (retrying) "Retrying…" else "Retry", enabled = !retrying) {
                retrying = true
                failed = false
                vm.refreshProfile { q ->
                    retrying = false
                    if (q?.plan != null) {
                        nav.navigate("plan") { popUpTo("building") { inclusive = true } }
                    } else {
                        vm.saveProfile(mapOf("complete" to true)) { r ->
                            if (r?.plan != null) {
                                nav.navigate("plan") { popUpTo("building") { inclusive = true } }
                            } else failed = true
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            MorSecondaryButton("Back to meal times") { nav.popBackStack() }
        }
    }
}

@Composable
fun PlanScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    val plan = profile?.plan
    val target = plan?.kcal_target ?: 1850
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFDEBC8)).statusBarsPadding()
            .navigationBarsPadding().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Neelu("dance", 200.dp, enter = true)
        H1("Your plan is ready!", 32, TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        MorCard(Modifier.fillMaxWidth()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("YOUR DAILY TARGETS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                Text("$target kcal", fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
                if (plan?.goal_line != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(plan.goal_line, fontSize = 14.sp, color = Sub, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                PlanMacroBars(
                    plan?.protein_g ?: 75.0,
                    plan?.carbs_g ?: 250.0,
                    plan?.fat_g ?: 60.0,
                )
                if (plan?.target_date != null) {
                    Spacer(Modifier.height(12.dp))
                    Pill("Target by ${plan.target_date}", Mint2, Teal)
                }
            }
        }
        Muted(
            "Split for your ${profile?.goal ?: "health"} goal on a " +
                "${profile?.diet ?: "balanced"} plate. Log meals and Home tracks these down.",
            13, TextAlign.Center)
        Spacer(Modifier.weight(1f))
        MorButton("Start") { nav.navigate("notify") }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
fun NotifyScreen(nav: NavController, vm: AppVm) {
    val ctx = LocalContext.current
    var ask by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    fun finish(remindersOn: Boolean) {
        if (leaving) return
        leaving = true
        vm.saveProfile(mapOf("reminders_on" to remindersOn)) {
            vm.refreshProfile()
            vm.refreshHome()
            if (!remindersOn) vm.showSnack("Reminders are off. Turn them on in Settings anytime.")
            nav.navigate("home") { popUpTo("splash") { inclusive = true } }
        }
    }
    // Real runtime permission request (item 1): "Allow" must open the
    // Android notification permission sheet, not just save a flag.
    val notifPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> finish(granted) }
    fun requestReminders() {
        if (android.os.Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            finish(true)
        } else {
            try {
                notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } catch (_: Exception) {
                finish(false)
            }
        }
    }
    Column(
        Modifier.fillMaxSize().background(Chandan).navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Neelu("wave", 200.dp)
        Spacer(Modifier.height(10.dp))
        H1("Remind you at mealtimes?", 30, TextAlign.Center)
        Muted("Max 4 a day. Never at night.", align = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        MorButton("Yes, remind me") { ask = true }
        Spacer(Modifier.height(10.dp))
        MorSecondaryButton("Not now") { finish(false) }
    }
    if (ask) {
        MorSheet({ if (!leaving) ask = false }) {
            Text("Allow Mor to send you notifications?", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Muted("Reminders may include alerts and sounds.", 13)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) {
                    MorSecondaryButton("Don't allow") { finish(false) }
                }
                Box(Modifier.weight(1f)) {
                    MorButton("Allow") { requestReminders() }
                }
            }
        }
    }
}
