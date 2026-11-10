package dev.mor.app.ui

import android.content.Intent
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import dev.mor.app.data.AppVm
import dev.mor.app.data.Notif
import dev.mor.app.data.friendlyErr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun timeAgo(iso: String): String {
    // Backend timestamps are UTC ("YYYY-MM-DD HH:MM:SS"); render relative to
    // the device clock, not as naive local time (C5).
    return try {
        val d = LocalDateTime.parse(iso.replace(" ", "T"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME).atZone(java.time.ZoneOffset.UTC)
        val mins = maxOf(0, java.time.Duration.between(d, java.time.ZonedDateTime.now()).toMinutes())
        if (mins < 1) "just now"
        else if (mins < 60) "$mins min ago"
        else if (mins < 1440) "${mins / 60} h ago"
        else "Yesterday"
    } catch (_: Exception) { "" }
}

// ---------- Inbox ----------

@Composable
fun InboxScreen(nav: NavController, vm: AppVm) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Notif>?>(null) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            try {
                val j = vm.api().notifications()
                withContext(Dispatchers.Main) { items = j.items }
            } catch (_: Exception) { withContext(Dispatchers.Main) { items = emptyList() } }
        }
    }
    val list = items
    if (list == null) {
        Box(Modifier.fillMaxSize().background(Chandan), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }
    // Wall surface: feather mosaic behind solid Chandan (~42%), per redesign spec.
    Box(Modifier.fillMaxSize().background(Chandan)) {
        MosaicWall(alpha = 0.42f)
        Column(Modifier.fillMaxSize()) {
            MorTopBar("Notifications", onBack = { nav.navigate("home") }, right = {
            Text("Mark all read", fontSize = 13.sp, color = Teal,
                modifier = Modifier.clickable {
                    scope.launch(Dispatchers.IO) {
                        try { vm.api().markRead(mapOf()) } catch (_: Exception) { }
                        vm.refreshHome()
                        withContext(Dispatchers.Main) { items = list.map { it.copy(read = true) } }
                    }
                })
        })
        if (list.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Neelu("sleep", 160.dp)
                Text("All caught up", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
                Muted("No new reminders. Neelu is napping.", align = TextAlign.Center)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.width(220.dp)) {
                    MorSecondaryButton("Reminder settings") { nav.navigate("reminders") }
                }
            }
        } else {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp, 0.dp, 16.dp, 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                list.filter { it.today }.takeIf { it.isNotEmpty() }?.let { today ->
                    Text("TODAY", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                    today.forEach { NotifRow(it, nav, vm) { items = list.map { n -> if (n.id == it.id) it.copy(read = true) else n } } }
                }
                list.filter { !it.today }.takeIf { it.isNotEmpty() }?.let { old ->
                    Text("YESTERDAY", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                    old.forEach { NotifRow(it, nav, vm) { items = list.map { n -> if (n.id == it.id) it.copy(read = true) else n } } }
                }
            }
        }
    }
    }
}

@Composable
private fun NotifRow(n: Notif, nav: NavController, vm: AppVm, onRead: () -> Unit) {
    val scope = rememberCoroutineScope()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = if (n.read) 0.65f else 1f))
            .clickable {
                scope.launch(Dispatchers.IO) {
                    try { vm.api().markRead(mapOf("id" to n.id)) } catch (_: Exception) { }
                    vm.refreshHome()
                    withContext(Dispatchers.Main) {
                        onRead()
                        nav.navigate(when (n.type) {
                            "badge" -> "challenges"
                            "missed_meal" -> "add"
                            else -> "home"
                        })
                    }
                }
            }.padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(Mint2),
            contentAlignment = Alignment.Center) {
            Text(when (n.type) {
                "missed_meal" -> "🍽️"; "badge" -> "🏅"; "streak" -> "🪔"; else -> "💧"
            }, fontSize = 20.sp)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(n.title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (!n.read) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.size(8.dp).clip(CircleShape).background(Kumkum))
                }
            }
            Text(n.body, fontSize = 13.sp, color = Sub, maxLines = 1)
        }
        Text(timeAgo(n.created_at), fontSize = 13.sp, color = Sub)
    }
}

// ---------- Profile ----------

