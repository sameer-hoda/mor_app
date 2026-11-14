package dev.mor.app.ui

// Mor · Onboarding option card (Jharokha medallion direction) — reference
// implementation from the final-build package (04-option-icons), adapted to
// this package and the shared MorColor tokens.
// Requires: androidx.compose.animation:animation-graphics (AnimatedImageVector).

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mor.app.R

/** One option. `staticIcon` = ic_opt_<key>, `selectedIcon` = ic_opt_<key>_selected, `animIcon` = avd_opt_<key>. */
data class OptionSpec(
    val key: String,
    val title: String,
    val subtitle: String,
    @DrawableRes val staticIcon: Int,
    @DrawableRes val selectedIcon: Int,
    @DrawableRes val animIcon: Int,
)

/**
 * Plays an AnimatedVectorDrawable once, forward, each time [selected] turns true.
 * When not selected it shows the static drawable, so deselecting never plays the
 * animation backwards.
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun SelectAnimatedIcon(
    @DrawableRes staticRes: Int,
    @DrawableRes selectedRes: Int,
    @DrawableRes avdRes: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    val reduce = rememberReducedMotion()
    when {
        !selected -> Image(painterResource(staticRes), null, modifier)
        reduce -> Image(painterResource(selectedRes), null, modifier)
        else -> {
            val image = AnimatedImageVector.animatedVectorResource(avdRes)
            var atEnd by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { atEnd = true }
            Image(rememberAnimatedVectorPainter(image, atEnd), null, modifier)
        }
    }
}

@Composable
fun MedallionIcon(option: OptionSpec, selected: Boolean) {
    Box(Modifier.size(width = 44.dp, height = 50.dp), contentAlignment = Alignment.Center) {
        SelectAnimatedIcon(
            R.drawable.ic_medallion_arch, R.drawable.ic_medallion_arch_selected,
            R.drawable.avd_medallion_arch, selected, Modifier.matchParentSize(),
        )
        SelectAnimatedIcon(
            option.staticIcon, option.selectedIcon, option.animIcon, selected,
            Modifier.padding(top = 6.dp).size(24.dp),
        )
    }
}

/**
 * The option card. `dimmed` = another option in the same group is selected (opacity 0.62).
 * `tile` = the two-up Female / Male layout (icon above the title, 120dp tall).
 */
@Composable
fun OptionCard(
    option: OptionSpec,
    selected: Boolean,
    dimmed: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    tile: Boolean = false,
) {
    val haptics = LocalHapticFeedback.current
    val reduce = rememberReducedMotion()
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val springSpec = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)
    val border by animateColorAsState(if (selected) MorColor.Gold else MorColor.CardBorder, tween(300), label = "border")
    val lift by animateFloatAsState(if (selected) -2f else 0f, springSpec, label = "lift")
    val elevation by animateFloatAsState(if (selected) 12f else 0f, tween(300), label = "elev")
    val alpha by animateFloatAsState(if (dimmed) 0.62f else 1f, tween(300), label = "dim")
    val scale by animateFloatAsState(if (pressed) 0.98f else 1f, tween(150), label = "press")

    // Gold sheen: one sweep left → right, 900 ms, starting 150 ms after selection.
    val sheen = remember { Animatable(-0.3f) }
    LaunchedEffect(selected) {
        if (selected && !reduce) {
            sheen.snapTo(-0.3f)
            sheen.animateTo(1.3f, tween(900, delayMillis = 150, easing = FastOutSlowInEasing))
        } else sheen.snapTo(-0.3f)
    }

    val shape = RoundedCornerShape(if (tile) 18.dp else 16.dp)
    Box(
        modifier
            .graphicsLayer { translationY = lift.dp.toPx(); this.alpha = alpha; scaleX = scale; scaleY = scale }
            .shadow(elevation.dp, shape, ambientColor = MorColor.Maroon.copy(alpha = .16f), spotColor = MorColor.Maroon.copy(alpha = .16f))
            .background(MorColor.Card, shape)
            .border(BorderStroke(1.dp, border), shape)
            .selectable(
                selected = selected,
                interactionSource = interaction,
                indication = null,
                role = Role.RadioButton,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect()
                },
            )
            .drawWithContent {
                drawContent()
                val x = sheen.value * size.width
                if (sheen.value > -0.3f && sheen.value < 1.3f) {
                    val r = if (tile) 18.dp.toPx() else 16.dp.toPx()
                    clipPath(Path().apply {
                        addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(r, r)))
                    }) {
                        drawRect(
                            Brush.linearGradient(
                                listOf(Color(0x00FFF0C8), Color(0xA6FFECBE), Color(0x00FFF0C8)),
                                start = Offset(x - 60.dp.toPx(), 0f), end = Offset(x, size.height),
                            )
                        )
                    }
                }
            }
    ) {
        if (tile) {
            Column(
                Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            ) {
                MedallionIcon(option, selected)
                Text(option.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Ink, fontFamily = MorInter)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 66.dp).padding(start = 10.dp, end = 14.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                MedallionIcon(option, selected)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(option.title, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold, color = MorColor.Ink, fontFamily = MorInter)
                    Text(option.subtitle, fontSize = 12.5.sp, lineHeight = 16.sp, color = MorColor.Muted, fontFamily = MorInter)
                }
                Spacer(Modifier.size(2.dp))
                SelectAnimatedIcon(
                    R.drawable.ic_indicator_diamond, R.drawable.ic_indicator_diamond_selected,
                    R.drawable.avd_indicator_diamond, selected, Modifier.size(18.dp),
                )
            }
        }
    }
}

/** A radio group of cards. Use `tiles = true` for Female / Male. */
@Composable
fun OptionGroup(
    options: List<OptionSpec>,
    selectedKey: String?,
    tiles: Boolean = false,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    val content: @Composable (OptionSpec, Modifier) -> Unit = { o, m ->
        OptionCard(o, o.key == selectedKey, selectedKey != null && o.key != selectedKey, { onSelect(o.key) }, m, tiles)
    }
    if (tiles) {
        Row(modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { content(it, Modifier.weight(1f)) }
        }
    } else {
        Column(modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            options.forEach { content(it, Modifier.fillMaxWidth()) }
        }
    }
}
