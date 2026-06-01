package com.example.remotecontroller

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etIpAddress: EditText
    private lateinit var btnConnect: Button
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button

    private var remoteService: RemoteService? = null
    private var isBound = false
    private var isConnected = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as RemoteService.LocalBinder
            remoteService = binder.getService()
            isBound = true

            remoteService?.onConnectionStateChanged = { connected ->
                runOnUiThread {
                    isConnected = connected
                    updateUi()
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            remoteService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        etIpAddress = findViewById(R.id.etIpAddress)
        btnConnect = findViewById(R.id.btnConnect)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)

        checkPermissions()

        val serviceIntent = Intent(this, RemoteService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)

        btnConnect.setOnClickListener {
            if (isConnected) {
                remoteService?.disconnect()
            } else {
                val ip = etIpAddress.text.toString().trim()
                if (ip.isNotEmpty()) {
                    remoteService?.connect(ip)
                }
            }
        }

        btnPrev.setOnClickListener {
            remoteService?.sendCommand("prev")
        }

        btnNext.setOnClickListener {
            remoteService?.sendCommand("next")
        }
        
        updateUi()
    }

    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun updateUi() {
        if (isConnected) {
            tvStatus.text = getString(R.string.status_connected)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
            btnConnect.text = getString(R.string.disconnect)
            etIpAddress.isEnabled = false
            btnPrev.isEnabled = true
            btnNext.isEnabled = true
        } else {
            tvStatus.text = getString(R.string.status_disconnected)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.black))
            btnConnect.text = getString(R.string.connect)
            etIpAddress.isEnabled = true
            btnPrev.isEnabled = false
            btnNext.isEnabled = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
}
