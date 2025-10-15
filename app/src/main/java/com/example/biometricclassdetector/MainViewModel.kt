package com.example.biometricclassdetector

import android.app.KeyguardManager
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.NoSuchAlgorithmException
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException

private const val ANDROID_KEYSTORE = "AndroidKeyStore"
private const val SAMPLE_AES_KEY_ALIAS = "sample_aes_key"

class MainViewModel: ViewModel() {

    sealed class UiEvent {
        data class ShowBiometricPrompt(
            val useDeviceCredential: Boolean,
        ): UiEvent()
        data class ShowSecureBiometricPrompt(
            val useDeviceCredential: Boolean,
            val cryptoObject: BiometricPrompt.CryptoObject
        ): UiEvent()
        data object FailedToShowBiometricPrompt: UiEvent()
        data class AuthenticationError(
            val errorCode: Int,
            val errorString: CharSequence
        ): UiEvent()
        data object AuthenticationSucceeded: UiEvent()
        data object AuthenticationFailed: UiEvent()
    }

    private val _eventChannel = Channel<UiEvent>(Channel.BUFFERED)
    val eventChannel = _eventChannel.receiveAsFlow()

    private val _deviceInfo = MutableStateFlow(
        DeviceInfo(
            deviceName = Build.MODEL ?: "Unknown",
            deviceBrand = Build.MANUFACTURER ?: "Unknown",
            deviceModel = Build.DEVICE ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            androidApiLevel = Build.VERSION.SDK_INT,
        )
    )
    val deviceInfo = _deviceInfo.asStateFlow()

    private val _biometricProperties = MutableStateFlow<BiometricProperties?>(null)
    val biometricProperties = _biometricProperties.asStateFlow()

    private var cryptoObject: BiometricPrompt.CryptoObject? = null

    private var keystore: KeyStore? = null

    init {
        keystore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
            load(null)
        }
    }

    fun showBiometricPrompt(useDeviceCredential: Boolean) = viewModelScope.launch {
        cryptoObject = null
        _eventChannel.send(UiEvent.ShowBiometricPrompt(useDeviceCredential))
    }

    fun showSecureBiometricPrompt(useDeviceCredential: Boolean) {
        val cipher = createCipher()
        if (cipher != null && initCipher(cipher)) {
            BiometricPrompt.CryptoObject(cipher).let {
                cryptoObject = it
                viewModelScope.launch {
                    _eventChannel.send(
                        UiEvent.ShowSecureBiometricPrompt(useDeviceCredential, cryptoObject = it)
                    )
                }
            }
        } else {
            viewModelScope.launch {
                _eventChannel.send(UiEvent.FailedToShowBiometricPrompt)
            }
        }
    }

    fun onAuthenticationError(errorCode: Int, errorString: CharSequence) = viewModelScope.launch {
        _eventChannel.send(UiEvent.AuthenticationError(errorCode, errorString))
    }

    fun onAuthenticationSucceeded(cryptoObjectFromResult: BiometricPrompt.CryptoObject? = null) {
        if (cryptoObject != null) {
            if (cryptoObjectFromResult?.cipher == cryptoObject?.cipher) {
                cryptoObject = null
                viewModelScope.launch {
                    _eventChannel.send(UiEvent.AuthenticationSucceeded)
                }
            } else {
                viewModelScope.launch {
                    _eventChannel.send(UiEvent.AuthenticationFailed)
                }
            }
        } else {
            viewModelScope.launch {
                _eventChannel.send(UiEvent.AuthenticationSucceeded)
            }
        }
    }

    fun onAuthenticationFailed() = viewModelScope.launch {
        _eventChannel.send(UiEvent.AuthenticationFailed)
    }

    fun retrieveBiometricProperties(context: Context) {
        val packageManager = context.packageManager
        val biometricManager = BiometricManager.from(context)
        val keyGuardManager: KeyguardManager = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        val availableBiometricTypes = ArrayList<BiometricType>()
        val availableBiometricClasses = ArrayList<BiometricClassDetails>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && faceSensorAvailable(packageManager)) {
            availableBiometricTypes.add(BiometricType.FACE)
        }
        if (irisSensorAvailable(packageManager)) {
            availableBiometricTypes.add(BiometricType.IRIS)
        }
        if (fingerprintSensorAvailable(packageManager)) {
            availableBiometricTypes.add(BiometricType.FINGERPRINT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            availableBiometricTypes.add(BiometricType.DEVICE_CREDENTIAL)
        }

        when (val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                availableBiometricClasses.add(
                    BiometricClassDetails(
                        biometricClass = BiometricClass.CLASS2,
                        enrolled = result == BiometricManager.BIOMETRIC_SUCCESS,
                    )
                )
            }
            else -> { /* handle biometric auth not possible */ }
        }
        when (val result = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS, BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                availableBiometricClasses.add(
                    BiometricClassDetails(
                        biometricClass = BiometricClass.CLASS3,
                        enrolled = result == BiometricManager.BIOMETRIC_SUCCESS,
                    )
                )
            }
            else -> { /* handle biometric auth not possible */ }
        }

        _biometricProperties.value = BiometricProperties(keyGuardManager.isDeviceSecure, availableBiometricTypes, availableBiometricClasses)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun faceSensorAvailable(packageManager: PackageManager): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)

    private fun irisSensorAvailable(packageManager: PackageManager): Boolean {
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)) {
            return true
        } else {
            try {
                val samsungIrisInfo: PackageInfo? = packageManager.getPackageInfo("com.samsung.android.server.iris", PackageManager.GET_META_DATA)
                return samsungIrisInfo != null
            } catch (_: Exception) {
                return false
            }
        }
    }

    private fun fingerprintSensorAvailable(packageManager: PackageManager): Boolean =
        packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)

    private fun createKeyGenParameterSpec() = KeyGenParameterSpec.Builder(
        SAMPLE_AES_KEY_ALIAS,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        .setUserAuthenticationRequired(false)
        .build()

    private fun generateKey() =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            val keyGenParameterSpec = createKeyGenParameterSpec()
            init(keyGenParameterSpec)
            generateKey()
        }

    private fun getSecretKey() = keystore?.getKey(SAMPLE_AES_KEY_ALIAS, null)

    private fun createCipher(): Cipher? {
        try {
            return Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7)
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to create Cipher", ex)
        } catch (ex: NoSuchPaddingException) {
            throw RuntimeException("Failed to create Cipher", ex)
        }
    }

    private fun initCipher(cipher: Cipher): Boolean {
        try {
            val secretKey = getSecretKey() ?: generateKey()
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            return true
        } catch (_: KeyPermanentlyInvalidatedException) {
            return false
        } catch (ex: KeyStoreException) {
            throw RuntimeException("Failed to create Cipher", ex)
        } catch (ex: UnrecoverableKeyException) {
            throw RuntimeException("Failed to create Cipher", ex)
        } catch (ex: NoSuchAlgorithmException) {
            throw RuntimeException("Failed to create Cipher", ex)
        } catch (ex: Exception) {
            throw RuntimeException("Failed to create Cipher", ex)
        }
    }

}
