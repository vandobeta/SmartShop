package com.smartshop.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val itemsJson: String, // JSON array of CartItem
    val total: Int, // Total in UGX
    val timestamp: Long = System.currentTimeMillis()
)