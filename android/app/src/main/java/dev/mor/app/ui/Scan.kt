package dev.mor.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.mor.app.R
import dev.mor.app.data.AppVm
import dev.mor.app.data.ScanResult
import dev.mor.app.data.friendlyErr
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.abs

// ---------- image helpers (mirror web downscaleToJpeg: max edge 1024, q85) ----------

fun downscaleToJpegBytes(src: Bitmap, maxEdge: Int = 1024, quality: Int = 85): ByteArray {
    val scale = minOf(1f, maxEdge / maxOf(src.width, src.height).toFloat())
    val w = (src.width * scale).toInt().coerceAtLeast(1)
    val h = (src.height * scale).toInt().coerceAtLeast(1)
    val small = Bitmap.createScaledBitmap(src, w, h, true)
    val out = ByteArrayOutputStream()
    small.compress(Bitmap.CompressFormat.JPEG, quality, out)
    if (small !== src) small.recycle()
    return out.toByteArray()
}

fun bitmapToBase64(b: Bitmap): String =
    Base64.encodeToString(downscaleToJpegBytes(b), Base64.NO_WRAP)

fun readBitmap(ctx: Context, uri: Uri): Bitmap? =
    ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }

// ---------- Camera (09-camera-scan-13b: ivory deck, no rainbow ring) ----------

private val GuideEase = CubicBezierEasing(0.3f, 0.8f, 0.2f, 1f)

