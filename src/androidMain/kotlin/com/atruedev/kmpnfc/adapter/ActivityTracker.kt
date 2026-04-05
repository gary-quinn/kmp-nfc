package com.atruedev.kmpnfc.adapter

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.lang.ref.WeakReference

/**
 * Singleton that tracks the current resumed [Activity].
 *
 * Registered once at initialization via [KmpNfcInitializer] — never
 * re-registered. Uses a [WeakReference] to avoid leaking activities.
 */
internal object ActivityTracker : Application.ActivityLifecycleCallbacks {
    private var current: WeakReference<Activity>? = null
    private var registered = false

    /** Register on the application. Idempotent — safe to call multiple times. */
    fun install(application: Application) {
        if (registered) return
        registered = true
        application.registerActivityLifecycleCallbacks(this)
    }

    /** The currently resumed activity, or null if none. */
    val resumedActivity: Activity? get() = current?.get()

    override fun onActivityResumed(activity: Activity) {
        current = WeakReference(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        if (current?.get() === activity) current = null
    }

    override fun onActivityCreated(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) = Unit

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
        activity: Activity,
        outState: Bundle,
    ) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit
}
