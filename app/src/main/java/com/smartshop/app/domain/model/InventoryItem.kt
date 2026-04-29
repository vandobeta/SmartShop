package com.smartshop.app.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "inventory_items")
data class InventoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val barcode: String,
    val name: String,
    val quantity: Int,
    val category: String,
    val price: Int, // Price in UGX
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val salesCount: Int = 0
)

enum class ItemCategory(val displayName: String) {
    BEVERAGES("Beverages"),
    SNACKS("Snacks"),
    DAIRY("Dairy"),
    GROCERIES("Groceries"),
    HOUSEHOLD("Household"),
    OTHER("Other")
}