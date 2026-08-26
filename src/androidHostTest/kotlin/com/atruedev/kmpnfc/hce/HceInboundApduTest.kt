package com.atruedev.kmpnfc.hce

import com.atruedev.kmpnfc.tag.ApduCommand
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HceInboundApduTest {
    @Test
    fun returns_6f00_when_session_absent() {
        val response = processInboundApdu(byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00), null)
        assertContentEquals(byteArrayOf(0x6F.toByte(), 0x00), response)
    }

    @Test
    fun returns_6f00_when_apdu_malformed() {
        val response = processInboundApdu(byteArrayOf(0x01), FakeSession())
        assertContentEquals(byteArrayOf(0x6F.toByte(), 0x00), response)
    }

    @Test
    fun dispatches_valid_apdu_and_returns_null() {
        var dispatched = false
        val session =
            object : HceSession {
                override fun dispatch(command: ApduCommand) {
                    dispatched = true
                }

                override fun onDeactivated(reason: DeactivationReason) = Unit
            }

        val result =
            processInboundApdu(
                byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00),
                session,
            )

        assertNull(result)
        assertTrue(dispatched)
    }

    private class FakeSession : HceSession {
        override fun dispatch(command: ApduCommand) = Unit

        override fun onDeactivated(reason: DeactivationReason) = Unit
    }
}
