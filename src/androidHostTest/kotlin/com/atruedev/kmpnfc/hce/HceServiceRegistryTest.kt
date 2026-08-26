package com.atruedev.kmpnfc.hce

import com.atruedev.kmpnfc.tag.ApduCommand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class HceServiceRegistryTest {
    @Test
    fun register_and_get_round_trip() {
        val session = FakeSession()
        HceServiceRegistry.register(session)
        assertSame(session, HceServiceRegistry.get())
        HceServiceRegistry.unregister(session)
        assertNull(HceServiceRegistry.get())
    }

    @Test
    fun double_register_throws() {
        val session = FakeSession()
        HceServiceRegistry.register(session)
        try {
            val error =
                runCatching { HceServiceRegistry.register(FakeSession()) }.exceptionOrNull()
            assertEquals("HCE service already active", error?.message)
        } finally {
            HceServiceRegistry.unregister(session)
        }
    }

    @Test
    fun unregister_leaves_host_service_alone() {
        val session = FakeSession()
        HceServiceRegistry.register(session)
        HceServiceRegistry.unregister(session)
        assertNull(HceServiceRegistry.get())
    }

    private class FakeSession : HceSession {
        override fun dispatch(command: ApduCommand) = Unit

        override fun onDeactivated(reason: DeactivationReason) = Unit
    }
}
