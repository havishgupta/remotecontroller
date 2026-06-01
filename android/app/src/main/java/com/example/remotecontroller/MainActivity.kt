package com.example.remotecontroller

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etIpAddress: EditText
    private lateinit var btnConnect: Button
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var btnScanQr: Button
    
    private lateinit var swInvertVolume: Switch
    private lateinit var rgNavMode: RadioGroup
    private lateinit var rbUpDown: RadioButton
    private lateinit var rbLeftRight: RadioButton

    private lateinit var prefs: SharedPreferences

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

    private val qrCodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            etIpAddress.setText(result.contents)
            // Auto connect when scanned
            if (!isConnected) {
                remoteService?.connect(result.contents)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        prefs = getSharedPreferences("RemotePrefs", Context.MODE_PRIVATE)

        tvStatus = findViewById(R.id.tvStatus)
        etIpAddress = findViewById(R.id.etIpAddress)
        btnConnect = findViewById(R.id.btnConnect)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        btnScanQr = findViewById(R.id.btnScanQr)
        
        swInvertVolume = findViewById(R.id.swInvertVolume)
        rgNavMode = findViewById(R.id.rgNavMode)
        rbUpDown = findViewById(R.id.rbUpDown)
        rbLeftRight = findViewById(R.id.rbLeftRight)

        loadSettings()

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
        
        btnScanQr.setOnClickListener {
            val options = ScanOptions()
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            options.setPrompt("Scan PC IP QR Code")
            options.setCameraId(0)
            options.setBeepEnabled(false)
            options.setBarcodeImageEnabled(true)
            qrCodeLauncher.launch(options)
        }
        
        setupSettingsListeners()
        updateUi()
    }
    
    private fun loadSettings() {
        swInvertVolume.isChecked = prefs.getBoolean("InvertVolume", false)
        val mode = prefs.getString("NavMode", "UP_DOWN")
        if (mode == "LEFT_RIGHT") {
            rbLeftRight.isChecked = true
        } else {
            rbUpDown.isChecked = true
        }
    }
    
    private fun setupSettingsListeners() {
        swInvertVolume.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("InvertVolume", isChecked).apply()
        }
        
        rgNavMode.setOnCheckedChangeListener { _, checkedId ->
            val mode = if (checkedId == R.id.rbLeftRight) "LEFT_RIGHT" else "UP_DOWN"
            prefs.edit().putString("NavMode", mode).apply()
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CAMERA)
        }
        
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
        }
    }

    private fun updateUi() {
        if (isConnected) {
            tvStatus.text = getString(R.string.status_connected)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.teal_700))
            btnConnect.text = getString(R.string.disconnect)
            etIpAddress.isEnabled = false
            btnScanQr.isEnabled = false
            btnPrev.isEnabled = true
            btnNext.isEnabled = true
        } else {
            tvStatus.text = getString(R.string.status_disconnected)
            tvStatus.setTextColor(ContextCompat.getColor(this, R.color.black))
            btnConnect.text = getString(R.string.connect)
            etIpAddress.isEnabled = true
            btnScanQr.isEnabled = true
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
