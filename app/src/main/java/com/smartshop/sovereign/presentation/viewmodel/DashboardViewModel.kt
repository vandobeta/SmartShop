package com.smartshop.sovereign.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartshop.sovereign.data.local.dao.ProductDao
import com.smartshop.sovereign.data.local.dao.SaleDao
import com.smartshop.sovereign.data.local.datastore.SettingsDataStore
import com.smartshop.sovereign.domain.model.Product
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalProducts: Int = 0,
    val lowStockProducts: List<Product> = emptyList(),
    val topSellingProducts: List<TopProduct> = emptyList(),
    val todaySales: Long = 0,
    val totalRevenue: Long = 0,
    val categoryStock: List<CategoryStock> = emptyList(),
    val isLoading: Boolean = true,
    val error: String = ""
)

data class TopProduct(
    val name: String,
    val totalSold: Int,
    val revenue: Long
)

data class CategoryStock(
    val category: String,
    val count: Int,
    val percentage: Float
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val productDao: ProductDao,
    private val saleDao: SaleDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Get all products
                val products = mutableListOf<Product>()
                productDao.getAllProducts().collect { list ->
                    products.clear()
                    products.addAll(list.map { entity ->
                        Product(
                            barcode = entity.barcode,
                            name = entity.name,
                            category = entity.category,
                            price = entity.price,
                            quantity = entity.quantity,
                            costPrice = entity.costPrice
                        )
                    })
                    
                    // Calculate low stock (less than 5 items)
                    val lowStock = products.filter { it.quantity < 5 }
                    
                    // Calculate category stock
                    val categoryMap = products.groupBy { it.category }
                    val totalProducts = products.size
                    val categoryStock = categoryMap.map { (cat, prods) ->
                        CategoryStock(
                            category = cat,
                            count = prods.size,
                            percentage = if (totalProducts > 0) prods.size.toFloat() / totalProducts else 0f
                        )
                    }.sortedByDescending { it.count }

                    _uiState.update {
                        it.copy(
                            totalProducts = totalProducts,
                            lowStockProducts = lowStock,
                            categoryStock = categoryStock,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load dashboard")
                }
            }
        }

        viewModelScope.launch {
            try {
                // Get total revenue
                val revenue = saleDao.getTotalRevenue() ?: 0L
                _uiState.update { it.copy(totalRevenue = revenue) }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun updateStock(product: Product, newQuantity: Int) {
        viewModelScope.launch {
            try {
                val entity = com.smartshop.sovereign.data.local.entity.ProductEntity(
                    barcode = product.barcode,
                    name = product.name,
                    category = product.category,
                    price = product.price,
                    quantity = newQuantity,
                    costPrice = product.costPrice
                )
                productDao.updateProduct(entity)
                loadDashboard() // Refresh
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to update stock") }
            }
        }
    }
}