package com.smartshop.sovereign.presentation.ui.screen.setup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartshop.sovereign.presentation.ui.theme.SmartShopColors
import com.smartshop.sovereign.presentation.viewmodel.SetupViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onSetupComplete: () -> Unit,
    viewModel: SetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isComplete) {
        if (uiState.isComplete) {
            onSetupComplete()
        }
    }

    Scaffold(
        containerColor = SmartShopColors.DarkBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(SmartShopColors.DarkBackground)
        ) {
            when (uiState.step) {
                0 -> WelcomeStep(onNext = { viewModel.nextStep() })
                1 -> PasscodeStep(
                    passcode = uiState.passcode,
                    confirmPasscode = uiState.confirmPasscode,
                    error = uiState.error,
                    onPasscodeChange = { viewModel.setPasscode(it) },
                    onConfirmChange = { viewModel.setConfirmPasscode(it) },
                    onNext = { viewModel.nextStep() }
                )
                2 -> ShopTypeStep(
                    selectedType = uiState.shopType,
                    onSelectType = { viewModel.setShopType(it) },
                    onNext = { viewModel.nextStep() }
                )
                3 -> ShopInfoStep(
                    shopName = uiState.shopName,
                    shopTel = uiState.shopTel,
                    error = uiState.error,
                    isLoading = uiState.isLoading,
                    onNameChange = { viewModel.setShopName(it) },
                    onTelChange = { viewModel.setShopTel(it) },
                    onComplete = { viewModel.completeSetup() }
                )
                else -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = SmartShopColors.ElectricBlue
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Store,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = SmartShopColors.ElectricBlue
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Welcome to SmartShop",
            style = MaterialTheme.typography.headlineLarge,
            color = SmartShopColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Sovereign POS System",
            style = MaterialTheme.typography.titleMedium,
            color = SmartShopColors.ElectricBlue
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            "Let's set up your shop in a few simple steps. You'll need to create an admin PIN and enter your shop details.",
            style = MaterialTheme.typography.bodyLarge,
            color = SmartShopColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = SmartShopColors.ElectricBlue,
                contentColor = SmartShopColors.Black
            )
        ) {
            Text("Get Started", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasscodeStep(
    passcode: String,
    confirmPasscode: String,
    error: String,
    onPasscodeChange: (String) -> Unit,
    onConfirmChange: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Icon(
            Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SmartShopColors.ElectricBlue
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Create Admin PIN",
            style = MaterialTheme.typography.headlineSmall,
            color = SmartShopColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "This PIN will be required to access admin features and add new products",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartShopColors.TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = passcode,
            onValueChange = { if (it.length <= 6) onPasscodeChange(it) },
            label = { Text("Enter PIN (4-6 digits)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SmartShopColors.ElectricBlue,
                focusedLabelColor = SmartShopColors.ElectricBlue
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = confirmPasscode,
            onValueChange = { if (it.length <= 6) onConfirmChange(it) },
            label = { Text("Confirm PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SmartShopColors.ElectricBlue,
                focusedLabelColor = SmartShopColors.ElectricBlue
            )
        )
        
        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error,
                color = SmartShopColors.ErrorRed,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = passcode.length >= 4 && passcode == confirmPasscode,
            colors = ButtonDefaults.buttonColors(
                containerColor = SmartShopColors.ElectricBlue,
                contentColor = SmartShopColors.Black
            )
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@Composable
private fun ShopTypeStep(
    selectedType: String,
    onSelectType: (String) -> Unit,
    onNext: () -> Unit
) {
    val shopTypes = listOf(
        "Supermarket", "General Store", "Pharmacy", "Hardware",
        "Electronics", "Clothing", "Restaurant", "Service Station", "Other"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Text(
            "Select Shop Type",
            style = MaterialTheme.typography.headlineSmall,
            color = SmartShopColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "This helps us customize your experience",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartShopColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(shopTypes) { type ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectType(type) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selectedType == type) 
                            SmartShopColors.ElectricBlue.copy(alpha = 0.2f) 
                        else SmartShopColors.CardBackground
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            type,
                            style = MaterialTheme.typography.bodyLarge,
                            color = SmartShopColors.TextPrimary
                        )
                        if (selectedType == type) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = SmartShopColors.ElectricBlue
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedType.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = SmartShopColors.ElectricBlue,
                contentColor = SmartShopColors.Black
            )
        ) {
            Text("Continue", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ArrowForward, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShopInfoStep(
    shopName: String,
    shopTel: String,
    error: String,
    isLoading: Boolean,
    onNameChange: (String) -> Unit,
    onTelChange: (String) -> Unit,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Icon(
            Icons.Default.Storefront,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = SmartShopColors.ElectricBlue
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            "Shop Information",
            style = MaterialTheme.typography.headlineSmall,
            color = SmartShopColors.TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "This information will appear on your receipts",
            style = MaterialTheme.typography.bodyMedium,
            color = SmartShopColors.TextSecondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = shopName,
            onValueChange = onNameChange,
            label = { Text("Shop Name") },
            leadingIcon = { Icon(Icons.Default.Store, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SmartShopColors.ElectricBlue,
                focusedLabelColor = SmartShopColors.ElectricBlue
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = shopTel,
            onValueChange = { if (it.length <= 15) onTelChange(it) },
            label = { Text("Phone Number") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SmartShopColors.ElectricBlue,
                focusedLabelColor = SmartShopColors.ElectricBlue
            )
        )
        
        if (error.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                error,
                color = SmartShopColors.ErrorRed,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            enabled = shopName.isNotEmpty() && shopTel.isNotEmpty() && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = SmartShopColors.ElectricBlue,
                contentColor = SmartShopColors.Black
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = SmartShopColors.ElectricBlue
                )
            } else {
                Text("Complete Setup", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.Check, contentDescription = null)
            }
        }
    }
}