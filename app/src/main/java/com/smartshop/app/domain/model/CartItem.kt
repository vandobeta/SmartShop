package com.smartshop.app.domain.model

data class CartItem(
    val inventoryItem: InventoryItem,
    var quantity: Int = 1
) {
    val totalPrice: Int get() = inventoryItem.price * quantity
}