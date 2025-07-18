package com.example.imagenesvainilla

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Pantalla_bloc_notas : AppCompatActivity() {

    private lateinit var imageViewFoto: ImageView
    private lateinit var buttonSeleccionarFoto: Button
    private lateinit var editTextNota: EditText
    private lateinit var buttonGuardarNota: Button
    private lateinit var buttonMicrofono: Button
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isRecording = false
    private var uriFotoSeleccionada: Uri? = null

    // Seleccionar foto desde historial
    private val seleccionarFotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.getStringExtra("foto_seleccionada")?.let { ruta ->
                uriFotoSeleccionada = Uri.parse(ruta)
                cargarImagenDesdeUri(uriFotoSeleccionada!!)
            }
        }
    }

    private fun cargarImagenDesdeUri(uri: Uri) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inSampleSize = 8
            }
            val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            imageViewFoto.setImageBitmap(bitmap)
            inputStream?.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al cargar la imagen. 📷", Toast.LENGTH_SHORT).show()
        }
    }

    // Solicitar permiso de grabación
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startSpeechRecognition()
        } else {
            Toast.makeText(this, "Permiso de grabación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_bloc_notas)

        // Vincular vistas
        imageViewFoto = findViewById(R.id.imageViewFoto)
        buttonSeleccionarFoto = findViewById(R.id.buttonSeleccionarFoto)
        editTextNota = findViewById(R.id.editTextNota)
        buttonGuardarNota = findViewById(R.id.buttonGuardarNota)
        buttonMicrofono = findViewById(R.id.buttonMicrofono)

        // Inicializar SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()

        // Botón seleccionar foto del historial
        buttonSeleccionarFoto.setOnClickListener {
            val intent = Intent(this, Pantalla_historial::class.java).apply {
                putExtra("modo_seleccion", true)
            }
            seleccionarFotoLauncher.launch(intent)
        }

        // Botón para guardar nota
        buttonGuardarNota.setOnClickListener {
            guardarNota()
        }

        val verNotasButton: Button = findViewById(R.id.buttonVerNotas)
        verNotasButton.setOnClickListener {
            val intent = Intent(this, Pantalla_lista_notas::class.java)
            startActivity(intent)
        }

        // Botón micrófono
        buttonMicrofono.setOnClickListener {
            if (isRecording) {
                stopSpeechRecognition()
            } else {
                checkAndRequestAudioPermission()
            }
        }
    }

    // Configurar el SpeechRecognizer para voz
    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isRecording = true
                buttonMicrofono.setBackgroundColor(ContextCompat.getColor(this@Pantalla_bloc_notas, android.R.color.holo_red_light))
                Toast.makeText(this@Pantalla_bloc_notas, "Grabando...", Toast.LENGTH_SHORT).show()
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                isRecording = false
                buttonMicrofono.setBackgroundColor(ContextCompat.getColor(this@Pantalla_bloc_notas, android.R.color.darker_gray))
                Toast.makeText(this@Pantalla_bloc_notas, "Error en reconocimiento de voz", Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                isRecording = false
                buttonMicrofono.setBackgroundColor(ContextCompat.getColor(this@Pantalla_bloc_notas, android.R.color.darker_gray))
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    appendTextToEditText(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.firstOrNull()?.let { text ->
                    appendTextToEditText(text, isPartial = true)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    // Añadir texto al EditText
    private fun appendTextToEditText(text: String, isPartial: Boolean = false) {
        val currentText = editTextNota.text.toString()
        val newText = if (currentText.isEmpty()) text else "$currentText $text"
        editTextNota.setText(newText)
        editTextNota.setSelection(editTextNota.text.length) // Mover cursor al final
    }

    // Verificar y solicitar permiso de grabación
    private fun checkAndRequestAudioPermission() {
        when {
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED -> {
                startSpeechRecognition()
            }
            else -> {
                requestPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    // Iniciar reconocimiento de voz
    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX") // Español de México
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Habilitar resultados parciales
        }
        speechRecognizer.startListening(intent)
    }

    // Detener reconocimiento de voz
    private fun stopSpeechRecognition() {
        speechRecognizer.stopListening()
    }

    private fun guardarNota() {
        val textoNota = editTextNota.text.toString().trim()
        val fotoUri = uriFotoSeleccionada?.toString() ?: ""

        if (textoNota.isEmpty()) {
            Toast.makeText(this, "Por favor escribe una nota.", Toast.LENGTH_SHORT).show()
            return
        }

        guardarNuevaNota(textoNota, fotoUri)

        // Limpiar campos
        editTextNota.text.clear()
        imageViewFoto.setImageResource(android.R.color.darker_gray)
        uriFotoSeleccionada = null
    }

    // Guarda lista de notas
    private fun guardarNuevaNota(textoNota: String, fotoUri: String) {
        val sharedPref = getSharedPreferences("NotasPrefs", MODE_PRIVATE)
        val editor = sharedPref.edit()

        // Leer notas existentes
        val gson = Gson()
        val jsonNotasExistente = sharedPref.getString("notas", null)
        val tipoLista = object : TypeToken<MutableList<Nota>>() {}.type
        val listaNotas: MutableList<Nota> = if (jsonNotasExistente != null) {
            gson.fromJson(jsonNotasExistente, tipoLista)
        } else {
            mutableListOf()
        }

        // Agregar nueva nota
        val nuevaNota = Nota(textoNota, fotoUri)
        listaNotas.add(nuevaNota)

        // Guardar la lista actualizada
        val jsonNotasActualizado = gson.toJson(listaNotas)
        editor.putString("notas", jsonNotasActualizado)
        editor.apply()

        Toast.makeText(this, "Nota guardada exitosamente ✅", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy() // Liberar recursos
    }
}