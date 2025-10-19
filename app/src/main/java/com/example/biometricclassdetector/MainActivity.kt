package com.example.biometricclassdetector

import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.biometricclassdetector.ui.theme.BiometricClassDetectorTheme

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            BiometricClassDetectorTheme {
                val deviceInfoState by viewModel.deviceInfo.collectAsState()
                val biometricPropertiesState by viewModel.biometricProperties.collectAsState()

                LaunchedEffect(Unit) {
                    viewModel.eventChannel.collect { event ->
                        when(event) {
                            is MainViewModel.UiEvent.ShowBiometricPrompt -> {
                                showBiometricPrompt(event.useDeviceCredential)
                            }
                            is MainViewModel.UiEvent.ShowSecureBiometricPrompt -> {
                                showSecureBiometricPrompt(event.useDeviceCredential, event.cryptoObject)
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
                var authenticateWithDeviceCredentialChecked by remember { mutableStateOf(false) }

                BiometricClassDisplayScreen(
                    deviceInfoState = deviceInfoState,
                    biometricPropertiesState = biometricPropertiesState,
                    useCryptoObjectChecked = useCryptoObjectChecked,
                    onUseCryptoObjectCheckedChange = {
                        useCryptoObjectChecked = it
                    },
                    authenticateWithDeviceCredentialChecked = authenticateWithDeviceCredentialChecked,
                    onAuthenticateWithDeviceCredentialCheckedChange = {
                        authenticateWithDeviceCredentialChecked = it
                    },
                    onShowBiometricPromptClick = {
                        if (useCryptoObjectChecked) {
                            viewModel.showSecureBiometricPrompt(
                                useDeviceCredential = authenticateWithDeviceCredentialChecked
                            )
                        } else {
                            viewModel.showBiometricPrompt(
                                useDeviceCredential = authenticateWithDeviceCredentialChecked
                            )
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
        .apply {
            if ((allowedAuthenticators and BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.Authenticators.DEVICE_CREDENTIAL) {
                setNegativeButtonText("Cancel")
            }
        }
        .build()

    private fun showBiometricPrompt(useDeviceCredential: Boolean) {
        val biometricPrompt = createBiometricPrompt()
        val allowedAuthenticators = if (useDeviceCredential) {
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        val promptInfo =
            createPromptInfo(allowedAuthenticators)
        biometricPrompt.authenticate(promptInfo)
    }

    private fun showSecureBiometricPrompt(useDeviceCredential: Boolean, cryptoObject: BiometricPrompt.CryptoObject) {
        val biometricPrompt = createBiometricPrompt()
        val allowedAuthenticators = if (useDeviceCredential) {
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
        val promptInfo = createPromptInfo(allowedAuthenticators)
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
    authenticateWithDeviceCredentialChecked: Boolean,
    onAuthenticateWithDeviceCredentialCheckedChange: ((Boolean) -> Unit)?,
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
            authenticateWithDeviceCredentialChecked = authenticateWithDeviceCredentialChecked,
            onAuthenticateWithDeviceCredentialCheckedChange = onAuthenticateWithDeviceCredentialCheckedChange,
            modifier = Modifier.padding(innerPadding),
        )
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
        SecureDeviceLockDisplay(
            isDeviceSecure = isDeviceSecure,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        AvailableBiometricTypesDisplay(
            biometricTypes = biometricTypes,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        AvailableBiometricClassesDisplay(
            biometricClasses = biometricClasses
        )
    }
}


@Composable
fun BiometricClassDisplay(
    state: BiometricProperties?,
    useCryptoObjectChecked: Boolean,
    onUseCryptoObjectCheckedChange: ((Boolean) -> Unit)?,
    authenticateWithDeviceCredentialChecked: Boolean,
    onAuthenticateWithDeviceCredentialCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (state != null) {
        val configuration = LocalConfiguration.current
        when (configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> {
                Column(
                    modifier = modifier
                        .padding(start = 16.dp, end = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    DeviceSecurityDisplay(
                        isDeviceSecure = state.isDeviceSecure,
                        biometricTypes = state.availableBiometricTypes,
                        biometricClasses = state.availableBiometricClasses,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    BiometricPromptOptions(
                        useCryptoObjectChecked = useCryptoObjectChecked,
                        onUseCryptoObjectCheckedChange = onUseCryptoObjectCheckedChange,
                        biometricTypes = state.availableBiometricTypes,
                        authenticateWithDeviceCredentialChecked = authenticateWithDeviceCredentialChecked,
                        onAuthenticateWithDeviceCredentialCheckedChange = onAuthenticateWithDeviceCredentialCheckedChange,
                    )
                }
            }
            else -> { // Configuration.ORIENTATION_LANDSCAPE
                Row(
                    modifier = modifier.padding(start = 16.dp, end = 16.dp)
                ) {
                    DeviceSecurityDisplay(
                        isDeviceSecure = state.isDeviceSecure,
                        biometricTypes = state.availableBiometricTypes,
                        biometricClasses = state.availableBiometricClasses,
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BiometricPromptOptions(
                        useCryptoObjectChecked = useCryptoObjectChecked,
                        onUseCryptoObjectCheckedChange = onUseCryptoObjectCheckedChange,
                        biometricTypes = state.availableBiometricTypes,
                        authenticateWithDeviceCredentialChecked = authenticateWithDeviceCredentialChecked,
                        onAuthenticateWithDeviceCredentialCheckedChange = onAuthenticateWithDeviceCredentialCheckedChange,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SecureDeviceLockDisplay(
    isDeviceSecure: Boolean,
    modifier: Modifier = Modifier,
) {
    Row (
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
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
}

@Composable
fun AvailableBiometricTypesDisplay(
    biometricTypes: List<BiometricType>,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "Available biometric types: ${biometricTypes.joinToString(separator = ", ")}",
        modifier = modifier,
    )
}

@Composable
fun AvailableBiometricClassesDisplay(
    biometricClasses: List<BiometricClassDetails>,
    modifier: Modifier = Modifier,
) {
    Column (
        modifier = modifier,
    ) {
        Text(
            text = "Available biometric classes:",
            modifier = Modifier.padding(bottom = 8.dp),
        )
        biometricClasses.forEach {
            val icon = if (it.enrolled) {
                Icons.Default.CheckCircle
            } else Icons.Default.Cancel
            val iconTint = if (it.enrolled) {
                Color(0xFF4CAF50)
            } else Color(0xFFF44336)
            val text = if (it.enrolled) {
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

@Composable
fun BiometricPromptOptions(
    useCryptoObjectChecked: Boolean,
    onUseCryptoObjectCheckedChange: ((Boolean) -> Unit)?,
    biometricTypes: List<BiometricType>,
    authenticateWithDeviceCredentialChecked: Boolean,
    onAuthenticateWithDeviceCredentialCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
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
        if (biometricTypes.contains(BiometricType.DEVICE_CREDENTIAL)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Switch(
                    checked = authenticateWithDeviceCredentialChecked,
                    onCheckedChange = onAuthenticateWithDeviceCredentialCheckedChange,
                )
                Text(
                    text = "Authenticate with Device Credential",
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
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
                availableBiometricTypes = listOf(
                    BiometricType.FINGERPRINT,
                    BiometricType.FACE,
                    BiometricType.DEVICE_CREDENTIAL,
                    ),
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
            authenticateWithDeviceCredentialChecked = false,
            onAuthenticateWithDeviceCredentialCheckedChange = {},
            onShowBiometricPromptClick = {},
        )
    }
}

@Preview(
    showBackground = true,
    device = "spec:width=411dp,height=891dp,dpi=420,isRound=false,chinSize=0dp,orientation=landscape,cutout=none,navigation=gesture"
)
@Composable
fun BiometricClassDisplayScreenLandscapePreview() {
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
                availableBiometricTypes = listOf(
                    BiometricType.FINGERPRINT,
                    BiometricType.FACE,
                    BiometricType.DEVICE_CREDENTIAL,
                ),
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
            authenticateWithDeviceCredentialChecked = false,
            onAuthenticateWithDeviceCredentialCheckedChange = {},
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
                BiometricType.FACE,
                BiometricType.DEVICE_CREDENTIAL,
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

@Preview(showBackground = true)
@Composable
fun BiometricPromptOptionsPreview() {
    BiometricClassDetectorTheme {
        BiometricPromptOptions(
            useCryptoObjectChecked = false,
            onUseCryptoObjectCheckedChange = {},
            biometricTypes = listOf(
                BiometricType.FINGERPRINT,
                BiometricType.FACE,
                BiometricType.DEVICE_CREDENTIAL,
            ),
            authenticateWithDeviceCredentialChecked = false,
            onAuthenticateWithDeviceCredentialCheckedChange = {},
        )
    }
}
