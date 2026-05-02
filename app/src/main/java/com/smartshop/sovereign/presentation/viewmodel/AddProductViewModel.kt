package com.smartshop.sovereign.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartshop.sovereign.domain.usecase.AddProductUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddProductUiState(
    val barcode: String = "",
    val name: String = "",
    val category: String = "",
    val price: String = "",
    val quantity: String = "",
    val costPrice: String = "",
    val categories: List<String> = listOf(
        "Groceries", "Beverages", "Snacks", "Household", 
        "Personal Care", "Medicines", "Electronics", "Clothing", "Other"
    ),
    val isLoading: Boolean = false,
    val isComplete: Boolean = false,
    val error: String = ""
) {
    val isValid: Boolean
        get() = barcode.isNotBlank() && 
                name.isNotBlank() && 
                category.isNotBlank() && 
                price.toLongOrNull() != null && 
                (quantity.toIntOrNull() ?: 0) > 0
}

@HiltViewModel
class AddProductViewModel @Inject constructor(
    private val addProductUseCase: AddProductUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProductUiState())
    val uiState: StateFlow<AddProductUiState> = _uiState.asStateFlow()

    fun setBarcode(barcode: String) {
        _uiState.update { it.copy(barcode = barcode, error = "") }
    }

    fun setName(name: String) {
        _uiState.update { it.copy(name = name, error = "") }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(category = category, error = "") }
    }

    fun setPrice(price: String) {
        _uiState.update { it.copy(price = price, error = "") }
    }

    fun setQuantity(quantity: String) {
        _uiState.update { it.copy(quantity = quantity, error = "") }
    }

    fun setCostPrice(costPrice: String) {
        _uiState.update { it.copy(costPrice = costPrice, error = "") }
    }

    fun addProduct() {
        val state = _uiState.value
        if (!state.isValid) {
            _uiState.update { it.copy(error = "Please fill all required fields correctly") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = "") }

            try {
                val priceCents = (state.price.toLongOrNull() ?: 0L) * 100
                val costCents = if (state.costPrice.isNotEmpty()) {
                    (state.costPrice.toLongOrNull() ?: 0L) * 100
                } else 0L

                addProductUseCase(
                    barcode = state.barcode,
                    name = state.name,
                    category = state.category,
                    price = priceCents,
                    quantity = state.quantity.toIntOrNull() ?: 1,
                    costPrice = costCents
                )

                _uiState.update { it.copy(isLoading = false, isComplete = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Failed to add product") }
            }
        }
    }
}