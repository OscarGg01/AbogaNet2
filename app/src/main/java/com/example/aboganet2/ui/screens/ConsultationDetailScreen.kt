package com.example.aboganet2.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aboganet2.data.User
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsultationDetailScreen(
    consultationId: String,
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onLawyerProfileClick: (String) -> Unit,
    onOpenChatClick: (String, String) -> Unit
) {
    val clientConsultations by authViewModel.clientConsultations.collectAsState()

    val consultationPair by remember(consultationId, clientConsultations) {
        derivedStateOf {
            clientConsultations.find { it.first.id == consultationId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de la Consulta", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        consultationPair?.let { (consultation, lawyer) ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailItem("Título", consultation.title)
                    DetailItem("Descripción", consultation.description)
                    DetailItem("Estado", consultation.status.replaceFirstChar { it.uppercase() })
                    DetailItem("Fecha y Hora de la Cita", formatTimestamp(consultation.appointmentTimestamp, includeTime = true))
                    DetailItem("Fecha de Solicitud", formatTimestamp(consultation.timestamp))

                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    FinalCostDisplay(finalCost = consultation.finalCost)
                    Divider(modifier = Modifier.padding(vertical = 16.dp))

                    if (lawyer != null) {
                        LawyerInfoSection(
                            lawyer = lawyer,
                            onLawyerProfileClick = onLawyerProfileClick
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (lawyer != null) {
                            onOpenChatClick(consultation.id, lawyer.uid)
                        }
                    },
                    enabled = lawyer != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp)
                ) {
                    Text("Abrir Chat")
                }
            }
        } ?: run {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun LawyerInfoSection(lawyer: User, onLawyerProfileClick: (String) -> Unit) {
    Column {
        Text(
            text = "Abogado Asignado",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = lawyer.nombre,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier
                .clickable { onLawyerProfileClick(lawyer.uid) }
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun FinalCostDisplay(finalCost: Double?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Costo Total del Caso",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (finalCost != null) {
            Text(
                text = "S/. ${"%.2f".format(finalCost)}",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        } else {
            Text(
                text = "El abogado está analizando tu caso...",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                fontStyle = FontStyle.Italic
            )
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp?, includeTime: Boolean = false): String {
    return timestamp?.toDate()?.let {
        val pattern = if (includeTime) {
            "dd 'de' MMMM 'de' yyyy, hh:mm a" // Formato con hora
        } else {
            "dd 'de' MMMM 'de' yyyy" // Formato solo fecha
        }
        SimpleDateFormat(pattern, Locale("es", "ES")).format(it)
    } ?: "No disponible"
}