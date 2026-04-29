package com.smartshop.app.data.repository

import com.smartshop.app.data.local.dao.InventoryDao
import com.smartshop.app.data.local.dao.TransactionDao
import com.smartshop.app.domain.model.CartItem
import com.smartshop.app.domain.model.InventoryItem
import com.smartshop.app.domain.model.ItemCategory
import com.smartshop.app.domain.model.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val transactionDao: TransactionDao
) {
    // Inventory operations
    fun getAllItems(): Flow<List<InventoryItem>> = inventoryDao.getAllItems()
    
    suspend fun getItemById(id: Long): InventoryItem? = inventoryDao.getItemById(id)
    
    suspend fun getItemByBarcode(barcode: String): InventoryItem? = 
        inventoryDao.getItemByBarcode(barcode)
    
    fun getLowStockItems(threshold: Int = 5): Flow<List<InventoryItem>> = 
        inventoryDao.getLowStockItems(threshold)
    
    fun searchItems(query: String): Flow<List<InventoryItem>> = 
        inventoryDao.searchItems(query)
    
    fun getItemsByCategory(category: String): Flow<List<InventoryItem>> = 
        inventoryDao.getItemsByCategory(category)
    
    suspend fun insertItem(item: InventoryItem): Long = inventoryDao.insertItem(item)
    
    suspend fun updateItem(item: InventoryItem) = inventoryDao.updateItem(item)
    
    suspend fun deleteItem(item: InventoryItem) = inventoryDao.deleteItem(item)
    
    suspend fun addQuantity(itemId: Long, quantity: Int) = 
        inventoryDao.addQuantity(itemId, quantity)
    
    suspend fun deductQuantity(itemId: Long, quantity: Int): Boolean {
        val result = inventoryDao.deductQuantity(itemId, quantity)
        return result > 0
    }
    
    // Transaction operations
    suspend fun saveTransaction(cartItems: List<CartItem>, total: Int): Long {
        val gson = Gson()
        val itemsJson = gson.toJson(cartItems.map {_cartItem ->
            mapOf(
                "itemId" to _cartItem.inventoryItem.id,
                "quantity" to _cartItem.quantity,
                "price" to _cartItem.inventoryItem.price
            )
        })
        val transaction = Transaction(itemsJson = itemsJson, total = total)
        return transactionDao.insertTransaction(transaction)
    }
    
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAllTransactions()
    
    // Analytics
    suspend fun getTotalSales(): Int = transactionDao.getTotalSales() ?: 0
    
    suspend fun getTodaySales(): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val endOfDay = calendar.timeInMillis
        
        return transactionDao.getTodaySales(startOfDay, endOfDay) ?: 0
    }
    
    suspend fun getInventoryWorth(): Int = inventoryDao.getInventoryWorth() ?: 0
    
    suspend fun getSalesByCategory(): List<com.smartshop.app.data.local.dao.CategorySales> = 
        inventoryDao.getSalesByCategory()
    
    suspend fun getTopSellingItems(limit: Int = 10): List<InventoryItem> = 
        inventoryDao.getTopSellingItems(limit)
}