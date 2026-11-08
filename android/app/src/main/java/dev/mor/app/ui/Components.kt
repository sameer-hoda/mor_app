package dev.mor.app.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mor.app.R

// ---------- Buttons ----------

@Composable
fun MorButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    gold: Boolean = false,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (gold) Gold else Teal,
            contentColor = if (gold) Ink else Color.White,
            disabledContainerColor = Disabled,
            disabledContentColor = Color(0xFF5A564D),
        ),
    ) {
        Text(text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun MorSecondaryButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Teal),
        border = androidx.compose.foundation.BorderStroke(2.dp, Teal),
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ---------- Text / layout ----------

@Composable
fun H1(text: String, fontSize: Int = 30, align: TextAlign = TextAlign.Start) {
    Text(text, fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
        textAlign = align, lineHeight = (fontSize * 1.12).sp)
}

@Composable
fun Muted(text: String, fontSize: Int = 16, align: TextAlign = TextAlign.Start) {
    Text(text, fontSize = fontSize.sp, color = Sub, textAlign = align,
        lineHeight = (fontSize * 1.45).sp)
}

@Composable
fun StepDots(step: Int, total: Int = 6) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            Box(
                Modifier
                    .size(if (i < step) 22.dp else 8.dp, 8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (i < step) Teal else Line2),
            )
        }
    }
}

@Composable
fun MorTopBar(
    title: String? = null,
    onBack: (() -> Unit)? = null,
    right: @Composable (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp, 14.dp, 16.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.White, CircleShape),
            ) { Icon(Icons.Filled.ArrowBack, "Back", tint = Ink) }
        } else Spacer(Modifier.width(44.dp))
        if (title != null) {
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Ink,
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        } else Spacer(Modifier.weight(1f))
        if (right != null) right() else Spacer(Modifier.width(44.dp))
    }
}

/** Onboarding step scaffold: top bar with dots, title block, content, bottom button. */
@Composable
fun StepScaffold(
    step: Int,
    title: String,
    sub: String,
    onBack: (() -> Unit)?,
    actionLabel: String = "Continue",
    actionEnabled: Boolean = true,
    onAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(Modifier.background(Chandan)) {
        FeatherBackdrop()
        Column(Modifier.imePadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp, 14.dp, 16.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White, CircleShape),
                ) { Icon(Icons.Filled.ArrowBack, "Back", tint = Ink) }
            } else Spacer(Modifier.width(44.dp))
            Spacer(Modifier.weight(1f))
            StepDots(step)
        }
        Column(Modifier.padding(20.dp, 26.dp, 20.dp, 8.dp)) {
            Text(sub, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Sub)
            Spacer(Modifier.height(4.dp))
            H1(title)
        }
        Box(Modifier.weight(1f).padding(horizontal = 20.dp)) { content() }
        Column(Modifier.navigationBarsPadding().padding(20.dp, 10.dp, 20.dp, 26.dp)) {
            MorButton(actionLabel, enabled = actionEnabled, onClick = onAction)
        }
        }
    }
}

// ---------- Options / segmented / toggle ----------

@Composable
fun OptRow(
    title: String,
    sub: String? = null,
    selected: Boolean,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    big: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Mint else Color.White)
            .border(
                if (big) 2.dp else 1.5.dp,
                if (selected) Teal else Color.Transparent,
                RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(if (big) 18.dp else 16.dp, if (big) 18.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(if (big) 28.dp else 22.dp)
                .border(2.dp, if (selected) Teal else Color(0xFFCFC8B8), CircleShape)
                .background(if (selected) Teal else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = Color.White,
                modifier = Modifier.size(if (big) 18.dp else 14.dp))
        }
        Spacer(Modifier.width(12.dp))
        if (leading != null) { leading(); Spacer(Modifier.width(8.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = if (big) 19.sp else 16.sp, fontWeight = FontWeight.SemiBold, color = Ink)
            if (sub != null) Text(sub, fontSize = 13.sp, color = Sub)
        }
        if (trailing != null) trailing()
    }
}

