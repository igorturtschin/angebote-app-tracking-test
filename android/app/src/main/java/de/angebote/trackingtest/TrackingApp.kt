package de.angebote.trackingtest

import android.app.Application
import com.amplitude.android.Amplitude
import com.amplitude.android.AutocaptureOption
import com.amplitude.android.TrackingOptions
import com.amplitude.android.plugins.SessionReplayPlugin
import com.amplitude.common.Logger
import com.amplitude.core.ServerZone

/**
 * Starts the Amplitude SDK for the whole app.
 *
 * The SDK is created here, in [Application], and not in an Activity: it counts
 * sessions and sends the app lifecycle events itself, so it has to see the
 * lifecycle of the whole app and needs the application context.
 *
 * Every value below is a decision of docs/tracking-concept.md, step 5.
 */
class TrackingApp : Application() {

    override fun onCreate() {
        super.onCreate()

        amplitude = Amplitude(BuildConfig.AMPLITUDE_API_KEY, applicationContext) {
            serverZone = ServerZone.EU

            // A whole set, not single flags: what is not listed is not
            // collected, including options that do not exist yet.
            autocapture = setOf(
                AutocaptureOption.SESSIONS,
                AutocaptureOption.APP_LIFECYCLES,
                AutocaptureOption.DEEP_LINKS,
            )
            enableAutocaptureRemoteConfig = false

            trackingOptions = TrackingOptions()
                .disableIpAddress()
                .disableAdid()
                .disableAppSetId()

            useAdvertisingIdForDeviceId = false
            useAppSetIdForDeviceId = false
            locationListening = false
            newDeviceIdPerInstall = false
            migrateLegacyData = false
            enableDiagnostics = false

            // Manual checking on one device: every event goes out on its own.
            // A real app keeps the defaults (30 events / 30 seconds).
            flushQueueSize = 1
            flushIntervalMillis = 5000
        }

        // Debug builds print every event to logcat: adb logcat -s Amplitude.
        // A release build stays quiet, so event contents never reach a bug
        // report from a user's phone.
        if (BuildConfig.DEBUG) {
            amplitude.logger.logMode = Logger.LogMode.DEBUG
        }

        amplitude.add(SessionReplayPlugin())
    }

    companion object {
        /** One instance per process. Events are sent through it. */
        lateinit var amplitude: Amplitude
            private set
    }
}
