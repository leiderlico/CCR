package com.leiderl.CCR.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.leiderl.CCR.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mensaje = intent.getStringExtra("mensaje") ?: return
        val id      = intent.getIntExtra("notif_id", 0)
        mostrarNotificacion(context, mensaje, id)
    }

    private fun mostrarNotificacion(context: Context, mensaje: String, id: Int) {
        val channelId = "ccr_servicios"
        val manager   = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Servicios CCR",
                NotificationManager.IMPORTANCE_HIGH  // HIGH para que suene y aparezca en pantalla
            ).apply {
                description = "Recordatorios de servicios de la iglesia"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_devocionales)  // ícono monocromático — mejor para notificaciones
            .setContentTitle("Iglesia CCR")
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(mensaje))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .build()

        manager.notify(id, notif)
    }
}