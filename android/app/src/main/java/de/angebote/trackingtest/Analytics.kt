package de.angebote.trackingtest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Screen tracking, see docs/tracking-concept.md, section "3. Экраны —
 * screen_view".
 *
 * The automatic screen_view of Amplitude (autocapture SCREEN_VIEWS) is off:
 * it reads the screen name from the Activity, and all five screens of this
 * app live in one Activity. Every screen sends its own event instead.
 *
 * The event and property names are our own, not the ones of the autocapture
 * schema. Amplitude has no constants for them — an event name is a plain
 * string — so they are declared here, once.
 */

private const val EVENT_SCREEN_VIEW = "screen_view"
private const val PROP_SCREEN_NAME = "screen_name"
private const val PROP_CURRENT_OFFER = "current_offer"

/** Start screen values, from "Значения трекинга" in Attachment 1 of the concept. */
const val START_SCREEN_NAME = "Startseite"
const val START_CURRENT_OFFER = "Neustarter & Highlights"

/**
 * Sends screen_view every time the screen is resumed.
 *
 * The screens of this app are Compose state inside one Activity, so there is
 * no onResume method to override. The lifecycle observer gives the same
 * moment: it fires when the screen comes to the front, and again on every
 * return to it — back from an offer, or back from the browser.
 *
 * [screenName] is also the key of the effect. When the user opens another
 * offer, the same composable stays on screen with a new name, and the effect
 * runs again, so the new screen reports itself.
 */
@Composable
fun ScreenViewEffect(screenName: String, currentOffer: String) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, screenName) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                TrackingApp.amplitude.track(
                    EVENT_SCREEN_VIEW,
                    mapOf(
                        PROP_SCREEN_NAME to screenName,
                        // No screen class. All screens are one Activity, so the
                        // value would say nothing about the screen.
                        PROP_CURRENT_OFFER to currentOffer,
                    ),
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
