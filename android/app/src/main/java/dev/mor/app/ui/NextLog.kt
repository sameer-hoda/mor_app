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
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import dev.mor.app.data.AppVm
import dev.mor.app.data.FoodItem
import dev.mor.app.data.friendlyErr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Next-version logging (pack items 12-15).
 *
 * Item 12: the static food catalog is gone. Photo, voice and text all resolve
 * through POST /api/foods/ai-search (Gemini + Google Search), and every figure
 * is labeled as an estimate.
 * Item 13: the pill recorder never auto-stops - recording ends only on Stop.
 * Item 14: one plain field plus an explicit gold Search button; nothing lists
 * while typing.
 * Item 15: the same rising-bubble chooser appears for every capture path, and
 * nothing is written until a candidate is tapped.
 */

private val NlInk = MorColor.Ink
private val NlInk2 = MorColor.Ink2
private val NlMuted = MorColor.Muted
private val NlGold = MorColor.Carbs

/**
 * Issue 20: merge SpeechRecognizer output without repeating words.
 * Some vendors return cumulative partials (each one starts with the previous),
 * others incremental ones. `latest` is the newest partial/final string.
 * Returns the new (settled, partial) pair.
 */
internal fun mergeVoiceText(
    settled: String,
    partial: String,
    latest: String,
    isFinal: Boolean,
): Pair<String, String> {
    val text = latest.trim()
    if (text.isEmpty()) return settled to partial
    if (isFinal) {
        val nextSettled = if (settled.contains(text)) settled else
            listOf(settled, text).filter { it.isNotBlank() }.joinToString(" ")
        return nextSettled.trim() to ""
    }
    val nextPartial = if (partial.isNotBlank() && text.startsWith(partial)) text
    else listOf(partial, text).filter { it.isNotBlank() }.joinToString(" ")
    return settled to nextPartial.trim()
}

@Composable
fun NextLogScreen(
    nav: NavController,
    vm: AppVm,
    slot: String,
    mode0: String = "text",
    q0: String = "",
    auto: Boolean = false,
) {
    MorSystemBars(dark = false)
    var mode by remember { mutableStateOf(if (mode0 == "voice" || mode0 == "scan") mode0 else "text") }
    var q by remember { mutableStateOf(q0) }
    var sent by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var cands by remember { mutableStateOf<List<FoodItem>?>(null) }
    var logging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun runSearch(query: String) {
        val text = query.trim()
        if (text.isEmpty() || loading) return
        sent = text
        loading = true
        error = null
        cands = null
        scope.launch(Dispatchers.IO) {
            try {
                val r = vm.api().aiSearch(mapOf("query" to text))
                withContext(Dispatchers.Main) {
                    // Issue 23: drop only rows that are identical in name, kcal and
                    // portion; different portions are real variants and stay.
                    val items = r.items.distinctBy {
                        "${it.name.lowercase()}|${it.kcal.roundToInt()}|${it.portion.orEmpty().lowercase()}"
                    }
                    if (items.isEmpty()) {
                        error = "No match for “$text”. Try describing the portion too."
                    } else {
                        cands = items
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "The AI search didn't answer (${friendlyErr(e)}). Try again."
                }
            } finally {
                withContext(Dispatchers.Main) { loading = false }
            }
        }
    }

    fun logPick(c: FoodItem) {
        if (logging) return
        logging = true
        scope.launch(Dispatchers.IO) {
            try {
                val r = vm.api().logMeal(mapOf(
                    "slot" to slot,
                    "source" to "ai",
                    "name" to c.name,
                    "items" to listOf(mapOf(
                        "name" to c.name,
                        "kcal" to c.kcal.roundToInt(),
                        "protein_g" to c.protein_g.roundToInt(),
                        "carbs_g" to c.carbs_g.roundToInt(),
                        "fat_g" to c.fat_g.roundToInt(),
                        "portion" to (c.portion ?: "1 serving"),
                    )),
                ))
                vm.lastLog = r
                vm.refreshHome()
                withContext(Dispatchers.Main) {
                    nav.navigate("logged") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    error = "Log failed: ${friendlyErr(e)}"
                    logging = false
                }
            }
        }
    }

    LaunchedEffect(auto, q0) {
        if (auto && q0.isNotBlank()) {
            mode = "text"
            runSearch(q0)
        }
    }

    Box(Modifier.fillMaxSize().background(MorColor.ScreenCream).imePadding()) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp, 12.dp, 16.dp, 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color.White)
                        .border(1.dp, MorColor.Sand, CircleShape)
                        .clickable { if (!nav.popBackStack()) nav.navigate("add?slot=$slot") },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", fontSize = 15.sp, color = NlInk) }
                Spacer(Modifier.width(12.dp))
                Text(
                    "Log a meal", fontSize = 24.sp, color = NlInk,
                    fontFamily = Cormorant, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    slot.replaceFirstChar { it.uppercase() }, fontSize = 12.sp,
                    color = MorColor.GoldText, fontWeight = FontWeight.SemiBold,
                    fontFamily = MorInter,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF1E4D3))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }

            ModeRow(mode) { mode = it }

            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp),
            ) {
                when (mode) {
                    "voice" -> VoiceSection { transcript -> q = transcript; runSearch(transcript) }
                    "scan" -> ScanSection { ScanState.next = true; nav.navigate("camera?slot=$slot") }
                    else -> TextSection(
                        q = q,
                        onQ = { q = it },
                        onSearch = { runSearch(q) },
                        loading = loading,
                    )
                }

                when {
                    loading -> SearchingRow(sent)
                    error != null -> {
                        Spacer(Modifier.height(18.dp))
                        Text(
                            error!!, fontSize = 12.5.sp, lineHeight = 18.sp,
                            color = MorColor.Error, fontFamily = MorInter,
                        )
                        Spacer(Modifier.height(10.dp))
                        GoldButton("Try again", enabled = true) {
                            if (sent.isNotBlank()) runSearch(sent) else runSearch(q)
                        }
                    }
                    cands != null -> Column {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "AI + web results for “$sent” — estimates:",
                            fontSize = 11.sp, color = NlMuted, fontFamily = MorInter,
                        )
                        // Issue 21: no silent promise that several foods are detected.
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Log one food at a time for the best match.",
                            fontSize = 10.5.sp, color = MorColor.NoteFaint, fontFamily = MorInter,
                        )
                        Spacer(Modifier.height(10.dp))
                        cands!!.forEachIndexed { i, c ->
                            RisingBubble(c, i, enabled = !logging) { logPick(c) }
                            Spacer(Modifier.height(9.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap a match to confirm — nothing logs before you pick.",
                            fontSize = 10.5.sp, color = MorColor.NoteFaint, fontFamily = MorInter,
                        )
                    }
                }
                Spacer(Modifier.navigationBarsPadding().height(30.dp))
            }
        }
        if (logging) {
            Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = MorColor.Cream)
                    Spacer(Modifier.height(10.dp))
                    Text("Logging…", fontSize = 13.sp, color = MorColor.Cream, fontFamily = MorInter)
                }
            }
        }
    }
}

