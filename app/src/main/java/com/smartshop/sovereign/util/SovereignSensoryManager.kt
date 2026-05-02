package com.smartshop.sovereign.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sovereign Sensory Manager - Hardware Latency Fix
 * Zero-lag beep and vibration feedback
 */
@Singleton
class SovereignSensoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val toneGenerator: ToneGenerator
    private val vibrator: Vibrator

    init {
        toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * Success feedback - short beep + short vibration
     */
    fun onScanSuccess() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    }

    /**
     * Error feedback - long vibration
     */
    fun onScanError() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(500)
        }
    }

    /**
     * Success feedback with custom duration
     */
    fun playSuccess(durationMs: Int = 150) {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
    }

    fun release() {
        toneGenerator.release()
    }
}