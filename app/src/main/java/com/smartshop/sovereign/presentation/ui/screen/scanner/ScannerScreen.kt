package com.smartshop.sovereign.presentation.ui.screen.scanner

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.smartshop.sovereign.presentation.ui.theme.SmartShopColors
import com.smartshop.sovereign.presentation.viewmodel.ScannerViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
    onNavigateToCheckout: () -> Unit = {},
    onNavigateToAddProduct: (String) -> Unit = {},
    onNavigateToDashboard: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraPermission = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermission.status.isGranted) {
            cameraPermission.launchPermissionRequest()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SmartShopColors.Black)
    ) {
        if (cameraPermission.status.isGranted) {
            CameraPreview(
                isTorchOn = uiState.isTorchOn,
                onBarcodeScanned = { barcode ->
                    viewModel.onBarcodeScanned(barcode)
                }
            )

            ScannerOverlay(
                productName = uiState.scannedProduct?.name,
                productPrice = uiState.scannedProduct?.price,
                productStock = uiState.scannedProduct?.quantity ?: 0,
                isProductNotFound = uiState.isProductNotFound,
                barcode = uiState.lastBarcode,
                cartItemCount = uiState.cartItems.size,
                isTorchOn = uiState.isTorchOn,
                onTorchToggle = { viewModel.toggleTorch() },
                onCheckoutClick = onNavigateToCheckout,
                onAddProductClick = { onNavigateToAddProduct(uiState.lastBarcode) },
                onRetryClick = { viewModel.clearLastScan() },
                showAdminAuth = uiState.showAdminAuth,
                adminAuthError = uiState.adminAuthError,
                onAdminAuthSuccess = { viewModel.onAdminAuthSuccess() },
                onAdminAuthFailed = { viewModel.onAdminAuthFailed(it) },
                onCancelAuth = { viewModel.cancelAdminAuth() },
                pendingBarcode = uiState.pendingBarcode
            )
        } else {
            PermissionDeniedContent(
                onRequestPermission = { cameraPermission.launchPermissionRequest() }
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = SmartShopColors.ElectricBlue
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall,
            color = SmartShopColors.TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            "SmartShop needs camera access to scan barcodes.",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartShopColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = SmartShopColors.ElectricBlue,
                contentColor = SmartShopColors.Black
            )
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Grant Camera Permission")
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
    var lastScannedBarcode by remember { mutableStateOf("") }
    var lastScanTime by remember { mutableStateOf(0L) }

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
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastScanTime > 500) {
                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                barcode.rawValue?.let { value ->
                                                    if (value != lastScannedBarcode) {
                                                        lastScannedBarcode = value
                                                        lastScanTime = currentTime
                                                        onBarcodeScanned(value)
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
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
                        lifecycleOwner, cameraSelector, preview, imageAnalysis
                    )
                    camera.cameraControl.enableTorch(isTorchOn)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, context.mainExecutor)
        }
    )
}

@Composable
private fun ScannerOverlay(
    productName: String?,
    productPrice: Long?,
    productStock: Int,
    isProductNotFound: Boolean,
    barcode: String,
    cartItemCount: Int,
    isTorchOn: Boolean,
    onTorchToggle: () -> Unit,
    onCheckoutClick: () -> Unit,
    onAddProductClick: () -> Unit,
    onRetryClick: () -> Unit,
    showAdminAuth: Boolean,
    adminAuthError: String,
    onAdminAuthSuccess: () -> Unit,
    onAdminAuthFailed: (String) -> Unit,
    onCancelAuth: () -> Unit,
    pendingBarcode: String
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Semi-transparent overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
        )

        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (cartItemCount > 0) {
                Surface(
                    color = SmartShopColors.ElectricBlue,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "🛒 $cartItemCount",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = SmartShopColors.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }
            
            FilledIconButton(
                onClick = onTorchToggle,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = SmartShopColors.SurfaceVariant
                )
            ) {
                Icon(
                    if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Torch",
                    tint = SmartShopColors.ElectricBlue
                )
            }
        }

        // Product found card
        AnimatedVisibility(
            visible = productName != null && !isProductNotFound,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SmartShopColors.CardBackground.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SmartShopColors.SuccessGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        productName ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = SmartShopColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "UGX ${((productPrice ?: 0) / 100).toLong().let { String.format("%,d", it) }}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SmartShopColors.ElectricBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Stock: $productStock",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (productStock < 5) SmartShopColors.ErrorRed else SmartShopColors.TextSecondary
                    )
                }
            }
        }

        // Product not found + Admin Auth
        AnimatedVisibility(
            visible = isProductNotFound && barcode.isNotEmpty() && !showAdminAuth,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Card(
                modifier = Modifier.padding(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = SmartShopColors.CardBackground.copy(alpha = 0.95f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = SmartShopColors.ErrorRed,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = onRetryClick) {
                            Text("Retry")
                        }
                        Button(
                            onClick = onAddProductClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SmartShopColors.ElectricBlue,
                                contentColor = SmartShopColors.Black
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Product")
                        }
                    }
                }
            }
        }

        // Admin Authentication Dialog
        if (showAdminAuth) {
            AdminAuthDialog(
                barcode = pendingBarcode,
                error = adminAuthError,
                onSuccess = onAdminAuthSuccess,
                onFailed = onAdminAuthFailed,
                onCancel = onCancelAuth,
                onAddProduct = onAddProductClick
            )
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (cartItemCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onCheckoutClick,
                    containerColor = SmartShopColors.ElectricBlue,
                    contentColor = SmartShopColors.Black
                ) {
                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checkout ($cartItemCount)", fontWeight = FontWeight.Bold)
                }
            } else {
                Surface(
                    color = SmartShopColors.Overlay,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        "Point camera at barcode to scan",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        color = SmartShopColors.TextMuted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdminAuthDialog(
    barcode: String,
    error: String,
    onSuccess: () -> Unit,
    onFailed: (String) -> Unit,
    onCancel: () -> Unit,
    onAddProduct: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = SmartShopColors.CardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Lock,
                contentDescription = null,
                tint = SmartShopColors.ElectricBlue,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Admin Authentication",
                style = MaterialTheme.typography.titleLarge,
                color = SmartShopColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                "Enter admin PIN to add new product",
                style = MaterialTheme.typography.bodyMedium,
                color = SmartShopColors.TextSecondary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Barcode: $barcode",
                style = MaterialTheme.typography.bodySmall,
                color = SmartShopColors.TextMuted
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = passcode,
                onValueChange = { if (it.length <= 6) passcode = it },
                label = { Text("Admin PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SmartShopColors.ElectricBlue,
                    focusedLabelColor = SmartShopColors.ElectricBlue
                )
            )
            
            if (error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    error,
                    color = SmartShopColors.ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        if (passcode.length >= 4) {
                            onSuccess()
                            onAddProduct()
                        } else {
                            onFailed("PIN must be at least 4 digits")
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmartShopColors.ElectricBlue,
                        contentColor = SmartShopColors.Black
                    )
                ) {
                    Text("Verify & Add")
                }
            }
        }
    }
}