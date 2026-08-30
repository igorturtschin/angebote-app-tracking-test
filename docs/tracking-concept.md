# Angebote — tracking concept

Tracking concept for the **Angebote** Android test app
(application ID `de.angebote.trackingtest`).

This document is the single source of truth for tracking: the SDK setup,
the events, their parameters and the screen list. A developer can build
the whole tracking from this document alone. The app itself — screens,
buttons, and where each value comes from — is described in
[Appendix A: the app](#appendix-a--the-app).

Analytics SDK: **Firebase / Google Analytics for Firebase**.

The document grows block by block. Block 1 is the SDK, block 2 is
`screen_view`. Interaction events (coupon code, click-out, PDF download)
come in later blocks.

---

## 1. Analytics SDK setup

Source: [Firebase — Get started with Google Analytics on Android](https://firebase.google.com/docs/analytics/android/get-started).

Status: **done.** The steps below are written down so the setup can be
repeated or reviewed.

### 1.1 Firebase project and app registration

1. Create the Firebase project (here: `angebote-app-tracking-test`).
2. Keep **Enable Google Analytics** switched on in the wizard. It creates
   the linked GA4 property. The Firebase console and the GA4 interface are
   two views on the same data.
3. Register the Android app with the package name
   `de.angebote.trackingtest` — it must be the same as `applicationId` in
   `app/build.gradle.kts`.
4. Download `google-services.json` and put it into
   `android/app/google-services.json`.

The file `google-services.json` is not a secret: the same file is shipped
inside the APK. It is kept in the repository so the project builds out of
the box.

### 1.2 Gradle

`android/gradle/libs.versions.toml`:

```toml
[versions]
googleServices = "4.5.0"
firebaseBom = "34.18.0"

[libraries]
firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-analytics = { group = "com.google.firebase", name = "firebase-analytics" }

[plugins]
google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

`android/build.gradle.kts` (project level):

```kotlin
plugins {
    alias(libs.plugins.google.services) apply false
}
```

`android/app/build.gradle.kts` (module level):

```kotlin
plugins {
    alias(libs.plugins.google.services)
}

dependencies {
    // The BoM sets one version for all Firebase libraries.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
}
```

The BoM (Bill of Materials) fixes one matching version set for all
Firebase libraries, so `firebase-analytics` carries no version of its own.

After the change: **Sync Project with Gradle Files** in Android Studio.
The first sync downloads Gradle, AGP and the Firebase dependencies and
takes 5–20 minutes; later syncs take seconds.

### 1.3 Initialise Analytics in code

The SDK starts itself through a manifest entry that comes with the
library, so automatic events (`first_open`, `session_start`,
`user_engagement`, …) are collected without any Kotlin code. To send our
own events, the singleton is used:

```kotlin
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

Firebase.analytics.logEvent(...)
```

The Firebase guide keeps the instance in a field of the Activity
(`private lateinit var firebaseAnalytics`). This app does not: the screens
are composables, not Activities, so the events are sent from
`Analytics.kt` and `Firebase.analytics` is read there. It returns the same
single instance.

### 1.4 Check that events arrive

DebugView shows events from one device in near real time, without the
normal batching delay. Standard reports in Firebase and GA4 are delayed
by several hours up to a day.

```
adb shell setprop debug.firebase.analytics.app de.angebote.trackingtest
adb shell am force-stop de.angebote.trackingtest
```

The flag is read at the next app start, so the app has to be stopped
once. Then start the app and open **Firebase Console → Analytics →
DebugView** and pick the device at the top.

Switch the debug mode off again with:

```
adb shell setprop debug.firebase.analytics.app .none.
```

An emulator needs a **system image with Google Play**. Without Google
Play services the SDK cannot send events.

[AI INFO]
`adb` is not in PATH on the user's machine; it is in
`%LOCALAPPDATA%\Android\Sdk\platform-tools\`. Two devices are usually
connected (emulator and a phone over Wi-Fi), so the real commands need
`-s emulator-5554`. The full PowerShell versions are in
`infos/firebase-sdk-setup-log.md`. Kept out of the main text because this
document is written for any developer, not for one machine.
[/AI INFO]

---

## 2. Screen tracking — `screen_view`

### 2.1 Automatic screen tracking is switched off

Firebase sends its own `screen_view` from the Activity lifecycle. This app
draws all 5 screens inside one Activity, so the automatic event would
always report the same screen and would fire next to our own event —
duplicate and useless data.

Therefore: **automatic screen reporting is off, every screen sends its own
`screen_view` manually.** This is also what Google recommends when the app
controls the screen names itself.

Add inside `<application>` in `android/app/src/main/AndroidManifest.xml`:

```xml
<meta-data
    android:name="google_analytics_automatic_screen_reporting_enabled"
    android:value="false" />
```

**What this switches off:** only the automatic sending of `screen_view`.
All other automatic events and all manual events keep working.

### 2.2 Screen list

The app has **5 screens** of **2 types**:

| Type | Count | Description |
|---|---|---|
| Start screen | 1 | Shows all four offers as blocks. |
| Offer screen | 4 | One screen per offer. Opened with the button **"Zum Angebot"**. |

All screens live inside one Activity (`MainActivity`) and are built with
Jetpack Compose. There are no Fragments and no Navigation component: a
"screen" is a Compose state, not a separate Android component.

#### Screen table

| Screen | Type | `screen_name` | `screen_class` | `current_offer` | App offer id |
|---|---|---|---|---|---|
| Start | Start screen | `Startseite` | *(see 2.3)* | `Neustarter & Highlights` | — |
| Fitwerk | Offer screen | `Fitwerk` | *(see 2.3)* | `20 % auf alle Sportschuhe` | `offer_01` |
| Nordlicht Wohnen | Offer screen | `Nordlicht Wohnen` | *(see 2.3)* | `15 € Rabatt ab 75 € Bestellwert` | `offer_02` |
| Kaffeekontor | Offer screen | `Kaffeekontor` | *(see 2.3)* | `Versandkostenfrei bestellen` | `offer_03` |
| Sichtbar Optik | Offer screen | `Sichtbar Optik` | *(see 2.3)* | `2 für 1 auf Brillengläser` | `offer_04` |

The **app offer id** (`offer_01` …) is the id of the offer in the app code
(`Offer.id` in `Offers.kt`). It is used whenever an offer has to be
identified in a later event.

### 2.3 The event

| Parameter | Constant | Example |
|---|---|---|
| Screen name | `FirebaseAnalytics.Param.SCREEN_NAME` | `Fitwerk` |
| Screen class | `FirebaseAnalytics.Param.SCREEN_CLASS` | *optional, see below* |
| Current offer | `current_offer` (custom) | `20 % auf alle Sportschuhe` |

```kotlin
firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
    param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
    // param(FirebaseAnalytics.Param.SCREEN_CLASS, "...")
    param("current_offer", currentOffer)
}
```

`SCREEN_CLASS` is commented out on purpose. The developer either fills it
with a correct value or does not send it at all. What arrives in this
parameter says more about the technical quality of the app than about user
behaviour, so it is not needed for the analysis.

### 2.4 When it is sent: `onResume`

**The `screen_view` event is sent from the `onResume` moment of the
screen** — not from `onCreate` or the first composition.

- `onResume` is the moment the screen is fully in front and ready for
  input. It is the closest technical signal to "the user is looking at
  this screen now".
- It fires **again** on every return to the screen: back from an offer
  screen to the start screen, and back into the app after the click-out to
  the browser or after the PDF download. `onCreate` and the first
  composition fire only once and would miss these returns, so screen
  counts would be too low.
- It is the same lifecycle moment that Firebase's own automatic screen
  tracking uses, so the numbers stay comparable.

Trade-off: a very short return also produces a `screen_view`. That is
expected and is the normal Android behaviour.

### 2.5 Implementation

The screens are Compose state inside one Activity, so there is no
per-screen `onResume` method to override. A lifecycle observer gives the
same moment. The helper lives in
`android/app/src/main/java/de/angebote/trackingtest/Analytics.kt`:

```kotlin
@Composable
fun ScreenViewEffect(screenName: String, currentOffer: String) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, screenName) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    param("current_offer", currentOffer)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
```

- `StartScreen` calls it with `START_SCREEN_NAME` and
  `START_CURRENT_OFFER` (the two constants in `Analytics.kt`).
- `OfferScreen` calls it with `offer.shop` and `offer.title`.
- `screenName` is the key of the effect. Opening another offer keeps the
  same composable on screen with a new name, so the effect runs again and
  a new `screen_view` is sent.
- `LocalLifecycleOwner` needs the dependency
  `androidx.lifecycle:lifecycle-runtime-compose`.

---

## 3. Later — open notes

[AI INFO]
Section 3 is a parking area for the user. Not reworked on purpose: these
points are the next step and will be picked up after blocks 1 and 2 are
agreed.
[/AI INFO]

### Known bug

Open issue in `firebase-android-sdk`: after the flag is set to `false`,
events can disappear from **DebugView** until the app is reinstalled.
Fix: uninstall and install the app again (or clear app data) before
checking DebugView. This only affects the debug tool, not the data that
is collected.

### How to validate

1. Uninstall the app from the device / emulator (because of the known
   bug above).
2. Build and install the debug build.
3. Enable DebugView:
   `adb shell setprop debug.firebase.analytics.app de.angebote.trackingtest`
4. In Firebase DebugView, check for each screen:
   - exactly **one** `screen_view` per screen visit (no duplicate from
     automatic tracking),
   - correct `screen_name` (`Startseite`, `Fitwerk`, …),
   - `current_offer` present,
   - a new `screen_view` after going back to the start screen and after
     returning from the browser click-out.

---

# Appendix A — the app

This appendix describes the app itself: what is on the screens, what the
buttons do, and where the values used for tracking come from. It is
reference material for the tracking blocks above.

An Android app used as a test ground for mobile tracking. It is not a
product. It will not be published on Google Play, but it is built as a
normal, installable app.

The user interface is in German, because the project is shown to a
German-speaking audience. All documentation is in English.

## A.1 Screens

The app has two screen types: one start screen and one offer screen.
There are 5 screens in total — the start screen plus one per offer. The
screen list with all tracking values is in section 2.2.

### Start screen

Four offers are built into the app. There is no server. Each offer is one
block with a shop name, a title, a short text and a button
**"Zum Angebot"** that opens the offer screen.

The order is fixed. There is no sorting and no filtering.

The shops and the offers are invented. They do not refer to real
companies.

### Offer screen

At the top there is a button back to the start screen. The system back
button works as well.

Below it, from top to bottom:

| Element | Behaviour |
|---|---|
| Offer text | Two or three paragraphs with the terms of the offer. Static. |
| Button **"Gutschein generieren"** | Shows the code `654-321` and the button "Kopieren" below it. |
| Button **"Kopieren"** | Copies the code to the clipboard. A short message confirms it. |
| Button **"Zum Shop"** | Click-out. Opens `https://www.google.de` in the external browser. |
| Text **"Oder Gutschein für die Filiale herunterladen"** | Static. |
| Button **"Download"** | Creates a PDF coupon and saves it to the Downloads folder. |

