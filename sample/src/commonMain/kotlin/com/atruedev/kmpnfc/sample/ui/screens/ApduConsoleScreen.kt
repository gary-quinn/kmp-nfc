package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
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
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.sample.ui.components.ErrorCard
import com.atruedev.kmpnfc.sample.ui.components.SampleTopBar
import com.atruedev.kmpnfc.sample.util.toHexString
import com.atruedev.kmpnfc.sample.viewmodel.rememberTagViewModel

@Composable
fun ApduConsoleScreen(
    tag: NfcTag,
    onBack: () -> Unit,
) {
    val vm = rememberTagViewModel(tag)
    val response by vm.lastApduResponse.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.isLoading.collectAsState()
    var command by remember { mutableStateOf("00 A4 04 00 07 A0 00 00 00 04 10 10") }

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "APDU console", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("Command (hex)") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(onClick = { vm.transceive(command) }, enabled = !loading) {
                Text("Transceive")
            }
            if (loading) CircularProgressIndicator()
            error?.let { ErrorCard(it) }
            response?.let { InfoCard("Response", it.toHexString()) }
        }
    }
}
