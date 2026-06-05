package com.nova.assistant.commands

import android.content.Context
import com.nova.assistant.model.NovaCommand
import com.nova.assistant.model.NovaResponse

class CommandRouter(context: Context) {

    private val volumeExecutor = VolumeExecutor(context)

    fun route(command: NovaCommand): NovaResponse {
        return try {
            val message = when (command.action) {
                "set_volume" -> {
                    val level = (command.params["level"] as? Number)?.toInt()
                        ?: return NovaResponse(command.id, "error", "Missing or invalid 'level' param")
                    volumeExecutor.setVolume(level)
                }
                "get_volume" -> volumeExecutor.getVolume()
                else -> return NovaResponse(command.id, "error", "Unknown action: ${command.action}")
            }
            NovaResponse(command.id, "ok", message)
        } catch (e: Exception) {
            NovaResponse(command.id, "error", e.message ?: "Execution failed")
        }
    }
}
