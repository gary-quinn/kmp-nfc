package com.atruedev.kmpnfc.sample.platform

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberOpenNfcSettingsAction(): () -> Unit {
    val context = LocalContext.current
    return remember(context) {
        {
            val nfcIntent =
                Intent(Settings.ACTION_NFC_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            runCatching { context.startActivity(nfcIntent) }
                .onFailure {
                    val fallback =
                        Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    context.startActivity(fallback)
                }
        }
    }
}
