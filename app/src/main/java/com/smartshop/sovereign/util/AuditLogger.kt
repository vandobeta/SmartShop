package com.smartshop.sovereign.util

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Audit Logger - Production-Grade Logging System
 * Format: [TIMESTAMP] | [LEVEL] | [MODULE] | [ACTION] | [STATUS] | [PAYLOAD/ERROR_CODE]
 */
@Singleton
class AuditLogger @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val filenameDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        private const val TAG = "SmartShop-AUDIT"
        private const val MODULE = "APP"
    }

    enum class Level { INFO, WARN, ERROR, DEBUG }

    enum class Action {
        SCAN_SUCCESS, SCAN_ERROR, SALE_COMPLETE, SALE_FAILED,
        STOCK_UPDATE, LOGIN_SUCCESS, LOGIN_FAILED,
        PERMISSION_GRANTED, PERMISSION_DENIED
    }

    enum class Status { SUCCESS, FAILURE, PENDING }

    /**
     * Log to both Logcat and file
     */
    fun log(
        level: Level,
        action: Action,
        status: Status,
        payload: String = ""
    ) {
        val timestamp = dateFormat.format(Date())
        val logLine = "[$timestamp] | [${level.name}] | [$MODULE] | [${action.name}] | [${status.name}] | [$payload]"

        // Log to Logcat
        when (level) {
            Level.INFO -> Log.i(TAG, logLine)
            Level.WARN -> Log.w(TAG, logLine)
            Level.ERROR -> Log.e(TAG, logLine)
            Level.DEBUG -> Log.d(TAG, logLine)
        }

        // Also write to file (if storage available)
        writeToFile(logLine)
    }

    private fun writeToFile(line: String) {
        try {
            // Use app's external files dir for persistence
            val logDir = context.getExternalFilesDir(null)
                ?: context.filesDir
            val logFile = File(logDir, "audit_log_${filenameDateFormat.format(Date())}.csv")

            FileOutputStream(logFile, true).bufferedWriter().use { writer ->
                writer.write(line)
                writer.newLine()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write audit log: ${e.message}")
        }
    }

    // Convenience methods
    fun onScanSuccess(barcode: String) = log(Level.INFO, Action.SCAN_SUCCESS, Status.SUCCESS, barcode)
    fun onScanError(barcode: String) = log(Level.ERROR, Action.SCAN_ERROR, Status.FAILURE, barcode)
    fun onSaleComplete(amount: Long) = log(Level.INFO, Action.SALE_COMPLETE, Status.SUCCESS, amount.toString())
    fun onSaleFailed(reason: String) = log(Level.ERROR, Action.SALE_FAILED, Status.FAILURE, reason)
}