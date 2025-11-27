package com.example.aboganet2.data

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class User(
    val id: String = "",
    val nombre: String = "",
    val dni: String = "",
    val email: String = "",
    val telefono: String = "",
    val direccion: String = "",
    val fotoUrl: String = "",
    val rol: String = "cliente",
    val estado: String = "activo",
    @ServerTimestamp
    val createdAt: Date? = null
)