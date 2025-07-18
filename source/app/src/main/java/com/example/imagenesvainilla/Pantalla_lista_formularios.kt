package com.example.imagenesvainilla

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.imagenesvainilla.databinding.ActivityPantallaListaFormulariosBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class Pantalla_lista_formularios : AppCompatActivity() {

    private lateinit var binding: ActivityPantallaListaFormulariosBinding
    private lateinit var adapter: FormularioAdapter
    private lateinit var formularios: MutableList<SeguimientoVainilla>
    private val TAG = "PantallaListaFormularios"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            binding = ActivityPantallaListaFormulariosBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Configurar RecyclerView
            binding.recyclerViewFormularios.layoutManager = LinearLayoutManager(this)

            // Cargar formularios desde SharedPreferences
            val sharedPref = getSharedPreferences("SeguimientoVainilla", MODE_PRIVATE)
            val gson = Gson()
            val jsonSeguimientos = sharedPref.getString("seguimientos", null)
            val tipoLista = object : TypeToken<MutableList<SeguimientoVainilla>>() {}.type
            formularios = try {
                if (jsonSeguimientos != null) {
                    gson.fromJson(jsonSeguimientos, tipoLista) ?: mutableListOf()
                } else {
                    mutableListOf()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear formularios desde SharedPreferences: ${e.message}", e)
                mutableListOf()
            }

            // Configurar adaptador
            adapter = FormularioAdapter(
                formularios,
                context = this,
                onItemClick = { formulario ->
                    val intent = Intent(this, Pantalla_formulario::class.java)
                    intent.putExtra("FORMULARIO", gson.toJson(formulario))
                    intent.putExtra("IS_FROM_NO_CLICK", false)
                    startActivity(intent)
                },
                onDeleteClick = { formulario, position ->
                    eliminarFormulario(formulario, position)
                },
                onSiClick = { formulario, position ->
                    actualizarASiguienteFase(formulario, position)
                },
                onNoClick = { formulario, position ->
                    reprogramarNotificacion(formulario, position)
                }
            )

            // Mostrar mensaje si no hay formularios
            if (formularios.isEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewFormularios.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewFormularios.visibility = View.VISIBLE
                binding.recyclerViewFormularios.adapter = adapter
            }

            // Botón Regresar
            binding.buttonRegresar.setOnClickListener {
                finish()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error en onCreate: ${e.message}", e)
            Toast.makeText(this, "Error al abrir la lista de formularios", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun eliminarFormulario(formulario: SeguimientoVainilla, position: Int) {
        Log.d(TAG, "Eliminando formulario: ${formulario.idPlantacion}")
        val sharedPref = getSharedPreferences("NotificacionesVainilla", MODE_PRIVATE)
        val gson = Gson()
        val jsonNotificaciones = sharedPref.getString("notificaciones", null)
        val tipoLista = object : TypeToken<MutableList<NotificacionData>>() {}.type
        val listaNotificaciones: MutableList<NotificacionData> = if (jsonNotificaciones != null) {
            try {
                gson.fromJson(jsonNotificaciones, tipoLista) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear notificaciones: ${e.message}", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        // Encontrar notificación asociada
        val notificacion = listaNotificaciones.find {
            it.idPlantacion == formulario.idPlantacion &&
                    it.etapaSiguiente == SeguimientoVainilla.gddPorEtapa[formulario.etapaFenologica]?.first
        }

        // Cancelar notificación local
        notificacion?.notificationId?.let { notificationId ->
            Log.d(TAG, "Cancelando notificación ID: $notificationId")
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
            sharedPref.edit().remove("notificacionDisparada_$notificationId").apply()
        }

        // Eliminar evento de calendario
        notificacion?.calendarEventId?.let { eventId ->
            try {
                contentResolver.delete(
                    CalendarContract.Events.CONTENT_URI,
                    "${CalendarContract.Events._ID} = ?",
                    arrayOf(eventId.toString())
                )
                Log.d(TAG, "Evento de calendario eliminado: $eventId")
            } catch (e: SecurityException) {
                Log.e(TAG, "Error al eliminar evento de calendario: ${e.message}")
            }
        }

        // Eliminar notificación de SharedPreferences
        listaNotificaciones.removeIf { it.notificationId == notificacion?.notificationId }
        val jsonActualizadoNotificaciones = gson.toJson(listaNotificaciones)
        sharedPref.edit().putString("notificaciones", jsonActualizadoNotificaciones).apply()

        // Eliminar formulario de SharedPreferences
        val sharedPrefFormularios = getSharedPreferences("SeguimientoVainilla", MODE_PRIVATE)
        formularios.removeAt(position)
        val jsonActualizadoFormularios = gson.toJson(formularios)
        sharedPrefFormularios.edit().putString("seguimientos", jsonActualizadoFormularios).apply()

        // Actualizar RecyclerView
        adapter.notifyItemRemoved(position)
        if (formularios.isEmpty()) {
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.recyclerViewFormularios.visibility = View.GONE
        }

        Toast.makeText(this, "Formulario eliminado exitosamente", Toast.LENGTH_SHORT).show()
    }

    private fun actualizarASiguienteFase(formulario: SeguimientoVainilla, position: Int) {
        Log.d(TAG, "Actualizando formulario a siguiente fase: ${formulario.idPlantacion}")
        val transicion = SeguimientoVainilla.gddPorEtapa[formulario.etapaFenologica] ?: return
        val etapaSiguiente = transicion.first

        val nuevoFormulario = SeguimientoVainilla(
            idPlantacion = formulario.idPlantacion,
            fecha = formulario.fecha,
            etapaFenologica = etapaSiguiente,
            temperatura = formulario.temperatura,
            humedadSuelo = formulario.humedadSuelo
        )

        val sharedPref = getSharedPreferences("SeguimientoVainilla", MODE_PRIVATE)
        val gson = Gson()
        val jsonSeguimientos = sharedPref.getString("seguimientos", null)
        val tipoLista = object : TypeToken<MutableList<SeguimientoVainilla>>() {}.type
        val listaSeguimientos: MutableList<SeguimientoVainilla> = if (jsonSeguimientos != null) {
            try {
                gson.fromJson(jsonSeguimientos, tipoLista) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear seguimientos: ${e.message}", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        listaSeguimientos[position] = nuevoFormulario
        val jsonActualizado = gson.toJson(listaSeguimientos)
        sharedPref.edit().putString("seguimientos", jsonActualizado).apply()

        cancelarNotificacion(formulario)
        programarNuevaNotificacion(nuevoFormulario, etapaSiguiente)

        formularios[position] = nuevoFormulario
        adapter.notifyItemChanged(position)

        Toast.makeText(this, "Formulario actualizado a $etapaSiguiente", Toast.LENGTH_SHORT).show()
    }

    private fun reprogramarNotificacion(formulario: SeguimientoVainilla, position: Int) {
        Log.d(TAG, "Reprogramando notificación para: ${formulario.idPlantacion}")
        val sharedPref = getSharedPreferences("NotificacionesVainilla", MODE_PRIVATE)
        val gson = Gson()
        val jsonNotificaciones = sharedPref.getString("notificaciones", null)
        val tipoLista = object : TypeToken<MutableList<NotificacionData>>() {}.type
        val listaNotificaciones: MutableList<NotificacionData> = if (jsonNotificaciones != null) {
            try {
                gson.fromJson(jsonNotificaciones, tipoLista) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear notificaciones: ${e.message}", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        val notificacion = listaNotificaciones.find {
            it.idPlantacion == formulario.idPlantacion &&
                    it.etapaSiguiente == SeguimientoVainilla.gddPorEtapa[formulario.etapaFenologica]?.first
        }

        if (notificacion != null) {
            Log.d(TAG, "Notificación encontrada, ID: ${notificacion.notificationId}")
            cancelarNotificacion(formulario, keepFlag = false)

            val intent = Intent(this, Pantalla_formulario::class.java)
            intent.putExtra("FORMULARIO", gson.toJson(formulario))
            intent.putExtra("IS_FROM_NO_CLICK", true)
            startActivity(intent)

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaRevision = getNotificacionFecha(formulario.fecha, formulario.etapaFenologica, formulario.temperatura, formulario.humedadSuelo)
            val fechaNotificacion = dateFormat.parse(fechaRevision ?: notificacion.fechaNotificacion) ?: run {
                Log.e(TAG, "Error al parsear fecha de notificación: ${notificacion.fechaNotificacion}")
                return
            }
            val calendar = Calendar.getInstance().apply { time = fechaNotificacion }
            val nuevaFechaNotificacion = calendar.time
            Log.d(TAG, "Fecha de notificación para Toast: ${dateFormat.format(nuevaFechaNotificacion)}")

            val nuevoNotificationId = System.currentTimeMillis().toInt()
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val intentNotif = Intent(this, NotificationReceiver::class.java).apply {
                putExtra("ID_PLANTACION", formulario.idPlantacion)
                putExtra("ETAPA_SIGUIENTE", notificacion.etapaSiguiente)
                putExtra("NOTIFICATION_ID", nuevoNotificationId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                nuevoNotificationId,
                intentNotif,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendarLocal = Calendar.getInstance()
            calendarLocal.add(Calendar.SECOND, 30)
            try {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendarLocal.timeInMillis,
                    pendingIntent
                )
                Log.d(TAG, "Notificación local reprogramada para: ${calendarLocal.time}, ID: $nuevoNotificationId")
                Toast.makeText(this, "Notificación reprogramada para ${dateFormat.format(nuevaFechaNotificacion)}", Toast.LENGTH_SHORT).show()
            } catch (e: SecurityException) {
                Log.e(TAG, "Error al reprogramar notificación: ${e.message}")
                Toast.makeText(this, "Error al reprogramar notificación", Toast.LENGTH_SHORT).show()
            }

            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, nuevaFechaNotificacion.time)
                put(CalendarContract.Events.DTEND, nuevaFechaNotificacion.time + 60 * 60 * 1000)
                put(CalendarContract.Events.TITLE, "Revisar vainilla: ${formulario.idPlantacion}")
                put(CalendarContract.Events.DESCRIPTION, "Revisar planta para confirmar transición a ${notificacion.etapaSiguiente}")
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }

            var nuevoCalendarEventId: Long? = null
            try {
                val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                nuevoCalendarEventId = uri?.lastPathSegment?.toLong()
                Log.d(TAG, "Evento de calendario reprogramado: $nuevoCalendarEventId")
            } catch (e: SecurityException) {
                Log.e(TAG, "Error al reprogramar evento en calendario: ${e.message}")
                Toast.makeText(this, "Error al reprogramar evento en calendario", Toast.LENGTH_SHORT).show()
            }

            listaNotificaciones.removeIf { it.notificationId == notificacion.notificationId }
            val nuevaNotificacion = NotificacionData(
                notificationId = nuevoNotificationId,
                idPlantacion = formulario.idPlantacion,
                etapaSiguiente = notificacion.etapaSiguiente,
                fechaNotificacion = dateFormat.format(nuevaFechaNotificacion),
                calendarEventId = nuevoCalendarEventId
            )
            listaNotificaciones.add(nuevaNotificacion)
            val jsonActualizadoNotificaciones = gson.toJson(listaNotificaciones)
            sharedPref.edit()
                .putString("notificaciones", jsonActualizadoNotificaciones)
                .apply()
            Log.d(TAG, "Notificación guardada, bandera notificacionDisparada_$nuevoNotificationId se establecerá en NotificationReceiver")

            adapter.notifyDataSetChanged()
        } else {
            Log.e(TAG, "No se encontró notificación para: ${formulario.idPlantacion}")
        }
    }

    private fun cancelarNotificacion(formulario: SeguimientoVainilla, keepFlag: Boolean = false) {
        Log.d(TAG, "Cancelando notificación para: ${formulario.idPlantacion}, keepFlag: $keepFlag")
        val sharedPref = getSharedPreferences("NotificacionesVainilla", MODE_PRIVATE)
        val gson = Gson()
        val jsonNotificaciones = sharedPref.getString("notificaciones", null)
        val tipoLista = object : TypeToken<MutableList<NotificacionData>>() {}.type
        val listaNotificaciones: MutableList<NotificacionData> = if (jsonNotificaciones != null) {
            try {
                gson.fromJson(jsonNotificaciones, tipoLista) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear notificaciones: ${e.message}", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        val notificacion = listaNotificaciones.find {
            it.idPlantacion == formulario.idPlantacion &&
                    it.etapaSiguiente == SeguimientoVainilla.gddPorEtapa[formulario.etapaFenologica]?.first
        }

        notificacion?.notificationId?.let { notificationId ->
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            val intent = Intent(this, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                notificationId,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d(TAG, "Notificación local cancelada: $notificationId")
            }
            if (!keepFlag) {
                sharedPref.edit().remove("notificacionDisparada_$notificationId").apply()
                Log.d(TAG, "Bandera notificacionDisparada_$notificationId eliminada")
            }
        }

        notificacion?.calendarEventId?.let { eventId ->
            try {
                contentResolver.delete(
                    CalendarContract.Events.CONTENT_URI,
                    "${CalendarContract.Events._ID} = ?",
                    arrayOf(eventId.toString())
                )
                Log.d(TAG, "Evento de calendario eliminado: $eventId")
            } catch (e: SecurityException) {
                Log.e(TAG, "Error al eliminar evento de calendario: ${e.message}")
            }
        }

        listaNotificaciones.removeIf { it.notificationId == notificacion?.notificationId }
        val jsonActualizadoNotificaciones = gson.toJson(listaNotificaciones)
        sharedPref.edit().putString("notificaciones", jsonActualizadoNotificaciones).apply()
    }

    private fun programarNuevaNotificacion(formulario: SeguimientoVainilla, etapaSiguiente: String) {
        Log.d(TAG, "Programando nueva notificación para: ${formulario.idPlantacion}, etapa: $etapaSiguiente")
        val gddRequeridos = SeguimientoVainilla.gddPorEtapa[formulario.etapaFenologica]?.second ?: return
        val semanasAjustadas = calcularSemanasAjustadas(gddRequeridos, formulario.temperatura, formulario.humedadSuelo)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaActual = dateFormat.parse(formulario.fecha)!!
        val calendar = Calendar.getInstance().apply { time = fechaActual }
        calendar.add(Calendar.WEEK_OF_YEAR, semanasAjustadas)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val fechaNotificacion = calendar.time

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("ID_PLANTACION", formulario.idPlantacion)
            putExtra("ETAPA_SIGUIENTE", etapaSiguiente)
            putExtra("NOTIFICATION_ID", System.currentTimeMillis().toInt())
        }
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", System.currentTimeMillis().toInt())
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendarLocal = Calendar.getInstance()
        calendarLocal.add(Calendar.SECOND, 30)
        try {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendarLocal.timeInMillis,
                pendingIntent
            )
            Log.d(TAG, "Nueva notificación local programada: $notificationId")
            Toast.makeText(this, "Nueva notificación local programada", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "Error al programar nueva notificación: ${e.message}")
            Toast.makeText(this, "Error al programar nueva notificación", Toast.LENGTH_SHORT).show()
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, fechaNotificacion.time)
            put(CalendarContract.Events.DTEND, fechaNotificacion.time + 60 * 60 * 1000)
            put(CalendarContract.Events.TITLE, "Revisar vainilla: ${formulario.idPlantacion}")
            put(CalendarContract.Events.DESCRIPTION, "Revisar planta para confirmar transición a $etapaSiguiente")
            put(CalendarContract.Events.CALENDAR_ID, 1)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        var nuevoCalendarEventId: Long? = null
        try {
            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            nuevoCalendarEventId = uri?.lastPathSegment?.toLong()
            Log.d(TAG, "Evento de calendario programado: $nuevoCalendarEventId")
            Toast.makeText(this, "Nueva notificación en calendario para ${dateFormat.format(fechaNotificacion)}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e(TAG, "Error al programar nuevo evento en calendario: ${e.message}")
            Toast.makeText(this, "Error al programar nuevo evento en calendario", Toast.LENGTH_SHORT).show()
        }

        val sharedPref = getSharedPreferences("NotificacionesVainilla", MODE_PRIVATE)
        val gson = Gson()
        val jsonNotificaciones = sharedPref.getString("notificaciones", null)
        val tipoLista = object : TypeToken<MutableList<NotificacionData>>() {}.type
        val listaNotificaciones: MutableList<NotificacionData> = if (jsonNotificaciones != null) {
            try {
                gson.fromJson(jsonNotificaciones, tipoLista) ?: mutableListOf()
            } catch (e: Exception) {
                Log.e(TAG, "Error al parsear notificaciones: ${e.message}", e)
                mutableListOf()
            }
        } else {
            mutableListOf()
        }

        val nuevaNotificacion = NotificacionData(
            notificationId = notificationId,
            idPlantacion = formulario.idPlantacion,
            etapaSiguiente = etapaSiguiente,
            fechaNotificacion = dateFormat.format(fechaNotificacion),
            calendarEventId = nuevoCalendarEventId
        )
        listaNotificaciones.add(nuevaNotificacion)
        val jsonActualizadoNotificaciones = gson.toJson(listaNotificaciones)
        sharedPref.edit()
            .putString("notificaciones", jsonActualizadoNotificaciones)
            .apply()
        Log.d(TAG, "Notificación guardada, bandera notificacionDisparada_$notificationId se establecerá en NotificationReceiver")
    }

    private fun getNotificacionFecha(fecha: String, etapa: String, temperatura: Float?, humedadSuelo: Float?): String? {
        val transicion = SeguimientoVainilla.gddPorEtapa[etapa] ?: return null
        val gddRequeridos = transicion.second
        val semanasAjustadas = calcularSemanasAjustadas(gddRequeridos, temperatura, humedadSuelo)
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaActual = dateFormat.parse(fecha) ?: return null
        val calendar = Calendar.getInstance().apply { time = fechaActual }
        calendar.add(Calendar.WEEK_OF_YEAR, semanasAjustadas)
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(calendar.time)
    }

    private fun calcularSemanasAjustadas(gddRequeridos: Int, temperatura: Float?, humedadSuelo: Float?): Int {
        if (temperatura == null) {
            return gddRequeridos / 350
        }

        val gddPorSemana = (temperatura - SeguimientoVainilla.TEMPERATURA_BASE) * 7
        if (gddPorSemana <= 0) return Int.MAX_VALUE

        var semanas = (gddRequeridos / gddPorSemana).toDouble()

        humedadSuelo?.let {
            val factorHumedad = when {
                it < 30 -> 0.0
                it < 50 -> 0.5
                it in 50.0..70.0 -> 0.8
                it > 90 -> 1.1
                else -> 1.0
            }
            if (factorHumedad == 0.0) return Int.MAX_VALUE
            semanas /= factorHumedad
        }

        return semanas.toInt().coerceAtLeast(1)
    }
}