@Composable
private fun ModeRow(mode: String, onMode: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("scan" to "Photo", "voice" to "Voice", "text" to "Type").forEach { (key, label) ->
            val sel = key == mode
            Box(
                Modifier.weight(1f).height(38.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (sel) MorColor.Maroon else Color.White)
                    .border(1.dp, if (sel) MorColor.Maroon else MorColor.Sand, RoundedCornerShape(12.dp))
                    .clickable { onMode(key) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label, fontSize = 13.sp, fontFamily = MorInter,
                    fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (sel) MorColor.CreamText else NlInk,
                )
            }
        }
    }
}

@Composable
private fun TextSection(q: String, onQ: (String) -> Unit, onSearch: () -> Unit, loading: Boolean) {
    Spacer(Modifier.height(10.dp))
    Text("Type your meal", fontSize = 20.sp, color = NlInk, fontFamily = Cormorant,
        fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp))
                .background(Color.White).border(1.5.dp, MorColor.Sand, RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            BasicTextField(
                value = q,
                onValueChange = onQ,
                singleLine = true,
                textStyle = TextStyle(fontSize = 15.sp, color = NlInk, fontFamily = MorInter),
                cursorBrush = SolidColor(MorColor.Maroon),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (q.isEmpty()) {
                        Text(
                            "e.g. dhokla, 2 pieces", fontSize = 15.sp,
                            color = MorColor.NoteFaint, fontFamily = MorInter,
                        )
                    }
                    inner()
                },
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.height(52.dp).widthIn(min = 84.dp).clip(RoundedCornerShape(14.dp))
                .background(NlGold)
                .clickable(enabled = q.isNotBlank() && !loading) { onSearch() }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Search", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = Color(0xFF211B0C), fontFamily = MorInter,
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        "Nothing lists while you type — Search runs the AI + web lookup.",
        fontSize = 10.5.sp, lineHeight = 15.sp, color = MorColor.NoteFaint, fontFamily = MorInter,
    )
}

@Composable
private fun SearchingRow(query: String) {
    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp), color = NlGold, strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                "Searching AI and the web…", fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold, color = NlInk, fontFamily = MorInter,
            )
            if (query.isNotBlank()) Text(
                "“$query”", fontSize = 11.sp, color = NlMuted, fontFamily = MorInter,
            )
        }
    }
}

