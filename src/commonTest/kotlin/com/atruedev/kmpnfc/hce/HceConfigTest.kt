package com.atruedev.kmpnfc.hce

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HceConfigTest {
    @Test
    fun aid_registration_valid() {
        val reg = AidRegistration("F0010203040506")
        assertEquals("F0010203040506", reg.aid)
        assertEquals(AidCategory.OTHER, reg.category)
    }

    @Test
    fun aid_registration_hex_validation() {
        assertFailsWith<IllegalArgumentException> {
            AidRegistration("ZZZZ0203040506")
        }
    }

    @Test
    fun aid_registration_length_validation() {
        assertFailsWith<IllegalArgumentException> {
            AidRegistration("F001")
        }
    }
}
