package com.example.remotecontroller

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.*
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.VolumeProviderCompat
import okhttp3.*
import java.util.concurrent.TimeUnit

class RemoteService : Service() {

    private val binder = LocalBinder()
    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    private var mediaSession: MediaSessionCompat? = null

    // Callbacks for UI updates
    var onConnectionStateChanged: ((Boolean) -> Unit)? = null
    var onTimerTick: ((String) -> Unit)? = null

    // Timer and Chord logic
    private var countDownTimer: CountDownTimer? = null
    var presentationMinutes = 10 // Default duration
    
    private var volumeHandler = Handler(Looper.getMainLooper())
    private var pendingVolumeRunnable: Runnable? = null
    private var lastVolumeDirection = 0
    private var lastVolumeEventTime = 0L

    inner class LocalBinder : Binder() {
        fun getService(): RemoteService = this@RemoteService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1, createNotification())
        setupMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnect()
        countDownTimer?.cancel()
        mediaSession?.release()
    }

    fun connect(ipAddress: String) {
        val request = Request.Builder().url("ws://$ipAddress:8765").build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                onConnectionStateChanged?.invoke(true)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onConnectionStateChanged?.invoke(false)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onConnectionStateChanged?.invoke(false)
            }
        })
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
        onConnectionStateChanged?.invoke(false)
    }

    fun sendCommand(command: String) {
        webSocket?.send(command)
    }

    private fun setupMediaSession() {
        mediaSession = MediaSessionCompat(this, "RemoteControllerMediaSession").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            
            val state = PlaybackStateCompat.Builder()
                .setActions(PlaybackStateCompat.ACTION_PLAY or PlaybackStateCompat.ACTION_PLAY_PAUSE)
                .setState(PlaybackStateCompat.STATE_PLAYING, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1.0f)
                .build()
            setPlaybackState(state)

            val volumeProvider = object : VolumeProviderCompat(VOLUME_CONTROL_RELATIVE, 100, 50) {
                override fun onAdjustVolume(direction: Int) {
                    val currentTime = System.currentTimeMillis()
                    
                    // Chord detection: If direction changes within 350ms, it's a combo
                    if (currentTime - lastVolumeEventTime < 350 && lastVolumeDirection != direction && lastVolumeDirection != 0) {
                        // Chord detected! Cancel pending slide move and start timer.
                        pendingVolumeRunnable?.let { volumeHandler.removeCallbacks(it) }
                        lastVolumeDirection = 0
                        
                        // Inform UI to update via handler to avoid thread issues
                        Handler(Looper.getMainLooper()).post {
                            startTimer(presentationMinutes)
                        }
                    } else {
                        // Normal press. Delay slightly to see if it becomes a chord.
                        lastVolumeDirection = direction
                        lastVolumeEventTime = currentTime
                        pendingVolumeRunnable?.let { volumeHandler.removeCallbacks(it) }
                        
                        pendingVolumeRunnable = Runnable {
                            if (direction == 1) sendCommand("next")
                            else if (direction == -1) sendCommand("prev")
                            lastVolumeDirection = 0
                        }
                        volumeHandler.postDelayed(pendingVolumeRunnable!!, 250) // 250ms chord window
                    }
                }
            }
            setPlaybackToRemote(volumeProvider)
            isActive = true
        }
    }

    fun startTimer(minutes: Int) {
        presentationMinutes = minutes
        countDownTimer?.cancel()
        
        // Haptic feedback to confirm start (500ms)
        vibrate(500)
        
        val totalMillis = minutes * 60 * 1000L
        countDownTimer = object : CountDownTimer(totalMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val mins = millisUntilFinished / 1000 / 60
                val secs = (millisUntilFinished / 1000) % 60
                onTimerTick?.invoke(String.format("%02d:%02d", mins, secs))
                
                // Haptic feedback at exactly 1 minute left (long vibrate)
                if (mins == 1L && secs == 0L) {
                    vibrate(1000)
                }
            }

            override fun onFinish() {
                onTimerTick?.invoke("00:00")
                // Double long vibration on finish
                vibrate(1000)
                Handler(Looper.getMainLooper()).postDelayed({ vibrate(1000) }, 1500)
            }
        }.start()
    }

    fun stopTimer() {
        countDownTimer?.cancel()
        onTimerTick?.invoke("Stopped")
    }

    private fun vibrate(duration: Long) {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "remote_service_channel",
                "Remote Controller Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "remote_service_channel")
            .setContentTitle("Remote Controller")
            .setContentText("Connected and listening for volume keys")
            .setSmallIcon(android.R.drawable.sym_def_app_icon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
