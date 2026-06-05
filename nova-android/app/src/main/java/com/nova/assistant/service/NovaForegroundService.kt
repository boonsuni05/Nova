package com.nova.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nova.assistant.server.MessageHandler
import com.nova.assistant.server.NovaWebSocketServer

class NovaForegroundService : Service() {

    private var server: NovaWebSocketServer? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        startForeground(NOTIFICATION_ID, buildNotification())
        startServer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        try {
            server?.stop(1000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        Log.i(TAG, "Service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startServer() {
        try {
            val handler = MessageHandler(this)
            server = NovaWebSocketServer(PORT, handler).also { it.start() }
            Log.i(TAG, "WebSocket server started on port $PORT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WebSocket server", e)
            stopSelf()
        }
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Nova Service", NotificationManager.IMPORTANCE_LOW)
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Nova")
            .setContentText("WebSocket server running on :$PORT")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val PORT = 8765
        private const val CHANNEL_ID = "nova_service"
        private const val NOTIFICATION_ID = 1
        private const val TAG = "NovaService"

        @Volatile
        var isRunning = false
            private set
    }
}
