package dev.mor.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import dev.mor.app.data.AppVm
import dev.mor.app.data.FoodItem
import dev.mor.app.data.friendlyErr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

// ---------- Search ----------

@Composable
fun SearchScreen(nav: NavController, vm: AppVm, slot: String, q0: String, voice0: Boolean) {
    MorSystemBars(dark = false)
    val ctx = LocalContext.current
    var q by remember { mutableStateOf(q0) }
    var rows by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var tab by remember { mutableStateOf("Recent") }
    var cart by remember { mutableStateOf<List<CartLine>>(emptyList()) }
    var searched by remember { mutableStateOf(q0.isNotEmpty()) }
    var voice by remember { mutableStateOf(voice0) }
    var estimating by remember { mutableStateOf(false) }
    // Quantity/servings step (item 5): the bar opens a sheet where every
    // line gets −/+ steppers, so "two boiled eggs" logs as one ×2 line.
    var cartSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // v4 #10: Recent shows this user's last 20 logged foods, never the DB dump.
    var recentRows by remember { mutableStateOf<List<FoodItem>?>(null) }
    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val items = runCatching { vm.api().recentFoods().items }.getOrDefault(emptyList())
            withContext(Dispatchers.Main) { recentRows = items }
        }
    }

    // v4 #10: the voice review card — one row per detected item, each matched
    // through the normal food search with the good-match rule (never by index).
    var review by remember { mutableStateOf<List<ReviewRow>?>(null) }
    var replacing by remember { mutableStateOf<Int?>(null) }
    var matching by remember { mutableStateOf(false) }
    var clientKey by remember { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }

    fun runSearch(query: String) {
        scope.launch(Dispatchers.IO) {
            try {
                val j = vm.api().foods(q = query)
                withContext(Dispatchers.Main) { rows = j.items; searched = true }
            } catch (_: Exception) { }
        }
    }
    // An empty query never touches the food database — the tabs own the list.
    LaunchedEffect(q) {
        if (q.isBlank()) { rows = emptyList(); searched = false }
        else runSearch(q)
    }

    // v4 #10: a new user lands on Recent with Search focused.
    LaunchedEffect(recentRows, tab, review, q) {
        if (tab == "Recent" && recentRows?.isEmpty() == true &&
            review == null && q.isBlank()
        ) runCatching { searchFocus.requestFocus() }
    }

    fun logCart() {
        scope.launch(Dispatchers.IO) {
            try {
                val r = vm.api().logMeal(mapOf(
                    "slot" to slot, "source" to "search",
                    "name" to if (cart.size == 1) cart[0].name else "Mixed meal",
                    "items" to cart.map {
                        mapOf("name" to it.name, "kcal" to Math.round(it.kcal * it.qty),
                            "protein_g" to it.protein, "carbs_g" to it.carbs,
                            "fat_g" to it.fat, "portion" to "${it.portion} ×${it.qty}")
                    }))
                vm.lastLog = r
                withContext(Dispatchers.Main) { nav.navigate("logged") }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { vm.showSnack("Log failed: ${friendlyErr(e)}", true) }
            }
        }
    }

    // v4 #10: the voice review card's write path. All rows go through the
    // normal log endpoint with the idempotency key, then "Logged!" → Home.
    fun logReview(rowsToLog: List<ReviewRow>) {
        if (clientKey.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            try {
                val r = vm.api().logMeal(mapOf(
                    "slot" to slot, "source" to "voice",
                    "name" to if (rowsToLog.size == 1)
                        (rowsToLog[0].match?.name ?: rowsToLog[0].detected) else "Voice meal",
                    "client_key" to clientKey,
                    "items" to rowsToLog.map {
                        val m = it.match
                        mapOf("name" to (m?.name ?: it.detected),
                            "kcal" to Math.round(m?.kcal ?: it.kcal),
                            "protein_g" to Math.round(m?.protein_g ?: it.protein),
                            "carbs_g" to Math.round(m?.carbs_g ?: it.carbs),
                            "fat_g" to Math.round(m?.fat_g ?: it.fat),
                            "portion" to (m?.portion ?: it.portion))
                    }))
                vm.lastLog = r
                vm.refreshHome()
                withContext(Dispatchers.Main) {
                    review = null
                    replacing = null
                    nav.navigate("logged")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { vm.showSnack("Log failed: ${friendlyErr(e)}", true) }
            }
        }
    }

    // v4 #10: run the normal food search per detected item and attach the good
    // match. Recent (logged-before) names feed the preference; never an index.
    fun matchDetectedItems(items: List<FoodItem>) {
        matching = true
        scope.launch(Dispatchers.IO) {
            val recentNorm = runCatching { vm.api().recentFoods().items }
                .getOrDefault(emptyList()).map { normalizeFoodName(it.name) }.toSet()
            val diet = vm.profile.value?.diet
            val matched = items.map {
                pickMatch({ qq -> vm.api().foods(q = qq).items }, it.name, diet, recentNorm)
            }
            withContext(Dispatchers.Main) {
                val cur = review
                if (cur != null && cur.size == matched.size) {
                    review = cur.zip(matched) { row, m -> row.copy(match = m) }
                }
                matching = false
            }
        }
    }

    val cartTotal = cart.sumOf { it.kcal * it.qty }.toInt()

    Box(Modifier.fillMaxSize().background(Chandan).imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp, 14.dp, 16.dp, 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White)
                .border(1.dp, Line, CircleShape).clickable { if (!nav.popBackStack()) nav.navigate("add") },
                contentAlignment = Alignment.Center) { Text("✕", fontSize = 16.sp) }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                MorField(q, { q = it }, "Search dal, paneer, dosa…",
                    modifier = Modifier.focusRequester(searchFocus))
            }
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).background(Teal)
                .clickable { voice = true }, contentAlignment = Alignment.Center) {
                Text("🎙️", fontSize = 20.sp)
            }
        }
        Row(Modifier.padding(16.dp, 4.dp, 16.dp, 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Recent", "Saved", "Restaurant").forEach { t ->
                Chip(t, tab == t) { tab = t }
            }
        }

        if (voice) {
            VoicePanel(
                initial = q,
                onClose = { voice = false },
                // v4 #10: suspend parse with an 8 s hard timeout and a Boolean
                // result — "Add items" can never stick on "Reading…".
                onParse = { transcript ->
                    val r = withTimeoutOrNull(8000) {
                        vm.api().voiceLog(mapOf("transcript" to transcript))
                    }
                    val items = r?.items
                    if (items.isNullOrEmpty()) {
                        false
                    } else {
                        clientKey = java.util.UUID.randomUUID().toString()
                        review = items.map {
                            ReviewRow(it.name, it.portion ?: "1 serving", null,
                                it.kcal, it.protein_g, it.carbs_g, it.fat_g)
                        }
                        replacing = null
                        voice = false
                        matchDetectedItems(items)
                        true
                    }
                },
            )
        } else {
            Box(Modifier.weight(1f)) {
                val rev = review
                val rp = replacing
                when {
                    // Replace mode: search results for the detected name; a tap
                    // swaps the row's match. Never falls back to any list row 0.
                    rev != null && rp != null && rp in rev.indices -> ReplaceList(
                        rev[rp], rows, q, searched,
                        onCancel = { replacing = null },
                        onPick = { f ->
                            review = rev.mapIndexed { i, r ->
                                if (i == rp) r.copy(match = f) else r
                            }
                            replacing = null
                        },
                    )

                    // Review card: one row per detected item with its DB match.
                    rev != null -> ReviewCard(rev, matching,
                        onClear = { review = null; replacing = null },
                        onRowTap = { i ->
                            replacing = i
                            q = rev[i].detected
                        })

                    q.isNotBlank() -> {
                        if (rows.isEmpty() && searched) {                            Column(Modifier.fillMaxSize().padding(30.dp),
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Neelu("think", 140.dp)
                                Text("No match for “$q”", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
                                Muted("Neelu can estimate it from its name.", align = TextAlign.Center)
                                Spacer(Modifier.height(10.dp))
                                MorButton(if (estimating) "Estimating…" else "Estimate with Neelu",
                                    enabled = !estimating && q.isNotEmpty()) {
                                    scope.launch(Dispatchers.IO) {
                                        estimating = true
                                        try {
                                            val r = vm.api().estimate(mapOf("name" to q))
                                            withContext(Dispatchers.Main) {
                                                rows = listOf(r.item) + rows
                                                cart = cart + CartLine(r.item.name, r.item.kcal,
                                                    r.item.protein_g, r.item.carbs_g, r.item.fat_g,
                                                    r.item.portion ?: "1 serving", 1.0)
                                                vm.showSnack("Neelu estimates ${r.item.kcal.toInt()} kcal")
                                            }
                                        } catch (_: Exception) {
                                            withContext(Dispatchers.Main) { vm.showSnack("Estimate failed", true) }
                                        }
                                        estimating = false
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                MorSecondaryButton("Create custom food") {
                                    cart = cart + CartLine(q, 200.0, 4.0, 20.0, 8.0, "1 serving", 1.0)
                                }
                                Spacer(Modifier.height(8.dp))
                                MorSecondaryButton("Scan instead") { nav.navigate("camera?slot=$slot") }
                            }
                        } else {
                            FoodList(rows, cart, nav, slot, onCartToggle = { f ->
                                cart = if (cart.any { it.name == f.name })
                                    cart.filter { it.name != f.name }
                                else cart + CartLine(f.name, f.kcal, f.protein_g,
                                    f.carbs_g, f.fat_g, f.portion ?: "1 serving", 1.0)
                            })
                        }
                    }

                    // v4 #10: tabs hold real user data or an empty state — the
                    // food database is never dumped into them.
                    tab == "Recent" -> {
                        val r = recentRows
                        when {
                            r == null -> Box(Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Teal)
                            }
                            r.isEmpty() -> RecentEmpty(searchFocus)
                            else -> FoodList(r, cart, nav, slot, onCartToggle = { f ->
                                cart = if (cart.any { it.name == f.name })
                                    cart.filter { it.name != f.name }
                                else cart + CartLine(f.name, f.kcal, f.protein_g,
                                    f.carbs_g, f.fat_g, f.portion ?: "1 serving", 1.0)
                            })
                        }
                    }
                    else -> EmptyTab(
                        if (tab == "Saved") "Foods you save will show up here"
                        else "Restaurant meals you add will show up here",
                    )
                }
            }
        }

        val rev = review
        if (rev != null && replacing == null && !voice) {
            // v4 #10: the review card's write bar. All rows log in one call.
            Row(
                Modifier.fillMaxWidth().background(Color.White)
                    .border(1.dp, Line).padding(16.dp, 10.dp, 16.dp, 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("${slot.replaceFirstChar { it.uppercase() }} · ${rev.size} item${if (rev.size > 1) "s" else ""}",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(rev.joinToString(" · ") { it.match?.name ?: it.detected },
                        fontSize = 13.sp, color = Sub, maxLines = 1)
                    Text(if (matching) "Matching…" else "Tap a row to change the match",
                        fontSize = 12.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.width(160.dp)) {
                    MorButton("Log ${rev.sumOf { (it.match?.kcal ?: it.kcal).toInt() }} kcal",
                        enabled = !matching) { logReview(rev) }
                }
            }
        }
        if (cart.isNotEmpty() && rev == null && !voice) {
            Row(
                Modifier.fillMaxWidth().background(Color.White)
                    .border(1.dp, Line).padding(16.dp, 10.dp, 16.dp, 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).clickable { cartSheet = true }) {
                    Text("${slot.replaceFirstChar { it.uppercase() }} · ${cart.size} item${if (cart.size > 1) "s" else ""}",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(cart.joinToString(" · ") { "${it.name} ×${it.qty}" },
                        fontSize = 13.sp, color = Sub, maxLines = 1)
                    Text("Tap to change servings", fontSize = 12.sp, color = Teal,
                        fontWeight = FontWeight.SemiBold)
                }
                Box(Modifier.width(160.dp)) { MorButton("Log $cartTotal kcal") { logCart() } }
            }
        }
        if (cartSheet) {
            MorSheet({ cartSheet = false }) {
                Text("Servings", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
                Spacer(Modifier.height(4.dp))
                Muted("Set how much of each item you ate.", 13)
                Spacer(Modifier.height(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    cart.forEach { line ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(line.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("${Math.round(line.kcal * line.qty)} kcal · ${line.portion}",
                                    fontSize = 13.sp, color = Sub)
                            }
                            Box(Modifier.size(38.dp).clip(CircleShape).background(Mint2)
                                .clickable {
                                    cart = cart.map {
                                        if (it.name == line.name) it.copy(qty = maxOf(0.5, Math.round((it.qty - 0.5) * 2) / 2.0))
                                        else it
                                    }
                                }, contentAlignment = Alignment.Center) {
                                Text("−", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
                            }
                            Text("×${line.qty}", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                            Box(Modifier.size(38.dp).clip(CircleShape).background(Mint2)
                                .clickable {
                                    cart = cart.map {
                                        if (it.name == line.name) it.copy(qty = minOf(10.0, it.qty + 0.5))
                                        else it
                                    }
                                }, contentAlignment = Alignment.Center) {
                                Text("+", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
                            }
                            Spacer(Modifier.width(4.dp))
                            Box(Modifier.clickable {
                                cart = cart.filter { it.name != line.name }
                                if (cart.isEmpty()) cartSheet = false
                            }.padding(8.dp)) { Text("✕", fontSize = 16.sp, color = Sub) }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                MorButton("Log $cartTotal kcal") { cartSheet = false; logCart() }
            }
        }
        }
    }
}

private data class CartLine(
    val name: String, val kcal: Double, val protein: Double, val carbs: Double,
    val fat: Double, val portion: String, val qty: Double,
)

/** v4 #10: one detected item and the food it was matched to in the DB. */
private data class ReviewRow(
    val detected: String,
    val portion: String,
    val match: FoodItem?,
    val kcal: Double, val protein: Double, val carbs: Double, val fat: Double,
)

/** The voice review card: one row per detected item, matched name + qty + kcal.
 *  A row with no good match reads "Find 'x'"; tapping any row opens Search
 *  pre-filled with the detected text. */
@Composable
private fun ReviewCard(
    rows: List<ReviewRow>,
    matching: Boolean,
    onClear: () -> Unit,
    onRowTap: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Review items", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MorDisplay)
                Text("Check what Neelu heard before logging.", fontSize = 13.sp, color = Sub)
            }
            Box(Modifier.clickable(onClick = onClear).padding(8.dp)) {
                Text("✕", fontSize = 16.sp, color = Sub)
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            rows.forEachIndexed { i, row ->
                val m = row.match
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                        .background(Color.White).border(1.dp, Line, RoundedCornerShape(14.dp))
                        .clickable { onRowTap(i) }.padding(12.dp, 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(emojiForFood(m?.name ?: row.detected), fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        if (m != null) {
                            Text(m.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${m.portion ?: row.portion} · ${m.kcal.toInt()} kcal",
                                fontSize = 13.sp, color = Sub)
                        } else {
                            Text("Find “${row.detected}”", fontSize = 15.sp,
                                fontWeight = FontWeight.Bold, color = Kumkum)
                            Text("${row.portion} · ~${row.kcal.toInt()} kcal",
                                fontSize = 13.sp, color = Sub)
                        }
                    }
                    Pill(if (m == null) "Find" else "Matched",
                        if (m == null) LavBg else Mint2,
                        if (m == null) Jamun else Teal)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (matching) {
                Text("Matching…", fontSize = 13.sp, color = Sub,
                    fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(170.dp))
        }
    }
}

/** Replace mode: search results for the detected text, the current match
 *  highlighted. Picking a row swaps it in; nothing else is auto-selected. */
@Composable
private fun ReplaceList(
    row: ReviewRow,
    results: List<FoodItem>,
    q: String,
    searched: Boolean,
    onCancel: () -> Unit,
    onPick: (FoodItem) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp)).background(Mint).padding(12.dp, 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Pick a food for “${row.detected}”", fontSize = 14.sp,
                    fontWeight = FontWeight.Bold)
                Text(row.match?.let { "Current match: ${it.name}" } ?: "No match yet",
                    fontSize = 12.sp, color = Sub)
            }
            Box(Modifier.clickable(onClick = onCancel).padding(6.dp)) {
                Text("✕", fontSize = 15.sp, color = Sub)
            }
        }
        if (results.isEmpty() && searched) {
            Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
                Text("No match for “$q”. Try another spelling.", fontSize = 15.sp,
                    color = Sub, textAlign = TextAlign.Center)
            }
        } else {
            FoodList(results, emptyList(), null, null, highlightName = row.match?.name,
                onPick = onPick)
        }
    }
}

/** One food row, shared by search results and the Recent tab. */
@Composable
private fun FoodList(
    items: List<FoodItem>,
    cart: List<CartLine>,
    nav: NavController?,
    slot: String?,
    highlightName: String? = null,
    onPick: ((FoodItem) -> Unit)? = null,
    onCartToggle: ((FoodItem) -> Unit)? = null,
) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        items.forEach { f ->
            val inCart = cart.any { it.name == f.name }
            Row(
                Modifier.fillMaxWidth().clickable {
                    if (onPick != null) onPick(f)
                    else nav?.navigate("food?name=${Uri.enc(f.name)}&slot=$slot")
                }.padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(emojiForFood(f.name), fontSize = 36.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(vegBadge(f.veg), fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        // weight(fill=false): long names wrap instead of
                        // squeezing the status pill into a vertical strip.
                        Text(f.name, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f, fill = false))
                        Spacer(Modifier.width(6.dp))
                        if (f.name == highlightName) {
                            Pill("Current match", Mint2, Teal)
                        } else {
                            Pill(if (f.verified == 1) "Verified" else "AI",
                                if (f.verified == 1) Mint2 else LavBg,
                                if (f.verified == 1) Teal else Jamun)
                        }
                    }
                    Text("${f.portion ?: ""} · ${f.kcal.toInt()} kcal",
                        fontSize = 13.sp, color = Sub)
                }
                if (onCartToggle != null) {
                    Box(Modifier.size(34.dp).clip(CircleShape)
                        .background(if (inCart) Teal else Color.White)
                        .border(1.5.dp, Teal, CircleShape)
                        .clickable { onCartToggle(f) }, contentAlignment = Alignment.Center) {
                        Text(if (inCart) "✓" else "+", color = if (inCart) Color.White else Teal,
                            fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(150.dp))
    }
}

/** v4 #10: a new user's Recent tab — empty state with Search focused, never
 *  filled from the food database. */
@Composable
private fun RecentEmpty(focus: FocusRequester) {
    Column(
        Modifier.fillMaxSize().padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Neelu("think", 110.dp)
        Spacer(Modifier.height(10.dp))
        Text("Foods you log will show up here", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Muted("Speak, scan or search to add your first meal.", 13, align = TextAlign.Center)
        Spacer(Modifier.weight(1.2f))
    }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
}

@Composable
private fun EmptyTab(text: String) {
    Box(Modifier.fillMaxSize().padding(30.dp), contentAlignment = Alignment.Center) {
        Text(text, fontSize = 15.sp, color = Sub, textAlign = TextAlign.Center)
    }
}

private object Uri {
    fun enc(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")
}

@Composable
private fun VoicePanel(initial: String, onClose: () -> Unit, onParse: suspend (String) -> Boolean) {
    val ctx = LocalContext.current
    var transcript by remember { mutableStateOf(initial) }
    var listening by remember { mutableStateOf(false) }
    var parsing by remember { mutableStateOf(false) }
    var supported by remember { mutableStateOf(true) }
    // v4 #10: a timed-out or failed parse keeps the transcript on screen with
    // a way out — the button never stays disabled with no outcome.
    var parseError by remember { mutableStateOf(false) }
    // Item 7: every mic state says something. Silent no-ops were the bug —
    // no partials, swallowed errors, and dead ends on denied permission.
    var hint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    // v4 #10 (mitigation): hold transient audio focus for the recognition
    // session so other audio apps are asked to pause; the full AudioRecord
    // path from 10-voice-logging/SPEC.md stays the real fix.
    val am = remember {
        ctx.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
    }
    var focusReq by remember { mutableStateOf<android.media.AudioFocusRequest?>(null) }
    fun grabFocus() {
        val req = android.media.AudioFocusRequest.Builder(
            android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT,
        ).setAudioAttributes(
            android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        ).build()
        focusReq = req
        am.requestAudioFocus(req)
    }
    fun dropFocus() {
        focusReq?.let { am.abandonAudioFocusRequest(it) }
        focusReq = null
    }
    val rec = remember {
        if (!SpeechRecognizer.isRecognitionAvailable(ctx)) { supported = false; null }
        else try {
            SpeechRecognizer.createSpeechRecognizer(ctx).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onResults(r: Bundle) {
                        dropFocus()
                        transcript = r.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.joinToString(" ") ?: transcript
                        listening = false
                        hint = null
                    }
                    override fun onError(e: Int) {
                        dropFocus()
                        listening = false
                        hint = "Didn't catch that. Tap Listen and speak again."
                    }
                    override fun onReadyForSpeech(p: Bundle) {
                        hint = null
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(r: Float) {}
                    override fun onBufferReceived(b: ByteArray) {}
                    override fun onEndOfSpeech() { listening = false }
                    override fun onPartialResults(p: Bundle) {
                        p.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.joinToString(" ")?.takeIf { it.isNotBlank() }?.let { transcript = it }
                    }
                    override fun onEvent(t: Int, p: Bundle) {}
                })
            }
        } catch (_: Exception) { supported = false; null }
    }
    DisposableEffect(Unit) {
        onDispose { dropFocus(); rec?.destroy() }
    }

    val audioPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            hint = null
            grabFocus()
            startListening(rec, ctx,
                onStart = { listening = true },
                onFail = { hint = "Couldn't start listening. Try again." })
        } else {
            hint = "Mic is blocked. Allow the microphone in Settings, then Listen."
        }
    }

    Column(
        Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(22.dp))
            .background(Color.White).border(1.dp, Line, RoundedCornerShape(22.dp)).padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill("English", Mint2, Teal)
            Spacer(Modifier.weight(1f))
            Box(Modifier.clickable(onClick = onClose).padding(8.dp)) { Text("✕", fontSize = 18.sp) }
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Neelu("wave", 120.dp) }
        Text(transcript.ifEmpty { "Say: “Two rotis, a bowl of dal and some bhindi…”" },
            fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        val chips = transcript.split(Regex(",| and | with ")).map { it.trim() }
            .filter { it.isNotEmpty() }.take(4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEach { c -> Pill(c, Mint2, Teal) }
        }
        hint?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, fontSize = 13.sp, color = Kumkum, fontWeight = FontWeight.SemiBold)
        }
        if (parseError && !parsing) {
            Spacer(Modifier.height(8.dp))
            Text("Couldn't read that. Try again or type it.",
                fontSize = 13.sp, color = Kumkum, fontWeight = FontWeight.SemiBold)
        }
        if (listening) {
            Spacer(Modifier.height(8.dp))
            Text("● Listening… speak now", fontSize = 13.sp, color = Teal,
                fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                MorSecondaryButton(if (listening) "■ Stop" else "🎙️ Listen") {
                    if (!supported) {
                        hint = "Voice typing isn't on this device. Type or search instead."
                        return@MorSecondaryButton
                    }
                    if (listening) { rec?.stopListening(); dropFocus(); listening = false }
                    else if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED) {
                        grabFocus()
                        startListening(rec, ctx,
                            onStart = { listening = true },
                            onFail = { hint = "Couldn't start listening. Try again." })
                    } else audioPerm.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            Box(Modifier.weight(1f)) {
                MorButton(if (parsing) "Reading…" else "Add items",
                    enabled = transcript.isNotEmpty() && !parsing) {
                    scope.launch {
                        parsing = true
                        parseError = false
                        val ok = try {
                            onParse(transcript)
                        } catch (_: Exception) {
                            false
                        } finally {
                            parsing = false
                        }
                        if (!ok) parseError = true
                    }
                }
            }
        }
    }
}

