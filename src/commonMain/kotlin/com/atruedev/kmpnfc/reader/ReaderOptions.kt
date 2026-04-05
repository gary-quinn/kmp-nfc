package com.atruedev.kmpnfc.reader

import com.atruedev.kmpnfc.tag.TagType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for an NFC tag reader session.
 *
 * @property pollingTypes Tag types to poll for. Defaults to all types.
 * @property alertMessage Message shown in the iOS system NFC sheet. Ignored on Android.
 * @property isMultiTagSession Whether to keep the session open after reading the first tag.
 * @property timeout Maximum duration for the reader session before auto-closing.
 */
public data class ReaderOptions(
    val pollingTypes: Set<TagType> = TagType.all,
    val alertMessage: String? = null,
    val isMultiTagSession: Boolean = false,
    val timeout: Duration = 60.seconds,
)
