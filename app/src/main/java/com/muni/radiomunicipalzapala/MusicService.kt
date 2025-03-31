package com.muni.radiomunicipalzapala

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector

@Suppress("DEPRECATION")
@UnstableApi
class MusicService : Service() {
    private val binder = MusicBinder()
    private lateinit var player: ExoPlayer
    private var isPlaying: Boolean = false
    private val listeners = mutableListOf<PlaybackStateListener>()

    companion object {
        const val CHANNEL_ID = "radio_channel"
        private const val NOTIFICATION_ID = 1
        private const val STREAM_URL = "https://stream.zeno.fm/p0h74pf1w7zuv"
        const val ACTION_PLAY = "com.muni.radiomunicipalzapala.ACTION_PLAY"
        const val ACTION_PAUSE = "com.muni.radiomunicipalzapala.ACTION_PAUSE"
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        initializePlayer()
        createNotificationChannel()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntentAction(intent)
        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    private fun handleIntentAction(intent: Intent?) {
        when (intent?.action) {
            ACTION_PLAY -> play()
            ACTION_PAUSE -> pause()
        }
    }

    private fun initializePlayer() {
        player = ExoPlayer.Builder(this)
            .setTrackSelector(DefaultTrackSelector(this))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(STREAM_URL))
                prepare()
            }
    }

    fun play() {
        player.play()
        isPlaying = true
        listeners.forEach { it.onPlaybackStateChanged(true) }
        startForeground(NOTIFICATION_ID, createNotification())
    }

    fun pause() {
        player.pause()
        isPlaying = false
        listeners.forEach { it.onPlaybackStateChanged(false) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            stopForeground(false) // Para versiones anteriores
        }
    }

    fun isPlaying(): Boolean = isPlaying

    fun addListener(listener: PlaybackStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: PlaybackStateListener) {
        listeners.remove(listener)
    }

    private fun createNotification(): Notification {
        // Intent para abrir la actividad principal
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent para controlar reproducción/pausa desde la notificación
        val playPauseIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }

        val playPausePendingIntent = PendingIntent.getBroadcast(
            this, 0, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.radio_24px)
            .setContentTitle("Radio Municipal Zapala")
            .setContentText("Reproduciendo en segundo plano")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(pendingIntent) // Este es el cambio clave
            .addAction(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play,
                if (isPlaying) "Pausar" else "Reproducir",
                playPausePendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "Reproducción de Radio",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificación de la radio en segundo plano"
            }.also { channel ->
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                    .createNotificationChannel(channel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
        listeners.clear()
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    interface PlaybackStateListener {
        fun onPlaybackStateChanged(isPlaying: Boolean)
    }
}