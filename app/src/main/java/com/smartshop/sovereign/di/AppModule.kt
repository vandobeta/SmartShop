package com.smartshop.sovereign.di

import android.content.Context
import androidx.room.Room
import com.smartshop.sovereign.data.local.SmartShopDatabase
import com.smartshop.sovereign.data.local.dao.ProductDao
import com.smartshop.sovereign.data.local.dao.SaleDao
import com.smartshop.sovereign.data.local.datastore.SettingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartShopDatabase {
        return Room.databaseBuilder(
            context,
            SmartShopDatabase::class.java,
            "smartshop_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideProductDao(database: SmartShopDatabase): ProductDao {
        return database.productDao()
    }

    @Provides
    fun provideSaleDao(database: SmartShopDatabase): SaleDao {
        return database.saleDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }
}