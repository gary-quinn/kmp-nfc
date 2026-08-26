package com.atruedev.kmpnfc.hce

/**
 * Process-scoped registry bridging the system [HostApduService] and the
 * consumer's [AndroidHceService].
 *
 * [session] is written once per active HCE session from [AndroidHceService.start].
 * [hostBridge] is bound for the [KmpNfcHostApduService] lifetime.
 */
internal object HceServiceRegistry {
    @Volatile
    private var session: HceSession? = null

    @Volatile
    private var hostBridge: HostApduBridge? = null

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

    fun bindHost(bridge: HostApduBridge) {
        hostBridge = bridge
    }

    fun unbindHost(bridge: HostApduBridge) {
        if (hostBridge === bridge) hostBridge = null
    }

    fun getHostBridge(): HostApduBridge? = hostBridge
}