@Composable
fun CameraScreen(nav: NavController, vm: AppVm, slot: String, gallery: Boolean) {
    MorSystemBars(dark = true)
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var hasCam by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { hasCam = it }
    var mode by remember { mutableStateOf("meal") }
    var flashOn by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    var capture: ImageCapture? by remember { mutableStateOf(null) }
    var flashKey by remember { mutableIntStateOf(0) }     // >0 = a capture just happened
    var capturing by remember { mutableStateOf(false) }   // fades the guide + hint
    val flash = remember { Animatable(0f) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val bmp = readBitmap(ctx, uri)
            if (bmp != null) {
                ScanState.imageB64 = bitmapToBase64(bmp)
                bmp.recycle()
                ScanState.mode = mode
                vm.pendingSlot = slot
                ScanState.fromGallery = true
                withContext(Dispatchers.Main) {
                    nav.navigate("scanning") { popUpTo("camera?slot=$slot&gallery=${if (gallery) 1 else 0}") { inclusive = true } }
                }
            } else withContext(Dispatchers.Main) {
                vm.showSnack("Could not read that image", true)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCam) permLauncher.launch(Manifest.permission.CAMERA)
        if (gallery) galleryLauncher.launch("image/*")
    }

    LaunchedEffect(flashKey) {
        if (flashKey > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            flash.snapTo(0.85f)
            flash.animateTo(0f, tween(350, easing = LinearEasing))
        }
    }

    fun shift(d: Int) {
        val tabs = listOf("meal", "label", "barcode")
        mode = tabs[((tabs.indexOf(mode) + d) % tabs.size + tabs.size) % tabs.size]
    }

    fun shoot() {
        val ic = capture ?: return
        capturing = true
        flashKey++
        val out = File(ctx.cacheDir, "mor_${System.currentTimeMillis()}.jpg")
        ic.takePicture(
            ImageCapture.OutputFileOptions.Builder(out).build(),
            ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(o: ImageCapture.OutputFileResults) {
                    scope.launch(Dispatchers.IO) {
                        val bmp = BitmapFactory.decodeFile(out.absolutePath)
                        if (bmp != null) {
                            ScanState.imageB64 = bitmapToBase64(bmp)
                            bmp.recycle()
                            ScanState.mode = mode
                            vm.pendingSlot = slot
                            ScanState.fromGallery = false
                            out.delete()
                            withContext(Dispatchers.Main) {
                                nav.navigate("scanning") { popUpTo("add") }
                            }
                        }
                    }
                }
                override fun onError(e: ImageCaptureException) {
                    capturing = false
                    vm.showSnack("Capture failed: ${e.message}", true)
                }
            })
    }

    val guideW = when (mode) { "meal" -> 300.dp; "label" -> 230.dp; else -> 300.dp }
    val guideH = when (mode) { "meal" -> 300.dp; "label" -> 320.dp; else -> 140.dp }
    val wAnim by animateDpAsState(guideW, tween(if (rememberReducedMotion()) 1 else 450, easing = GuideEase), label = "gw")
    val hAnim by animateDpAsState(guideH, tween(if (rememberReducedMotion()) 1 else 450, easing = GuideEase), label = "gh")
    val uiAlpha by animateFloatAsState(if (capturing) 0f else 1f, tween(200), label = "guideFade")

    Box(Modifier.fillMaxSize().background(MorColor.Ivory)) {
        Column(Modifier.fillMaxSize()) {
            // -- Preview: top 71%, bottom corners 28dp, edge-to-edge under the status bar.
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(71f)
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(Color(0xFF0A0F12))
                    .pointerInput(Unit) {
                        var acc = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { acc = 0f },
                            onDragEnd = {
                                if (acc < -90f) shift(1)
                                else if (acc > 90f) shift(-1)
                            },
                            onDragCancel = { acc = 0f },
                        ) { change, amt ->
                            change.consume()
                            acc += amt
                        }
                    },
            ) {
                if (hasCam && !denied) {
                    AndroidView(
                        factory = { c ->
                            PreviewView(c).also { pv ->
                                val provider = ProcessCameraProvider.getInstance(c).get()
                                val preview = Preview.Builder().build()
                                preview.setSurfaceProvider(pv.surfaceProvider)
                                val ic = ImageCapture.Builder()
                                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                    .setFlashMode(ImageCapture.FLASH_MODE_OFF)
                                    .build()
                                capture = ic
                                try {
                                    provider.unbindAll()
                                    provider.bindToLifecycle(
                                        lifecycle as androidx.lifecycle.LifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA, preview, ic)
                                } catch (e: Exception) {
                                    Log.e("Mor", "camera bind", e)
                                    denied = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Legibility gradient over the first 22% of the preview.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.22f)
                            .drawBehind {
                                drawRect(
                                    Brush.verticalGradient(
                                        0f to Color(0x80140C08), 1f to Color.Transparent,
                                    )
                                )
                            },
                    )
                    // Framing guide, centred and nudged 2% up.
                    val guideAlpha by animateFloatAsState(uiAlpha, tween(200), label = "g")
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = -maxHeight * 0.02f)
                            .alpha(guideAlpha)
                            .size(wAnim, hAnim),
                    ) {
                        AnimatedContent(
                            targetState = mode,
                            transitionSpec = {
                                fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                            },
                            label = "guide",
                        ) { m ->
                            GuideArt(m, Modifier.fillMaxSize())
                        }
                    }
                    // Hint chip, 22dp above the preview's bottom edge.
                    Box(
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 22.dp)
                            .alpha(uiAlpha),
                    ) {
                        AnimatedContent(
                            targetState = mode,
                            transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                            label = "hint",
                        ) { m ->
                            Text(
                                when (m) {
                                    "meal" -> "Fit the whole plate"
                                    "label" -> "Line up the nutrition label"
                                    else -> "Centre the barcode"
                                },
                                fontSize = 13.sp, color = Color(0xFF3A2F25),
                                fontFamily = MorInter, fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xEBFFFBF3))
                                    .padding(14.dp, 7.dp),
                            )
                        }
                    }
                } else {
                    Column(
                        Modifier.align(Alignment.Center).padding(20.dp)
                            .clip(RoundedCornerShape(28.dp)).background(Color(0xFF142027)).padding(24.dp, 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Neelu("droop", 150.dp)
                        Text("Camera access needed", color = Color.White, fontSize = 30.sp, fontFamily = MorDisplay,
                            fontWeight = FontWeight.ExtraBold)
                        Text("Neelu needs to see your food.", color = Color(0xFFC3D1D4), fontSize = 17.sp)
                        Spacer(Modifier.height(8.dp))
                        MorButton("Search food instead") { nav.navigate("nextlog?slot=$slot&mode=text") }
                    }
                }
            }

            // -- Ivory deck: mode tabs + dock.
            Column(
                Modifier
                    .fillMaxWidth()
                    .weight(29f)
                    .background(MorColor.Ivory)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    Modifier.selectableGroup().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                ) {
                    listOf("meal", "label", "barcode").forEach { m ->
                        val on = mode == m
                        Column(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .height(44.dp)
                                .selectable(
                                    selected = on, role = Role.Tab,
                                    onClick = { if (mode != m) { mode = m; MorSound.ensure(ctx); MorSound.select(2) } },
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                m.replaceFirstChar { it.uppercase() },
                                fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                                color = if (on) MorColor.Maroon else Color(0xFF6B5E4E),
                                fontFamily = MorInter,
                            )
                            Spacer(Modifier.height(5.dp))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (on) MorColor.Maroon else Color.Transparent),
                            )
                        }
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MorColor.Card)
                                .border(1.5.dp, Color(0xFFD9C49B), CircleShape)
                                .clickable {
                                    MorSound.ensure(ctx)
                                    MorSound.select(1)
                                    galleryLauncher.launch("image/*")
                                }
                                .semantics { this.contentDescription = "Pick from gallery" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_cam_gallery), null,
                                tint = MorColor.Maroon, modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Gallery", fontSize = 11.5.sp, color = Color(0xFF6B5E4E), fontFamily = MorInter)
                    }
                    // Shutter: 78dp maroon disc, gold hairline, ivory gap, gold ring.
                    val sInter = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    val pressed by sInter.collectIsPressedAsState()
                    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(120), label = "shutter")
                    Box(
                        Modifier
                            .size(78.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .shadow(10.dp, CircleShape, spotColor = MorColor.Maroon.copy(alpha = 0.3f))
                            .clip(CircleShape)
                            .background(MorColor.Maroon)
                            .clickable(interactionSource = sInter, indication = null, onClick = ::shoot)
                            .semantics { this.contentDescription = "Take photo" },
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            val c = Offset(size.width / 2f, size.height / 2f)
                            drawCircle(MorColor.Gold, size.width / 2f - 1.25.dp.toPx(), c, style = Stroke(2.5.dp.toPx()))
                            drawCircle(MorColor.Ivory, size.width / 2f - 4.25.dp.toPx(), c, style = Stroke(4.dp.toPx()))
                            drawCircle(MorColor.Gold.copy(alpha = 0.6f), 30.dp.toPx(), c, style = Stroke(1.5.dp.toPx()))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MorColor.Card)
                                .border(1.5.dp, Color(0xFFD9C49B), CircleShape)
                                .clickable {
                                    MorSound.ensure(ctx)
                                    MorSound.select(1)
                                    nav.navigate("nextlog?slot=$slot&mode=voice")
                                }
                                .semantics { this.contentDescription = "Describe by voice" },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_cam_mic), null,
                                tint = MorColor.Maroon, modifier = Modifier.size(24.dp),
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Speak", fontSize = 11.5.sp, color = Color(0xFF6B5E4E), fontFamily = MorInter)
                    }
                }
            }
        }

        // Top bar over the preview: Close + Flash.
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp, 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xE0FFFBF3))
                    .clickable { if (!nav.popBackStack()) nav.navigate("add") }
                    .semantics { this.contentDescription = "Close" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_cam_close), null,
                    tint = MorColor.Maroon, modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xE0FFFBF3))
                    .clickable {
                        flashOn = !flashOn
                        capture?.flashMode =
                            if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                    }
                    .semantics { this.contentDescription = if (flashOn) "Flash on" else "Flash off" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(if (flashOn) R.drawable.ic_cam_flash_on else R.drawable.ic_cam_flash_off), null,
                    tint = MorColor.Maroon, modifier = Modifier.size(24.dp),
                )
            }
        }

        // Capture flash: white 85% → 0 over 350ms, over the preview only.
        if (flash.value > 0f) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.71f)
                    .graphicsLayer { alpha = flash.value }
                    .background(Color.White),
            )
        }
    }
}

