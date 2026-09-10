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

# Part 1 — Amplitude (`v1/amplitude`, then `v2/amplitude`)

Status: **the SDK is installed, `screen_view` and the section 4 e-commerce
events are sent.** `TrackingApp.kt` starts Amplitude with the configuration of
the concept, the dependencies are in the version catalog, the INTERNET
permission is in the manifest. `Analytics.kt` holds the screen event,
`Ecommerce.kt` holds `view_item_list` / `select_item` / `view_item` /
`begin_checkout`. The *DA — настройка в Amplitude* part of section 4 (property
splitting, the reports, the funnel) is a project setting, not code, and is
still open.

## App v2 (`v2/amplitude`)

The branch adds the app changes of concept v2.0: a bottom navigation bar,
eight offers in four categories, two offer blocks on the start screen and
five test screens. It carries no SDK change — `v2/amplitude` is branched off
`v1/amplitude` and the Amplitude setup is untouched.

What this does to the tracking:

- **Bottom navigation** (`MainActivity.App`, a `NavigationBar` with a `Tab`
  enum) sends no event of its own. A tap on it switches the screen, and the
  new screen sends its `screen_view` as any other screen entry does. The home
  icon is a hand-drawn `ImageVector` (`HomeIcon`) so the app needs no
  material-icons dependency. The `Scaffold` with this bar wraps every screen,
  the offer screen included: the offer screen has **no back button of its
  own** — the way back to the start screen is the home item or the system
  back key. `OfferScreen` lost its `onBack` parameter.
- **Two blocks on the start screen** (`OFFER_LISTS`, rendered by `OfferBlock`)
  do not change screen tracking: it is still one `Startseite` screen with one
  `screen_view` and `current_offer = "Neustarter & Highlights"`. The list
  ids/names (`Offers.kt`, `HIGHLIGHTS` / `NEUSTARTER`) feed the section 4
  `view_item_list` events — see *E-commerce events* below.
- **Test screens** (`MainActivity.TestScreen`) send `screen_view` with
  `screen_name = "Testseite 1".."Testseite 5"` and **no `current_offer`**.
  `ScreenViewEffect` now takes `currentOffer: String?` and leaves the property
  off the event when it is null (`Analytics.kt`, `buildMap`). Moving between
  test screens changes the effect key, so each one sends its own event — same
  mechanism as opening another offer.
- The `Angebot 1`..`Angebot 8` buttons on a test screen open the offer screen
  directly. The offer's own `screen_view` fires as usual; nothing tells the
  offer it was reached without a list, which is the point of these screens for
  section 4.

Checked on emulator `8a` (Android 17), 2026-09-06: start screen shows both
blocks, the bottom bar shows the house icon and the `1–5` button, test screens
render, and the queued event payloads are `{"screen_name":"Testseite 3"}` for
a test screen and `{"screen_name":"Fitwerk","current_offer":"20 % auf alle
Sportschuhe"}` for an offer opened from it. Re-checked after moving the bottom
bar onto the offer screen: the offer screen has no back button, the home item
returns to `Startseite` and sends its `screen_view`. `versionName` bumped to
`2.0`, `versionCode` to `2`.

## What carries over from part 2, and what does not

| Event | Firebase (part 2) | Amplitude |
|---|---|---|
| app comes to the foreground | sent by hand from a `ProcessLifecycleOwner` observer | autocapture `APP_LIFECYCLES`, no code at all — but it also fires on a rotation, which the hand-written one did not |
| screen | own `screen_view` from an `ON_RESUME` observer, automatic one switched off | same hook and event name; the SDK call differs, and a rotation no longer repeats the event — see *`screen_view`* below |
| offer view | `LaunchedEffect(offer.id)`, deliberately not the lifecycle observer | same idea, now `ViewItemEffect(offer, opening)` keyed on an opening counter so a rotation that keeps the offer does not repeat it — see *E-commerce events* |
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
  (User Look-Up), not in logcat. Checked on 2026-09-06: they are sent — see
  the second run below.
- Two findings that belong to the concept, written into its Attachment 2:
  Session Replay runs at a 1 % sample rate and fetches its own remote config,
  and the SDK warns that offline mode needs `ACCESS_NETWORK_STATE`.

## Event hooks

### `screen_view`

Concept: send `screen_view` when a screen becomes visible and on every return
to it, but not when a rotation rebuilds a screen the user is already on
(`tracking-concept.md`, section 3, *Trigger*).