@Composable
fun SliderCard(
    label: String,
    valueText: String,
    sliderValue: Float,
    range: ClosedFloatingPointRange<Float>,
    onValue: (Float) -> Unit,
    labelAction: @Composable (() -> Unit)? = null,
    bottom: @Composable (() -> Unit)? = null,
) {
    // Item 9: the single slider design for age, height, current weight and
    // target weight — one card, one value style, one slider treatment.
    MorCard {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Sub)
                if (labelAction != null) {
                    Spacer(Modifier.width(6.dp))
                    labelAction()
                }
            }
            Text(valueText, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
            androidx.compose.material3.Slider(
                value = sliderValue,
                onValueChange = onValue,
                valueRange = range,
                colors = androidx.compose.material3.SliderDefaults.colors(
                    thumbColor = Teal, activeTrackColor = Teal),
            )
            if (bottom != null) bottom()
        }
    }
}

@Composable
fun MorToast(msg: String, err: Boolean, modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    // Item 13: a designed feedback toast — dark rounded card with a status
    // dot, replacing the plain system snackbar (e.g. "Water logged · 3/8").
    Row(
        modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (err) Kumkum else Ink)
            .clickable(onClick = onDismiss)
            .padding(12.dp, 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(28.dp).clip(CircleShape)
                .background(Color.White.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (err) "!" else "✓", fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        Spacer(Modifier.width(10.dp))
        Text(msg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = Color.White, modifier = Modifier.weight(1f))
    }
}

@Composable
fun Segmented(options: List<String>, value: String, tall: Boolean = false, onChange: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEFE9DC))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { o ->
            val on = o == value
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (on) Color.White else Color.Transparent)
                    .clickable { onChange(o) }
                    .padding(vertical = if (tall) 12.dp else 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(o, fontSize = if (tall) 15.sp else 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (on) Teal else Sub, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun MorToggle(on: Boolean, onChange: (Boolean) -> Unit) {
    // Custom switch with unambiguous on/off states (item 22).
    Box(
        Modifier
            .size(56.dp, 34.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(if (on) Teal else Color(0xFFD8D1C0))
            .border(1.5.dp, if (on) TealDark else Color(0xFFBFB7A4), RoundedCornerShape(17.dp))
            .clickable { onChange(!on) }
            .padding(4.dp),
        contentAlignment = if (on) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (on) "✓" else "–",
                fontSize = 14.sp, fontWeight = FontWeight.ExtraBold,
                color = if (on) Teal else Color(0xFFA39B88),
            )
        }
    }
}

@Composable
fun MorField(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String = "",
    isError: Boolean = false,
    numeric: Boolean = false,
) {
    TextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = Sub) },
        isError = isError,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            errorContainerColor = Color(0xFFFFF6F5),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

// ---------- Cards / rows / pills ----------

@Composable
fun MorCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color.White)
            .border(1.dp, Line, RoundedCornerShape(22.dp))
            .padding(14.dp),
    ) { content() }
}

@Composable
fun Pill(text: String, bg: Color, fg: Color) {
    Text(
        text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(10.dp, 3.dp),
    )
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (on) Teal else Color.White)
            .border(1.5.dp, if (on) Teal else Line2, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(14.dp, 10.dp),
    ) {
        Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = if (on) Color.White else Ink)
    }
}

// ---------- Progress ring / macro bars ----------

@Composable
fun ProgressRing(progress: Float, size: Dp = 100.dp, color: Color = Teal, content: @Composable () -> Unit) {
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val stroke = 10.dp.toPx()
            drawArc(Color(0xFFEFE9DC), -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        content()
    }
}

