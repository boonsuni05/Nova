package com.nova.assistant.commands

import android.content.Context
import android.provider.Settings
import kotlin.math.roundToInt

class BrightnessExecutor(private val context: Context) {

    fun setBrightness(percent: Int): String {
        if (!Settings.System.canWrite(context)) {
            return "Permission denied: open Nova, tap 'Grant Settings Permission', and enable 'Modify system settings'"
        }
        val clamped = percent.coerceIn(0, 100)
        // Disable auto-brightness so the manual value takes effect immediately.
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        val raw = (clamped / 100.0 * 255).roundToInt().coerceIn(0, 255)
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, raw)
        return "Brightness set to $clamped%"
    }

    fun getBrightness(): String {
        val raw = Settings.System.getInt(
            context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, -1
        )
        if (raw < 0) return "Brightness unavailable"
        val percent = (raw / 255.0 * 100).roundToInt()
        return "Brightness: $percent%"
    }
}
