package com.mswdo.kalayaan.faced.ui.login

import android.util.Log
import androidx.credentials.Credential
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.mswdo.kalayaan.faced.data.entity.UserEntity
import com.mswdo.kalayaan.faced.data.entity.UserRole
import com.mswdo.kalayaan.faced.data.repository.SessionManager
import com.mswdo.kalayaan.faced.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val fullName: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val registered: Boolean = false
)

data class TwoFactorUiState(
    val codeSent: Boolean = false,
    val codeInput: String = "",
    val timerSeconds: Int = 60,
    val canResend: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVerified: Boolean = false
)

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val requires2FA: Boolean = false,
    val loggedInUser: UserEntity? = null
)

class LoginViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    private val _registerState = MutableStateFlow(RegisterUiState())
    val registerState: StateFlow<RegisterUiState> = _registerState.asStateFlow()

    private val _twoFactorState = MutableStateFlow(TwoFactorUiState())
    val twoFactorState: StateFlow<TwoFactorUiState> = _twoFactorState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private var pendingUserEntity: UserEntity? = null
    
    // Simulate stored code for prototype
    private var currentOtp: String = ""

    fun onLoginEmailChange(v: String) { _state.value = _state.value.copy(email = v, error = null) }
    fun onLoginPasswordChange(v: String) { _state.value = _state.value.copy(password = v, error = null) }

    fun loginWithEmail() {
        val s = _state.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _state.value = s.copy(error = "Please enter your email and password.")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val result = auth.signInWithEmailAndPassword(s.email, s.password).await()
                val user = result.user
                if (user != null) {
                    user.reload().await() // Get fresh verification status
                    
                    val entity = userRepository.getOrCreateUser(
                        username = user.email ?: "unknown",
                        fullName = user.displayName ?: "Staff Member",
                        avatarInitials = (user.email?.take(2) ?: "ST").uppercase()
                    )
                    var currentEntity = entity
                    val userEmail = user.email ?: ""
                    
                    if (userEmail.isNotBlank()) {
                        try {
                            val firebaseDb = FirebaseDatabase.getInstance("https://facedform-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
                            val snapshot = firebaseDb.child("users").child(userEmail.replace(".", "_")).get().await()
                            if (snapshot.exists()) {
                                val map = snapshot.value as? Map<*, *>
                                val remoteIsActive = (map?.get("active") as? Boolean) ?: (map?.get("isActive") as? Boolean) ?: false
                                if (remoteIsActive && !currentEntity.isActive) {
                                    val local = userRepository.findUserByUsername(userEmail)
                                    if (local != null) {
                                        userRepository.setActive(local.id, true)
                                        currentEntity = currentEntity.copy(isActive = true)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("LoginViewModel", "Failed to check live approval from Firebase", e)
                        }
                    }

                    pendingUserEntity = currentEntity
                    
                    if (currentEntity.role != UserRole.ADMIN && !currentEntity.isActive) {
                        _state.value = _state.value.copy(isLoading = false, error = "Your account is pending admin approval. Please wait for an administrator to activate your account.")
                        return@launch
                    }

                    if (!user.isEmailVerified) {
                        // STRICT BLOCK: Email must be verified via Firebase link
                        try { user.sendEmailVerification().await() } catch (_: Exception) {}
                        _state.value = _state.value.copy(isLoading = false, requires2FA = true)
                        _twoFactorState.value = _twoFactorState.value.copy(
                            error = "Email not verified. A verification link has been sent to ${user.email}. Please click the link first."
                        )
                    } else {
                        // Email verified -> Log in
                        SessionManager.login(entity)
                        _state.value = _state.value.copy(isLoading = false, loggedInUser = entity)
                    }
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("auth credential", ignoreCase = true) == true -> "Incorrect email or password, or Google Sign-In is not enabled in Firebase Console."
                    e.message?.contains("user-not-found", ignoreCase = true) == true -> "No account found with this email address. Please register first."
                    else -> e.localizedMessage ?: "Invalid login credentials."
                }
                _state.value = _state.value.copy(isLoading = false, error = msg)
            }
        }
    }

    fun onRegisterEmailChange(v: String) { _registerState.value = _registerState.value.copy(email = v, error = null) }
    fun onRegisterPasswordChange(v: String) { _registerState.value = _registerState.value.copy(password = v, error = null) }
    fun onRegisterNameChange(v: String) { _registerState.value = _registerState.value.copy(fullName = v, error = null) }

    fun register() {
        val s = _registerState.value
        if (s.email.isBlank() || s.password.isBlank() || s.fullName.isBlank()) {
            _registerState.value = s.copy(error = "Please fill in all fields.")
            return
        }
        if (s.password.length < 6) {
            _registerState.value = s.copy(error = "Password must be at least 6 characters.")
            return
        }
        
        viewModelScope.launch {
            _registerState.value = _registerState.value.copy(isLoading = true, error = null)
            try {
                // Delete user from Firebase Auth to simulate a reset for testing
                try {
                    val signInResult = auth.signInWithEmailAndPassword(s.email, s.password).await()
                    signInResult.user?.delete()?.await()
                } catch (_: Exception) {
                    // Ignore if account doesn't exist
                }

                val result = auth.createUserWithEmailAndPassword(s.email, s.password).await()
                result.user?.let { u ->
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(s.fullName)
                        .build()
                    u.updateProfile(profileUpdates).await()
                    
                    // Send official Firebase email verification to the user's inbox
                    try {
                        u.sendEmailVerification().await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    
                    userRepository.getOrCreateUser(
                        username = s.email,
                        fullName = s.fullName,
                        avatarInitials = s.fullName.take(2).uppercase()
                    )
                    
                    _registerState.value = _registerState.value.copy(isLoading = false, registered = true)
                }
            } catch (e: Exception) {
                _registerState.value = _registerState.value.copy(isLoading = false, error = e.localizedMessage ?: "Registration failed.")
            }
        }
    }

    fun resetRegistration() {
        _registerState.value = RegisterUiState()
    }

    fun onTwoFactorCodeChange(code: String) {
        _twoFactorState.value = _twoFactorState.value.copy(codeInput = code, error = null)
    }

    fun sendTwoFactorCode() {
        sendVerificationEmail()
    }

    fun sendVerificationEmail() {
        _twoFactorState.value = _twoFactorState.value.copy(codeSent = true, error = null)
        viewModelScope.launch {
            val email = pendingUserEntity?.username ?: _state.value.email
            try {
                auth.currentUser?.sendEmailVerification()?.await()
            } catch (_: Exception) {
                try {
                    if (email.isNotBlank() && email.contains("@")) {
                        auth.sendPasswordResetEmail(email).await()
                    }
                } catch (_: Exception) {
                    _twoFactorState.value = _twoFactorState.value.copy(
                        error = "Could not send verification email. Please check your internet connection."
                    )
                }
            }
        }
    }
    
    fun getMockCode(): String = ""

    fun verifyTwoFactorCode() {
        checkEmailVerified()
    }

    fun checkEmailVerified() {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                user?.reload()?.await()
                if (user?.isEmailVerified == true) {
                    SessionManager.last2faTimeMillis = System.currentTimeMillis()
                    pendingUserEntity?.let {
                        SessionManager.login(it)
                        _state.value = _state.value.copy(loggedInUser = it)
                    }
                    _twoFactorState.value = _twoFactorState.value.copy(isVerified = true)
                } else {
                    _twoFactorState.value = _twoFactorState.value.copy(error = "Email not verified yet. Please check your inbox and click the verification link.")
                }
            } catch (e: Exception) {
                _twoFactorState.value = _twoFactorState.value.copy(error = e.localizedMessage ?: "Verification check failed.")
            }
        }
    }

    fun signInWithGoogle(credential: GoogleIdTokenCredential) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val googleIdToken = credential.idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                
                val firebaseUser = authResult.user
                if (firebaseUser != null) {
                    var userEntity = userRepository.getOrCreateUser(
                        username = firebaseUser.email ?: "unknown",
                        fullName = firebaseUser.displayName ?: "Staff Member",
                        avatarInitials = (firebaseUser.displayName?.take(2) ?: "ST").uppercase()
                    )
                    
                    val userEmail = firebaseUser.email ?: ""
                    if (userEmail.isNotBlank()) {
                        try {
                            val firebaseDb = FirebaseDatabase.getInstance("https://facedform-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
                            val snapshot = firebaseDb.child("users").child(userEmail.replace(".", "_")).get().await()
                            if (snapshot.exists()) {
                                val map = snapshot.value as? Map<*, *>
                                val remoteIsActive = (map?.get("active") as? Boolean) ?: (map?.get("isActive") as? Boolean) ?: false
                                if (remoteIsActive && !userEntity.isActive) {
                                    val local = userRepository.findUserByUsername(userEmail)
                                    if (local != null) {
                                        userRepository.setActive(local.id, true)
                                        userEntity = userEntity.copy(isActive = true)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("LoginViewModel", "Failed to check live approval from Firebase", e)
                        }
                    }
                    
                    if (userEntity.role != UserRole.ADMIN && !userEntity.isActive) {
                        _state.value = _state.value.copy(isLoading = false, error = "Your account is pending admin approval. Please wait for an administrator to activate your account.")
                        return@launch
                    }
                    
                    SessionManager.login(userEntity)
                    _state.value = _state.value.copy(isLoading = false, loggedInUser = userEntity)
                } else {
                    _state.value = _state.value.copy(isLoading = false, error = "Firebase authentication failed.")
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("auth credential", ignoreCase = true) == true -> "Google Sign-In is not enabled in Firebase Console yet. Please add SHA-1 fingerprint."
                    else -> e.localizedMessage ?: "Google Sign-In failed."
                }
                _state.value = _state.value.copy(isLoading = false, error = msg)
            }
        }
    }

    fun continueOffline() {
        SessionManager.loginGuest()
        _state.value = _state.value.copy(loggedInUser = SessionManager.GUEST_USER)
    }
}
