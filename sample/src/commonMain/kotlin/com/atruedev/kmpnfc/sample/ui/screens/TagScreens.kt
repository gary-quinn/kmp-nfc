package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.atruedev.kmpnfc.ndef.NdefRecord
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.sample.ui.components.ErrorCard
import com.atruedev.kmpnfc.sample.ui.components.SampleTopBar
import com.atruedev.kmpnfc.sample.util.toHexString
import com.atruedev.kmpnfc.sample.viewmodel.TagViewModel

@Composable
fun TagDetailScreen(
    tag: NfcTag,
    onBack: () -> Unit,
    onNdefReader: () -> Unit,
    onNdefWriter: () -> Unit,
    onApduConsole: () -> Unit,
) {
    val vm: TagViewModel = viewModel(key = tag.identifier.contentHashCode().toString()) { TagViewModel(tag) }

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "Tag detail", onBack = onBack)
        Column(
            modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InfoCard("UID", vm.identifier.toHexString())
            InfoCard("Type", vm.type.name)
            InfoCard("Technologies", vm.technologies.joinToString { it.name })
            NavigationCard("NDEF reader", "Read records from this tag", onNdefReader)
            NavigationCard("NDEF writer", "Write URI or text records", onNdefWriter)
            NavigationCard("APDU console", "Send raw ISO 7816-4 commands", onApduConsole)
        }
    }
}

@Composable
fun NdefReaderScreen(
    tag: NfcTag,
    onBack: () -> Unit,
) {
    val vm: TagViewModel = viewModel(key = tag.identifier.contentHashCode().toString()) { TagViewModel(tag) }
    val ndef by vm.ndefMessage.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "NDEF reader", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = vm::readNdef, enabled = !loading) { Text("Read NDEF") }
            if (loading) CircularProgressIndicator()
            error?.let { ErrorCard(it) }
            ndef?.records?.forEachIndexed { index, record ->
                NdefRecordCard(index, record)
            } ?: Text("No NDEF message loaded yet.")
        }
    }
}

@Composable
fun NdefWriterScreen(
    tag: NfcTag,
    onBack: () -> Unit,
) {
    val vm: TagViewModel = viewModel(key = tag.identifier.contentHashCode().toString()) { TagViewModel(tag) }
    val error by vm.error.collectAsState()
    val loading by vm.isLoading.collectAsState()
    var uri by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("https://github.com/gary-quinn/kmp-nfc")
    }
    var text by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
            "Hello from kmp-nfc sample",
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "NDEF writer", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.OutlinedTextField(
                value = uri,
                onValueChange = { uri = it },
                label = { Text("URI") },
                modifier = Modifier.fillMaxWidth(),
            )
            androidx.compose.material3.OutlinedTextField(
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

@Composable
fun ApduConsoleScreen(
    tag: NfcTag,
    onBack: () -> Unit,
) {
    val vm: TagViewModel = viewModel(key = tag.identifier.contentHashCode().toString()) { TagViewModel(tag) }
    val response by vm.lastApduResponse.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.isLoading.collectAsState()
    var command by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf("00 A4 04 00 07 A0 00 00 00 04 10 10")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "APDU console", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            androidx.compose.material3.OutlinedTextField(
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

@Composable
private fun InfoCard(
    title: String,
    value: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavigationCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NdefRecordCard(
    index: Int,
    record: NdefRecord,
) {
    val summary =
        when (record) {
            is NdefRecord.Uri -> "URI: ${record.uri}"
            is NdefRecord.Text -> "Text (${record.locale}): ${record.text}"
            is NdefRecord.MimeMedia -> "MIME ${record.mimeType}: ${record.data.size} bytes"
            is NdefRecord.ExternalType -> "External ${record.domain}:${record.externalType}"
            is NdefRecord.Unknown -> "Unknown TNF ${record.tnf}"
        }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Record $index", style = MaterialTheme.typography.labelLarge)
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
