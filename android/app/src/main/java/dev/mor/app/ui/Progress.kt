package dev.mor.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.mor.app.data.AppVm
import dev.mor.app.data.Challenge
import dev.mor.app.data.Insights
import dev.mor.app.data.friendlyErr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

// ---------- Insights ----------

@Composable
fun InsightsScreen(nav: NavController, vm: AppVm) {
    val home by vm.home.collectAsState()
    var range by remember { mutableStateOf("week") }
    var data by remember { mutableStateOf<Insights?>(null) }
    var weightSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Item 6: the toggle reloads from the server with the chosen range —
    // week and month show their own correct data, never a stale copy.
    fun load(r: String) {
        data = null
        scope.launch(Dispatchers.IO) {
            try {
                val d = vm.api().insights(r)
                withContext(Dispatchers.Main) { data = d }
            } catch (_: Exception) { }
        }
    }
    LaunchedEffect(range) { load(range) }

    val d = data
    val loggedDays = d?.logged_days ?: 0
    val goal = d?.kcal_target ?: 2000.0

    // Wall surface: feather mosaic behind solid Chandan (~42%), per redesign spec.
    Box(Modifier.fillMaxSize().background(Chandan)) {
        MosaicWall(alpha = 0.42f)
        Column(Modifier.fillMaxSize()) {
            MorTopBar("Insights")
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(16.dp, 0.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.width(200.dp)) {
                Segmented(listOf("week", "month"), range) { range = it }
            }
            if (d == null || loggedDays == 0) {
                MorCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(12.dp, 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Neelu("think", 150.dp)
                        Text("Log ${3 - loggedDays} days to unlock", fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
                        Muted("Your weekly view appears after 3 logged days." +
                            if (loggedDays > 0) " $loggedDays of 3 so far." else "",
                            align = TextAlign.Center)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(3) { i ->
                                Box(Modifier.size(54.dp, 8.dp).clip(RoundedCornerShape(4.dp))
                                    .background(if (i < loggedDays) Teal else Line2))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        MorButton("Log a meal") { nav.navigate("add") }
                    }
                }
            } else {
                Text("${d.days.firstOrNull()?.date?.substring(8)} – ${d.days.lastOrNull()?.date?.substring(8)} ${d.days.lastOrNull()?.date?.substring(0, 4)}",
                    fontSize = 17.sp, fontWeight = FontWeight.Bold)
                MorCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Daily average", fontSize = 13.sp, color = Sub)
                            Text("%,d".format(d.avg), fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
                            Text("kcal/day", fontSize = 13.sp, color = Sub)
                        }
                        Spacer(Modifier.weight(1f))
                        Pill("${d.on_track_days}/${d.days.size} on track",
                            if (d.on_track_days >= d.days.size - 2) GreenBg else AmberBg,
                            if (d.on_track_days >= d.days.size - 2) Green else Amber)
                    }
                }
                MorCard {
                    Column {
                        val isMonth = range == "month"
                        Text("This ${if (isMonth) "month" else "week"} vs your ${"%,d".format(goal.toInt())} kcal goal",
                            fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        // Month shows 30 daily bars in a sideways scroll; week fits 7.
                        val chartMod = if (isMonth) Modifier.fillMaxWidth().height(130.dp)
                            .horizontalScroll(rememberScrollState())
                        else Modifier.fillMaxWidth().height(130.dp)
                        Row(chartMod,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            d.days.forEach { day ->
                                Column((if (isMonth) Modifier.width(30.dp) else Modifier.weight(1f)).fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Bottom) {
                                    Text(if (day.kcal > 0) "${day.kcal.toInt()}" else "–",
                                        fontSize = 10.sp, color = Sub)
                                    Box(Modifier.fillMaxWidth()
                                        .weight(minOf(1f, (day.kcal / (goal * 1.25)).toFloat())
                                            .coerceAtLeast(if (day.kcal > 0) 0.05f else 0.02f))
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (day.kcal == 0.0) Line2
                                            else if (day.kcal <= goal) Teal else Genda))
                                    Text(if (day.weekday.isNotEmpty()) day.weekday[0].toString() else "",
                                        fontSize = 13.sp, color = Sub)
                                }
                            }
                        }
                        Text("goal ${"%,d".format(goal.toInt())}", fontSize = 13.sp, color = Sub,
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End)
                    }
                }
                MorCard {
                    Row(Modifier.fillMaxWidth().clickable { weightSheet = true },
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("⚖️", fontSize = 26.sp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${d.weights.lastOrNull()?.kg ?: home?.weight_kg} kg",
                                fontSize = 22.sp, fontWeight = FontWeight.Bold, fontFamily = MorDisplay)
                            Text(
                                if (d.weight_trend < 0) "▼ ${Math.abs(d.weight_trend)} kg since first log"
                                else if (d.weight_trend > 0) "▲ ${d.weight_trend} kg"
                                else "Log your weight",
                                fontSize = 13.sp, color = Sub)
                        }
                        Text("Log ›", color = Teal, fontWeight = FontWeight.Bold)
                    }
                }
                MorCard {
                    Column {
                        Row {
                            Text("Protein", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f))
                            Text("${d.protein_today.toInt()}/${d.protein_goal.toInt()} g",
                                fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (d.protein_today / d.protein_goal.coerceAtLeast(1.0))
                                .coerceIn(0.0, 1.0).toFloat() },
                            modifier = Modifier.fillMaxWidth(), color = Neel, trackColor = Color(0xFFEFE9DC))
                        Spacer(Modifier.height(6.dp))
                        Text("Lowest at breakfast — add curd or sprouts.",
                            fontSize = 13.sp, color = Sub)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
        }
        MorBottomNav("insights", Modifier.align(Alignment.BottomCenter)) { nav.navigate(it) }
    }
    if (weightSheet) {
        WeightSheet(home?.weight_kg ?: 72.0, {
            weightSheet = false
            load(range)
            vm.refreshHome()
        }, vm)
    }
}

