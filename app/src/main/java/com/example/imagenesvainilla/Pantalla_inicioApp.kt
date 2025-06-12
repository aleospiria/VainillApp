package com.example.imagenesvainilla


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

class Pantalla_inicioApp : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pantalla_inicio_app)


        // Duración de la pantalla de inicio (Splash) en milisegundos
        val SPLASH_TIME_OUT = 3000L // 3 segundos

        Handler().postDelayed({
            // Lanzar la MainActivity después de 3 segundos
            val intent = Intent(this, Pantalla_inicioSe::class.java)
            startActivity(intent)
            finish()  // Terminar SplashActivity para que no se quede en el stack
        }, SPLASH_TIME_OUT)
    }
}
