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
import com.smartshop.sovereign.presentation.ui.screen.checkout.CheckoutScreen
import com.smartshop.sovereign.presentation.ui.screen.scanner.ScannerScreen
import com.smartshop.sovereign.presentation.ui.theme.SmartShopColors
import com.smartshop.sovereign.presentation.ui.theme.SmartShopTheme
import com.smartshop.sovereign.presentation.viewmodel.ScannerViewModel
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

@Composable
fun SmartShopApp() {
    val navController = rememberNavController()
    val scannerViewModel: ScannerViewModel = hiltViewModel()
    val scannerState by scannerViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "scanner"
    ) {
        composable("scanner") {
            ScannerScreen(
                viewModel = scannerViewModel,
                onNavigateToCheckout = {
                    navController.navigate("checkout")
                }
            )
        }

        composable("checkout") {
            CheckoutScreen(
                cartItems = scannerState.cartItems,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onSaleComplete = { receiptText ->
                    // Show receipt or share
                    scannerViewModel.clearCart()
                    navController.popBackStack()
                }
            )
        }
    }
}