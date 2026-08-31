package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atruedev.kmpnfc.ndef.NdefRecord

@Composable
internal fun InfoCard(
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
internal fun NavigationCard(
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
internal fun NdefRecordCard(
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
