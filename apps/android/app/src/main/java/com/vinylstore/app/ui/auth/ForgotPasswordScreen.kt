package com.vinylstore.app.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vinylstore.app.VinylApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val app = VinylApp.instance
    val viewModel: ForgotPasswordViewModel = viewModel(
        factory = ForgotPasswordViewModel.Factory(app.authRepository)
    )
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("忘记密码") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            if (uiState.step == 1) {
                Step1SendCode(viewModel, uiState)
            } else {
                Step2ResetPassword(viewModel, uiState)
            }
        }
    }
}

@Composable
private fun Step1SendCode(
    viewModel: ForgotPasswordViewModel,
    uiState: ForgotPasswordUiState
) {
    Text("找回密码", style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(8.dp))
    Text("输入注册时使用的邮箱",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(40.dp))

    OutlinedTextField(
        value = uiState.email,
        onValueChange = viewModel::onEmailChange,
        label = { Text("邮箱") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall
    )

    if (uiState.error != null) {
        Spacer(Modifier.height(12.dp))
        Text(uiState.error!!, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = viewModel::sendCode,
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = MaterialTheme.shapes.extraSmall,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text("发送验证码", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Step2ResetPassword(
    viewModel: ForgotPasswordViewModel,
    uiState: ForgotPasswordUiState
) {
    Text("重置密码", style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground)
    Spacer(Modifier.height(8.dp))
    if (uiState.message != null) {
        Text(uiState.message!!, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    Spacer(Modifier.height(32.dp))

    OutlinedTextField(
        value = uiState.code,
        onValueChange = viewModel::onCodeChange,
        label = { Text("验证码") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall
    )

    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = uiState.newPassword,
        onValueChange = viewModel::onNewPasswordChange,
        label = { Text("新密码（至少6位）") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall
    )

    Spacer(Modifier.height(16.dp))

    OutlinedTextField(
        value = uiState.confirmPassword,
        onValueChange = viewModel::onConfirmPasswordChange,
        label = { Text("确认新密码") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraSmall
    )

    if (uiState.error != null) {
        Spacer(Modifier.height(12.dp))
        Text(uiState.error!!, color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }

    Spacer(Modifier.height(24.dp))

    Button(
        onClick = viewModel::resetPassword,
        enabled = !uiState.isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        shape = MaterialTheme.shapes.extraSmall,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        if (uiState.isLoading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary)
        } else {
            Text("重置密码", style = MaterialTheme.typography.labelLarge)
        }
    }
}
