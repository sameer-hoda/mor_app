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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.navigation.NavController
import dev.mor.app.data.AppVm
import dev.mor.app.data.STEPS_PERMS
import dev.mor.app.data.StepsState
import dev.mor.app.data.friendlyErr
import dev.mor.app.data.readTodaySteps
import dev.mor.app.data.relevantSlot
import dev.mor.app.data.stepsPermissionContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val SLOTS = listOf("breakfast", "lunch", "snack", "dinner")

/** Time-of-day header art (item 26): eight Nano Banana themes picked by hour. */
fun headerArt(hour: Int): Int = when (hour) {
    in 5..6 -> dev.mor.app.R.drawable.hdr_dawn
    in 7..10 -> dev.mor.app.R.drawable.hdr_morning
    in 11..13 -> dev.mor.app.R.drawable.hdr_noon
    in 14..16 -> dev.mor.app.R.drawable.hdr_afternoon
    in 17..18 -> dev.mor.app.R.drawable.hdr_evening
    in 19..20 -> dev.mor.app.R.drawable.hdr_sunset
    in 21..23 -> dev.mor.app.R.drawable.hdr_night
    else -> dev.mor.app.R.drawable.hdr_latenight
}

/** Daypart greeting from the device clock (C4). */
fun localGreeting(): String = when (java.time.LocalTime.now().hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Good night"
}

/** "2026-11-06" → "Fri 6 Nov", falling back to today. */
fun prettyDate(iso: String): String {
    return try {
        java.time.LocalDate.parse(iso).format(
            java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.ENGLISH),
        )
    } catch (_: Exception) {
        java.time.LocalDate.now().format(
            java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.ENGLISH),
        )
    }
}

