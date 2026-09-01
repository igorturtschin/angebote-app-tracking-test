# attachment-ai.md — notes for the AI assistant

Not part of the tracking concept. Implementation mechanics, machine-specific
commands and history, pulled out of `tracking-concept.md` so that document
stays a clean concept: **why** an event exists, the **code** to add, the
**values**, and a plain-language **trigger**. The developer chooses the
technical hook; this file records the one this app happens to use.

The user does not read this file. Keep concept-level material out of it and
implementation-level material out of the concept.

---

## adb on this machine

To read the events the app really sends, in the real order and without
Firebase DebugView, turn on the SDK's own logging and watch logcat:

```
adb shell setprop log.tag.FA VERBOSE
adb shell setprop log.tag.FA-SVC VERBOSE
adb logcat -d -s FA-SVC | grep "Logging event"
```

The app has to be restarted after `setprop`. The `FA-SVC` lines carry the
event name and the full parameter bundle.

`adb` is not in PATH. It is in
`%LOCALAPPDATA%\Android\Sdk\platform-tools\`. Two devices are usually
connected (emulator + a phone over Wi-Fi), so real commands need
`-s emulator-5554` (or the phone's id). Full PowerShell versions:
`infos/firebase-sdk-setup-log.md`.

---

## `screen_view` — hook used in this app

Concept says: send `screen_view` when a screen becomes visible, on first
draw and on every return.

The screens are Compose state inside one Activity, so there is no
per-screen `onResume()` method to override. A lifecycle observer gives the
same moment. Helper in
`android/app/src/main/java/de/angebote/trackingtest/Analytics.kt`:

```kotlin
@Composable
fun ScreenViewEffect(
    screenName: String,
    currentOffer: String,
    sendViewItemList: Boolean = false,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, screenName) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // screen_view, plus view_item_list on the start screen
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
```

- `ON_RESUME` (the lifecycle event) and the `onResume()` method are the
  same moment; only the listener location differs. This app uses the
  observer because the Activity does not know which of the 5 screens is
  open — the screen name lives in the composable.
- The observer also catches "start screen → offer screen", which happens
  inside an already-resumed Activity and would never hit `onResume()`.
- `screenName` is the `DisposableEffect` key: opening another offer keeps
  the same composable with a new name, so the effect re-runs and a new
  `screen_view` fires.
- `LocalLifecycleOwner` needs `androidx.lifecycle:lifecycle-runtime-compose`.
- `StartScreen` passes `START_SCREEN_NAME` / `START_CURRENT_OFFER`
  (constants in `Analytics.kt`); `OfferScreen` passes `offer.shop` /
  `offer.title`.
- The same `ON_RESUME` also sends `view_item_list`, but only on the start
  screen (`sendViewItemList = true`). The list is on the screen again, so
  it is reported again. The offer screen has no such companion: its
  `view_item` is not a screen event, see the next section.

---

## `app_open` — hook used in this app

Concept says: send `app_open` when the whole app comes to the foreground,
first, before `screen_view`, and not on rotation / dialogs / in-app
navigation.

`TrackingApp.onCreate` (the `Application`) adds one observer on
`ProcessLifecycleOwner` and sends `app_open` on `Lifecycle.Event.ON_START`:

```kotlin
ProcessLifecycleOwner.get().lifecycle.addObserver(
    LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_START) logAppOpen()
    },
)
```

- Process `ON_START` is the real foregrounding — same moment as
  `applicationDidBecomeActive` on iOS. The first launch and every return
  from background or from the browser click-out.
- An Activity's own `ON_RESUME` is not used: it also fires on Activity
  recreation (rotation, theme change) and on a system dialog on top, which
  would send a second `app_open` mid-visit.
- Ordering: the observer is registered once for the process in
  `TrackingApp.onCreate`, so its `ON_START` runs before any Activity
  `ON_RESUME` where the screens send `screen_view`. `app_open` comes out
  first.

---

## `view_item` — hook used in this app

Concept says: send `view_item` once, right after the first `screen_view` of
an offer screen, and not again when the user returns to that screen.

It is not sent from the lifecycle observer. `ON_RESUME` fires again on the
way back from the browser, from the background and after a system dialog,
and each of those would repeat `view_item` for one and the same opening.
The event belongs to the opening of the offer, not to the screen being in
front. Helper in `Analytics.kt`:

```kotlin
@Composable
fun OfferViewItemEffect(offer: Offer) {
    LaunchedEffect(offer.id) { logViewItem(offer) }
}
```

- `LaunchedEffect` runs when `OfferScreen` enters the composition and does
  not react to lifecycle events at all.
- Going back to the list removes `OfferScreen` from the composition, so
  picking the same offer again is a new opening and sends `view_item`
  again — which is what the concept asks for.
- Order: inside `OfferScreen`, `ScreenViewEffect` is called first. Its
  `DisposableEffect` runs in the apply phase and the observer, added to an
  already resumed lifecycle, gets `ON_RESUME` at once; the `LaunchedEffect`
  coroutine starts after that. So `screen_view` goes out before
  `view_item`. Checked on the emulator with `FA` verbose logging.
- This holds only while a rotation throws the app back to the start screen.
  If the open offer is ever made to survive a rotation, the composition is
  built again and this effect would fire a second time for the same
  opening. See the concept, Attachment 2, "Rotation resets the app to the
  start screen".

---

## History — the `screen_view` hook

An earlier draft of the concept recommended sending from `onResume` and
described overriding the `onResume()` method. That method does not exist
per-screen here (one Activity, Compose screens), so it was replaced by the
lifecycle observer above. If restructuring the concept again, do not
reintroduce "override `onResume()`".
