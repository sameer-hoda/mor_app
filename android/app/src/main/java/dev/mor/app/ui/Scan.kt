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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import dev.mor.app.data.AppVm
import dev.mor.app.data.ScanResult
import dev.mor.app.data.friendlyErr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

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

// ---------- Camera ----------

@Composable
fun CameraScreen(nav: NavController, vm: AppVm, slot: String, gallery: Boolean) {
    val ctx = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var hasCam by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()) { hasCam = it }
    var mode by remember { mutableStateOf("meal") }
    var denied by remember { mutableStateOf(false) }
    var capture: ImageCapture? by remember { mutableStateOf(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch(Dispatchers.IO) {
            val bmp = readBitmap(ctx, uri)
            if (bmp != null) {
                ScanState.imageB64 = bitmapToBase64(bmp)
                bmp.recycle()
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

    DisposableEffect(Unit) { onDispose { } }

    Box(Modifier.fillMaxSize().background(Color(0xFF0A0F12))) {
        if (hasCam && !denied) {
            AndroidView(
                factory = { c ->
                    PreviewView(c).also { pv ->
                        val provider = ProcessCameraProvider.getInstance(c).get()
                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(pv.surfaceProvider)
                        val ic = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
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
            // Frame guide
            Box(Modifier.align(Alignment.Center).size(280.dp)
                .border(4.dp, Gold, RoundedCornerShape(6.dp)))
            Text("Fit the whole plate", color = Color.White, fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
                    .padding(top = 340.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xB80A0F12)).padding(16.dp, 6.dp))
        }

        // Top bar
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp, 18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0x24FFFFFF))
                .clickable { if (!nav.popBackStack()) nav.navigate("add") },
                contentAlignment = Alignment.Center) {
                Text("✕", color = Color.White, fontSize = 18.sp)
            }
            Spacer(Modifier.weight(1f))
            Row(Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0x990A0F12)).padding(3.dp)) {
                listOf("meal", "label", "barcode").forEach { m ->
                    val on = mode == m
                    Box(Modifier.clip(RoundedCornerShape(17.dp))
                        .background(if (on) Color.White else Color.Transparent)
                        .clickable { mode = m }.padding(12.dp, 8.dp)) {
                        Text(m.replaceFirstChar { it.uppercase() }, fontSize = 13.sp,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (on) Color(0xFF142027) else Color(0xFFE5ECEE))
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Spacer(Modifier.width(44.dp))
        }

        if (!hasCam || denied) {
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
                MorButton("Search food instead") { nav.navigate("search?slot=$slot") }
            }
        }

        // Bottom controls
        if (hasCam && !denied) {
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color(0xFF0A0F12)).navigationBarsPadding().padding(24.dp, 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                Box(Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2A3A40)).border(2.dp, Color.White, RoundedCornerShape(14.dp))
                    .clickable { galleryLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center) {
                    Text("🖼️", fontSize = 28.sp)
                }
                val ic = capture
                Box(
                    Modifier.size(84.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape)
                        .clickable(enabled = ic != null) {
                            val out = File(ctx.cacheDir, "mor_${System.currentTimeMillis()}.jpg")
                            ic?.takePicture(
                                ImageCapture.OutputFileOptions.Builder(out).build(),
                                ContextCompat.getMainExecutor(ctx),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(o: ImageCapture.OutputFileResults) {
                                        scope.launch(Dispatchers.IO) {
                                            val bmp = BitmapFactory.decodeFile(out.absolutePath)
                                            if (bmp != null) {
                                                ScanState.imageB64 = bitmapToBase64(bmp)
                                                bmp.recycle()
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
                                        vm.showSnack("Capture failed: ${e.message}", true)
                                    }
                                })
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(66.dp).clip(CircleShape).background(Gold),
                        contentAlignment = Alignment.Center) {
                        Box(Modifier.size(50.dp).clip(CircleShape).background(Pankh),
                            contentAlignment = Alignment.Center) {
                            Box(Modifier.size(30.dp).clip(CircleShape).background(Jamun))
                        }
                    }
                }
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0x24FFFFFF))
                    .clickable { nav.navigate("search?slot=$slot") },
                    contentAlignment = Alignment.Center) {
                    Text("🔍", fontSize = 22.sp, color = Color.White)
                }
            }
        }
    }
}

