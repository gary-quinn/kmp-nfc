package com.atruedev.kmpnfc.sample.model

import com.atruedev.kmpnfc.reader.NfcTag

sealed interface Screen {
    data object Home : Screen

    data object Capabilities : Screen

    data object HceServer : Screen

    data object SimulateTag : Screen

    data class TagDetail(
        val tag: NfcTag,
    ) : Screen

    data class NdefReader(
        val tag: NfcTag,
    ) : Screen

    data class NdefWriter(
        val tag: NfcTag,
    ) : Screen

    data class ApduConsole(
        val tag: NfcTag,
    ) : Screen
}
