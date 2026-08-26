package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atruedev.kmpnfc.adapter.NfcAdapter
import com.atruedev.kmpnfc.adapter.NfcCapabilities
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.reader.ReaderOptions
import com.atruedev.kmpnfc.sample.scan.ScanSession
import com.atruedev.kmpnfc.sample.ui.components.ErrorCard
import com.atruedev.kmpnfc.sample.util.toHexString

@Composable
fun HomeScreen(
    adapter: NfcAdapter,
    capabilities: NfcCapabilities,
    simulateMode: Boolean,
    scanSession: ScanSession,
    readerOptions: ReaderOptions,
    onReaderOptionsChange: (ReaderOptions) -> Unit,
    onSimulateModeChange: (Boolean) -> Unit,
    onTagSelected: (NfcTag) -> Unit,
    onCapabilities: () -> Unit,
    onHceServer: () -> Unit,
    onSimulateTag: () -> Unit,
) {
    val scanState by scanSession.state.collectAsState()

    LaunchedEffect(simulateMode) {
        scanSession.stop(closeTag = false)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("kmp-nfc sample", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Demonstrates tag reading, NDEF, APDU transceive, and Android HCE.",
            style = MaterialTheme.typography.bodySmall,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Simulate mode")
            Switch(
                checked = simulateMode,
                onCheckedChange = { enabled ->
                    scanSession.stop(closeTag = false)
                    onSimulateModeChange(enabled)
                },
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            ReaderOptionsPanel(
                options = readerOptions,
                onOptionsChange = onReaderOptionsChange,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (scanState.isScanning) {
                        scanSession.stop()
                    } else {
                        scanSession.start(adapter, readerOptions)
                    }
                },
                enabled = capabilities.canReadNdef || simulateMode,
            ) {
                Text(if (scanState.isScanning) "Stop scan" else "Start scan")
            }
            if (scanState.isScanning) {
                CircularProgressIndicator()
            }
        }

        scanState.scanError?.let { ErrorCard(it) }

        scanState.lastTag?.let { tag ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Last tag", style = MaterialTheme.typography.titleSmall)
                    Text("UID: ${tag.identifier.toHexString()}")
                    Text("Type: ${tag.type.name}")
                    Button(onClick = { onTagSelected(tag) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Open tag detail")
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        OutlinedButton(onClick = onCapabilities, modifier = Modifier.fillMaxWidth()) { Text("Capabilities") }
        OutlinedButton(onClick = onHceServer, modifier = Modifier.fillMaxWidth()) { Text("HCE server (Android)") }
        if (simulateMode) {
            OutlinedButton(onClick = onSimulateTag, modifier = Modifier.fillMaxWidth()) { Text("Simulate tag") }
        }
    }
}
