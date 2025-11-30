package com.example.aboganet2.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aboganet2.R

private enum class PaymentMethod { NONE, CARD, YAPE_PLIN }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    lawyerName: String,
    cost: Double,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf(PaymentMethod.NONE) }
    var cardNumber by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    val isCardFormValid = cardNumber.length == 16 && expiryDate.matches(Regex("(0[1-9]|1[0-2])/[0-9]{2}")) && cvv.length == 3

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pagar Primera Consulta", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White) } },
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
            PaymentSummary(lawyerName = lawyerName, cost = cost)
            Spacer(modifier = Modifier.height(24.dp))

            Text("Seleccione un método de pago", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                PaymentMethodButton(
                    text = "Tarjeta",
                    isSelected = selectedPaymentMethod == PaymentMethod.CARD,
                    onClick = { selectedPaymentMethod = PaymentMethod.CARD },
                    modifier = Modifier.weight(1f)
                )
                PaymentMethodButton(
                    text = "Yape / Plin",
                    isSelected = selectedPaymentMethod == PaymentMethod.YAPE_PLIN,
                    onClick = { selectedPaymentMethod = PaymentMethod.YAPE_PLIN },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            when (selectedPaymentMethod) {
                PaymentMethod.CARD -> {
                    CardPaymentForm(
                        cardNumber = cardNumber,
                        onCardNumberChange = { if (it.length <= 16) cardNumber = it },
                        isCardNumberValid = isCardFormValid,
                        expiryDate = expiryDate,
                        onExpiryDateChange = { if (it.length <= 5) expiryDate = it },
                        isExpiryDateValid = isCardFormValid,
                        cvv = cvv,
                        onCvvChange = { if (it.length <= 3) cvv = it },
                        isCvvValid = isCardFormValid
                    )
                }
                PaymentMethod.YAPE_PLIN -> {
                    YapePlinInfo()
                }
                PaymentMethod.NONE -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Seleccione una opción para continuar", color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            if (selectedPaymentMethod == PaymentMethod.CARD) {
                Button(
                    onClick = onPaymentSuccess,
                    enabled = isCardFormValid,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Pagar S/ %.2f".format(cost), fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun YapePlinInfo() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Escanea el QR o usa el número para realizar el pago.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(20.dp))

        Image(
            painter = painterResource(id = R.drawable.yape_qr),
            contentDescription = "Código QR para pago",
            modifier = Modifier
                .size(200.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            "987 654 321",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Al realizar el pago se procederá con los datos de la consulta.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
private fun PaymentSummary(lawyerName: String, cost: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Resumen de la Consulta", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Profesional:", color = Color.Gray)
                Text(lawyerName, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total a Pagar:", color = Color.Gray)
                Text(
                    "S/ %.2f".format(cost),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray
        )
    ) {
        Text(text, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray)
    }
}

@Composable
private fun CardPaymentForm(
    cardNumber: String, onCardNumberChange: (String) -> Unit, isCardNumberValid: Boolean,
    expiryDate: String, onExpiryDateChange: (String) -> Unit, isExpiryDateValid: Boolean,
    cvv: String, onCvvChange: (String) -> Unit, isCvvValid: Boolean
) {
    Column {
        OutlinedTextField(
            value = cardNumber,
            onValueChange = onCardNumberChange,
            label = { Text("Número de Tarjeta") },
            leadingIcon = { Icon(Icons.Default.CreditCard, "Card") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = cardNumber.isNotEmpty() && !isCardNumberValid,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = expiryDate,
                onValueChange = onExpiryDateChange,
                label = { Text("MM/AA") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = expiryDate.isNotEmpty() && !isExpiryDateValid,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = cvv,
                onValueChange = onCvvChange,
                label = { Text("CVV") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = cvv.isNotEmpty() && !isCvvValid,
                modifier = Modifier.weight(1f)
            )
        }
    }
}