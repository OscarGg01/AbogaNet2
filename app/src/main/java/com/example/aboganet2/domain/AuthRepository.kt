package com.example.aboganet2.domain

import com.example.aboganet2.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun registerUser(user: User, password: String): Result<Unit> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(user.email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Error al crear el usuario.")

            val userWithId = user.copy(id = firebaseUser.uid)
            firestore.collection("users").document(firebaseUser.uid).set(userWithId).await()

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

    suspend fun getUserProfile(): Result<User> {
        return try {
            val userId = firebaseAuth.currentUser?.uid ?: throw Exception("Usuario no autenticado.")

            val document = firestore.collection("users").document(userId).get().await()

            val user = document.toObject(User::class.java) ?: throw Exception("No se encontraron los datos del perfil.")

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserRole(): String? {
        val firebaseUser = firebaseAuth.currentUser ?: return null // Si no hay usuario, devuelve null

        return try {
            val userDocument = firestore.collection("users").document(firebaseUser.uid).get().await()
            if (!userDocument.exists() || userDocument.getString("estado") != "activo") {
                firebaseAuth.signOut() // Limpiamos la sesión si los datos son inválidos
                return null
            }
            userDocument.getString("rol")
        } catch (e: Exception) {
            null // Si hay cualquier error, tratamos como si no hubiera sesión
        }
    }

    suspend fun updateProfilePictureUrl(newUrl: String): Result<Unit> {
        return try {
            // 1. Obtenemos el ID del usuario actual
            val userId = firebaseAuth.currentUser?.uid ?: throw Exception("Usuario no autenticado.")

            // 2. Actualizamos solo el campo "fotoUrl" en el documento correspondiente
            firestore.collection("users").document(userId)
                .update("fotoUrl", newUrl)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }
}