package com.smartshop.sovereign.domain.model

/**
 * Domain Model for Cart Item
 */
data class CartItem(
    val product: Product,
    val quantity: Int = 1
) {
    val totalPrice: Long = product.price * quantity
}