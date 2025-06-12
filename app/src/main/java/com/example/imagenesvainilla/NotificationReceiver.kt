package com.example.imagenesvainilla

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import android.util.Log

class NotificationReceiver : BroadcastReceiver() {

    private val TAG = "NotificationReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notificación recibida para: ${intent.getStringExtra("ID_PLANTACION")}")
        val idPlantacion = intent.getStringExtra("ID_PLANTACION") ?: return
        val etapaSiguiente = intent.getStringExtra("ETAPA_SIGUIENTE") ?: return
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", 0)

        // Establecer bandera
        val sharedPref = context.getSharedPreferences("NotificacionesVainilla", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("notificacionDisparada_$notificationId", true).apply()
        Log.d(TAG, "Bandera notificacionDisparada_$notificationId establecida a true")

        // Crear canal de notificación (necesario para Android 8.0 en adelante)
        val channelId = "VainillaChannel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Seguimiento Vainilla",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones para el seguimiento de vainilla"
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Crear notificación
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Revisar Vainilla: $idPlantacion")
            .setContentText("Revisar planta para confirmar transición a $etapaSiguiente")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Mostrar notificación
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
        Log.d(TAG, "Notificación mostrada con ID: $notificationId")
    }
}