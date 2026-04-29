package com.smartshop.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.smartshop.app.data.local.database.SmartShopDatabase
import com.smartshop.app.data.repository.InventoryRepository
import com.smartshop.app.data.security.AuthManager
import com.smartshop.app.workers.LowStockWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class SmartShopApplication : Application(), Configuration.Provider {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    lateinit var database: SmartShopDatabase
        private set
    
    lateinit var inventoryRepository: InventoryRepository
        private set
    
    lateinit var authManager: AuthManager
        private set
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize database
        database = SmartShopDatabase.getDatabase(this, scope)
        
        // Initialize repositories
        inventoryRepository = InventoryRepository(
            database.inventoryDao(),
            database.transactionDao()
        )
        
        // Initialize auth manager
        authManager = AuthManager(this)
        
        // Schedule low stock worker
        com.smartshop.app.workers.LowStockWorker.schedule(this)

        // Create notification channel
        createNotificationChannel()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
    
    companion object {
        lateinit var instance: SmartShopApplication
            private set
        
        const val NOTIFICATION_CHANNEL_ID = "low_stock_channel"
        
        // Get application instance
        fun get(): SmartShopApplication = instance
    }
}