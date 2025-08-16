package com.example.biometricclassdetector

import android.app.KeyguardManager
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class MainViewModel: ViewModel() {

    sealed class UiEvent {
        data object ShowBiometricPrompt: UiEvent()
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

    fun showBiometricPrompt() = viewModelScope.launch {
        _eventChannel.send(UiEvent.ShowBiometricPrompt)
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

    private fun fingerprintSensorAvailable(packageManager: PackageManager): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)

}
