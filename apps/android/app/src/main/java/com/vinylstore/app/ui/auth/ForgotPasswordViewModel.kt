package com.vinylstore.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vinylstore.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ForgotPasswordUiState(
    val step: Int = 1,           // 1 = 输入邮箱, 2 = 输入验证码+新密码
    val email: String = "",
    val code: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val isSuccess: Boolean = false
)

class ForgotPasswordViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }
    fun onCodeChange(code: String) {
        _uiState.value = _uiState.value.copy(code = code, error = null)
    }
    fun onNewPasswordChange(pw: String) {
        _uiState.value = _uiState.value.copy(newPassword = pw, error = null)
    }
    fun onConfirmPasswordChange(pw: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = pw, error = null)
    }

    fun sendCode() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "请输入邮箱")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authRepository.forgotPassword(email)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    step = 2,
                    message = "验证码已发送到 $email"
                )
            } catch (e: Exception) {
                val msg = e.message ?: "发送失败"
                _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
            }
        }
    }

    fun resetPassword() {
        val state = _uiState.value
        when {
            state.code.isBlank() -> {
                _uiState.value = state.copy(error = "请输入验证码"); return
            }
            state.newPassword.length < 6 -> {
                _uiState.value = state.copy(error = "密码至少 6 位"); return
            }
            state.newPassword != state.confirmPassword -> {
                _uiState.value = state.copy(error = "两次密码不一致"); return
            }
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                authRepository.resetPassword(
                    state.email.trim(),
                    state.code.trim(),
                    state.newPassword
                )
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                val msg = e.message ?: "重置失败"
                _uiState.value = _uiState.value.copy(isLoading = false, error = msg)
            }
        }
    }

    class Factory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
                return ForgotPasswordViewModel(authRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
