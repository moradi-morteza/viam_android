package com.app.viam.ui.warehouse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.viam.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanTransactScreen(
    viewModel: ScanTransactViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }
    var manualCode by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Show success snackbar
    LaunchedEffect(uiState.transactionSuccess) {
        if (uiState.transactionSuccess) {
            snackbarHostState.showSnackbar("تراکنش با موفقیت ثبت شد")
            viewModel.onSuccessConsumed()
        }
    }

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(uiState.error!!)
            viewModel.onErrorDismissed()
        }
    }

    // Show scanner overlay
    if (showScanner) {
        QrScanScreen(
            onQrScanned = { raw ->
                showScanner = false
                viewModel.onQrScanned(raw)
            },
            onBack = { showScanner = false }
        )
        return
    }

    // Show transaction sheet when box is loaded
    if (uiState.showTransactionSheet && uiState.box != null) {
        val box = uiState.box!!
        val locationParts = listOfNotNull(
            box.row?.shelf?.zone?.name,
            box.row?.shelf?.name,
            box.row?.name
        )
        TransactionFormSheet(
            boxLabel = box.code,
            boxLocation = locationParts.joinToString(" › "),
            currentQuantity = box.quantity,
            isLoading = uiState.isSubmitting,
            error = uiState.transactionError,
            onDismiss = { viewModel.onDismissSheet() },
            onSubmit = { req -> viewModel.submitTransaction(req) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_transaction)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowForwardIos, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Loading indicator
            AnimatedVisibility(
                visible = uiState.isLoadingBox,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // QR Scan button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                onClick = { showScanner = true }
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.scan_qr_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.scan_qr_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Divider with "یا"
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(R.string.scan_or_enter),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // Manual box code entry
            OutlinedTextField(
                value = manualCode,
                onValueChange = { manualCode = it },
                label = { Text(stringResource(R.string.box_code_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        keyboard?.hide()
                        viewModel.onCodeEntered(manualCode)
                    }
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            keyboard?.hide()
                            viewModel.onCodeEntered(manualCode)
                        },
                        enabled = manualCode.isNotBlank() && !uiState.isLoadingBox
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = stringResource(R.string.box_code_search_button))
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    keyboard?.hide()
                    viewModel.onCodeEntered(manualCode)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = manualCode.isNotBlank() && !uiState.isLoadingBox
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.box_code_search_button))
            }
        }
    }
}
