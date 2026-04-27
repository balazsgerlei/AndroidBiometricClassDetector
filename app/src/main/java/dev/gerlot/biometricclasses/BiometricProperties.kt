package dev.gerlot.biometricclasses

enum class BiometricType {
    FINGERPRINT, FACE, IRIS, DEVICE_CREDENTIAL
}

enum class BiometricClass {
    CLASS1, CLASS2, CLASS3
}

data class BiometricClassDetails(
    val biometricClass: dev.gerlot.biometricclasses.BiometricClass,
    val enrolled: Boolean,
)

data class BiometricProperties(
    val isDeviceSecure: Boolean,
    val availableBiometricTypes:  List<dev.gerlot.biometricclasses.BiometricType>,
    val availableBiometricClasses: List<dev.gerlot.biometricclasses.BiometricClassDetails>,
)