@Composable
fun WeightSheet(current: Double, onClose: () -> Unit, vm: AppVm) {
    var kg by remember { mutableDoubleStateOf(current) }
    var delta by remember { mutableStateOf<Double?>(null) }
    val scope = rememberCoroutineScope()
    MorSheet(onClose) {
        Text("Today's weight", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
        Text("Last: $current kg", fontSize = 13.sp, color = Sub)
        Spacer(Modifier.height(10.dp))
        MorCard(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally) {
                Text("%.1f kg".format(kg), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
                Slider(kg.toFloat(), { kg = ((it * 10).toInt() / 10.0) },
                    valueRange = 35f..150f,
                    colors = SliderDefaults.colors(thumbColor = Teal, activeTrackColor = Teal))
            }
        }
        delta?.let { dl ->
            Spacer(Modifier.height(10.dp))
            Pill(if (dl == 0.0) "Same as last time"
                else if (dl < 0) "▼ ${Math.abs(dl)} kg since last time" else "▲ $dl kg since last time",
                GreenBg, Green)
        }
        Spacer(Modifier.height(14.dp))
        MorButton("Save") {
            scope.launch(Dispatchers.IO) {
                try {
                    val r = vm.api().weight(mapOf("kg" to kg))
                    vm.refreshHome()
                    withContext(Dispatchers.Main) { delta = r.delta; onClose() }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) { vm.showSnack("Save failed: ${friendlyErr(e)}", true) }
                }
            }
        }
    }
}

@Composable
fun WeightScreen(nav: NavController, vm: AppVm) {
    val home by vm.home.collectAsState()
    Column(Modifier.fillMaxSize().background(Chandan)) {
        MorTopBar("Log weight", onBack = { nav.popBackStack() })
        Spacer(Modifier.height(20.dp))
        WeightSheet(home?.weight_kg ?: 72.0, {
            vm.refreshHome()
            nav.popBackStack()
        }, vm)
    }
}

// ---------- Streak ----------

