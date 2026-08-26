package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atruedev.kmpnfc.hce.HceService
import com.atruedev.kmpnfc.sample.ui.components.SampleTopBar
import com.atruedev.kmpnfc.sample.viewmodel.HceViewModel

@Composable
fun HceServerScreen(
    hce: HceService,
    onBack: () -> Unit,
) {
    val vm: HceViewModel = viewModel { HceViewModel(hce) }
    val isRunning by vm.isRunning.collectAsState()
    val logs by vm.logs.collectAsState()
    val error by vm.error.collectAsState()
    var aid by remember { mutableStateOf("F0010203040506") }

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "HCE server", onBack = onBack)
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!vm.capabilities.isSupported) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Host Card Emulation is not supported on this platform.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            "Use an Android device for the two-phone HCE demo. iOS requires the EEA-only HCE entitlement.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                return@Column
            }

            OutlinedTextField(
                value = aid,
                onValueChange = { aid = it },
                label = { Text("AID (hex)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.start(aid) }, enabled = !isRunning) { Text("Start HCE") }
                OutlinedButton(onClick = vm::stop, enabled = isRunning) { Text("Stop") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Text(
                "Tap another phone running the reader with an ISO-DEP session. SELECT uses INS=A4, custom read uses INS=CA.",
                style = MaterialTheme.typography.bodySmall,
            )
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                items(logs.size) { index ->
                    val entry = logs[index]
                    Text("${entry.direction}: ${entry.summary}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
