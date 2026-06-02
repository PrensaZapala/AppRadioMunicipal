package com.muni.radiomunicipalzapala

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.session.MediaSession
import android.util.Log

@Suppress("DEPRECATION")
@UnstableApi
class MusicService : Service() {
    private val binder = MusicBinder()
    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private var isPlaying: Boolean = false
    private val listeners = mutableListOf<PlaybackStateListener>()

    // Audio Focus
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false

    companion object {
        const val CHANNEL_ID = "radio_channel"
        private const val NOTIFICATION_ID = 1
        private const val STREAM_URL = "http://radiomunicipal.ddns.net:5883/radio.mp3"
        const val ACTION_PLAY = "com.muni.radiomunicipalzapala.ACTION_PLAY"
        const val ACTION_PAUSE = "com.muni.radiomunicipalzapala.ACTION_PAUSE"
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        initializePlayer()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handleIntentAction(intent)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification())
            }
        } catch (e: Exception) {
            Log.e("MusicService", "Error en onStartCommand startForeground: ${e.message}")
        }
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
                addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        this@MusicService.isPlaying = isPlaying
                        listeners.forEach { it.onPlaybackStateChanged(isPlaying) }
                        
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                startForeground(
                                    NOTIFICATION_ID,
                                    createNotification(),
                                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
                                )
                            } else {
                                startForeground(NOTIFICATION_ID, createNotification())
                            }
                        } catch (e: Exception) {
                            Log.e("MusicService", "No se pudo iniciar foreground service: ${e.message}")
                        }
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("MusicService", "Error de ExoPlayer: ${error.message}", error)
                    }
                })

                val mediaSource = createMediaSource()
                setMediaSource(mediaSource)
                prepare()
            }
    }

    private fun createMediaSource(): ProgressiveMediaSource {
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("RadioMunicipalApp/1.0")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(8000)
            .setReadTimeoutMs(8000)

        val mediaItem = MediaItem.fromUri(STREAM_URL)

        return ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(mediaItem)
    }

    // ===== AUDIO FOCUS HANDLING =====

    private fun requestAudioFocus(): Boolean {
        if (hasAudioFocus) return true

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
        hasAudioFocus = false
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Recuperamos el foco, volver a reproducir
                Log.d("MusicService", "Audio focus gained")
                if (!player.isPlaying) {
                    player.play()
                }
                player.volume = 1.0f
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Perdimos el foco permanentemente, pausar
                Log.d("MusicService", "Audio focus lost permanently")
                pause()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Perdimos el foco temporalmente (ej: notificación), pausar
                Log.d("MusicService", "Audio focus lost temporarily")
                if (player.isPlaying) {
                    player.pause()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Podemos seguir reproduciendo a bajo volumen
                Log.d("MusicService", "Audio focus: can duck")
                if (player.isPlaying) {
                    player.volume = 0.3f
                }
            }
        }
    }

    fun play() {
        if (requestAudioFocus()) {
            player.playWhenReady = true
            player.play()
        }
    }

    fun pause() {
        player.pause()
        abandonAudioFocus()
    }

    fun isPlaying(): Boolean = player.isPlaying

    fun addListener(listener: PlaybackStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: PlaybackStateListener) {
        listeners.remove(listener)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_PLAY
        }

        val playPausePendingIntent = PendingIntent.getBroadcast(
            this, 0, playPauseIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val iconRes = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.radio_24px)
            .setContentTitle("Radio Municipal Zapala")
            .setContentText(if (isPlaying) "Reproduciendo en vivo" else "Pausado")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isPlaying) // Solo es permanente mientras suena
            .setContentIntent(pendingIntent)
            .addAction(
                iconRes,
                if (isPlaying) "Pausar" else "Reproducir",
                playPausePendingIntent
            )
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
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
        abandonAudioFocus()
        mediaSession.release()
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