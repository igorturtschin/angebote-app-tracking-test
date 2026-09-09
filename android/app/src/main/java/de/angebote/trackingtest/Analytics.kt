package de.angebote.trackingtest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Screen tracking, see docs/tracking-concept.md, section "3. Экраны —
 * screen_view".
 *
 * The automatic screen_view of Amplitude (autocapture SCREEN_VIEWS) is off:
 * it reads the screen name from the Activity, and all 14 screens of this
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

/** Test screen names: "Testseite 1" … "Testseite 5". These screens carry no
 *  current_offer — see the concept, section 3, "Code". */
fun testScreenName(page: Int): String = "Testseite $page"

/**
 * Remembers the screen the last screen_view was sent for.
 *
 * A ViewModel survives an Activity recreation (a rotation) but dies with the
 * process — a cold start, or the system reclaiming the app from the
 * background. That is the exact line [ScreenViewEffect] needs: a rotation is
 * the app rebuilding a screen the user never left, so no new screen_view;
 * everything else is the user arriving at or returning to a screen, so a
 * screen_view goes out.
 */
class ScreenTrackingViewModel : ViewModel() {
    var lastLoggedScreen: String? = null
}

/**
 * Sends screen_view when a screen becomes visible, and on every return to it,
 * but not when a rotation rebuilds a screen the user is already on.
 *
 * The screens of this app are Compose state inside one Activity, so there is
 * no onResume method to override. A lifecycle observer gives the same moment:
 * it fires when the screen comes to the front, and again on every return —
 * back from an offer, back from the browser, back from the background.
 *
 * A rotation also lands on ON_RESUME: the Activity is recreated, this
 * composable is built again and the observer is added anew. It is told apart
 * from a real return by two facts together — the app never left the
 * foreground (no ON_STOP reached this observer) and the screen is the one the
 * last screen_view was already sent for ([ScreenTrackingViewModel], which
 * survives the rotation).
 *
 * [screenName] is also the key of the effect. When the user opens another
 * offer, the same composable stays on screen with a new name, the effect runs
 * again, and the new screen reports itself.
 *
 * [currentOffer] is null on the test screens, which have no offer. The
 * property is then left off the event rather than sent empty.
 *
 * [onLogged] runs in the same moment as the screen_view, right after it, and
 * under the same rotation guard. The start screen uses it to send its
 * view_item_list events together with the screen_view (concept, section 4).
 */
@Composable
fun ScreenViewEffect(
    screenName: String,
    currentOffer: String?,
    onLogged: (() -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val tracking: ScreenTrackingViewModel = viewModel()
    DisposableEffect(lifecycleOwner, screenName) {
        var leftForeground = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> leftForeground = true
                Lifecycle.Event.ON_RESUME -> {
                    val rotation = !leftForeground && screenName == tracking.lastLoggedScreen
                    leftForeground = false
                    if (!rotation) {
                        val properties = buildMap {
                            put(PROP_SCREEN_NAME, screenName)
                            // No screen class. All screens are one Activity,
                            // so the value would say nothing about the screen.
                            if (currentOffer != null) {
                                put(PROP_CURRENT_OFFER, currentOffer)
                            }
                        }
                        TrackingApp.amplitude.track(EVENT_SCREEN_VIEW, properties)
                        tracking.lastLoggedScreen = screenName
                        onLogged?.invoke()
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
