package com.atruedev.kmpnfc.sample.platform

import android.content.Intent
import android.provider.Settings

actual fun openNfcSettings() {
    val context = settingsContext ?: return
    val intent =
        Intent(Settings.ACTION_NFC_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    runCatching { context.startActivity(intent) }
        .onFailure {
            val fallback =
                Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(fallback)
        }
}

internal var settingsContext: android.content.Context? = null

fun bindSettingsContext(context: android.content.Context) {
    settingsContext = context.applicationContext
}
