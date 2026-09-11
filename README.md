# Angebote

A small native Android app used as a test ground for mobile analytics
tracking.

It copies the shape of a real coupon service: a start screen with two
offer lists, Highlights and Neustarter, eight offers in total, and an
offer screen where the user can reveal a coupon code, copy it, download a
printable coupon, or click out to the shop. A bottom navigation and five
test screens add paths into the offers that do not go through a list.
That is enough to fire the events that matter for tracking, and nothing
more.

The app is not a product. It is not published on Google Play, but it is
built as a normal, installable app.

| Start screen | Offer screen |
|:---:|:---:|
| <img src="./docs/start-screen.png" width="500" alt="start"> | <img src="./docs/offer-screen.png" width="500" alt="offer"> |

**This branch adds Amplitude to the app.** It grows from
`v1/no-tracking`, the same app with no SDK at all, so the difference
between the two branches is exactly what adding Amplitude costs. Firebase
/ Google Analytics 4 on the same app is on the `main` branch.

## Why the e-commerce data differs from the Firebase branch

The two branches send the same event names, but not the same shape. On
`main` the offer travels inside the GA4 `items` array, and `view_item_list`
is one event per list. Here every property sits directly on the event, and
`view_item_list` is one event per card.

That is not a bug and not an oversight. GA4 breaks the `items` array into
child properties out of the box; Amplitude does it only with property
splitting, which needs a plan above the one this project runs on. Without
it, `items` is stored but cannot be grouped, filtered or held constant — so
the schema was adapted to how the platform actually counts. The price of
that choice is written down in the tracking concept, section 4, *What the flat
schema costs*.

## Documentation

[docs/tracking-concept.md](docs/tracking-concept.md) — the tracking
concept: how the SDK is set up and why, plus a full description of the app
in Attachment 1.

## Build

The project needs a Java runtime and the Android SDK. Both come with
Android Studio.

```
cd android
./gradlew assembleDebug
```

The APK is written to
`android/app/build/outputs/apk/debug/app-debug.apk` and installs on a
phone or an emulator like any other debug build.

## Language

The user interface is in German. All code, comments and documentation are
in English.

## AI

The app is built with Claude Code. The code was written by Claude, models
Sonnet 5 and Opus 5, on tasks set by the repository owner.

## Status

App v2 and its Amplitude tracking are finished. The SDK setup, the screen
events, the e-commerce chain and the four button events are built and
checked on emulators, in logcat and in Amplitude. Open questions are
collected in the tracking concept, Attachment 2.
