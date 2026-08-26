package com.atruedev.kmpnfc.sample.platform

import androidx.compose.runtime.Composable

/** Returns a callback that opens the platform NFC or app settings screen. */
@Composable
expect fun rememberOpenNfcSettingsAction(): () -> Unit
