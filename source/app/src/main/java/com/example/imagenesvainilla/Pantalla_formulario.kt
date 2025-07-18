package com.example.imagenesvainilla

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.imagenesvainilla.databinding.ActivityPantallaFormularioBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

data class SeguimientoVainilla(
    val idPlantacion: String,
    val fecha: String,
    val etapaFenologica: String,
    val temperatura: Float?,
    val humedadSuelo: Float?
) {
    companion object {
        val gddPorEtapa = mapOf(
            "Plántula" to Pair("Crecimiento Vegetativo", 12600),
            "Crecimiento Vegetativo" to Pair("Yemas Florales", 25200),
            "Yemas Florales" to Pair("Floración/Polinización", 2800),
            "Floración/Polinización" to Pair("Crecimiento del Fruto", 700),
            "Crecimiento del Fruto" to Pair("Maduración", 9800),
            "Maduración" to Pair("Cosecha", 2100)
        )
        const val TEMPERATURA_BASE = 10.0f
    }
}

class Pantalla_formulario : AppCompatActivity() {

    private lateinit var binding: ActivityPantallaFormularioBinding
    private val etapasFenologicas = listOf(
        "Plántula",
        "Crecimiento Vegetativo",
        "Yemas Florales",
        "Floración/Polinización",
        "Crecimiento del Fruto",
        "Maduración",
        "Cosecha"
    )

