package com.atruedev.kmpnfc.adapter

import android.app.Application
import android.content.Context
import androidx.startup.Initializer

/**
 * Auto-initializes [KmpNfc] via AndroidX App Startup.
 *
 * This runs automatically before the app's `Application.onCreate()`,
 * so consumers never need to call `KmpNfc.init(context)` manually.
 *
 * To disable auto-init and call `KmpNfc.init()` yourself, add to your
 * app's AndroidManifest.xml:
 * ```xml
 * <provider
 *     android:name="androidx.startup.InitializationProvider"
 *     android:authorities="${applicationId}.androidx-startup"
 *     tools:node="merge">
 *     <meta-data
 *         android:name="com.atruedev.kmpnfc.adapter.KmpNfcInitializer"
 *         tools:node="remove" />
 * </provider>
 * ```
 */
public class KmpNfcInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        KmpNfc.init(context)
        (context.applicationContext as? Application)?.let { ActivityTracker.install(it) }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