/** Item 15: one bubble, rising 14dp + fade, 0.45s, 0.22s stagger. */
@Composable
private fun RisingBubble(c: FoodItem, index: Int, enabled: Boolean, onPick: () -> Unit) {
    var shown by remember(c.name) { mutableStateOf(false) }
    LaunchedEffect(c.name) {
        delay(index * 220L)
        shown = true
    }
    val rise by animateFloatAsState(if (shown) 0f else 14f, tween(450), label = "bubbleRise")
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(450), label = "bubbleFade")
    Box(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { translationY = rise; this.alpha = alpha }
            .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
            .background(Color.White)
            .border(1.5.dp, MorColor.Sand, RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
            .clickable(enabled = enabled, onClick = onPick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column {
            Text(c.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NlInk,
                fontFamily = MorInter)
            Spacer(Modifier.height(2.dp))
            // Issue 23: show the portion so same-name variants are distinguishable.
            Text(
                "~${c.kcal.roundToInt()} kcal est. · ${c.portion?.takeIf { it.isNotBlank() } ?: "1 serving"} · ${c.match ?: "Possible"}",
                fontSize = 11.sp, lineHeight = 15.sp, color = NlInk2, fontFamily = MorInter,
            )
        }
    }
}

@Composable
private fun GoldButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.height(46.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (enabled) NlGold else MorColor.CtaDisabledBg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            color = Color(0xFF211B0C), fontFamily = MorInter,
        )
    }
}

// ---------- Voice (item 13) ----------

@Composable
private fun VoiceSection(onStop: (String) -> Unit) {
    val ctx = LocalContext.current
    var recording by remember { mutableStateOf(false) }
    var seconds by remember { mutableIntStateOf(0) }
    var settled by remember { mutableStateOf("") }
    var partial by remember { mutableStateOf("") }
    var hint by remember { mutableStateOf<String?>(null) }
    val transcript = listOf(settled, partial).filter { it.isNotBlank() }
        .joinToString(" ").trim()
    val supported = remember { SpeechRecognizer.isRecognitionAvailable(ctx) }
    val scope = rememberCoroutineScope()

    val recHolder = remember { arrayOfNulls<SpeechRecognizer>(1) }
    fun restart() {
        if (!recording) return
        scope.launch {
            delay(160)
            if (!recording) return@launch
            startRecognition(recHolder[0], ctx)
        }
    }
    val rec = remember {
        if (!supported) null else try {
            SpeechRecognizer.createSpeechRecognizer(ctx).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(p: Bundle?) { hint = null }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(r: Float) {}
                    override fun onBufferReceived(b: ByteArray?) {}
                    override fun onEvent(t: Int, p: Bundle?) {}
                    override fun onPartialResults(p: Bundle?) {
                        val part = p
                            ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.joinToString(" ")?.trim()
                        if (!part.isNullOrBlank()) {
                            val r = mergeVoiceText(settled, partial, part, false)
                            settled = r.first
                            partial = r.second
                        }
                    }
                    override fun onResults(r: Bundle?) {
                        val got = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            ?.joinToString(" ")?.trim()
                        if (!got.isNullOrBlank()) {
                            val m = mergeVoiceText(settled, partial, got, true)
                            settled = m.first
                            partial = m.second
                        }
                        // Item 13: never auto-stop. Silence ends an utterance,
                        // not the recording — keep the session alive until Stop.
                        if (recording) restart()
                    }
                    override fun onError(e: Int) {
                        if (recording) restart()
                        else hint = "Didn't catch that. Tap to speak again."
                    }
                    override fun onEndOfSpeech() {
                        if (recording) restart()
                    }
                })
            }
        } catch (_: Exception) { null }
    }

    recHolder[0] = rec

    DisposableEffect(Unit) { onDispose { rec?.destroy() } }

    LaunchedEffect(recording) {
        if (recording) {
            seconds = 0
            while (recording) {
                delay(1000)
                if (recording) seconds++
            }
        }
    }

    val audioPerm = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            recording = true
            settled = ""
            partial = ""
            hint = null
            startRecognition(rec, ctx)
        } else {
            hint = "Mic is blocked. Allow the microphone in Settings, then try again."
        }
    }

    Spacer(Modifier.height(10.dp))
    Text("Log by voice", fontSize = 20.sp, color = NlInk, fontFamily = Cormorant,
        fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(22.dp))

    if (!recording) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.clip(RoundedCornerShape(999.dp)).background(MorColor.Maroon)
                    .clickable {
                        if (!supported) {
                            hint = "Voice typing isn't on this device. Type your meal instead."
                        } else if (ContextCompat.checkSelfPermission(
                                ctx, Manifest.permission.RECORD_AUDIO,
                            ) == PackageManager.PERMISSION_GRANTED
                        ) {
                            settled = ""
                            partial = ""
                            recording = true
                            hint = null
                            startRecognition(rec, ctx)
                        } else {
                            audioPerm.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    .padding(horizontal = 28.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MicMark(20.dp, MorColor.Cream)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Tap to speak", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    color = MorColor.Cream, fontFamily = MorInter,
                )
            }
        }
    } else {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp))
                .background(MorColor.Maroon).padding(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Waveform(MorColor.Cream)
                Spacer(Modifier.width(12.dp))
                Text(
                    "%d:%02d".format(seconds / 60, seconds % 60),
                    fontSize = 14.sp, color = MorColor.Cream, fontFamily = MorInter,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(RoundedCornerShape(999.dp))
                        .border(1.2.dp, MorColor.Cream.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
                        .clickable {
                            recording = false
                            rec?.stopListening()
                            val text = transcript.trim()
                            if (text.isBlank()) {
                                hint = "Nothing was heard. Tap to speak again, or type it."
                            } else {
                                onStop(text)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "Stop", fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        color = MorColor.Cream, fontFamily = MorInter,
                    )
                }
            }
            if (transcript.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    transcript, fontSize = 12.5.sp, lineHeight = 17.sp,
                    color = MorColor.Cream.copy(alpha = 0.9f), fontFamily = MorInter,
                )
            }
        }
    }
    hint?.let {
        Spacer(Modifier.height(10.dp))
        Text(it, fontSize = 12.sp, color = MorColor.Error, fontFamily = MorInter)
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Listening until Stop is tapped — no pause detection, no cutoff mid-sentence.",
        fontSize = 10.5.sp, lineHeight = 15.sp, color = MorColor.NoteFaint, fontFamily = MorInter,
    )
}

