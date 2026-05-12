package com.atruedev.kmpnfc.reader

import com.atruedev.kmpnfc.tag.TagType

/**
 * Configuration for an NFC tag reader session.
 *
 * @property pollingTypes Tag types to poll for. Defaults to [TagType.pollable].
 * @property alertMessage Message shown in the iOS system NFC sheet. Ignored on Android.
 * @property isMultiTagSession Whether to keep the session open after reading the first tag.
 * @property enableForegroundDispatch Whether to enable foreground dispatch of NFC tag read events,
 *           instead of using the adapter's "reader mode". Ignored on iOS.
 */
public data class ReaderOptions(
    val pollingTypes: Set<TagType> = TagType.pollable,
    val alertMessage: String? = null,
    val isMultiTagSession: Boolean = false,
    val enableForegroundDispatch: Boolean = false,
)