@Composable
fun ProfileScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    val home by vm.home.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmDelete by remember { mutableStateOf(false) }
    var editServer by remember { mutableStateOf(false) }
    var devTaps by remember { mutableStateOf(0) }
    var serverText by remember(vm.store.baseUrl) { mutableStateOf(vm.store.baseUrl) }

    if (profile == null || home == null) {
        Box(Modifier.fillMaxSize().background(Chandan), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }
    val p = profile!!
    val h = home!!
    val plan = p.plan

    @Composable
    fun Row2(label: String, value: String, to: String? = null, onClick: (() -> Unit)? = null) {
        Row(
            Modifier.fillMaxWidth().clickable(enabled = to != null || onClick != null) {
                if (onClick != null) onClick() else if (to != null) nav.navigate(to)
            }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, fontSize = 13.sp, color = Sub, modifier = Modifier.weight(1f))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (to != null) { Spacer(Modifier.width(4.dp)); Text("›", color = Sub) }
        }
    }

    // Wall surface: feather mosaic behind solid Chandan (~42%), per redesign spec.
    Box(Modifier.fillMaxSize().background(Chandan)) {
        MosaicWall(alpha = 0.42f)
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Column(Modifier.fillMaxWidth().background(Teal).statusBarsPadding().padding(16.dp, 20.dp)) {
                Text(p.name ?: "Guest", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay,
                    color = Color.White)
                Text("🔥 ${h.streak}-day streak   🪙 ${h.coins} coins",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            }
            Column(Modifier.padding(16.dp, 12.dp, 16.dp, 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MorCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("☁️", fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Text("Your data lives on your private Mor server.",
                            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f))
                    }
                }
                Column {
                    Text("YOUR PLAN", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                    MorCard {
                        Column {
                            Row2("Goal", if (plan != null) "${p.weight_kg} → ${p.target_weight_kg} kg" else "—", "target")
                            Row2("Daily target", if (plan != null) "%,d kcal".format(plan.kcal_target) else "—", "plan")
                            Row2("Diet", p.diet ?: "—", "diet")
                        }
                    }
                }
                Column {
                    Text("APP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                    MorCard {
                        Column {
                            Row2("Reminders",
                                if (p.reminders_on == 1) "${p.meal_times?.size ?: 0} on" else "Off",
                                "reminders")
                            Row2("Units", if (p.units == "kg-ft") "kg · ft" else p.units ?: "—")
                        }
                    }
                }
                Column {
                    Text("DATA", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                    MorCard {
                        Column {
                            Row2("Export data", "Download") {
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val prof = vm.api().profile()
                                        val ins = vm.api().insights()
                                        val not = vm.api().notifications()
                                        val json = com.google.gson.GsonBuilder().setPrettyPrinting().create()
                                            .toJson(mapOf("profile" to prof, "insights" to ins,
                                                "notifications" to not))
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
                            Row(
                                Modifier.fillMaxWidth().clickable { confirmDelete = true }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Delete all data", color = Red, fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
                // Developer-only server setting: tap the version 5 times (item 35).
                Text("Mor v0.1 · android build", fontSize = 13.sp, color = Sub,
                    modifier = Modifier.fillMaxWidth().clickable {
                        devTaps++
                        if (devTaps >= 5) { devTaps = 0; editServer = true }
                    }, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
            }
        }
        }
        MorBottomNav("profile", Modifier.align(Alignment.BottomCenter)) { nav.navigate(it) }
    }

    if (editServer) {
        AlertDialog(
            onDismissRequest = { editServer = false },
            title = { Text("Backend URL") },
            text = {
                Column {
                    Text("e.g. http://192.168.1.5:4000", fontSize = 13.sp, color = Sub)
                    Spacer(Modifier.height(8.dp))
                    MorField(serverText, { serverText = it }, "http://…:4000")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (serverText.isNotBlank()) {
                        vm.store.baseUrl = serverText
                        vm.rebuildApi()
                        vm.refreshProfile()
                        vm.refreshHome()
                    }
                    editServer = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editServer = false }) { Text("Cancel") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ALL your data?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch(Dispatchers.IO) {
                        try { vm.api().reset() } catch (_: Exception) { }
                        withContext(Dispatchers.Main) {
                            vm.showSnack("Data cleared")
                            vm.refreshProfile()
                            vm.refreshHome()
                            nav.navigate("splash") { popUpTo(0) }
                        }
                    }
                }) { Text("Delete", color = Red) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
        )
    }
}

// ---------- Reminders ----------

@Composable
fun RemindersScreen(nav: NavController, vm: AppVm) {
    val profile by vm.profile.collectAsState()
    val scope = rememberCoroutineScope()
    var times by remember(profile?.meal_times) { mutableStateOf(profile?.meal_times ?: emptyMap()) }
    val off = profile?.reminders_on != 1
    val slots = listOf("breakfast", "lunch", "snack", "dinner")

    // Wall surface: feather mosaic behind solid Chandan (~42%), per redesign spec.
    Box(Modifier.fillMaxSize().background(Chandan)) {
        MosaicWall(alpha = 0.42f)
        Column(Modifier.fillMaxSize()) {
            MorTopBar("Reminders", onBack = { vm.refreshHome(); nav.popBackStack() })
        if (off) {
            Row(Modifier.fillMaxWidth().background(RedBg).padding(16.dp, 10.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Notifications are off", color = Red, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f))
                Box(Modifier.clip(RoundedCornerShape(22.dp)).background(Teal)
                    .clickable { vm.saveProfile(mapOf("reminders_on" to true)) }
                    .padding(14.dp, 8.dp)) {
                    Text("Turn on", color = Color.White, fontSize = 14.sp)
                }
            }
        }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp, 12.dp, 16.dp, 24.dp)) {
            Text("MEALS", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
            Spacer(Modifier.height(6.dp))
            MorCard {
                Column {
                    slots.forEach { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(s.replaceFirstChar { it.uppercase() }, fontSize = 15.sp,
                                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Pill(times[s] ?: "—", Color.White, Ink)
                            Spacer(Modifier.width(8.dp))
                            MorToggle(times.containsKey(s)) { v ->
                                val next = if (v) times + (s to when (s) {
                                    "breakfast" -> "08:30"; "lunch" -> "13:30"
                                    "snack" -> "17:00"; else -> "21:00"
                                }) else times - s
                                times = next
                                scope.launch(Dispatchers.IO) {
                                    try { vm.api().saveProfile(mapOf("meal_times" to next)) } catch (_: Exception) { }
                                    withContext(Dispatchers.Main) { vm.refreshProfile() }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("NUDGES", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
            Spacer(Modifier.height(6.dp))
            MorCard {
                Column {
                    listOf("Missed meal" to "Once, 90 min later",
                        "Streak saver" to "At 10:00 PM",
                        "Water" to "Every 2 hours",
                        "Weekly report" to "Sundays").forEach { (label, when_) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(label, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(when_, fontSize = 13.sp, color = Sub)
                            }
                            MorToggle(true) { }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            MorCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Neelu("sleepy", 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Quiet hours: 10:30 PM – 7:00 AM", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Pill("Max 4/day", Mint2, Teal)
                }
            }
            if (off) Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Neelu("hungry", 140.dp)
            }
        }
    }
    }
}