    private val requestCalendarPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            programarNotificacionCalendario()
        } else {
            Toast.makeText(this, "Permiso de calendario denegado", Toast.LENGTH_SHORT).show()
        }
        programarNotificacionLocal()
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            programarNotificacionLocal()
        } else {
            Toast.makeText(this, "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    private var seguimientoToSave: SeguimientoVainilla? = null
    private var notificationId: Int = 0
    private var calendarEventId: Long? = null
    private var formularioOriginal: SeguimientoVainilla? = null
    private var isFromNoClick: Boolean = false
    private var fechaNotificacionManual: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPantallaFormularioBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Spinner de etapas fenológicas
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, etapasFenologicas)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerEtapa.adapter = adapter

        // Verificar si se abrió para editar
        val formularioJson = intent.getStringExtra("FORMULARIO")
        isFromNoClick = intent.getBooleanExtra("IS_FROM_NO_CLICK", false)

        if (formularioJson != null) {
            val gson = Gson()
            formularioOriginal = gson.fromJson(formularioJson, SeguimientoVainilla::class.java)

            binding.editTextIdPlantacion.setText(formularioOriginal?.idPlantacion)
            binding.editTextFecha.setText(formularioOriginal?.fecha)
            binding.spinnerEtapa.setSelection(etapasFenologicas.indexOf(formularioOriginal?.etapaFenologica))
            binding.editTextTemperatura.setText(formularioOriginal?.temperatura?.toString() ?: "")
            binding.editTextHumedadSuelo.setText(formularioOriginal?.humedadSuelo?.toString() ?: "")

            // Obtener notificationId y calendarEventId del formulario original
            val sharedPref = getSharedPreferences("NotificacionesVainilla", MODE_PRIVATE)
            val jsonNotificaciones = sharedPref.getString("notificaciones", null)
            val tipoLista = object : TypeToken<List<NotificacionData>>() {}.type
            val notificaciones: List<NotificacionData> = if (jsonNotificaciones != null) {
                gson.fromJson(jsonNotificaciones, tipoLista)
            } else {
                emptyList()
            }
            val notificacion = notificaciones.find {
                it.idPlantacion == formularioOriginal?.idPlantacion &&
                        it.etapaSiguiente == SeguimientoVainilla.gddPorEtapa[formularioOriginal?.etapaFenologica]?.first
            }
            notificationId = notificacion?.notificationId ?: System.currentTimeMillis().toInt()
            calendarEventId = notificacion?.calendarEventId

            // Mostrar campo de fecha si isFromNoClick es true
            if (isFromNoClick) {
                binding.editTextProximaNotificacion.visibility = View.VISIBLE
                binding.editTextProximaNotificacion.isEnabled = true
                // Configurar DatePickerDialog
                binding.editTextProximaNotificacion.setOnClickListener {
                    val calendar = Calendar.getInstance()
                    val year = calendar.get(Calendar.YEAR)
                    val month = calendar.get(Calendar.MONTH)
                    val day = calendar.get(Calendar.DAY_OF_MONTH)

                    val datePickerDialog = DatePickerDialog(
                        this,
                        { _, selectedYear, selectedMonth, selectedDay ->
                            val selectedDate = String.format(
                                Locale.getDefault(),
                                "%02d/%02d/%d",
                                selectedDay,
                                selectedMonth + 1,
                                selectedYear
                            )
                            binding.editTextProximaNotificacion.setText(selectedDate)
                        },
                        year,
                        month,
                        day
                    )
                    datePickerDialog.datePicker.minDate = System.currentTimeMillis()
                    datePickerDialog.show()
                }
            } else {
                binding.editTextProximaNotificacion.visibility = View.GONE
                binding.editTextProximaNotificacion.isEnabled = false
            }
        }

        // Botón Regresar
        binding.buttonRegresar.setOnClickListener {
            finish()
        }

        // Botón Guardar
        binding.buttonGuardar.setOnClickListener {
            if (guardarSeguimiento()) {
                checkAndRequestPermissions()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val calendarPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
        val notificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            PackageManager.PERMISSION_GRANTED
        }

        if (calendarPermission == PackageManager.PERMISSION_GRANTED) {
            programarNotificacionCalendario()
        } else {
            requestCalendarPermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
            return
        }

        if (notificationPermission == PackageManager.PERMISSION_GRANTED) {
            programarNotificacionLocal()
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun guardarSeguimiento(): Boolean {
        val idPlantacion = binding.editTextIdPlantacion.text.toString().trim()
        val fecha = binding.editTextFecha.text.toString().trim()
        val etapa = binding.spinnerEtapa.selectedItem.toString()
        val temperatura = binding.editTextTemperatura.text.toString().toFloatOrNull()
        val humedadSuelo = binding.editTextHumedadSuelo.text.toString().toFloatOrNull()
        fechaNotificacionManual = if (isFromNoClick) binding.editTextProximaNotificacion.text.toString().trim() else null

        if (idPlantacion.isEmpty() || fecha.isEmpty()) {
            Toast.makeText(this, "Por favor completa el ID de plantación y la fecha", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar formato de fecha del formulario
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        dateFormat.isLenient = false
        val date: Date
        try {
            date = dateFormat.parse(fecha)!!
        } catch (e: Exception) {
            Toast.makeText(this, "Formato de fecha inválido (dd/MM/yyyy)", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validar fecha de notificación manual si existe
        if (isFromNoClick && fechaNotificacionManual?.isNotEmpty() == true) {
            try {
                val fechaManual = dateFormat.parse(fechaNotificacionManual!!)!!
                val hoy = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                if (fechaManual.before(hoy.time)) {
                    Toast.makeText(this, "La fecha de notificación no puede ser anterior a hoy", Toast.LENGTH_SHORT).show()
                    return false
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Formato de fecha de notificación inválido (dd/MM/yyyy)", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        // Validar rangos de temperatura y humedad
        temperatura?.let {
            if (it < 10 || it > 40) {
                Toast.makeText(this, "Temperatura debe estar entre 10 y 40 °C", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        humedadSuelo?.let {
            if (it < 0 || it > 100) {
                Toast.makeText(this, "Humedad del suelo debe estar entre 0 y 100 %", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        // Verificar condiciones críticas
        if (temperatura != null && (temperatura < 15 || temperatura > 38)) {
            Toast.makeText(this, "Temperatura extrema detectada (<15 °C o >38 °C). Desarrollo detenido.", Toast.LENGTH_LONG).show()
            return false
        }
        if (humedadSuelo != null && humedadSuelo < 30) {
            Toast.makeText(this, "Humedad del suelo crítica (<30 %). Desarrollo detenido.", Toast.LENGTH_LONG).show()
            return false
        }

        // Crear seguimiento
        seguimientoToSave = SeguimientoVainilla(
            idPlantacion = idPlantacion,
            fecha = fecha,
            etapaFenologica = etapa,
            temperatura = temperatura,
            humedadSuelo = humedadSuelo
        )

        // Guardar en SharedPreferences
        val sharedPref = getSharedPreferences("SeguimientoVainilla", MODE_PRIVATE)
        val gson = Gson()
        val jsonSeguimientos = sharedPref.getString("seguimientos", null)
        val tipoLista = object : TypeToken<MutableList<SeguimientoVainilla>>() {}.type
        val listaSeguimientos: MutableList<SeguimientoVainilla> = if (jsonSeguimientos != null) {
            gson.fromJson(jsonSeguimientos, tipoLista)
        } else {
            mutableListOf()
        }

        if (formularioOriginal != null) {
            // Modo edición: reemplazar el formulario original
            val index = listaSeguimientos.indexOfFirst {
                it.idPlantacion == formularioOriginal?.idPlantacion &&
                        it.fecha == formularioOriginal?.fecha &&
                        it.etapaFenologica == formularioOriginal?.etapaFenologica
            }
            if (index != -1) {
                listaSeguimientos[index] = seguimientoToSave!!
            } else {
                listaSeguimientos.add(seguimientoToSave!!)
            }
        } else {
            // Añadir nuevo formulario
            listaSeguimientos.add(seguimientoToSave!!)
        }

        val jsonActualizado = gson.toJson(listaSeguimientos)
        sharedPref.edit().putString("seguimientos", jsonActualizado).apply()

        // Cancelar notificaciones previas si es modo edición
        if (formularioOriginal != null) {
            cancelarNotificacionesPrevias()
        }

        Toast.makeText(this, "Seguimiento guardado exitosamente ✅", Toast.LENGTH_SHORT).show()

        // Limpiar campos (solo en modo nuevo)
        if (formularioOriginal == null) {
            binding.editTextIdPlantacion.text.clear()
            binding.editTextFecha.text.clear()
            binding.editTextTemperatura.text.clear()
            binding.editTextHumedadSuelo.text.clear()
            binding.spinnerEtapa.setSelection(0)
            binding.editTextProximaNotificacion.text.clear()
        }

        // Generar ID único para la notificación si no está definido
        if (notificationId == 0) {
            notificationId = System.currentTimeMillis().toInt()
        }

        return true
    }

    private fun cancelarNotificacionesPrevias() {
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

        calendarEventId?.let { eventId ->
            try {
                contentResolver.delete(
                    CalendarContract.Events.CONTENT_URI,
                    "${CalendarContract.Events._ID} = ?",
                    arrayOf(eventId.toString())
                )
            } catch (e: SecurityException) {
            }
        }

        val sharedPref = getSharedPreferences("NotificacionesVainilla", MODE_PRIVATE)
        val gson = Gson()
        val jsonNotificaciones = sharedPref.getString("notificaciones", null)
        val tipoLista = object : TypeToken<MutableList<NotificacionData>>() {}.type
        val listaNotificaciones: MutableList<NotificacionData> = if (jsonNotificaciones != null) {
            gson.fromJson(jsonNotificaciones, tipoLista)
        } else {
            mutableListOf()
        }
        listaNotificaciones.removeIf { it.notificationId == notificationId }
        val jsonActualizado = gson.toJson(listaNotificaciones)
        sharedPref.edit().putString("notificaciones", jsonActualizado).apply()
    }

    private fun programarNotificacionCalendario() {
        val seguimiento = seguimientoToSave ?: return
        val etapaActual = seguimiento.etapaFenologica
        val transicion = SeguimientoVainilla.gddPorEtapa[etapaActual] ?: return
        val etapaSiguiente = transicion.first

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaNotificacion: Date = if (isFromNoClick && fechaNotificacionManual?.isNotEmpty() == true) {
            dateFormat.parse(fechaNotificacionManual!!)!!
        } else {
            val gddRequeridos = transicion.second
            val semanasAjustadas = calcularSemanasAjustadas(gddRequeridos, seguimiento.temperatura, seguimiento.humedadSuelo)
            val fechaActual = dateFormat.parse(seguimiento.fecha)!!
            val calendar = Calendar.getInstance().apply { time = fechaActual }
            calendar.add(Calendar.WEEK_OF_YEAR, semanasAjustadas)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar.time
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.DTSTART, fechaNotificacion.time)
            put(CalendarContract.Events.DTEND, fechaNotificacion.time + 60 * 60 * 1000)
            put(CalendarContract.Events.TITLE, "Revisar vainilla: ${seguimiento.idPlantacion}")
            put(CalendarContract.Events.DESCRIPTION, "Revisar planta para confirmar transición a $etapaSiguiente")
            put(CalendarContract.Events.CALENDAR_ID, 1)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }

        try {
            val uri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            calendarEventId = uri?.lastPathSegment?.toLong()
            Toast.makeText(this, "Evento en calendario programado para ${dateFormat.format(fechaNotificacion)}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error al programar evento en el calendario", Toast.LENGTH_SHORT).show()
        }

        guardarDatosNotificacion(seguimiento, etapaSiguiente, dateFormat.format(fechaNotificacion))
    }

    private fun programarNotificacionLocal() {
        val seguimiento = seguimientoToSave ?: return
        val etapaActual = seguimiento.etapaFenologica
        val transicion = SeguimientoVainilla.gddPorEtapa[etapaActual] ?: return
        val etapaSiguiente = transicion.first

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fechaNotificacion: Date = if (isFromNoClick && fechaNotificacionManual?.isNotEmpty() == true) {
            dateFormat.parse(fechaNotificacionManual!!)!!
        } else {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.SECOND, 30)
            calendar.time
        }

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, NotificationReceiver::class.java).apply {
            putExtra("ID_PLANTACION", seguimiento.idPlantacion)
            putExtra("ETAPA_SIGUIENTE", etapaSiguiente)
            putExtra("NOTIFICATION_ID", notificationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                fechaNotificacion.time,
                pendingIntent
            )
            val dateFormatDisplay = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            Toast.makeText(this, "Notificación local programada para ${dateFormatDisplay.format(fechaNotificacion)}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Toast.makeText(this, "Error al programar notificación local", Toast.LENGTH_SHORT).show()
        }

        val fechaRevision = if (isFromNoClick && fechaNotificacionManual?.isNotEmpty() == true) {
            fechaNotificacionManual
        } else {
            getNotificacionFecha(seguimiento.fecha, seguimiento.etapaFenologica, seguimiento.temperatura, seguimiento.humedadSuelo)
        }
        guardarDatosNotificacion(seguimiento, etapaSiguiente, fechaRevision ?: dateFormat.format(fechaNotificacion))

        val sharedPref = getSharedPreferences("NotificacionesVainilla", MODE_PRIVATE)
        sharedPref.edit().putBoolean("notificacionDisparada_${notificationId}", true).apply()
    }

    private fun guardarDatosNotificacion(seguimiento: SeguimientoVainilla, etapaSiguiente: String, fechaNotificacion: String) {
        val sharedPref = getSharedPreferences("NotificacionesVainilla", MODE_PRIVATE)
        val gson = Gson()
        val jsonNotificaciones = sharedPref.getString("notificaciones", null)
        val tipoLista = object : TypeToken<MutableList<NotificacionData>>() {}.type
        val listaNotificaciones: MutableList<NotificacionData> = if (jsonNotificaciones != null) {
            gson.fromJson(jsonNotificaciones, tipoLista)
        } else {
            mutableListOf()
        }

        listaNotificaciones.removeIf { it.notificationId == notificationId }

        val notificacionData = NotificacionData(
            notificationId = notificationId,
            idPlantacion = seguimiento.idPlantacion,
            etapaSiguiente = etapaSiguiente,
            fechaNotificacion = fechaNotificacion,
            calendarEventId = calendarEventId
        )
        listaNotificaciones.add(notificacionData)
        val jsonActualizado = gson.toJson(listaNotificaciones)
        sharedPref.edit().putString("notificaciones", jsonActualizado).apply()
    }

    private fun getNotificacionFecha(fecha: String?, etapa: String?, temperatura: Float?, humedadSuelo: Float?): String? {
        if (fecha == null || etapa == null) return null
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