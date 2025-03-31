package com.muni.radiomunicipalzapala

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import com.airbnb.lottie.LottieAnimationView


@UnstableApi
class MainActivity : AppCompatActivity(), MusicService.PlaybackStateListener {

    private lateinit var btnPlay: ImageButton
    private lateinit var lottieAnimationView: LottieAnimationView
    private var isPlaying = false
    private var musicService: MusicService? = null
    private var isBound = false

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1
        private const val ANIMATION_DURATION = 500L
        private const val ANIMATION_DELAY = 1500L
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService().apply {
                addListener(this@MainActivity)
            }
            isBound = true
            updatePlayButtonState()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!isTaskRoot) {
            finish()
            return
        }

        handleIntentAction(intent)
        requestNotificationPermission()
        initializeViews()
        createNotificationChannel()
        startAndBindService()
    }

    private fun startAndBindService() {
        val serviceIntent = Intent(this, MusicService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE)
    }

    private fun handleIntentAction(intent: Intent?) {
        when (intent?.action) {
            MusicService.ACTION_PAUSE -> musicService?.pause()
            MusicService.ACTION_PLAY -> musicService?.play()
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun initializeViews() {
        btnPlay = findViewById(R.id.btnPlay)
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        btnPlay.setOnClickListener { togglePlayback() }
    }

    private fun togglePlayback() {
        if (isPlaying) {
            musicService?.pause()
        } else {
            musicService?.play()
        }
    }

    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        runOnUiThread {
            this.isPlaying = isPlaying
            updatePlayButtonState()
        }
    }

    private fun updatePlayButtonState() {
        btnPlay.setImageResource(if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play)

        if (isPlaying) {
            fadeInAnimation()
            lottieAnimationView.postDelayed({
                if (!lottieAnimationView.isAnimating) {
                    lottieAnimationView.playAnimation()
                }
            }, ANIMATION_DELAY)
        } else {
            fadeOutAnimation()
        }
    }

    private fun fadeInAnimation() {
        ObjectAnimator.ofFloat(lottieAnimationView, View.ALPHA, 0f, 1f).apply {
            duration = ANIMATION_DURATION
            start()
        }
        lottieAnimationView.playAnimation()
    }

    private fun fadeOutAnimation() {
        ObjectAnimator.ofFloat(lottieAnimationView, View.ALPHA, 1f, 0f).apply {
            duration = ANIMATION_DURATION
            addListener(onAnimationEnd { lottieAnimationView.pauseAnimation() })
            start()
        }
    }

    private inline fun onAnimationEnd(crossinline action: () -> Unit) =
        object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) = action()
        }

    // Métodos para redes sociales (se mantienen igual)
    private fun launchUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }

    private fun performAnimatedClick(animationView: LottieAnimationView, action: () -> Unit) {
        animationView.playAnimation()
        (animationView.parent as View).performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        action()
    }

    fun muniWeb(v: View) = launchUrl("https://www.zapala.gob.ar")
    fun emailClick(view: View) = performAnimatedClick(findViewById(R.id.icMail)) {
        Intent(Intent.ACTION_SENDTO).apply {
            data = "mailto:fmradiomunicipalzapala@gmail.com".toUri()
            putExtra(Intent.EXTRA_SUBJECT, "Subject")
            putExtra(Intent.EXTRA_TEXT, "Body")
        }.let { startActivity(Intent.createChooser(it, "Enviar email via")) }
    }

    fun telClick(v: View) = performAnimatedClick(findViewById(R.id.icTel)) {
        launchUrl("tel:2942636749")
    }

    fun wwwClick(v: View) = performAnimatedClick(findViewById(R.id.icWeb)) {
        launchUrl("https://prensazapala.wixsite.com/radiomunicipalzapala")
    }

    fun wpClick(v: View) = performAnimatedClick(findViewById(R.id.icWhatsapp)) {
        launchUrl("https://api.whatsapp.com/send?phone=+542942636749")
    }

    fun fbClick(view: View) = performAnimatedClick(findViewById(R.id.buttonFacebook)) {
        launchUrl(getFacebookUrl())
    }

    fun igClick(view: View) = performAnimatedClick(findViewById(R.id.buttonInstagram)) {
        launchUrl("https://www.instagram.com/radio.municipal.zapala")
    }

    private fun getFacebookUrl() = try {
        packageManager.getPackageInfo("com.facebook.katana", 0)
        "fb://page/100139338201213"
    } catch (_: PackageManager.NameNotFoundException) {
        "https://www.facebook.com/radiomunicipalzapala"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                MusicService.CHANNEL_ID,
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
        musicService?.removeListener(this)
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            val message =
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    "Notification permission granted"
                } else {
                    "Notification permission denied"
                }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}