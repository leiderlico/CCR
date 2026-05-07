package com.leiderl.CCR.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Receiver EXTERNO (clase separada, no inner class) para los botones
 * de la notificación de radio. Recibe el broadcast y lo reenvía al
 * servicio como startForegroundService para que lo maneje en onStartCommand.
 */
class RadioButtonReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val serviceIntent = Intent(context, RadioService::class.java).apply {
            this.action = action
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}