There is no per-screen `onResume()` to override — the five screens are Compose
state inside one Activity — so the moment comes from a lifecycle observer.
`ScreenViewEffect` in `Analytics.kt`:

```kotlin
class ScreenTrackingViewModel : ViewModel() {
    var lastLoggedScreen: String? = null
}

@Composable
fun ScreenViewEffect(screenName: String, currentOffer: String) {
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
                        TrackingApp.amplitude.track(EVENT_SCREEN_VIEW, mapOf(...))
                        tracking.lastLoggedScreen = screenName
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
```

- `ON_RESUME` and `onResume()` are the same moment; only the place of the
  listener differs. The observer is used because the Activity does not know
  which of the five screens is open — the screen name lives in the composable.
- The observer also catches "start screen → offer screen". That happens inside
  an already resumed Activity and would never reach `onResume()`.
- `screenName` is the key of the effect: opening another offer keeps the same
  composable with a new name, so the effect runs again and a new `screen_view`
  goes out.
- `StartScreen` passes `START_SCREEN_NAME` / `START_CURRENT_OFFER` (constants
  in `Analytics.kt`); `OfferScreen` passes `offer.shop` / `offer.title`.

**Why the rotation guard is not just `rememberSaveable`.** The rule is: a
rotation sends nothing, but a user who left the app and came back must get a
`screen_view` — even if the system killed the app in the background and the
screen had to be rebuilt. So the code needs to tell "the system rebuilt this
screen under the user" apart from "the user came back to this screen". Two
signals together do it:

- `ScreenTrackingViewModel` holds the name of the last logged screen. A
  `ViewModel` survives a rotation (the process lives) but dies with the
  process — a cold start, or the system reclaiming the app from the
  background. So after a rotation `lastLoggedScreen` still equals the current
  screen; after a kill-and-return it is `null`.
- `leftForeground` is set on `ON_STOP`. A real return to the foreground (from
  the browser, from another app, from Home) passes through `ON_STOP` on the
  *same* observer first. A rotation gives the new composition a *fresh*
  observer that never saw `ON_STOP`.

`screen_view` is skipped only when both say "rotation": the app never left the
foreground **and** this screen was already logged. Every other path — new
screen, back-navigation, return from background, return after a process kill,
cold start — sends.

`viewModel()` needs `androidx.lifecycle:lifecycle-viewmodel-compose`; it was
added to the version catalog and `app/build.gradle.kts` next to
`lifecycle-runtime-compose` (which came back earlier for `LocalLifecycleOwner`).
`lifecycle-process` is still **not** back — autocapture covers `app_open`.

**The open offer surviving the rotation is what made this necessary.**
`MainActivity.kt` now keeps the open offer as an id in `rememberSaveable`
(`openOfferId`), and `OfferScreen` keeps `codeVisible` / `used` the same way,
so a rotation stays on the offer screen instead of dropping to the list. Once
the offer screen survives the rotation, its `ScreenViewEffect` runs again for
the same screen — which is exactly the duplicate the guard above removes. On
the Firebase branch the offer was lost on rotation, so this never showed.

What differs from Firebase, and it is only the call itself:

| | Firebase | Amplitude |
|---|---|---|
| call | `Firebase.analytics.logEvent(...) { param(...) }` | `TrackingApp.amplitude.track(name, mapOf(...))` |
| instance | singleton from the library | `TrackingApp.amplitude`, one per process |
| names | `FirebaseAnalytics.Event` / `.Param` constants | plain strings, declared at the top of `Analytics.kt` |

`sendViewItemList`, the boolean third parameter this helper had on the
Firebase branch, came back in a different shape: an `onLogged: (() -> Unit)?`
lambda that runs in the same guarded branch as the `screen_view`. The start
screen uses it for its two `view_item_list` events — see *E-commerce events*.

## Second run on an emulator, 2026-09-06

Emulator `8a`, Android 17, app uninstalled first so the run starts clean.
33 events, all of them checked in Amplitude through User Look-Up (MCP), not
only in logcat.

What the run confirms:

- Cold start: `[Amplitude] Start Session` → `Application Installed` →
  `Application Opened` → `screen_view(Startseite)`.
- Navigation: one `screen_view` per screen entry, and one on every return to
  the list. Names and values are exactly the ones in the concept, Attachment
  1: `screen_name` = `Startseite` / `Fitwerk` / `Nordlicht Wohnen`,
  `current_offer` the matching offer title.
