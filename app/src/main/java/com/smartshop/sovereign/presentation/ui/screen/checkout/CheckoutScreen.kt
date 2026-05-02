package com.smartshop.sovereign.presentation.ui.screen.checkout

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
                title = { 
                    Text(
                        "Checkout", 
                        color = SmartShopColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    ) 
                },
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
            // Cart items list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.cartItems) { item ->
                    CartItemCard(
                        item = item,
                        onQuantityChange = { newQty ->
                            viewModel.updateQuantity(item.product.barcode, newQty)
                        },
                        onRemove = {
                            viewModel.removeItem(item.product.barcode)
                        }
                    )
                }
            }

            // Totals
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SmartShopColors.CardBackground
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    TotalRow("Subtotal", uiState.subtotal)
                    Spacer(modifier = Modifier.height(8.dp))
                    TotalRow("VAT (18%)", uiState.tax)
                    Divider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = SmartShopColors.SurfaceVariant
                    )
                    TotalRow("TOTAL", uiState.total, isTotal = true)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel button
                OutlinedButton(
                    onClick = {
                        viewModel.cancelSale()
                        onNavigateBack()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SmartShopColors.ErrorRed
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel")
                }

                // Paid button
                Button(
                    onClick = { viewModel.completeSale() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isProcessing && uiState.cartItems.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmartShopColors.SuccessGreen,
                        contentColor = SmartShopColors.Black
                    )
                ) {
                    if (uiState.isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = SmartShopColors.SuccessGreen
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PAID", fontWeight = FontWeight.Bold)
                    }
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
private fun CartItemCard(
    item: CartItem,
    onQuantityChange: (Int) -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SmartShopColors.CardBackground
        ),
        shape = RoundedCornerShape(12.dp)
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
                    style = MaterialTheme.typography.titleMedium,
                    color = SmartShopColors.TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "UGX ${String.format("%,d", item.product.price / 100)} each",
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartShopColors.TextSecondary
                )
            }

            // Quantity selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = SmartShopColors.SurfaceVariant
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    "${item.quantity}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SmartShopColors.ElectricBlue,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                FilledIconButton(
                    onClick = { onQuantityChange(item.quantity + 1) },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = SmartShopColors.ElectricBlue
                    ),
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(16.dp),
                        tint = SmartShopColors.Black
                    )
                }
            }

            // Total and remove
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "UGX ${String.format("%,d", item.totalPrice / 100)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = SmartShopColors.ElectricBlue,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove",
                        tint = SmartShopColors.ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                }
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
            color = if (isTotal) SmartShopColors.ElectricBlue else SmartShopColors.TextPrimary,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            "UGX ${String.format("%,d", amount / 100)}",
            style = if (isTotal) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            color = if (isTotal) SmartShopColors.ElectricBlue else SmartShopColors.TextPrimary,
            fontWeight = if (isTotal) FontWeight.Bold else FontWeight.Normal
        )
    }
}