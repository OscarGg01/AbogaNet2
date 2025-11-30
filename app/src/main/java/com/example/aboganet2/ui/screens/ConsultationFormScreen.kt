package com.example.aboganet2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aboganet2.data.Consultation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationFormScreen(
    lawyerId: String,
    cost: Double,
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onSubmissionSuccess: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current

    val consultationState by authViewModel.consultationState.collectAsState()

    // Observador para reaccionar al resultado del guardado
    LaunchedEffect(consultationState) {
        if (consultationState == true) {
            Toast.makeText(context, "Consulta enviada con éxito", Toast.LENGTH_LONG).show()
            authViewModel.resetConsultationState() // Limpiar el estado
            onSubmissionSuccess() // Navegar a la siguiente pantalla
        } else if (consultationState == false) {
            Toast.makeText(context, "Error al enviar la consulta", Toast.LENGTH_LONG).show()
            authViewModel.resetConsultationState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles de la Consulta", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Proporcione los detalles de su caso", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título de la Consulta") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción detallada del caso") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    val clientId = authViewModel.getCurrentUserId() ?: ""
                    val newConsultation = Consultation(
                        clientId = clientId,
                        lawyerId = lawyerId,
                        title = title,
                        description = description,
                        cost = cost
                    )
                    authViewModel.submitConsultation(newConsultation)
                },
                enabled = title.isNotBlank() && description.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Enviar Consulta")
            }
        }
    }
}
