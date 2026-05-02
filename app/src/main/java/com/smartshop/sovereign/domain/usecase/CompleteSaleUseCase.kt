package com.smartshop.sovereign.domain.usecase

import com.smartshop.sovereign.data.local.dao.SaleDao
import com.smartshop.sovereign.data.local.entity.SaleEntity
import com.smartshop.sovereign.domain.model.CartItem
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

/**
 * Use Case: Complete a sale
 */
class CompleteSaleUseCase @Inject constructor(
    private val saleDao: SaleDao,
    private val productDao: com.smartshop.sovereign.data.local.dao.ProductDao
) {
    suspend operator fun invoke(
        items: List<CartItem>,
        subtotal: Long,
        tax: Long,
        total: Long,
        cashierName: String
    ): Long {
        // Create JSON of items
        val itemsJson = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("barcode", item.product.barcode)
                put("name", item.product.name)
                put("quantity", item.quantity)
                put("price", item.product.price)
                put("total", item.totalPrice)
            }
            itemsJson.put(obj)

            // Decrement stock
            productDao.decrementStock(item.product.barcode, item.quantity)
        }

        // Insert sale
        val sale = SaleEntity(
            itemsJson = itemsJson.toString(),
            subtotal = subtotal,
            tax = tax,
            total = total,
            cashierName = cashierName
        )
        return saleDao.insertSale(sale)
    }
}