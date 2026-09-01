package de.angebote.trackingtest

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner

/**
 * Adds the process-wide lifecycle observer that sends app_open
 * (see docs/tracking-concept.md, block 4).
 *
 * ON_START of the whole app is a real foregrounding: the first launch and
 * every return from the background or from the browser click-out. Unlike
 * an Activity's ON_RESUME, it does not fire on an Activity recreation
 * (rotation, theme) or when a system dialog sits on top of the app, so
 * app_open is not sent again in those cases.
 *
 * The observer is added here, in TrackingApp.onCreate, so it is registered
 * once for the process and runs before any Activity ON_RESUME. app_open
 * still comes out before the screen_view of the visible screen.
 */
class TrackingApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_START) logAppOpen()
            },
        )
    }
}
