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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
                val biometricPropertiesState = viewModel.biometricProperties.observeAsState()

                BiometricClassDisplayScreen(
                    state = biometricPropertiesState.value,
                    onShowBiometricPromptClick = {
                        showBiometricPrompt()
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

    private fun showBiometricPrompt() {
        val biometricPrompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
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
            })
        val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("Biometric login for my app")
            .setSubtitle("Log in using your biometric credential")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Cancel")
            .build()
        biometricPrompt.authenticate(promptInfo)
    }

}

@Composable
fun BiometricClassDisplayScreen(state: BiometricProperties?,
                                onShowBiometricPromptClick: () -> Unit,
                                modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background
    ) {
        state?.let {
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
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp)
                ) {
                    Text("Show Biometric Prompt")
                }
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
fun BiometricClassesDisplayPreview() {
    BiometricClassDetectorTheme {
        BiometricClassesDisplay(
            biometricClasses = listOf(
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
            state = BiometricProperties(
                isDeviceSecure = true,
                availableBiometricTypes = listOf(BiometricType.FINGERPRINT, BiometricType.FACE),
                availableBiometricClasses = listOf(
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
