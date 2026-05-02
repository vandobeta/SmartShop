package com.smartshop.sovereign.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartshop.sovereign.data.local.dao.ProductDao
import com.smartshop.sovereign.data.local.dao.SaleDao
import com.smartshop.sovereign.data.local.datastore.SettingsDataStore
import com.smartshop.sovereign.domain.model.CartItem
import com.smartshop.sovereign.domain.model.ReceiptData
import com.smartshop.sovereign.domain.model.ReceiptItem
import com.smartshop.sovereign.domain.usecase.CalculateTaxUseCase
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

data class CheckoutUiState(
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: Long = 0,
    val tax: Long = 0,
    val total: Long = 0,
    val isProcessing: Boolean = false,
    val receiptText: String = "",
    val saleComplete: Boolean = false,
    val error: String = "",
    val shopName: String = "",
    val shopTel: String = "",
    val cashierName: String = "",
    val taxRate: Long = 1800L
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val calculateTaxUseCase: CalculateTaxUseCase,
    private val saleDao: SaleDao,
    private val productDao: ProductDao,
    private val settingsDataStore: SettingsDataStore,
    private val receiptGenerator: ReceiptGenerator,
    private val auditLogger: AuditLogger,
    private val sensoryManager: SovereignSensoryManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val receiptDateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US)

    init {
        loadShopInfo()
    }

    private fun loadShopInfo() {
        viewModelScope.launch {
            val shopName = settingsDataStore.shopName.first()
            val shopTel = settingsDataStore.shopTel.first()
            val taxRate = settingsDataStore.taxRate.first()
            
            _uiState.update {
                it.copy(
                    shopName = shopName,
                    shopTel = shopTel,
                    cashierName = "Admin", // Default
                    taxRate = taxRate
                )
            }
        }
    }

    fun setCartItems(items: List<CartItem>) {
        _uiState.update { state ->
            val (subtotal, tax, total) = calculateTaxUseCase(items, state.taxRate)
            state.copy(cartItems = items, subtotal = subtotal, tax = tax, total = total)
        }
    }

    fun updateQuantity(barcode: String, newQuantity: Int) {
        _uiState.update { state ->
            if (newQuantity <= 0) {
                val newItems = state.cartItems.filter { it.product.barcode != barcode }
                val (subtotal, tax, total) = calculateTaxUseCase(newItems, state.taxRate)
                state.copy(cartItems = newItems, subtotal = subtotal, tax = tax, total = total)
            } else {
                val newItems = state.cartItems.map {
                    if (it.product.barcode == barcode) it.copy(quantity = newQuantity)
                    else it
                }
                val (subtotal, tax, total) = calculateTaxUseCase(newItems, state.taxRate)
                state.copy(cartItems = newItems, subtotal = subtotal, tax = tax, total = total)
            }
        }
    }

    fun removeItem(barcode: String) {
        _uiState.update { state ->
            val newItems = state.cartItems.filter { it.product.barcode != barcode }
            val (subtotal, tax, total) = calculateTaxUseCase(newItems, state.taxRate)
            state.copy(cartItems = newItems, subtotal = subtotal, tax = tax, total = total)
        }
    }

    fun completeSale() {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }

            try {
                val state = _uiState.value
                if (state.cartItems.isEmpty()) {
                    _uiState.update { it.copy(isProcessing = false, error = "Cart is empty") }
                    return@launch
                }

                val (subtotal, tax, total) = calculateTaxUseCase(state.cartItems, state.taxRate)

                // Reduce stock for each item
                state.cartItems.forEach { item ->
                    val product = item.product
                    val newQuantity = product.quantity - item.quantity
                    if (newQuantity >= 0) {
                        productDao.decrementStock(product.barcode, item.quantity)
                    }
                }

                // Save sale record
                val saleId = saleDao.insertSale(
                    com.smartshop.sovereign.data.local.entity.SaleEntity(
                        itemsJson = state.cartItems.map { "${it.product.barcode}:${it.quantity}:${it.product.price}" }.joinToString(";"),
                        subtotal = subtotal,
                        tax = tax,
                        total = total,
                        cashierName = state.cashierName
                    )
                )

                // Generate receipt
                val receiptData = ReceiptData(
                    shopName = state.shopName,
                    shopTel = state.shopTel,
                    cashierName = state.cashierName,
                    timestamp = receiptDateFormat.format(Date()),
                    transactionId = "TXN-${String.format("%06d", saleId)}",
                    items = state.cartItems.map {
                        ReceiptItem(it.product.name, it.quantity, it.product.price, it.totalPrice)
                    },
                    subtotal = subtotal,
                    tax = tax,
                    total = total,
                    nicheType = "Shop"
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

    fun cancelSale() {
        _uiState.update { CheckoutUiState(shopName = it.shopName, shopTel = it.shopTel, cashierName = it.cashierName) }
    }

    fun clearSale() {
        _uiState.update { CheckoutUiState(shopName = it.shopName, shopTel = it.shopTel, cashierName = it.cashierName) }
    }
}