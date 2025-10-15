package com.example.biometricclassdetector

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.biometricclassdetector.ui.theme.BiometricClassDetectorTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BiometricClassDetectorTheme {
                val deviceInfoState by viewModel.deviceInfo.collectAsState()
                val biometricPropertiesState by viewModel.biometricProperties.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.eventChannel.collect { event ->
                        when(event) {
                            is MainViewModel.UiEvent.ShowBiometricPrompt -> {
                                showBiometricPrompt()
                            }
                            is MainViewModel.UiEvent.ShowSecureBiometricPrompt -> {
                                showSecureBiometricPrompt(event.cryptoObject)
                            }
                            is MainViewModel.UiEvent.FailedToShowBiometricPrompt -> {
                                Toast.makeText(this@MainActivity,
                                    "Could not show the Biometric Prompt",
                                    Toast.LENGTH_SHORT)
                                    .show()
                            }
                            is MainViewModel.UiEvent.AuthenticationError -> {
                                Toast.makeText(this@MainActivity,
                                    "Authentication error: ${event.errorString}",
                                    Toast.LENGTH_SHORT)
                                    .show()
                            }
                            is MainViewModel.UiEvent.AuthenticationSucceeded -> {
                                Toast.makeText(this@MainActivity,
                                    "Authentication succeeded!",
                                    Toast.LENGTH_SHORT)
                                    .show()
                            }
                            is MainViewModel.UiEvent.AuthenticationFailed -> {
                                Toast.makeText(this@MainActivity,
                                    "Authentication failed",
                                    Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    }
                }

                var useCryptoObjectChecked by remember { mutableStateOf(false) }

                BiometricClassDisplayScreen(
                    deviceInfoState = deviceInfoState,
                    biometricPropertiesState = biometricPropertiesState,
                    useCryptoObjectChecked = useCryptoObjectChecked,
                    onUseCryptoObjectCheckedChange = {
                        useCryptoObjectChecked = it
                    },
                    onShowBiometricPromptClick = {
                        if (useCryptoObjectChecked) {
                            viewModel.showSecureBiometricPrompt()
                        } else {
                            viewModel.showBiometricPrompt()
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.retrieveBiometricProperties(this)
    }

    private fun createBiometricPrompt(): BiometricPrompt {
        val authenticationCallback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int,
                                               errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                viewModel.onAuthenticationError(errorCode, errString)
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                viewModel.onAuthenticationSucceeded(result.cryptoObject)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                viewModel.onAuthenticationFailed()
            }
        }
        return BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            authenticationCallback
        )
    }

    private fun createPromptInfo(allowedAuthenticators: Int) = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(allowedAuthenticators)
            .setNegativeButtonText("Cancel")
            .build()

    private fun showBiometricPrompt() {
        val biometricPrompt = createBiometricPrompt()
        val promptInfo =
            createPromptInfo(BiometricManager.Authenticators.BIOMETRIC_STRONG
                    or BiometricManager.Authenticators.BIOMETRIC_WEAK)
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showSecureBiometricPrompt(cryptoObject: BiometricPrompt.CryptoObject) {
        val biometricPrompt = createBiometricPrompt()
        val promptInfo = createPromptInfo(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        biometricPrompt.authenticate(promptInfo, cryptoObject)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricClassDisplayScreen(
    deviceInfoState: DeviceInfo,
    biometricPropertiesState: BiometricProperties?,
    useCryptoObjectChecked: Boolean,
    onUseCryptoObjectCheckedChange: ((Boolean) -> Unit)?,
    onShowBiometricPromptClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (!deviceInfoState.deviceName.lowercase().contains(deviceInfoState.deviceBrand.lowercase())) {
                                "${deviceInfoState.deviceBrand} ${deviceInfoState.deviceName} (${deviceInfoState.deviceModel})"
                            } else {
                                "${deviceInfoState.deviceName} (${deviceInfoState.deviceModel})"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Android ${deviceInfoState?.androidVersion} (API ${deviceInfoState?.androidApiLevel})",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            )
        },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    ExtendedFloatingActionButton(
                        onClick = onShowBiometricPromptClick,
                        icon = { Icon(Icons.Filled.Fingerprint, "Fingerprint icon") },
                        text = { Text(text = "Show Biometric Prompt") },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                    )
                }
            }
        },
    ) { innerPadding ->
        BiometricClassDisplay(
            state = biometricPropertiesState,
            useCryptoObjectChecked = useCryptoObjectChecked,
            onUseCryptoObjectCheckedChange = onUseCryptoObjectCheckedChange,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun BiometricClassDisplay(
    state: BiometricProperties?,
    useCryptoObjectChecked: Boolean,
    onUseCryptoObjectCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    if (state != null) {
        Column(
            modifier = modifier.padding(16.dp)
        ) {
            DeviceSecurityDisplay(
                isDeviceSecure = state.isDeviceSecure,
                biometricTypes = state.availableBiometricTypes,
                biometricClasses = state.availableBiometricClasses,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Switch(
                    checked = useCryptoObjectChecked,
                    onCheckedChange = onUseCryptoObjectCheckedChange,
                )
                Text(
                    text = "Use CryptoObject",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
fun DeviceSecurityDisplay(
    isDeviceSecure: Boolean,
    biometricTypes: List<BiometricType>,
    biometricClasses: List<BiometricClassDetails>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
    ) {
        Row (
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            val icon = if (isDeviceSecure) {
                Icons.Default.Lock
            } else Icons.Default.Warning
            val iconTint = if (isDeviceSecure) {
                Color(0xFF4CAF50)
            } else Color(0xFFF44336)
            val text = if (isDeviceSecure) {
                "Secure lock (PIN, pattern or password) set"
            } else "NO secure lock (PIN, pattern or password) set"
            Icon(
                icon,
                tint = iconTint,
                contentDescription = null,
            )
            Text(
                text = text,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = "Available biometric types: ${biometricTypes.joinToString(separator = ", ")}",
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = "Available biometric classes:",
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column() {
            biometricClasses.forEach {
                val icon = if (it.enrolled) {
                    Icons.Default.CheckCircle
                } else Icons.Default.Cancel
                val iconTint = if (isDeviceSecure) {
                    Color(0xFF4CAF50)
                } else Color(0xFFF44336)
                val text = if (isDeviceSecure) {
                    "${it.biometricClass} enrolled"
                } else "${it.biometricClass} NOT enrolled"
                Row (
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(
                        icon,
                        tint = iconTint,
                        contentDescription = null,
                    )
                    Text(
                        text = text,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun BiometricClassDisplayScreenPreview() {
    BiometricClassDetectorTheme {
        BiometricClassDisplayScreen(
            deviceInfoState = DeviceInfo(
                deviceName = "Pixel 8 Pro",
                deviceBrand = "Google",
                deviceModel = "husky",
                androidVersion = "14",
                androidApiLevel = 34,
            ),
            biometricPropertiesState = BiometricProperties(
                isDeviceSecure = true,
                availableBiometricTypes = listOf(BiometricType.FINGERPRINT, BiometricType.FACE),
                availableBiometricClasses = listOf(
                    BiometricClassDetails(
                        biometricClass = BiometricClass.CLASS2,
                        enrolled = true,
                    ),
                    BiometricClassDetails(
                        biometricClass = BiometricClass.CLASS3,
                        enrolled = true
                    ),
                ),
            ),
            useCryptoObjectChecked = false,
            onUseCryptoObjectCheckedChange = {},
            onShowBiometricPromptClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceSecurityDisplayPreview() {
    BiometricClassDetectorTheme {
        DeviceSecurityDisplay(
            isDeviceSecure = true,
            biometricTypes = listOf(
                BiometricType.FINGERPRINT,
                BiometricType.FACE
            ),
            biometricClasses = listOf(
                BiometricClassDetails(
                    biometricClass = BiometricClass.CLASS2,
                    enrolled = true,
                ),
                BiometricClassDetails(
                    biometricClass = BiometricClass.CLASS3,
                    enrolled = true,
                ),
            ),
        )
    }
}