- `session_start` **is** sent. Part 1 above left this open after the first
  run, because logcat prints no line for it; User Look-Up shows it as event 1
  with the display name `[Amplitude] Start Session`.
- All 33 events carry the same `session_id`. Nothing in the run — not the
  rotations, not the trips to the home screen — started a second session,
  because none of them was longer than `minTimeBetweenSessionsMillis`.

Two findings went into the concept, Attachment 2, *Автозахват `Application
Opened`*: a rotation produces a `Backgrounded` + `Opened` pair, and the order
of `Application Opened` against our `screen_view` is not stable.

## Third run on an emulator, 2026-09-06 — the rotation fix

Emulator `8a`, Android 17. This run checks the change above; `screen_view`
events counted in `adb logcat -s Amplitude` per step, the screen confirmed
with screenshots. The first `screen_view` payload was read from the SDK queue
file (network cut) and carried `screen_name: Startseite`,
`current_offer: Neustarter & Highlights`; the code puts the same `screenName`
into every event, so the values on later events are unchanged from run 2.

| Step | `screen_view` | On screen |
|---|---|---|
| cold start | 1 (`Startseite`) | list |
| open Fitwerk offer | 1 | offer screen |
| rotate on offer (either way) | **0** | still the Fitwerk offer, code/buttons kept |
| back to list | 1 | list |
| rotate on list (either way) | **0** | list — run 2 would have sent a spurious `screen_view(Startseite)` here |
| Home, then reopen | 1 | — |
| "Don't keep activities" on: offer → Home → reopen | 1 | back on the offer |

Every rotation still produced the `[Amplitude] Application Backgrounded` +
`Application Opened` pair — that autocapture event is untouched (concept,
Attachment 2).

### Section 4 (e-commerce) will have to handle the rotation too

`view_item` on the Firebase branch is sent from `LaunchedEffect(offer.id)`,
which fires whenever `OfferScreen` enters the composition. Now that the open
offer survives a rotation, that composition is rebuilt on rotation and the
effect would fire `view_item` again for the same opening. Whoever ports
section 4 needs an id kept across the rebuild (a `rememberSaveable` flag, or
the same `ScreenTrackingViewModel` idea) so `view_item` stays one-per-opening.

### Reading the event payload without the Amplitude UI

logcat only prints `Logged event with type: screen_view` — the name, never
the properties. To see the properties on the device, cut the network so the
events stay in the SDK queue, then read the queue file:

```
adb shell svc wifi disable && adb shell svc data disable
adb shell "run-as de.angebote.trackingtest cat \
  'app_amplitude/de.angebote.trackingtest/\$default_instance/analytics/events/\$default_instance-13.tmp'"
```

The file is JSON objects separated by a NUL byte, one per event, with the
full payload. `run-as` works because the build is debuggable; no root is
needed. Turning the network back on empties the queue.

---

# E-commerce events — section 4 (`v2/amplitude`)

`Ecommerce.kt`. The chain `view_item_list` → `select_item` → `view_item` →
`begin_checkout`, plus the `attribution_context` that carries the source list
from `select_item` to the target action. The concept fixes names, values and
moments; this is where each one is hooked.

## `AttributionContext` and `EcommerceViewModel`

The concept keeps **one** context — only one offer is open at a time. It is
held in `EcommerceViewModel` (obtained with `viewModel()` from
`MainActivity.App` and from `OfferScreen`), not in `rememberSaveable`:

- it must survive a rotation (a `ViewModel` does) — a rotation keeps the user
  on the offer, so `view_item` / `begin_checkout` must still find the list;
- it must die with the process (a `ViewModel` does) — the concept says the
  context lives "until it is overwritten or the app restarts", and after a
  cold start `view_item` falls back to the offer on screen with no list.

The same ViewModel holds `lastViewItemOpening`, the guard that keeps
`view_item` one-per-opening (below).

## `view_item_list`

Concept: sent together with the `screen_view` of the start screen, one event
per block, on every start-screen entry (first, back from an offer, bottom-nav,
return from the browser), not on scroll or rotation.

