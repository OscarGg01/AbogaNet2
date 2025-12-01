package com.example.aboganet2.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aboganet2.data.Message
import com.example.aboganet2.data.User
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    consultationId: String,
    authViewModel: AuthViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val messages by authViewModel.chatMessages.collectAsState()
    val currentUserId = authViewModel.getCurrentUserId()
    val currentUserProfile by authViewModel.userProfile.collectAsState()
    val clientConsultations by authViewModel.clientConsultations.collectAsState()
    val lawyerConsultations by authViewModel.lawyerConsultations.collectAsState()

    val otherUser: User? = when (currentUserProfile?.rol) {
        "cliente" -> clientConsultations.find { it.first.id == consultationId }?.second
        "abogado" -> lawyerConsultations.find { it.first.id == consultationId }?.second
        else -> null
    }

    LaunchedEffect(currentUserProfile, otherUser) {
        if (currentUserProfile == null) {
            authViewModel.fetchUserProfile()
        } else {
            if (otherUser == null) {
                if (currentUserProfile?.rol == "cliente") {
                    authViewModel.fetchClientConsultations()
                } else if (currentUserProfile?.rol == "abogado") {
                    authViewModel.fetchLawyerConsultations()
                }
            }
        }
    }

    LaunchedEffect(consultationId) {
        authViewModel.listenForMessages(consultationId)
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            authViewModel.sendFile(consultationId, it)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                user = otherUser,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            MessageInput(
                onSendMessage = { text ->
                    authViewModel.sendMessage(consultationId, text)
                },
                onAttachFileClick = {
                    filePickerLauncher.launch("*/*")
                }
            )
        }
    ) { paddingValues ->
        val listState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                coroutineScope.launch {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isSentByCurrentUser = message.senderId == currentUserId
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar(user: User?, onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Column {
                Text(
                    text = user?.nombre ?: "Chat",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (user != null) {
                    val subText = when (user.rol) {
                        "abogado" -> {
                            val availability = if (user.estado == "activo") "Disponible" else "No Disponible"
                            "Abogado ($availability)"
                        }
                        "cliente" -> {
                            val phoneInfo = user.telefono.ifEmpty { "Teléfono no disponible" }
                            "Cliente (Tel: $phoneInfo)"
                        }
                        else -> ""
                    }
                    Text(
                        text = subText,
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun MessageBubble(message: Message, isSentByCurrentUser: Boolean) {
    val alignment = if (isSentByCurrentUser) Alignment.CenterEnd else Alignment.CenterStart
    val backgroundColor = if (isSentByCurrentUser) MaterialTheme.colorScheme.primary else Color.DarkGray
    val shape = if (isSentByCurrentUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp)
    }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isSentByCurrentUser) 64.dp else 0.dp,
                end = if (isSentByCurrentUser) 0.dp else 64.dp
            ),
        contentAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(backgroundColor)
                .clickable(enabled = message.fileUrl != null) {
                    message.fileUrl?.let {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                        context.startActivity(intent)
                    }
                }
        ) {
            if (message.fileUrl != null) {
                FileMessageContent(message)
            } else {
                Text(
                    text = message.text,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun FileMessageContent(message: Message) {
    Row(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.InsertDriveFile,
            contentDescription = "Archivo",
            tint = Color.White,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = message.text.ifEmpty { "Archivo" },
            color = Color.White,
            textDecoration = TextDecoration.Underline
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    onAttachFileClick: () -> Unit
) {
    var text by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón para adjuntar archivos
            IconButton(onClick = onAttachFileClick) {
                Icon(
                    Icons.Default.AttachFile,
                    contentDescription = "Adjuntar archivo",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            IconButton(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = text.isNotBlank()
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Enviar mensaje",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}