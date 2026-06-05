package com.nova.assistant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.nova.assistant.service.NovaForegroundService
import java.net.Inet4Address

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvAddress: TextView
    private lateinit var btnToggle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvAddress = findViewById(R.id.tvAddress)
        btnToggle = findViewById(R.id.btnToggle)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 0)
        }

        btnToggle.setOnClickListener {
            if (NovaForegroundService.isRunning) {
                stopService(Intent(this, NovaForegroundService::class.java))
            } else {
                startForegroundService(Intent(this, NovaForegroundService::class.java))
            }
            // Give the service a moment to update its state before refreshing UI.
            tvStatus.postDelayed({ updateUi() }, 300)
        }
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

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
