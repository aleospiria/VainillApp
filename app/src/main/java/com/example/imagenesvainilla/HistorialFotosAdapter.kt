package com.example.imagenesvainilla

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class HistorialFotosAdapter(
    private val fotos: List<FotoHistorial>,
    private val context: Context,
    private val esModoSeleccion: Boolean, // Nuevo parámetro
    private val onItemClick: (FotoHistorial) -> Unit // Nuevo parámetro
) : RecyclerView.Adapter<HistorialFotosAdapter.ViewHolder>() {

    // Clase ViewHolder sin cambios
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageFoto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_foto, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val foto = fotos[position]

        // Cargar imagen
        Glide.with(context)
            .load(foto.uri)
            .placeholder(android.R.color.darker_gray)
            .into(holder.imageView)

        // Configurar el click listener
        holder.itemView.setOnClickListener {
            onItemClick(foto)
        }

        // Cambiar aspecto en modo selección
        if (esModoSeleccion) {
            holder.itemView.background = ContextCompat.getDrawable(context, R.drawable.borde_seleccion)
        } else {
            holder.itemView.background = null // Quitar el fondo si no está en modo selección
        }
    }

    override fun getItemCount(): Int = fotos.size
}