package com.atruedev.kmpnfc.adapter

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build

public class NfcBroadcastReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        val tag: Tag? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }

        if (tag == null) return

        handleTag(tag)
    }

    public companion object {
        private var handleTag: (Tag) -> Unit = { println("no callback set") }

        public fun setCallback(cb: (Tag) -> Unit) {
            handleTag = cb
        }
    }
}
