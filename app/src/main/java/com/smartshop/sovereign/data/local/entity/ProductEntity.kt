package com.smartshop.sovereign.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Product Entity - stored in Room database
 * All money values in UGX cents (Long) to avoid floating point errors
 */
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey
    val barcode: String,
    val name: String,
    val category: String,
    val price: Long,        // Price in UGX cents
    val quantity: Int,      // Stock quantity
    val costPrice: Long = 0, // Cost price for profit calculation
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)