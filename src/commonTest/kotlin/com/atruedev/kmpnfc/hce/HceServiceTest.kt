package com.atruedev.kmpnfc.hce

import com.atruedev.kmpnfc.tag.ApduCommand
import com.atruedev.kmpnfc.tag.ApduResponse
import com.atruedev.kmpnfc.testing.FakeHceService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HceServiceTest {
    // --- Capabilities ---

    @Test
    fun capabilities_isSupported_true() {
        val hce = FakeHceService()
        assertTrue(hce.capabilities.isSupported)
    }

    @Test
    fun capabilities_HceCapabilities_NONE() {
        val caps = HceCapabilities.NONE
        assertFalse(caps.isSupported)
        assertFalse(caps.canPaymentCategory)
    }

    // --- AID Registration ---

    @Test
    fun start_registersAids() =
        runTest {
            val hce = FakeHceService()
            val job = backgroundStart(hce, AidRegistration("F0010203040506"))
            delay(10)

            assertEquals(1, hce.registeredAids.size)
            assertEquals("F0010203040506", hce.registeredAids[0].aid)

            hce.stop()
            job.join()
        }

    // --- Processor receives commands ---

    @Test
    fun processor_receives_select_aid() =
        runTest {
            val hce = FakeHceService()
            val selectApdu =
                ApduCommand(
                    cla = 0x00.toByte(),
                    ins = 0xA4.toByte(),
                    p1 = 0x04.toByte(),
                    p2 = 0x00.toByte(),
                    data =
                        byteArrayOf(
                            0xF0.toByte(),
                            0x01,
                            0x02,
                            0x03,
                            0x04,
                            0x05,
                            0x06,
                        ),
                )

            var receivedCommand: ApduCommand? = null

            val job =
                launch {
                    hce.start(
                        config =
                            HceConfig(
                                aids = listOf(AidRegistration("F0010203040506")),
                            ),
                    ) { command ->
                        receivedCommand = command
                        ApduResponse.success()
                    }
                }
            delay(10)

            val response = hce.simulateCommand(selectApdu)
            assertEquals(0x90.toByte(), response.sw1)
            assertEquals(0x00.toByte(), response.sw2)

            val cmd = receivedCommand ?: error("Expected command")
            assertEquals(0x00.toByte(), cmd.cla)
            assertEquals(0xA4.toByte(), cmd.ins)

            hce.stop()
            job.join()
        }

    // --- Responses are tracked ---

    @Test
    fun responses_are_recorded() =
        runTest {
            val hce = FakeHceService()
            val job =
                launch {
                    hce.start(
                        config =
                            HceConfig(
                                aids = listOf(AidRegistration("A00000000101")),
                            ),
                    ) {
                        ApduResponse.success(byteArrayOf(0x01, 0x02))
                    }
                }
            delay(10)

            hce.simulateCommand(
                ApduCommand(
                    cla = 0x00.toByte(),
                    ins = 0xB0.toByte(),
                    p1 = 0x00.toByte(),
                    p2 = 0x00.toByte(),
                ),
            )

            assertEquals(1, hce.responses.size)
            assertEquals(byteArrayOf(0x01, 0x02).toList(), hce.responses[0].data.toList())
            assertEquals(0x90.toByte(), hce.responses[0].sw1)

            hce.stop()
            job.join()
        }

    // --- Deactivation ---

    @Test
    fun deactivation_cancels_processor() =
        runTest {
            val hce = FakeHceService()
            var processorCancelled = false

            val job =
                launch {
                    try {
                        hce.start(
                            config =
                                HceConfig(
                                    aids = listOf(AidRegistration("F0010203040506")),
                                ),
                        ) {
                            delay(1000)
                            ApduResponse.success()
                        }
                    } catch (e: DeactivationException) {
                        processorCancelled = true
                        assertEquals(DeactivationReason.LINK_LOSS, e.reason)
                    }
                }
            delay(10)

            hce.simulateDeactivation(DeactivationReason.LINK_LOSS)
            job.join()

            assertTrue(processorCancelled)
            assertFalse(hce.isStarted)
        }

    // --- Stop ---

    @Test
    fun stop_ends_session() =
        runTest {
            val hce = FakeHceService()
            val job =
                launch {
                    hce.start(
                        config =
                            HceConfig(
                                aids = listOf(AidRegistration("F0010203040506")),
                            ),
                    ) {
                        ApduResponse.success()
                    }
                }
            delay(10)
            assertTrue(hce.isStarted)

            hce.stop()
            job.join()
            assertFalse(hce.isStarted)
        }

    // --- Double start ---

    @Test
    fun double_start_throws() =
        runTest {
            val hce = FakeHceService()
            val job =
                launch {
                    hce.start(
                        config =
                            HceConfig(
                                aids = listOf(AidRegistration("F0010203040506")),
                            ),
                    ) {
                        ApduResponse.success()
                    }
                }
            delay(10)

            assertFailsWith<IllegalStateException> {
                hce.start(
                    config =
                        HceConfig(
                            aids = listOf(AidRegistration("A00000000101")),
                        ),
                ) {
                    ApduResponse.success()
                }
            }

            hce.stop()
            job.join()
        }

    // --- Stop when idle is a no-op ---

    @Test
    fun stop_when_idle_is_noop() {
        val hce = FakeHceService()
        hce.stop()
    }

    // --- Multiple AidRegistration ---

    @Test
    fun start_registers_multiple_aids() =
        runTest {
            val hce = FakeHceService()
            val job =
                backgroundStart(
                    hce,
                    AidRegistration("F0010203040506"),
                    AidRegistration("A00000000101", AidCategory.PAYMENT),
                )
            delay(10)

            assertEquals(2, hce.registeredAids.size)
            assertEquals("F0010203040506", hce.registeredAids[0].aid)
            assertEquals(AidCategory.OTHER, hce.registeredAids[0].category)
            assertEquals("A00000000101", hce.registeredAids[1].aid)
            assertEquals(AidCategory.PAYMENT, hce.registeredAids[1].category)

            hce.stop()
            job.join()
        }

    // --- ApduCommand.fromBytes parses correctly ---

    @Test
    fun apdu_command_fromBytes_select() {
        val bytes =
            byteArrayOf(
                0x00,
                0xA4.toByte(),
                0x04,
                0x00,
                0x07,
                0xF0.toByte(),
                0x01,
                0x02,
                0x03,
                0x04,
                0x05,
                0x06,
            )
        val cmd = ApduCommand.fromBytes(bytes)

        assertEquals(0x00.toByte(), cmd.cla)
        assertEquals(0xA4.toByte(), cmd.ins)
        assertEquals(0x04.toByte(), cmd.p1)
        assertEquals(0x00.toByte(), cmd.p2)
        assertEquals(7, cmd.data!!.size)
        assertEquals(0xF0.toByte(), cmd.data!![0])
    }

    // --- ApduResponse.toBytes ---

    @Test
    fun apdu_response_toBytes() {
        val response = ApduResponse.success(byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F))
        val bytes = response.toBytes()

        assertEquals(7, bytes.size)
        assertEquals(0x48.toByte(), bytes[0])
        assertEquals(0x90.toByte(), bytes[5])
        assertEquals(0x00.toByte(), bytes[6])
    }

    // --- AidRegistration validation ---

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

    // --- DeactivationException ---

    @Test
    fun deactivation_exception_carries_reason() {
        val ex = DeactivationException(DeactivationReason.LINK_LOSS)
        assertEquals(DeactivationReason.LINK_LOSS, ex.reason)
        assertTrue(ex.message!!.contains("LINK_LOSS"))
    }

    // --- Helpers ---

    private fun TestScope.backgroundStart(
        hce: FakeHceService,
        vararg aids: AidRegistration,
    ): Job =
        launch {
            hce.start(
                config =
                    HceConfig(
                        aids = aids.toList(),
                    ),
            ) {
                ApduResponse.success()
            }
        }
}