private fun startListening(
    rec: SpeechRecognizer?, ctx: android.content.Context,
    onStart: () -> Unit, onFail: () -> Unit,
) {
    if (rec == null) {
        onFail()
        return
    }
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
    }
    try {
        rec.startListening(intent)
        onStart()
    } catch (_: Exception) {
        onFail()
    }
}

// ---------- Food item ----------

@Composable
fun FoodItemScreen(nav: NavController, vm: AppVm, name: String, slot: String) {
    MorSystemBars(dark = false)
    val scope = rememberCoroutineScope()
    var food by remember { mutableStateOf<FoodItem?>(null) }
    var unit by remember { mutableStateOf("Bowl") }
    var qty by remember { mutableDoubleStateOf(1.0) }
    var oil by remember { mutableStateOf("normal") }

    LaunchedEffect(name) {
        scope.launch(Dispatchers.IO) {
            try {
                val items = vm.api().foods(q = name).items
                withContext(Dispatchers.Main) { food = items.firstOrNull() }
            } catch (_: Exception) { }
        }
    }

    val f = food
    if (f == null) {
        Box(Modifier.fillMaxSize().background(Chandan), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }
    val oilMult = mapOf("less" to 0.85, "normal" to 1.0, "more" to 1.15)[oil] ?: 1.0
    val kcal = Math.round(f.kcal * qty * oilMult).toInt()
    val protein = f.protein_g * qty
    val carbs = f.carbs_g * qty
    val fat = f.fat_g * qty * (if (oil == "more") 1.25 else if (oil == "less") 0.75 else 1.0)

    Box(Modifier.fillMaxSize().background(Chandan)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().background(Teal).statusBarsPadding().padding(16.dp, 14.dp, 16.dp, 18.dp),
                verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color(0x24FFFFFF))
                .clickable { nav.popBackStack() }, contentAlignment = Alignment.Center) {
                Text("‹", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(vegBadge(f.veg), fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(f.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text(if (f.verified == 1) "Verified · home-style portion" else "AI estimate",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
            }
            Text(emojiForFood(f.name), fontSize = 40.sp)
        }

        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MorCard {
                Column {
                    Text("UNIT", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Bowl", "Plate", "Spoon", "Grams").forEach { u ->
                            Chip(u, unit == u) { unit = u }
                        }
                    }
                }
            }
            MorCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Mint2)
                        .clickable { qty = maxOf(0.5, Math.round((qty - 0.5) * 2) / 2.0) },
                        contentAlignment = Alignment.Center) {
                        Text("−", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$qty ${unit.lowercase(Locale.ENGLISH)}${if (qty > 1) "s" else ""}",
                            fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MorDisplay)
                        if (unit == "Bowl") Text("≈ ${Math.round(150 * qty)} g",
                            fontSize = 13.sp, color = Sub)
                    }
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Mint2)
                        .clickable { qty = minOf(5.0, qty + 0.5) },
                        contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Teal)
                    }
                }
            }
            MorCard {
                Column {
                    Text("OIL LEVEL", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                    Spacer(Modifier.height(8.dp))
                    Segmented(listOf("less", "normal", "more"), oil) { oil = it }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                .background(Mint).padding(16.dp, 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(f.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${protein.toInt()} g protein · ${carbs.toInt()} g carbs · ${fat.toInt()} g fat",
                        fontSize = 13.sp, color = Sub)
                }
                Text("$kcal kcal", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Teal, fontFamily = MorDisplay)
            }
        }

        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp, 0.dp, 16.dp, 22.dp)) {
            Box(Modifier.width(120.dp)) {
                MorSecondaryButton("♡ Save") { vm.showSnack("Saved ${f.name} to Meri thali") }
            }
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                MorButton("Add to $slot · $kcal kcal") {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val r = vm.api().logMeal(mapOf(
                                "slot" to slot, "source" to "search", "name" to f.name,
                                "items" to listOf(mapOf("name" to f.name, "kcal" to kcal,
                                    "protein_g" to Math.round(protein),
                                    "carbs_g" to Math.round(carbs),
                                    "fat_g" to Math.round(fat),
                                    "portion" to "$qty ${unit.lowercase(Locale.ENGLISH)}${if (qty > 1) "s" else ""}"))))
                            vm.lastLog = r
                            vm.refreshHome()
                            withContext(Dispatchers.Main) { nav.navigate("logged") }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                vm.showSnack("Add failed: ${friendlyErr(e)}", true)
                            }
                        }
                    }
                }
            }
        }
    }
}
}

