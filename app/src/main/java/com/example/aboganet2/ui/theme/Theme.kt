package com.example.aboganet2.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val AzulPetroleoOscuro = Color(0xFF0F3C4C) // Base
val VerdeAzulado = Color(0xFF146B78)       // Primario
val Cobre = Color(0xFFC47F4E)             // Acento
val GrisClaro = Color(0xFFEFEFEF)         // Background/Formularios
val TextoBlanco = Color.White

private val DarkColorScheme = darkColorScheme(
    primary = VerdeAzulado,
    secondary = Cobre,
    background = AzulPetroleoOscuro,
    surface = Color(0xFF1A4958), // Un tono ligeramente más claro que la base
    onPrimary = TextoBlanco,
    onSecondary = TextoBlanco,
    onBackground = GrisClaro,
    onSurface = GrisClaro
)

private val LightColorScheme = lightColorScheme(
    primary = VerdeAzulado,                // Color principal para botones, elementos activos.
    secondary = Cobre,                     // Color de acento para elementos secundarios.
    background = GrisClaro,                // Color de fondo principal de la app.
    surface = Color.White,                 // Color para las "superficies" como cards, formularios.
    onPrimary = TextoBlanco,               // Color del texto sobre el color primario (ej. texto en un botón).
    onSecondary = TextoBlanco,             // Color del texto sobre el color secundario.
    onBackground = AzulPetroleoOscuro,     // Color del texto sobre el fondo principal.
    onSurface = AzulPetroleoOscuro         // Color del texto sobre las superficies.
)

@Composable
fun AbogaNet2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // La tipografía que ya tenías
        content = content
    )
}