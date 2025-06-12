package com.example.imagenesvainilla

import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Size
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.imagenesvainilla.databinding.ActivityPantallaCamaraBinding
import com.example.imagenesvainilla.ml.ModeloVainillasConMetadata
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.label.Category
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import android.media.ExifInterface
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Pantalla_camara : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityPantallaCamaraBinding
    private lateinit var imageCapture: ImageCapture
    private lateinit var camera: Camera
    private lateinit var model: ModeloVainillasConMetadata
    private lateinit var resultTextView: TextView
    private lateinit var imagePath: String
    private lateinit var tts: TextToSpeech

    // Definir las etiquetas válidas para la clasificación
    private val validLabels = setOf(
        "Floracion",
        "Vegetacion",
        "No polinizada",
        "Posible polinizada",
        "Fruto"
    )

    // Umbral mínimo de confianza para considerar una predicción válida
    private val confidenceThreshold = 0.3f //Antes eran de 0.6 pero puesto que despues de las metricas quedo con un promedio de 30%

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPantallaCamaraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializar la cámara
        startCamera()

        // Inicializar TTS
        tts = TextToSpeech(this, this)

        // Cargar modelo TFLite
        model = ModeloVainillasConMetadata.newInstance(this)

        // Configuración de los botones
        binding.btnBack.setOnClickListener {
            finish()
        }

        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        binding.btnGallery.setOnClickListener {
            openGallery()
        }

        binding.btnSavePhoto.setOnClickListener {
            savePhoto()
        }

        binding.btnRetake.setOnClickListener {
            retakePhoto()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale("es", "MX"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Idioma no soportado", Toast.LENGTH_SHORT).show()
            } else {
                tts.setSpeechRate(0.9f)
                tts.setPitch(0.9f)
            }
        } else {
            Toast.makeText(this, "Error al inicializar Text-to-Speech", Toast.LENGTH_SHORT).show()
        }
    }

    private fun speakText(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = binding.cameraPreview.surfaceProvider
            }

            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(1280, 720))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                println("Error al inicializar la cámara: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val photoFile = File(externalMediaDirs.first(), "photo_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    guardarFotoEnHistorial(savedUri.toString())

                    val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)

                    // Corregir la orientación de la imagen
                    val correctedBitmap = correctImageOrientation(bitmap, photoFile)

                    // Llamar a processBitmap para la clasificación
                    runOnUiThread {
                        binding.imgBitMap.setImageBitmap(correctedBitmap)
                        binding.imgBitMap.visibility = ImageView.VISIBLE
                        binding.imgBitMap.scaleType = ImageView.ScaleType.FIT_CENTER
                        binding.cameraPreview.visibility = View.GONE
                        binding.btnOptions.visibility = LinearLayout.VISIBLE
                        processBitmap(correctedBitmap)  // Aquí es donde se hace la clasificación
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    println("Error al tomar la foto: ${exception.message}")
                }
            })
    }

    private fun savePhoto() {
        val bitmap = (binding.imgBitMap.drawable as? BitmapDrawable)?.bitmap

        if (bitmap != null) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "photo_${System.currentTimeMillis()}.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                val resolver = applicationContext.contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                imageUri?.let { uri ->
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                }

                runOnUiThread {
                    Toast.makeText(this, "Foto guardada exitosamente!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this, "Error al guardar la foto", Toast.LENGTH_SHORT).show()
                }
            }
        }
        println("Foto guardada!")
    }

    private fun retakePhoto() {
        binding.imgBitMap.visibility = ImageView.GONE
        binding.btnOptions.visibility = LinearLayout.GONE
        startCamera()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, 1001)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val selectedImageUri: Uri? = data?.data
            selectedImageUri?.let {
                try {
                    val realPath = getRealPathFromURI(it)
                    if (realPath != null) {
                        imagePath = realPath
                    }
                    val inputStream: InputStream? = contentResolver.openInputStream(it)
                    val options = BitmapFactory.Options().apply {
                        inSampleSize = 2
                    }
                    val bitmap = BitmapFactory.decodeStream(inputStream, null, options)

                    if (bitmap != null) {
                        binding.imgBitMap.setImageBitmap(bitmap)
                        binding.imgBitMap.visibility = View.VISIBLE
                        binding.cameraPreview.visibility = View.GONE
                        processBitmap(bitmap)  // Aquí se sigue llamando a processBitmap
                    } else {
                        Toast.makeText(this, "Error al procesar imagen seleccionada", Toast.LENGTH_SHORT).show()
                    }

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Corregir la orientación de la imagen
    private fun correctImageOrientation(bitmap: Bitmap, photoFile: File): Bitmap {
        val exif = ExifInterface(photoFile.absolutePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val matrix = Matrix()

        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
        }

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Función para realizar la clasificación
    private fun processBitmap(bitmap: Bitmap) {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
        val image = TensorImage.fromBitmap(resizedBitmap)
        val outputs = model.process(image)
        val probability = outputs.probabilityAsCategoryList

        // Validar y filtrar la predicción
        val validatedResult = validatePrediction(probability)

        runOnUiThread {
            val resultText = validatedResult.message
            binding.resultTextView.text = resultText
            binding.resultTextView.visibility = View.VISIBLE
            speakText(validatedResult.spokenText)
        }
    }

    //Para encapsular el resultado de la validación
    data class ValidationResult(
        val isValid: Boolean,
        val message: String,
        val spokenText: String
    )

    fun getRealPathFromURI(uri: Uri): String? {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)

        cursor?.let {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            val filePath = it.getString(columnIndex)
            it.close()
            return filePath
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
        tts.stop()
        tts.shutdown()
    }

    private fun guardarFotoEnHistorial(uri: String) {
        val sharedPref = getSharedPreferences("HistorialFotos", MODE_PRIVATE)
        val gson = Gson()
        val jsonFotos = sharedPref.getString("fotos", null)
        val tipoLista = object : TypeToken<MutableList<FotoHistorial>>() {}.type
        val listaFotos: MutableList<FotoHistorial> = if (jsonFotos != null) {
            gson.fromJson(jsonFotos, tipoLista)
        } else {
            mutableListOf()
        }

        val fechaHora = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val nuevaFoto = FotoHistorial(uri, fechaHora)
        listaFotos.add(nuevaFoto)

        val jsonActualizado = gson.toJson(listaFotos)
        sharedPref.edit().putString("fotos", jsonActualizado).apply()
    }

    // Función para validar las predicciones
    private fun validatePrediction(predictions: List<Category>): ValidationResult {
        // Ordenar las predicciones por la probabilidad (de mayor a menor)
        val sortedPredictions = predictions.sortedByDescending { it.score }

        // Buscar la primera predicción válida con suficiente confianza
        for (prediction in sortedPredictions) {
            val cleanLabel = cleanLabel(prediction.label)

            // Comprobamos si la etiqueta es válida y si la confianza supera el umbral
            if (validLabels.contains(cleanLabel) && prediction.score >= confidenceThreshold) {
                val confidence = (prediction.score * 100).toInt()
                val displayLabel = formatLabelForDisplay(cleanLabel)

                // Si es válida, devolvemos el resultado
                return ValidationResult(
                    isValid = true,
                    message = "Estado: $displayLabel",
                    spokenText = "Estado detectado: $displayLabel"
                )
            }
        }

        // Si no se encuentra una predicción válida, devolvemos un mensaje
        val bestPrediction = sortedPredictions.firstOrNull()
        val lowConfidenceMessage = if (bestPrediction != null && bestPrediction.score < confidenceThreshold) {
            "\nPredicción con baja confianza: ${cleanLabel(bestPrediction.label)} (${(bestPrediction.score * 100).toInt()}%)"
        } else {
            ""
        }

        return ValidationResult(
            isValid = false,
            message = "No se reconoce el estado de la planta$lowConfidenceMessage",
            spokenText = "No se pudo identificar el estado de la planta. Por favor, toma otra foto con mejor iluminación."
        )
    }

    // Limpiar la etiqueta
    private fun cleanLabel(label: String): String {
        return label.trim()
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            }
    }

    // Formatear la etiqueta para mostrarla de forma adecuada
    private fun formatLabelForDisplay(label: String): String {
        return when (label.lowercase().replace(" ", "")) {
            "floracion" -> "Floración"
            "vegetacion" -> "Vegetación"
            "nopolinizada" -> "No Polinizada"
            "posiblepolinizada" -> "Posible Polinizada"
            "fruto" -> "Fruto"
            else -> label
        }
    }

}
