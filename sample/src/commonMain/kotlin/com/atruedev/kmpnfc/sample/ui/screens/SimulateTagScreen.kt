package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atruedev.kmpnfc.ndef.ndefMessage
import com.atruedev.kmpnfc.sample.scan.ScanSession
import com.atruedev.kmpnfc.sample.ui.components.SampleTopBar
import com.atruedev.kmpnfc.tag.TagType
import com.atruedev.kmpnfc.testing.FakeNfcAdapter
import com.atruedev.kmpnfc.testing.fakeNfcTag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SimulateTagScreen(
    fakeAdapter: FakeNfcAdapter?,
    scanSession: ScanSession,
    scope: CoroutineScope,
    onBack: () -> Unit,
    onTagEmitted: () -> Unit,
) {
    val scanState by scanSession.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "Simulate tag", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (fakeAdapter == null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Simulation requires simulate mode. Enable it on the home screen.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
                return@Column
            }

            if (!scanState.isScanning) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Start a scan on Home before emitting a simulated tag.",
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            Text(
                "Emits a fake tag into the active reader session, then returns to Home.",
                style = MaterialTheme.typography.bodySmall,
            )
            Button(
                onClick = {
                    scope.launch {
                        val tag =
                            fakeNfcTag {
                                identifier(byteArrayOf(0x04, 0x12, 0x34, 0x56, 0x78.toByte()))
                                type(TagType.ISO_DEP)
                                ndef(
                                    ndefMessage {
                                        uri("https://github.com/gary-quinn/kmp-nfc")
                                        text("Simulated tag", locale = "en")
                                    },
                                )
                                onTransceive { byteArrayOf(0x90.toByte(), 0x00) }
                            }
                        fakeAdapter.emitTag(tag)
                        onTagEmitted()
                    }
                },
                enabled = scanState.isScanning,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Emit simulated tag")
            }
        }
    }
}