All three actions — code, click-out and download — are on the same offer
screen at the same time. There are no different offer types.

The code `654-321` is the same for every offer.

## A.2 General behaviour

**A button that was tapped changes colour.** Normal is blue, already
tapped is grey. The colour goes back to blue when the offer screen is
opened again.

**Navigation** works only forward through "Zum Angebot" and back to the
start screen.

## A.3 What the app does not have

No login, no search, no search history, no Merkzettel, no map, no
profile, no settings, no category list, no shopping cart.

## A.4 The coupon PDF

The "Download" button builds a one-page PDF inside the app. The page
shows the shop name, the offer title and the code in large type, with a
line asking the user to print it and show it in the shop.

The file is saved to the public Downloads folder as
`gutschein-<offer-id>.pdf`, for example `gutschein-offer_01.pdf`. The user
can open it, print it or share it like any other download.

## A.5 Technical decisions

| Item | Value |
|---|---|
| App name on the phone | Angebote |
| Application ID | `de.angebote.trackingtest` |
| Language and UI toolkit | Kotlin, Jetpack Compose |
| Lowest supported Android | 10 (API 29) |
| Built against | API 37 |
| Analytics SDK | Firebase / Google Analytics for Firebase (see block 1) |

**Why Android 10 as the lowest version.** Saving a file into the public
Downloads folder in a clean way needs API 29. Phones older than Android
10 are rare in Europe, so this is not a real limit for a test app.

**Why the SDK came later.** The app was first built without any analytics
SDK. The point of this app is to measure how much effort each tracking
change costs *after* the app already exists. If the SDK had gone in
together with the first build, that effort could not be seen.

## A.6 State

The app was built, installed and used on two devices:

- emulator Pixel 10a, Android 17
- a real Pixel 10a, Android 17

Checked by hand: start screen, offer screen, code generation, the colour
change of used buttons, the PDF download into the Downloads folder, and
the click-out into the browser.
