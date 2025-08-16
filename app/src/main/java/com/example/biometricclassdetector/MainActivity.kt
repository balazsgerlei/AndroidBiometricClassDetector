package com.example.biometricclassdetector

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
                        if (event is MainViewModel.UiEvent.ShowBiometricPrompt) {
                            showBiometricPrompt()
                        }
                    }
                }

                BiometricClassDisplayScreen(
                    deviceInfoState = deviceInfoState,
                    biometricPropertiesState = biometricPropertiesState,
                    onShowBiometricPromptClick = {
                        viewModel.showBiometricPrompt()
                    },
                    modifier = Modifier.fillMaxSize()
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
                Toast.makeText(this@MainActivity,
                    "Authentication error: $errString", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Toast.makeText(this@MainActivity,
                    "Authentication succeeded!", Toast.LENGTH_SHORT)
                    .show()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Toast.makeText(this@MainActivity, "Authentication failed",
                    Toast.LENGTH_SHORT)
                    .show()
            }
        }
        return BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            authenticationCallback
        )
    }

    private fun createPromptInfo() = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Cancel")
            .build()

    private fun showBiometricPrompt() {
        val biometricPrompt = createBiometricPrompt()
        val promptInfo = createPromptInfo()
        biometricPrompt.authenticate(promptInfo)
    }

}

@Composable
fun BiometricClassDisplayScreen(
    deviceInfoState: DeviceInfo,
    biometricPropertiesState: BiometricProperties?,
    onShowBiometricPromptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = modifier,
        ) {
            DeviceInfoDisplay(
                state = deviceInfoState,
            )
            BiometricClassDisplay(
                state = biometricPropertiesState,
                onShowBiometricPromptClick = onShowBiometricPromptClick,
                modifier = modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun DeviceInfoDisplay(
    state: DeviceInfo,
    modifier: Modifier = Modifier
) {
    Column (
        modifier = modifier
            .padding(start = 8.dp, top = 16.dp, end = 8.dp, bottom = 8.dp)
            .fillMaxWidth(),
    ) {
        Text(
            text = "${state.deviceBrand} ${state.deviceName} (${state.deviceModel})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .fillMaxWidth(),
        )
        Text(
            text = "Android ${state.androidVersion} (API ${state.androidApiLevel})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .fillMaxWidth(),
        )
    }
}

@Composable
fun BiometricClassDisplay(
    state: BiometricProperties?,
    onShowBiometricPromptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state != null) {
        Column(
            modifier = modifier.padding(16.dp)
        ) {
            DeviceSecureDisplay(
                isDeviceSecure = state.isDeviceSecure,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            BiometricTypesDisplay(
                biometricTypes = state.availableBiometricTypes.joinToString(separator = ", "),
                modifier = Modifier.padding(bottom = 16.dp),
            )
            BiometricClassesDisplay(
                biometricClasses = state.availableBiometricClasses,
            )
            Button(
                onClick = onShowBiometricPromptClick,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 24.dp)
            ) {
                Text("Show Biometric Prompt")
            }
        }
    }
}

@Composable
fun DeviceSecureDisplay(isDeviceSecure: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = "Device is protected with a PIN, pattern or password: $isDeviceSecure",
        modifier = modifier
    )
}

@Composable
fun BiometricTypesDisplay(biometricTypes: String, modifier: Modifier = Modifier) {
    Text(
        text = "Available biometric types: $biometricTypes",
        modifier = modifier
    )
}

@Composable
fun BiometricClassesDisplay(biometricClasses: List<BiometricClassDetails>, modifier: Modifier = Modifier) {
    Column() {
        Text(
            text = "Available biometric classes:",
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Column() {
            biometricClasses.forEach {
                Text(
                    text = "${it.biometricClass}, enrolled: ${it.enrolled}",
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun DeviceInfoDisplayPreview() {
    BiometricClassDetectorTheme {
        DeviceInfoDisplay(
            state = DeviceInfo(
                deviceName = "Pixel 8 Pro",
                deviceBrand = "Google",
                deviceModel = "husky",
                androidVersion = "14",
                androidApiLevel = 34,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BiometricClassesDisplayPreview() {
    BiometricClassDetectorTheme {
        BiometricClassesDisplay(
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
            onShowBiometricPromptClick = {},
            modifier = Modifier.fillMaxSize())
    }
}
