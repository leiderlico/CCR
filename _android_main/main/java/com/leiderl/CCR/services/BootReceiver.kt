package com.leiderl.CCR.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// Re-programa las alarmas si el teléfono se reinicia
// (las alarmas se pierden al apagar el dispositivo)
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationScheduler.programarTodas(context)
        }
    }
}