/** Plan-target allocations (not intake): gram targets with kcal-share bars. */
@Composable
fun PlanMacroBars(proteinG: Double, carbsG: Double, fatG: Double) {
    val pKcal = proteinG * 4
    val cKcal = carbsG * 4
    val fKcal = fatG * 9
    val total = (pKcal + cKcal + fKcal).coerceAtLeast(1.0)
    @Composable
    fun Row2(label: String, g: Double, kcal: Double, c: Color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Ink,
                modifier = Modifier.width(64.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFEFE9DC)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((kcal / total).coerceIn(0.02, 1.0).toFloat())
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(c),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("${g.toInt()}g · ${(kcal / total * 100).toInt()}%",
                fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row2("Protein", proteinG, pKcal, Neel)
        Row2("Carbs", carbsG, cKcal, Genda)
        Row2("Fat", fatG, fKcal, Pankh)
    }
}

@Composable
fun MacroBars(eatenP: Double, eatenC: Double, eatenF: Double, goalP: Double, goalC: Double, goalF: Double) {
    @Composable
    fun Row2(label: String, e: Double, g: Double, c: Color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, fontSize = 13.sp, color = Ink, modifier = Modifier.width(52.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEFE9DC)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((e / g.coerceAtLeast(1.0)).coerceIn(0.0, 1.0).toFloat())
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(c),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("${e.toInt()}/${g.toInt()}g", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row2("Carbs", eatenC, goalC, Genda)
        Row2("Protein", eatenP, goalP, Neel)
        Row2("Fat", eatenF, goalF, Pankh)
    }
}

// ---------- Bottom nav ----------

data class NavTab(val key: String, val label: String, val icon: ImageVector?, val route: String)

@Composable
fun MorBottomNav(active: String, onNav: (String) -> Unit) {
    NavigationBar(containerColor = Color.White, modifier = Modifier.navigationBarsPadding()) {
        val tabs = listOf(
            NavTab("home", "Home", Icons.Filled.Home, "home"),
            NavTab("insights", "Insights", Icons.Filled.ShowChart, "insights"),
            NavTab("log", "Log", null, "add"),
            // Item 14: tab matches the screen — Challenges with a fire icon.
            NavTab("challenges", "Challenges", Icons.Filled.Whatshot, "challenges"),
            NavTab("profile", "Profile", Icons.Filled.Person, "profile"),
        )
        tabs.forEach { t ->
            if (t.key == "log") {
                NavigationBarItem(
                    selected = false,
                    onClick = { onNav("add") },
                    icon = {
                        // Obvious camera action in brand treatment (item 31).
                        Box(
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Teal)
                                .border(3.dp, Gold, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Filled.PhotoCamera, "Log a meal",
                                    tint = Teal, modifier = Modifier.size(26.dp))
                            }
                        }
                    },
                    label = { Text("") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent),
                )
            } else {
                NavigationBarItem(
                    selected = active == t.key,
                    onClick = { onNav(t.route) },
                    icon = { Icon(t.icon ?: Icons.Filled.Home, t.label) },
                    label = { Text(t.label, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Teal,
                        selectedTextColor = Teal,
                        indicatorColor = Mint2,
                        unselectedIconColor = Sub,
                        unselectedTextColor = Sub,
                    ),
                )
            }
        }
    }
}

// ---------- Neelu mascot ----------

private val moodRes = mapOf(
    "happy" to R.drawable.neelu_happy,
    "hungry" to R.drawable.neelu_hungry,
    "sleepy" to R.drawable.neelu_sleepy,
    "thinking" to R.drawable.neelu_thinking,
    "joy" to R.drawable.neelu_joy,
    "cool" to R.drawable.neelu_cool,
    "love" to R.drawable.neelu_love,
)

/** Web anim name -> bundled mood art. */
fun moodFor(anim: String): Int = moodRes[
    mapOf(
        "splash" to "cool", "flex" to "cool", "wave" to "happy", "peek" to "happy",
        "hop" to "love", "dance" to "joy", "think" to "thinking", "shrug" to "thinking",
        "hungry" to "hungry", "droop" to "hungry", "sleep" to "sleepy",
    )[anim] ?: "happy",
] ?: R.drawable.neelu_happy

@Composable
fun Neelu(
    anim: String = "wave",
    size: Dp = 160.dp,
    modifier: Modifier = Modifier,
    enter: Boolean = false,
    idle: Boolean = false,
) {
    // Entrance: gentle fade+rise once (no spring jump, item A5). Idle: a light
    // endless bob for the welcome moments (item A2). Otherwise a still image.
    var shown by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(!enter) }
    androidx.compose.runtime.LaunchedEffect(Unit) { shown = true }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        if (shown) 1f else 0f,
        androidx.compose.animation.core.tween(350),
        label = "neelu-fade",
    )
    val rise by androidx.compose.animation.core.animateFloatAsState(
        if (shown) 0f else 26f,
        androidx.compose.animation.core.tween(350),
        label = "neelu-rise",
    )
    val bobAnim = remember(anim) { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(idle, anim) {
        if (!idle) {
            bobAnim.snapTo(0f)
            return@LaunchedEffect
        }
        bobAnim.animateTo(-11f, androidx.compose.animation.core.tween(1300))
        while (true) {
            bobAnim.animateTo(0f, androidx.compose.animation.core.tween(1300))
            bobAnim.animateTo(-11f, androidx.compose.animation.core.tween(1300))
        }
    }
    val bob = bobAnim.value
    androidx.compose.foundation.Image(
        painter = painterResource(moodFor(anim)),
        contentDescription = "Neelu",
        modifier = modifier
            .size(size)
            .graphicsLayer {
                this.alpha = alpha
                translationY = rise + bob
            },
        contentScale = ContentScale.Fit,
    )
}

