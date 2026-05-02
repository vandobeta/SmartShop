package com.smartshop.sovereign.domain.usecase

import com.smartshop.sovereign.data.local.dao.ProductDao
import com.smartshop.sovereign.data.local.entity.ProductEntity
import javax.inject.Inject

/**
 * Use Case: Add new product
 */
class AddProductUseCase @Inject constructor(
    private val productDao: ProductDao
) {
    suspend operator fun invoke(
        barcode: String,
        name: String,
        category: String,
        price: Long,
        quantity: Int,
        costPrice: Long = 0
    ) {
        val entity = ProductEntity(
            barcode = barcode,
            name = name,
            category = category,
            price = price,
            quantity = quantity,
            costPrice = costPrice
        )
        productDao.insertProduct(entity)
    }
}