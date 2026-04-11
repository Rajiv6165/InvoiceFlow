package com.invoiceflow.billing.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.invoiceflow.billing.model.User
import com.invoiceflow.billing.repository.AuthRepository
import com.invoiceflow.billing.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Login Screen
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val navigateToHome: Boolean = false,
    val navigateToRegister: Boolean = false
)

/**
 * UI State for Registration Screen
 */
data class RegistrationUiState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val storeName: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val navigateToLogin: Boolean = false,
    val navigateToHome: Boolean = false
)

/**
 * ViewModel for handling authentication logic
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : BaseViewModel() {
    
    companion object {
        private const val TAG = "AuthViewModel"
    }
    
    // Login state
    private val _loginUiState = MutableStateFlow(LoginUiState())
    val loginUiState: StateFlow<LoginUiState> = _loginUiState.asStateFlow()
    
    // Registration state
    private val _registrationUiState = MutableStateFlow(RegistrationUiState())
    val registrationUiState: StateFlow<RegistrationUiState> = _registrationUiState.asStateFlow()
    
    // Current user
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    init {
        // Check if user is already logged in
        checkCurrentUser()
    }
    
    /**
     * Check if user is currently logged in
     */
    private fun checkCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user
            Log.d(TAG, "Current user checked: ${user?.email}")
        }
    }
    
    /**
     * Update login form fields
     */
    fun updateLoginEmail(email: String) {
        _loginUiState.value = _loginUiState.value.copy(
            email = email,
            errorMessage = null
        )
    }
    
    fun updateLoginPassword(password: String) {
        _loginUiState.value = _loginUiState.value.copy(
            password = password,
            errorMessage = null
        )
    }
    
    /**
     * Login with email and password
     */
    fun login() {
        val currentState = _loginUiState.value
        
        // Validate input
        if (!validateLoginInput(currentState)) {
            return
        }
        
        viewModelScope.launch {
            _loginUiState.value = currentState.copy(isLoading = true)
            
            authRepository.signIn(currentState.email, currentState.password)
                .collect { result ->
                    when (result) {
                        is Result.Loading -> {
                            _loginUiState.value = currentState.copy(isLoading = true)
                        }
                        is Result.Success -> {
                            _currentUser.value = result.data
                            _loginUiState.value = currentState.copy(
                                isLoading = false,
                                successMessage = "Login successful!",
                                navigateToHome = true
                            )
                            Log.d(TAG, "Login successful: ${result.data.email}")
                        }
                        is Result.Error -> {
                            _loginUiState.value = currentState.copy(
                                isLoading = false,
                                errorMessage = result.message ?: "Login failed"
                            )
                            Log.e(TAG, "Login error: ${result.message}", result.exception)
                        }
                    }
                }
        }
    }
    
    /**
     * Update registration form fields
     */
    fun updateRegistrationName(name: String) {
        _registrationUiState.value = _registrationUiState.value.copy(
            name = name,
            errorMessage = null
        )
    }
    
    fun updateRegistrationEmail(email: String) {
        _registrationUiState.value = _registrationUiState.value.copy(
            email = email,
            errorMessage = null
        )
    }
    
    fun updateRegistrationPassword(password: String) {
        _registrationUiState.value = _registrationUiState.value.copy(
            password = password,
            errorMessage = null
        )
    }
    
    fun updateRegistrationConfirmPassword(confirmPassword: String) {
        _registrationUiState.value = _registrationUiState.value.copy(
            confirmPassword = confirmPassword,
            errorMessage = null
        )
    }
    
    fun updateRegistrationStoreName(storeName: String) {
        _registrationUiState.value = _registrationUiState.value.copy(
            storeName = storeName,
            errorMessage = null
        )
    }
    
    /**
     * Register new user
     */
    fun register() {
        val currentState = _registrationUiState.value
        
        // Validate input
        if (!validateRegistrationInput(currentState)) {
            return
        }
        
        viewModelScope.launch {
            _registrationUiState.value = currentState.copy(isLoading = true)
            
            authRepository.signUp(
                name = currentState.name,
                email = currentState.email,
                password = currentState.password,
                storeName = currentState.storeName
            ).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _registrationUiState.value = currentState.copy(isLoading = true)
                    }
                    is Result.Success -> {
                        _currentUser.value = result.data
                        _registrationUiState.value = currentState.copy(
                            isLoading = false,
                            successMessage = "Account created successfully!",
                            navigateToHome = true
                        )
                        Log.d(TAG, "Registration successful: ${result.data.email}")
                    }
                    is Result.Error -> {
                        _registrationUiState.value = currentState.copy(
                            isLoading = false,
                            errorMessage = result.message ?: "Registration failed"
                        )
                        Log.e(TAG, "Registration error: ${result.message}", result.exception)
                    }
                }
            }
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut().collect { result ->
                when (result) {
                    is Result.Success -> {
                        _currentUser.value = null
                        _loginUiState.value = LoginUiState()
                        _registrationUiState.value = RegistrationUiState()
                        Log.d(TAG, "Sign out successful")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Sign out error: ${result.message}", result.exception)
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * Navigate to login screen
     */
    fun navigateToLogin() {
        _loginUiState.value = _loginUiState.value.copy(navigateToRegister = false)
        _registrationUiState.value = _registrationUiState.value.copy(navigateToLogin = false)
    }
    
    fun navigateToRegister() {
        _loginUiState.value = _loginUiState.value.copy(navigateToRegister = true)
    }
    
    fun clearMessages() {
        _loginUiState.value = _loginUiState.value.copy(errorMessage = null, successMessage = null)
        _registrationUiState.value = _registrationUiState.value.copy(errorMessage = null, successMessage = null)
    }
    
    /**
     * Validate login input
     */
    private fun validateLoginInput(state: LoginUiState): Boolean {
        if (state.email.isBlank()) {
            _loginUiState.value = state.copy(errorMessage = "Email is required")
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _loginUiState.value = state.copy(errorMessage = "Invalid email address")
            return false
        }
        
        if (state.password.isBlank()) {
            _loginUiState.value = state.copy(errorMessage = "Password is required")
            return false
        }
        
        if (state.password.length < 6) {
            _loginUiState.value = state.copy(errorMessage = "Password must be at least 6 characters")
            return false
        }
        
        return true
    }
    
    /**
     * Validate registration input
     */
    private fun validateRegistrationInput(state: RegistrationUiState): Boolean {
        if (state.name.isBlank()) {
            _registrationUiState.value = state.copy(errorMessage = "Name is required")
            return false
        }
        
        if (state.email.isBlank()) {
            _registrationUiState.value = state.copy(errorMessage = "Email is required")
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _registrationUiState.value = state.copy(errorMessage = "Invalid email address")
            return false
        }
        
        if (state.password.isBlank()) {
            _registrationUiState.value = state.copy(errorMessage = "Password is required")
            return false
        }
        
        if (state.password.length < 6) {
            _registrationUiState.value = state.copy(errorMessage = "Password must be at least 6 characters")
            return false
        }
        
        if (state.confirmPassword != state.password) {
            _registrationUiState.value = state.copy(errorMessage = "Passwords do not match")
            return false
        }
        
        return true
    }
}
