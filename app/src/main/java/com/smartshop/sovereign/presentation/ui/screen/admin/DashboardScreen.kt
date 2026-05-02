package com.smartshop.sovereign.presentation.ui.screen.admin

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartshop.sovereign.presentation.ui.theme.SmartShopColors
import com.smartshop.sovereign.presentation.viewmodel.DashboardViewModel
import com.smartshop.sovereign.presentation.viewmodel.CategoryStock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateBack: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard", color = SmartShopColors.TextPrimary) },
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SmartShopColors.ElectricBlue)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Cards Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Total Products",
                            value = uiState.totalProducts.toString(),
                            icon = Icons.Default.Inventory,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Revenue",
                            value = "UGX ${uiState.totalRevenue / 100}",
                            icon = Icons.Default.AttachMoney,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Category Progress Bars
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SmartShopColors.CardBackground
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Category Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                color = SmartShopColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (uiState.categoryStock.isEmpty()) {
                                Text(
                                    "No products in inventory",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = SmartShopColors.TextMuted
                                )
                            } else {
                                uiState.categoryStock.forEach { category ->
                                    CategoryProgressBar(category = category)
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }

                // Low Stock Alert
                if (uiState.lowStockProducts.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = SmartShopColors.ErrorRed.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = SmartShopColors.ErrorRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Low Stock Alert (${uiState.lowStockProducts.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SmartShopColors.ErrorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                uiState.lowStockProducts.take(5).forEach { product ->
                                    LowStockItem(
                                        name = product.name,
                                        stock = product.quantity
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }

                // Quick Actions
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SmartShopColors.CardBackground
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Quick Actions",
                                style = MaterialTheme.typography.titleMedium,
                                color = SmartShopColors.TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                QuickActionButton(
                                    icon = Icons.Default.Add,
                                    label = "Add Product",
                                    modifier = Modifier.weight(1f)
                                )
                                QuickActionButton(
                                    icon = Icons.Default.QrCodeScanner,
                                    label = "Scan",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = SmartShopColors.CardBackground
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = SmartShopColors.ElectricBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                color = SmartShopColors.ElectricBlue,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = SmartShopColors.TextSecondary
            )
        }
    }
}

@Composable
private fun CategoryProgressBar(category: CategoryStock) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                category.category,
                style = MaterialTheme.typography.bodyMedium,
                color = SmartShopColors.TextPrimary
            )
            Text(
                "${category.count} items",
                style = MaterialTheme.typography.bodySmall,
                color = SmartShopColors.TextMuted
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { category.percentage },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = SmartShopColors.ElectricBlue,
            trackColor = SmartShopColors.SurfaceVariant,
        )
    }
}

@Composable
private fun LowStockItem(name: String, stock: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = SmartShopColors.TextPrimary
        )
        Text(
            "Stock: $stock",
            style = MaterialTheme.typography.bodySmall,
            color = if (stock < 3) SmartShopColors.ErrorRed else SmartShopColors.WarningOrange
        )
    }
}

@Composable
private fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = { /* Navigate to action */ },
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = SmartShopColors.ElectricBlue,
            contentColor = SmartShopColors.Black
        )
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}