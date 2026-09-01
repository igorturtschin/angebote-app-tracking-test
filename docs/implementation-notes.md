# Implementation notes

How the events of `tracking-concept.md` are wired into this app, and how to
check that they really go out.

The concept says **what** to send and **why**, and leaves the technical hook
to the developer — the hook depends on the app. Here all screens are Compose
state inside a single Activity, so there is no per-screen `onResume()` to
override, and the usual advice does not apply one to one. This file records
the hook chosen for each event and the reason for it.

All events live in one file:
`android/app/src/main/java/de/angebote/trackingtest/Analytics.kt`.

---

## Checking the events

The Firebase SDK can print every event it builds, with the full parameter
bundle, before it is uploaded. That is the fastest way to see what the app
sends, and the only way to see the real order:

```
adb shell setprop log.tag.FA VERBOSE
adb shell setprop log.tag.FA-SVC VERBOSE
adb logcat -d -s FA-SVC | grep "Logging event"
```

The app has to be restarted after `setprop`. The same output is visible in
the Logcat window of Android Studio, with `FA-SVC` as the tag filter.

To send the events to Firebase **DebugView** instead, mark the app as a
debug device:

```
adb shell setprop debug.firebase.analytics.app de.angebote.trackingtest
```

`.none.` in place of the package name switches it off again. DebugView
shows the events as the server receives them, so it also confirms the
parameters that actually arrive; logcat shows them earlier, on the device.

On Windows `adb` is not in PATH — it comes with the Android SDK, in
`%LOCALAPPDATA%\Android\Sdk\platform-tools\`.

---

## `screen_view`

Concept: send `screen_view` when a screen becomes visible, on the first
draw and on every return to it.

There is no per-screen `onResume()` to override, so the same moment is
taken from a lifecycle observer. Helper `ScreenViewEffect` in
`Analytics.kt`:

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

- The `ON_RESUME` lifecycle event and the `onResume()` method are the same
  moment; only the place of the listener differs. The observer is used
  because the Activity does not know which of the five screens is open —
  the screen name lives in the composable.
- The observer also catches "start screen → offer screen". That happens
  inside an already resumed Activity and would never reach `onResume()`.
- `screenName` is the key of the effect: opening another offer keeps the
  same composable with a new name, so the effect runs again and a new
  `screen_view` goes out.
- `LocalLifecycleOwner` needs `androidx.lifecycle:lifecycle-runtime-compose`.
- `StartScreen` passes `START_SCREEN_NAME` / `START_CURRENT_OFFER`
  (constants in `Analytics.kt`); `OfferScreen` passes `offer.shop` /
  `offer.title`.
- The same `ON_RESUME` also sends `view_item_list`, but only on the start
  screen (`sendViewItemList = true`): the list is on the screen again, so
  it is reported again. The offer screen has no such companion — see
  `view_item` below.

An earlier draft of the concept described overriding `onResume()`. That
method does not exist per screen in an app built this way, so the concept
now names the moment and leaves the hook open.

---

## `app_open`

Concept: send `app_open` when the whole app comes to the foreground, first,
before `screen_view`, and not on a rotation, a dialog, or in-app
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

- Process `ON_START` is the real foregrounding — the same moment as
  `applicationDidBecomeActive` on iOS: the first launch and every return
  from the background or from the browser click-out.
- The Activity's own `ON_RESUME` is not used. It also fires on an Activity
  recreation (rotation, theme change) and after a system dialog on top,
  which would send a second `app_open` in the middle of a visit.
- Order: the observer is registered once for the process in
  `TrackingApp.onCreate`, so its `ON_START` runs before any Activity
  `ON_RESUME`, where the screens send `screen_view`. `app_open` comes out
  first.

---

## `view_item`

Concept: `view_item` belongs to one opening of an offer screen — the tap on
an offer in the list opened it. A return to the same screen does not repeat
it.

It is therefore **not** sent from the lifecycle observer. `ON_RESUME` fires
again on the way back from the browser, from the background and after a
system dialog, and each of those would repeat `view_item` for one and the
same opening. Helper in `Analytics.kt`:

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
  `DisposableEffect` runs in the apply phase, and the observer, added to an
  already resumed lifecycle, receives `ON_RESUME` at once; the
  `LaunchedEffect` coroutine starts after that. So `screen_view` goes out
  before `view_item`. Checked on an emulator with `FA` verbose logging.
- This holds while a rotation throws the app back to the start screen. If
  the open offer is ever made to survive a rotation, the composition is
  built again and this effect would fire a second time for the same
  opening. See the concept, Attachment 2, "Rotation resets the app to the
  start screen".
