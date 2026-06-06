package com.nova.assistant.commands

import android.content.Context
import android.media.AudioManager
import android.view.KeyEvent

class MediaExecutor(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun play(): String {
        dispatch(KeyEvent.KEYCODE_MEDIA_PLAY)
        return "Play"
    }

    fun pause(): String {
        dispatch(KeyEvent.KEYCODE_MEDIA_PAUSE)
        return "Pause"
    }

    fun next(): String {
        dispatch(KeyEvent.KEYCODE_MEDIA_NEXT)
        return "Next track"
    }

    fun previous(): String {
        dispatch(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
        return "Previous track"
    }

    @Suppress("DEPRECATION")
    private fun dispatch(keyCode: Int) {
        // dispatchMediaKeyEvent routes to the active media session without requiring
        // BIND_NOTIFICATION_LISTENER_SERVICE. Deprecated in API 31 but functional.
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }
}