@Composable
fun HomeScreen(nav: NavController, vm: AppVm) {
    val home by vm.home.collectAsState()
    val profile by vm.profile.collectAsState()
    var sheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Fresh data on every entry: never show a stale pre-plan goal.
    LaunchedEffect(Unit) {
        vm.refreshProfile()
        vm.refreshHome()
    }

    if (home == null || profile == null) {
        Box(Modifier.fillMaxSize().background(Chandan), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }
    val h = home!!
    val night = h.state == "night"
    val eatenPct = (h.kcal_eaten / h.kcal_target.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat()
    val over = h.kcal_eaten > h.kcal_target
    val anim = if (night) "sleep" else when (h.state) {
        "over" -> "shrug"; "goal" -> "dance"; "missed" -> "hungry"; else -> "wave"
    }

    // Wall surface: feather mosaic behind solid Chandan (~42%), per redesign spec.
    Box(Modifier.fillMaxSize().background(Chandan)) {
        MosaicWall(alpha = 0.42f)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Header scene: time-of-day art (item 26) with a readability scrim.
            Box(Modifier.fillMaxWidth().height(300.dp)) {
                androidx.compose.foundation.Image(
                    painter = painterResource(headerArt(java.time.LocalTime.now().hour)),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
                Box(
                    Modifier.fillMaxSize().background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                Chandan.copy(alpha = 0.94f),
                                Chandan.copy(alpha = 0.55f),
                                Color.Transparent,
                            ),
                        ),
                    ),
                )
                Column {
                    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp, 12.dp, 16.dp, 0.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            // Greeting from the device clock, not server time (C4).
                            Text(localGreeting(), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(prettyDate(h.date), fontSize = 13.sp, color = Sub)
                        }
                        // Item 12: the badge lives OUTSIDE the clipped bell circle,
                        // overhanging its top-end, so the circle can never clip it.
                        Box(
                            Modifier.size(48.dp).clickable { nav.navigate("inbox") },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("🔔", fontSize = 18.sp)
                            }
                            // Clearly visible unread badge with count (A1).
                            if (h.unread > 0) Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                                    .size(if (h.unread > 9) 26.dp else 22.dp)
                                    .clip(CircleShape)
                                    .background(Kumkum)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (h.unread > 99) "99+" else "${h.unread}",
                                    fontSize = if (h.unread > 9) 9.sp else 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                )
                            }
                        }
                    }
                }
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp)) {
                    Neelu(anim, 200.dp)
                }
            }

            Column(Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // State banner
                Banner(h.state,
                    onScan = { nav.navigate("camera?slot=${relevantSlot(profile?.meal_times)}") },
                    onSearch = { nav.navigate("search?slot=${relevantSlot(profile?.meal_times)}") },
                    onAdd = { sheet = true },
                    onInsights = { nav.navigate("insights") },
                    onChallenges = { nav.navigate("challenges") },
                    onDismiss = { vm.showSnack("Noted. Neelu will not nag.") })

                // Item 11: water + steps live at the top — no scroll needed.
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MorCard(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💧", fontSize = 22.sp)
                            Column(Modifier.weight(1f)) {
                                Text("${h.water}/${h.water_goal}", fontWeight = FontWeight.Bold)
                                Text("glasses", fontSize = 13.sp, color = Sub)
                            }
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Mint2)
                                .clickable {
                                    scope.launch {
                                        try {
                                            val j = vm.api().water(mapOf("delta" to 1))
                                            vm.showSnack("Water logged · ${j.glasses}/8")
                                            vm.refreshHome()
                                        } catch (e: Exception) {
                                            vm.showSnack("Water failed: ${friendlyErr(e)}", true)
                                        }
                                    }
                                }, contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
                            }
                        }
                    }
                    MorCard(Modifier.weight(1f)) {
                        StepsCard(vm)
                    }
                }

                // Streak card with a real Mon–Sun strip: today circled from the
                // local date, centred labels, gold dot if today has a logged
                // meal. The circle marks today; the dot marks logged (item 28).
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFFFF3DC)).clickable { nav.navigate("streak") }
                        .padding(16.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🪔", fontSize = 26.sp)
                    Text("${h.streak}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
                    Text(" day streak", fontSize = 14.sp, color = Amber,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    val todayIdx = (java.time.LocalDate.now().dayOfWeek.value - 1 + 7) % 7
                    val loggedToday = h.meals.values.any { it.logged }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("M", "T", "W", "T", "F", "S", "S").forEachIndexed { i, d ->
                            val isToday = i == todayIdx
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    Modifier.size(24.dp).clip(CircleShape)
                                        .background(if (isToday) Teal else Color.Transparent)
                                        .border(
                                            if (isToday) 0.dp else 1.dp,
                                            if (isToday) Color.Transparent else Color(0xFFC98A1E),
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(d, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                        color = if (isToday) Color.White else Amber,
                                        textAlign = TextAlign.Center)
                                }
                                Spacer(Modifier.height(2.dp))
                                Box(
                                    Modifier.size(5.dp).clip(CircleShape)
                                        .background(if (isToday && loggedToday) Gold else Color.Transparent),
                                )
                            }
                        }
                    }
                }

                // Kcal card
                MorCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(if (over) "KCAL OVER" else "KCAL LEFT",
                                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                            Text(
                                (if (over) h.kcal_eaten - h.kcal_target else h.kcal_left).toInt().toString(),
                                fontSize = 44.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay,
                                color = if (over) Amber else Teal)
                            Text("of ${h.kcal_target.toInt()} kcal · ${h.kcal_eaten.toInt()} eaten",
                                fontSize = 13.sp, color = Sub)
                            Spacer(Modifier.height(10.dp))
                            MacroBars(h.macros.eaten.protein_g, h.macros.eaten.carbs_g,
                                h.macros.eaten.fat_g, h.macros.goal.protein_g,
                                h.macros.goal.carbs_g, h.macros.goal.fat_g)
                        }
                        Spacer(Modifier.width(14.dp))
                        Box(Modifier.clickable { sheet = true }) {
                            ProgressRing(eatenPct, 100.dp, if (over) Genda else Teal) {
                                Box(Modifier.size(58.dp).clip(CircleShape).background(Teal),
                                    contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(40.dp).clip(CircleShape).background(Gold),
                                        contentAlignment = Alignment.Center) {
                                        Box(Modifier.size(22.dp).clip(CircleShape).background(Pankh),
                                            contentAlignment = Alignment.Center) {
                                            Box(Modifier.size(12.dp).clip(CircleShape).background(Jamun))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Meals
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Today's meals", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Text("${h.kcal_eaten.toInt()} kcal today", fontSize = 13.sp, color = Sub)
                }
                val nowSlot = relevantSlot(profile?.meal_times)
                SLOTS.forEach { slot ->
                    val m = h.meals[slot]
                    val cap = slot.replaceFirstChar { it.uppercase() }
                    // Subtle "log now" nudge on the time-relevant unlogged meal (item 30).
                    val isNow = slot == nowSlot && m?.logged != true
                    if (m != null && m.logged) {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                                .background(Color.White).clickable { nav.navigate("meal/${m.id}") }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row {
                                (m.items ?: emptyList()).take(4).forEach { it ->
                                    Text(emojiForFood(it.name), fontSize = 30.sp)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(cap, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text((m.items ?: emptyList()).joinToString(", ") { it.name },
                                    fontSize = 13.sp, color = Sub, maxLines = 1)
                            }
                            Text("${m.kcal.toInt()} kcal", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                                .background(Color.White)
                                .border(if (isNow) 1.5.dp else 0.dp, if (isNow) Teal else Color.Transparent, RoundedCornerShape(20.dp))
                                .clickable { nav.navigate("add?slot=$slot") }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(42.dp).clip(CircleShape)
                                .border(2.dp,
                                    if (h.state == "missed" && slot == "lunch") Kumkum else Line2,
                                    CircleShape),
                                contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 20.sp, color = Sub)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(cap, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                    if (isNow) {
                                        Spacer(Modifier.width(6.dp))
                                        Pill("Now", Mint2, Teal)
                                    }
                                }
                                Text(
                                    if (h.state == "missed" && slot == "lunch") "Not logged"
                                    else "Opens ${m?.time ?: ""}",
                                    fontSize = 13.sp,
                                    color = if (h.state == "missed" && slot == "lunch") Red else Sub)
                            }
                            Box(Modifier.size(30.dp).clip(CircleShape).background(Mint2),
                                contentAlignment = Alignment.Center) {
                                Text("+", fontWeight = FontWeight.ExtraBold, color = Teal)
                            }
                        }
                    }
                }

                // Content scrolls behind the floating pill; ~96dp clearance plus
                // pill height keeps the last card clear.
                Spacer(Modifier.height(150.dp))
            }
        }
        MorBottomNav("home", Modifier.align(Alignment.BottomCenter)) { nav.navigate(it) }
    }
    if (sheet) AddSheet(nav, vm) { sheet = false }
}

