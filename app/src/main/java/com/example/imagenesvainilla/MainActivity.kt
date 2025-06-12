package com.example.imagenesvainilla

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Botón de la cámara
        val btnCamera: ImageView = findViewById(R.id.btnCamera)
        btnCamera.setOnClickListener {
            val intent = Intent(this, Pantalla_camara::class.java)
            startActivity(intent)
        }

        // Botón de configuración
        val settingsBtn: ImageView = findViewById(R.id.settingsBtn)
        settingsBtn.setOnClickListener {
            val intent = Intent(this, Pantalla_configuracion::class.java)
            startActivity(intent)
        }

        // Botón de bloc de notas
        val btnNotas: ImageView = findViewById(R.id.manualInputBtn)
        btnNotas.setOnClickListener {
            val intent = Intent(this, Pantalla_bloc_notas::class.java)
            startActivity(intent)
        }

        // Botón de historial de fotos
        val btnHistory: ImageView = findViewById(R.id.btnHistory)
        btnHistory.setOnClickListener {
            val intent = Intent(this, Pantalla_historial::class.java)
            startActivity(intent)
        }

        // Botón para abrir Pantalla_formulario
        val btnLlenarInfo: android.widget.Button = findViewById(R.id.buttonLlenarInfo)
        btnLlenarInfo.setOnClickListener {
            val intent = Intent(this, Pantalla_formulario::class.java)
            startActivity(intent)
        }

        // Botón para abrir Pantalla_lista_formularios
        val btnListaFormularios: android.widget.Button = findViewById(R.id.buttonListaFormularios)
        btnListaFormularios.setOnClickListener {
            val intent = Intent(this, Pantalla_lista_formularios::class.java)
            startActivity(intent)
        }

        //Borrar datos al hacer cambios para evitar problemas de compatibilidad
        //getSharedPreferences("SeguimientoVainilla", MODE_PRIVATE).edit().remove("seguimientos").apply()

        //Borrar datos de notificaciones al hacer cambios para evitar problemas de compatibilidad
        //getSharedPreferences("NotificacionesVainilla", MODE_PRIVATE).edit().remove("notificaciones").apply()

        // Mostrar vista previa de formularios
        mostrarFormulariosPreview()
    }

    private fun mostrarFormulariosPreview() {
        val textViewPreview: TextView = findViewById(R.id.textViewFormulariosPreview)
        val sharedPref = getSharedPreferences("SeguimientoVainilla", MODE_PRIVATE)
        val gson = Gson()
        val jsonSeguimientos = sharedPref.getString("seguimientos", null)
        val tipoLista = object : TypeToken<List<SeguimientoVainilla>>() {}.type
        val listaSeguimientos: List<SeguimientoVainilla> = if (jsonSeguimientos != null) {
            gson.fromJson(jsonSeguimientos, tipoLista)
        } else {
            emptyList()
        }

        if (listaSeguimientos.isEmpty()) {
            textViewPreview.text = "No hay formularios guardados"
            } else {
            val ultimoSeguimiento = listaSeguimientos.last()
            textViewPreview.text = """
                Último formulario:
                ID Plantación: ${ultimoSeguimiento.idPlantacion}
                Fecha: ${ultimoSeguimiento.fecha}
                Etapa: ${ultimoSeguimiento.etapaFenologica}
                Temperatura: ${ultimoSeguimiento.temperatura?.let { "$it °C" } ?: "No registrado"}
                Humedad del Suelo: ${ultimoSeguimiento.humedadSuelo?.let { "$it %" } ?: "No registrado"}
            """.trimIndent()
        }
    }
}