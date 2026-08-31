package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atruedev.kmpnfc.reader.AndroidScanMode
import com.atruedev.kmpnfc.reader.ReaderOptions

@Composable
fun ReaderOptionsPanel(
    options: ReaderOptions,
    onOptionsChange: (ReaderOptions) -> Unit,
    modifier: Modifier = Modifier,
) {
    var alertMessage by remember(options.alertMessage) { mutableStateOf(options.alertMessage ?: "") }
    var multiTag by remember(options.isMultiTagSession) { mutableStateOf(options.isMultiTagSession) }
    var scanMode by remember(options.androidScanMode) { mutableStateOf(options.androidScanMode) }

    Column(modifier = modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Reader options", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(
            value = alertMessage,
            onValueChange = {
                alertMessage = it
                onOptionsChange(options.copy(alertMessage = it.ifBlank { null }))
            },
            label = { Text("iOS alert message") },
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Multi-tag session")
            Switch(
                checked = multiTag,
                onCheckedChange = {
                    multiTag = it
                    onOptionsChange(options.copy(isMultiTagSession = it))
                },
            )
        }
        Text("Android scan mode", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = scanMode == AndroidScanMode.ReaderMode,
                onClick = {
                    scanMode = AndroidScanMode.ReaderMode
                    onOptionsChange(options.copy(androidScanMode = AndroidScanMode.ReaderMode))
                },
                label = { Text("Reader mode") },
            )
            FilterChip(
                selected = scanMode == AndroidScanMode.ForegroundDispatch,
                onClick = {
                    scanMode = AndroidScanMode.ForegroundDispatch
                    onOptionsChange(options.copy(androidScanMode = AndroidScanMode.ForegroundDispatch))
                },
                label = { Text("Foreground dispatch") },
            )
        }
    }
}
