package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.sample.ui.components.ErrorCard
import com.atruedev.kmpnfc.sample.ui.components.SampleTopBar
import com.atruedev.kmpnfc.sample.viewmodel.rememberTagViewModel

@Composable
fun NdefReaderScreen(
    tag: NfcTag,
    onBack: () -> Unit,
) {
    val vm = rememberTagViewModel(tag)
    val ndef by vm.ndefMessage.collectAsState()
    val error by vm.error.collectAsState()
    val loading by vm.isLoading.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SampleTopBar(title = "NDEF reader", onBack = onBack)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = vm::readNdef, enabled = !loading, modifier = Modifier.fillMaxWidth()) {
                Text("Read NDEF")
            }
            if (loading) CircularProgressIndicator()
            error?.let { ErrorCard(it) }
            ndef?.records?.forEachIndexed { index, record ->
                NdefRecordCard(index, record)
            } ?: Text("No NDEF message loaded yet.")
        }
    }
}
