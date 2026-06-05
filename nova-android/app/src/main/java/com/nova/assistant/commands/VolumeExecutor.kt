package com.nova.assistant.commands

import android.content.Context
import android.media.AudioManager
import kotlin.math.roundToInt

class VolumeExecutor(context: Context) {

    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setVolume(percent: Int): String {
        val clamped = percent.coerceIn(0, 100)
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val target = (clamped / 100.0 * max).roundToInt()
        audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        return "Volume set to $clamped%"
    }

    fun getVolume(): String {
        val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        val percent = (current.toDouble() / max * 100).roundToInt()
        return "Volume is at $percent%"
    }
}