/** The dotted ring / corner brackets / barcode line, drawn in the animated guide box. */
@Composable
private fun GuideArt(mode: String, modifier: Modifier) {
    val reduced = rememberReducedMotion()
    when (mode) {
        "meal" -> {
            val inf = rememberInfiniteTransition(label = "ring")
            val rot by inf.animateFloat(
                0f, 360f,
                infiniteRepeatable(tween(if (reduced) 1 else 14000, easing = LinearEasing)),
                label = "ringRot",
            )
            Canvas(modifier.rotate(rot)) {
                drawCircle(
                    Color(0xFFF2C86A), size.width / 2f - 1.5.dp.toPx(), center,
                    style = Stroke(
                        3.dp.toPx(), cap = StrokeCap.Round,
                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                            floatArrayOf(2.dp.toPx(), 12.dp.toPx()),
                        ),
                    ),
                )
            }
        }
        else -> {
            val inf = rememberInfiniteTransition(label = "scan")
            val line by inf.animateFloat(
                -46f, 46f,
                infiniteRepeatable(
                    tween(if (reduced) 1 else 1100, easing = FastOutSlowInEasing),
                    repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
                ),
                label = "scanY",
            )
            Canvas(modifier) {
                val r = 16.dp.toPx()
                val arm = 36.dp.toPx()
                val s = 3.dp.toPx()
                val g = Color(0xFFF2C86A)
                // Four corner brackets: arm → quarter arc → arm.
                listOf(
                    listOf(Offset(0f, arm), Offset(0f, 0f), Offset(arm, 0f)),
                    listOf(Offset(size.width - arm, 0f), Offset(size.width, 0f), Offset(size.width, arm)),
                    listOf(Offset(size.width, size.height - arm), Offset(size.width, size.height), Offset(size.width - arm, size.height)),
                    listOf(Offset(arm, size.height), Offset(0f, size.height), Offset(0f, size.height - arm)),
                ).forEach { pts ->
                    val p = Path()
                    p.moveTo(pts[0].x, pts[0].y)
                    p.lineTo(pts[1].x, pts[1].y)
                    p.lineTo(pts[2].x, pts[2].y)
                    drawPath(p, g, style = Stroke(s, cap = StrokeCap.Round))
                }
                if (mode == "barcode") {
                    // Gold scan line with a soft glow, ±46dp around the middle.
                    val y = center.y + line.dp.toPx()
                    drawLine(
                        g.copy(alpha = 0.25f), Offset(6.dp.toPx(), y), Offset(size.width - 6.dp.toPx(), y),
                        6.dp.toPx(), cap = StrokeCap.Round,
                    )
                    drawLine(
                        g, Offset(6.dp.toPx(), y), Offset(size.width - 6.dp.toPx(), y),
                        2.dp.toPx(), cap = StrokeCap.Round,
                    )
                }
            }
        }
    }
}

