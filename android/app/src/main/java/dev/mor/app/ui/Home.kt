package dev.mor.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.navigation.NavController
import dev.mor.app.R
import dev.mor.app.data.AppVm
import dev.mor.app.data.friendlyErr
import dev.mor.app.data.relevantSlot
import kotlinx.coroutines.launch

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

private val QA_STEP_CIRCLE = Color(0x140F5560) // rgba(15,85,96,.08)

/** M1 "Log a meal" sheet: hungry Neelu head, four slot pills, four ways and
 *  the gold quick-add bar. Routes and the quick-add log body are unchanged. */
@OptIn(ExperimentalMaterial3Api::class)
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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = H2Cream2,
        dragHandle = { H2Grab() },
        sheetState = sheetState,
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Neelu("hungry", 46.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Log a meal", fontSize = 24.sp, color = H2Ink,
                    fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(QA_STEP_CIRCLE)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(R.drawable.ic_close_x), null,
                        tint = H2Ink, modifier = Modifier.size(18.dp),
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("breakfast", "lunch", "snack", "dinner").forEach { s ->
                    val sel = s == slot
                    Box(
                        Modifier
                            .weight(1f)
                            .height(42.dp)
                            .clip(RoundedCornerShape(21.dp))
                            .background(if (sel) H2Teal else Color.White)
                            .border(
                                1.5.dp,
                                if (sel) H2Teal else Color(0x2E0F5560),
                                RoundedCornerShape(21.dp),
                            )
                            .clickable { slot = s },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            s.replaceFirstChar { it.uppercase() }, fontSize = 14.sp,
                            color = if (sel) Color.White else H2Ink,
                            fontFamily = FontFamily.Default,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WayRow(R.drawable.ic_scan_camera, "Scan meal", "camera?slot=$slot", nav)
                WayRow(R.drawable.ic_search, "Search food", "search?slot=$slot", nav)
                WayRow(R.drawable.ic_mic, "Speak", "search?slot=$slot&voice=1", nav)
                WayRow(R.drawable.ic_gallery, "Gallery", "camera?slot=$slot&gallery=1", nav)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(H2QaBg)
                    .border(1.5.dp, H2Gold, RoundedCornerShape(18.dp))
                    .padding(start = 12.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_bolt), null,
                    tint = H2Gold, modifier = Modifier.size(22.dp),
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        "Quick add", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = H2Ink, fontFamily = FontFamily.Default,
                    )
                    Text(
                        "${150 * qaQty} kcal", fontSize = 12.sp, color = H2Muted,
                        fontFamily = FontFamily.Default,
                    )
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(QA_STEP_CIRCLE)
                        .clickable { qaQty = maxOf(1, qaQty - 1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("−", fontSize = 18.sp, color = H2Ink, fontFamily = FontFamily.Default)
                }
                Text(
                    "×$qaQty", fontSize = 13.sp, color = H2Ink,
                    fontFamily = FontFamily.Default,
                    modifier = Modifier.width(22.dp), textAlign = TextAlign.Center,
                )
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(QA_STEP_CIRCLE)
                        .clickable { qaQty = minOf(4, qaQty + 1) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", fontSize = 18.sp, color = H2Ink, fontFamily = FontFamily.Default)
                }
                Box(
                    Modifier
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(H2Teal)
                        .clickable(enabled = !saving) {
                            // Single-flight: ignore repeated taps while a save is in
                            // progress; report success only after the server confirms.
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
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Add", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = Color.White, fontFamily = FontFamily.Default,
                    )
                }
            }
        }
    }
}

/** One "way" row: mint icon circle, label, teal chevron, hair bottom edge. */
@Composable
private fun WayRow(icon: Int, label: String, route: String, nav: NavController) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .clickable { nav.navigate(route) }
            .drawBehind {
                drawLine(
                    Color(0x140F5560),
                    Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f),
                    strokeWidth = 1f,
                )
            }
            .padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(H2Mint),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, tint = H2Teal, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label, fontSize = 16.sp, color = H2Ink,
            fontFamily = FontFamily.Default, modifier = Modifier.weight(1f),
        )
        Icon(
            painterResource(R.drawable.ic_chev), null,
            tint = H2Teal, modifier = Modifier.size(18.dp),
        )
    }
}
