package com.atruedev.kmpnfc

import app.cash.turbine.test
import com.atruedev.kmpnfc.adapter.NfcAdapterState
import com.atruedev.kmpnfc.adapter.NfcCapabilities
import com.atruedev.kmpnfc.tag.TagType
import com.atruedev.kmpnfc.testing.FakeNfcAdapter
import com.atruedev.kmpnfc.testing.FakeNfcTag
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NfcAdapterTest {
    @Test
    fun initialStateIsOn() {
        val adapter = FakeNfcAdapter()
        assertEquals(NfcAdapterState.ON, adapter.state.value)
    }

    @Test
    fun simulateDisabledTransitionsState() =
        runTest {
            val adapter = FakeNfcAdapter()
            adapter.state.test {
                assertEquals(NfcAdapterState.ON, awaitItem())
                adapter.simulateDisabled()
                assertEquals(NfcAdapterState.OFF, awaitItem())
            }
        }

    @Test
    fun simulateEnabledTransitionsState() =
        runTest {
            val adapter = FakeNfcAdapter(initialState = NfcAdapterState.OFF)
            adapter.state.test {
                assertEquals(NfcAdapterState.OFF, awaitItem())
                adapter.simulateEnabled()
                assertEquals(NfcAdapterState.ON, awaitItem())
            }
        }

    @Test
    fun simulateNotSupportedTransitionsState() =
        runTest {
            val adapter = FakeNfcAdapter()
            adapter.state.test {
                assertEquals(NfcAdapterState.ON, awaitItem())
                adapter.simulateNotSupported()
                assertEquals(NfcAdapterState.NOT_SUPPORTED, awaitItem())
            }
        }

    @Test
    fun simulateUnauthorizedTransitionsState() =
        runTest {
            val adapter = FakeNfcAdapter()
            adapter.state.test {
                assertEquals(NfcAdapterState.ON, awaitItem())
                adapter.simulateUnauthorized()
                assertEquals(NfcAdapterState.UNAUTHORIZED, awaitItem())
            }
        }

    @Test
    fun defaultCapabilitiesIncludeCommonFeatures() {
        val adapter = FakeNfcAdapter()
        assertTrue(adapter.capabilities.canReadNdef)
        assertTrue(adapter.capabilities.canWriteNdef)
        assertTrue(adapter.capabilities.canReadRawTag)
        assertFalse(adapter.capabilities.canBackgroundRead)
        assertTrue(TagType.NFC_A in adapter.capabilities.supportedTagTypes)
        assertTrue(TagType.ISO_DEP in adapter.capabilities.supportedTagTypes)
    }

    @Test
    fun noneCapabilitiesAreAllFalse() {
        val none = NfcCapabilities.NONE
        assertFalse(none.canReadNdef)
        assertFalse(none.canWriteNdef)
        assertFalse(none.canReadRawTag)
        assertFalse(none.canBackgroundRead)
        assertTrue(none.supportedTagTypes.isEmpty())
    }

    @Test
    fun customCapabilities() {
        val caps =
            NfcCapabilities(
                canReadNdef = true,
                canWriteNdef = false,
                canReadRawTag = false,
                canBackgroundRead = true,
                supportedTagTypes = setOf(TagType.NFC_A),
            )
        val adapter = FakeNfcAdapter(capabilities = caps)
        assertTrue(adapter.capabilities.canReadNdef)
        assertFalse(adapter.capabilities.canWriteNdef)
        assertTrue(adapter.capabilities.canBackgroundRead)
        assertEquals(setOf(TagType.NFC_A), adapter.capabilities.supportedTagTypes)
    }

    @Test
    fun tagsFlowEmitsDiscoveredTags() =
        runTest {
            val adapter = FakeNfcAdapter()
            val fakeTag = FakeNfcTag()

            adapter.tags().test {
                adapter.emitTag(fakeTag)
                val tag = awaitItem()
                assertEquals(fakeTag, tag)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun closeIsIdempotent() {
        val adapter = FakeNfcAdapter()
        adapter.close()
        adapter.close()
    }
}
