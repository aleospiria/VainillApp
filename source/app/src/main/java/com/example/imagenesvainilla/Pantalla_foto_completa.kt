package com.example.imagenesvainilla

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.net.Uri
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Pantalla_foto_completa : AppCompatActivity() {

    private lateinit var uriFoto: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_foto_completa)

        val imageView = findViewById<ImageView>(R.id.imageCompleta)
        val btnEliminar = findViewById<Button>(R.id.btnEliminar)

        uriFoto = intent.getStringExtra("foto_uri") ?: return

        imageView.setImageURI(Uri.parse(uriFoto))

        btnEliminar.setOnClickListener {
            eliminarFotoDelHistorial(uriFoto)
        }
    }

    private fun eliminarFotoDelHistorial(uri: String) {
        val sharedPref = getSharedPreferences("HistorialFotos", MODE_PRIVATE)
        val gson = Gson()
        val jsonFotos = sharedPref.getString("fotos", null)
        val tipoLista = object : TypeToken<MutableList<FotoHistorial>>() {}.type

        val listaFotos: MutableList<FotoHistorial> = if (jsonFotos != null) {
            gson.fromJson(jsonFotos, tipoLista)
        } else {
            mutableListOf()
        }

        val nuevaLista = listaFotos.filter { it.uri != uri }

        val jsonActualizado = gson.toJson(nuevaLista)
        sharedPref.edit().putString("fotos", jsonActualizado).apply()

        Toast.makeText(this, "Foto eliminada 🗑️", Toast.LENGTH_SHORT).show()

        // Volver al historial y cerrarse
        val intent = Intent(this, Pantalla_historial::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }
}