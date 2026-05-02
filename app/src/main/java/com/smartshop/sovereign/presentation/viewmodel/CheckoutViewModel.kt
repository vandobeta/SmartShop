package com.smartshop.sovereign.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartshop.sovereign.data.local.datastore.SettingsDataStore
import com.smartshop.sovereign.domain.model.CartItem
import com.smartshop.sovereign.domain.model.ReceiptData
import com.smartshop.sovereign.domain.model.ReceiptItem
import com.smartshop.sovereign.domain.usecase.CalculateTaxUseCase
import com.smartshop.sovereign.domain.usecase.CompleteSaleUseCase
import com.smartshop.sovereign.util.AuditLogger
import com.smartshop.sovereign.util.ReceiptGenerator
import com.smartshop.sovereign.util.SovereignSensoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

/**
 * Checkout Screen UI State
 */
data class CheckoutUiState(
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: Long = 0,
    val tax: Long = 0,
    val total: Long = 0,
    val isProcessing: Boolean = false,
    val receiptText: String = "",
    val saleComplete: Boolean = false,
    val error: String = ""
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val calculateTaxUseCase: CalculateTaxUseCase,
    private val completeSaleUseCase: CompleteSaleUseCase,
    private val settingsDataStore: SettingsDataStore,
    private val receiptGenerator: ReceiptGenerator,
    private val auditLogger: AuditLogger,
    private val sensoryManager: SovereignSensoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun setCartItems(items: List<CartItem>) {
        viewModelScope.launch {
            val taxRate = settingsDataStore.taxRate.first()
            val (subtotal, tax, total) = calculateTaxUseCase(items, taxRate)
            _uiState.update {
                it.copy(cartItems = items, subtotal = subtotal, tax = tax, total = total)
            }
        }
    }

    fun completeSale() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            try {
                val state = _uiState.value
                val (subtotal, tax, total) = calculateTaxUseCase(state.cartItems, settingsDataStore.taxRate.first())

                // Complete sale
                completeSaleUseCase(
                    items = state.cartItems,
                    subtotal = subtotal,
                    tax = tax,
                    total = total,
                    cashierName = "CASHIER" // TODO: Get from settings
                )

                // Generate receipt
                val receiptData = ReceiptData(
                    shopName = settingsDataStore.shopName.first(),
                    shopTel = settingsDataStore.shopTel.first(),
                    cashierName = "CASHIER",
                    timestamp = dateFormat.format(Date()),
                    transactionId = UUID.randomUUID().toString().take(8).uppercase(),
                    items = state.cartItems.map {
                        ReceiptItem(it.product.name, it.quantity, it.product.price, it.totalPrice)
                    },
                    subtotal = subtotal,
                    tax = tax,
                    total = total,
                    nicheType = settingsDataStore.shopNiche.first()
                )

                val receiptText = receiptGenerator.generate(receiptData)

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        saleComplete = true,
                        receiptText = receiptText
                    )
                }

                sensoryManager.onScanSuccess()
                auditLogger.onSaleComplete(total)

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isProcessing = false, error = e.message ?: "Sale failed")
                }
                sensoryManager.onScanError()
                auditLogger.onSaleFailed(e.message ?: "Unknown")
            }
        }
    }

    fun clearSale() {
        _uiState.update { CheckoutUiState() }
    }
}