package com.atruedev.kmpnfc.hce

/**
 * An ISO 7816-4 APDU response sent back to an external NFC reader.
 *
 * @property data Response data field.
 * @property sw1 Status word 1.
 * @property sw2 Status word 2.
 */
public data class ApduResponse(
    val data: ByteArray,
    val sw1: Byte,
    val sw2: Byte,
) {
    /** Serialize to raw bytes: data + SW1 + SW2. */
    public fun toBytes(): ByteArray = data + byteArrayOf(sw1, sw2)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApduResponse) return false
        return data.contentEquals(other.data) && sw1 == other.sw1 && sw2 == other.sw2
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + sw1.hashCode()
        result = 31 * result + sw2.hashCode()
        return result
    }

    public companion object {
        /** Success (SW 9000). */
        public fun success(data: ByteArray = byteArrayOf()): ApduResponse = ApduResponse(data, 0x90.toByte(), 0x00)

        /** File or application not found (SW 6A82). */
        public fun fileNotFound(): ApduResponse = ApduResponse(byteArrayOf(), 0x6A.toByte(), 0x82.toByte())

        /** Wrong parameters / incorrect data (SW 6A80). */
        public fun wrongParameters(): ApduResponse = ApduResponse(byteArrayOf(), 0x6A.toByte(), 0x80.toByte())

        /** Command not allowed — conditions of use not satisfied (SW 6985). */
        public fun conditionsNotSatisfied(): ApduResponse = ApduResponse(byteArrayOf(), 0x69.toByte(), 0x85.toByte())

        /** Instruction not supported (SW 6D00). */
        public fun instructionNotSupported(): ApduResponse = ApduResponse(byteArrayOf(), 0x6D.toByte(), 0x00)

        /** Class not supported (SW 6E00). */
        public fun classNotSupported(): ApduResponse = ApduResponse(byteArrayOf(), 0x6E.toByte(), 0x00)
    }
}
