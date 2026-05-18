package com.atruedev.kmpnfc

import com.atruedev.kmpnfc.reader.AndroidScanMode
import com.atruedev.kmpnfc.reader.ReaderOptions
import com.atruedev.kmpnfc.tag.TagType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ReaderOptionsTest {
    @Test
    fun defaultsAreSafeForBothPlatforms() {
        val options = ReaderOptions()
        assertEquals(TagType.pollable, options.pollingTypes)
        assertFalse(options.isMultiTagSession)
        assertNull(options.alertMessage)
        assertEquals(AndroidScanMode.ReaderMode, options.androidScanMode)
    }

    @Test
    fun alertMessageIsPreservedAtTopLevel() {
        val options = ReaderOptions(alertMessage = "Hold near tag")
        assertEquals("Hold near tag", options.alertMessage)
        assertEquals(AndroidScanMode.ReaderMode, options.androidScanMode)
    }

    @Test
    fun androidScanModeIsConfigurable() {
        val options = ReaderOptions(androidScanMode = AndroidScanMode.ForegroundDispatch)
        assertEquals(AndroidScanMode.ForegroundDispatch, options.androidScanMode)
        assertNull(options.alertMessage)
    }

    @Test
    fun copyPreservesUnchangedFields() {
        val original =
            ReaderOptions(
                alertMessage = "Hold",
                androidScanMode = AndroidScanMode.ForegroundDispatch,
            )
        val copy = original.copy(isMultiTagSession = true)
        assertEquals(original.alertMessage, copy.alertMessage)
        assertEquals(original.androidScanMode, copy.androidScanMode)
    }
}
