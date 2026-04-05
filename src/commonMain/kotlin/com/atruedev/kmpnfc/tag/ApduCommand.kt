package com.atruedev.kmpnfc.tag

/**
 * An ISO 7816-4 APDU command received from an external NFC reader.
 *
 * @property cla Class byte.
 * @property ins Instruction byte.
 * @property p1 Parameter 1.
 * @property p2 Parameter 2.
 * @property data Optional command data field.
 * @property le Expected response length (0 means 256 bytes expected).
 */
public data class ApduCommand(
    val cla: Byte,
    val ins: Byte,
    val p1: Byte,
    val p2: Byte,
    val data: ByteArray? = null,
    val le: Int? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApduCommand) return false
        return cla == other.cla &&
            ins == other.ins &&
            p1 == other.p1 &&
            p2 == other.p2 &&
            (data contentEquals other.data) &&
            le == other.le
    }

    override fun hashCode(): Int {
        var result = cla.hashCode()
        result = 31 * result + ins.hashCode()
        result = 31 * result + p1.hashCode()
        result = 31 * result + p2.hashCode()
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (le ?: 0)
        return result
    }

    public companion object {
        /** Parse raw APDU bytes into a structured command. */
        public fun fromBytes(bytes: ByteArray): ApduCommand {
            require(bytes.size >= 4) { "APDU command must be at least 4 bytes (CLA INS P1 P2)" }
            val cla = bytes[0]
            val ins = bytes[1]
            val p1 = bytes[2]
            val p2 = bytes[3]

            if (bytes.size == 4) {
                return ApduCommand(cla, ins, p1, p2)
            }

            val lc = bytes[4].toInt() and 0xFF
            return if (lc > 0 && bytes.size >= 5 + lc) {
                val data = bytes.copyOfRange(5, 5 + lc)
                val le = if (bytes.size > 5 + lc) bytes[5 + lc].toInt() and 0xFF else null
                ApduCommand(cla, ins, p1, p2, data, le)
            } else {
                val le = bytes[4].toInt() and 0xFF
                ApduCommand(cla, ins, p1, p2, le = le)
            }
        }
    }
}
