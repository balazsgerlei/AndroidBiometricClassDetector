package com.example.biometricclassdetector

enum class BiometricType {
    FINGERPRINT, FACE, IRIS, DEVICE_CREDENTIAL
}

enum class BiometricClass {
    CLASS1, CLASS2, CLASS3
}

data class BiometricClassDetails(
    val biometricClass: BiometricClass,
    val enrolled: Boolean,
)

data class BiometricProperties(
    val isDeviceSecure: Boolean,
    val availableBiometricTypes:  List<BiometricType>,
    val availableBiometricClasses: List<BiometricClassDetails>,
)