/** In-memory pending capture (mirrors web pendingScan.image). */
object ScanState {
    var imageB64: String? = null
    var fromGallery: Boolean = false
    /** Camera mode that captured the photo — drives the "Reading …" copy. */
    var mode: String = "meal"
    /** Next-version flow: after the read, route to the AI + web chooser
     *  instead of the legacy result screen. Set by the next log screen. */
    var next: Boolean = false

    fun imageBitmap(): Bitmap? = try {
        imageB64?.let {
            val bytes = Base64.decode(it, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    } catch (_: Exception) { null }
}

// ---------- Scanning ----------

@Composable
fun ScanningScreen(nav: NavController, vm: AppVm) {
    MorSystemBars(dark = true)
    var failMsg by remember { mutableStateOf<String?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(attempt) {
        if (ScanState.imageB64 == null) {
            nav.navigate("add") { popUpTo("scanning") { inclusive = true } }
            return@LaunchedEffect
        }
        failMsg = null
        scope.launch(Dispatchers.IO) {
            try {
                val r = vm.api().scan(mapOf(
                    "image_base64" to ScanState.imageB64!!,
                    "mime" to "image/jpeg",
                    "slot" to vm.pendingSlot,
                ))
                vm.lastScan = r
                withContext(Dispatchers.Main) {
                    if (ScanState.next) {
                        // Items 12 + 15: photo lookups end in the same
                        // AI + web candidate chooser as text and voice.
                        val q = java.net.URLEncoder.encode(
                            (r.meal_name ?: "").ifBlank { "my meal" }, "UTF-8")
                        nav.navigate("nextlog?slot=${vm.pendingSlot}&mode=text&q=$q&auto=1") {
                            popUpTo("scanning") { inclusive = true }
                        }
                    } else {
                        nav.navigate("result/${r.scan_id}") { popUpTo("scanning") { inclusive = true } }
                    }
                }
            } catch (e: Exception) {
                // Network/processing failure is NOT a "not food" verdict: the photo is
                // kept so the user can retry it directly (items 2, 37).
                val msg = when (e) {
                    is java.io.IOException -> "The photo couldn't be uploaded — connection broke off. Your plate photo is kept."
                    is retrofit2.HttpException -> "The server couldn't process the photo right now (code ${e.code()}). Your plate photo is kept."
                    else -> "The scan didn't complete (${friendlyErr(e)}). Your plate photo is kept."
                }
                withContext(Dispatchers.Main) { failMsg = msg }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0F12))) {
        val bmp = remember { ScanState.imageBitmap() }
        // Reading state (09): the captured plate, dimmed, behind a gold dash frame.
        if (bmp != null) {
            Image(bmp.asImageBitmap(), "Your plate",
                Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop, alpha = 0.35f)
        }
        Box(Modifier.fillMaxSize().background(Color(0x80140C08)))

        if (failMsg == null) {
            val title = when (ScanState.mode) {
                "label" -> "Reading the label…"
                "barcode" -> "Looking up the product…"
                else -> "Reading your plate…"
            }
            Column(
                Modifier.align(Alignment.Center).padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ReadingFrame()
                Spacer(Modifier.height(22.dp))
                Text(
                    title, fontFamily = Cormorant, fontWeight = FontWeight.SemiBold,
                    fontSize = 26.sp, color = Color(0xFFFFF6E6), textAlign = TextAlign.Center,
                    modifier = Modifier.semantics {
                        liveRegion = androidx.compose.ui.semantics.LiveRegionMode.Polite
                    },
                )
                Spacer(Modifier.height(4.dp))
                Text("Usually a few seconds", fontSize = 13.sp, color = Color(0xFFEADBC0))
                Spacer(Modifier.height(18.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, Color(0x66EADBC0), RoundedCornerShape(999.dp))
                        .clickable { nav.navigate("camera?slot=${vm.pendingSlot}") }
                        .padding(horizontal = 24.dp, vertical = 9.dp),
                ) {
                    Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        color = Color(0xFFF6F0E4), fontFamily = MorInter)
                }
            }
        } else {
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))
                    .background(Color(0xFF142027)).padding(20.dp, 18.dp, 20.dp, 26.dp),
            ) {
                Text(failMsg!!, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.weight(1f)) {
                        MorSecondaryButton("New photo") {
                            nav.navigate("camera?slot=${vm.pendingSlot}&gallery=1") {
                                popUpTo("scanning") { inclusive = true }
                            }
                        }
                    }
                    Box(Modifier.weight(1f)) {
                        MorButton("Try again") { attempt++ }
                    }
                }
            }
        }
    }
}

