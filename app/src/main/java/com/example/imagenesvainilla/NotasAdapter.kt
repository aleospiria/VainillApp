package com.example.imagenesvainilla

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.imagenesvainilla.Nota

class NotasAdapter(private val listaNotas: List<Nota>) : RecyclerView.Adapter<NotasAdapter.NotaViewHolder>() {

    class NotaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textoNota: TextView = view.findViewById(R.id.textoNota)
        val imagenNota: ImageView = view.findViewById(R.id.imagenNota)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_nota, parent, false)
        return NotaViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotaViewHolder, position: Int) {
        val nota = listaNotas[position]
        holder.textoNota.text = nota.texto

        if (!nota.fotoUri.isNullOrEmpty()) {
            try {
                val uri = Uri.parse(nota.fotoUri)
                val inputStream = holder.itemView.context.contentResolver.openInputStream(uri)

                val options = android.graphics.BitmapFactory.Options().apply {
                    inSampleSize = 8 // para reducir tamaño y memoria
                }

                val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream, null, options)

                if (bitmap != null) {
                    holder.imagenNota.setImageBitmap(bitmap)
                } else {
                    holder.imagenNota.setImageResource(android.R.drawable.ic_menu_report_image)
                }

                inputStream?.close()
            } catch (e: Exception) {
                e.printStackTrace()
                holder.imagenNota.setImageResource(android.R.drawable.ic_menu_report_image)
            }
        } else {
            holder.imagenNota.setImageResource(android.R.drawable.ic_menu_report_image)
        }
    }

    override fun getItemCount(): Int = listaNotas.size
}