@Composable
fun StreakScreen(nav: NavController, vm: AppVm) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<dev.mor.app.data.StreakInfo?>(null) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val s = vm.api().streak()
                withContext(Dispatchers.Main) { data = s }
            } catch (_: Exception) { }
        }
    }
    val now = LocalDate.now()
    val monthName = now.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
    Column(Modifier.fillMaxSize().background(Color(0xFFFDEBC8))) {
        MorTopBar(onBack = { nav.popBackStack() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp, 0.dp, 16.dp, 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("🪔", fontSize = 34.sp)
                Neelu("flex", 130.dp)
                Text("🪔", fontSize = 34.sp)
            }
            H1("${data?.streak ?: 0} days streak!", 38, TextAlign.Center)
            Muted("Every logged day lights a diya.", align = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            MorCard(Modifier.fillMaxWidth()) {
                Column {
                    Text("$monthName ${now.year}", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    val days = data?.days ?: emptyList()
                    val lead = data?.first_weekday ?: 0
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row {
                            listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                                Text(d, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Sub,
                                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                            }
                        }
                        ((0 until lead).map { null } + days).chunked(7).forEach { week ->
                            Row {
                                week.forEach { d ->
                                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                        if (d == null) Spacer(Modifier.size(34.dp))
                                        else {
                                            val idx = days.indexOf(d) + 1
                                            Box(
                                                Modifier.size(34.dp).clip(CircleShape)
                                                    .background(if (d.lit) Gold else Color.Transparent)
                                                    .border(if (d.today) 2.dp else 0.dp, Teal, CircleShape),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text("$idx", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                                    color = if (d.lit) Amber
                                                        else if (d.future) Line2 else Sub)
                                            }
                                        }
                                    }
                                }
                                if (week.size < 7) repeat(7 - week.size) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Text("Best: ${data?.best ?: 0} days", fontSize = 13.sp, color = Sub,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(10.dp))
            MorCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("❄️", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text("Streak freeze ×${data?.freezes ?: 2} — saves a missed day",
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
            MorButton("Share") { /* Android share sheet omitted in v0.1 */ }
            Spacer(Modifier.height(10.dp))
            MorButton("Keep it going") { nav.navigate("home") }
        }
    }
}

// ---------- Challenges ----------

@Composable
fun ChallengesScreen(nav: NavController, vm: AppVm) {
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<dev.mor.app.data.Challenges?>(null) }
    var badge by remember { mutableStateOf(false) }
    fun load() {
        scope.launch(Dispatchers.IO) {
            try {
                val c = vm.api().challenges()
                withContext(Dispatchers.Main) { data = c }
            } catch (_: Exception) { }
        }
    }
    LaunchedEffect(Unit) { load() }
    val d = data
    if (d == null) {
        Box(Modifier.fillMaxSize().background(Chandan), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }
    // Wall surface: feather mosaic behind solid Chandan (~42%), per redesign spec.
    Box(Modifier.fillMaxSize().background(Chandan)) {
        MosaicWall(alpha = 0.42f)
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Teal).statusBarsPadding().padding(16.dp, 18.dp),
                verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Challenges", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, fontFamily = MorDisplay)
                Text("Small wins every day", fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            }
            Pill("🪙 ${d.coins} coins", Gold, Amber)
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp, 8.dp, 16.dp, 24.dp)) {
            @Composable
            fun Group(title: String, sub: String, list: List<Challenge>) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                    Text(sub, fontSize = 13.sp, color = Sub)
                }
                Spacer(Modifier.height(8.dp))
                list.forEach { c ->
                    MorCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (c.done) "✅" else "🎯", fontSize = 22.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(c.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    LinearProgressIndicator(
                                        progress = { (c.progress / c.target.coerceAtLeast(1.0))
                                            .coerceIn(0.0, 1.0).toFloat() },
                                        modifier = Modifier.weight(1f),
                                        color = if (c.done) Panna else Teal,
                                        trackColor = Color(0xFFEFE9DC))
                                    Spacer(Modifier.width(8.dp))
                                    Text("${minOf(c.progress, c.target).toInt()}/${c.target.toInt()}",
                                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            if (c.done && !c.claimed) {
                                Box(Modifier.clip(RoundedCornerShape(22.dp)).background(Teal)
                                    .clickable {
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                val r = vm.api().claim(mapOf("key" to c.key))
                                                withContext(Dispatchers.Main) {
                                                    vm.showSnack("+${r.awarded} coins!")
                                                    load()
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    vm.showSnack("Claim failed: ${friendlyErr(e)}", true)
                                                }
                                            }
                                        }
                                    }.padding(14.dp, 10.dp)) {
                                    Text("+${c.coins}", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            } else if (c.claimed) Pill("Done", GreenBg, Green)
                            else Text("🪙${c.coins}", fontSize = 13.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
            Group("Today", "small wins", d.challenges.filter { it.scope == "today" })
            Group("This week", "resets Sunday", d.challenges.filter { it.scope == "week" })
            MorCard(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().clickable { badge = true },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("🏅", fontSize = 26.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Badges", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("${d.badges_unlocked} unlocked", fontSize = 13.sp, color = Sub)
                    }
                    Text("View ›", color = Teal, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        }
        MorBottomNav("challenges", Modifier.align(Alignment.BottomCenter)) { nav.navigate(it) }
    }
    if (badge) BadgeModal(vm) { badge = false }
}

@Composable
fun BadgeModal(vm: AppVm, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    var awarded by remember { mutableStateOf(false) }
    MorSheet(onClose) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Neelu("dance", 130.dp)
            Spacer(Modifier.height(8.dp))
            Text("🏅", fontSize = 44.sp)
            Text("Protein Pro", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
            Muted("Protein goal, 3 days in a row.", 13, TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Pill("+50 coins", Color(0xFFFFF3DC), Amber)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.weight(1f)) { MorSecondaryButton("Share") { } }
                Box(Modifier.weight(1f)) {
                    MorButton(if (awarded) "Collected" else "Collect", enabled = !awarded) {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val r = vm.api().awardBadge(mapOf("key" to "protein_pro"))
                                withContext(Dispatchers.Main) {
                                    awarded = true
                                    vm.showSnack("Badge collected · +${r.awarded} coins")
                                    onClose()
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    vm.showSnack("Award failed: ${friendlyErr(e)}", true)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