/**
 * Crisp repeating feather wallpaper drawn tiled at native resolution
 * (WhatsApp-wallpaper style understatement, items 15 / A6).
 */
@Composable
fun FeatherBackdrop(
    modifier: Modifier = Modifier.fillMaxSize(),
    alpha: Float = 0.32f,
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val img = remember {
        (android.graphics.BitmapFactory.decodeResource(ctx.resources, R.drawable.feather_tile)
            ?: error("missing feather_tile"))
            .asImageBitmap()
    }
    Canvas(
        modifier = modifier,
        onDraw = {
            drawRect(
                brush = androidx.compose.ui.graphics.ShaderBrush(
                    androidx.compose.ui.graphics.ImageShader(
                        img,
                        androidx.compose.ui.graphics.TileMode.Repeated,
                        androidx.compose.ui.graphics.TileMode.Repeated,
                    ),
                ),
                alpha = alpha,
            )
        },
    )
}

// ---------- Food emoji (stand-in for FoodIcon SVG set) ----------

fun emojiForFood(name: String): String {
    val n = (name).lowercase()
    val map = listOf(
        Regex("dal|rajma|chole|sambar|curry") to "🍲",
        Regex("roti|phulka|chapati|naan") to "🫓",
        Regex("rice|biryani|pulao|khichdi") to "🍚",
        Regex("dosa") to "🥞",
        Regex("idli") to "🍡",
        Regex("poha|upma") to "🍛",
        Regex("chai|coffee|tea|lassi") to "🍵",
        Regex("paratha") to "🫓",
        Regex("paneer") to "🧀",
        Regex("samosa") to "🥟",
        Regex("raita|curd|yogurt|dahi") to "🥣",
        Regex("salad|fruit|chaat") to "🥗",
        Regex("sprouts") to "🌱",
        Regex("egg") to "🥚",
        Regex("chicken|mutton|fish|prawn") to "🍗",
        Regex("thali|plate|meal") to "🍽️",
    )
    for ((re, e) in map) if (re.containsMatchIn(n)) return e
    return "🍽️"
}

fun vegBadge(veg: String?): String = when (veg) {
    "non" -> "🔴"
    "egg" -> "🟡"
    else -> "🟢"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MorSheet(onClose: () -> Unit, content: @Composable () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, containerColor = Chandan) {
        Column(Modifier.navigationBarsPadding().padding(16.dp, 0.dp, 16.dp, 26.dp)) { content() }
    }
}
