package com.example.imagenesvainilla

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Pantalla_inicioSe : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_pantalla_inicio_se)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.Iniciosesion)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Obtener el botón de "Ingresar como invitado" por su ID
        val guestButton: Button = findViewById(R.id.guest_button)

        // Establecer el evento onClick para abrir la MainActivity
        guestButton.setOnClickListener {
            // Crear un Intent para abrir la MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)  // Iniciar la MainActivity
        }
    }
}