/** In-memory pending capture (mirrors web pendingScan.image). */
object ScanState {
    var imageB64: String? = null
    var fromGallery: Boolean = false

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
                    nav.navigate("result/${r.scan_id}") { popUpTo("scanning") { inclusive = true } }
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
        Text("Reading your plate…", color = Color.White, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
                .clip(RoundedCornerShape(22.dp)).background(Color(0xB80A0F12)).padding(16.dp, 6.dp))
        // Decorative develop loop: photo card ejects, develops, becomes a sticker.
        // Purely decorative — it never claims detection progress (spec 10 Nov).
        if (bmp != null) {
            Image(bmp.asImageBitmap(), "Your plate",
                Modifier.fillMaxWidth().height(420.dp).background(Color(0xFF0A0F12)),
                contentScale = ContentScale.Crop, alpha = 0.5f)
            DevelopLoop(bmp.asImageBitmap(),
                modifier = Modifier.align(Alignment.Center),
                reduced = rememberReducedMotion())
        }
        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .clip(RoundedCornerShape(28.dp, 28.dp, 0.dp, 0.dp))
                .background(Color(0xFF142027)).padding(20.dp, 18.dp, 20.dp, 26.dp),
        ) {
            Text("Reading your plate…", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Usually takes a few seconds", color = Color(0xFF9FB4BB), fontSize = 14.sp)
            if (failMsg == null) {
                Text("Cancel", color = Color(0xFFC3D1D4),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 14.dp)
                        .clickable { nav.navigate("camera?slot=${vm.pendingSlot}") })
            } else {
                Spacer(Modifier.height(12.dp))
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

/** The eject → develop → sticker wait, one ~4.6s infinite loop. Reduced motion
 *  freezes at the developed end state (crisp photo, ring on, no sparkles). */
@Composable
private fun DevelopLoop(photo: androidx.compose.ui.graphics.ImageBitmap, modifier: Modifier, reduced: Boolean) {
    val inf = rememberInfiniteTransition(label = "develop")
    val t by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4600, easing = LinearEasing)),
        label = "developT",
    )
    val u = if (reduced) 1f else t
    // Beats (all times ms): eject ease-out to 650; develop 650→2250; ring pop
    // ~2250→2600; sparkles staggered pops 2700→3300; hold to 4600.
    fun seg(a: Float, b: Float) = ((u - a) / (b - a)).coerceIn(0f, 1f)
    val outPow = 1f - (1f - seg(0f, 650f / 4600f)).let { it * it }      // ease-out cubic-ish
    val ejectY = (1f - outPow) * 340f                                  // slides up into centre
    val ejectRot = (1f - outPow) * -7f
    val dev = seg(650f / 4600f, 2250f / 4600f)
    val sat = dev.coerceAtLeast(0f)
    val photoBlur = (1f - dev) * 12f
    val photoAlpha = 0.45f + 0.55f * dev
    val ring = (seg(2250f / 4600f, 2600f / 4600f))
    val ringScale = 0.6f + 0.4f * (ring * ring)                        // soft pop
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(210.dp, 214.dp)) {
            // White photo card + its white sticker ring.
            Canvas(
                Modifier.matchParentSize(),
            ) {
                val r = size.minDimension / 2f + 14.dp.toPx()
                if (ring > 0f) {
                    drawCircle(
                        Color.White.copy(alpha = ring), r * ringScale,
                        style = Stroke(6.dp.toPx() * ring),
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationY = ejectY
                        rotationZ = ejectRot
                        scaleX = 0.94f + 0.06f * outPow
                        scaleY = 0.94f + 0.06f * outPow
                    },
            ) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(7.dp),
                ) {
                    Box(
                        Modifier.size(196.dp, 148.dp).clip(RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            photo,
                            "Your plate photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(photoAlpha).blur(photoBlur.dp),
                            colorFilter = ColorFilter.colorMatrix(
                                ColorMatrix().apply { setToSaturation(0.15f + 0.85f * sat) },
                            ),
                        )
                    }
                    Text(
                        "plate_01", fontFamily = MorBody,
                        fontSize = 10.sp, color = Sub,
                        modifier = Modifier.padding(4.dp, 4.dp, 0.dp, 0.dp),
                    )
                }
            }
            if (reduced || u > 0.5f) {
                // Mosaic Neelu (thinking) watches from the card's lower-right.
                NeeluBitmap("thinking", Modifier.size(56.dp).align(Alignment.BottomEnd),
                    contentDescription = null)
            }
            // Three gold sparkles, staggered pops after the sticker lands.
            (0..2).map { i ->
                val s0 = (2700f + i * 180f) / 4600f
                val s1 = s0 + 260f / 4600f
                val sp = seg(s0, s1)
                val sizeDp = if (i == 1) 9f else 6f
                val px = when (i) {
                    0 -> 40.dp
                    1 -> 176.dp
                    else -> 96.dp
                }
                val py = when (i) {
                    0 -> 26.dp
                    1 -> 16.dp
                    else -> 194.dp
                }
                if (sp > 0f && !reduced) {
                    Canvas(
                        Modifier
                            .offset(x = px, y = py)
                            .size(sizeDp.dp * (0.5f + sp)),
                    ) {
                        val r = size.minDimension / 2f
                        drawPath(
                            Path().apply {
                                moveTo(center.x - r * sp / 2.4f, center.y)
                                quadraticTo(center.x, center.y, center.x, center.y - r * sp)
                                quadraticTo(center.x, center.y, center.x + r * sp / 2.4f, center.y)
                                quadraticTo(center.x, center.y, center.x, center.y + r * sp)
                                quadraticTo(center.x, center.y, center.x - r * sp / 2.4f, center.y)
                            },
                            color = Gold.copy(alpha = sp),
                        )
                    }
                }
            }
        }
    }
}

