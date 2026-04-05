package com.atruedev.kmpnfc.adapter

/**
 * The state of the device's NFC hardware.
 */
public enum class NfcAdapterState {
    /** NFC hardware is available and enabled. */
    ON,

    /** NFC hardware is present but disabled in system settings. */
    OFF,

    /** This device has no NFC hardware. */
    NOT_SUPPORTED,

    /** NFC permission has not been granted (iOS-specific). */
    UNAUTHORIZED,
}
