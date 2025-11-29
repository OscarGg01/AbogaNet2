package com.example.aboganet2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ExitToApp
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
import com.example.aboganet2.data.FullLawyerProfile

private const val SORT_AZ = "A-Z"
private const val SORT_ZA = "Z-A"
private const val AVAILABILITY_ALL = "Todos"
private const val AVAILABILITY_AVAILABLE = "Disponible"
private const val AVAILABILITY_UNAVAILABLE = "No Disponible"
private const val SPECIALTY_ALL = "Todas"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientHomeScreen(
    authViewModel: AuthViewModel = viewModel(),
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLawyerClick: (String) -> Unit
) {
    val originalLawyerList by authViewModel.availableLawyers.collectAsState()
    val isLoading by authViewModel.isLoading.collectAsState()

    var availabilityFilter by remember { mutableStateOf(AVAILABILITY_ALL) }
    var specialtyFilter by remember { mutableStateOf(SPECIALTY_ALL) }
    var sortOrder by remember { mutableStateOf(SORT_AZ) }

    val filteredAndSortedLawyers = remember(originalLawyerList, availabilityFilter, specialtyFilter, sortOrder) {
        originalLawyerList
            .filter { profile ->
                when (availabilityFilter) {
                    AVAILABILITY_AVAILABLE -> profile.professionalInfo?.disponibilidad == true
                    AVAILABILITY_UNAVAILABLE -> profile.professionalInfo?.disponibilidad == false
                    else -> true
                }
            }
            .filter { profile ->
                if (specialtyFilter == SPECIALTY_ALL) {
                    true
                } else {
                    profile.professionalInfo?.especialidad == specialtyFilter
                }
            }
            .sortedWith(compareBy { it.basicInfo?.nombre })
            .let {
                if (sortOrder == SORT_ZA) it.reversed() else it
            }
    }

    LaunchedEffect(Unit) {
        authViewModel.fetchAllActiveLawyers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Encuentra tu Abogado", fontWeight = FontWeight.Bold, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(Icons.Default.AccountCircle, "Perfil", tint = Color.White)
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.Default.ExitToApp, "Cerrar Sesión", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            FilterControls(
                availability = availabilityFilter,
                onAvailabilityChange = { availabilityFilter = it },
                specialty = specialtyFilter,
                onSpecialtyChange = { specialtyFilter = it },
                sortOrder = sortOrder,
                onSortOrderChange = { sortOrder = it },
                onClearFilters = {
                    availabilityFilter = AVAILABILITY_ALL
                    specialtyFilter = SPECIALTY_ALL
                    sortOrder = SORT_AZ
                }
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredAndSortedLawyers) { lawyerProfile ->
                        LawyerCard(lawyerProfile = lawyerProfile, onClick = {
                            lawyerProfile.basicInfo?.uid?.let { onLawyerClick(it) }
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun FilterControls(
    availability: String, onAvailabilityChange: (String) -> Unit,
    specialty: String, onSpecialtyChange: (String) -> Unit,
    sortOrder: String, onSortOrderChange: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterDropDown(
                label = "Disponibilidad",
                selectedValue = availability,
                options = listOf(AVAILABILITY_ALL, AVAILABILITY_AVAILABLE, AVAILABILITY_UNAVAILABLE),
                onOptionSelected = onAvailabilityChange,
                modifier = Modifier.weight(1f)
            )
            FilterDropDown(
                label = "Ordenar por",
                selectedValue = sortOrder,
                options = listOf(SORT_AZ, SORT_ZA),
                onOptionSelected = onSortOrderChange,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            FilterDropDown(
                label = "Especialidad",
                selectedValue = specialty,
                options = listOf(SPECIALTY_ALL) + lawSpecialties,
                onOptionSelected = onSpecialtyChange,
                modifier = Modifier.weight(1f)
            )
            OutlinedButton(
                onClick = onClearFilters,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(Icons.Default.Clear, contentDescription = "Limpiar Filtros")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Limpiar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterDropDown(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, fontSize = 12.sp) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun LawyerCard(lawyerProfile: FullLawyerProfile, onClick: () -> Unit) {
    val isAvailable = lawyerProfile.professionalInfo?.disponibilidad ?: false

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val painter = rememberAsyncImagePainter(
                model = lawyerProfile.basicInfo?.fotoUrl?.ifEmpty { R.drawable.ic_default_profile }
            )
            Image(
                painter = painter,
                contentDescription = "Foto de ${lawyerProfile.basicInfo?.nombre}",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = lawyerProfile.basicInfo?.nombre ?: "Sin nombre",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = lawyerProfile.professionalInfo?.especialidad?.takeIf { it.isNotEmpty() } ?: "Especialidad no definida",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                if (isAvailable) Color(0xFF4CAF50) else Color.Gray,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isAvailable) "Disponible" else "No Disponible",
                        fontSize = 12.sp,
                        color = if (isAvailable) Color(0xFF4CAF50) else Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}