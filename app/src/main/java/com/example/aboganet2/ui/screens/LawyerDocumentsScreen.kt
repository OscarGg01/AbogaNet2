package com.example.aboganet2.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aboganet2.data.Message
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun LawyerDocumentsScreen(authViewModel: AuthViewModel = viewModel()) {
    val allConsultations by authViewModel.lawyerConsultations.collectAsState()
    val currentUserId = authViewModel.getCurrentUserId()
    var allDocuments by remember { mutableStateOf<Map<String, List<Message>>>(emptyMap()) }

    LaunchedEffect(allConsultations) {
        allConsultations.forEach { (consultation, _) ->
            if (!allDocuments.containsKey(consultation.id)) {
                val result = authViewModel.getMessagesForConsultation(consultation.id)
                result.onSuccess { messages ->
                    // CORRECCIÓN 1: Usar fileUrl != null para identificar documentos
                    val documents = messages.filter { it.fileUrl != null }
                    if (documents.isNotEmpty()) {
                        allDocuments = allDocuments + (consultation.id to documents)
                    }
                }
            }
        }
    }

    val documentsByConsultation = allConsultations
        .mapNotNull { (consultation, client) ->
            val documents = allDocuments[consultation.id]
            if (!documents.isNullOrEmpty()) {
                Triple(consultation.title, client?.nombre ?: "Cliente desconocido", documents)
            } else {
                null
            }
        }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        if (documentsByConsultation.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay documentos en ninguno de tus casos.")
                }
            }
        } else {
            items(documentsByConsultation) { (caseTitle, clientName, documents) ->
                Text(
                    text = caseTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Caso con $clientName",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Divider(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

                documents.forEach { documentMessage ->
                    DocumentCard(
                        document = documentMessage,
                        clientName = clientName,
                        currentUserId = currentUserId ?: ""
                    )
                }
            }
        }
    }
}

@Composable
fun DocumentCard(
    document: Message,
    clientName: String,
    currentUserId: String
) {
    val isSentByLawyer = document.senderId == currentUserId
    val cardColor = if (isSentByLawyer) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    val senderText = if (isSentByLawyer) "Enviado por ti" else "Recibido de $clientName"

    val fileName = document.text.ifEmpty { "Archivo adjunto" }

    // CORRECCIÓN 3: Usar el campo 'fileType' para el icono.
    val fileIcon = getIconForFileType(document.fileType)
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = fileIcon,
                contentDescription = "File type",
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = senderText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val formattedDate = document.timestamp?.toDate()?.let {
                    SimpleDateFormat("dd MMM yyyy", Locale("es", "ES")).format(it)
                } ?: ""
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {
                document.fileUrl?.let { url ->
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                }
            }) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = "Ver Documento",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun getIconForFileType(fileType: String?): ImageVector {
    return when {
        fileType?.startsWith("image/") == true -> Icons.Default.Image
        fileType == "application/pdf" -> Icons.Default.PictureAsPdf
        else -> Icons.Default.InsertDriveFile
    }
}