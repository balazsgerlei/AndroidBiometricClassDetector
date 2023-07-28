package com.example.biometricclassdetector

import android.app.KeyguardManager
import android.content.Context
import android.content.Context.KEYGUARD_SERVICE
import android.content.pm.PackageManager
import androidx.biometric.BiometricManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainViewModel: ViewModel() {

    private val _biometricProperties = MutableLiveData<BiometricProperties>()
    val biometricProperties: LiveData<BiometricProperties> = _biometricProperties

    fun retrieveBiometricProperties(context: Context) {
        val packageManager = context.packageManager
        val biometricManager = BiometricManager.from(context)
        val keyGuardManager: KeyguardManager = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager

        val availableBiometricTypes = ArrayList<BiometricType>()
        val availableBiometricClasses = ArrayList<BiometricClassDetails>()

        if (packageManager.hasSystemFeature(PackageManager.FEATURE_FACE)) {
            availableBiometricTypes.add(BiometricType.FACE)
        }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_IRIS)) {
            availableBiometricTypes.add(BiometricType.IRIS)
        }
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
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

}
