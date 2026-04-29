package com.smartshop.app.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast

class DeviceAdminReceiver : DeviceAdminReceiver() {
    
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        return "Warning: Disabling SmartShop will clear inventory. Backup first."
    }
}
