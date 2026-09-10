package de.angebote.trackingtest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.amplitude.core.Amplitude
import com.amplitude.core.events.BaseEvent
import com.amplitude.core.platform.Plugin
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
private const val PROP_PREVIOUS_SCREEN_NAME = "previous_screen_name"
private const val PROP_CURRENT_OFFER = "current_offer"

/** previous_screen_name of the first screen of a session. Not a screen name:
 *  a mark that there was no screen before this one (concept, section 3). */
private const val SESSION_START = "session_start"

/** Start screen values, from "Значения трекинга" in Attachment 1 of the concept. */
const val START_SCREEN_NAME = "Startseite"
const val START_CURRENT_OFFER = "Neustarter & Highlights"

/** Test screen names: "Testseite 1" … "Testseite 5". These screens carry no
 *  current_offer — see the concept, section 3, "Code". */
fun testScreenName(page: Int): String = "Testseite $page"

/**
 * The one field the app keeps about navigation: the screen the user is on
 * (concept, section 3, "Что помнит приложение").
 *
 * Two readers:
 * - [ScreenViewEffect], which takes what is in it as previous_screen_name and
 *   then writes the new screen into it;
 * - [ScreenNamePlugin], which stamps screen_name on every other event.
 *
 * The field lives in the process, not on disk: after a restart the chain
 * starts over, otherwise the first screen of a new launch would report a
 * transition that never happened.
 *
 * It is scoped to a session. The session boundary is the SDK's, not ours
 * (concept, section 3, "Откуда берутся значения"): the field is dropped when
 * the SDK reports a new session id, and a screen written in an older session
 * is not handed out. So the events the SDK makes at the boundary —
 * session_end, session_start, the Application Opened of the new session — go
 * without screen_name, and the first screen_view of the session reports
 * session_start as its previous screen.
 */
object CurrentScreen {
    private const val NO_SESSION = -1L

    private var session: Long = NO_SESSION
    private var name: String? = null

    /** Step 1-2 of the concept: read what is remembered, then write the new
     *  screen. Returns the previous_screen_name of the event about to go. */
    @Synchronized
    fun enter(screenName: String, sessionId: Long): String {
        if (sessionId != session) {
            session = sessionId
            name = null
        }
        val previous = name ?: SESSION_START
        name = screenName
        return previous
    }

    /** The screen name for an event of this session, or null when the field is
     *  empty or holds a screen from another session. */
    @Synchronized
    fun nameFor(sessionId: Long?): String? =
        if (sessionId != null && sessionId == session) name else null

    @Synchronized
    fun forget() {
        session = NO_SESSION
        name = null
    }
}

/**
 * Puts screen_name on every event that does not carry it already (concept,
 * section 1, step 8).
 *
 * Amplitude does not remember the screen: an event carries the properties it
 * was given and nothing else. A plugin is the SDK's own extension point — the
 * path of an event is a pipeline of stages, and this object sits in the
 * Enrichment stage, so every event passes through [execute].
 *
 * Only where the property is missing: screen_view puts its own name in at the
 * moment of the call, which is more reliable than this one — between track()
 * and the pipeline the screen may already have changed.
 */
class ScreenNamePlugin : Plugin {
    override lateinit var amplitude: Amplitude
    override val type = Plugin.Type.Enrichment

    override fun execute(event: BaseEvent): BaseEvent {
        val screenName = CurrentScreen.nameFor(event.sessionId) ?: return event
        val properties = event.eventProperties ?: mutableMapOf()
        if (!properties.containsKey(PROP_SCREEN_NAME)) {
            properties[PROP_SCREEN_NAME] = screenName
            event.eventProperties = properties
        }
        return event
    }

    /** The SDK opened a new session: the remembered screen belongs to the old
     *  one and is dropped. */
    override fun onSessionIdChanged(sessionId: Long) = CurrentScreen.forget()
}

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
 * previous_screen_name comes from [CurrentScreen]: what is remembered there
 * is the screen the user came from, and it is replaced with this screen right
 * before the event goes out. A rotation sends nothing and so writes nothing —
 * the memory keeps the screen the user actually came from.
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
                        val previous = CurrentScreen.enter(
                            screenName,
                            TrackingApp.amplitude.sessionId,
                        )
                        val properties = buildMap {
                            put(PROP_SCREEN_NAME, screenName)
                            put(PROP_PREVIOUS_SCREEN_NAME, previous)
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
