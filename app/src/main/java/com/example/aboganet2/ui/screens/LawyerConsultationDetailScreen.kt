package com.example.aboganet2.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aboganet2.data.User
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerConsultationDetailScreen(
    consultationId: String,
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onOpenChatClick: (consultationId: String, clientId: String) -> Unit
) {
    val lawyerConsultations by authViewModel.lawyerConsultations.collectAsState()
    // Encontrar la consulta y el cliente correspondientes desde el ViewModel
    val consultationPair by remember(consultationId, lawyerConsultations) {
        derivedStateOf {
            lawyerConsultations.find { it.first.id == consultationId }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del Caso", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        consultationPair?.let { (consultation, client) ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // --- Detalles de la Consulta ---
                    Text(consultation.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    val formattedDate = consultation.timestamp?.toDate()?.let {
                        SimpleDateFormat("dd 'de' MMMM 'de' yyyy, HH:mm 'hs'", Locale("es", "ES")).format(it)
                    } ?: "N/A"
                    Text("Recibido el: $formattedDate", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(consultation.description, style = MaterialTheme.typography.bodyLarge)
                    Divider(modifier = Modifier.padding(vertical = 24.dp))

                    // --- Detalles del Cliente ---
                    client?.let {
                        ClientInfoSection(client = it)
                        Divider(modifier = Modifier.padding(vertical = 24.dp))
                    }

                    FinalCostSection(
                        initialCost = consultation.finalCost,
                        onSaveCost = { newCost ->
                            authViewModel.updateFinalCost(consultation.id, newCost)
                        }
                    )
                    Divider(modifier = Modifier.padding(vertical = 24.dp))

                    // --- Sección para cambiar estado ---
                    StatusSelector(
                        currentStatus = consultation.status,
                        onStatusChange = { newStatus ->
                            authViewModel.updateConsultationStatus(consultation.id, newStatus)
                        }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // --- Botón para abrir el chat ---
                Button(
                    onClick = {
                        // Solo permitir abrir el chat si tenemos el ID del cliente
                        if (client != null) {
                            onOpenChatClick(consultation.id, client.uid)
                        }
                    },
                    enabled = client != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(50.dp)
                ) {
                    Text("Abrir Chat con Cliente")
                }
            }
        } ?: run {
            // Muestra un indicador de carga si los datos aún no están listos
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun ClientInfoSection(client: User) {
    Column {
        Text("Información del Cliente", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        InfoRow("Nombre:", client.nombre)
        InfoRow("Correo:", client.email)
        InfoRow("DNI:", client.dni.ifEmpty { "No especificado" })
        InfoRow("Dirección:", client.direccion.ifEmpty { "No especificada" })
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(100.dp))
        Text(value)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusSelector(currentStatus: String, onStatusChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val statusOptions = listOf("Pendiente", "En progreso", "Finalizada", "Cobro pendiente")

    Column {
        Text("Estado de la Consulta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = currentStatus.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                statusOptions.forEach { status ->
                    DropdownMenuItem(
                        text = { Text(status) },
                        onClick = {
                            onStatusChange(status.lowercase())
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FinalCostSection(
    initialCost: Double?,
    onSaveCost: (Double) -> Unit
) {
    var costText by remember(initialCost) {
        mutableStateOf(initialCost?.toString() ?: "")
    }
    val context = LocalContext.current

    Column {
        Text("Costo Final del Caso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = costText,
            onValueChange = { costText = it.filter { char -> char.isDigit() || char == '.' } },
            label = { Text("Monto Final") },
            leadingIcon = { Text("S/.") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                val newCost = costText.toDoubleOrNull()
                if (newCost != null) {
                    onSaveCost(newCost)
                    Toast.makeText(context, "Costo guardado", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Por favor, ingrese un monto válido", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = costText.isNotBlank() && costText.toDoubleOrNull() != initialCost,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Guardar Costo")
        }
    }
}