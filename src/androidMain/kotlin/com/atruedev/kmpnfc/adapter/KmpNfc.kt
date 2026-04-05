package com.atruedev.kmpnfc.adapter

import android.content.Context

public object KmpNfc {
    internal lateinit var appContext: Context
        private set

    public fun init(context: Context) {
        appContext = context.applicationContext
    }

    internal fun requireContext(): Context {
        check(::appContext.isInitialized) {
            "Call KmpNfc.init(context) in your Application.onCreate() before using kmp-nfc"
        }
        return appContext
    }
}
