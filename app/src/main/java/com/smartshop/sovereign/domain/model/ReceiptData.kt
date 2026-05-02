package com.smartshop.sovereign.domain.model

/**
 * Data for receipt generation
 */
data class ReceiptData(
    val shopName: String,
    val shopTel: String,
    val cashierName: String,
    val timestamp: String,
    val transactionId: String,
    val items: List<ReceiptItem>,
    val subtotal: Long,
    val tax: Long,
    val total: Long,
    val nicheType: String
)

data class ReceiptItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Long,
    val totalPrice: Long
)