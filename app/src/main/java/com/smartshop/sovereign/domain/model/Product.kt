package com.smartshop.sovereign.domain.model

/**
 * Domain Model for Product
 */
data class Product(
    val barcode: String,
    val name: String,
    val category: String,
    val price: Long,        // Price in UGX cents
    val quantity: Int,      // Stock quantity
    val costPrice: Long = 0
) {
    /**
     * Format price for display (e.g., 150000 -> "UGX 1,500")
     */
    fun formatPrice(): String {
        return "UGX ${price / 100}"
    }

    /**
     * Calculate profit
     */
    fun profit(): Long = if (costPrice > 0) price - costPrice else 0L
}