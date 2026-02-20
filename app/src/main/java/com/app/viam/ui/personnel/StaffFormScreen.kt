package com.app.viam.ui.personnel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.viam.R
import com.app.viam.ui.common.LtrFormField
import com.app.viam.ui.common.RtlFormField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffFormScreen(
    viewModel: StaffFormViewModel,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaveSuccess) {
        if (uiState.isSaveSuccess) {
            viewModel.onSaveNavigated()
            onSaveSuccess()
        }
    }

    val title = if (viewModel.isEditMode)
        stringResource(R.string.personnel_edit)
    else
        stringResource(R.string.personnel_create)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp)
        ) {
            RtlFormField(
                value = uiState.name,
                onValueChange = viewModel::onNameChange,
                label = stringResource(R.string.profile_name),
                error = uiState.nameError,
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            LtrFormField(
                value = uiState.username,
                onValueChange = viewModel::onUsernameChange,
                label = stringResource(R.string.profile_username),
                error = uiState.usernameError,
                keyboardType = KeyboardType.Text,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Password field
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = viewModel::onPasswordChange,
                    label = {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                if (viewModel.isEditMode)
                                    stringResource(R.string.personnel_password_optional)
                                else
                                    stringResource(R.string.password_label)
                            )
                        }
                    },
                    singleLine = true,
                    isError = uiState.passwordError != null,
                    supportingText = uiState.passwordError?.let { err ->
                        {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(err)
                            }
                        }
                    },
                    visualTransformation = if (uiState.isPasswordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = viewModel::onTogglePasswordVisibility) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible)
                                    Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            LtrFormField(
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                label = stringResource(R.string.profile_email),
                error = uiState.emailError,
                keyboardType = KeyboardType.Email,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            LtrFormField(
                value = uiState.mobile,
                onValueChange = viewModel::onMobileChange,
                label = stringResource(R.string.profile_mobile),
                error = uiState.mobileError,
                keyboardType = KeyboardType.Phone,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))

            RtlFormField(
                value = uiState.address,
                onValueChange = viewModel::onAddressChange,
                label = stringResource(R.string.profile_address),
                error = null,
                keyboardType = KeyboardType.Text,
                singleLine = false,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            uiState.generalError?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Button(
                onClick = viewModel::onSaveClicked,
                enabled = !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.save))
                }
            }
        }
    }
}
