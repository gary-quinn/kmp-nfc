package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import com.atruedev.kmpnfc.sample.viewmodel.rememberTagViewModel

@Composable
fun NdefWriterScreen(
    tag: NfcTag,
    onBack: () -> Unit,
) {
    val vm = rememberTagViewModel(tag)
    val error by vm.error.collectAsState()
    val loading by vm.isLoading.collectAsState()
    var uri by remember { mutableStateOf("https://github.com/gary-quinn/kmp-nfc") }
    var text by remember { mutableStateOf("Hello from kmp-nfc sample") }

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "NDEF writer", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = uri,
                onValueChange = { uri = it },
                label = { Text("URI") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Text") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { vm.writeNdef(uri, text) },
                enabled = !loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Write NDEF")
            }
            if (loading) CircularProgressIndicator()
            error?.let { ErrorCard(it) }
        }
    }
}
