package com.nova.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nova.assistant.commands.CommandRouter
import com.nova.assistant.commands.SpeechParser
import com.nova.assistant.service.NovaForegroundService
import java.net.Inet4Address
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnToggle: Button
    private lateinit var btnMic: Button
    private lateinit var tvTranscript: TextView

    private var speechRecognizer: SpeechRecognizer? = null
    private lateinit var commandRouter: CommandRouter

    companion object {
        private const val TAG = "NovaSpeech"
        private const val REQUEST_RECORD_AUDIO = 1
        // Error codes added in API 28 — use literals to stay safe at minSdk 26
        private const val ERROR_TOO_MANY_REQUESTS  = 10
        private const val ERROR_SERVER_DISCONNECTED = 11
        // Error codes added in API 31
        private const val ERROR_LANGUAGE_NOT_SUPPORTED  = 12
        private const val ERROR_LANGUAGE_UNAVAILABLE    = 13
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus    = findViewById(R.id.tvStatus)
        tvAddress   = findViewById(R.id.tvAddress)
        btnToggle   = findViewById(R.id.btnToggle)
        btnMic      = findViewById(R.id.btnMic)
        tvTranscript = findViewById(R.id.tvTranscript)

        commandRouter = CommandRouter(this)

        val permissions = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
            add(Manifest.permission.RECORD_AUDIO)
        }
        requestPermissions(permissions.toTypedArray(), 0)

        btnToggle.setOnClickListener {
            if (NovaForegroundService.isRunning) {
                stopService(Intent(this, NovaForegroundService::class.java))
            } else {
                startForegroundService(Intent(this, NovaForegroundService::class.java))
            }
            tvStatus.postDelayed({ updateUi() }, 300)
        }

        btnMic.setOnClickListener { startListening() }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: destroying SpeechRecognizer")
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    // ---------------------------------------------------------------------------
    // Speech recognition
    // ---------------------------------------------------------------------------

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "RECORD_AUDIO permission not granted — requesting")
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO)
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Log.e(TAG, "SpeechRecognizer.isRecognitionAvailable() returned false")
            tvTranscript.text = "Speech recognition not available on this device"
            return
        }

        // Reuse the recognizer instance across taps.
        // destroy()+createSpeechRecognizer() on every press causes Samsung to return
        // ERROR_SERVER_DISCONNECTED (11) because the service unbind is asynchronous.
        ensureRecognizer()
        speechRecognizer!!.cancel() // stop any in-progress session safely

        btnMic.isEnabled = false
        btnMic.text = "Listening..."
        tvTranscript.text = ""

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            // Prefer on-device recognition where available (Samsung Galaxy A34 supports this)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }
        Log.d(TAG, "startListening — language=${Locale.getDefault()}")
        speechRecognizer!!.startListening(intent)
    }

    private fun ensureRecognizer() {
        if (speechRecognizer == null) {
            Log.d(TAG, "Creating new SpeechRecognizer instance")
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).also {
                it.setRecognitionListener(buildListener())
            }
        }
    }

    private fun buildListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "onReadyForSpeech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // called many times per second — omit from logcat
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            Log.v(TAG, "onBufferReceived: ${buffer?.size} bytes")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "onEndOfSpeech")
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
            Log.d(TAG, "onPartialResults: \"$partial\"")
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            Log.d(TAG, "onEvent: type=$eventType")
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            Log.d(TAG, "onResults: \"$text\"")
            resetMicButton()
            if (text.isEmpty()) {
                tvTranscript.text = "No speech detected"
                return
            }
            tvTranscript.text = "Heard: \"$text\""
            routeSpeechCommand(text)
        }

        override fun onError(error: Int) {
            val msg = recognitionErrorString(error)
            Log.w(TAG, "onError: code=$error ($msg)")

            // ERROR_SERVER_DISCONNECTED means this recognizer instance is unusable.
            // Null it so ensureRecognizer() creates a fresh one on the next tap.
            if (error == ERROR_SERVER_DISCONNECTED) {
                Log.d(TAG, "Server disconnected — will recreate recognizer on next tap")
                speechRecognizer?.destroy()
                speechRecognizer = null
            }

            tvTranscript.text = "Error: $msg"
            resetMicButton()
        }
    }

    private fun routeSpeechCommand(text: String) {
        val command = SpeechParser.parse(text)
        if (command == null) {
            Log.d(TAG, "No matching command for: \"$text\"")
            tvTranscript.text = "${tvTranscript.text}\nNo matching command"
            return
        }
        Log.d(TAG, "Routing command: action=${command.action} params=${command.params}")
        Thread {
            val response = commandRouter.route(command)
            Log.d(TAG, "Command response: status=${response.status} message=${response.message}")
            runOnUiThread {
                tvTranscript.text = "${tvTranscript.text}\n-> ${response.message}"
            }
        }.start()
    }

    private fun resetMicButton() {
        btnMic.text = "Speak"
        btnMic.isEnabled = true
    }

    private fun recognitionErrorString(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO                 -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT                -> "Client side error"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_NETWORK               -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT       -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH              -> "No speech match — try again"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY       -> "Recognizer busy — tap Speak to retry"
        SpeechRecognizer.ERROR_SERVER                -> "Recognition server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT        -> "No speech detected — tap Speak to retry"
        ERROR_TOO_MANY_REQUESTS                      -> "Too many requests — wait a moment"
        ERROR_SERVER_DISCONNECTED                    -> "Recognizer service disconnected — tap Speak to retry"
        ERROR_LANGUAGE_NOT_SUPPORTED                 -> "Language not supported by recognizer"
        ERROR_LANGUAGE_UNAVAILABLE                   -> "Language model not yet downloaded"
        else                                         -> "Unrecognized error ($error)"
    }

    // ---------------------------------------------------------------------------
    // UI helpers
    // ---------------------------------------------------------------------------

    private fun updateUi() {
        if (NovaForegroundService.isRunning) {
            tvStatus.text = "Status: Running"
            tvAddress.text = "ws://${localIp()}:${NovaForegroundService.PORT}"
            btnToggle.text = "Stop Nova"
        } else {
            tvStatus.text = "Status: Stopped"
            tvAddress.text = ""
            btnToggle.text = "Start Nova"
        }
    }

    private fun localIp(): String {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val lp = cm.getLinkProperties(cm.activeNetwork ?: return "Unknown") ?: return "Unknown"
        return lp.linkAddresses
            .firstOrNull { it.address is Inet4Address }
            ?.address?.hostAddress ?: "Unknown"
    }
}
