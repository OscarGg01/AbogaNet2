package com.example.aboganet2.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aboganet2.data.Consultation
import com.example.aboganet2.data.User
import java.text.SimpleDateFormat
import java.util.*

// Data class para definir los ítems de la barra de navegación
data class BottomNavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerHomeScreen(
    authViewModel: AuthViewModel = viewModel(),
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onConsultationClick: (String) -> Unit
) {
    val lawyerConsultations by authViewModel.lawyerConsultations.collectAsState()

    // --- ESTADO PARA LA BARRA DE NAVEGACIÓN ---
    // Lista de ítems para la barra inferior
    val bottomNavItems = listOf(
        BottomNavItem(
            title = "Casos",
            selectedIcon = Icons.Filled.Assignment,
            unselectedIcon = Icons.Outlined.Assignment
        ),
        BottomNavItem(
            title = "Agenda",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth
        ),
        BottomNavItem(
            title = "Documentos",
            selectedIcon = Icons.Filled.Folder,
            unselectedIcon = Icons.Outlined.Folder
        )
    )

    // Estado para recordar el ítem seleccionado. "rememberSaveable" mantiene el estado
    // incluso si la pantalla se rota. Empezamos en el índice 0 ("Casos").
    var selectedItemIndex by rememberSaveable {
        mutableStateOf(0)
    }

    LaunchedEffect(Unit) {
        authViewModel.fetchLawyerConsultations()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Casos Asignados", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, "Perfil", tint = Color.White)
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.ExitToApp, "Cerrar Sesión", tint = Color.White)
                    }
                }
            )
        },
        // --- AÑADIMOS LA BARRA DE NAVEGACIÓN INFERIOR ---
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItemIndex == index,
                        onClick = {
                            selectedItemIndex = index
                            // Aquí irá la lógica de navegación en el futuro
                        },
                        label = { Text(text = item.title) },
                        icon = {
                            Icon(
                                imageVector = if (selectedItemIndex == index) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        // El contenido de la pantalla ahora se muestra basado en el ítem seleccionado
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedItemIndex) {
                0 -> { // Casos Asignados (la pantalla que ya tenías)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(lawyerConsultations) { (consultation, client) ->
                            LawyerConsultationCard(
                                consultation = consultation,
                                client = client,
                                onClick = { onConsultationClick(consultation.id) }
                            )
                        }
                    }
                }
                1 -> {
                    LawyerAgendaScreen(authViewModel = authViewModel)
                }
                2 -> {
                    LawyerDocumentsScreen(authViewModel = authViewModel)
                }
            }
        }
    }
}


@Composable
fun LawyerConsultationCard(
    consultation: Consultation,
    client: User?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = consultation.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cliente: ${client?.nombre ?: "Cargando..."}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(4.dp))

            val formattedDate = consultation.appointmentTimestamp?.toDate()?.let {
                SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es", "ES")).format(it)
            } ?: "Fecha por confirmar"
            Text(
                text = "Fecha de la Cita: $formattedDate",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = getStatusColor(consultation.status),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = consultation.status.replaceFirstChar { it.uppercase() },
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "pendiente" -> Color.Red
        "en progreso" -> Color(0xFFFFA500) // Naranja
        "finalizada" -> Color(0xFF008000) // Verde oscuro
        "cobro pendiente" -> Color(0xFF4169E1) // Azul "RoyalBlue"
        else -> Color.Gray
    }
}