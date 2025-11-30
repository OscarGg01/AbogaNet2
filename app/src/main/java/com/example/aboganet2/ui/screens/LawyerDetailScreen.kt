package com.example.aboganet2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.aboganet2.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerDetailScreen(
    lawyerId: String,
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPayment: (String, String, Float) -> Unit
) {
    val fullProfile by authViewModel.fullLawyerProfile.collectAsState()
    var showConfirmationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(lawyerId) {
        authViewModel.fetchLawyerProfileById(lawyerId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalles del Abogado", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        },
        floatingActionButton = {
            if (fullProfile.professionalInfo?.disponibilidad == true) {
                ExtendedFloatingActionButton(
                    onClick = { showConfirmationDialog = true },
                    icon = { Icon(Icons.Default.Check, "Reservar") },
                    text = { Text("Reservar Consulta") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { paddingValues ->
        if (fullProfile.basicInfo == null || fullProfile.professionalInfo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val basicInfo = fullProfile.basicInfo!!
            val professionalInfo = fullProfile.professionalInfo!!

            if (showConfirmationDialog) {
                val costo = professionalInfo.costoConsulta?.let { "S/ %.2f".format(it) } ?: "No establecido"
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = { Text("Confirmar Reserva") },
                    text = { Text("El costo de la primera consulta con ${basicInfo.nombre} es de $costo. ¿Deseas continuar con el pago?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showConfirmationDialog = false
                                onNavigateToPayment(
                                    basicInfo.nombre,
                                    lawyerId, // Pasamos el ID del abogado
                                    professionalInfo.costoConsulta?.toFloat() ?: 0f
                                )
                            }
                        ) {
                            Text("Aceptar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showConfirmationDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { ProfileHeader(profile = fullProfile, isAvailable = professionalInfo.disponibilidad) }
                item { Spacer(modifier = Modifier.height(24.dp)) }

                val costoFormatted = professionalInfo.costoConsulta?.let { "S/ %.2f".format(it) } ?: "Consultar tarifa"
                item { ProfileStaticSection("Tarifa por Consulta", costoFormatted) }
                item { ProfileStaticSection("Especialidad", professionalInfo.especialidad) }
                item { ProfileStaticSection("Logros y Resumen", professionalInfo.logros) }
                item { SectionTitle("Educación") }
                items(professionalInfo.educacion) { item ->
                    InfoCard(line1 = item.titulo, line2 = item.universidad, line3 = "Año: ${item.anio}")
                }
                item { SectionTitle("Experiencia Laboral") }
                items(professionalInfo.experiencia) { item ->
                    InfoCard(line1 = item.puesto, line2 = item.empresa, line3 = item.periodo)
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}