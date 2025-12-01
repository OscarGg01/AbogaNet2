// En data/Message.kt
package com.example.aboganet2.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    @ServerTimestamp
    val timestamp: Timestamp? = null,
    val fileUrl: String? = null,
    val fileType: String? = null
)