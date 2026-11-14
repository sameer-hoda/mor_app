package dev.mor.app.ui

// Mor · Onboarding scaffold from the final-build package (02-onboarding-layout):
// art full-bleed on top, one ivory sheet anchored to the bottom edge, used by
// every onboarding step after the palace door.

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

enum class ArtHeight(val height: androidx.compose.ui.unit.Dp) {
    Standard(140.dp), Compact(96.dp), None(0.dp)
}

/** "STEP n OF 6" + 6 segments, 3dp tall, 4dp gaps; gold when filled. */
@Composable
private fun ObStepHeader(step: Int) {
    Column {
        Text(
            "STEP $step OF 6", fontFamily = MorInter, fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em,
            color = MorColor.Eyebrow,
        )
        Spacer(Modifier.height(7.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..6).forEach { i ->
                Box(
                    Modifier
                        .size(width = 34.dp, height = 3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= step) MorColor.Gold else Color(0xFFE6DAC5))
                )
            }
        }
    }
}

/** 26dp mascot avatar + assistant pill. */
@Composable
private fun ObBubble(bubble: String, pose: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        NeeluBitmap(pose, Modifier.size(26.dp), contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                .background(MorColor.Card)
                .border(1.dp, MorColor.SheetBorder, RoundedCornerShape(14.dp, 14.dp, 14.dp, 4.dp))
                .padding(horizontal = 11.dp, vertical = 6.dp)
        ) {
            Text(
                bubble, fontFamily = MorInter, fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium, color = MorColor.Ink2,
            )
        }
    }
}

@Composable
fun ObPrimaryButton(
    text: String, enabled: Boolean, disabledText: String? = null, onClick: () -> Unit,
) {
    val label = if (!enabled && disabledText != null) disabledText else text
    Box(
        Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(MorShape.Button))
            .background(if (enabled) MorColor.Maroon else MorColor.CtaDisabledBg)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontFamily = MorInter, fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MorColor.CreamText else MorColor.Muted,
        )
    }
}

/**
 * The one layout for every onboarding step after the palace door: the looping
 * Red Court video (v4 #3+8, hoisted in V2Onboarding) shows through behind the
 * sheet, a 35% status-bar scrim keeps the clock readable, and an ivory sheet
 * is anchored to the bottom edge with the header fixed, content scrolling only
 * when it must, and the primary button pinned above the navigation bar.
 */
@Composable
fun OnboardingScaffold(
    step: Int,
    artKey: Any,
    bubble: String,
    title: String,
    subtitle: String? = null,
    artHeight: ArtHeight = ArtHeight.Standard,
    widePadding: Boolean = false,
    dir: Int = 1,
    primaryText: String,
    primaryEnabled: Boolean,
    primaryDisabledText: String? = null,
    onPrimary: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val statusBarPx = WindowInsets.statusBars.getTop(density)
    val topReserve = statusBarPx + with(density) { artHeight.height.toPx() }
    val maxSheetPx = with(density) {
        (androidx.compose.ui.platform.LocalContext.current.resources.displayMetrics.heightPixels - topReserve)
    }
    val maxSheet = with(density) { maxSheetPx.toDp() }
    val reduced = rememberReducedMotion()
    val pose = if (artKey is String) artKey else "happy"
    val scroll = rememberScrollState()
    val canScrollForward by remember { derivedStateOf { scroll.canScrollForward } }
    val sheetTop = LocalV2SheetTop.current

    Box(Modifier.fillMaxSize()) {
        // 1. Status-bar scrim: keeps the clock readable over the video.
        Box(
            Modifier
                .fillMaxWidth()
                .height(with(density) { (statusBarPx + 24.dp.toPx()).toDp() })
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.35f),
                            1f to Color.Transparent,
                        )
                    )
                }
        )
        // 2. The sheet.
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .heightIn(max = maxSheet)
                    .animateContentSize(spring(dampingRatio = 0.85f, stiffness = 300f))
                    .clip(RoundedCornerShape(topStart = MorShape.Sheet, topEnd = MorShape.Sheet))
                    .background(MorColor.Ivory)
                    .drawBehind {
                        drawLine(
                            MorColor.Gold, Offset(0f, 0.6.dp.toPx()),
                            Offset(size.width, 0.6.dp.toPx()), 1.2.dp.toPx(),
                        )
                    }
                    .onGloballyPositioned { coords ->
                        // v4 #3+8: aim the background video at the band above the card.
                        sheetTop.value = with(density) { coords.positionInRoot().y.toDp() }
                    }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (widePadding) 18.dp else 16.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(top = 20.dp, bottom = 16.dp)
                        .imePadding(),
                ) {
                    AnimatedContent(
                        modifier = Modifier.weight(1f, fill = false),
                        targetState = Triple(step, title, bubble),
                        transitionSpec = {
                            if (reduced) {
                                fadeIn(tween(1)) togetherWith fadeOut(tween(1))
                            } else {
                                (fadeIn(tween(300, easing = FastOutSlowInEasing)) +
                                    slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { it / 14 * dir }) togetherWith
                                    (fadeOut(tween(130, easing = FastOutSlowInEasing)) +
                                        slideOutHorizontally(tween(130, easing = FastOutSlowInEasing)) { it / 14 * dir })
                            }
                        },
                        label = "ob-body",
                    ) { (s, t, b) ->
                        Column {
                            ObStepHeader(s)
                            Spacer(Modifier.height(14.dp))
                            ObBubble(b, pose)
                            Spacer(Modifier.height(14.dp))
                            Text(
                                t, fontFamily = Cormorant, fontSize = 30.sp,
                                fontWeight = FontWeight.SemiBold, lineHeight = 33.sp,
                                color = MorColor.Ink,
                            )
                            if (subtitle != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    subtitle, fontFamily = MorInter, fontSize = 13.5.sp,
                                    color = MorColor.Muted,
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            Column(
                                Modifier
                                    .weight(1f, fill = false)
                                    .verticalScroll(scroll)
                                    .padding(top = 4.dp, bottom = 4.dp),
                            ) { content() }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    if (canScrollForward) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .drawBehind {
                                    drawRect(
                                        Brush.verticalGradient(
                                            0f to Color.Transparent, 1f to MorColor.Ivory,
                                        )
                                    )
                                }
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    ObPrimaryButton(primaryText, primaryEnabled, primaryDisabledText, onPrimary)
                }
            }
        }
    }
}
