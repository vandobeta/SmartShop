package com.smartshop.sovereign.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Sale Entity - stores completed transactions
 */
@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemsJson: String,     // JSON array of items
    val subtotal: Long,     // Subtotal in UGX cents
    val tax: Long,          // Tax amount in UGX cents
    val total: Long,        // Total in UGX cents
    val timestamp: Long = System.currentTimeMillis(),
    val cashierName: String = "DEFAULT"
)