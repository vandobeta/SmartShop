package com.smartshop.sovereign.presentation.ui.screen.receipt

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartshop.sovereign.presentation.ui.theme.SmartShopColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    receiptText: String,
    shopName: String,
    shopTel: String,
    onDone: () -> Unit,
    onNewSale: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Receipt", 
                        color = SmartShopColors.TextPrimary,
                        fontWeight = FontWeight.Bold
                    ) 
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
            // Receipt display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = SmartShopColors.CardBackground
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = receiptText,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    color = SmartShopColors.TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Share buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // WhatsApp share
                Button(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, receiptText)
                            type = "text/plain"
                            setPackage("com.whatsapp")
                        }
                        try {
                            context.startActivity(sendIntent)
                        } catch (e: Exception) {
                            // WhatsApp not installed, use general share
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, receiptText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Receipt"))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmartShopColors.SuccessGreen,
                        contentColor = SmartShopColors.Black
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WhatsApp")
                }

                // SMS share
                Button(
                    onClick = {
                        val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("sms:")
                            putExtra("sms_body", receiptText)
                        }
                        context.startActivity(smsIntent)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmartShopColors.ElectricBlue,
                        contentColor = SmartShopColors.Black
                    )
                ) {
                    Icon(Icons.Default.Sms, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SMS")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Done / New Sale buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDone,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Done")
                }

                Button(
                    onClick = onNewSale,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SmartShopColors.ElectricBlue,
                        contentColor = SmartShopColors.Black
                    )
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("New Sale")
                }
            }
        }
    }
}