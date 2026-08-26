package com.atruedev.kmpnfc.hce

import android.nfc.cardemulation.HostApduService

/**
 * Process-scoped registry bridging Android's [HostApduService] (created by the
 * system) and the library's [AndroidHceService] (created by the consumer).
 *
 * [session] is written once per active HCE session from [AndroidHceService.start].
 * [hostService] is owned by the system [HostApduService] lifecycle.
 */
internal object HceServiceRegistry {
    @Volatile
    private var session: HceSession? = null

    @Volatile
    private var hostService: HostApduService? = null

    fun register(service: HceSession) {
        check(session == null) { "HCE service already active" }
        session = service
    }

    fun unregister(service: HceSession) {
        if (session === service) {
            session = null
        }
    }

    fun get(): HceSession? = session

    fun setHostService(service: HostApduService) {
        hostService = service
    }

    fun clearHostService(service: HostApduService) {
        if (hostService === service) hostService = null
    }

    fun getHostService(): HostApduService? = hostService
}
