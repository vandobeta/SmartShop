package com.smartshop.sovereign.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartshop.sovereign.data.local.dao.ProductDao
import com.smartshop.sovereign.data.local.dao.SaleDao
import com.smartshop.sovereign.data.local.entity.ProductEntity
import com.smartshop.sovereign.data.local.entity.SaleEntity

/**
 * SmartShop Sovereign Database
 * Uses WAL (Write-Ahead Logging) for better performance
 */
@Database(
    entities = [ProductEntity::class, SaleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class SmartShopDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun saleDao(): SaleDao
}