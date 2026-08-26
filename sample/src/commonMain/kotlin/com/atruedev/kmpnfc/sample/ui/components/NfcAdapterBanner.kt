package com.atruedev.kmpnfc.sample.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.atruedev.kmpnfc.adapter.NfcAdapter
import com.atruedev.kmpnfc.adapter.NfcAdapterState
import com.atruedev.kmpnfc.sample.platform.rememberOpenNfcSettingsAction

@Composable
fun NfcAdapterBanner(adapter: NfcAdapter) {
    val state by adapter.state.collectAsState()
    val openSettings = rememberOpenNfcSettingsAction()

    val message =
        when (state) {
            NfcAdapterState.ON -> null
            NfcAdapterState.OFF -> "NFC is off - tap to open settings"
            NfcAdapterState.NOT_SUPPORTED -> "NFC not supported on this device"
            NfcAdapterState.UNAUTHORIZED -> "NFC unauthorized - tap to open settings"
        }

    val color =
        when (state) {
            NfcAdapterState.ON -> Color.Transparent
            NfcAdapterState.UNAUTHORIZED -> Color(0xFFFF9800)
            else -> MaterialTheme.colorScheme.error
        }

    AnimatedVisibility(visible = message != null) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(color)
                    .clickable(enabled = state == NfcAdapterState.OFF || state == NfcAdapterState.UNAUTHORIZED) {
                        openSettings()
                    }.padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = message ?: "",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
