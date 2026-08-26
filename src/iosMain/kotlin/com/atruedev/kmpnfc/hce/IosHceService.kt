package com.atruedev.kmpnfc.hce

import com.atruedev.kmpnfc.error.NfcException
import com.atruedev.kmpnfc.error.NotSupported
import com.atruedev.kmpnfc.tag.ApduCommand
import com.atruedev.kmpnfc.tag.ApduResponse

internal class IosHceService : HceService {
    override val capabilities: HceCapabilities = HceCapabilities.NONE

    override suspend fun start(
        config: HceConfig,
        processor: suspend (ApduCommand) -> ApduResponse,
    ): Nothing =
        throw NfcException(
            NotSupported(
                "HCE requires iOS 17.4+ with com.apple.developer.nfc.hce " +
                    "entitlement (EEA organization accounts only).",
            ),
        )

    override fun stop() = Unit
}

public actual fun HceService(): HceService = IosHceService()
