package com.example.imagenesvainilla

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Pantalla_historial : AppCompatActivity() {

    private lateinit var recyclerFotos: RecyclerView
    private lateinit var adapter: HistorialFotosAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_historial)

        recyclerFotos = findViewById(R.id.recyclerFotos)
        recyclerFotos.layoutManager = GridLayoutManager(this, 2) // 2 columnas tipo galería

        val esModoSeleccion = intent.getBooleanExtra("modo_seleccion", false)
        val listaFotos = cargarFotosGuardadas()

        if (listaFotos.isEmpty()) {
            Toast.makeText(this, "No hay fotos guardadas aún.", Toast.LENGTH_SHORT).show()
        }

        adapter = HistorialFotosAdapter(
            listaFotos,
            this,
            esModoSeleccion
        ) { fotoSeleccionada ->
            if (esModoSeleccion) {
                // Devolver la foto seleccionada
                val resultadoIntent = Intent()
                resultadoIntent.putExtra("foto_seleccionada", fotoSeleccionada.uri)
                setResult(RESULT_OK, resultadoIntent)
                finish()
            } else {
                // Modo normal (solo visualización)
                // Aquí puedes implementar la lógica para ver la foto en grande si lo deseas
            }
        }

        recyclerFotos.adapter = adapter
    }

    private fun cargarFotosGuardadas(): List<FotoHistorial> {
        val sharedPref = getSharedPreferences("HistorialFotos", MODE_PRIVATE)
        val gson = Gson()
        val jsonFotos = sharedPref.getString("fotos", null)
        val tipoLista = object : TypeToken<List<FotoHistorial>>() {}.type
        return if (jsonFotos != null) {
            gson.fromJson(jsonFotos, tipoLista)
        } else {
            emptyList()
        }
    }
}