package com.smartshop.app.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartshop.app.data.local.dao.InventoryDao
import com.smartshop.app.data.local.dao.TransactionDao
import com.smartshop.app.domain.model.InventoryItem
import com.smartshop.app.domain.model.ItemCategory
import com.smartshop.app.domain.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [InventoryItem::class, Transaction::class],
    version = 1,
    exportSchema = false
)
abstract class SmartShopDatabase : RoomDatabase() {
    
    abstract fun inventoryDao(): InventoryDao
    abstract fun transactionDao(): TransactionDao
    
    companion object {
        @Volatile
        private var INSTANCE: SmartShopDatabase? = null
        
        fun getDatabase(context: Context, scope: CoroutineScope): SmartShopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SmartShopDatabase::class.java,
                    "smartshop_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
    
    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : Callback() {
        
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.inventoryDao())
                }
            }
        }
        
        suspend fun populateDatabase(inventoryDao: InventoryDao) {
            // Pre-populate with sample inventory items
            val sampleItems = listOf(
                InventoryItem(
                    barcode = "6901234567890",
                    name = "Coca Cola 500ml",
                    quantity = 50,
                    category = ItemCategory.BEVERAGES.displayName,
                    price = 2500
                ),
                InventoryItem(
                    barcode = "6901234567891",
                    name = "Pepsi 500ml",
                    quantity = 45,
                    category = ItemCategory.BEVERAGES.displayName,
                    price = 2500
                ),
                InventoryItem(
                    barcode = "6901234567892",
                    name = "Water 500ml",
                    quantity = 100,
                    category = ItemCategory.BEVERAGES.displayName,
                    price = 1500
                ),
                InventoryItem(
                    barcode = "6901234567893",
                    name = "Pringles Original",
                    quantity = 30,
                    category = ItemCategory.SNACKS.displayName,
                    price = 5000
                ),
                InventoryItem(
                    barcode = "6901234567894",
                    name = "Oreo Cookies",
                    quantity = 40,
                    category = ItemCategory.SNACKS.displayName,
                    price = 3000
                ),
                InventoryItem(
                    barcode = "6901234567895",
                    name = "Milk Fresh 1L",
                    quantity = 25,
                    category = ItemCategory.DAIRY.displayName,
                    price = 4500
                ),
                InventoryItem(
                    barcode = "6901234567896",
                    name = "Yogurt Bowl",
                    quantity = 35,
                    category = ItemCategory.DAIRY.displayName,
                    price = 2000
                ),
                InventoryItem(
                    barcode = "6901234567897",
                    name = "Sugar 1kg",
                    quantity = 20,
                    category = ItemCategory.GROCERIES.displayName,
                    price = 5500
                ),
                InventoryItem(
                    barcode = "6901234567898",
                    name = "Rice 2kg",
                    quantity = 15,
                    category = ItemCategory.GROCERIES.displayName,
                    price = 8000
                ),
                InventoryItem(
                    barcode = "6901234567899",
                    name = "Soap Bar",
                    quantity = 60,
                    category = ItemCategory.HOUSEHOLD.displayName,
                    price = 3000
                ),
                InventoryItem(
                    barcode = "6901234567900",
                    name = "Toothpaste",
                    quantity = 8,
                    category = ItemCategory.HOUSEHOLD.displayName,
                    price = 6000
                )
            )
            inventoryDao.insertItems(sampleItems)
        }
    }
}