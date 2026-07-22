package com.atruedev.kmpnfc.hce

import android.nfc.cardemulation.HostApduService

/**
 * Process-scoped registry bridging Android's [HostApduService] (created by the
 * system) and the library's [AndroidHceService] (created by the consumer).
 *
 * Single-writer, sequential-reader pattern -- no locks needed:
 * - [register] is called once from [AndroidHceService.start]
 * - [unregister] is called once from [AndroidHceService.cleanup]
 * - [get] is called from [KmpNfcHostApduService] on the UI thread, sequentially
 */
internal object HceServiceRegistry {
    @Volatile
    private var hceService: AndroidHceService? = null

    @Volatile
    private var hostService: HostApduService? = null

    fun register(service: AndroidHceService) {
        check(hceService == null) { "HCE service already active" }
        hceService = service
    }

    fun unregister(service: AndroidHceService) {
        if (hceService === service) {
            hceService = null
            hostService = null
        }
    }

    fun get(): AndroidHceService? = hceService

    fun setHostService(service: HostApduService) {
        hostService = service
    }

    fun clearHostService(service: HostApduService) {
        if (hostService === service) hostService = null
    }

    fun getHostService(): HostApduService? = hostService
}
