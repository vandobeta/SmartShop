package com.smartshop.sovereign.presentation.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.smartshop.sovereign.presentation.ui.screen.addproduct.AddProductScreen
import com.smartshop.sovereign.presentation.ui.screen.checkout.CheckoutScreen
import com.smartshop.sovereign.presentation.ui.screen.scanner.ScannerScreen
import com.smartshop.sovereign.presentation.ui.screen.setup.SetupScreen
import com.smartshop.sovereign.presentation.ui.theme.SmartShopColors
import com.smartshop.sovereign.presentation.ui.theme.SmartShopTheme
import com.smartshop.sovereign.presentation.viewmodel.ScannerViewModel
import com.smartshop.sovereign.presentation.viewmodel.SetupViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SmartShopTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SmartShopColors.DarkBackground
                ) {
                    SmartShopApp()
                }
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Scanner : Screen("scanner")
    object Checkout : Screen("checkout")
    object AddProduct : Screen("add_product/{barcode}") {
        fun createRoute(barcode: String) = "add_product/$barcode"
    }
}

@Composable
fun SmartShopApp() {
    val navController = rememberNavController()
    val setupViewModel: SetupViewModel = hiltViewModel()
    val setupState by setupViewModel.uiState.collectAsState()
    
    val startDestination = if (setupState.isComplete) Screen.Scanner.route else Screen.Setup.route

    NavHost(
        navController = navController,
        startDestination = startDestination
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
            val scannerViewModel: ScannerViewModel = hiltViewModel()
            val scannerState by scannerViewModel.uiState.collectAsState()

            ScannerScreen(
                viewModel = scannerViewModel,
                onNavigateToCheckout = {
                    navController.navigate(Screen.Checkout.route)
                },
                onNavigateToAddProduct = { barcode ->
                    navController.navigate(Screen.AddProduct.createRoute(barcode))
                }
            )
        }

        composable(Screen.Checkout.route) {
            val scannerViewModel: ScannerViewModel = hiltViewModel()
            val scannerState by scannerViewModel.uiState.collectAsState()

            CheckoutScreen(
                cartItems = scannerState.cartItems,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaleComplete = { _ ->
                    scannerViewModel.clearCart()
                    navController.popBackStack(Screen.Scanner.route, inclusive = false)
                }
            )
        }

        composable(Screen.AddProduct.route) { backStackEntry ->
            val barcode = backStackEntry.arguments?.getString("barcode") ?: ""
            
            AddProductScreen(
                prefillBarcode = barcode,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProductAdded = {
                    navController.popBackStack()
                }
            )
        }
    }
}