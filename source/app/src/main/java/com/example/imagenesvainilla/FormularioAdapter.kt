package com.example.imagenesvainilla

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FormularioAdapter(
    private val formularios: MutableList<SeguimientoVainilla>,
    private val context: Context,
    private val onItemClick: (SeguimientoVainilla) -> Unit,
    private val onDeleteClick: (SeguimientoVainilla, Int) -> Unit,
    private val onSiClick: (SeguimientoVainilla, Int) -> Unit,
    private val onNoClick: (SeguimientoVainilla, Int) -> Unit
) : RecyclerView.Adapter<FormularioAdapter.FormularioViewHolder>() {

    private val TAG = "FormularioAdapter"

    class FormularioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewIdPlantacion: TextView = itemView.findViewById(R.id.textViewIdPlantacion)
        val textViewFecha: TextView = itemView.findViewById(R.id.textViewFecha)
        val textViewEtapa: TextView = itemView.findViewById(R.id.textViewEtapa)
        val textViewTemperatura: TextView = itemView.findViewById(R.id.textViewTemperatura)
        val textViewHumedadSuelo: TextView = itemView.findViewById(R.id.textViewHumedadSuelo)
        val textViewSiguienteRevision: TextView = itemView.findViewById(R.id.textViewSiguienteRevision)
        val textViewPregunta: TextView = itemView.findViewById(R.id.textViewPregunta)
        val buttonSi: android.widget.Button = itemView.findViewById(R.id.buttonSi)
        val buttonNo: android.widget.Button = itemView.findViewById(R.id.buttonNo)
        val linearBotonesConfirmacion: android.widget.LinearLayout = itemView.findViewById(R.id.linearBotonesConfirmacion)
        val buttonEliminar: android.widget.Button = itemView.findViewById(R.id.buttonEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormularioViewHolder {
        try {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_formulario, parent, false)
            return FormularioViewHolder(view)
        } catch (e: Exception) {
            Log.e(TAG, "Error al inflar item_formulario: ${e.message}", e)
            throw e
        }
    }

    override fun onBindViewHolder(holder: FormularioViewHolder, position: Int) {
        try {
            val formulario = formularios[position]
            holder.textViewIdPlantacion.text = "ID Plantación: ${formulario.idPlantacion}"
            holder.textViewFecha.text = "Fecha: ${formulario.fecha}"
            holder.textViewEtapa.text = "Etapa: ${formulario.etapaFenologica}"
            holder.textViewTemperatura.text = "Temperatura: ${formulario.temperatura?.let { "$it °C" } ?: "No registrado"}"
            holder.textViewHumedadSuelo.text = "Humedad del Suelo: ${formulario.humedadSuelo?.let { "$it %" } ?: "No registrado"}"

            val fechaRevision = getNotificacionFecha(formulario.fecha, formulario.etapaFenologica, formulario.temperatura, formulario.humedadSuelo)
            holder.textViewSiguienteRevision.text = if (fechaRevision != null) {
                "Siguiente revisión: $fechaRevision"
            } else {
                "Siguiente revisión: No aplica"
            }

            val sharedPref = context.getSharedPreferences("NotificacionesVainilla", Context.MODE_PRIVATE)
            val gson = Gson()
            val jsonNotificaciones = sharedPref.getString("notificaciones", null)
            val tipoLista = object : TypeToken<List<NotificacionData>>() {}.type
            val notificaciones: List<NotificacionData> = if (jsonNotificaciones != null) {
                try {
                    gson.fromJson(jsonNotificaciones, tipoLista) ?: emptyList()
                } catch (e: Exception) {
                    Log.e(TAG, "Error al parsear notificaciones: ${e.message}", e)
                    emptyList()
                }
            } else {
                emptyList()
            }
            val notificacion = notificaciones.find {
                it.idPlantacion == formulario.idPlantacion &&
                        it.etapaSiguiente == SeguimientoVainilla.gddPorEtapa[formulario.etapaFenologica]?.first
            }
            val notificacionDisparada = notificacion?.notificationId?.let {
                sharedPref.getBoolean("notificacionDisparada_$it", false)
            } ?: false
            val transicion = SeguimientoVainilla.gddPorEtapa[formulario.etapaFenologica]
            val etapaSiguiente = transicion?.first

            Log.d(TAG, "Formulario: ${formulario.idPlantacion}, Notificación ID: ${notificacion?.notificationId}, Disparada: $notificacionDisparada")

            if (notificacionDisparada && etapaSiguiente != null) {
                holder.textViewPregunta.visibility = View.VISIBLE
                holder.linearBotonesConfirmacion.visibility = View.VISIBLE
                holder.textViewPregunta.text = "¿La planta ha pasado a la siguiente fase ($etapaSiguiente)?"
                Log.d(TAG, "Mostrando botones para: ${formulario.idPlantacion}")
            } else {
                holder.textViewPregunta.visibility = View.GONE
                holder.linearBotonesConfirmacion.visibility = View.GONE
                Log.d(TAG, "Ocultando botones para: ${formulario.idPlantacion}")
            }

            holder.buttonSi.setOnClickListener {
                Log.d(TAG, "Clic en Sí para: ${formulario.idPlantacion}")
                onSiClick(formulario, position)
            }

            holder.buttonNo.setOnClickListener {
                Log.d(TAG, "Clic en No para: ${formulario.idPlantacion}")
                onNoClick(formulario, position)
            }

            holder.itemView.setOnClickListener {
                onItemClick(formulario)
            }

            holder.buttonEliminar.setOnClickListener {
                onDeleteClick(formulario, position)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al vincular datos en posición $position: ${e.message}", e)
        }
    }

    override fun getItemCount(): Int = formularios.size

    private fun getNotificacionFecha(fecha: String, etapa: String, temperatura: Float?, humedadSuelo: Float?): String? {
        try {
            val transicion = SeguimientoVainilla.gddPorEtapa[etapa] ?: return null
            val gddRequeridos = transicion.second
            val semanasAjustadas = calcularSemanasAjustadas(gddRequeridos, temperatura, humedadSuelo)
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val fechaActual = dateFormat.parse(fecha) ?: return null
            val calendar = Calendar.getInstance().apply { time = fechaActual }
            calendar.add(Calendar.WEEK_OF_YEAR, semanasAjustadas)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            return dateFormat.format(calendar.time)
        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular fecha de notificación: ${e.message}", e)
            return null
        }
    }

    private fun calcularSemanasAjustadas(gddRequeridos: Int, temperatura: Float?, humedadSuelo: Float?): Int {
        try {
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
        } catch (e: Exception) {
            Log.e(TAG, "Error al calcular semanas ajustadas: ${e.message}", e)
            return 1
        }
    }
}