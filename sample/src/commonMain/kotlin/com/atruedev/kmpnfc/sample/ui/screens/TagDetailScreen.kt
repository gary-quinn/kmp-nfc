package com.atruedev.kmpnfc.sample.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.sample.ui.components.SampleTopBar
import com.atruedev.kmpnfc.sample.util.toHexString
import com.atruedev.kmpnfc.sample.viewmodel.rememberTagViewModel

@Composable
fun TagDetailScreen(
    tag: NfcTag,
    onBack: () -> Unit,
    onNdefReader: () -> Unit,
    onNdefWriter: () -> Unit,
    onApduConsole: () -> Unit,
) {
    val vm = rememberTagViewModel(tag)

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
