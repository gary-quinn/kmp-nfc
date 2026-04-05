package com.atruedev.kmpnfc.tag

/**
 * Low-level tag technology capabilities discovered on a tag.
 *
 * A single physical tag may support multiple technologies — e.g., an NFC Forum
 * Type 4 tag supports both [NFC_A] and [ISO_DEP].
 */
public enum class TagTechnology {
    NFC_A,
    NFC_B,
    NFC_F,
    NFC_V,
    ISO_DEP,
    NDEF,
    NDEF_FORMATABLE,
    MIFARE_CLASSIC,
    MIFARE_ULTRALIGHT,
}