/** 300dp rounded square, radius 36: a faint gold track with one bright dash
 *  (60/340 of the loop) running around it in a 1.4s linear loop. */
@Composable
private fun ReadingFrame() {
    val reduced = rememberReducedMotion()
    val inf = rememberInfiniteTransition(label = "read")
    val t by inf.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(if (reduced) 1 else 1400, easing = LinearEasing)),
        label = "readT",
    )
    Canvas(Modifier.size(300.dp)) {
        val r = 36.dp.toPx()
        drawRoundRect(
            Color(0xFFF2C86A).copy(alpha = 0.25f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
        )
        // The moving dash: measure the rounded-rect path, slice [start, start+dash].
        val rect = Path().apply {
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    0f, 0f, size.width, size.height,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r),
                )
            )
        }
        val pm = androidx.compose.ui.graphics.PathMeasure()
        pm.setPath(rect, false)
        val len = pm.length
        val dash = len * 60f / 340f
        val start = (t % 1f) * len
        val end = start + dash
        val seg = Path()
        if (end <= len) {
            pm.getSegment(start, end, seg, true)
        } else {
            pm.getSegment(start, len, seg, true)
            pm.getSegment(0f, end - len, seg, true)
        }
        drawPath(seg, Color(0xFFF2C86A), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

// ---------- Result ----------

@Composable
fun ResultScreen(nav: NavController, vm: AppVm, scanId: Long) {
    MorSystemBars(dark = true)
    val scope = rememberCoroutineScope()
    var scan by remember { mutableStateOf(vm.lastScan?.takeIf { it.scan_id == scanId }) }
    var oil by remember { mutableStateOf("normal") }
    var items by remember(scan) { mutableStateOf(scan?.items) }
    var swapFor by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(scanId) {
        if (scan == null) {
            scope.launch(Dispatchers.IO) {
                try {
                    val s = vm.api().scanDetail(scanId)
                    withContext(Dispatchers.Main) {
                        scan = ScanResult(scan_id = s.id, status = s.status,
                            meal_name = s.meal_name, items = s.items,
                            image_url = s.image_path?.let { "/uploads/$it" })
                        items = s.items
                    }
                } catch (_: Exception) {
                    withContext(Dispatchers.Main) { nav.navigate("add") }
                }
            }
        }
    }

    if (scan == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF0E1A1F)), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Gold)
        }
        return
    }
    val s = scan!!
    val list = items
    val failed = s.status == "failed" || list.isNullOrEmpty()
    val unsureRow = list?.indexOfFirst { it.unsure == true } ?: -1
    val total = list?.sumOf { it.kcal }?.toInt() ?: 0

    Column(Modifier.fillMaxSize().background(Color(0xFF0E1A1F))) {
        Box(Modifier.fillMaxWidth().height(232.dp), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.padding(top = 34.dp).size(180.dp).clip(CircleShape)
                .background(Color(0xFF3B2B21))
                .border(3.dp, if (failed) Kumkum else Gold, CircleShape)) {
                if (s.image_url != null) {
                    AsyncImage("${vm.store.baseUrl}${s.image_url}", "Your plate",
                        modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else Text(emojiForFood(s.meal_name ?: ""), fontSize = 90.sp,
                    modifier = Modifier.align(Alignment.Center))
            }
            Box(Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp, 18.dp).size(44.dp)
                .clip(CircleShape).background(Color(0x24FFFFFF))
                .clickable { nav.navigate("camera?slot=${vm.pendingSlot}") },
                contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, fontSize = 18.sp)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))
                .background(Chandan).padding(16.dp),
        ) {
            if (failed) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Neelu("droop", 150.dp)
                    H1("Couldn't see the food", 32, TextAlign.Center)
                    Muted(if (s.status == "failed" && list.isNullOrEmpty()) "No dish recognised. Try better light."
                        else "Scan failed. Try again.", 18, TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    MorButton("Retake photo") { nav.navigate("camera?slot=${vm.pendingSlot}") }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) {
                            MorSecondaryButton("Search food") {
                                nav.navigate("nextlog?slot=${vm.pendingSlot}&mode=text")
                            }
                        }
                        Box(Modifier.weight(1f)) {
                            MorSecondaryButton("Speak") {
                                nav.navigate("nextlog?slot=${vm.pendingSlot}&mode=voice")
                            }
                        }
                    }
                }
            } else {
                val nn = list!!
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${vm.pendingSlot.replaceFirstChar { it.uppercase() }} · Just now",
                            fontSize = 13.sp, color = Sub, fontWeight = FontWeight.SemiBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(s.meal_name ?: "Your meal", fontSize = 24.sp, fontFamily = MorDisplay,
                                fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                            Pill("AI estimate", LavBg, Jamun)
                        }
                    }
                    Box(Modifier.size(40.dp).clip(CircleShape).background(Disabled)
                        .clickable { nav.navigate("add?slot=${vm.pendingSlot}") },
                        contentAlignment = Alignment.Center) { Text("🗑️", fontSize = 18.sp) }
                }
                if (unsureRow >= 0) {
                    val u = nn[unsureRow]
                    MorCard(Modifier.fillMaxWidth().background(AmberBg)) {
                        Column {
                            Text("${u.idx ?: unsureRow + 1} · ${u.name}?", fontSize = 18.sp,
                                fontWeight = FontWeight.Bold, color = Amber)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (u.alternatives ?: emptyList()).take(2).forEach { a ->
                                    Box(Modifier.weight(1f).clip(RoundedCornerShape(22.dp))
                                        .background(Color.White).border(1.5.dp, Color(0xFFC98A1E),
                                            RoundedCornerShape(22.dp))
                                        .clickable { swapFor = unsureRow; swapItem(vm, scope, nn, unsureRow, a) { items = it } }
                                        .padding(8.dp),
                                        contentAlignment = Alignment.Center) {
                                        Text(a, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    nn.forEachIndexed { i, it ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box {
                                Text(emojiForFood(it.name), fontSize = 34.sp)
                                Box(Modifier.size(17.dp).clip(CircleShape).background(Ink)
                                    .align(Alignment.TopStart),
                                    contentAlignment = Alignment.Center) {
                                    Text("${it.idx ?: i + 1}", color = Color.White, fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(it.name, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Text("${it.portion ?: ""}${if (it.unsure == true) " · needs a check" else ""}",
                                    fontSize = 13.sp, color = Teal, fontWeight = FontWeight.SemiBold)
                            }
                            Text(if (it.unsure == true) "—" else "${it.kcal.toInt()} kcal",
                                fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Oil", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Sub)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        Segmented(listOf("less", "normal", "more"), oil) { oil = it }
                    }
                }
                Spacer(Modifier.height(8.dp))
                MorButton("Log $total kcal", enabled = unsureRow < 0) {
                    scope.launch(Dispatchers.IO) {
                        try {
                            val clean = nn.filter { it.unsure != true }
                            val r = vm.api().logMeal(mapOf(
                                "slot" to vm.pendingSlot, "source" to "scan",
                                "scan_id" to s.scan_id, "oil_level" to oil,
                                "name" to (s.meal_name ?: "Scanned meal"),
                                "items" to clean.map {
                                    mapOf("name" to it.name, "kcal" to it.kcal,
                                        "protein_g" to it.protein_g, "carbs_g" to it.carbs_g,
                                        "fat_g" to it.fat_g, "portion" to (it.portion ?: ""))
                                }))
                            vm.lastLog = r
                            withContext(Dispatchers.Main) { nav.navigate("logged") }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                vm.showSnack("Log failed: ${friendlyErr(e)}", true)
                            }
                        }
                    }
                }
            }
        }
    }
    if (swapFor != null) { /* swap runs inline; clear flag when items change */ }
}

private fun swapItem(
    vm: AppVm,
    scope: kotlinx.coroutines.CoroutineScope,
    cur: List<dev.mor.app.data.FoodItem>,
    row: Int,
    alt: String,
    done: (List<dev.mor.app.data.FoodItem>) -> Unit,
) {
    scope.launch(Dispatchers.IO) {
        try {
            val matches = vm.api().foods(q = alt).items
            val hit = matches.firstOrNull { it.name.equals(alt, true) } ?: matches.firstOrNull()
            val next = cur.mapIndexed { i, it ->
                if (i != row) it else it.copy(name = alt, unsure = false,
                    alternatives = emptyList(),
                    kcal = hit?.kcal ?: it.kcal,
                    protein_g = hit?.protein_g ?: it.protein_g,
                    carbs_g = hit?.carbs_g ?: it.carbs_g,
                    fat_g = hit?.fat_g ?: it.fat_g)
            }
            withContext(Dispatchers.Main) { done(next) }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { vm.showSnack("Could not swap dish", true) }
        }
    }
}

// ---------- Logged ----------

/**
 * Next-version item 16 (S16, "Growing bar") + item 18 beat 2.
 *
 * The real screen chrome stays: perched Neelu, "Logged!", the kcal line and
 * See details / Done. The celebration is the day's bar: the existing progress
 * holds, then a gold segment grows by the meal's share, the add label lands,
 * and the macro lines tick in one by one. About 2.6s in, the already-updated
 * home slides in over it (see the "home" enter transition) - the totals are
 * already written, so there is no stale flash.
 */
@Composable
fun LoggedScreen(nav: NavController, vm: AppVm) {
    MorSystemBars(dark = false)
    val log = vm.lastLog
    val home by vm.home.collectAsState()
    LaunchedEffect(Unit) { vm.refreshHome() }

    val reduced = rememberReducedMotion()
    val target = (home?.kcal_target ?: 2000.0).coerceAtLeast(1.0)
    val mealKcal = log?.kcal ?: 0.0
    val postKcal = (home?.kcal_eaten ?: mealKcal).coerceAtLeast(mealKcal)
    val preKcal = (postKcal - mealKcal).coerceAtLeast(0.0)
    val left = (target - postKcal).coerceAtLeast(0.0)
    val mealName = log?.name?.takeIf { it.isNotBlank() } ?: "meal"

    val preFrac = (preKcal / target).coerceIn(0.0, 1.0).toFloat()
    val goldFrac = remember { Animatable(preFrac) }
    LaunchedEffect(postKcal) {
        if (reduced) {
            goldFrac.snapTo((postKcal / target).coerceIn(0.0, 1.0).toFloat())
        } else {
            delay(320)
            goldFrac.animateTo(
                (postKcal / target).coerceIn(0.0, 1.0).toFloat(),
                tween(900, easing = H2OutCubic),
            )
        }
    }

    // Item 18 beat 2: hop -> the fan opens in the Red Court frame.
    var fanIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(if (reduced) 0 else 520)
        fanIn = true
    }

    // Continuity: the updated home slides in over the celebration.
    LaunchedEffect(Unit) {
        // Issue 25: measured code baseline 2,600ms + 300ms transition; +5s.
        delay(7600)
        if (nav.currentDestination?.route?.startsWith("logged") == true) {
            nav.navigate("home") {
                popUpTo("home") { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val eaten = home?.macros?.eaten
    val goal = home?.macros?.goal
    val macroRows = listOf(
        Triple("Protein", eaten?.protein_g ?: 0.0, goal?.protein_g ?: 0.0) to MorColor.Protein,
        Triple("Carbs", eaten?.carbs_g ?: 0.0, goal?.carbs_g ?: 0.0) to MorColor.Carbs,
        Triple("Fat", eaten?.fat_g ?: 0.0, goal?.fat_g ?: 0.0) to MorColor.Fat,
    )

    Column(
        Modifier.fillMaxSize().background(MorColor.ScreenCream)
            .statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.height(150.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
            // The fan reveal sits behind the perched bird, in a court frame.
            if (fanIn) {
                val fanScale by animateFloatAsState(1f, tween(520), label = "fanScale")
                Box(
                    Modifier.size(132.dp).clip(RoundedCornerShape(18.dp))
                        .background(MorColor.MaroonDark)
                        .border(2.dp, MorColor.Gold, RoundedCornerShape(18.dp))
                        .graphicsLayer { scaleX = fanScale; scaleY = fanScale },
                ) {
                    Image(
                        painterResource(R.drawable.neelu_fan), "Neelu's fan",
                        Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    )
                }
            }
            Image(
                painterResource(R.drawable.neelu_cut), "Neelu",
                Modifier.size(118.dp).offset(y = if (fanIn) (-6).dp else 0.dp),
                contentScale = ContentScale.Fit,
            )
        }

        Spacer(Modifier.height(10.dp))
        Text(
            "Logged!", fontSize = 40.sp, color = MorColor.Ink,
            fontFamily = Cormorant, fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "${h2fmt(mealKcal)} kcal · ${h2fmt(left)} kcal left today",
            fontSize = 13.5.sp, color = MorColor.Ink2, fontFamily = MorInter,
        )

        Spacer(Modifier.height(26.dp))

        // The day's bar: existing progress first, then the gold add segment.
        Canvas(Modifier.fillMaxWidth().height(12.dp)) {
            val r = size.height / 2f
            drawRoundRect(MorColor.Track, size = size, cornerRadius = CornerRadius(r))
            val preW = size.width * preFrac
            if (preW > 0f) {
                drawRoundRect(
                    Color(0xFF0F5560),
                    size = Size(preW, size.height),
                    cornerRadius = CornerRadius(r),
                )
            }
            val g = goldFrac.value.coerceAtLeast(preFrac)
            val gW = size.width * g
            if (gW > preW) {
                drawRoundRect(
                    MorColor.Carbs,
                    topLeft = Offset(preW, 0f),
                    size = Size(gW - preW, size.height),
                    cornerRadius = CornerRadius(r),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            Text(
                "${h2fmt(postKcal)} of ${h2fmt(target)} kcal",
                fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold,
                color = MorColor.Ink, fontFamily = MorInter, modifier = Modifier.weight(1f),
            )
            Text(
                "${h2fmt(left)} left", fontSize = 12.5.sp,
                color = MorColor.Ink2, fontFamily = MorInter,
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "+${h2fmt(mealKcal)} from the $mealName",
            fontSize = 11.5.sp, color = MorColor.GoldText, fontFamily = MorInter,
        )

        Spacer(Modifier.height(18.dp))
        macroRows.forEachIndexed { i, (row, color) ->
            val (label, g, t) = row
            var shown by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(if (reduced) 0 else 900L + i * 200L)
                shown = true
            }
            val a by animateFloatAsState(if (shown) 1f else 0f, tween(300), label = "macro$i")
            Text(
                "$label ${g.roundToInt()} / ${t.roundToInt()} g",
                fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = color,
                fontFamily = MorInter,
                modifier = Modifier.alpha(a),
            )
            Spacer(Modifier.height(3.dp))
        }

        Spacer(Modifier.height(28.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                MorSecondaryButton("See details") {
                    log?.let { nav.navigate("meal/${it.id}") }
                }
            }
            Box(Modifier.weight(1f)) {
                MorButton("Done") {
                    nav.navigate("home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}
