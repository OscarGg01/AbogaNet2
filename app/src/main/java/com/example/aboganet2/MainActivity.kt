package com.example.aboganet2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.aboganet2.navigation.AppNavigation // <-- Importa tu navegación
import com.example.aboganet2.ui.theme.AbogaNet2Theme // <-- El nombre de tu tema

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AbogaNet2Theme { // Usa el tema de tu app
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Aquí está la magia: Llamamos a nuestro sistema de navegación
                    AppNavigation()
                }
            }
        }
    }
}