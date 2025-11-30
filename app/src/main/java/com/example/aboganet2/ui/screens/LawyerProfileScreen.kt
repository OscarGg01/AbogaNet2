package com.example.aboganet2.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.aboganet2.R
import com.example.aboganet2.data.EducationItem
import com.example.aboganet2.data.ExperienceItem
import com.example.aboganet2.data.FullLawyerProfile
import com.example.aboganet2.data.LawyerProfile

val lawSpecialties = listOf(
    "Derecho Penal", "Derecho Civil", "Derecho Laboral", "Derecho Mercantil",
    "Derecho Administrativo", "Derecho Tributario", "Derecho de Familia",
    "Derecho Constitucional", "Derecho Internacional", "Derecho Ambiental",
    "Propiedad Intelectual", "Derecho Informático"
)

class CurrencyVisualTransformation(private val prefix: String = "S/ ") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val out = prefix + text.text
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + prefix.length
            override fun transformedToOriginal(offset: Int): Int = (offset - prefix.length).coerceAtLeast(0)
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LawyerProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val fullProfile by authViewModel.fullLawyerProfile.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var costoConsultaInput by remember { mutableStateOf("") }
    var costoConsulta by remember { mutableStateOf<Double?>(null) }
    var disponibilidad by remember { mutableStateOf(false) }
    var fotoUrl by remember { mutableStateOf("") }
    var especialidad by remember { mutableStateOf("") }
    var logros by remember { mutableStateOf("") }
    var educacionList by remember { mutableStateOf<List<EducationItem>>(emptyList()) }
    var experienciaList by remember { mutableStateOf<List<ExperienceItem>>(emptyList()) }
    var showEducationDialog by remember { mutableStateOf(false) }
    var showExperienceDialog by remember { mutableStateOf(false) }

    LaunchedEffect(fullProfile) {
        fullProfile.basicInfo?.let {
            fotoUrl = it.fotoUrl
        }
        fullProfile.professionalInfo?.let {
            costoConsulta = it.costoConsulta
            costoConsultaInput = it.costoConsulta?.toString() ?: ""
            disponibilidad = it.disponibilidad
            especialidad = it.especialidad
            logros = it.logros
            educacionList = it.educacion
            experienciaList = it.experiencia
        }
    }

    LaunchedEffect(Unit) {
        authViewModel.fetchFullLawyerProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil Profesional", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            val updatedProfile = LawyerProfile(
                                costoConsulta = costoConsultaInput.toDoubleOrNull(),
                                disponibilidad = disponibilidad,
                                especialidad = especialidad,
                                logros = logros,
                                educacion = educacionList,
                                experiencia = experienciaList
                            )
                            authViewModel.saveLawyerProfile(updatedProfile)
                            if (fotoUrl != fullProfile.basicInfo?.fotoUrl) {
                                authViewModel.updateUserProfilePicture(fotoUrl)
                            }
                            isEditing = false
                        }) {
                            Icon(Icons.Default.Done, "Guardar", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, "Editar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        if (fullProfile.basicInfo == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { Spacer(modifier = Modifier.height(16.dp)) }
                item { ProfileHeader(fullProfile, disponibilidad) }
                item { Spacer(modifier = Modifier.height(24.dp)) }

                if (isEditing) {
                    item {
                        AvailabilitySwitch(
                            isAvailable = disponibilidad,
                            onCheckedChange = { disponibilidad = it }
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = costoConsultaInput,
                            onValueChange = { newValue ->
                                if (newValue.matches(Regex("^\\d*\\.?\\d*\$"))) {
                                    costoConsultaInput = newValue
                                }
                            },
                            label = { Text("Costo por Consulta") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            visualTransformation = CurrencyVisualTransformation()
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = fotoUrl,
                            onValueChange = { fotoUrl = it },
                            label = { Text("URL de la Foto de Perfil") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    }
                    item { SpecialtyDropDown(selected = especialidad, onSelected = { especialidad = it }) }
                    item {
                        OutlinedTextField(
                            value = logros,
                            onValueChange = { logros = it },
                            label = { Text("Logros y Resumen Profesional") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            minLines = 4
                        )
                    }
                    item { SectionWithAddButton("Educación") { showEducationDialog = true } }
                } else {
                    val costoFormatted = costoConsulta?.let { "S/ %.2f".format(it) } ?: "No establecido"
                    item { ProfileStaticSection("Tarifa por Consulta", costoFormatted) }
                    item { ProfileStaticSection("Especialidad", especialidad) }
                    item { ProfileStaticSection("Logros y Resumen", logros) }
                    item { SectionTitle("Educación") }
                }

                items(educacionList) { item ->
                    InfoCard(line1 = item.titulo, line2 = item.universidad, line3 = "Año: ${item.anio}")
                }

                if (isEditing) {
                    item { SectionWithAddButton("Experiencia Laboral") { showExperienceDialog = true } }
                } else {
                    item { SectionTitle("Experiencia Laboral") }
                }

                items(experienciaList) { item ->
                    InfoCard(line1 = item.puesto, line2 = item.empresa, line3 = item.periodo)
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    if (showEducationDialog) {
        AddEducationDialog(
            onDismiss = { showEducationDialog = false },
            onSave = { newItem ->
                educacionList = educacionList + newItem
                showEducationDialog = false
            }
        )
    }

    if (showExperienceDialog) {
        AddExperienceDialog(
            onDismiss = { showExperienceDialog = false },
            onSave = { newItem ->
                experienciaList = experienciaList + newItem
                showExperienceDialog = false
            }
        )
    }
}

@Composable
fun ProfileHeader(profile: FullLawyerProfile, isAvailable: Boolean) {
    val basicInfo = profile.basicInfo ?: return
    val painter = rememberAsyncImagePainter(model = basicInfo.fotoUrl.ifEmpty { R.drawable.ic_default_profile })
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painter,
            contentDescription = "Foto de perfil",
            modifier = Modifier.size(120.dp).clip(CircleShape).background(Color.LightGray),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = basicInfo.nombre, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (isAvailable) Color(0xFF4CAF50) else Color.Gray,
                        shape = CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isAvailable) "Disponible" else "No Disponible",
                fontSize = 14.sp,
                color = if (isAvailable) Color(0xFF4CAF50) else Color.Gray,
                fontWeight = FontWeight.Bold
            )
        }
        Text(text = basicInfo.email, fontSize = 16.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
        Text(text = basicInfo.telefono, fontSize = 16.sp, color = Color.Gray)
    }
}

@Composable
fun AvailabilitySwitch(isAvailable: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Disponibilidad", fontWeight = FontWeight.Bold)
                Text(
                    text = if (isAvailable) "Estoy disponible para nuevos casos" else "No estoy disponible actualmente",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Switch(
                checked = isAvailable,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun ProfileStaticSection(title: String, content: String) {
    if (content.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = content, style = MaterialTheme.typography.bodyLarge)
            Divider(modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpecialtyDropDown(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Especialidad Principal") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            lawSpecialties.forEach { specialty ->
                DropdownMenuItem(
                    text = { Text(specialty) },
                    onClick = {
                        onSelected(specialty)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SectionWithAddButton(title: String, onAddClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onAddClick) {
            Icon(Icons.Default.Add, contentDescription = "Añadir", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun InfoCard(line1: String, line2: String, line3: String) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = line1, fontWeight = FontWeight.Bold)
            Text(text = line2, style = MaterialTheme.typography.bodyMedium)
            Text(text = line3, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    }
}

@Composable
fun AddEducationDialog(onDismiss: () -> Unit, onSave: (EducationItem) -> Unit) {
    var titulo by remember { mutableStateOf("") }
    var universidad by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Añadir Estudio", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") })
                OutlinedTextField(value = universidad, onValueChange = { universidad = it }, label = { Text("Universidad") })
                OutlinedTextField(value = anio, onValueChange = { anio = it }, label = { Text("Año de Graduación") })
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(EducationItem(titulo, universidad, anio)) }) { Text("Guardar") }
                }
            }
        }
    }
}

@Composable
fun AddExperienceDialog(onDismiss: () -> Unit, onSave: (ExperienceItem) -> Unit) {
    var puesto by remember { mutableStateOf("") }
    var empresa by remember { mutableStateOf("") }
    var periodo by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Añadir Experiencia", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = puesto, onValueChange = { puesto = it }, label = { Text("Puesto") })
                OutlinedTextField(value = empresa, onValueChange = { empresa = it }, label = { Text("Empresa / Estudio") })
                OutlinedTextField(value = periodo, onValueChange = { periodo = it }, label = { Text("Periodo (ej. 2020 - Presente)") })
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(ExperienceItem(puesto, empresa, periodo)) }) { Text("Guardar") }
                }
            }
        }
    }
}