/** Live device steps via Health Connect, or an honest non-data state.
 *  The backend's seeded sample count is never displayed (items 6, 32). */
@Composable
private fun StepsCard(vm: dev.mor.app.data.AppVm) {
    val ctx = LocalContext.current
    var state by remember { mutableStateOf<StepsState>(StepsState.Loading) }
    // After one system-sheet round that still leaves us ungranted (denied,
    // or a device whose permission UI never opens), the tap goes somewhere
    // useful instead of silently doing nothing (item 2).
    var askedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun load() {
        scope.launch(Dispatchers.IO) {
            val s = readTodaySteps(ctx)
            withContext(Dispatchers.Main) { state = s }
        }
    }
    fun openHealthSettings() {
        askedOnce = true
        try {
            ctx.startActivity(android.content.Intent(
                androidx.health.connect.client.HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
            vm.showSnack("Allow steps for Mor in Health Connect, then come back.")
        } catch (_: Exception) {
            try {
                ctx.startActivity(android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")))
                vm.showSnack("Install Health Connect, then tap steps again.")
            } catch (_: Exception) {
                vm.showSnack("Steps need the Health Connect app from the Play Store.", true)
            }
        }
    }
    val permLauncher = rememberLauncherForActivityResult(stepsPermissionContract()) {
        askedOnce = true
        load()
    }
    LaunchedEffect(Unit) { load() }
    Row(
        Modifier.clickable {
            when (state) {
                is StepsState.NeedsPermission -> {
                    if (askedOnce) openHealthSettings()
                    else try {
                        permLauncher.launch(STEPS_PERMS)
                    } catch (_: Exception) {
                        openHealthSettings()
                    }
                }
                is StepsState.Error -> load()
                else -> {}
            }
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("👟", fontSize = 22.sp)
        Spacer(Modifier.width(8.dp))
        when (val s = state) {
            is StepsState.Live -> Column {
                Text("%,d".format(s.steps), fontWeight = FontWeight.Bold)
                Text("steps today", fontSize = 13.sp, color = Sub)
            }
            is StepsState.Loading -> Column {
                Text("…", fontWeight = FontWeight.Bold)
                Text("reading steps", fontSize = 13.sp, color = Sub)
            }
            is StepsState.NeedsPermission -> Column(Modifier.weight(1f)) {
                Text("Steps off", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(if (askedOnce) "tap for settings" else "tap to allow",
                    fontSize = 13.sp, color = Teal, fontWeight = FontWeight.SemiBold)
            }
            is StepsState.Unavailable -> Column {
                Text("—", fontWeight = FontWeight.Bold)
                Text("no step counter", fontSize = 13.sp, color = Sub)
            }
            is StepsState.Error -> Column {
                Text("—", fontWeight = FontWeight.Bold)
                Text("tap to retry", fontSize = 13.sp, color = Sub)
            }
        }
    }
}

@Composable
private fun Banner(
    state: String,
    onScan: () -> Unit, onSearch: () -> Unit, onAdd: () -> Unit,
    onInsights: () -> Unit, onChallenges: () -> Unit, onDismiss: () -> Unit,
) {
    data class Act(val label: String, val fn: () -> Unit)
    val (bg, text, acts) = when (state) {
        "firstday" -> Triple(Mint2, "Log your first meal",
            listOf(Act("Scan", onScan), Act("Search", onSearch)))
        "missed" -> Triple(RedBg, "Missed lunch?",
            listOf(Act("Log lunch", onAdd), Act("I skipped it", onDismiss)))
        "over" -> Triple(AmberBg, "A bit over today. That's okay. Tomorrow is a fresh start.", emptyList())
        "goal" -> Triple(GreenBg, "Goal reached! +40 coins", listOf(Act("See coins", onChallenges)))
        "night" -> Triple(Mint2, "Day done. Well eaten.", listOf(Act("See insights", onInsights)))
        else -> return
    }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).padding(14.dp, 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        acts.forEach { a ->
            Spacer(Modifier.width(6.dp))
            Box(Modifier.clip(RoundedCornerShape(18.dp)).background(Color.White)
                .clickable(onClick = a.fn).padding(14.dp, 8.dp)) {
                Text(a.label, fontSize = 13.sp, color = Teal, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AddSheet(nav: NavController, vm: AppVm, initialSlot: String? = null, onClose: () -> Unit) {
    val profile by vm.profile.collectAsState()
    var slot by remember {
        mutableStateOf(initialSlot ?: relevantSlot(profile?.meal_times))
    }
    var saving by remember { mutableStateOf(false) }
    // Quick-add servings (item 5): 1–4 × 150 kcal, chosen inline.
    var qaQty by remember { mutableStateOf(1) }
    val scope = rememberCoroutineScope()
    MorSheet(onClose) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Neelu("peek", 54.dp)
            Spacer(Modifier.width(10.dp))
            Text("Log a meal", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
        }
        Spacer(Modifier.height(10.dp))
        Segmented(listOf("breakfast", "lunch", "snack", "dinner"), slot, tall = true) { slot = it }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            listOf("📷  Scan meal" to "camera?slot=$slot",
                "🔍  Search food" to "search?slot=$slot",
                "🎙️  Speak" to "search?slot=$slot&voice=1",
                "🖼️  Gallery" to "camera?slot=$slot&gallery=1").forEach { (label, route) ->
                OptRow(label, null, false) { nav.navigate(route) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    OptRow("⚡  Quick add · ${150 * qaQty} kcal", null, false) {
                        // Single-flight: ignore repeated taps while a save is in progress,
                        // and only report success after the server confirms + totals refresh.
                        if (!saving) {
                            saving = true
                            scope.launch {
                                try {
                                    val r = vm.api().logMeal(mapOf("slot" to slot, "source" to "quick",
                                        "name" to "Quick add",
                                        "items" to listOf(mapOf("name" to "Snack", "kcal" to 150 * qaQty,
                                            "portion" to if (qaQty == 1) "1 serving" else "$qaQty servings"))))
                                    vm.refreshHome()
                                    vm.showSnack("Logged ${150 * qaQty} kcal to ${slot.replaceFirstChar { it.uppercase() }} · +${r.coins_awarded} coins")
                                    onClose()
                                } catch (e: Exception) {
                                    vm.showSnack("Quick add failed: ${friendlyErr(e)}", true)
                                }
                                saving = false
                            }
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(38.dp).clip(CircleShape).background(Mint2)
                    .clickable { qaQty = maxOf(1, qaQty - 1) },
                    contentAlignment = Alignment.Center) {
                    Text("−", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
                }
                Text("×$qaQty", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.width(44.dp), textAlign = TextAlign.Center)
                Box(Modifier.size(38.dp).clip(CircleShape).background(Mint2)
                    .clickable { qaQty = minOf(4, qaQty + 1) },
                    contentAlignment = Alignment.Center) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
                }
            }
        }
    }
}
