---
title: Angebote — Tracking Concept (Amplitude)
---

# Overview

## What this document covers

Tracking concept for the **Angebote** Android test app
(application ID `de.angebote.trackingtest`), branch `v2/amplitude`.
Analytics SDK: **Amplitude, Android-Kotlin SDK**.

This version covers the SDK setup, the app open, screens, the offer funnel
and the four buttons on the offer screen.

App v2 has a bottom navigation, eight offers, two offer lists on the start
screen and five test screens. How the app works and why each part of it is
there — [Attachment 1](#attachment-1--app-description).

## How to read this document

The document says **what** to set up and send, **with which names and
values**, **at which moment** and **why**. It does not say where in the
code to put it: the developer knows the app.

Order: section 0 — decisions made before the first event; section 1 — SDK
setup; sections 2–4 — one event per section. Attachment 1 — the app
description and the tracking values; Attachment 2 — open questions, a
working list, not a spec; Attachment 3 — reference material the developer
can do without.

The event sections all have the same structure — *Why* (why we measure
it), *Code* (SDK names and constants), *Values* (where the values come
from), *Trigger* (the moment to send, in words). Decisions you need to
understand before the code stand between *Why* and *Code*. Things the code
is clear without, but that are needed for the implementation or for the
analyst, stand after *Trigger*. Sections 0 and 1 are different: they are
not events but a sequence of steps, and the code in them is the
configuration itself.

Every section has a **Status** line under its heading (*done* / *in
progress* / *not done*). The text stays in the imperative even where the
status is *done*: the concept is not rewritten after the code exists.

Every value the tracking uses is defined once in
[Attachment 1](#attachment-1--app-description); the sections point to it
and do not copy it. The whole document works the same way: a detail is
written in one place, and the rest of the text points to it. You have to
jump between places while reading, but the information is not duplicated
and cannot drift away from a copy.

**We recommend reading in two windows.** In the first, read the document
from top to bottom, so the flow is not broken. In the second, open the
places the links point to.

Language: the document is in English, like the rest of the repository.

## The data architecture repeats the Firebase branch — on purpose

The app is measured twice: by a branch with Firebase/GA4 and by this branch
with Amplitude. The point is to compare the two systems on the same data,
so the event names, the property names and the event boundaries here are
taken from the Firebase branch, not chosen again.

In some places Amplitude works differently, and where we do it "like in
Firebase", this is a decision, not a lack of knowledge. These places are
marked where they are: section 3, *Event name*, and section 4, *Why
`begin_checkout` is the target action*.

**One intended difference — the shape of the item properties.** The item
and the list travel as flat event properties, not as an `items` array like
in GA4 and in the Firebase branch: on the plan of this Amplitude project
the array is not split. This is also why `view_item_list` is sent per
card, not per list, and why its numbers in the two branches do not match.
Why it is done this way, what it costs and when to go back to the array —
section 4, *Data shape* and *What the flat schema costs*. The difference is
described here so that a reader of both branches does not take it for a
bug.

## Version history

**1.0 — 2026-09-10.**  First version.



---

# 0. Decisions made before the first event

Status: **done.**

The three decisions below either cannot be changed later at all, or are expensive to change. Details and exact values are in section 1, step 5.

1. **The project region is the EU.** (Set up in the Amplitude UI; in the
   code — `serverZone = ServerZone.EU`.) The API key belongs to a region:
   the key of an EU project does not work in a US project, and the other
   way round. Before you put the key in, make sure it comes from the EU
   project. Data sent to the wrong region does not move back.

2. **The set of built-in fields — `trackingOptions`.** (SDK configuration.)
   According to the documentation, it only works on projects that have
   never received data; after the first event these fields can only be
   switched off through Amplitude support. A privacy decision: we switch
   off the IP address, the advertising ID and the App Set ID.

3. **Migration from the old SDK — `migrateLegacyData`.** (SDK
   configuration.) Ours is `false`: the retired `com.amplitude:android-sdk`
   was never used here. In an app that **moves** from it, the parameter
   must stay `true`. Otherwise existing users get new device IDs, show up
   in reports as new people, and the history breaks at the date of the
   update. This cannot be fixed afterwards.

---

# 1. SDK setup

Status: **done.** Built and checked with a build on 2026-09-04.

Sources: [Amplitude — Android-Kotlin SDK](https://amplitude.com/docs/sdks/analytics/android/android-kotlin-sdk)
and the setup guide that Amplitude shows inside a project after it is
created. Where the two differ, this document follows the SDK reference —
see [Attachment 3](#attachment-3--additional-information), *Differences
from the setup guide in the Amplitude UI*.

## Step 1. Amplitude project and zone

The project is created in the EU Amplitude, and the API key is taken from
that project. The zone is chosen once, before the first event (section 0):
`serverZone` in the code and the instance the project lives in must match.

## Step 2. API key

**How the key gets into the real app is the developer's decision.** This
step only describes the solution of the test app.

Here the key is kept outside the code. This is not an Amplitude
requirement: in the original guide the key is a string in the code, and
that works too.

The key is in `android/api-key.properties`; the file is in `.gitignore`
and is not committed:
```
AMPLITUDE_API_KEY=<key of the EU project>
```

No quotes, no spaces around `=`. Gradle reads the file and turns the value
into a build constant — in `android/app/build.gradle.kts`:
```kotlin
import java.util.Properties

val apiKeys = Properties().apply {
    val file = rootProject.file("api-key.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    buildFeatures { buildConfig = true }   // off by default since AGP 8

    defaultConfig {
        buildConfigField(
            "String",
            "AMPLITUDE_API_KEY",
            "\"${apiKeys.getProperty("AMPLITUDE_API_KEY", "")}\"",
        )
    }
}
```

The code reads `BuildConfig.AMPLITUDE_API_KEY`. If the file is missing, the
key is empty, but the build still passes: the events just do not arrive.
This is clearer than a build error on a fresh clone of the repository.

## Step 3. Gradle dependencies

Two dependencies: the SDK itself and the Session Replay plugin (see
step 6).

`android/gradle/libs.versions.toml`:

```toml
[versions]
amplitudeAnalytics = "1.+"
amplitudeSessionReplay = "0.20.14"

[libraries]
amplitude-analytics = { group = "com.amplitude", name = "analytics-android", version.ref = "amplitudeAnalytics" }
amplitude-session-replay = { group = "com.amplitude", name = "plugin-session-replay-android", version.ref = "amplitudeSessionReplay" }
```

`android/app/build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.amplitude.analytics)
    implementation(libs.amplitude.session.replay)
}
```

**The version is floating, as Amplitude recommends:** `1.+` is the newest
version of the first generation at build time. The range is resolved at
build time and frozen into the APK, so the plus does not update an
installed app: a new SDK version reaches people only with the next
release. The Session Replay plugin has an exact version number — it is on
major version zero, and Amplitude itself gives it as exact.

What makes a floating version risky for the data, and how that risk is
covered — Attachment 2, *Floating SDK version*.

## Step 4. INTERNET and ACCESS_NETWORK_STATE permissions, desugaring

For tracking it matters that the events are sent. As we understand it,
this needs the `INTERNET` permission, but which permissions go into the
manifest is the developer's decision. In the test app `AndroidManifest.xml`
declares both:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

**`INTERNET`.** The Android-Kotlin SDK reference says nothing about it,
while the documentation of Amplitude's old Android SDK asks you to add it
to `AndroidManifest.xml`. The Amplitude libraries do not bring it
themselves — checked in their manifests. In the Firebase branch the line
came with the Firebase library.

**`ACCESS_NETWORK_STATE` — recommended by Amplitude** (SDK reference,
section *Offline mode*). With it, the SDK notices that the connection is
back and sends the queued events at once. Without it, events go out on the
normal schedule — `flushIntervalMillis` and `flushQueueSize`. Events are
not lost in either case.

**No desugaring needed.** The SDK uses a few Java 8 APIs and needs either
desugaring or `minSdk` 21 or higher. Ours is 29 — see Attachment 1,
*Technical decisions*. There is nothing to switch on.

## Step 5. SDK initialisation

The SDK is initialised **in `Application`, before the first Activity
starts**, and gets the application context, not an Activity. This is not a
matter of style: Amplitude counts sessions and sends lifecycle events
itself, so it must see the lifecycle of the whole app. The instance is
created once per process and is available where events are sent; where
exactly it is kept is the developer's decision.

### Code
```kotlin
import com.amplitude.android.Amplitude
import com.amplitude.android.AutocaptureOption
import com.amplitude.android.TrackingOptions
import com.amplitude.core.ServerZone

val amplitude = Amplitude(BuildConfig.AMPLITUDE_API_KEY, applicationContext) {
    serverZone = ServerZone.EU

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
    newDeviceIdPerInstall = true
    migrateLegacyData = false
    enableDiagnostics = false

    flushQueueSize = 1
    flushIntervalMillis = 5000
}
```

### Values

| Parameter | Value | Why this value |
|---|---|---|
| `serverZone` | `ServerZone.EU` | German audience, the project is in the EU Amplitude. Decided before the first event — section 0. |
| `autocapture` | `SESSIONS`, `APP_LIFECYCLES`, `DEEP_LINKS` | See *Autocapture — what is on* below. |
| `enableAutocaptureRemoteConfig` | `false` | The default `true` lets the autocapture set be changed from the Amplitude UI. Then the code is no longer the truth about what the app collects: the document says one thing, the app sends another. |
| `trackingOptions` | `disableIpAddress()`, `disableAdid()`, `disableAppSetId()` | Built-in fields that the SDK adds to every event by itself. The IP is the most sensitive one, and the app does not need it. Can only be changed before the first event — section 0. |
| `useAdvertisingIdForDeviceId` | `false` | Would tie the device ID to the advertising ID: an extra Google Play module, an extra permission in the manifest and an advertising ID in an app without ads. |
| `useAppSetIdForDeviceId` | `false` | Softer than the advertising ID, but still an extra module with no gain. The SDK's own ID is enough. |
| `migrateLegacyData` | `false` | The old Amplitude SDK was never here, there is nothing to migrate. For an app that moves from the old SDK the decision is the opposite — section 0. |
| `enableDiagnostics` | `false` | Sends data about how the SDK itself works to Amplitude. It has nothing to do with our events and gives us nothing. |
| `locationListening` | `false` | The app does not need location data, and collecting it would need a permission. The default has been the same since version 1.20.7, but this is exactly the parameter whose default has already changed once — so it is written out. |
| `newDeviceIdPerInstall` | `true` | **This is intended for the test app: every install must give a new user.** The app is checked with runs, and every run starts with a reinstall; a new ID gives a clean user and a clean session, otherwise all runs merge into one person and the funnels cannot be read. A real app sets `false`: there a reinstall is the same person. |
| `flushQueueSize` | `1` | The default is 30 events. In a manual check 30 events never pile up, so the timer would always trigger the upload. `1` sends every event on its own. A real app keeps the default. |
| `flushIntervalMillis` | `5000` | The default is 30 s. The app is checked by hand on one device, and half a minute of waiting per event costs more than extra requests. A real app keeps the default. |

Five of them — `useAdvertisingIdForDeviceId`, `useAppSetIdForDeviceId`,
`locationListening`, `migrateLegacyData`, `enableDiagnostics` — switch off
what is already off. They are written out for two reasons: the code must
show that someone thought about them, and an explicit value does not
depend on what the default becomes in the next SDK version.
`newDeviceIdPerInstall` is not in this list: it is the only line in the
block that changes the SDK behaviour compared with the default (`false`).

### Autocapture — what is on

Amplitude can collect six groups of events without a single line of code.

| Option | Ours | Why |
|---|---|---|
| `SESSIONS` | **on** | Start and end of a visit, plus the session ID on every event. The base for everything counted "per visit". The only option that Amplitude switches on by default. |
| `APP_LIFECYCLES` | **on** | Install, update, open and backgrounding of the app. The "open" is what the Firebase branch had to send by hand as `app_open`; here it costs one word in the configuration. Comparing this work between the two SDKs is one of the goals of the app. |
| `DEEP_LINKS` | **on** | The app has no deep links yet, so there will be no events. On for the future: when deep links appear, they get into the data without a code change. |
| `FRUSTRATION_INTERACTIONS` | **off** | Rage clicks and dead clicks. Important, but outside the scope of the test app. |
| `SCREEN_VIEWS` | **off** | The screen name is taken from the title, the label or the class of the Activity. All 14 screens of the app live in one Activity, so the event would always report the same name and duplicate our own. The same decision, for the same reason, as switching off the automatic `screen_view` in Firebase. |
| `ELEMENT_INTERACTIONS` | **off** | Fires on every clickable element. It creates a lot of events, and Amplitude charges for them. Far from all of them are needed, and most carry data that says little. |

### Parameters we do not set

There is no line in the code — the SDK default applies. Three of them are
left at the default **on purpose**: the default suits us, but it is not
neutral in itself.

| Parameter | Default | Why it is worth a look |
|---|---|---|
| `serverUrl` | empty | **Must stay empty.** A value here overrides `serverZone`, and the EU choice silently stops working. |
| `optOut` | `false` | The master switch for data collection. The decision from a consent banner will go here once there is one. |
| `minTimeBetweenSessionsMillis` | 5 min | How long the app can stay in the background for a return to count as the same visit. The app sends people to the browser, so here the default is not a neutral decision, see Attachment 2. |

The others are not set, and there is nothing to decide in them — the list
is in Attachment 3, *SDK parameters with nothing to decide*.

### The user is the device: no `userId`, no identify

The app has no login, so `userId` is not set: there is nothing to link the
device to. A user in the data is a device; the SDK creates its `device_id`
itself, a new one per install (`newDeviceIdPerInstall`, *Values* table
above). We do not send user properties with `identify` either: everything
we measure describes a screen or an offer and travels as event properties.

## Step 6. Session Replay

The plugin is added right after the instance is created:
```kotlin
import com.amplitude.android.plugins.SessionReplayPlugin

amplitude.add(SessionReplayPlugin())
```

The plugin records what happens on the screen and shows the recording next
to the events. In this app that is safe: the offers and shops are invented,
the coupon code is the same for all, and there is no login, no search and
no text input at all — see Attachment 1. The plugin stays on.

The **sample rate** (what share of sessions is recorded) and **masking**
(what on the screen is hidden) are not decided here — see Attachment 2.

## Check that events arrive

This part is for the analyst, not for the developer.

Amplitude has no separate debug mode that is switched on from outside, like
Firebase DebugView with `adb shell setprop`. Events go into the normal
project; what changes is the speed and the level of detail in the logs.

**1. Events go out one by one.** This is what `flushQueueSize = 1` and
`flushIntervalMillis = 5000` are for. What is left in the queue can be
pushed out by hand:

```kotlin
amplitude.flush()
```

**2. SDK log in logcat.** The built-in logger writes to logcat; the level
is raised right after the instance is created:

```kotlin
amplitude.logger.logMode = Logger.LogMode.DEBUG
```

Then:

```
adb logcat -s Amplitude
```

**3. The events themselves in Amplitude.** In the project, *User Look-Up*
finds the test device and shows its events in order — the closest thing to
DebugView.

Google Play services are not needed, so a normal emulator image will do.

---

# 2. App open

Status: **done** — nothing to do in the code.

## Why

The app open marks the start of a visit: the event goes out every time the
app comes to the foreground. It is used to count how often people come
back and what they do right after coming back.

Amplitude collects this by itself: autocapture `APP_LIFECYCLES` (section 1,
step 5) gives `[Amplitude] Application Opened`, and together with it
`Application Installed`, `Updated` and `Backgrounded`. Checked on the first
launch on 2026-09-04.

So there is **no** manual equivalent of the Firebase event `app_open`
here — it would duplicate the autocaptured one. The section needs no code
and no values: the SDK sets the name, the moment and the properties of the
event, and puts `[Amplitude] From Background`, `Version` and `Build` on it.
In the Firebase branch the same thing cost a process lifecycle observer
and our own call.

## Trigger

The SDK controls the moment; we cannot influence it. What the analyst
should know about it (checked on an emulator on 2026-09-06):

- the event goes out when the app comes to the foreground — the first
  launch and every return from the background, including a return from
  the browser and from the PDF download;
- it does not go out on moves between screens inside the app;
- **it also goes out on a screen rotation**: a rotation gives the pair
  `Application Backgrounded` + `Application Opened`, although the app
  stays in the foreground the whole time. The SDK builds the event, there
  is no call of ours, and there is nowhere to add a rotation check. **This
  is not solved**; three possible workarounds and the cost of each —
  [Attachment 2](#attachment-2--open-questions), *Autocaptured
  `Application Opened`*. Our `screen_view` is not sent on a rotation
  (section 3, *Trigger*), so in the data a rotation shows up as this pair
  with no `screen_view` next to it;
- the order relative to `screen_view` is **not guaranteed**: on a cold
  start `Application Opened` comes first, on a return from the background
  it comes second, a few milliseconds later. Do not build reports on the
  assumption "first the open, then the screen".

---

# 3. Screens — `screen_view` and the screen name on every event

Status: **done.** The start screen, the offer screens, the test screens and
moves through the bottom navigation were checked on an emulator on
2026-09-06 and 2026-09-09: the events reached Amplitude with `screen_name`,
`current_offer` and `previous_screen_name`, in the expected order. The
plugin that puts `screen_name` on the other events was checked on an
emulator on 2026-09-09.

## Why

We want to know which screens are opened more often, which less, and in
which order people go through them. This is the entry point for work on
screens and the base for the offer funnel.

The app sends the event. Autocapture `SCREEN_VIEWS` is off and must not be
switched on: with one Activity for all 14 screens it would report the same
name every time — section 1, *Autocapture — what is on*.

The screen name is also needed on the other events: it links the button
events to an offer (section 4), and on `Application Backgrounded` it shows
from which screen the person left the app. Amplitude does not put it there
by itself.

So the app remembers the current screen. `previous_screen_name` for
`screen_view` comes from this memory, and our plugin — a standard
extension point of the SDK — uses it to put `screen_name` on all other
events. How this works — *Test app solution*, at the end of the section.

## Event name: our own, not from the autocapture schema

Amplitude has no separate API for screens — a screen view is a normal
event, and we choose its name. There are two options:

| Option | Event | Property |
|---|---|---|
| autocapture schema | `[Amplitude] Screen Viewed` | `[Amplitude] Screen Name` |
| our own name | `screen_view` | `screen_name` |

We take **our own name**. Names in square brackets belong to the SDK: if it
ever starts filling these fields by itself, its values and ours end up in
one definition, and nothing can tell them apart. The argument for the
autocapture schema — "manual and automatic events merge if `SCREEN_VIEWS`
is switched on" — does not work here: in this app autocapture cannot work
in principle (one Activity), it is not just postponed. The plugin marks
all other events with the same `screen_name` property (*Screen name on
every event* below) — one name for the whole project.

The cost of the decision: the event does not match the Mobile Autocapture
schema and appears in the Amplitude UI as a normal custom event. That is
exactly the intent.

## Code

```kotlin
amplitude.track(
    "screen_view",
    mapOf(
        "screen_name" to "Fitwerk",
        "previous_screen_name" to "Startseite",
        "current_offer" to "20 % auf alle Sportschuhe",
    )
)
```

The values in the example are a move from the start screen to the offer
screen of `offer_01`. Every event carries the values of its own screen, see
*Values*.

`screen_name` is set explicitly in the call, even though the plugin
(*Screen name on every event* below) would add it. For the screen event the
name is known at the moment of sending, and taking it from our own call is
more reliable.

On the test screens `screen_view` carries `screen_name` and
`previous_screen_name`, but **not `current_offer`**: these screens have no
offer. Whether it can be sent there as `null` —
[Attachment 2](#attachment-2--open-questions), *A property with no value on
some screens*.

All three properties are strings.

## Values

| Property | Where the value comes from |
|---|---|
| `screen_name` | Attachment 1 → *Tracking values*, column `screen_name` |
| `previous_screen_name` | Attachment 1 → *Tracking values*, column `screen_name` — the name of the screen the user came from. For the first screen of a session — `fixed: first_screen_in_session`, see *Previous screen* below. |
| `current_offer` | Attachment 1 → *Tracking values*, column `current_offer`. The property is not present on the test screens. |

## Trigger

Send `screen_view` every time a screen becomes visible to the user:

- when the app opens — the start screen;
- on a move to an offer screen — from a card in a list or from a button on
  a test screen;
- on a move through the bottom navigation (from any screen, including an
  offer screen): the house icon — the start screen, the "1–5" button —
  `Testseite 1`;
- on a move between test screens with the link buttons: `Testseite 1` →
  `Testseite 3` and so on — its own event for each;
- on **every return** to a screen: back from an offer, back into the app
  from the browser or from another app.

One event per visit to a screen: a return is a new event, even a short
one. The bottom navigation does not create events by itself: its button is
checked by the `screen_view` that comes from the target screen.

**Do not send:** when a screen is built but the user cannot see it yet; on
scrolling; on a screen rotation or any other redraw without a change of
screen.

A rotation is not a view: the phone rebuilds the screen, but the user did
not leave it and did not come back to it. A return to the app is a view:
the user came back on their own, and `screen_view` is needed, also when the
phone removed the app from memory while it was in the background. How to
separate returns in reports —
[Attachment 2](#attachment-2--open-questions), *`screen_view` on a return
to the app*.

## Test app solution: where `screen_name` and `previous_screen_name` come from

### What is the requirement and what is the method

This solution was made for the test app. In a real app the developers
decide how to build it. The job of the concept is that `screen_name` and
`previous_screen_name` go out with the events with the right values.

Requirement:

- **First screen of a session** — `previous_screen_name` is
  `first_screen_in_session`.
- **Return to the same screen** — `previous_screen_name` equals
  `screen_name`.
- **Session boundary events** — no `screen_name`.

The method of the test app is one field in memory and a plugin.

### What the app remembers

One field in process memory — the **current screen**. Two properties come
from it: `previous_screen_name` on `screen_view` and `screen_name` on all
other events. The previous screen does not need to be stored separately:
it is what was in this field before the move.

When a screen becomes visible, in this order, before the event is sent:

1. `previous_screen_name` of this event is what is in the "current screen".
   If the field is empty, the value is fixed: `first_screen_in_session`.
2. The "current screen" gets the name of the new screen.
3. `screen_view` goes out — both properties are ready.

The field is empty in two cases:

1. **The app started again** — the first launch, or a launch after the
   app process was ended. The field lives in process memory and is not
   saved to disk: otherwise the first screen of a new launch would get the
   previous screen from the last launch, that is, a move that never
   happened.
2. **The SDK started a new session while the app is alive** — the person
   came back after a pause longer than `minTimeBetweenSessionsMillis`
   (section 1). The app learns about the new session from the change of
   `session_id` and clears the field. We take the boundary from the SDK and
   do not count it with our own timer: otherwise our chain would drift away
   from the `session_id` that charts use to cut the data.

### Previous screen — `previous_screen_name`

**The first `screen_view` of a session** — both on a cold start and after
a return that the SDK counted as a new session — carries
`previous_screen_name` with the value `first_screen_in_session`. This is
not a screen name but an entry mark: there was no previous screen in this
session. The value is different from the name of the SDK event
`session_start` on purpose, so that the two are not mixed up in charts.

**A return to the same screen within a session** — from the background,
from the browser, from the downloaded PDF, if the person came back within
the timeout — gives `previous_screen_name` equal to `screen_name`. We keep
this loop: it means "the person came back to where they left from", and in
charts it differs from a move between different screens. A return after
the timeout is already a new session, and the rule above applies.

### Screen name on every event — the `screen_name` plugin

Amplitude does not remember which screen the user is on. Every event
carries exactly the properties that were put into it; the screen name is
not added to the following events by itself. This is a difference from
GA4, where `firebase_screen` is added to all events automatically.

So the screen name is set by a plugin. A plugin here is an extension point
of the SDK itself, not a third-party library: the path of an event before
it is sent is a pipeline of stages (`Before` → `Enrichment` →
`Destination`), and `amplitude.add(...)` inserts our object into it. Then
every event passes through its `execute` method, where `screen_name` from
the "current screen" is added to the properties.

```kotlin
class ScreenNamePlugin : Plugin {
    override lateinit var amplitude: Amplitude
    override val type = Plugin.Type.Enrichment

    override fun execute(event: BaseEvent): BaseEvent {
        val screenName = CurrentScreen.nameFor(event.sessionId) ?: return event
        val properties = event.eventProperties ?: mutableMapOf()
        if (!properties.containsKey("screen_name")) {
            properties["screen_name"] = screenName
            event.eventProperties = properties
        }
        return event
    }

    override fun onSessionIdChanged(sessionId: Long) = CurrentScreen.forget()
}
```

`CurrentScreen` is the "current screen". `nameFor` returns it only if it
was written in the same session as the event; otherwise it returns
nothing, and the event goes out without the property, not with an empty
value. `onSessionIdChanged` is called by the SDK itself when it starts a
new session — this is where the field is cleared (*What the app
remembers*, the second case).

The plugin is added right after the instance is created, in the same place
as Session Replay (section 1, step 6):

```kotlin
amplitude.add(ScreenNamePlugin())
```

**What gets the property.** Everything that happens while the person is on
a screen: app events, `Application Backgrounded`, `Deep Link Opened`,
`Application Opened` on a return within the session.

**What does not get it.** The events of a cold start and of a session
boundary: `Application Installed`, `session_start`, `session_end` and the
first `Application Opened` of a launch or of a new session. At these
moments the "current screen" is empty. No empty value is put in: the
property is simply missing. Why, and what is lost with this — *What is lost
at the session boundary* below.

The property name is our own, `screen_name`, not `[Amplitude] Screen Name`
— for the reason see *Event name: our own, not from the autocapture schema*
above.

### What is lost at the session boundary

While the session is alive, `screen_name` from the plugin is on all SDK
events, including `Application Backgrounded` — so the page the person left
the app from is recorded. At the session boundary the field is cleared,
and the events that the SDK creates at that moment — `session_end` of the
closed session, `session_start` and `Application Opened` of the new one —
go out without `screen_name`.

This is a trade-off we chose. Keeping the field alive across the boundary
would put on `session_start` and `Application Opened` of the new session
the name of the screen the person was on ten minutes ago: formally there is
a value, but in meaning it lies about the start of the new session. An
empty field is better than a wrong one. The exit page is not lost: it
stays on `Application Backgrounded` of the previous session, and that is
where to count it.

It is lost only where `Application Backgrounded` has no time to go out: a
crash, and a stop of the app from the settings.

---

# 4. E-commerce events

Status: **done.** The chain `view_item_list` → `begin_checkout` and the four
button events are built and were checked on an emulator on 2026-09-07,
2026-09-09 and — with the flat schema — on 2026-09-10
(`implementation-notes.md`, *E-commerce events* and *Flat e-commerce
properties*). The reports from the subsection *DA — setup in Amplitude*
were built on the data of the run on 2026-09-10.

## Why

We want to know what the offer lists bring. There are two questions the
whole chain `view_item_list` → `select_item` → `view_item` →
`begin_checkout` is collected for:

- **list → result:** how many times the list was shown, how many card
  clicks it got, and how many target actions happened on the offers
  opened from it;
- **offer → source:** for one offer — from which lists its target actions
  come.

The second question needs the source to travel with the offer all the way
to the target action: when the user taps "Zum Shop", they left the list
long ago. How this works — *`attribution_context`* below.

Next to the chain there are **four button events** — `generate_code`,
`copy_code`, `go_to_shop`, `download_coupon`, one for each button on the
offer screen. They answer a different question: what the person does
inside the offer and in which order — generated the code, copied it, went
to the shop, downloaded the coupon. They carry no item properties; their
offer is read from `screen_name`, which the plugin puts on every event
(section 3, *Screen name on every event*).

Amplitude collects none of this by itself, and it has no reserved
e-commerce names: all names here are ours, taken from the Firebase
branch — *Overview*, *The data architecture repeats the Firebase branch*.

## Why `begin_checkout` is the target action

In Firebase this decision had a technical reason: only reserved e-commerce
events accept the `items` array; on a custom event Firebase drops the
array with an error. So there both target taps — "Zum Shop" and
"Download" — merge into one `begin_checkout` with the item, and the buttons
send separate events without item properties.

Amplitude does not have this limit: item properties can go on an event
with any name, so `go_to_shop` and `download_coupon` could carry the item
themselves, and `begin_checkout` would not be needed. We still repeat the
Firebase schema — so that the funnels of the two branches are built from
the same events and can be compared. This is the only reason; see
*Overview*, *The data architecture repeats the Firebase branch*.

## Data shape: flat event properties

All four events of the chain have the same structure: both the list
properties — `item_list_id`, `item_list_name` — and the offer properties —
`item_id`, `item_name`, `item_brand`, `coupon`, `index` — sit **directly
on the event**. None of the four has the `items` array that is standard in
GA4 and in the Firebase branch.

`view_item_list` is sent **for every card of the list**: a flat event
carries one offer, and there is no other way to keep the per-item
breakdown of impressions. The start screen gives ten events — four for
Highlights and six for Neustarter. `select_item`, `view_item` and
`begin_checkout` have only one offer anyway, so the flat shape does not
change the number of these events.

**Why not an array — the project plan.** Amplitude accepts and stores an
array of objects, but it can split it into child fields only with
**property splitting** switched on, and that is available on plans above
`starter_v4`, the plan of this project. Without it `items` is not a
dimension: you cannot group by it, filter by it or hold it constant in a
funnel. Checked with queries on 2026-09-09: a segmentation of `view_item`
grouped by `item_id` puts all events into `(none)`, and a funnel can only
be built as "any list → any checkout". A flat property gives all of this
without a single project setting.

We do not keep an array next to the flat copies "for the future" either: a
property that is only stored but not used in analysis is not needed in the
schema, and a second source of truth drifts away from the first one over
time.

**This is done to have working reports on this plan, not because it is
more correct.** In an enterprise setup the item travels as an array of
objects and is split by splitting — this is how the Firebase branch works
and how the previous version of the app worked. When to go back to the
array — Attachment 2, *The `items` array: when to go back to it*.

**Why the list properties kept their names.** `item_list_id` /
`item_list_name` are at event level in the GA4 schema too — they just
ended up in one row with the item properties. The names are kept from the
Firebase branch.

## What the flat schema costs

Flat properties are a trade-off, not an improvement. The cost is:

1. **A difference from GA4 and from the Firebase branch.** The `items`
   array is the standard GA4 schema, and the Firebase branch stays on it. The difference is described in the *Overview* and in the branch README — otherwise a reader of both branches will think it is a bug.
2. **Ready-made Amplitude tools.** The *Purchase by Product* and *Product
   Discovery* hubs and item-level attribution expect the array; flat
   properties are invisible to them. A conscious trade: without splitting
   they would not work anyway.
3. **Five times more events on the start screen.** Ten events per view
   instead of two, on the most frequent screen of the app. For the test app
   this does not matter — the organisation quota is 2 million events a
   month. For a real app this is money. What it buys is the per-item
   breakdown of impressions: without an event per card, the table "card
   impressions → selections → checkouts" cannot be built.
4. **The meaning of `view_item_list` has shifted.** An event per card reads
   as "the card was shown", although scrolling is not tracked and the user
   may not have reached the lower cards — Attachment 2, *`view_item_list`
   and the cards really seen*.

## Test app solution: how the source reaches the target action

### What is the requirement and what is the method

This solution was made for the test app. In a real app the developers
decide how to build it. The job of the concept is that `view_item` and
`begin_checkout` go out with the right properties.

Requirement:
- **Offer properties** — always.
- **List properties** (`item_list_id`, `item_list_name`, `index`) — only if
  the last `select_item` was for this same offer. The list is taken from
  that `select_item`.
- **The list changes only with the next `select_item`.** Going back, the
  bottom navigation, a test screen and leaving for the browser do not
  change it.

The method of the test app is one `attribution_context` variable in
memory.

### What the app remembers — `attribution_context`

The app keeps **one** `attribution_context` — the source the current offer
was opened from:
```text
attribution_context = { item_list_id, item_list_name, index, offer properties }
```
One variable is enough because only one offer is open in the app at a
time.

Rules:

- **Written** on `select_item`, at the moment an offer is chosen from a
  list. It gets exactly what went into the event.
- **Overwritten** on every next real choice of an offer: another card, the
  same offer from another list, the same offer chosen again.
- **Not changed** by going back, a move through the bottom navigation, a
  walk through a test screen, leaving for the browser and coming back to
  the app: the user did not choose anything.
- **Lives** until it is overwritten or until the app restarts.

### When an event has no list

If there is no context, or it belongs to another offer — this happens after
an app restart — `view_item` and `begin_checkout` carry the properties of
the offer open on the screen and no list properties. The offer is always
known, the list is not; an event without an offer is never sent.

## Code

All blocks are filled with the values of one example: offer `offer_01`,
Fitwerk, list Highlights, `index` `0`. Every event carries the values of
its own offer and its own list, see *Values*.

Types: `index` is a number, all other properties are strings. There is no
nesting: the event is a flat set of scalars.

### `view_item_list`

One event per card. On the start screen there are ten: four for Highlights
and six for Neustarter.

```kotlin
amplitude.track(
    "view_item_list",
    mapOf(
        "item_list_id" to "home_highlights",
        "item_list_name" to "Highlights",
        "item_id" to "offer_01",
        "item_name" to "Fitwerk",
        "item_brand" to "Fitwerk",
        "coupon" to "20 % auf alle Sportschuhe",
        "index" to 0,
    )
)
```

The events for `offer_02` (`index` 1), `offer_03` (2), `offer_04` (3) and
the six events for the Neustarter list go out the same way.

### `select_item`

The properties of the tapped card. The same set of values is saved in
`attribution_context`.

```kotlin
amplitude.track(
    "select_item",
    mapOf(
        "item_list_id" to "home_highlights",
        "item_list_name" to "Highlights",
        "item_id" to "offer_01",
        "item_name" to "Fitwerk",
        "item_brand" to "Fitwerk",
        "coupon" to "20 % auf alle Sportschuhe",
        "index" to 0,
    )
)
```

### `view_item`

The same properties as in `select_item`, taken from the saved
`attribution_context`:

```kotlin
amplitude.track("view_item", /* the same map as in select_item */)
```

An offer that was opened from a test screen and was not chosen in a list
before has no list — then the event carries only the offer properties,
without `index`: the position describes a place in a list, and there was
no list. If the same offer was chosen in a list before, the list stays: a
walk through a test screen does not cancel the choice, see
*`attribution_context`*.

```kotlin
amplitude.track(
    "view_item",
    mapOf(
        "item_id" to "offer_01",
        "item_name" to "Fitwerk",
        "item_brand" to "Fitwerk",
        "coupon" to "20 % auf alle Sportschuhe",
    )
)
```

### `begin_checkout`

The same set of properties that went into the `view_item` of this opening,
from the same `attribution_context`. The event carries neither the button
nor any other property: it only says that a target action happened on this
offer.

```kotlin
amplitude.track("begin_checkout", /* the same map as in view_item */)
```

### The four button events

One event per button on the offer screen. They carry no item properties —
the offer is read from `screen_name`, which the plugin sets (section 3,
*Screen name on every event*).

| Event | Button | Property |
|---|---|---|
| `generate_code` | "Gutschein generieren" | none |
| `copy_code` | "Kopieren" | none |
| `go_to_shop` | "Zum Shop" | `code_copied` |
| `download_coupon` | "Download" | none |

```kotlin
amplitude.track("generate_code")
amplitude.track("copy_code")
amplitude.track("download_coupon")
amplitude.track("go_to_shop", mapOf("code_copied" to "yes"))
```

A tap on "Zum Shop" gives two events — `go_to_shop` and `begin_checkout`.
A tap on "Download" also gives two — `download_coupon` and
`begin_checkout`. The first says which button was tapped; the second says
that a target action happened on this offer and carries the item.

**`code_copied`** — a property of `go_to_shop` only. It answers one
question: did the person take the code with them to the shop?

The value `no` is set **every time the offer screen is opened**, and it
changes to `yes` if "Kopieren" was tapped on this visit to the screen
before the click-out. This is the same per-visit state as the colour of a
tapped button (Attachment 1, *Behaviour*).

The values are the strings `yes` / `no`, not a boolean. Amplitude would
accept a boolean, unlike Firebase, which has no boolean property type; the
strings are kept so that the value reads the same in the reports of both
branches.

## Values

| Property | Where the value comes from |
|---|---|
| `item_list_id` | Attachment 1 → *Offer lists*, column `item_list_id` |
| `item_list_name` | Attachment 1 → *Offer lists*, column `item_list_name` |
| `item_id` | Attachment 1 → *Offers and categories*, column "Offer ID" |
| `item_name` | same table, column "Shop" |
| `item_brand` | the same value as `item_name` |
| `coupon` | Attachment 1 → *Offers and categories*, column "Title (`current_offer`)" |
| `index` | Attachment 1 → *Offer lists*, position table |
| `code_copied` | `fixed: yes` / `fixed: no` — the state of the visit to the offer screen, see *The four button events* |

The events of an offer opened from a test screen have no list properties
and no `index` — see *`attribution_context`*.

## Trigger

**`view_item_list`** — when the list becomes visible: together with the
`screen_view` of the start screen, one event **per card** of each of the
two lists — ten events per view. Every view of the start screen counts —
the first one, a return from an offer, a move through the bottom
navigation, a return to the app from the browser.

Do not send: on scrolling to the second block (all ten events have already
gone out), on a screen rotation, on a redraw without a change of screen.
The event says that the card was in the list on the open screen, not that
the user scrolled down to it — Attachment 2, *`view_item_list` and the
cards really seen*.

**`select_item`** — a tap on **"Zum Angebot"** in a card. One event per
tap, with the offer and the list of the card that was tapped.

Do not send: on a move to an offer with a button on a test screen (there
is no list).

**`view_item`** — right after the `screen_view` of the offer screen,
exactly one per opening of the screen. Send it on any entry to the offer
screen, including an entry from a test screen.

Do not send: on a return from the browser, on a return to the app, on a
system dialog over the screen. In these cases `screen_view` goes out again
(section 3), but `view_item` does not: it is the same opening. On a screen
rotation even `screen_view` does not go out, so `view_item` does not
either: the screen is rebuilt, but the user did not leave it.

A second tap on the same card in the list is not a return but a new
opening: again `select_item`, `screen_view` and `view_item`.

**`begin_checkout`** — a tap on **"Zum Shop"** or **"Download"**, at the
moment of the tap and **before** the side effect. The browser may fail to
open and the file may fail to save — that is technology, not user
behaviour. One event per tap: two taps on "Zum Shop" in a row give two
events.

Do not send: on **"Gutschein generieren"** and **"Kopieren"** — with them
the user stays on the offer; on the result of the action — the opened
browser, the saved file.

**The four button events** — at the moment of the tap on their button and
**before** the side effect, exactly like `begin_checkout`. One event per
tap: two taps on "Kopieren" in a row give two `copy_code`.

Do not send: on the result of the action — the shown code, the copied
text, the opened browser, the saved file; on a repeated view of the screen
if the button was not tapped.

## DA — setup in Amplitude

This part is for the analyst, not for the developer. Status: **done**
(checked on 2026-09-10, `implementation-notes.md`).

There is nothing to set up in the project. Amplitude creates the properties
by itself as soon as the first event arrives, and a flat property works as
a dimension out of the box: group by, filters and hold constant are
available on it without a single setting. Property splitting does not need
to be switched on and cannot be — the `starter_v4` plan does not have it
(*Data shape*).

Check on the first events that arrive:

1. **Funnel `view_item_list` → `select_item` → `view_item` →
   `begin_checkout` with hold constant on `item_id`.** The answer to
   "offer → source" from *Why* depends on it.
2. **Group by `item_id` and by `item_list_id`** on each of the four events —
   values, not `(none)`.
3. **Table "card impressions → selections → checkouts"** broken down by
   `item_id`. The schema was made flat for this table.
4. **`screen_name` on the four button events.** It comes from the plugin
   (section 3, *Screen name on every event*), and without it these events
   have no link to an offer.


---

# Attachment 1 — App description

## Purpose

**Angebote** is a prototype. The goal: the smallest app on which mobile
tracking can be set up and measured.

The app is not a product. It is not published on Google Play, but it is
built as a normal installable app. The user interface is in German. The
shops and offers are invented and do not refer to real companies.

## Screens

Three screen types, 14 screens in total: the start screen, one screen for
each of the eight offers, and five test screens. The bottom navigation is
visible on all of them — see *Behaviour*.

| Start screen | Offer screen |
|:---:|:---:|
| <img src="./start-screen.png" width="500" alt="start screen"> | <img src="./offer-screen.png" width="500" alt="offer screen"> |

The offer screen is shown after "Gutschein generieren" and "Kopieren" were
tapped: the code is visible, and both buttons are grey.

**Start screen.** The screen title is **Angebote**. Two offer blocks, one
under the other — to measure the tracking of two lists. The order is
fixed, with no sorting and no filters:

- **Highlights** — the first block, four offers;
- **Neustarter** — the second block, six offers.

The names `Highlights` and `Neustarter` are visible on the screen as block
subtitles. Each offer is a card: shop, title, teaser and a
**"Zum Angebot"** button that opens the offer screen. For testing, Fitwerk
and Kaffeekontor are in both blocks — see *Tracking values*, *Offer
lists*.

**Offer screen.** Eight screens, one per offer, all built the same way.
Content from top to bottom:

| Element | Behaviour |
|---|---|
| Offer text | 2–3 paragraphs with the terms. Static. |
| Button **"Gutschein generieren"** | Shows the code `654-321` and a "Kopieren" button below it. |
| Button **"Kopieren"** | Copies the code to the clipboard, shows a short message. |
| Button **"Zum Shop"** | Click-out. Opens `https://www.google.de` in the external browser. |
| Text **"Oder Gutschein für die Filiale herunterladen"** | Static. |
| Button **"Download"** | Builds a PDF coupon and saves it to the Downloads folder (see *Coupon PDF*). |

All three actions — code, click-out, download — are available on the offer
screen at the same time. There are no different offer types. The code
`654-321` is the same for all offers.

**Test screens.** Five screens `Testseite 1` … `Testseite 5`; they differ
from each other only by name (`screen_name`). Each of them has:

- five link buttons **"Seite 1"** … **"Seite 5"** — a move to the matching
  test screen;
- eight buttons **"Angebot 1"** … **"Angebot 8"** — a move straight to the
  screen of the matching offer, without a list.

The test screens are there for testing path and funnel charts. Moves
between them build a chain of screens; a button into an offer gives a path
into an offer that has no list context. The point is to have, in one
project, both list-linked and unlinked paths to the same offers. A test
screen does not overwrite the list context: if the offer was chosen in a
list before, the list is kept — section 4, *`attribution_context`*.

## Behaviour

**A tapped button changes colour:** blue → grey. The colour goes back to
blue when the offer screen is opened again.

**Bottom navigation.** A bar at the bottom of the screen with two buttons:
the house icon leads to the start screen, **"1–5"** to `Testseite 1`. It is
shown on all screens, including the offer screen.

**Navigation.** Forward — with "Zum Angebot" in a card, with the buttons
"Angebot 1" … "Angebot 8" on a test screen and with the bottom navigation
(house icon, "1–5"). Between test screens — with the link buttons
"Seite 1" … "Seite 5". From any screen to the start screen — with the
house icon in the bottom navigation or with the system back button; the
offer screen has no back button of its own.

**The app has no:** login, search, search history, Merkzettel, map,
profile, settings, cart. There is no category screen either — the offers
are just tagged with a category in the data.

## Coupon PDF

The **"Download"** button builds a one-page PDF inside the app: shop, offer
title, the code in large type, and a line asking the user to print it and
show it in the shop.

The file is saved to the public Downloads folder as
`gutschein-<offer-id>.pdf`, for example `gutschein-offer_01.pdf`. It can be
opened, printed or shared like any other file in Downloads.

## Tracking values

### Screens and `current_offer`

| Screen (`screen_name`) | Type | `current_offer` |
|---|---|---|
| `Startseite` | start | `Neustarter & Highlights` |
| `Fitwerk` | offer | `20 % auf alle Sportschuhe` |
| `Nordlicht Wohnen` | offer | `15 € Rabatt ab 75 € Bestellwert` |
| `Kaffeekontor` | offer | `Versandkostenfrei bestellen` |
| `Sichtbar Optik` | offer | `2 für 1 auf Brillengläser` |
| `Fadenwerk` | offer | `30 % auf die Herbstkollektion` |
| `Polstermanufaktur` | offer | `10 % auf alle Polstermöbel` |
| `Hofkiste` | offer | `Bio-Gemüsekiste zum Kennenlernpreis` |
| `Klarblick` | offer | `Kostenlose Sehanalyse und 20 % auf Sonnenbrillen` |
| `Testseite 1` … `Testseite 5` | test screen | — the property is not sent |

### Offers and categories

| Offer ID | Shop | Title (`current_offer`) | Category |
|---|---|---|---|
| `offer_01` | Fitwerk | `20 % auf alle Sportschuhe` | Bekleidung |
| `offer_02` | Nordlicht Wohnen | `15 € Rabatt ab 75 € Bestellwert` | Möbel |
| `offer_03` | Kaffeekontor | `Versandkostenfrei bestellen` | Lebensmittel |
| `offer_04` | Sichtbar Optik | `2 für 1 auf Brillengläser` | Optik |
| `offer_05` | Fadenwerk | `30 % auf die Herbstkollektion` | Bekleidung |
| `offer_06` | Polstermanufaktur | `10 % auf alle Polstermöbel` | Möbel |
| `offer_07` | Hofkiste | `Bio-Gemüsekiste zum Kennenlernpreis` | Lebensmittel |
| `offer_08` | Klarblick | `Kostenlose Sehanalyse und 20 % auf Sonnenbrillen` | Optik |

Four categories, two offers in each. The category is not sent with the
events: the `item_category` property is not part of section 4 yet.

### Offer lists

Two lists, both on the start screen. `item_list_id` / `item_list_name` are
properties of the section 4 events.

| List | `item_list_id` | `item_list_name` | Block |
|---|---|---|---|
| Highlights | `home_highlights` | `Highlights` | first |
| Neustarter | `home_neustarter` | `Neustarter` | second |

Content and position (`index`, from zero) — the place of the card inside
its own block:

| Offer | in Highlights | in Neustarter |
|---|---|---|
| `offer_01` Fitwerk | 0 | 4 |
| `offer_02` Nordlicht Wohnen | 1 | — |
| `offer_03` Kaffeekontor | 2 | 5 |
| `offer_04` Sichtbar Optik | 3 | — |
| `offer_05` Fadenwerk | — | 0 |
| `offer_06` Polstermanufaktur | — | 1 |
| `offer_07` Hofkiste | — | 2 |
| `offer_08` Klarblick | — | 3 |

### How to read the tables

- The values are hard-coded in the app, there is no server.
- On an offer screen `screen_name` is the same as the shop name. All eight
  names are different.
- `current_offer` on an offer screen is the title of that offer; on the
  start screen it is `Neustarter & Highlights`, which covers both blocks.
  It exists only in tracking: on the screen the blocks have separate
  subtitles, and the screen title is **Angebote**.
- Fitwerk (`offer_01`) and Kaffeekontor (`offer_03`) are in both lists,
  with a different position in each. In the section 4 events the position
  goes out as the `index` property.

## Technical decisions

| Item | Value |
|---|---|
| App name on the phone | Angebote |
| Application ID | `de.angebote.trackingtest` |
| Language and UI | Kotlin, Jetpack Compose |
| Lowest Android | 10 (API 29) |
| Built against | API 37 |
| Analytics SDK | Amplitude (on this branch) |

**Why Android 10 as the lowest version.** Writing a file into the public
Downloads folder in a clean way needs API 29. Phones older than Android 10
are rare in Europe, so for a test app this is not a real limit.

## App status

**v1 was checked by hand** on a real Pixel 10a (Android 17) and on several
emulators, including a tablet: start screen, offer screen, code
generation, the colour change of tapped buttons, the PDF download into
Downloads, the click-out.

**v2 is built** in the code of the `v2/amplitude` branch: bottom
navigation, eight offers in four categories, two blocks on the start
screen and five test screens. What was checked in tracking and when — the
status line of each section and `implementation-notes.md`.


---

# Attachment 2 — Open questions

## Floating SDK version: the risk and how it is covered

A floating version means that the set of collected data can, in theory,
change without a single change in the code — if a new version changes a
default or adds an option that is on by default.

What covers this:
- **`autocapture` is set as a complete set.** It is an allowlist, not a
  set of flags: what is not in the set is not collected — including
  autocapture options that do not exist yet today.
- **`enableAutocaptureRemoteConfig = false`.** Closes the second way the
  autocapture set could change without our knowledge — from the Amplitude
  UI, even without a new SDK version.
- **`locationListening = false` and `newDeviceIdPerInstall = true`.** Two
  parameters where a change of the default changes the collected data at
  once: location data, and whether a reinstall counts as a new user. Both
  are set explicitly, each with its own value: location data is not
  needed, and a new user per install is needed in the test app —
  section 1, step 5, *Values*.
- **`trackingOptions` with three `disable*` calls.** The IP, the
  advertising ID and the App Set ID are switched off explicitly, and after
  the first event they cannot change anyway.

What is **not** covered: a top-level parameter that appears in a future
version and is on by default. You cannot write down today what does not
exist yet. The only protection is to read the changelog when the SDK
version is raised; this is an item on the development checklist, not a
line in the code.

**Talk to Amplitude before launch.** Confirm that new collection options
are not switched on in existing apps by themselves, and that changes of
defaults are announced in the changelog. The chance of the opposite is
close to zero — the known precedent goes the other way: `locationListening`
was switched off by default in 1.20.7 — but it has to be discussed: the
cost of a mistake is data collected without a basis.

## `trackingOptions`: the other fields

The IP address, the advertising ID and the App Set ID are switched off.
The other built-in fields — carrier, city, country, region, device model
and manufacturer, language, OS version and more — go out as they are for
now. Which of them a real app should send is a privacy question, and it
has not been asked yet.

Separately: switching off the IP makes city and region useless — Amplitude
derives them from the IP. Checked on the first data on 2026-09-06: on all
events `city` and `region` are empty, while `country` is filled. The
country is apparently not taken from the IP; where exactly it comes from
is not clear yet, and until it is, it is better not to rely on it.

## Session Replay: sample rate, masking and its own remote config

`SessionReplayPlugin()` is added without arguments, so it runs on the
plugin defaults. The first launch on 2026-09-04 showed in the log what
they really are, and that raised three questions.

**The sample rate is 1 %, and the check session was not recorded.** The
plugin starts with `sampleRate: 0.0`, then fetches `sampleRate: 0.01` from
the server and writes to the log: `Opting session … out of recording due
to sample rate`. So Session Replay is on now, but it records one session
in a hundred — a random one. For a manual check this means there may be no
recording at all. Decide: keep 1 % or raise it for the time of the check.

**Masking — `maskLevel: MEDIUM`.** What falls under this level and what
does not has not been read yet.

**The plugin has its own remote config, and it is on.** The log shows
`enableRemoteConfig: true` and `configSource: remote` — the plugin fetches
its recording settings from Amplitude at every start. Our
`enableAutocaptureRemoteConfig = false` does not affect it: that is a
setting of event autocapture, not of the plugin. So the sample rate and
masking are now controlled from outside, not by the code — exactly what we
did not want under the principle "the code must be the truth about what
the app collects". Find out whether this can be switched off, and decide.

## Autocaptured `Application Opened`: rotation and event order

Found on 2026-09-06 while debugging `screen_view` on an emulator. Both
facts concern section 2 and cannot be fixed with a setting — only with a
plugin or by giving up autocapture.

Our `screen_view` has survived a rotation since 2026-09-06 — section 3,
*Trigger*. `Application Opened` below is a separate story: the SDK builds
this event, and it still fires on a rotation.

**A screen rotation gives an extra app open.** Every rotation of the
device gives the pair `[Amplitude] Application Backgrounded` +
`[Amplitude] Application Opened`, although the app stays in the foreground
the whole time. Autocapture is derived from the Activity lifecycle, and a
rotation recreates the Activity. The Firebase branch did not have this:
there `app_open` was sent by hand from an observer of the **process**
lifecycle, and a rotation does not recreate the process.

What this does to the numbers: the more often people rotate the screen,
the more app opens there are. An "open" stops meaning a "visit".

Options, none of them free:

- accept it and do not build visit metrics on this event — count visits by
  `session_id`, which a rotation does not reset (checked: all events
  before and after the rotation stayed in one session);
- switch off `APP_LIFECYCLES` and send our own open event from a process
  observer, as in Firebase — then the code comes back that autocapture was
  switched on to avoid;
- filter out the pair in an enrichment plugin — the plugin sees the event
  before it is sent, but it still has to learn from somewhere that "this
  is a rotation, not an open".

**The order with `screen_view` is not guaranteed.** Checked in three
scenarios:

| Scenario | Order |
|---|---|
| cold start | `Application Opened` → `screen_view` |
| screen rotation | `Backgrounded` → `Opened`; no `screen_view` goes out (section 3, *Trigger*) |
| return from the background | `screen_view` → `Application Opened` |

The gap in the last case is a few milliseconds: our `ON_RESUME` observer
gets there before the SDK reaches its Activity callback. The SDK sets the
order; the app cannot influence it.

Practical conclusion: a funnel that starts with `Application Opened` and
continues with `screen_view` will lose part of the returns. If such a
funnel is needed, build it on the session, not on the pair of events.

## `screen_view` on a return to the app

When a person comes back to a screen from the browser, from the PDF
download or from another app, the screen is in focus again and
`screen_view` goes out a second time. Formally this is not a view but a
return.

The decision of section 3 is to **send it**. After a long time in the
background Amplitude starts a new session, and without `screen_view` that
session would have no entry screen; besides, the numbers stay comparable
with the Firebase branch, where the decision is the same. The inflated
screen count is handled by marking, not by dropping the event.

Not decided:

1. **How to mark it.** A return `screen_view` needs a marker to exclude it
   from mass metrics — screen counts and screen depth, where it inflates
   the numbers. The marker partly exists: on a return within a session
   `previous_screen_name` equals `screen_name` (section 3, *Previous
   screen*) — inside the app a person does not move to the same screen,
   but on a return they stay on it. Such a filter can be built in any
   chart. A return after the session timeout does not fall into this
   filter: it opens a new session and looks the same as a cold start.
   Whether it needs to be caught too is not decided yet.
2. **How to use it.** After a return the person goes on through the app or
   closes it. This is a separate question, and the return event is its
   starting point.

A related question is `minTimeBetweenSessionsMillis` and the click-out,
below: it decides whether a return falls into the same session.

## `minTimeBetweenSessionsMillis` and the click-out

The default is 5 minutes: come back later, and Amplitude starts a new
session. The app sends the user to the browser and to the PDF in
Downloads, and the point of the funnel is what they do after coming back.
Someone who read the shop page for six minutes comes back in a new
session, and the funnel breaks in the middle.

Open: keep the default and accept this, or raise the value. Raising it
hides real breaks between visits, so it is a trade-off, not a solution.
The Firebase branch has the same question in another form — `screen_view`
on a return from the background.

## A property with no value on some screens: leave it out or `null`

Example — `current_offer`: the test screens have no offer, and now the
property is not sent there (section 3, *Code*).

It may be simpler for the developer to send `screen_view` on all screens
with one and the same call and one set of properties. Then a property with
no value needs to get something, and there are three options: leave it
out, an empty string `""`, or `null`.

**Do not send an empty string.** It is unknown whether Amplitude treats
`""` as the same missing value as a property that was not sent. If not,
the reports get two kinds of "no value", and a filter on one of them
silently loses the other.

**`null` is the option for such a call.** It says directly that there is no
value here and that this is on purpose — like `null` in JavaScript: the
value is missing on purpose, not forgotten.

Check before allowing `null`:

1. Does `null` reach Amplitude, or does the SDK drop such a property
   already on the device?
2. How does Amplitude show `null` in charts? According to the documentation
   the `(none)` group is the null value, so, presumably, `null` and a
   property that was not sent look the same in reports. Then the intent is
   visible only in the code, and the data does not change with the choice.
3. Where does `""` go: into `(none)` or as a separate value?

## `view_item_list` and the cards really seen

The events go out for all cards at once, together with the view of the
start screen. The event says that the card was in the list on the open
screen, not that the user scrolled down to it: the second block,
Neustarter, is not fully visible when the screen opens. This gives too
many impressions for the lower positions and a CTR that is too low.

The right solution for an enterprise setup or for Firebase is not covered in this document — what we have is enough at this stage.

## The `items` array: when to go back to it

The array of objects was removed from the schema on 2026-09-10 (section 4,
*Data shape*), but the question is not closed forever: on a plan with
property splitting the array becomes a working shape again, and with it
the *Purchase by Product* and *Product Discovery* hubs and item-level
attribution come back.

What this needs: a plan above `starter_v4` and property splitting switched
on for the `items` property. Splitting cannot be switched on through MCP —
the taxonomy API has no such tool, only the UI has it.

What to check **before** going back, not after: whether hold constant
works on a child property. The Amplitude documentation describes hold
constant only for a normal event property instrumented on every funnel
step, and says nothing about child properties; about arrays it only says
that they are available in Event Segmentation and Funnel Analysis and are
shown in property lists with the `{:}` mark. With the flat schema this
question does not arise: hold constant on `item_id` is a normal event
property.

Going back to the array also means that `view_item_list` goes back to one
event per list, which breaks the data series for this event. So this is a
release decision, not a quiet change.


---

# Attachment 3 — Additional information

## SDK parameters with nothing to decide

There is no line in the code — the SDK default applies. The three
parameters whose default is not neutral are discussed in section 1,
step 5, *Parameters we do not set*.

- **the default is fine as it is** — `deviceId`, `flushEventsOnClose`,
  `callback`, `httpClient`, `useBatch`, `offline`, `storageProvider`,
  `identifyInterceptStorageProvider`, `identityStorageProvider`,
  `loggerProvider`, `enableRequestBodyCompression`,
  `enableCoppaControl`, `instanceName`;
- **not our case** — `minIdLength`, `partnerId`,
  `identifyBatchIntervalMillis`, `ingestionMetadata`, `sessionId`, `plan`,
  `interactionsOptions`;
- **deprecated** — `flushMaxRetries`, `trackingSessionEvents`,
  `defaultTracking`.

## Differences from the setup guide in the Amplitude UI

The setup guide that Amplitude shows inside a project after it is created
works, but it is built differently. This document follows the SDK
reference. There are three differences:

1. **The constructor form.** The SDK reference documents the form with a
   lambda — `Amplitude(apiKey, context) { … }`, so section 1, step 5 uses
   it. The form `Configuration(apiKey = …, context = …)` from the guide
   works too.
2. **`defaultTracking = DefaultTrackingOptions.ALL`.** In the SDK reference
   `defaultTracking` is marked as deprecated and replaced by `autocapture`.
   Code with it compiles and works, but `ALL` would also switch on screen
   views, which we do not need.
3. **The key as a string in the code.** Ours comes from
   `BuildConfig.AMPLITUDE_API_KEY` — this is our decision, see section 1,
   step 2.