Hook: `ScreenViewEffect` grew an `onLogged: (() -> Unit)?` lambda that runs in
the same lifecycle-observer branch as the `screen_view`, right after it and
under the same rotation guard. `StartScreen` passes
`{ OFFER_LISTS.forEach { trackViewItemList(it) } }`. So the two
`view_item_list` events share the timing and the rotation/return rules of the
start-screen `screen_view` for free — no second observer. `index` is the
position of the card inside its block (`offerIds.mapIndexed`), which is exactly
the concept's position table.

## `select_item`

Concept: the tap on "Zum Angebot" in a card. One event, with the offer and the
list of that card. Not sent for an offer opened from a test screen.

Hook: `MainActivity.StartScreen`'s `onOfferClick` now carries
`(offer, list, index)`; `OfferBlock` uses `forEachIndexed` to know the index.
The click calls `selectItem(...)`, which sends the event **and** writes the
context, then navigation sets `openOfferId`. The test screen sends nothing
and writes nothing — see *The test screen no longer overwrites the
attribution context* below.

## `view_item`

Concept: right after the `screen_view` of the offer screen, exactly one per
opening. Not on return from the browser / the background, not on rotation.
Reopening the same offer from the list is a new opening and sends again.

Hook: `ViewItemEffect(offer, opening)` in `Ecommerce.kt`. It is **not** on the
lifecycle observer (that fires again on every return). `opening` is an `Int`
counter in `MainActivity` (`rememberSaveable`), bumped on every navigation into
an offer — from a card and from a test-screen button. `ViewItemEffect` sends
only when `opening != vm.lastViewItemOpening`:

- rotation: the composition is rebuilt and the `LaunchedEffect` runs again, but
  `opening` is unchanged and the ViewModel remembers it → nothing sent;
- return from browser/background: same composition, `LaunchedEffect` does not
  re-run at all;
- reopen the same offer: `opening` was bumped → sends again.

