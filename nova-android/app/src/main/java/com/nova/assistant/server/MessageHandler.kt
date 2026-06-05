package com.nova.assistant.server

import android.content.Context
import com.nova.assistant.commands.CommandRouter
import com.nova.assistant.model.NovaCommand
import org.json.JSONObject

class MessageHandler(context: Context) {

    private val router = CommandRouter(context)

    fun handle(raw: String): String {
        return try {
            val json = JSONObject(raw)
            val command = NovaCommand(
                id = json.optString("id", ""),
                action = json.getString("action"),
                params = parseParams(json.optJSONObject("params"))
            )
            val response = router.route(command)
            JSONObject().apply {
                put("id", response.id)
                put("status", response.status)
                put("message", response.message)
            }.toString()
        } catch (e: Exception) {
            JSONObject().apply {
                put("id", "")
                put("status", "error")
                put("message", "Parse error: ${e.message}")
            }.toString()
        }
    }

    private fun parseParams(obj: JSONObject?): Map<String, Any> {
        if (obj == null) return emptyMap()
        return obj.keys().asSequence().associateWith { obj.get(it) }
    }
}
