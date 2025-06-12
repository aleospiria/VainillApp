package com.example.imagenesvainilla

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.net.Uri
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imagenesvainilla.Nota
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.recyclerview.widget.ItemTouchHelper


class Pantalla_lista_notas : AppCompatActivity() {

    private lateinit var recyclerNotas: RecyclerView
    private lateinit var adapter: NotasAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //enableEdgeToEdge()
        setContentView(R.layout.activity_pantalla_lista_notas)

        recyclerNotas = findViewById(R.id.recyclerNotas)
        recyclerNotas.layoutManager = LinearLayoutManager(this)

        val Nota = cargarNotas()

        if (Nota.isEmpty()) {
            Toast.makeText(this, "No hay notas guardadas.", Toast.LENGTH_SHORT).show()
        }
        adapter = NotasAdapter(Nota)
        recyclerNotas.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                eliminarNota(position)
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerNotas)
    }

    private fun cargarNotas(): List<Nota> {
        val sharedPref = getSharedPreferences("NotasPrefs", MODE_PRIVATE)
        val gson = Gson()
        val jsonNotas = sharedPref.getString("notas", null)
        val tipoLista = object : TypeToken<List<Nota>>() {}.type
        return if (jsonNotas != null) {
            gson.fromJson(jsonNotas, tipoLista)
        } else {
            emptyList()
        }
    }

    private fun eliminarNota(position: Int) {
        val sharedPref = getSharedPreferences("NotasPrefs", MODE_PRIVATE)
        val gson = Gson()
        val jsonNotas = sharedPref.getString("notas", null)
        val tipoLista = object : TypeToken<MutableList<Nota>>() {}.type
        val listaNotas: MutableList<Nota> = if (jsonNotas != null) {
            gson.fromJson(jsonNotas, tipoLista)
        } else {
            mutableListOf()
        }

        if (position < listaNotas.size) {
            listaNotas.removeAt(position)

            val jsonNotasActualizado = gson.toJson(listaNotas)
            sharedPref.edit().putString("notas", jsonNotasActualizado).apply()

            adapter.notifyItemRemoved(position)
            Toast.makeText(this, "Nota eliminada 🗑️", Toast.LENGTH_SHORT).show()
        }
    }

}