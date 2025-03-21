package com.radiomunicipalzapala

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.radiomunicipalzapala.R

class MainActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private val radioUrl = "https://stream.zeno.fm/p0h74pf1w7zuv"
    private lateinit var btnPlay: ImageButton
    private var isPlaying = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Vincular vistas (ahora solo el botón)
        btnPlay = findViewById(R.id.btnPlay)

        // Inicializar ExoPlayer sin vincular a PlayerView
        player = ExoPlayer.Builder(this).build()

        // Configurar el MediaItem con la URL de la radio
        val mediaItem = MediaItem.fromUri(radioUrl)
        player.setMediaItem(mediaItem)
        player.prepare()

        // Configurar listener para actualizar UI cuando cambie el estado
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                updatePlayButtonState()
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                updatePlayButtonState()
            }
        })

        // Manejar botón de reproducción/pausa
        btnPlay.setOnClickListener {
            if (isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }

        // Establecer estado inicial del botón
        updatePlayButtonState()
    }

    private fun updatePlayButtonState() {
        if (isPlaying) {
            btnPlay.setImageResource(R.drawable.ic_pause) // Icono de pausa
        } else {
            btnPlay.setImageResource(R.drawable.ic_play) // Icono de reproducción
        }
    }

    override fun onResume() {
        super.onResume()
        // Opcionalmente, restaurar el estado de reproducción si es necesario
    }

    override fun onPause() {
        super.onPause()
        // Opcionalmente, guardar el estado de reproducción
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release() // Liberar recursos del reproductor
    }
}