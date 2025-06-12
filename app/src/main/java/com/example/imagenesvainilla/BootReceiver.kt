package com.example.imagenesvainilla

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            // Cargar notificaciones guardadas
            val sharedPref = context.getSharedPreferences("NotificacionesVainilla", Context.MODE_PRIVATE)
            val gson = Gson()
            val jsonNotificaciones = sharedPref.getString("notificaciones", null)
            val tipoLista = object : TypeToken<List<NotificacionData>>() {}.type
            val notificaciones: List<NotificacionData> = if (jsonNotificaciones != null) {
                gson.fromJson(jsonNotificaciones, tipoLista)
            } else {
                emptyList()
            }

            // Reprogramar cada notificación
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            notificaciones.forEach { notificacion ->
                val fechaNotificacion = dateFormat.parse(notificacion.fechaNotificacion) ?: return@forEach
                val intent = Intent(context, NotificationReceiver::class.java).apply {
                    putExtra("ID_PLANTACION", notificacion.idPlantacion)
                    putExtra("ETAPA_SIGUIENTE", notificacion.etapaSiguiente)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificacion.notificationId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        fechaNotificacion.time,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    // Manejar error si es necesario
                }
            }
        }
    }
}

data class NotificacionData(
    val notificationId: Int,
    val idPlantacion: String,
    val etapaSiguiente: String,
    val fechaNotificacion: String,
    val calendarEventId: Long? // ID del evento de calendario
)