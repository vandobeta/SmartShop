package com.smartshop.sovereign.presentation.ui.screen.checkout

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartshop.sovereign.domain.model.CartItem
import com.smartshop.sovereign.presentation.ui.theme.SmartShopColors
import com.smartshop.sovereign.presentation.viewmodel.CheckoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    cartItems: List<CartItem>,
    viewModel: CheckoutViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {},
    onSaleComplete: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(cartItems) {
        viewModel.setCartItems(cartItems)
    }

    LaunchedEffect(uiState.saleComplete) {
        if (uiState.saleComplete) {
            onSaleComplete(uiState.receiptText)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", color = SmartShopColors.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = SmartShopColors.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SmartShopColors.DarkBackground
                )
            )
        },
        containerColor = SmartShopColors.DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Cart items
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(
                        item = item,
                        onRemove = { /* TODO: Remove from cart */ }
                    )
                }
            }

            Divider(color = SmartShopColors.SurfaceVariant)

            // Totals
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                TotalRow("Subtotal", uiState.subtotal)
                TotalRow("Tax (Incl.)", uiState.tax)
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = SmartShopColors.SurfaceVariant
                )
                TotalRow("TOTAL", uiState.total, isTotal = true)
            }

            // Complete Sale Button
            Button(
                onClick = { viewModel.completeSale() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmartShopColors.ElectricBlue,
                    contentColor = SmartShopColors.Black
                )
            ) {
                if (uiState.isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SmartShopColors.ElectricBlue
                    )
                } else {
                    Icon(Icons.Default.ShoppingCart, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Sale", style = MaterialTheme.typography.titleMedium)
                }
            }

            // Error
            if (uiState.error.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    uiState.error,
                    color = SmartShopColors.ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SmartShopColors.CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = SmartShopColors.TextPrimary
                )
                Text(
                    "UGX ${item.product.price / 100} × ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartShopColors.TextSecondary
                )
            }
            Text(
                "UGX ${item.totalPrice / 100}",
                style = MaterialTheme.typography.titleMedium,
                color = SmartShopColors.ElectricBlue
            )
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = SmartShopColors.ErrorRed
                )
            }
        }
    }
}

@Composable
private fun TotalRow(
    label: String,
    amount: Long,
    isTotal: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            color = if (isTotal) SmartShopColors.ElectricBlue else SmartShopColors.TextPrimary
        )
        Text(
            "UGX ${String.format("%,d", amount / 100)}",
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            color = if (isTotal) SmartShopColors.ElectricBlue else SmartShopColors.TextPrimary
        )
    }
}