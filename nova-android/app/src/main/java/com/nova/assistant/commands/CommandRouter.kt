package com.nova.assistant.commands

import android.content.Context
import com.nova.assistant.model.NovaCommand
import com.nova.assistant.model.NovaResponse

class CommandRouter(context: Context) {

    private val volumeExecutor = VolumeExecutor(context)
    private val batteryExecutor = BatteryExecutor(context)
    private val brightnessExecutor = BrightnessExecutor(context)
    private val flashlightExecutor = FlashlightExecutor(context)
    private val mediaExecutor = MediaExecutor(context)
    private val appLauncherExecutor = AppLauncherExecutor(context)

    fun route(command: NovaCommand): NovaResponse {
        return try {
            val message = when (command.action) {
                "set_volume" -> {
                    val level = (command.params["level"] as? Number)?.toInt()
                        ?: return NovaResponse(command.id, "error", "Missing or invalid 'level' param")
                    volumeExecutor.setVolume(level)
                }
                "get_volume" -> volumeExecutor.getVolume()
                "get_battery" -> batteryExecutor.getBattery()
                "set_brightness" -> {
                    val level = (command.params["level"] as? Number)?.toInt()
                        ?: return NovaResponse(command.id, "error", "Missing or invalid 'level' param")
                    brightnessExecutor.setBrightness(level)
                }
                "get_brightness" -> brightnessExecutor.getBrightness()
                "get_status" -> listOf(
                    batteryExecutor.getBattery(),
                    volumeExecutor.getVolume(),
                    brightnessExecutor.getBrightness()
                ).joinToString(" | ")
                "set_flashlight" -> {
                    val on = command.params["on"] as? Boolean
                        ?: return NovaResponse(command.id, "error", "Missing or invalid 'on' param")
                    flashlightExecutor.setFlashlight(on)
                }
                "media_play" -> mediaExecutor.play()
                "media_pause" -> mediaExecutor.pause()
                "media_next" -> mediaExecutor.next()
                "media_previous" -> mediaExecutor.previous()
                "open_app" -> {
                    val query = command.params["query"] as? String
                        ?: return NovaResponse(command.id, "error", "Missing or invalid 'query' param")
                    appLauncherExecutor.launchApp(query)
                }
                else -> return NovaResponse(command.id, "error", "Unknown action: ${command.action}")
            }
            NovaResponse(command.id, "ok", message)
        } catch (e: Exception) {
            NovaResponse(command.id, "error", e.message ?: "Execution failed")
        }
    }
}
