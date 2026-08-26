package com.atruedev.kmpnfc.hce

/**
 * Why the HCE session ended.
 *
 * Thrown as [DeactivationException] from [HceService.start] when the external
 * reader disconnects. [HceService.stop] ends the session without throwing.
 */
public enum class DeactivationReason {
    /** RF field lost - the reader moved out of range. */
    LINK_LOSS,

    /** The reader sent a DESELECT command for the current AID. */
    DESELECTED,
}

/**
 * Thrown when [HceService.start] is interrupted by reader deactivation.
 *
 * This is normal HCE lifecycle, not a programming error.
 * Catch to clean up session state.
 */
public class DeactivationException(
    public val reason: DeactivationReason,
) : Exception("HCE deactivated: $reason")