// ---------- Meal detail ----------

fun healthScore(kcal: Double, fat: Double, protein: Double): Int {
    var score = 92 - maxOf(0.0, fat - 18) * 1.1 - (if (kcal > 650) 12 else 0) +
        maxOf(0.0, protein - 12) * 0.5
    return maxOf(40, minOf(96, Math.round(score).toInt()))
}

@Composable
fun MealDetailScreen(nav: NavController, vm: AppVm, id: Long) {
    MorSystemBars(dark = false)
    val scope = rememberCoroutineScope()
    var meal by remember { mutableStateOf<dev.mor.app.data.MealDetail?>(null) }
    LaunchedEffect(id) {
        scope.launch(Dispatchers.IO) {
            try {
                val m = vm.api().meal(id)
                withContext(Dispatchers.Main) { meal = m }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { nav.navigate("home") }
            }
        }
    }
    val m = meal
    if (m == null) {
        Box(Modifier.fillMaxSize().background(Chandan), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Teal)
        }
        return
    }
    val score = healthScore(m.kcal, m.fat_g, m.protein_g)
    val scoreColor = if (score >= 70) Green else Amber

    Box(Modifier.fillMaxSize().background(Chandan)) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(16.dp, 14.dp),
                verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White)
                .border(1.dp, Line, CircleShape).clickable { nav.navigate("home") },
                contentAlignment = Alignment.Center) {
                Text("‹", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text("${m.slot.replaceFirstChar { it.uppercase() }} · ${m.time ?: ""}",
                fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White)
                .border(1.dp, Line, CircleShape)
                .clickable {
                    scope.launch(Dispatchers.IO) {
                        try {
                            vm.api().deleteMeal(m.id)
                            vm.refreshHome()
                            withContext(Dispatchers.Main) {
                                vm.showSnack("Meal removed")
                                nav.navigate("home")
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                vm.showSnack("Delete failed: ${friendlyErr(e)}", true)
                            }
                        }
                    }
                }, contentAlignment = Alignment.Center) {
                Text("🗑️", fontSize = 17.sp)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp, 0.dp, 16.dp, 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MorCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(110.dp).clip(CircleShape).background(Color(0xFF3B2B21))
                        .border(3.dp, Gold, CircleShape), contentAlignment = Alignment.Center) {
                        if (m.scan?.image_path != null) {
                            AsyncImage("${vm.store.baseUrl}/uploads/${m.scan.image_path}",
                                "Meal", modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop)
                        } else Text(emojiForFood(m.name), fontSize = 70.sp)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(m.name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("${m.kcal.toInt()} kcal", fontSize = 40.sp, fontFamily = MorDisplay,
                            fontWeight = FontWeight.ExtraBold, color = Teal)
                        Pill("$score · ${if (score >= 78) "Good" else "Okay"}",
                            Color.White, scoreColor)
                    }
                }
            }
            MorCard {
                Row {
                    listOf(Triple("Carbs", m.carbs_g, Genda), Triple("Protein", m.protein_g, Neel),
                        Triple("Fat", m.fat_g, Pankh)).forEach { (k, v, c) ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(10.dp, 34.dp).clip(RoundedCornerShape(5.dp)).background(c))
                            Spacer(Modifier.height(6.dp))
                            Text("${Math.round(v)} g", fontWeight = FontWeight.Bold)
                            Text(k, fontSize = 13.sp, color = Sub)
                        }
                    }
                }
            }
            MorCard {
                Column {
                    m.items.forEach { it ->
                        Row(Modifier.fillMaxWidth().clickable {
                            nav.navigate("food?name=${Uri.enc(it.name)}&slot=${m.slot}")
                        }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(emojiForFood(it.name), fontSize = 34.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text(it.portion ?: "", fontSize = 13.sp, color = Sub)
                            }
                            Text("${it.kcal.toInt()} kcal", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Mint)
                .padding(14.dp, 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Neelu("happy", 48.dp)
                Spacer(Modifier.width(10.dp))
                Text("Tip: add a bowl of curd for +6 g protein.",
                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            }
            MorButton("Done") {
                vm.refreshHome()
                nav.navigate("home")
            }
        }
    }
}
}