private fun startRecognition(rec: SpeechRecognizer?, ctx: android.content.Context) {
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 60000)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 60000)
    }
    runCatching { rec?.startListening(intent) }
}

@Composable
private fun Waveform(color: Color) {
    val t by rememberInfiniteTransition(label = "wave").animateFloat(
        0f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "waveT",
    )
    Canvas(Modifier.width(96.dp).height(26.dp)) {
        val n = 14
        val gap = size.width / (n * 1.6f)
        val barW = gap * 0.6f
        for (i in 0 until n) {
            val phase = i * 0.62f
            val wave = abs(sin((t * (2f * Math.PI.toFloat()) + phase)))
            val h = size.height * (0.22f + 0.78f * wave)
            val x = i * (barW + gap)
            drawRoundRect(
                color = color,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(barW, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f),
            )
        }
    }
}

@Composable
private fun MicMark(size: androidx.compose.ui.unit.Dp, color: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(
            color, Offset(w * 0.36f, h * 0.06f), Size(w * 0.28f, h * 0.48f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.14f),
        )
        drawArc(
            color, 0f, 180f, false,
            topLeft = Offset(w * 0.2f, h * 0.3f), size = Size(w * 0.6f, h * 0.42f),
            style = Stroke(w * 0.075f, cap = StrokeCap.Round),
        )
        drawLine(color, Offset(w * 0.5f, h * 0.72f), Offset(w * 0.5f, h * 0.92f),
            w * 0.075f, StrokeCap.Round)
    }
}

// ---------- Photo (item 12/15 entry) ----------

@Composable
private fun ScanSection(onScan: () -> Unit) {
    Spacer(Modifier.height(10.dp))
    Text("Scan your plate", fontSize = 20.sp, color = NlInk, fontFamily = Cormorant,
        fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(12.dp))
    Box(
        Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(18.dp))
            .background(Color.White).border(1.5.dp, MorColor.Sand, RoundedCornerShape(18.dp))
            .clickable(onClick = onScan),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            PlateMark(56.dp)
            Spacer(Modifier.height(12.dp))
            Text(
                "Take a photo", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = NlInk, fontFamily = MorInter,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Mor reads the plate, then shows its best matches.",
                fontSize = 11.sp, color = NlMuted, fontFamily = MorInter,
                textAlign = TextAlign.Center,
            )
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(
        "Every figure from a scan is an estimate — confirm the match before it logs.",
        fontSize = 10.5.sp, lineHeight = 15.sp, color = MorColor.NoteFaint, fontFamily = MorInter,
    )
}

@Composable
private fun PlateMark(size: androidx.compose.ui.unit.Dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension
        val c = center
        drawCircle(MorColor.Gold, s * 0.46f, c, style = Stroke(s * 0.05f))
        drawCircle(MorColor.Gold.copy(alpha = 0.55f), s * 0.30f, c, style = Stroke(s * 0.04f))
        drawCircle(MorColor.Gold, s * 0.07f, c)
    }
}
