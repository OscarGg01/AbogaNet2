package com.example.aboganet2.ui.screens

import android.app.Application
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aboganet2.data.Consultation
import com.example.aboganet2.data.FullLawyerProfile
import com.example.aboganet2.data.LawyerProfile
import com.example.aboganet2.data.Message
import com.example.aboganet2.data.User
import com.example.aboganet2.domain.AuthRepository
import kotlinx.coroutines.Job
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

sealed class SubmissionState {
    object Idle : SubmissionState()
    data class Success(val consultationId: String) : SubmissionState()
    data class Error(val message: String) : SubmissionState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private var messagesJob: Job? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Loading)
    val sessionState: StateFlow<SessionState> = _sessionState

    private val _userProfile = MutableStateFlow<User?>(null)
    val userProfile: StateFlow<User?> = _userProfile

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _fullLawyerProfile = MutableStateFlow(FullLawyerProfile())
    val fullLawyerProfile: StateFlow<FullLawyerProfile> = _fullLawyerProfile

    private val _availableLawyers = MutableStateFlow<List<FullLawyerProfile>>(emptyList())
    val availableLawyers: StateFlow<List<FullLawyerProfile>> = _availableLawyers

    private val _clientConsultations = MutableStateFlow<List<Pair<Consultation, User?>>>(emptyList())
    val clientConsultations: StateFlow<List<Pair<Consultation, User?>>> = _clientConsultations

    private val _lawyerConsultations = MutableStateFlow<List<Pair<Consultation, User?>>>(emptyList())
    val lawyerConsultations: StateFlow<List<Pair<Consultation, User?>>> = _lawyerConsultations

    private val _chatMessages = MutableStateFlow<List<Message>>(emptyList())
    val chatMessages: StateFlow<List<Message>> = _chatMessages

    private val _submissionState = MutableStateFlow<SubmissionState>(SubmissionState.Idle)
    val submissionState: StateFlow<SubmissionState> = _submissionState

    fun fetchClientConsultations() {
        viewModelScope.launch {
            val clientId = getCurrentUserId() ?: return@launch
            _isLoading.value = true
            val result = authRepository.getClientConsultations(clientId)
            if (result.isSuccess) {
                val consultations = result.getOrNull() ?: emptyList()
                val newDetailedList = consultations.mapNotNull { consultation ->
                    val lawyerInfo = authRepository.getUserBasicInfo(consultation.lawyerId)
                    if (lawyerInfo != null) Pair(consultation, lawyerInfo) else null
                }
                _clientConsultations.value = newDetailedList
            }
            _isLoading.value = false
        }
    }

    fun fetchLawyerConsultations() {
        viewModelScope.launch {
            val lawyerId = getCurrentUserId() ?: return@launch
            _isLoading.value = true
            val result = authRepository.getLawyerConsultations(lawyerId)
            if (result.isSuccess) {
                val consultations = result.getOrNull() ?: emptyList()
                val newDetailedList = consultations.mapNotNull { consultation ->
                    val clientInfo = authRepository.getUserBasicInfo(consultation.clientId)
                    if (clientInfo != null) Pair(consultation, clientInfo) else null
                }
                _lawyerConsultations.value = newDetailedList
            }
            _isLoading.value = false
        }
    }

    fun getLawyerById(lawyerId: String, onResult: (FullLawyerProfile?) -> Unit) {
        viewModelScope.launch {
            val basicInfo = authRepository.getUserProfile(lawyerId).getOrNull()
            val profInfo = authRepository.getLawyerProfile(lawyerId).getOrNull()
            if (basicInfo != null && profInfo != null) {
                onResult(FullLawyerProfile(basicInfo, profInfo))
            } else {
                onResult(null)
            }
        }
    }

    fun submitConsultation(consultation: Consultation) {
        viewModelScope.launch {
            val result = authRepository.submitConsultation(consultation)
            result.onSuccess { consultationId ->
                fetchClientConsultations()
                _submissionState.value = SubmissionState.Success(consultationId)
            }.onFailure {
                _submissionState.value = SubmissionState.Error(it.message ?: "Error al enviar la consulta")
            }
        }
    }

    fun updateConsultationStatus(consultationId: String, newStatus: String) {
        viewModelScope.launch {
            authRepository.updateConsultationStatus(consultationId, newStatus).onSuccess {
                fetchLawyerConsultations()
            }
        }
    }

    fun updateFinalCost(consultationId: String, finalCost: Double) {
        viewModelScope.launch {
            authRepository.updateFinalCost(consultationId, finalCost).onSuccess {
                fetchLawyerConsultations()
                fetchClientConsultations()
            }
        }
    }

    fun resetConsultationState() {
        _submissionState.value = SubmissionState.Idle
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
            authRepository.loginUser(email, password).onSuccess { role ->
                _authState.value = AuthState.Success(role)
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error desconocido")
            }
        }
    }

    fun register(user: User, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.registerUser(user, password).onSuccess { role ->
                _authState.value = AuthState.Success(role)
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error desconocido en el registro")
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _userProfile.value = null
        _clientConsultations.value = emptyList()
        _lawyerConsultations.value = emptyList()
    }

    fun resetAuthState() {
        _authState.value = AuthState.Idle
    }

    fun getCurrentUserId(): String? {
        return authRepository.getCurrentUserId()
    }

    fun fetchUserProfile() {
        viewModelScope.launch {
            val uid = authRepository.getCurrentUserId() ?: return@launch
            authRepository.getUserProfile(uid).onSuccess { user ->
                _userProfile.value = user
            }.onFailure {
                _authState.value = AuthState.Error(it.message ?: "Error al cargar el perfil.")
            }
        }
    }

    fun fetchFullLawyerProfile() {
        val uid = authRepository.getCurrentUserId() ?: return
        fetchLawyerProfileById(uid)
    }

    fun fetchLawyerProfileById(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val basicInfo = authRepository.getUserProfile(userId).getOrNull()
            val profInfo = authRepository.getLawyerProfile(userId).getOrNull()
            _fullLawyerProfile.value = FullLawyerProfile(basicInfo, profInfo)
            _isLoading.value = false
        }
    }

    fun saveLawyerProfile(profile: LawyerProfile) {
        val uid = authRepository.getCurrentUserId() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            authRepository.saveLawyerProfile(uid, profile).onSuccess {
                fetchFullLawyerProfile()
            }
            _isLoading.value = false
        }
    }

    fun updateUserProfilePicture(newUrl: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            authRepository.updateProfilePictureUrl(newUrl).onSuccess {
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
            authRepository.getAllActiveLawyers().onSuccess { lawyers ->
                _availableLawyers.value = lawyers
            }
            _isLoading.value = false
        }
    }

    fun listenForMessages(consultationId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            authRepository.getChatMessages(consultationId).collect { messages ->
                _chatMessages.value = messages
            }
        }
    }

    fun sendMessage(consultationId: String, message: Message) {
        viewModelScope.launch {
            authRepository.sendMessage(consultationId, message)
        }
    }

    fun sendMessage(consultationId: String, text: String) {
        val senderId = getCurrentUserId() ?: return
        val message = Message(senderId = senderId, text = text)
        sendMessage(consultationId, message)
    }

    fun sendFile(consultationId: String, fileUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            val fileType = getMimeType(fileUri)
            authRepository.uploadFileToChat(consultationId, fileUri).onSuccess { downloadUrl ->
                val senderId = getCurrentUserId() ?: return@onSuccess
                val message = Message(
                    senderId = senderId,
                    text = "Archivo adjunto",
                    fileUrl = downloadUrl,
                    fileType = fileType
                )
                sendMessage(consultationId, message)
            }.onFailure {
                // Manejar el error de subida
            }
            _isLoading.value = false
        }
    }

    private fun getMimeType(uri: Uri): String? {
        return getApplication<Application>().contentResolver.getType(uri)
    }

    override fun onCleared() {
        super.onCleared()
        messagesJob?.cancel()
    }
}