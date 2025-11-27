package com.example.aboganet2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.aboganet2.data.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit // Parámetro para manejar la acción de volver
) {
    val userProfile by authViewModel.userProfile.collectAsState()

    LaunchedEffect(Unit) {
        authViewModel.fetchUserProfile()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Volver atrás",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary // #146B78 - Verde-azulado
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background), // #EFEFEF - Gris Claro
            contentAlignment = Alignment.Center
        ) {
            if (userProfile != null) {
                // Pasamos el authViewModel a ProfileData
                ProfileData(user = userProfile!!, authViewModel = authViewModel)
            } else {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ProfileData(user: User, authViewModel: AuthViewModel) {

    var isEditing by remember { mutableStateOf(false) }
    var newFotoUrl by remember { mutableStateOf(user.fotoUrl) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val painter = rememberAsyncImagePainter(
            model = user.fotoUrl.ifEmpty { R.drawable.ic_default_profile } // Usamos un placeholder
        )
        Image(
            painter = painter,
            contentDescription = "Foto de perfil",
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.LightGray),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = user.nombre,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground // #0F3C4C - Azul Petróleo
        )
        Spacer(modifier = Modifier.height(24.dp))

        ProfileDataItem("DNI:", user.dni)
        ProfileDataItem("Email:", user.email)
        ProfileDataItem("Teléfono:", user.telefono)
        ProfileDataItem("Dirección:", user.direccion)

        Spacer(modifier = Modifier.height(24.dp))

        if (isEditing) {
            OutlinedTextField(
                value = newFotoUrl,
                onValueChange = { newFotoUrl = it },
                label = { Text("Nueva URL de la foto") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(
                    onClick = {
                        // Llamamos a la función del ViewModel para guardar la nueva URL
                        authViewModel.updateUserProfilePicture(newFotoUrl)
                        isEditing = false // Ocultamos los controles después de guardar
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Guardar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { isEditing = false }) {
                    Text("Cancelar")
                }
            }
        } else {
            // Si no estamos editando, mostramos los dos botones
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        newFotoUrl = user.fotoUrl
                        isEditing = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Cambiar Foto")
                }
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = { /* TODO: Navegar a la pantalla de edición de datos */ },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("Editar Datos")
                }
            }
        }
    }
}

@Composable
fun ProfileDataItem(label: String, value: String) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 8.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary, // #146B78 - Verde-azulado
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground // #0F3C4C - Azul Petróleo
        )
        Divider(color = Color.LightGray, thickness = 1.dp, modifier = Modifier.padding(top = 8.dp))
    }
}