package com.leiderl.CCR.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object NotificationScheduler {

    private const val ID_MIERCOLES  = 101
    private const val ID_VIERNES    = 102
    private const val ID_SABADO_3PM = 103
    private const val ID_SABADO_8PM = 104


    fun programarTodas(context: Context) {

        programar(
            context,
            ID_MIERCOLES,
            Calendar.WEDNESDAY,
            13,
            0,
            "HOY HAY SERVICIO DE SANIDAD Y MILAGRO, NO FALTES!!"
        )


        programar(
            context,
            ID_VIERNES,
            Calendar.FRIDAY,
            18,
            0,
            "MAÑANA HAY SERVICIO DE AYUNO Y ORACIÓN, NO FALTES!!"
        )


        programar(
            context,
            ID_SABADO_3PM,
            Calendar.SATURDAY,
            13,
            0,
            "HOY HAY SERVICIO DE JÓVENES, NO FALTES!!"
        )


        programar(
            context,
            ID_SABADO_8PM,
            Calendar.SATURDAY,
            18,
            0,
            "MAÑANA HAY ESCUELA DOMINICAL, NO FALTES!!"
        )
    }

    private fun programar(
        context: Context,
        id: Int,
        diaSemana: Int,
        hora: Int,
        minuto: Int,
        mensaje: String
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("mensaje",  mensaje)
            putExtra("notif_id", id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendario = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_WEEK,  diaSemana)
            set(Calendar.HOUR_OF_DAY,  hora)
            set(Calendar.MINUTE,       minuto)
            set(Calendar.SECOND,       0)
            set(Calendar.MILLISECOND,  0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.WEEK_OF_YEAR, 1)
            }
        }

        val intervaloSemanal = AlarmManager.INTERVAL_DAY * 7

        // setRepeating es la forma correcta para alarmas semanales recurrentes
        // funciona en todos los niveles de API sin permiso SCHEDULE_EXACT_ALARM
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendario.timeInMillis,
            intervaloSemanal,
            pendingIntent
        )
    }
}