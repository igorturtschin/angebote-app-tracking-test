package de.angebote.trackingtest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

/**
 * Screen tracking, see docs/tracking-concept.md, block 2.
 *
 * The automatic screen_view of Firebase is switched off in the manifest.
 * Every screen sends its own event instead.
 */

/** Values of the start screen, taken from the screen table of the concept. */
const val START_SCREEN_NAME = "Startseite"
const val START_CURRENT_OFFER = "Neustarter & Highlights"

/**
 * Sends screen_view every time the screen is resumed.
 *
 * The screens of this app are Compose state inside one Activity, so there
 * is no onResume method to override. The lifecycle observer gives the same
 * moment: it fires when the screen comes to the front, and again on every
 * return to it — back from an offer, or back from the browser.
 *
 * [screenName] is also the key of the effect. When the user opens another
 * offer, the same composable stays on screen with a new name, and the
 * effect runs again.
 */
@Composable
fun ScreenViewEffect(screenName: String, currentOffer: String) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, screenName) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    // SCREEN_CLASS is not sent. All screens are one Activity,
                    // so the value would say nothing about the screen.
                    param("current_offer", currentOffer)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
