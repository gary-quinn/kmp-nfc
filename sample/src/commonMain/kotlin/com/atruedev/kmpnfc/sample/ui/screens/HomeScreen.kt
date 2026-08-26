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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atruedev.kmpnfc.adapter.NfcAdapter
import com.atruedev.kmpnfc.adapter.NfcCapabilities
import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.reader.ReaderOptions
import com.atruedev.kmpnfc.sample.ui.components.ErrorCard
import com.atruedev.kmpnfc.sample.util.toHexString
import com.atruedev.kmpnfc.testing.FakeNfcAdapter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    adapter: NfcAdapter,
    fakeAdapter: FakeNfcAdapter?,
    capabilities: NfcCapabilities,
    simulateMode: Boolean,
    onSimulateModeChange: (Boolean) -> Unit,
    onTagSelected: (NfcTag) -> Unit,
    onCapabilities: () -> Unit,
    onHceServer: () -> Unit,
    onSimulateTag: () -> Unit,
) {
    var readerOptions by remember { mutableStateOf(ReaderOptions(alertMessage = "Hold near NFC tag")) }
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<NfcException?>(null) }
    var lastTag by remember { mutableStateOf<NfcTag?>(null) }
    val scope = rememberCoroutineScope()
    var scanJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            scanJob?.cancel()
            isScanning = false
        }
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
            Switch(checked = simulateMode, onCheckedChange = onSimulateModeChange)
        }

        ReaderOptionsPanel(
            options = readerOptions,
            onOptionsChange = { readerOptions = it },
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    if (isScanning) {
                        scanJob?.cancel()
                        scanJob = null
                        isScanning = false
                    } else {
                        scanError = null
                        isScanning = true
                        scanJob =
                            scope.launch {
                                try {
                                    adapter.tags(readerOptions).collect { tag ->
                                        lastTag = tag
                                        if (!readerOptions.isMultiTagSession) {
                                            isScanning = false
                                            scanJob?.cancel()
                                        }
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: NfcException) {
                                    scanError = e
                                    isScanning = false
                                }
                            }
                    }
                },
                enabled = capabilities.canReadNdef || simulateMode,
            ) {
                Text(if (isScanning) "Stop scan" else "Start scan")
            }
            if (isScanning) {
                CircularProgressIndicator()
            }
        }

        scanError?.let { ErrorCard(it) }

        lastTag?.let { tag ->
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
