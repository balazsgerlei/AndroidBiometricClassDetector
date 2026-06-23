# Android Biometric Class Detector

[![latest release](https://img.shields.io/github/v/release/balazsgerlei/AndroidBiometricClassDetector)](https://github.com/balazsgerlei/AndroidBiometricClassDetector/releases/latest)
[![API](https://img.shields.io/badge/API-23%2B-brightgreen.svg)](https://android-arsenal.com/api?level=23)
[![license](https://img.shields.io/github/license/balazsgerlei/AndroidBiometricClassDetector)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![last commit](https://img.shields.io/github/last-commit/balazsgerlei/AndroidBiometricClassDetector?color=018786)](https://github.com/balazsgerlei/AndroidBiometricClassDetector/commits/main)

Not all biometric sensors are created equal. Manufacturers often omit how secure their sensors are in their specs, and most reviews don't say whether they can be used in, e.g., banking apps either. It is possible that some of them can only unlock the device. This utility app for developers and power users provides insight into the device's biometric capabilities.

It shows you:

- The type of the available sensors (Fingerprint, Face, Iris)
- The enrolled Biometric Classes (Class 2 or Weak, Class 3 or Strong) as defined by Android
- How a BiometricPrompt behaves with the option to trigger one

A higher class sensor also satisfies lower class requirements, so if there are multiple sensors enrolled (e.g., a Fingerprint and Face), there is no way to tell which sensor falls into which class, but if you run this app while only one of the sensors is enrolled, you will know its properties. With this app, you can see all this, making it quite handy for testing new devices.

## Download

[<img height="80" alt="Get it on Google Play"
src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
/>](https://play.google.com/store/apps/details?id=dev.gerlot.biometricclasses)
