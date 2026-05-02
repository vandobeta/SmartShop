package com.smartshop.sovereign.presentation.ui.screen.addproduct

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartshop.sovereign.presentation.ui.theme.SmartShopColors
import com.smartshop.sovereign.presentation.viewmodel.AddProductViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    prefillBarcode: String = "",
    onNavigateBack: () -> Unit,
    onProductAdded: () -> Unit,
    viewModel: AddProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(prefillBarcode) {
        if (prefillBarcode.isNotEmpty()) {
            viewModel.setBarcode(prefillBarcode)
        }
    }

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onProductAdded()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Product", color = SmartShopColors.TextPrimary) },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Barcode
            OutlinedTextField(
                value = uiState.barcode,
                onValueChange = { viewModel.setBarcode(it) },
                label = { Text("Barcode") },
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = prefillBarcode.isNotEmpty(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SmartShopColors.ElectricBlue,
                    focusedLabelColor = SmartShopColors.ElectricBlue
                )
            )

            // Product Name
            OutlinedTextField(
                value = uiState.name,
                onValueChange = { viewModel.setName(it) },
                label = { Text("Product Name") },
                leadingIcon = { Icon(Icons.Default.Inventory, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SmartShopColors.ElectricBlue,
                    focusedLabelColor = SmartShopColors.ElectricBlue
                )
            )

            // Category dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = it }
            ) {
                OutlinedTextField(
                    value = uiState.category.ifEmpty { "Select Category" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SmartShopColors.ElectricBlue,
                        focusedLabelColor = SmartShopColors.ElectricBlue
                    )
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    uiState.categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                viewModel.setCategory(category)
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Price
            OutlinedTextField(
                value = uiState.price,
                onValueChange = { viewModel.setPrice(it) },
                label = { Text("Price (UGX)") },
                leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SmartShopColors.ElectricBlue,
                    focusedLabelColor = SmartShopColors.ElectricBlue
                )
            )

            // Quantity
            OutlinedTextField(
                value = uiState.quantity,
                onValueChange = { viewModel.setQuantity(it) },
                label = { Text("Initial Stock") },
                leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SmartShopColors.ElectricBlue,
                    focusedLabelColor = SmartShopColors.ElectricBlue
                )
            )

            // Cost Price (optional)
            OutlinedTextField(
                value = uiState.costPrice,
                onValueChange = { viewModel.setCostPrice(it) },
                label = { Text("Cost Price (Optional)") },
                leadingIcon = { Icon(Icons.Default.MoneyOff, contentDescription = null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SmartShopColors.ElectricBlue,
                    focusedLabelColor = SmartShopColors.ElectricBlue
                )
            )

            // Error message
            if (uiState.error.isNotEmpty()) {
                Text(
                    uiState.error,
                    color = SmartShopColors.ErrorRed,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Product Button
            Button(
                onClick = { viewModel.addProduct() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !uiState.isLoading && uiState.isValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SmartShopColors.ElectricBlue,
                    contentColor = SmartShopColors.Black
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SmartShopColors.ElectricBlue
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Product", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}