// ---------- Result ----------

@Composable
fun ResultScreen(nav: NavController, vm: AppVm, scanId: Long) {
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
                            MorSecondaryButton("Search food") { nav.navigate("search?slot=${vm.pendingSlot}") }
                        }
                        Box(Modifier.weight(1f)) {
                            MorSecondaryButton("Speak") { nav.navigate("search?slot=${vm.pendingSlot}&voice=1") }
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

@Composable
fun LoggedScreen(nav: NavController, vm: AppVm) {
    val log = vm.lastLog
    val home by vm.home.collectAsState()
    LaunchedEffect(Unit) { vm.refreshHome() }
    val left = home?.let { maxOf(0.0, it.kcal_target - it.kcal_eaten).toInt() } ?: 940
    Column(
        Modifier.fillMaxSize().background(Color(0xFFFDEBC8)).statusBarsPadding()
            .navigationBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Pill("+${log?.coins_awarded ?: 10} coins", Color(0xFFFFF3DC), Amber)
        Neelu("hop", 200.dp, enter = true)
        H1("Logged!", 40, TextAlign.Center)
        Muted("${log?.kcal?.toInt() ?: 0} kcal · ${"%,d".format(left)} kcal left today", 18, TextAlign.Center)
        Spacer(Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.weight(1f)) {
                MorSecondaryButton("See details") { log?.let { nav.navigate("meal/${it.id}") } }
            }
            Box(Modifier.weight(1f)) {
                MorButton("Done") { nav.navigate("home") { popUpTo("home") { inclusive = true } } }
            }
        }
    }
}
