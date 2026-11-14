package dev.mor.app.ui

// Mor · S1 "Sliding pill" segmented control (v4 #4 + #6). One component used
// for Female/Male (About you) and Gradual/Steady/Faster (Target weight).

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class Segment(
    val key: String,
    val label: String,
    val sublabel: String? = null,          // e.g. "0.36 kg/wk"
    @DrawableRes val icon: Int? = null,    // static vector, e.g. R.drawable.ic_opt_female
    @DrawableRes val iconAnim: Int? = null, // AVD that draws the icon in, e.g. R.drawable.avd_opt_female
)

private val PillTrack = Color(0xFFEFE6D6)
private val PillBg = Color(0xFF7A1D16)
private val PillHairline = Color(0x8CE2B65A)   // gold at 55%
private val OnPill = Color(0xFFFFF6E6)
private val OffPill = Color(0xFF5A4D3E)

/**
 * A cream track with a maroon pill that springs under the selected segment,
 * with a slight overshoot. [selectedKey] may be null: then no pill shows until
 * the first tap, and the pill fades in at that segment (no slide).
 */
@Composable
fun PillSegmentedControl(
    segments: List<Segment>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
    height: Dp,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val reduce = rememberReducedMotion()
    var innerWidthPx by remember { mutableIntStateOf(0) } // track width minus its 4dp padding
    val segW = if (innerWidthPx == 0) 0f else innerWidthPx / segments.size.toFloat()
    val index = segments.indexOfFirst { it.key == selectedKey }

    // Pill position: a spring with a slight overshoot, settling in about 420 ms.
    val x = remember { Animatable(0f) }
    var hasShown by remember { mutableStateOf(index >= 0) }
    LaunchedEffect(index, segW) {
        if (index < 0 || segW == 0f) return@LaunchedEffect
        val target = index * segW
        if (!hasShown || reduce) { x.snapTo(target); hasShown = true }
        else x.animateTo(target, spring(dampingRatio = 0.62f, stiffness = 420f))
    }
    val pillAlpha by animateFloatAsState(if (index >= 0) 1f else 0f, tween(250), label = "pillAlpha")

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(PillTrack, RoundedCornerShape(16.dp))
            .padding(4.dp)
            .onSizeChanged { innerWidthPx = it.width }
            .selectableGroup()
            .semantics { this.contentDescription = contentDescription }
    ) {
        // The pill.
        if (segW > 0f) {
            Box(
                Modifier
                    .offset { IntOffset(x.value.roundToInt(), 0) }
                    .width(with(density) { segW.toDp() })
                    .fillMaxHeight()
                    .graphicsLayer { alpha = pillAlpha }
                    .shadow(
                        6.dp, RoundedCornerShape(12.dp),
                        spotColor = PillBg.copy(alpha = 0.28f),
                        ambientColor = PillBg.copy(alpha = 0.28f),
                    )
                    .background(PillBg, RoundedCornerShape(12.dp))
                    .border(1.dp, PillHairline, RoundedCornerShape(12.dp))
            )
        }
        // The segments.
        Row(Modifier.fillMaxSize()) {
            segments.forEach { seg ->
                val selected = seg.key == selectedKey
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val color by animateColorAsState(if (selected) OnPill else OffPill, tween(250), label = "segColor")
                val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(120), label = "press")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .selectable(
                            selected = selected,
                            interactionSource = interaction,
                            indication = null,
                            role = Role.RadioButton,
                            onClick = {
                                if (!selected) {
                                    // Compose 1.7 has no SegmentTick yet; TextHandleMove is the closest tick.
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelect(seg.key)
                                }
                            },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (seg.sublabel == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (seg.icon != null) {
                                // The icon draws in when selected (AVD), tinted to the text colour.
                                SelectAnimatedIconTinted(
                                    seg.icon, seg.iconAnim ?: seg.icon, selected, color, Modifier.size(20.dp),
                                )
                            }
                            Text(
                                seg.label, color = color, fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold, fontFamily = MorInter,
                            )
                        }
                    } else {
                        Text(
                            seg.label, color = color, fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold, fontFamily = MorInter,
                        )
                        Text(
                            seg.sublabel, color = color.copy(alpha = 0.75f), fontSize = 11.sp,
                            fontWeight = FontWeight.Medium, fontFamily = MorInter,
                        )
                    }
                }
            }
        }
    }
}

/** SelectAnimatedIcon from OptionCard.kt, tinted so the icon follows the text colour. */
@OptIn(androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi::class)
@Composable
fun SelectAnimatedIconTinted(
    @DrawableRes staticRes: Int, @DrawableRes avdRes: Int,
    selected: Boolean, tint: Color, modifier: Modifier,
) {
    val filter = ColorFilter.tint(tint)
    if (!selected || rememberReducedMotion()) {
        Image(painterResource(staticRes), null, modifier, colorFilter = filter)
    } else {
        val image = AnimatedImageVector.animatedVectorResource(avdRes)
        var atEnd by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { atEnd = true }
        Image(
            rememberAnimatedVectorPainter(image, atEnd), null, modifier, colorFilter = filter,
        )
    }
}
