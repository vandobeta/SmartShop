package com.smartshop.sovereign.presentation.ui.screen.scanner

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.smartshop.sovereign.presentation.ui.theme.SmartShopColors
import com.smartshop.sovereign.presentation.viewmodel.ScannerViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
    onNavigateToCheckout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermission.status.isGranted) {
            // Camera Preview
            CameraPreview(
                isTorchOn = uiState.isTorchOn,
                onBarcodeScanned = { barcode ->
                    viewModel.onBarcodeScanned(barcode)
                }
            )

            // Overlay UI
            ScannerOverlay(
                isTorchOn = uiState.isTorchOn,
                productName = uiState.scannedProduct?.name,
                productPrice = uiState.scannedProduct?.formatPrice(),
                isProductNotFound = uiState.isProductNotFound,
                barcode = uiState.lastBarcode,
                isTimerActive = uiState.isTimerActive,
                timerProgress = uiState.timerProgress,
                cartItemCount = uiState.cartItems.size,
                onTorchToggle = { viewModel.toggleTorch() },
                onCheckoutClick = onNavigateToCheckout,
                onAddProductClick = { /* TODO: Navigate to add product */ }
            )
        } else {
            // Permission denied UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = SmartShopColors.TextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Camera Permission Required",
                    style = MaterialTheme.typography.headlineSmall,
                    color = SmartShopColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { cameraPermission.launchPermissionRequest() }) {
                    Text("Grant Permission")
                }
            }
        }
    }
}

@Composable
private fun CameraPreview(
    isTorchOn: Boolean,
    onBarcodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner = remember { BarcodeScanning.getClient() }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        },
        modifier = Modifier.fillMaxSize(),
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor) { imageProxy ->
                            @androidx.camera.core.ExperimentalGetImage
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                barcodeScanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { value ->
                                                onBarcodeScanned(value)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    camera.cameraControl.enableTorch(isTorchOn)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

@Composable
private fun ScannerOverlay(
    isTorchOn: Boolean,
    productName: String?,
    productPrice: String?,
    isProductNotFound: Boolean,
    barcode: String,
    isTimerActive: Boolean,
    timerProgress: Float,
    cartItemCount: Int,
    onTorchToggle: () -> Unit,
    onCheckoutClick: () -> Unit,
    onAddProductClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Viewfinder overlay
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = 0.8f }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val frameWidth = canvasWidth * 0.85f
            val frameHeight = frameWidth * 0.6f
            val left = (canvasWidth - frameWidth) / 2
            val top = (canvasHeight - frameHeight) / 2

            // Semi-transparent overlay
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(frameWidth, frameHeight),
                topLeft = Offset(left, top)
            )

            // Viewfinder corners
            val cornerLength = 40f
            val strokeWidth = 4f
            val blue = SmartShopColors.ElectricBlue

            // Top-left
            drawLine(blue, Offset(left, top + cornerLength), Offset(left, top), strokeWidth, cap = StrokeCap.Round)
            drawLine(blue, Offset(left, top), Offset(left + cornerLength, top), strokeWidth, cap = StrokeCap.Round)
            // Top-right
            drawLine(blue, Offset(left + frameWidth - cornerLength, top), Offset(left + frameWidth, top), strokeWidth, cap = StrokeCap.Round)
            drawLine(blue, Offset(left + frameWidth, top), Offset(left + frameWidth, top + cornerLength), strokeWidth, cap = StrokeCap.Round)
            // Bottom-left
            drawLine(blue, Offset(left, top + frameHeight - cornerLength), Offset(left, top + frameHeight), strokeWidth, cap = StrokeCap.Round)
            drawLine(blue, Offset(left, top + frameHeight), Offset(left + cornerLength, top + frameHeight), strokeWidth, cap = StrokeCap.Round)
            // Bottom-right
            drawLine(blue, Offset(left + frameWidth - cornerLength, top + frameHeight), Offset(left + frameWidth, top + frameHeight), strokeWidth, cap = StrokeCap.Round)
            drawLine(blue, Offset(left + frameWidth, top + frameHeight - cornerLength), Offset(left + frameWidth, top + frameHeight), strokeWidth, cap = StrokeCap.Round)
        }

        // Product info card
        if (productName != null) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .padding(horizontal = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SmartShopColors.CardBackground.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        productName,
                        style = MaterialTheme.typography.titleLarge,
                        color = SmartShopColors.TextPrimary
                    )
                    Text(
                        productPrice ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SmartShopColors.ElectricBlue
                    )
                }
            }
        }

        // Product not found card
        if (isProductNotFound && barcode.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
                    .padding(horizontal = 32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SmartShopColors.CardBackground.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Product Not Found",
                        style = MaterialTheme.typography.titleMedium,
                        color = SmartShopColors.ErrorRed
                    )
                    Text(
                        barcode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SmartShopColors.TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onAddProductClick) {
                        Text("Add Product")
                    }
                }
            }
        }

        // Circular timer
        if (isTimerActive) {
            CircularRescanTimer(
                progress = timerProgress,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
            )
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Torch button
            FloatingActionButton(
                onClick = onTorchToggle,
                containerColor = SmartShopColors.SurfaceVariant
            ) {
                Icon(
                    if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = SmartShopColors.ElectricBlue
                )
            }

            // Checkout button
            if (cartItemCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onCheckoutClick,
                    containerColor = SmartShopColors.ElectricBlue,
                    contentColor = SmartShopColors.Black
                ) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checkout ($cartItemCount)")
                }
            }
        }
    }
}

@Composable
private fun CircularRescanTimer(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 4000),
        label = "timer"
    )

    Canvas(
        modifier = modifier
            .size(60.dp)
            .graphicsLayer { alpha = 0.9f }
    ) {
        val stroke = 4.dp.toPx()

        // Background circle
        drawCircle(
            color = SmartShopColors.ElectricBlue.copy(alpha = 0.2f),
            style = Stroke(stroke)
        )

        // Progress arc
        drawArc(
            color = SmartShopColors.ElectricBlue,
            startAngle = -90f,
            sweepAngle = 360f * animatedProgress,
            useCenter = false,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}