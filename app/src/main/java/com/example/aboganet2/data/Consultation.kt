package com.example.aboganet2.data

import com.google.firebase.Timestamp

data class Consultation(
    val id: String = "",
    val clientId: String = "",
    val lawyerId: String = "",
    val title: String = "",
    val description: String = "",
    val cost: Double = 0.0,
    val status: String = "pendiente",
    val timestamp: Timestamp = Timestamp.now(),
    val appointmentTimestamp: Timestamp? = null,
    val finalCost: Double? = null
)
