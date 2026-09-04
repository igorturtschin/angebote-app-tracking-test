# Angebote

A small native Android app used as a test ground for mobile analytics
tracking.

It copies the shape of a real coupon service: a start screen with four
offers, and an offer screen where the user can reveal a coupon code, copy
it, download a printable coupon, or click out to the shop. That is enough
to fire the events that matter for tracking, and nothing more.

The app is not a product. It is not published on Google Play, but it is
built as a normal, installable app.

**This branch adds Amplitude to the app.** It grows from
`v1/no-tracking`, the same app with no SDK at all, so the difference
between the two branches is exactly what adding Amplitude costs. Firebase
/ Google Analytics 4 on the same app is on the `main` branch.

## Documentation

[docs/tracking-concept.md](docs/tracking-concept.md) — the tracking
concept: how the SDK is set up and why, plus a full description of the app
in Attachment 1. Written in Russian for now; it gets translated later.

[docs/branches-and-tags.md](docs/branches-and-tags.md) — which branch and
tag holds which variant of the app.

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

The app is finished for its first version and checked by hand on a phone
and on emulators. The Amplitude SDK is installed and sends the automatic
app lifecycle events; the events of the app itself come next.
