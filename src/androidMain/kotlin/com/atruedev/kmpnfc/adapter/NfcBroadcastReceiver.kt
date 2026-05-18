package com.atruedev.kmpnfc.adapter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import java.util.concurrent.atomic.AtomicReference

public class NfcBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context?,
        intent: Intent?,
    ) {
        if (intent == null) return

        val tag: Tag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            } ?: return

        currentHandler.get()?.invoke(tag)
    }

    public companion object {
        private val currentHandler = AtomicReference<((Tag) -> Unit)?>(null)

        /**
         * Registers [cb] as the current tag handler. Returns a disposer that
         * clears the handler only if it has not been replaced by another caller.
         */
        public fun setCallback(cb: (Tag) -> Unit): () -> Unit {
            currentHandler.set(cb)
            return { currentHandler.compareAndSet(cb, null) }
        }
    }
}
