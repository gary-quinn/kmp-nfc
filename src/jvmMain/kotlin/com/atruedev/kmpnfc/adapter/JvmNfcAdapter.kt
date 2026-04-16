package com.atruedev.kmpnfc.adapter

import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.NotSupported
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.reader.ReaderOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow

internal class JvmNfcAdapter : NfcAdapter {
    override val state: StateFlow<NfcAdapterState> =
        MutableStateFlow(NfcAdapterState.NOT_SUPPORTED).asStateFlow()

    override val capabilities: NfcCapabilities = NfcCapabilities.NONE

    override fun tags(options: ReaderOptions): Flow<NfcTag> =
        flow {
            throw NfcException(NotSupported("NFC requires Android or iOS. Use FakeNfcAdapter for JVM tests."))
        }

    override fun close() = Unit
}

public actual fun NfcAdapter(): NfcAdapter = JvmNfcAdapter()
