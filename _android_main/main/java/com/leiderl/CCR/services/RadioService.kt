package com.leiderl.CCR.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.leiderl.CCR.R
import com.leiderl.CCR.ui.MainActivity

class RadioService : Service() {

    companion object {
        const val CHANNEL_ID        = "radio_channel"
        const val NOTIF_ID          = 101
        const val ACTION_PREV       = "com.leiderl.CCR.RADIO_PREV"
        const val ACTION_STOP       = "com.leiderl.CCR.RADIO_STOP"
        const val ACTION_NEXT       = "com.leiderl.CCR.RADIO_NEXT"
        const val EXTRA_URL         = "stream_url"
        const val EXTRA_NOMBRE      = "stream_nombre"
        const val EXTRA_FRECUENCIA  = "stream_frecuencia"
        const val EXTRA_INDEX       = "stream_index"
        const val EXTRA_URLS        = "stream_urls"
        const val EXTRA_NOMBRES     = "stream_nombres"
        const val EXTRA_FRECUENCIAS = "stream_frecuencias"

        var estaActiva = false

        fun iniciar(
            context: Context,
            url: String,
            nombre: String,
            frecuencia: String,
            index: Int = 0,
            urls: ArrayList<String> = arrayListOf(),
            nombres: ArrayList<String> = arrayListOf(),
            frecuencias: ArrayList<String> = arrayListOf()
        ) {
            val intent = Intent(context, RadioService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_NOMBRE, nombre)
                putExtra(EXTRA_FRECUENCIA, frecuencia)
                putExtra(EXTRA_INDEX, index)
                putStringArrayListExtra(EXTRA_URLS, urls)
                putStringArrayListExtra(EXTRA_NOMBRES, nombres)
                putStringArrayListExtra(EXTRA_FRECUENCIAS, frecuencias)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun detener(context: Context) {
            context.stopService(Intent(context, RadioService::class.java))
        }
    }

    inner class RadioBinder : Binder() {
        fun getService(): RadioService = this@RadioService
    }

    private val binder       = RadioBinder()
    private var player       : ExoPlayer? = null
    private var mediaSession : MediaSessionCompat? = null

    private var nombreActual     = ""
    private var frecuenciaActual = ""
    private var indiceActual     = 0
    private var listaUrls        : List<String> = emptyList()
    private var listaNombres     : List<String> = emptyList()
    private var listaFrecuencias : List<String> = emptyList()

    var onEstadoCambiado  : ((Int) -> Unit)? = null
    var onError           : (() -> Unit)?    = null
    var onEstacionCambiada: ((Int) -> Unit)? = null
    var onDetener         : (() -> Unit)?    = null

    val estadoActual      : Int     get() = player?.playbackState ?: Player.STATE_IDLE
    val estaReproduciendo : Boolean get() = player?.isPlaying == true
    val indiceActualPublic: Int     get() = indiceActual

    override fun onBind(intent: Intent): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        crearCanalNotificacion()

        mediaSession = MediaSessionCompat(this, "RadioCCR").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onSkipToPrevious() {
                    val total = listaUrls.size.coerceAtLeast(1)
                    cambiarEstacion((indiceActual - 1 + total) % total)
                }
                override fun onSkipToNext() {
                    val total = listaUrls.size.coerceAtLeast(1)
                    cambiarEstacion((indiceActual + 1) % total)
                }
                override fun onStop() {
                    onDetener?.invoke()
                    stopSelf()
                }
                override fun onPlay()  { player?.play() }
                override fun onPause() { onDetener?.invoke(); stopSelf() }
            })
            isActive = true
        }

        player = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    actualizarPlaybackState(state)
                    onEstadoCambiado?.invoke(state)
                    actualizarNotificacion()
                }
                override fun onPlayerError(error: PlaybackException) {
                    onError?.invoke()
                    prepare()
                    play()
                }
            })
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Acciones que llegan desde RadioButtonReceiver
        when (intent?.action) {
            ACTION_PREV -> {
                val total = listaUrls.size.coerceAtLeast(1)
                cambiarEstacion((indiceActual - 1 + total) % total)
                return START_STICKY
            }
            ACTION_NEXT -> {
                val total = listaUrls.size.coerceAtLeast(1)
                cambiarEstacion((indiceActual + 1) % total)
                return START_STICKY
            }
            ACTION_STOP -> {
                onDetener?.invoke()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Intent normal de inicio/cambio desde el Fragment
        val url        = intent?.getStringExtra(EXTRA_URL)       ?: return START_NOT_STICKY
        val nombre     = intent.getStringExtra(EXTRA_NOMBRE)     ?: ""
        val frecuencia = intent.getStringExtra(EXTRA_FRECUENCIA) ?: ""
        val index      = intent.getIntExtra(EXTRA_INDEX, 0)

        val urls        = intent.getStringArrayListExtra(EXTRA_URLS)
        val nombres     = intent.getStringArrayListExtra(EXTRA_NOMBRES)
        val frecuencias = intent.getStringArrayListExtra(EXTRA_FRECUENCIAS)
        if (!urls.isNullOrEmpty()) {
            listaUrls        = urls
            listaNombres     = nombres     ?: emptyList()
            listaFrecuencias = frecuencias ?: emptyList()
        }

        reproducir(url, nombre, frecuencia, index)
        return START_STICKY
    }

    override fun onDestroy() {
        estaActiva = false
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }

    private fun cambiarEstacion(nuevoIndex: Int) {
        if (listaUrls.isEmpty() || nuevoIndex !in listaUrls.indices) return
        reproducir(
            url        = listaUrls[nuevoIndex],
            nombre     = listaNombres.getOrElse(nuevoIndex)     { "" },
            frecuencia = listaFrecuencias.getOrElse(nuevoIndex) { "" },
            index      = nuevoIndex
        )
        onEstacionCambiada?.invoke(nuevoIndex)
    }

    private fun reproducir(url: String, nombre: String, frecuencia: String, index: Int) {
        nombreActual     = nombre
        frecuenciaActual = frecuencia
        indiceActual     = index
        estaActiva       = true
        startForeground(NOTIF_ID, construirNotificacion())
        player?.apply {
            stop()
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            play()
        }
    }

    private fun actualizarPlaybackState(state: Int) {
        val pbState = when (state) {
            Player.STATE_READY     -> PlaybackStateCompat.STATE_PLAYING
            Player.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
            else                   -> PlaybackStateCompat.STATE_STOPPED
        }
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(pbState, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1f)
                .setActions(
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP              or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT      or
                    PlaybackStateCompat.ACTION_PLAY              or
                    PlaybackStateCompat.ACTION_PAUSE
                )
                .build()
        )
    }

    private fun actualizarNotificacion() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID, construirNotificacion())
    }

    // ── PendingIntent al BroadcastReceiver externo ────────────────────────
    // Así funciona en TODAS las versiones, incluyendo Android 12+ con
    // foregroundServiceType="mediaPlayback"
    private fun makeBroadcastPending(requestCode: Int, action: String): PendingIntent {
        val intent = Intent(action).apply {
            setClass(this@RadioService, RadioButtonReceiver::class.java)
        }
        return PendingIntent.getBroadcast(
            this, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun construirNotificacion(): Notification {
        val abrirPending = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevPending = makeBroadcastPending(10, ACTION_PREV)
        val stopPending = makeBroadcastPending(11, ACTION_STOP)
        val nextPending = makeBroadcastPending(12, ACTION_NEXT)

        val subtitulo = if (frecuenciaActual.isNotEmpty()) "$nombreActual · $frecuenciaActual"
                        else nombreActual

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Radio CCR")
            .setContentText(subtitulo)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(abrirPending)
            .addAction(NotificationCompat.Action(R.drawable.ic_arrow_back, "Anterior",  prevPending))
            .addAction(NotificationCompat.Action(R.drawable.ic_close,      "Detener",   stopPending))
            .addAction(NotificationCompat.Action(R.drawable.ic_play_arrow, "Siguiente", nextPending))
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Radio en vivo", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Reproducción de radio cristiana"
                setSound(null, null)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}