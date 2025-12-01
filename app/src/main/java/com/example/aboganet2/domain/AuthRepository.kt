package com.example.aboganet2.domain

import com.example.aboganet2.data.Consultation
import com.example.aboganet2.data.FullLawyerProfile
import com.example.aboganet2.data.LawyerProfile
import com.example.aboganet2.data.User
import com.example.aboganet2.data.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    suspend fun getLawyerProfile(userId: String): Result<LawyerProfile> {
        return try {
            val document = firestore.collection("lawyer_profiles").document(userId).get().await()
            val profile = document.toObject(LawyerProfile::class.java)
                ?: throw Exception("No se encontraron los datos profesionales del abogado.")
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerUser(user: User, password: String): Result<String> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(user.email, password).await()
            val firebaseUser = authResult.user

            if (firebaseUser != null) {
                val userWithUid = user.copy(uid = firebaseUser.uid)
                firestore.collection("users").document(firebaseUser.uid)
                    .set(userWithUid)
                    .await()
                Result.success(user.rol)
            } else {
                Result.failure(Exception("No se pudo crear el usuario en Firebase."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getClientConsultations(clientId: String): Result<List<Consultation>> {
        return try {
            val snapshot = firestore.collection("consultations")
                .whereEqualTo("clientId", clientId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            val consultations = snapshot.toObjects(Consultation::class.java)
            Result.success(consultations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBookedAppointments(lawyerId: String): List<Timestamp> {
        return try {
            // Consulta simplificada: solo filtramos por el ID del abogado
            val snapshot = firestore.collection("consultations")
                .whereEqualTo("lawyerId", lawyerId)
                .get()
                .await()

            Log.d("AuthRepository", "Se encontraron ${snapshot.size()} citas para el abogado $lawyerId")
            snapshot.documents.mapNotNull { it.getTimestamp("appointmentTimestamp") }
        } catch (e: Exception) {
            // Este Log es crucial, lo mantenemos
            Log.e("AuthRepository", "Error fetching booked appointments", e)
            emptyList()
        }
    }

    suspend fun getUserBasicInfo(userId: String): User? {
        return try {
            firestore.collection("users").document(userId).get().await().toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun submitConsultation(consultation: Consultation): Result<String> {
        return try {
            val document = firestore.collection("consultations").document()
            val consultationWithId = consultation.copy(id = document.id)
            document.set(consultationWithId).await()
            Result.success(document.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllActiveLawyers(): Result<List<FullLawyerProfile>> {
        return try {
            val activeLawyers = mutableListOf<FullLawyerProfile>()

            val userProfiles = firestore.collection("users")
                .whereEqualTo("rol", "abogado")
                .whereEqualTo("estado", "activo")
                .get()
                .await()

            for (userDoc in userProfiles.documents) {
                val basicInfo = userDoc.toObject(User::class.java)
                if (basicInfo != null) {
                    val userId = userDoc.id
                    val profileDoc = firestore.collection("lawyer_profiles").document(userId).get().await()

                    val professionalInfo = if (profileDoc.exists()) {
                        profileDoc.toObject(LawyerProfile::class.java)
                    } else {
                        LawyerProfile()
                    }

                    activeLawyers.add(FullLawyerProfile(basicInfo, professionalInfo))
                }
            }
            Result.success(activeLawyers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFinalCost(consultationId: String, finalCost: Double): Result<Unit> {
        return try {
            firestore.collection("consultations").document(consultationId)
                .update("finalCost", finalCost)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, password: String): Result<String> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Usuario no encontrado.")

            val userDocument = firestore.collection("users").document(firebaseUser.uid).get().await()

            if (!userDocument.exists()) {
                throw Exception("Los datos del usuario no se encontraron en la base de datos.")
            }

            val userRole = userDocument.getString("rol") ?: throw Exception("El rol del usuario no está definido.")
            val userStatus = userDocument.getString("estado") ?: "activo"

            if (userStatus != "activo") {
                firebaseAuth.signOut()
                throw Exception("Este usuario ha sido desactivado.")
            }

            Result.success(userRole)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(userId: String): Result<User> {
        return try {
            val document = firestore.collection("users").document(userId).get().await()
            val user = document.toObject(User::class.java)
                ?: throw Exception("No se encontraron los datos del perfil.")
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserRole(): String? {
        val firebaseUser = firebaseAuth.currentUser ?: return null

        return try {
            val userDocument = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (!userDocument.exists() || userDocument.getString("estado") != "activo") {
                firebaseAuth.signOut()
                return null
            }
            userDocument.getString("rol")
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateProfilePictureUrl(newUrl: String): Result<Unit> {
        return try {
            val userId = firebaseAuth.currentUser?.uid ?: throw Exception("Usuario no autenticado.")
            firestore.collection("users").document(userId)
                .update("fotoUrl", newUrl)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveLawyerProfile(userId: String, profile: LawyerProfile): Result<Unit> {
        return try {
            firestore.collection("lawyer_profiles").document(userId)
                .set(profile)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }

    suspend fun sendMessage(consultationId: String, message: Message): Result<Unit> {
        return try {
            firestore.collection("consultations").document(consultationId)
                .collection("messages").add(message).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChatMessages(consultationId: String): Flow<List<Message>> {
        return firestore.collection("consultations").document(consultationId)
            .collection("messages")
            .orderBy("timestamp")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Message::class.java)
            }
    }

    suspend fun getLawyerConsultations(lawyerId: String): Result<List<Consultation>> {
        return try {
            val snapshot = firestore.collection("consultations")
                .whereEqualTo("lawyerId", lawyerId)
                .get()
                .await()
            var consultations = snapshot.toObjects(Consultation::class.java)
            consultations = consultations.sortedByDescending { it.timestamp }
            Result.success(consultations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMessages(consultationId: String): Result<List<Message>> {
        return try {
            val snapshot = firestore.collection("consultations")
                .document(consultationId)
                .collection("messages")
                .orderBy("timestamp") // Ordenamos los mensajes por fecha
                .get()
                .await()

            val messages = snapshot.toObjects(Message::class.java)
            Result.success(messages)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error fetching messages for consultation $consultationId", e)
            Result.failure(e)
        }
    }

    suspend fun updateConsultationStatus(consultationId: String, newStatus: String): Result<Unit> {
        return try {
            firestore.collection("consultations").document(consultationId)
                .update("status", newStatus)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadFileToChat(consultationId: String, fileUri: Uri): Result<String> {
        return try {
            val fileName = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("chats/$consultationId/$fileName")
            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}