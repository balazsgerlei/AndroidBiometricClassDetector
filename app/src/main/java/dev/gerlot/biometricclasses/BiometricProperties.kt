package dev.gerlot.biometricclasses

enum class BiometricType {
    FINGERPRINT, FACE, IRIS, DEVICE_CREDENTIAL
}

enum class BiometricClass(val displayName: String) {
    CLASS1("Class 1️⃣ (Convenience)"),
    CLASS2("Class 2️⃣ (Weak)"),
    CLASS3("Class 3️⃣ (Strong)")
}

data class BiometricClassDetails(
    val biometricClass: BiometricClass,
    val enrolled: Boolean,
)

data class BiometricProperties(
    val isDeviceSecure: Boolean,
    val supportedBiometricTypes:  List<BiometricType>,
    val availableBiometricClasses: List<BiometricClassDetails>,
)
