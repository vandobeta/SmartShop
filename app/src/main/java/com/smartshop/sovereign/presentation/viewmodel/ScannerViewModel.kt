package com.smartshop.sovereign.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartshop.sovereign.data.local.datastore.SettingsDataStore
import com.smartshop.sovereign.domain.model.CartItem
import com.smartshop.sovereign.domain.model.Product
import com.smartshop.sovereign.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Scanner Screen UI State
 */
data class ScannerUiState(
    val isScanning: Boolean = true,
    val scannedProduct: Product? = null,
    val isProductNotFound: Boolean = false,
    val lastBarcode: String = "",
    val isTimerActive: Boolean = false,
    val timerProgress: Float = 1f,
    val isLowLight: Boolean = false,
    val cartItems: List<CartItem> = emptyList(),
    val showCheckout: Boolean = false,
    val isTorchOn: Boolean = false
)

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val scanLookupUseCase: ScanLookupUseCase,
    private val addProductUseCase: AddProductUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    /**
     * Process scanned barcode
     */
    fun onBarcodeScanned(barcode: String) {
        viewModelScope.launch {
            // Prevent duplicate scans during timer
            if (_uiState.value.isTimerActive) return@launch

            val product = scanLookupUseCase(barcode)
            if (product != null) {
                // Add to cart or increment
                addToCart(product)
                _uiState.update {
                    it.copy(
                        scannedProduct = product,
                        isProductNotFound = false,
                        lastBarcode = barcode,
                        isTimerActive = true
                    )
                }
                // Start 4-second timer
                startRescanTimer()
            } else {
                _uiState.update {
                    it.copy(
                        isProductNotFound = true,
                        lastBarcode = barcode
                    )
                }
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
            _uiState.update { it.copy(isTimerActive = false) }
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