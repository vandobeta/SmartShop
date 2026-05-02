package com.smartshop.sovereign

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * SmartShop Sovereign Application
 * Entry point for Hilt dependency injection
 */
@HiltAndroidApp
class SmartShopApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide configurations here
    }
}