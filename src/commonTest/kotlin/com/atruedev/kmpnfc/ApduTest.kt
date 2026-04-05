package com.atruedev.kmpnfc

import com.atruedev.kmpnfc.tag.ApduCommand
import com.atruedev.kmpnfc.tag.ApduResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApduTest {
    @Test
    fun parseMinimalApduCommand() {
        val bytes = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00)
        val cmd = ApduCommand.fromBytes(bytes)
        assertEquals(0x00, cmd.cla)
        assertEquals(0xA4.toByte(), cmd.ins)
        assertEquals(0x04, cmd.p1)
        assertEquals(0x00, cmd.p2)
        assertNull(cmd.data)
    }

    @Test
    fun parseApduCommandWithData() {
        val aid = byteArrayOf(0xA0.toByte(), 0x00, 0x00, 0x00, 0x04, 0x10, 0x10)
        val bytes = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x07) + aid
        val cmd = ApduCommand.fromBytes(bytes)
        assertEquals(0x00, cmd.cla)
        assertEquals(0xA4.toByte(), cmd.ins)
        assertTrue(aid.contentEquals(cmd.data!!))
    }

    @Test
    fun successResponseHasCorrectStatusWords() {
        val response = ApduResponse.success(byteArrayOf(0x01, 0x02))
        assertEquals(0x90.toByte(), response.sw1)
        assertEquals(0x00, response.sw2)
        assertTrue(byteArrayOf(0x01, 0x02).contentEquals(response.data))
    }

    @Test
    fun fileNotFoundResponseHasCorrectStatusWords() {
        val response = ApduResponse.fileNotFound()
        assertEquals(0x6A.toByte(), response.sw1)
        assertEquals(0x82.toByte(), response.sw2)
    }

    @Test
    fun toBytesSerializesCorrectly() {
        val response = ApduResponse.success(byteArrayOf(0x42))
        val bytes = response.toBytes()
        assertEquals(3, bytes.size)
        assertEquals(0x42, bytes[0])
        assertEquals(0x90.toByte(), bytes[1])
        assertEquals(0x00, bytes[2])
    }
}
