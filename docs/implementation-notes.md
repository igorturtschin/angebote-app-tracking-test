# Implementation notes

Where the tracking of `tracking-concept.md` is hooked into this app, and why
each hook was chosen. The concept says **what** to send, with which names and
values, and at which moment; it deliberately leaves the hook open, because the
hook depends on how the app is built. This file closes that gap.

Two parts:

- **Part 1 — Amplitude, branch `v1/amplitude`.** The current state.
- **Part 2 — Firebase, branch `main`.** The previous implementation, kept as
  a source of decisions: half of the hooks do not depend on the SDK and carry
  over unchanged.

---

# Part 1 — Amplitude (`v1/amplitude`)

Status: **the SDK is installed, no events are sent yet.** `TrackingApp.kt`
starts Amplitude with the configuration of the concept, the two dependencies
are in the version catalog, the INTERNET permission is in the manifest.
`Analytics.kt` does not exist — the concept does not describe events yet.

## What carries over from part 2, and what does not

| Event | Firebase (part 2) | Amplitude |
|---|---|---|
| app comes to the foreground | sent by hand from a `ProcessLifecycleOwner` observer | autocapture `APP_LIFECYCLES`, no code at all |
| screen | own `screen_view` from an `ON_RESUME` observer, automatic one switched off | the same by hand: autocapture `SCREEN_VIEWS` is off, all 5 screens live in one Activity |
| offer view | `LaunchedEffect(offer.id)`, deliberately not the lifecycle observer | same hook, it does not depend on the SDK |
| checking events | `FA` / `FA-SVC` in logcat, DebugView | different — see `tracking-concept.md`, *Проверка, что события доходят* |

## SDK initialisation

The concept fixes the configuration values and one constraint: the SDK is
created in `Application`, before the first Activity, with the application
context. How the instance is held is left to the implementation. Here it is
`TrackingApp.kt`, a `companion object` holding one instance for the process.

- The class is declared in the manifest as `android:name=".TrackingApp"` on
  `<application>`. Without that entry the class is never instantiated and
  nothing is initialised.
- One instance per process, to be read from `Analytics.kt` when events are
  added — the same role `Firebase.analytics` played on the Firebase branch,
  where the singleton came with the library and no `Application` subclass was
  needed.
- On the Firebase branch `TrackingApp` existed for a different reason: it
  registered the `ProcessLifecycleOwner` observer that sent `app_open`. On
  Amplitude that observer is gone — `APP_LIFECYCLES` covers it — and the class
  exists only to start the SDK.

Two imports are not where the Amplitude setup page suggests, and the compiler
is the only place this shows up:

- `ServerZone` is in `com.amplitude.core`, not `com.amplitude.android`;
- `SessionReplayPlugin` is in `com.amplitude.android.plugins`, not
  `com.amplitude.android.plugins.sessionreplay`.

## Which SDK version was actually built

The Gradle entry is `com.amplitude:analytics-android:1.+`, so the version is
resolved at build time and two builds weeks apart can carry different SDKs.
When this branch is built, note the resolved version here — otherwise a set
of recorded events cannot be tied to the SDK that produced it.

Resolved version: **1.30.1**, first build 2026-09-04.

## First run on an emulator, 2026-09-04

Installed on the `8a` emulator (no Google Play image needed) and started.
`adb logcat -s Amplitude` showed:

- `[Amplitude] Application Installed`, then `[Amplitude] Application Opened`;
  home button and back gave `Application Backgrounded` and a second
  `Application Opened`. Every upload answered `Handle response, status:
  SUCCESS`.
- `deviceId=9e87b6a4-…R` — the trailing `R` means a random id, so neither the
  advertising id nor the App Set id was used. That is what the concept asks
  for.
- `Google Play Services Util not found!` — expected on this image, and it
  changes nothing: the SDK does not need Play services.
- No `session_start` / `session_end` line in the log. The session id is set,
  but whether those events are sent has to be checked in Amplitude itself
  (User Look-Up), not in logcat.
- Two findings that belong to the concept, written into its Attachment 2:
  Session Replay runs at a 1 % sample rate and fetches its own remote config,
  and the SDK warns that offline mode needs `ACCESS_NETWORK_STATE`.

## Event hooks

To be filled in when the code exists. The concept does not describe events
yet — version 1.1 covers the SDK setup only.

---

# Part 2 — Firebase (`main`), for reference

Written for the Firebase SDK. Everything SDK-specific here is wrong for
Amplitude (`FA` logs, DebugView, the automatic `screen_view`); the decisions
about the Compose lifecycle hold whatever the vendor is.

All events lived in one file:
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
