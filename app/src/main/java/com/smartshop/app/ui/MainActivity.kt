package com.smartshop.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartshop.app.SmartShopApplication
import com.smartshop.app.data.repository.InventoryRepository
import com.smartshop.app.domain.model.CartItem
import com.smartshop.app.domain.model.InventoryItem
import com.smartshop.app.ui.navigation.Screen
import com.smartshop.app.ui.screens.cart.CartScreen
import com.smartshop.app.ui.screens.checkout.CheckoutScreen
import com.smartshop.app.ui.screens.manager.ManagerDashboard
import com.smartshop.app.ui.screens.scanner.ScannerScreen
import com.smartshop.app.ui.screens.settings.PinDialog
import com.smartshop.app.ui.screens.settings.SettingsScreen
import com.smartshop.app.ui.screens.setup.SetupScreen
import com.smartshop.app.ui.theme.SmartShopTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = SmartShopApplication.get()
        
        setContent {
            SmartShopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val cartViewModel = remember { CartViewModel(app.inventoryRepository) }
                    val managerViewModel = remember { ManagerViewModel(app.inventoryRepository) }
                    
                    NavHost(
                        navController = navController,
                        startDestination = if (app.authManager.isPinSet()) Screen.Scanner.route else Screen.Setup.route
                    ) {
                        composable(Screen.Setup.route) {
                            SetupScreen(
                                onSetupComplete = {
                                    navController.navigate(Screen.Scanner.route) {
                                        popUpTo(Screen.Setup.route) { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        composable(Screen.Scanner.route) {
                            val cartItems by cartViewModel.cartItems.collectAsState()
                            
                            ScannerScreen(
                                cartItems = cartItems,
                                onAddToCart = { item, qty -> cartViewModel.addToCart(item, qty) },
                                onNavigateToCart = {
                                    navController.navigate(Screen.Cart.route)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Screen.Settings.route)
                                }
                            )
                        }
                        
                        composable(Screen.Cart.route) {
                            val cartItems by cartViewModel.cartItems.collectAsState()
                            
                            CartScreen(
                                cartItems = cartItems,
                                onUpdateQuantity = { item, delta -> cartViewModel.updateQuantity(item, delta) },
                                onRemove = { item -> cartViewModel.removeFromCart(item) },
                                onCheckout = {
                                    navController.navigate(Screen.Checkout.route)
                                },
                                onScanMore = {
                                    navController.popBackStack()
                                },
                                onClearCart = { cartViewModel.clearCart() },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(Screen.Checkout.route) {
                            val cartItems by cartViewModel.cartItems.collectAsState()
                            val total = cartItems.sumOf { it.totalPrice }
                            
                            CheckoutScreen(
                                cartItems = cartItems,
                                total = total,
                                onConfirmPayment = {
                                    cartViewModel.checkout()
                                },
                                onNewSale = {
                                    navController.navigate(Screen.Scanner.route) {
                                        popUpTo(Screen.Scanner.route) { inclusive = true }
                                    }
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        composable(Screen.Settings.route) {
                            var showPinDialog by remember { mutableStateOf(false) }
                            var pinError by remember { mutableStateOf<String?>(null) }
                            
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToManager = {
                                    navController.navigate(Screen.ManagerDashboard.route)
                                },
                                onShowPinDialog = { showPinDialog = true }
                            )
                            
                            if (showPinDialog) {
                                PinDialog(
                                    onDismiss = { showPinDialog = false },
                                    onVerify = {
                                        showPinDialog = false
                                        navController.navigate(Screen.ManagerDashboard.route)
                                    },
                                    onError = { error -> pinError = error }
                                )
                            }
                        }
                        
                        composable(Screen.ManagerDashboard.route) {
                            val totalSales by managerViewModel.totalSales.collectAsState()
                            val todaySales by managerViewModel.todaySales.collectAsState()
                            val inventoryWorth by managerViewModel.inventoryWorth.collectAsState()
                            
                            ManagerDashboard(
                                totalSales = totalSales,
                                todaySales = todaySales,
                                inventoryWorth = inventoryWorth,
                                onLogout = {
                                    navController.popBackStack(Screen.Scanner.route, false)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

class CartViewModel(
    private val repository: InventoryRepository
) : ViewModel() {
    
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems
    
    fun addToCart(item: InventoryItem, quantity: Int = 1) {
        val currentItems = _cartItems.value.toMutableList()
        val existingIndex = currentItems.indexOfFirst { it.inventoryItem.id == item.id }
        
        if (existingIndex >= 0) {
            val existing = currentItems[existingIndex]
            if (existing.quantity + quantity <= item.quantity) {
                currentItems[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
            }
        } else {
            currentItems.add(CartItem(item, quantity))
        _cartItems.value = currentItems
        }
        
        _cartItems.value = currentItems
    }
    
    fun updateQuantity(item: CartItem, delta: Int) {
        val currentItems = _cartItems.value.toMutableList()
        val index = currentItems.indexOfFirst { it.inventoryItem.id == item.inventoryItem.id }
        
        if (index >= 0) {
            val current = currentItems[index]
            val newQuantity = current.quantity + delta
            
            if (newQuantity <= 0) {
                currentItems.removeAt(index)
            } else if (newQuantity <= item.inventoryItem.quantity) {
                currentItems[index] = current.copy(quantity = newQuantity)
            }
        }
        
        _cartItems.value = currentItems
    }
    
    fun removeFromCart(item: CartItem) {
        _cartItems.value = _cartItems.value.filter { it.inventoryItem.id != item.inventoryItem.id }
    }
    
    fun clearCart() {
        _cartItems.value = emptyList()
    }
    
    fun checkout() {
        viewModelScope.launch {
            val items = _cartItems.value
            val total = items.sumOf { it.totalPrice }
            
            // Save transaction
            repository.saveTransaction(items, total)
            
            // Deduct inventory
            items.forEach { cartItem ->
                repository.deductQuantity(cartItem.inventoryItem.id, cartItem.quantity)
            }
            
            // Clear cart
            _cartItems.value = emptyList()
        }
    }
}

class ManagerViewModel(
    private val repository: InventoryRepository
) : ViewModel() {
    
    private val _totalSales = MutableStateFlow(0)
    val totalSales: StateFlow<Int> = _totalSales
    
    private val _todaySales = MutableStateFlow(0)
    val todaySales: StateFlow<Int> = _todaySales
    
    private val _inventoryWorth = MutableStateFlow(0)
    val inventoryWorth: StateFlow<Int> = _inventoryWorth
    
    private val _items = MutableStateFlow<List<InventoryItem>>(emptyList())
    val items: StateFlow<List<InventoryItem>> = _items
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _totalSales.value = repository.getTotalSales()
            _todaySales.value = repository.getTodaySales()
            _inventoryWorth.value = repository.getInventoryWorth()
            
            repository.getAllItems().collect { itemList ->
                _items.value = itemList
            }
        }
    }
}