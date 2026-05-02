package com.smartshop.sovereign.domain.usecase

import com.smartshop.sovereign.data.local.dao.ProductDao
import com.smartshop.sovereign.data.local.entity.ProductEntity
import com.smartshop.sovereign.domain.model.Product
import javax.inject.Inject

/**
 * Use Case: Look up product by barcode
 * Contains business logic - NO logic in ViewModels
 */
class ScanLookupUseCase @Inject constructor(
    private val productDao: ProductDao
) {
    suspend operator fun invoke(barcode: String): Product? {
        val entity = productDao.getProductByBarcode(barcode)
        return entity?.toDomain()
    }

    private fun ProductEntity.toDomain() = Product(
        barcode = barcode,
        name = name,
        category = category,
        price = price,
        quantity = quantity,
        costPrice = costPrice
    )
}