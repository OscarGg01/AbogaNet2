package com.example.aboganet2.domain

import androidx.compose.animation.core.copy
import com.example.aboganet2.data.Consultation
import com.example.aboganet2.data.FullLawyerProfile
import com.example.aboganet2.data.LawyerProfile
import com.example.aboganet2.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

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
            val authResult = firebaseAuth.createUserWithEmailAndPassword( user. email,  password).await()
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

    suspend fun submitConsultation(consultation: Consultation): Result<Unit> {
        return try {
            // Creamos un nuevo documento en la colección 'consultations'
            val document = firestore.collection("consultations").document()
            // Guardamos la consulta con el ID del documento asignado
            document.set(consultation.copy(id = document.id)).await()
            Result.success(Unit)
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
            userDocument.getString("role")
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
}