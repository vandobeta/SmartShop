package com.smartshop.app.ui.screens.scanner

import android.Manifest
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.smartshop.app.SmartShopApplication
import com.smartshop.app.domain.model.CartItem
import com.smartshop.app.domain.model.InventoryItem
import com.smartshop.app.ui.theme.Primary
import kotlinx.coroutines.delay
import com.google.accompanist.permissions.isGranted
import kotlinx.coroutines.runBlocking
import java.util.Locale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    cartItems: List<CartItem>,
    onAddToCart: (InventoryItem, Int) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var isTorchOn by remember { mutableStateOf(false) }
    var showImage by remember { mutableStateOf<String?>(null) }
    var lastScannedItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showNotFoundMessage by remember { mutableStateOf(false) }
    var isScanning by remember { mutableStateOf(true) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val tts = remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        cameraPermissionState.launchPermissionRequest()
        tts.value = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.value?.language = Locale.US
                ttsReady = true
            }
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            tts.value?.stop()
            tts.value?.shutdown()
        }
    }
    
    fun speak(message: String) {
        if (!ttsReady) return
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val ringerMode = audioManager.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            tts.value?.speak(message, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }
    
    fun vibrate(success: Boolean) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (success) {
                vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 100, 100), -1))
            }
        }
    }
    
    LaunchedEffect(showImage) {
        if (showImage != null) {
            delay(2000)
            showImage = null
            isScanning = true
        }
    }
    
    LaunchedEffect(showNotFoundMessage) {
        if (showNotFoundMessage) {
            delay(2000)
            showNotFoundMessage = false
        }
    }
    
    if (!cameraPermissionState.status.isGranted) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required")
        }
        return
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (isScanning && showImage == null) {
            AndroidView(
                factory = { ctx -> PreviewView(ctx).apply { implementationMode = PreviewView.ImplementationMode.COMPATIBLE } },
                modifier = Modifier.fillMaxSize()
            ) { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    val imageAnalyzer = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                                if (!isScanning) { imageProxy.close(); return@setAnalyzer }
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    val options = BarcodeScannerOptions.Builder()
                                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                                        .build()
                                    val scanner = BarcodeScanning.getClient(options)
                                    scanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                barcode.rawValue?.let { barcodeValue ->
                                                    val app = SmartShopApplication.get()
                                                    val item = runBlocking { app.inventoryRepository.getItemByBarcode(barcodeValue) }
                                                    item?.let { foundItem ->
                                                        isScanning = false
                                                        lastScannedItem = foundItem
                                                        showImage = foundItem.imagePath
                                                        speak("Item added")
                                                        vibrate(true)
                                                        onAddToCart(foundItem, 1)
                                                    } ?: run { showNotFoundMessage = true; vibrate(false) }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else { imageProxy.close() }
                            }
                        }
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalyzer)
                    } catch (e: Exception) { Log.e("Scanner", "Camera binding failed", e) }
                }, ContextCompat.getMainExecutor(context))
            }
        }
        
        if (isScanning && showImage == null) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
        }
        
        showImage?.let { _ ->
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f)), contentAlignment = Alignment.Center) {
                lastScannedItem?.let { item ->
                    Card(modifier = Modifier.padding(32.dp).fillMaxWidth(0.8f), shape = RoundedCornerShape(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            AsyncImage(model = showImage, contentDescription = item.name, modifier = Modifier.size(150.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = item.name, style = MaterialTheme.typography.headlineSmall)
                            Text(text = String.format("%,d UGX", item.price), style = MaterialTheme.typography.titleLarge, color = Primary)
                        }
                    }
                }
            }
        }
        
        if (showNotFoundMessage) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                Card(colors = CardDefaults.cardColors(containerColor = Color.Red)) {
                    Text(text = "Item not found in inventory", color = Color.White, modifier = Modifier.padding(16.dp))
                }
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp).statusBarsPadding(), horizontalArrangement = Arrangement.SpaceBetween) {
            FilledTonalIconButton(onClick = { camera?.cameraControl?.enableTorch(!isTorchOn); isTorchOn = !isTorchOn }, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = if (isTorchOn) Primary else Color.White)) {
                Icon(imageVector = if (isTorchOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff, contentDescription = "Torch", tint = if (isTorchOn) Color.White else Primary)
            }
            FilledTonalIconButton(onClick = onNavigateToSettings, colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = Color.White)) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Primary)
            }
        }
        
        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "Items in cart: ${cartItems.size}", style = MaterialTheme.typography.titleMedium)
                        if (cartItems.isNotEmpty()) {
                            Text(text = "Total: ${String.format("%,d UGX", cartItems.sumOf { it.totalPrice })}", style = MaterialTheme.typography.bodyMedium, color = Primary)
                        }
                    }
                    Button(onClick = onNavigateToCart, enabled = cartItems.isNotEmpty()) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Checkout")
                    }
                }
            }
        }
    }
}