This is the guard the earlier note ("Section 4 will have to handle the
rotation too") asked for. Order: `ScreenViewEffect`'s `DisposableEffect` runs
in the apply phase and its observer gets `ON_RESUME` synchronously;
`ViewItemEffect`'s `LaunchedEffect` coroutine starts after that, so
`screen_view` precedes `view_item`.

The event properties (`offerEventProps`): if the stored context is about this
offer and has a list, the event carries `item_list_id` / `item_list_name` and
the stored object (with `index`); otherwise just `items` with the offer on
screen, no list, no `index`.

## `begin_checkout`

Concept: the tap on "Zum Shop" or "Download", before the side effect, same
properties as the `view_item` of this opening, one per tap.

Hook: `trackBeginCheckout(ecommerce, offer)` is the first line of both
`onClick` blocks in `OfferScreen`, before `openShop` / `downloadCouponPdf`. It
reuses `offerEventProps`, so its payload equals the `view_item` of the same
opening. No dedup — two taps send two events.

## Emulator test, 2026-09-07

Emulator `8a`, Android 17, app uninstalled first. SDK version resolved at
build: **1.30.1** (unchanged). Network cut before launch, so every event
stayed in the queue file; read with `run-as` after the run (see *Reading the
event payload* above), then the network was turned back on and all 25 events
uploaded with `status: SUCCESS`.

Server side checked with the Amplitude MCP the same day: totals for
2026-09-07 are `view_item_list` 4, `select_item` 2, `view_item` 3,
`begin_checkout` 3, `screen_view` 8 — exactly the run. Split by
`item_list_id`: `select_item` is all `home_highlights` (the two list opens),
`view_item` and `begin_checkout` are `home_highlights` ×2 plus `(none)` ×1 —
the `(none)` being the offer opened from the test screen, the concept's
"без листа" group. `items` is stored as `type: any` (not split): the array
arrives whole but its children need property splitting, a project setting
that is off — concept, section 4, *DA — настройка в Amplitude*.

Actions in the emulator, and the events they produced, in order:

| # | Action in the emulator | Events (in queue order) |
|---|---|---|
| 1 | cold start, land on the start screen | `session_start`, `Application Installed`, `Application Opened`, `screen_view{Startseite}`, `view_item_list{home_highlights, 4 items, index 0–3}`, `view_item_list{home_neustarter, 6 items, index 0–5}` |
| 2 | tap "Zum Angebot" on Fitwerk (Highlights, index 0) | `select_item{home_highlights, offer_01, index 0}`, `screen_view{Fitwerk}`, `view_item{home_highlights, offer_01, index 0}` |
| 3 | tap "Zum Shop" | `begin_checkout{home_highlights, offer_01, index 0}`, then `Application Backgrounded` (browser opened) |
| 4 | return to the app | `screen_view{Fitwerk}` **only — no second `view_item`**, then `Application Opened` |
| 5 | system back to the start screen | `screen_view{Startseite}`, `view_item_list{home_highlights}`, `view_item_list{home_neustarter}` |
| 6 | scroll, tap "Zum Angebot" on Kaffeekontor (Highlights, index 2) | `select_item{home_highlights, offer_03, index 2}`, `screen_view{Kaffeekontor}`, `view_item{home_highlights, offer_03, index 2}` |
| 7 | tap "Download" | `begin_checkout{home_highlights, offer_03, index 2}` |
| 8 | bottom nav → Test | `screen_view{Testseite 1}` — **no `view_item_list`** |
| 9 | tap "Angebot 5" (offer_05, Fadenwerk) | `screen_view{Fadenwerk}`, `view_item{items only — no list, no index}` — **no `select_item`** |
| 10 | tap "Zum Shop" | `begin_checkout{items only — no list, no index}`, then `Application Backgrounded` |

What the run confirms against the concept:

- `view_item_list` fires once per block, with the concept's exact `index`
  table (`offer_01` is index 0 in Highlights and index 4 in Neustarter,
  `offer_03` is 2 and 5), and repeats on every return to the start screen
  (step 5), not on scroll (step 6 scrolled first, no extra event).
- The list context set at `select_item` reaches `view_item` and
  `begin_checkout` unchanged (steps 2, 6).
- A return to an offer screen sends `screen_view` again but **not**
  `view_item` (step 4) — the opening is the same.
- An offer opened from a test screen sends **no `select_item`**, and its
  `view_item` / `begin_checkout` carry `items` only, no list and no `index`
  (steps 9, 10).
- `screen_view` precedes `view_item` on every offer opening.
- Test screens send no `view_item_list` (step 8).

Order caveat seen again: on the return in step 4 the offer's `screen_view`
landed before `Application Opened`, matching the concept's note that the order
of `Application Opened` against our events is not stable.

---

# `screen_name` everywhere, `previous_screen_name`, button events (`v2/amplitude`)

Concept sections 1 (step 8), 3 (*Предыдущий экран*) and 4 (*Четыре события
кнопок*). Three pieces that share one thing: the app has to remember which
screen the user is on.

## One field: `CurrentScreen`

`Analytics.kt`. An object with a single meaningful field — the screen the
user is on — plus the session it was written in. Not a ViewModel and not
`rememberSaveable`: the plugin reads it from the SDK pipeline, outside any
composition, and the concept wants it to die with the process.

Two readers, and the order inside `ScreenViewEffect` is what makes them
agree:

1. `enter(screenName, sessionId)` returns what was in the field — that is
   the event's `previous_screen_name` — and writes the new screen in.
   Called **inside** the rotation guard, so a rotation, which sends no
   `screen_view`, also writes nothing and the memory keeps the screen the
   user really came from.
2. `nameFor(sessionId)` hands the screen to the plugin, but only for an
   event of the same session.

The session comes from the SDK (`amplitude.sessionId`), never from a timer
of our own, so our chain cannot drift from the `session_id` the charts cut
data by. `enter` clears the field when it sees a new session, and the plugin
clears it from `onSessionIdChanged`, the SDK's own callback. Either way the
first `screen_view` of a session reports `session_start` as its previous
screen.

## `ScreenNamePlugin`

An `Enrichment` plugin added in `TrackingApp` right after `SessionReplay`.
`execute` writes `screen_name` only when the event does not have it: our own
`screen_view` puts its name in at the moment of the call, which is more
reliable than the pipeline, where the screen may already have changed.

An event of another session — or one sent while the field is empty — goes
out without the property, which is exactly the concept's list of events that
do not get it (`session_start`, `Application Installed`, the first
`Application Opened`).

## The four button events

`ButtonEvents.kt`. Four plain calls with no item payload; `screen_name` from
the plugin is what ties them to the offer. `code_copied` on `go_to_shop`
reads the same per-visit state that colours the pressed button — the `used`
list of `OfferScreen`, which is dropped when the screen leaves the
composition, so every opening starts at `no`.

Both target buttons send two events, the button one first, then
`begin_checkout`, and both before the side effect.

## The test screen no longer overwrites the attribution context

The 2026-09-07 build wrote a list-less context when an offer was opened from
a test screen. The concept says the opposite: the context does not change on
a walk through a test screen, because nothing was chosen there. So that
write is gone — opening an offer from a test button navigates and nothing
else.

The fallback in `offerEventProps` covers both cases on its own: the stored
context is used when it is about the offer on screen and has a list,
otherwise the event carries the offer only. An offer chosen in a list and
reopened from a test button keeps its list; an offer never chosen in a list
has none.

## Emulator test, 2026-09-09

Emulator `9_Pro`, Android 17, app uninstalled first, SDK 1.30.1. Network cut
before launch, the queue file read with `run-as` after the run, then the
network turned back on and all 32 events uploaded with `status: SUCCESS`.

The run the concept's hardest case asks for is steps 2–5: a list choice, a
walk through a test screen, the same offer again, the target action.

| # | Action in the emulator | Events (in queue order) |
|---|---|---|
| 1 | cold start, land on the start screen | `session_start`, `Application Installed`, `Application Opened` — all three **without `screen_name`**; then `screen_view{Startseite, previous=session_start}`, `view_item_list{home_highlights}`, `view_item_list{home_neustarter}` |
| 2 | tap "Zum Angebot" on Fitwerk (Highlights, index 0) | `select_item{home_highlights, offer_01, index 0}`, `screen_view{Fitwerk, previous=Startseite}`, `view_item{home_highlights, offer_01, index 0}` |
| 3 | bottom nav → Test | `screen_view{Testseite 1, previous=Fitwerk}`, no `current_offer` |
| 4 | tap "Angebot 1" — the same Fitwerk | `screen_view{Fitwerk, previous=Testseite 1}`, `view_item{home_highlights, offer_01, index 0}` — **no `select_item`, and the list survived the test screen** |
| 5 | tap "Zum Shop" | `go_to_shop{code_copied=no, screen_name=Fitwerk}`, `begin_checkout{home_highlights, offer_01, index 0}`, then `Application Backgrounded{screen_name=Fitwerk}` |
| 6 | return from the browser | `screen_view{Fitwerk, previous=Fitwerk}` — the concept's loop — and **no second `view_item`**; `Application Opened{screen_name=Fitwerk}` |
| 7 | bottom nav → Home | `screen_view{Startseite, previous=Fitwerk}`, both `view_item_list` again |
| 8 | tap "Zum Angebot" on Kaffeekontor (Highlights, index 2) | `select_item{home_highlights, offer_03, index 2}`, `screen_view{Kaffeekontor, previous=Startseite}`, `view_item{home_highlights, offer_03, index 2}` |
| 9 | tap "Gutschein generieren" | `generate_code{screen_name=Kaffeekontor}` |
| 10 | tap "Kopieren" | `copy_code{screen_name=Kaffeekontor}` |
| 11 | tap "Zum Shop" | `go_to_shop{code_copied=yes}`, `begin_checkout{home_highlights, offer_03, index 2}`, `Application Backgrounded` |
| 12 | return from the browser, tap "Download" | `screen_view{Kaffeekontor, previous=Kaffeekontor}`, `Application Opened`, then `download_coupon`, `begin_checkout{home_highlights, offer_03, index 2}` |

What the run confirms:

- **The context survives a test screen.** Step 5's `begin_checkout` carries
  `home_highlights` / `Highlights` / `index 0` although the offer was
  reopened from a test button — nothing was chosen there, so nothing was
  overwritten. Step 4's `view_item` says the same.
- `previous_screen_name` follows the walk exactly:
  `session_start → Startseite → Fitwerk → Testseite 1 → Fitwerk → Startseite
  → Kaffeekontor`, with the return-to-the-same-screen loop in steps 6 and 12.
- `screen_name` from the plugin is on every event that has no name of its
  own, `Application Backgrounded` and `Application Opened` included, and on
  none of the three cold-start events.
- `code_copied` is `no` where "Kopieren" was not pressed on that visit
  (step 5) and `yes` where it was (step 11).
- Both target buttons produce their pair, button event first (steps 5, 11,
  12).
- Test screens still carry no `current_offer` (step 3).

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
  start screen". On `v1/amplitude` this is no longer hypothetical — the
  offer now survives the rotation; see Part 1, *Section 4 (e-commerce) will
  have to handle the rotation too*.
