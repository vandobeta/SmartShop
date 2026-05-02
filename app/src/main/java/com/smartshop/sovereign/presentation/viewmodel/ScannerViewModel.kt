package com.smartshop.sovereign.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartshop.sovereign.data.local.datastore.SettingsDataStore
import com.smartshop.sovereign.domain.model.CartItem
import com.smartshop.sovereign.domain.model.Product
import com.smartshop.sovereign.domain.usecase.*
import com.smartshop.sovereign.util.AuditLogger
import com.smartshop.sovereign.util.SovereignSensoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ScannerUiState(
    val isScanning: Boolean = true,
    val scannedProduct: Product? = null,
    val isProductNotFound: Boolean = false,
    val lastBarcode: String = "",
    val isTimerActive: Boolean = false,
    val timerProgress: Float = 1f,
    val cartItems: List<CartItem> = emptyList(),
    val showCheckout: Boolean = false,
    val isTorchOn: Boolean = false,
    val isFirstLaunch: Boolean = true,
    val showAdminAuth: Boolean = false,
    val adminAuthError: String = "",
    val isAdminAuthenticated: Boolean = false,
    val pendingBarcode: String = ""
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scanLookupUseCase: ScanLookupUseCase,
    private val addProductUseCase: AddProductUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val sensoryManager: SovereignSensoryManager,
    private val auditLogger: AuditLogger
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    init {
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        viewModelScope.launch {
            settingsDataStore.isFirstLaunch.collect { isFirst ->
                _uiState.update { it.copy(isFirstLaunch = isFirst) }
            }
        }
    }

    fun onBarcodeScanned(barcode: String) {
        // Prevent duplicate scans
        if (_uiState.value.isTimerActive) return

        viewModelScope.launch {
            val product = scanLookupUseCase(barcode)
            if (product != null) {
                // Product found - add to cart and show details
                addToCart(product)
                _uiState.update {
                    it.copy(
                        scannedProduct = product,
                        isProductNotFound = false,
                        lastBarcode = barcode,
                        isTimerActive = true
                    )
                }
                // Success feedback: short beep + vibration
                sensoryManager.onScanSuccess()
                auditLogger.onScanSuccess(barcode)
                startRescanTimer()
            } else {
                // Product not found - trigger admin auth
                _uiState.update {
                    it.copy(
                        isProductNotFound = true,
                        lastBarcode = barcode,
                        showAdminAuth = true,
                        pendingBarcode = barcode,
                        scannedProduct = null
                    )
                }
                // Error feedback: long vibration
                sensoryManager.onScanError()
                auditLogger.onScanError(barcode)
            }
        }
    }

    private fun addToCart(product: Product) {
        _uiState.update { state ->
            val existing = state.cartItems.find { it.product.barcode == product.barcode }
            val newItems = if (existing != null) {
                state.cartItems.map {
                    if (it.product.barcode == product.barcode) it.copy(quantity = it.quantity + 1)
                    else it
                }
            } else {
                state.cartItems + CartItem(product, 1)
            }
            state.copy(cartItems = newItems, showCheckout = newItems.isNotEmpty())
        }
    }

    private fun startRescanTimer() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(4000)
            _uiState.update { it.copy(isTimerActive = false, scannedProduct = null) }
        }
    }

    fun onAdminAuthSuccess() {
        _uiState.update {
            it.copy(
                showAdminAuth = false,
                isAdminAuthenticated = true,
                adminAuthError = ""
            )
        }
    }

    fun onAdminAuthFailed(error: String) {
        _uiState.update {
            it.copy(
                adminAuthError = error,
                isAdminAuthenticated = false
            )
        }
    }

    fun cancelAdminAuth() {
        _uiState.update {
            it.copy(
                showAdminAuth = false,
                pendingBarcode = "",
                isProductNotFound = false,
                lastBarcode = "",
                adminAuthError = ""
            )
        }
    }

    fun clearLastScan() {
        _uiState.update {
            it.copy(
                lastBarcode = "",
                isProductNotFound = false,
                scannedProduct = null,
                isTimerActive = false,
                showAdminAuth = false
            )
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList(), showCheckout = false, scannedProduct = null) }
    }

    fun removeFromCart(barcode: String) {
        _uiState.update { state ->
            val newItems = state.cartItems.filter { it.product.barcode != barcode }
            state.copy(cartItems = newItems, showCheckout = newItems.isNotEmpty())
        }
    }

    fun toggleTorch() {
        _uiState.update { it.copy(isTorchOn = !it.isTorchOn) }
    }

    fun navigateToCheckout() {
        _uiState.update { it.copy(showCheckout = true) }
    }

    fun dismissCheckout() {
        _uiState.update { it.copy(showCheckout = false) }
    }
}