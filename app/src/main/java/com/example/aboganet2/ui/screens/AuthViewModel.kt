package com.example.aboganet2.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aboganet2.data.Consultation
import com.example.aboganet2.data.FullLawyerProfile
import com.example.aboganet2.data.LawyerProfile
import com.example.aboganet2.data.User
import com.example.aboganet2.domain.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String) : AuthState()
    data class Error(val message: String) : AuthState()
    object RegistrationSuccess : AuthState()
}

sealed class SessionState {
    object Loading : SessionState()
    data class LoggedIn(val role: String) : SessionState()
    object LoggedOut : SessionState()
}

class AuthViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _fullLawyerProfile = MutableStateFlow(FullLawyerProfile())
    val fullLawyerProfile: StateFlow<FullLawyerProfile> = _fullLawyerProfile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _availableLawyers = MutableStateFlow<List<FullLawyerProfile>>(emptyList())
    val availableLawyers: StateFlow<List<FullLawyerProfile>> = _availableLawyers

    private val _consultationState = MutableStateFlow<Boolean?>(null)
    val consultationState: StateFlow<Boolean?> = _consultationState

    fun submitConsultation(consultation: Consultation) {
        viewModelScope.launch {
            val result = authRepository.submitConsultation(consultation)
            _consultationState.value = result.isSuccess
        }
    }

    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }

    fun resetConsultationState() {
        _consultationState.value = null
    }

    fun fetchFullLawyerProfile() {
        val uid = authRepository.getCurrentUserId() ?: return
        // Ahora llama a la nueva función con el ID del usuario actual
        fetchLawyerProfileById(uid)
    }

    // CAMBIA EL NOMBRE DE ESTA FUNCIÓN
    fun fetchLawyerProfileById(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _fullLawyerProfile.value = FullLawyerProfile() // Limpiamos el perfil anterior

            val basicInfoResult = authRepository.getUserProfile(userId)
            val professionalInfoResult = authRepository.getLawyerProfile(userId)

            _fullLawyerProfile.value = FullLawyerProfile(
                basicInfo = basicInfoResult.getOrNull(),
                professionalInfo = professionalInfoResult.getOrNull()
            )
            _isLoading.value = false
        }
    }

    fun saveLawyerProfile(profile: LawyerProfile) {
        val uid = authRepository.getCurrentUserId() ?: return

        viewModelScope.launch {
            _isLoading.value = true
            val result = authRepository.saveLawyerProfile(uid, profile)
            result.onSuccess {
                fetchFullLawyerProfile()
            }.onFailure {
            }
        }
    }

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

    fun register(user: User, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.registerUser(user, password)
            result.onSuccess { role ->
                // --- CAMBIO CLAVE ---
                // En lugar de emitir un estado de registro separado,
                // emitimos el mismo estado de éxito que el login.
                // La UI (AppNavigation) ya sabe cómo reaccionar a este estado.
                _authState.value = AuthState.Success(role)
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error desconocido en el registro")
            }
        }
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId()
            if (uid == null) {
                _authState.value = AuthState.Error("No hay un usuario autenticado.")
                return@launch
            }

            val result = authRepository.getUserProfile(uid)
            result.onSuccess { user ->
                _userProfile.value = user
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error al cargar el perfil.")
            }
        }
    }

    fun updateUserProfilePicture(newUrl: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val result = authRepository.updateProfilePictureUrl(newUrl)
            result.onSuccess {
                fetchUserProfile()
                _authState.value = AuthState.Idle
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error al actualizar la foto.")
            }
        }
    }

    fun fetchAllActiveLawyers() {
        viewModelScope.launch {
            _isLoading.value = true
            // Llamamos a la nueva función del repositorio
            val result = authRepository.getAllActiveLawyers()
            result.onSuccess { lawyers ->
                _availableLawyers.value = lawyers
            }.onFailure {
                // Manejar el error
            }
            _isLoading.value = false
        }
    }

    fun logout() {
        authRepository.logout()
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }
}