# Angebote

A small native Android app used as a test ground for mobile analytics
tracking.

It copies the shape of a real coupon service: a start screen with four
offers, and an offer screen where the user can reveal a coupon code, copy
it, download a printable coupon, or click out to the shop. That is enough
to fire the events that matter for tracking, and nothing more.

The app is not a product. It is not published on Google Play, but it is
built as a normal, installable app.

No analytics SDK is included yet. SDKs are added in a separate, later
step, so that the effort of adding tracking to an app that already exists
can be measured.

## Layout

| Folder | Content |
|---|---|
| `android/` | the app itself — a Gradle project, Kotlin and Jetpack Compose |
| `docs/` | documentation |

## Build

The project needs a Java runtime and the Android SDK. Both come with
Android Studio.

```
cd android
./gradlew assembleDebug
```

The APK is written to
`android/app/build/outputs/apk/debug/app-debug.apk`.

## Documentation

[docs/app-spec.md](docs/app-spec.md) — what the app contains, screen by
screen, and the technical decisions behind it.

[CLAUDE.md](CLAUDE.md) — working rules for this folder: language, commit
style, build, and what not to do.

## Language

The user interface is in German. All code, comments and documentation are
in English.

## Status

Work in progress. The app runs on an emulator and on a real phone. The
README and the rest of the project will grow as the work goes on.
