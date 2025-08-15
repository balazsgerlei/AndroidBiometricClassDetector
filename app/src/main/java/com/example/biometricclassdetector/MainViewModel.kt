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

class MainViewModel: ViewModel() {

    private val _deviceInfo = MutableLiveData<DeviceInfo>()
    val deviceInfo: LiveData<DeviceInfo> = _deviceInfo

    private val _biometricProperties = MutableLiveData<BiometricProperties>()
    val biometricProperties: LiveData<BiometricProperties> = _biometricProperties

    fun retrieveDeviceInfo() {
        _deviceInfo.value = DeviceInfo(
            deviceName = Build.MODEL,
            deviceBrand = Build.MANUFACTURER,
            deviceModel = Build.DEVICE,
            androidVersion = Build.VERSION.RELEASE,
            androidApiLevel = Build.VERSION.SDK_INT,
        )
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
