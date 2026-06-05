package com.nova.assistant.server

import android.util.Log
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress

class NovaWebSocketServer(
    port: Int,
    private val handler: MessageHandler
) : WebSocketServer(InetSocketAddress(port)) {

    init {
        isReuseAddr = true
    }

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        Log.i(TAG, "Client connected: ${conn.remoteSocketAddress}")
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
        Log.i(TAG, "Client disconnected: ${conn.remoteSocketAddress}")
    }

    override fun onMessage(conn: WebSocket, message: String) {
        Log.d(TAG, "Received: $message")
        val response = handler.handle(message)
        Log.d(TAG, "Sending: $response")
        conn.send(response)
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        Log.e(TAG, "WebSocket error on ${conn?.remoteSocketAddress}", ex)
    }

    override fun onStart() {
        Log.i(TAG, "Server started on port ${address.port}")
    }

    companion object {
        private const val TAG = "NovaWSServer"
    }
}
