package com.example.aboganet2.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    lawyerId: String,
    lawyerName: String,
    cost: Float,
    onNavigateBack: () -> Unit,
    onContinue: (String, String, Float) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Horario", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Pantalla de selección de horario", fontSize = 20.sp)
                Text("(En construcción)", fontSize = 16.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = {
                        // Al hacer clic, simplemente pasamos los datos que recibimos
                        onContinue(lawyerName, lawyerId, cost)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Continuar", fontSize = 18.sp)
                }
            }
        }
    }
}