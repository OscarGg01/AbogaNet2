package com.example.aboganet2.data

import com.google.firebase.Timestamp

data class Consultation(
    val id: String = "", // ID autogenerado por Firestore
    val clientId: String = "",
    val lawyerId: String = "",
    val title: String = "",
    val description: String = "",
    val cost: Double = 0.0,
    val status: String = "Enviado",
    val timestamp: Timestamp = Timestamp.now()
)
