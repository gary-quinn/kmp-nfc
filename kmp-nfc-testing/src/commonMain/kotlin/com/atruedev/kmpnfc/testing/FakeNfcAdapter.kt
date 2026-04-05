package com.atruedev.kmpnfc.testing

import com.atruedev.kmpnfc.adapter.NfcAdapter
import com.atruedev.kmpnfc.adapter.NfcAdapterState
import com.atruedev.kmpnfc.adapter.NfcCapabilities
import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.reader.ReaderOptions
import com.atruedev.kmpnfc.tag.TagType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Test double for [NfcAdapter] that allows controlling state, capabilities,
 * and tag emission for unit testing.
 *
 * ```
 * val adapter = FakeNfcAdapter()
 * adapter.state.value // NfcAdapterState.ON
 *
 * adapter.simulateDisabled()
 * adapter.state.value // NfcAdapterState.OFF
 *
 * // Emit a fake tag
 * adapter.emitTag(fakeTag)
 * ```
 */
public class FakeNfcAdapter(
    initialState: NfcAdapterState = NfcAdapterState.ON,
    override val capabilities: NfcCapabilities = DEFAULT_CAPABILITIES,
) : NfcAdapter {
    private val _state = MutableStateFlow(initialState)
    override val state: StateFlow<NfcAdapterState> = _state.asStateFlow()

    private val tagFlow = MutableSharedFlow<NfcTag>(extraBufferCapacity = 16)

    override fun tags(options: ReaderOptions): Flow<NfcTag> = tagFlow

    /** Emit a tag to all active collectors of [tags]. */
    public suspend fun emitTag(tag: NfcTag) {
        tagFlow.emit(tag)
    }

    /** Transition adapter to [NfcAdapterState.OFF]. */
    public fun simulateDisabled() {
        _state.value = NfcAdapterState.OFF
    }

    /** Transition adapter to [NfcAdapterState.ON]. */
    public fun simulateEnabled() {
        _state.value = NfcAdapterState.ON
    }

    /** Transition adapter to [NfcAdapterState.NOT_SUPPORTED]. */
    public fun simulateNotSupported() {
        _state.value = NfcAdapterState.NOT_SUPPORTED
    }

    /** Transition adapter to [NfcAdapterState.UNAUTHORIZED]. */
    public fun simulateUnauthorized() {
        _state.value = NfcAdapterState.UNAUTHORIZED
    }

    override fun close(): Unit = Unit

    public companion object {
        public val DEFAULT_CAPABILITIES: NfcCapabilities =
            NfcCapabilities(
                canReadNdef = true,
                canWriteNdef = true,
                canReadRawTag = true,
                canBackgroundRead = false,
                supportedTagTypes =
                    setOf(
                        TagType.NFC_A,
                        TagType.NFC_B,
                        TagType.NFC_F,
                        TagType.NFC_V,
                        TagType.ISO_DEP,
                    ),
            )
    }
}
