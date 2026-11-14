package dev.mor.app.ui

// Mor · About you · WheelPicker — reference implementation from the final-build
// package (05-about-you-8c), adapted to this package: 5 visible rows of 44dp, a
// maroon centre band, fading neighbours, a tick per step, tap a row to jump to
// it, tap the centre row to type a value.

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.abs

/** One row. `value` is always stored in metric (years, cm, kg). */
data class WheelItem(val value: Float, val label: String, val unit: String)

private val ItemHeight = 44.dp
private const val Visible = 5

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    items: List<WheelItem>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    onTapCentre: () -> Unit,
    contentDescription: String,
    displayFont: FontFamily = Cormorant,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex.coerceAtLeast(0))
    val fling = rememberSnapFlingBehavior(state)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    // The centred row = first visible + its scroll fraction (2 rows of padding top and bottom).
    val centre by remember(items) {
        derivedStateOf {
            val px = state.layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: 1
            (state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > px / 2) 1 else 0)
                .coerceIn(0, items.lastIndex)
        }
    }
    LaunchedEffect(state, items) {
        snapshotFlow { centre }.distinctUntilChanged().collect {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSelected(it)
        }
    }
    // Programmatic changes (keypad entry, unit switch) scroll the wheel.
    LaunchedEffect(selectedIndex) {
        val target = selectedIndex.coerceIn(0, items.lastIndex)
        if (target != centre) state.animateScrollToItem(target)
    }

    Box(
        modifier
            .height(ItemHeight * Visible)
            .semantics(mergeDescendants = true) {
                this.contentDescription = contentDescription
                stateDescription = items[centre].let { "${it.label} ${it.unit}".trim() }
                setProgress { target ->
                    scope.launch {
                        state.animateScrollToItem(target.toInt().coerceIn(0, items.lastIndex))
                    }
                    true
                }
                progressBarRangeInfo = ProgressBarRangeInfo(centre.toFloat(), 0f..items.lastIndex.toFloat(), items.size - 2)
                customActions = listOf(
                    CustomAccessibilityAction("Increase") {
                        scope.launch { state.animateScrollToItem((centre + 1).coerceAtMost(items.lastIndex)) }
                        true
                    },
                    CustomAccessibilityAction("Decrease") {
                        scope.launch { state.animateScrollToItem((centre - 1).coerceAtLeast(0)) }
                        true
                    },
                    CustomAccessibilityAction("Type a value") { onTapCentre(); true },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Centre band.
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .height(ItemHeight)
                .shadow(6.dp, RoundedCornerShape(12.dp), spotColor = Color(0x387A1D16))
                .background(MorColor.Maroon, RoundedCornerShape(12.dp))
        )
        LazyColumn(
            state = state,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = ItemHeight * 2),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    // Fade the top and bottom rows (mask 0 → 1 → 1 → 0).
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Transparent, 0.3f to Color.Black,
                            0.7f to Color.Black, 1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            itemsIndexed(items) { i, item ->
                val d = abs(i - centre)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(ItemHeight)
                        .graphicsLayer {
                            val s = when (d) { 0 -> 1f; 1 -> .94f; else -> .86f }
                            scaleX = s; scaleY = s
                            alpha = when (d) { 0 -> 1f; 1 -> .85f; else -> .45f }
                        }
                        .clickable {
                            if (i == centre) onTapCentre()
                            else scope.launch { state.animateScrollToItem(i) }
                        },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (d == 0) {
                        Text(
                            item.label, fontFamily = displayFont,
                            fontWeight = FontWeight.SemiBold, fontSize = 30.sp,
                            color = MorColor.CreamText,
                        )
                        if (item.unit.isNotEmpty()) {
                            Text(
                                " ${item.unit}", fontFamily = MorInter,
                                fontSize = 11.sp, color = Color(0xFFF3D9A6),
                            )
                        }
                    } else {
                        Text(item.label, fontFamily = MorInter, fontSize = 18.sp, color = Color(0xFF7D6F5A))
                    }
                }
            }
        }
    }
}
