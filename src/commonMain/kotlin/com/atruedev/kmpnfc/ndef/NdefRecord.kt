package com.atruedev.kmpnfc.ndef

/**
 * A single record within an [NdefMessage].
 *
 * Sealed hierarchy enables exhaustive matching on known record types.
 * [Unknown] serves as the catch-all for non-standard or unparseable records.
 */
public sealed interface NdefRecord {
    public val tnf: TypeNameFormat
    public val type: ByteArray
    public val payload: ByteArray

    /** Well-known URI record (TNF_WELL_KNOWN + RTD_URI). */
    public data class Uri(
        val uri: String,
    ) : NdefRecord {
        override val tnf: TypeNameFormat get() = TypeNameFormat.WELL_KNOWN
        override val type: ByteArray get() = byteArrayOf(0x55) // 'U'
        override val payload: ByteArray get() = encodeUriPayload(uri)
    }

    /** Well-known Text record (TNF_WELL_KNOWN + RTD_TEXT). */
    public data class Text(
        val text: String,
        val locale: String = "en",
        val encoding: NdefTextEncoding = NdefTextEncoding.UTF_8,
    ) : NdefRecord {
        override val tnf: TypeNameFormat get() = TypeNameFormat.WELL_KNOWN
        override val type: ByteArray get() = byteArrayOf(0x54) // 'T'
        override val payload: ByteArray get() = encodeTextPayload(text, locale, encoding)
    }

    /** MIME media record (TNF_MIME_MEDIA). */
    public data class MimeMedia(
        val mimeType: String,
        val data: ByteArray,
    ) : NdefRecord {
        override val tnf: TypeNameFormat get() = TypeNameFormat.MIME_MEDIA
        override val type: ByteArray get() = mimeType.encodeToByteArray()
        override val payload: ByteArray get() = data

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MimeMedia) return false
            return mimeType == other.mimeType && data.contentEquals(other.data)
        }

        override fun hashCode(): Int = 31 * mimeType.hashCode() + data.contentHashCode()
    }

    /** NFC Forum external type record (TNF_EXTERNAL_TYPE). */
    public data class ExternalType(
        val domain: String,
        val externalType: String,
        val data: ByteArray,
    ) : NdefRecord {
        override val tnf: TypeNameFormat get() = TypeNameFormat.EXTERNAL_TYPE
        override val type: ByteArray get() = "$domain:$externalType".encodeToByteArray()
        override val payload: ByteArray get() = data

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ExternalType) return false
            return domain == other.domain && externalType == other.externalType && data.contentEquals(other.data)
        }

        override fun hashCode(): Int {
            var result = domain.hashCode()
            result = 31 * result + externalType.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }

    /** Catch-all for records that don't match a known type. */
    public data class Unknown(
        override val tnf: TypeNameFormat,
        override val type: ByteArray,
        override val payload: ByteArray,
    ) : NdefRecord {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Unknown) return false
            return tnf == other.tnf && type.contentEquals(other.type) && payload.contentEquals(other.payload)
        }

        override fun hashCode(): Int {
            var result = tnf.hashCode()
            result = 31 * result + type.contentHashCode()
            result = 31 * result + payload.contentHashCode()
            return result
        }
    }
}

/** Text encoding for NDEF Text records. */
public enum class NdefTextEncoding {
    UTF_8,
    UTF_16,
}
