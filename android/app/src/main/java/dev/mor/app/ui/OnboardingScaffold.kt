package dev.mor.app.ui

// Mor · Next version onboarding scaffold (pack 01/02):
// a fixed Red Court fan header, a cream stage, and one step card that peels
// downward to reveal the next card already sitting statically beneath it.

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.mor.app.R

enum class ArtHeight(val height: androidx.compose.ui.unit.Dp) {
    Standard(140.dp), Compact(96.dp), None(0.dp)
}

/** "STEP n OF m" + one 3dp segment per step, 4dp gaps; done = gold. */
@Composable
private fun StepBadge(step: Int, total: Int) {
    Text(
        "STEP $step OF $total", fontFamily = MorInter, fontSize = 11.sp,
        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
        color = MorColor.GoldText,
    )
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (i < step) MorColor.Gold else MorColor.Sand)
            )
        }
    }
}

/** Small assistant tip under the progress bar. */
@Composable
private fun TipBubble(text: String) {
    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MorColor.Card)
            .border(1.dp, MorColor.Sand, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text, fontFamily = MorInter, fontSize = 11.5.sp,
            color = MorColor.Ink, textAlign = TextAlign.Start,
        )
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
            .height(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) MorColor.Maroon else MorColor.CtaDisabledBg)
            .then(if (enabled) Modifier.clickableNoIndication(onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontFamily = MorInter, fontSize = 16.sp,
            fontWeight = FontWeight.Bold, maxLines = 1,
            color = if (enabled) Color(0xFFFFFDF6) else MorColor.Muted,
        )
    }
}

private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier =
    this.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)

/**
 * One onboarding step after the palace door: fixed fan header on top, cream
 * stage below, scrollable content, pinned primary button.
 */
@Composable
fun OnboardingScaffold(
    step: Int,
    total: Int = 6,
    artKey: Any = "",
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
    val scroll = rememberScrollState()
    val sheetTop = LocalV2SheetTop.current

    // Issue 1: the card is content-height and sits on the bottom of the screen;
    // the palace art (the looping gate video behind the step) shows above it.
    // The opaque full-screen cream stage is gone, as is the separate fan header:
    // the video's own palace/peacock is the art. The card reports its top edge so
    // NeeluVideoBackground keeps the peacock in the band above the card.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val maxCard = maxHeight - 16.dp
        val hPad = if (widePadding) 18.dp else 14.dp
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = maxCard)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MorColor.ScreenCream)
                .onGloballyPositioned { coords ->
                    sheetTop.value = with(density) { coords.positionInRoot().y.toDp() }
                }
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = hPad)
            ) {
                Spacer(Modifier.height(14.dp))
                StepBadge(step, total)
                Spacer(Modifier.height(10.dp))
                TipBubble(bubble)
                Spacer(Modifier.height(12.dp))
                Text(
                    title, fontFamily = Cormorant, fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold, lineHeight = 27.sp,
                    color = MorColor.Ink,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        subtitle, fontFamily = MorInter, fontSize = 12.5.sp,
                        color = MorColor.Muted,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            Column(
                Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scroll)
                    .padding(horizontal = hPad)
                    .padding(bottom = 6.dp),
            ) { content() }
            Column(Modifier.padding(horizontal = hPad)) {
                ObPrimaryButton(primaryText, primaryEnabled, primaryDisabledText, onPrimary)
                Spacer(Modifier.height(8.dp))
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }
}
