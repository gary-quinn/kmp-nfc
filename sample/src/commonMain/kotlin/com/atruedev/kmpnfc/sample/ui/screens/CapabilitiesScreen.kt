package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atruedev.kmpnfc.adapter.NfcCapabilities
import com.atruedev.kmpnfc.sample.ui.components.SampleTopBar

@Composable
fun CapabilitiesScreen(
    capabilities: NfcCapabilities,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "Capabilities", onBack = onBack)
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CapabilityRow("NDEF read", capabilities.canReadNdef)
            CapabilityRow("NDEF write", capabilities.canWriteNdef)
            CapabilityRow("Raw transceive", capabilities.canReadRawTag)
            CapabilityRow("Background read", capabilities.canBackgroundRead)
            CapabilityRow("Host card emulation", capabilities.canHostCardEmulation)
            Text("Supported tag types", style = MaterialTheme.typography.titleSmall)
            capabilities.supportedTagTypes.forEach { type ->
                Text(type.name, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun CapabilityRow(
    label: String,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(
            if (enabled) "Yes" else "No",
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
    }
}
