package com.atruedev.kmpnfc.reader

import com.atruedev.kmpnfc.tag.TagType

/**
 * Configuration for an NFC tag reader session.
 *
 * @property pollingTypes Tag types to poll for. Empty falls back to [TagType.NFC_A].
 * @property isMultiTagSession Keep the session open after the first tag is read.
 * @property alertMessage Message shown in the iOS system NFC sheet. iOS only.
 * @property androidScanMode Selects the Android scanning API. Android only.
 */
public data class ReaderOptions(
    val pollingTypes: Set<TagType> = TagType.pollable,
    val isMultiTagSession: Boolean = false,
    val alertMessage: String? = null,
    val androidScanMode: AndroidScanMode = AndroidScanMode.ReaderMode,
)

/**
 * Android scanning API selection.
 *
 * - [ReaderMode] uses `NfcAdapter.enableReaderMode`. The system suppresses
 *   default NDEF intent resolution while the session is active, so URI tags
 *   do not launch the browser. Recommended for most cases.
 * - [ForegroundDispatch] uses `NfcAdapter.enableForegroundDispatch`. The app
 *   receives tags via a per-session broadcast receiver and takes priority over
 *   other apps' intent filters while in the foreground.
 */
public enum class AndroidScanMode {
    ReaderMode,
    ForegroundDispatch,
}
