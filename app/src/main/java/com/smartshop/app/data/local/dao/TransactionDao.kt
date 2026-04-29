package com.smartshop.app.data.local.dao

import androidx.room.*
import com.smartshop.app.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>
    
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?
    
    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime")
    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
    
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long
    
    @Delete
    suspend fun deleteTransaction(transaction: Transaction)
    
    // Analytics queries
    @Query("SELECT SUM(total) FROM transactions")
    suspend fun getTotalSales(): Int?
    
    @Query("SELECT SUM(total) FROM transactions WHERE timestamp >= :startOfDay AND timestamp <= :endOfDay")
    suspend fun getTodaySales(startOfDay: Long, endOfDay: Long): Int?
    
    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun getTransactionCount(): Int
}