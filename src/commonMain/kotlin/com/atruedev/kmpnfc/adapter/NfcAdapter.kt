package com.atruedev.kmpnfc.adapter

import com.atruedev.kmpnfc.reader.NfcTag
import com.atruedev.kmpnfc.reader.ReaderOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Entry point for NFC operations.
 *
 * Provides access to NFC adapter state, platform capabilities, and tag discovery.
 * Obtain an instance via the [NfcAdapter] factory function.
 *
 * The [tags] flow is cold - collecting starts a reader session, cancelling
 * collection ends it. On iOS, this displays the system NFC reading sheet.
 *
 * ```
 * val adapter = NfcAdapter()
 * adapter.tags().collect { tag ->
 *     val ndef = tag.readNdef()
 *     tag.close()
 * }
 * ```
 */
public interface NfcAdapter : AutoCloseable {
    /** NFC hardware state. Emits updates when the adapter is enabled/disabled. */
    public val state: StateFlow<NfcAdapterState>

    /** Platform NFC capabilities. Query before using features. */
    public val capabilities: NfcCapabilities

    /**
     * Discover NFC tags. Cold Flow - collecting starts the reader session.
     *
     * On iOS, this displays the system NFC sheet with [ReaderOptions.alertMessage].
     * The session auto-closes when the flow is cancelled.
     *
     * Each emitted [NfcTag] must be [closed][NfcTag.close] after use.
     */
    public fun tags(options: ReaderOptions = ReaderOptions()): Flow<NfcTag>

    /** Close adapter and release platform resources. */
    override fun close()
}

/** Create a platform-specific [NfcAdapter] instance. */
public expect fun NfcAdapter(): NfcAdapter
