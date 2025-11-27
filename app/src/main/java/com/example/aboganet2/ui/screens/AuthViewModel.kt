package com.example.aboganet2.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aboganet2.data.User
import com.example.aboganet2.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// Estados para el flujo de login/registro
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
    object RegistrationSuccess : AuthState()
}

// Estados para la comprobación de sesión al inicio de la app
sealed class SessionState {
    object Loading : SessionState()
    data class LoggedIn(val role: String) : SessionState()
    object LoggedOut : SessionState()
}

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    // --- ESTADOS ---
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile


    // --- FUNCIONES ---

    /**
     * Comprueba si hay una sesión activa al iniciar la app.
     * Actualiza el sessionState a LoggedIn(role) o LoggedOut.
     */
    fun checkActiveSession() {
        viewModelScope.launch {
            val role = authRepository.getCurrentUserRole()
            if (role != null) {
                _sessionState.value = SessionState.LoggedIn(role)
            } else {
                _sessionState.value = SessionState.LoggedOut
            }
        }
    }

    /**
     * Inicia sesión con email y contraseña.
     * En caso de éxito, obtiene el rol del usuario y lo emite en authState.
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.loginUser(email, password)
            result.onSuccess { role ->
                _authState.value = AuthState.Success(role)
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Registra un nuevo usuario en Firebase Auth y Firestore.
     */
    fun register(user: User, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.registerUser(user, password)
            result.onSuccess {
                _authState.value = AuthState.RegistrationSuccess
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Obtiene los datos del perfil del usuario actualmente logueado desde Firestore.
     */
    fun fetchUserProfile() {
        viewModelScope.launch {
            val result = authRepository.getUserProfile()
            result.onSuccess { user ->
                _userProfile.value = user
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error al cargar el perfil.")
            }
        }
    }

    /**
     * Actualiza la URL de la foto de perfil en Firestore.
     * Si tiene éxito, vuelve a cargar el perfil para refrescar la UI.
     */
    fun updateUserProfilePicture(newUrl: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.updateProfilePictureUrl(newUrl)
            result.onSuccess {
                fetchUserProfile() // Refresca los datos del perfil
                _authState.value = AuthState.Idle
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error al actualizar la foto.")
            }
        }
    }

    /**
     * Cierra la sesión del usuario actual en Firebase.
     */
    fun logout() {
        authRepository.logout()
    }

    /**
     * Resetea el authState a Idle para evitar que se muestren mensajes de error/éxito antiguos.
     */
    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}