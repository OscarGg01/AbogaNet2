package com.example.aboganet2.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AbogaNet",
                        fontWeight = FontWeight.Bold,
                        color = Color.White // Usamos blanco para que contraste con el fondo
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary // #146B78 - Verde-azulado
                ),
                actions = {
                    // Icono de Perfil
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Perfil de usuario",
                            tint = Color.White // Tinte blanco para el icono
                        )
                    }
                    // Icono de Cerrar Sesión
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Cerrar Sesión",
                            tint = Color.White // Tinte blanco para el icono
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        // El contenido principal de la pantalla irá aquí
        ClientScreenContent(paddingValues = paddingValues)
    }
}

@Composable
fun ClientScreenContent(paddingValues: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues), // Aplicamos el padding del Scaffold
        contentAlignment = Alignment.Center
    ) {
        Text(text = "Contenido de la Pantalla Cliente")
    }
}