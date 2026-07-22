package com.atruedev.kmpnfc.hce

/**
 * Why the HCE session ended.
 *
 * Thrown as [DeactivationException] from [HceService.start] when the external
 * reader disconnects or the consumer calls [HceService.stop].
 */
public enum class DeactivationReason {
    /** RF field lost -- the reader moved out of range. */
    LINK_LOSS,

    /** The reader sent a DESELECT command for the current AID. */
    DESELECTED,

    /** The consumer called [HceService.stop]. */
    STOPPED,
}

/**
 * Thrown when [HceService.start] is interrupted by deactivation.
 *
 * This is normal HCE lifecycle, not a programming error.
 * Catch to clean up session state.
 */
public class DeactivationException(
    public val reason: DeactivationReason,
) : Exception("HCE deactivated: $reason")
