# Angebote

A small native Android app used as a test ground for mobile analytics
tracking.

It copies the shape of a real coupon service: a start screen with four
offers, and an offer screen where the user can reveal a coupon code, copy
it, download a printable coupon, or click out to the shop. That is enough
to fire the events that matter for tracking, and nothing more.

The app is not a product. It is not published on Google Play, but it is
built as a normal, installable app.

**This branch is the app without tracking.** It carries no analytics SDK
and sends nothing. It is the common starting point: every tracking branch
grows from here, so the difference between this branch and a tracking one
is exactly what adding that SDK costs. Firebase / Google Analytics 4 on
this app is on the `main` branch.

## Documentation

[docs/app-description.md](docs/app-description.md) — what the app does,
screen by screen, with the technical decisions behind it.

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

## Status

The app is finished for its first version and checked by hand on a phone
and on emulators. Nothing more is planned here: this branch stays the
clean starting point, and the tracking lives on its own branches.
