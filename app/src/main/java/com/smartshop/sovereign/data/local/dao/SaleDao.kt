package com.smartshop.sovereign.data.local.dao

import androidx.room.*
import com.smartshop.sovereign.data.local.entity.SaleEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Sale operations
 */
@Dao
interface SaleDao {

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id LIMIT 1")
    suspend fun getSaleById(id: Long): SaleEntity?

    @Query("SELECT * FROM sales WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getSalesInRange(startTime: Long, endTime: Long): Flow<List<SaleEntity>>

    @Query("SELECT SUM(total) FROM sales")
    suspend fun getTotalRevenue(): Long?

    @Query("SELECT SUM(total) FROM sales WHERE timestamp BETWEEN :startTime AND :endTime")
    suspend fun getRevenueInRange(startTime: Long, endTime: Long): Long?

    @Insert
    suspend fun insertSale(sale: SaleEntity): Long

    @Delete
    suspend fun deleteSale(sale: SaleEntity)
}