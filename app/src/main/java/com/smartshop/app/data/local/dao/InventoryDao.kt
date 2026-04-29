package com.smartshop.app.data.local.dao

import androidx.room.*
import com.smartshop.app.domain.model.InventoryItem
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun getAllItems(): Flow<List<InventoryItem>>
    
    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: Long): InventoryItem?
    
    @Query("SELECT * FROM inventory_items WHERE barcode = :barcode")
    suspend fun getItemByBarcode(barcode: String): InventoryItem?
    
    @Query("SELECT * FROM inventory_items WHERE quantity <= :threshold")
    fun getLowStockItems(threshold: Int = 5): Flow<List<InventoryItem>>
    
    @Query("SELECT * FROM inventory_items WHERE category = :category")
    fun getItemsByCategory(category: String): Flow<List<InventoryItem>>
    
    @Query("SELECT * FROM inventory_items WHERE name LIKE '%' || :query || '%'")
    fun searchItems(query: String): Flow<List<InventoryItem>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<InventoryItem>)
    
    @Update
    suspend fun updateItem(item: InventoryItem)
    
    @Delete
    suspend fun deleteItem(item: InventoryItem)
    
    @Query("DELETE FROM inventory_items WHERE id = :id")
    suspend fun deleteItemById(id: Long)
    
    @Query("UPDATE inventory_items SET quantity = quantity + :quantity, updatedAt = :timestamp WHERE id = :id")
    suspend fun addQuantity(id: Long, quantity: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE inventory_items SET quantity = quantity - :quantity, salesCount = salesCount + :quantity, updatedAt = :timestamp WHERE id = :id AND quantity >= :quantity")
    suspend fun deductQuantity(id: Long, quantity: Int, timestamp: Long = System.currentTimeMillis()): Int
    
    // Analytics queries
    @Query("SELECT SUM(quantity * price) FROM inventory_items")
    suspend fun getInventoryWorth(): Int?
    
    @Query("SELECT category, SUM(salesCount) as totalSales FROM inventory_items GROUP BY category ORDER BY totalSales DESC")
    suspend fun getSalesByCategory(): List<CategorySales>
    
    @Query("SELECT * FROM inventory_items ORDER BY salesCount DESC LIMIT :limit")
    suspend fun getTopSellingItems(limit: Int = 10): List<InventoryItem>
}

data class CategorySales(
    val category: String,
    val totalSales: Int
)