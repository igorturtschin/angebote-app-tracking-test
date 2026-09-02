# Angebote

A small native Android app used as a test ground for mobile analytics
tracking.

It copies the shape of a real coupon service: a start screen with four
offers, and an offer screen where the user can reveal a coupon code, copy
it, download a printable coupon, or click out to the shop. That is enough
to fire the events that matter for tracking, and nothing more.

The app is not a product. It is not published on Google Play, but it is
built as a normal, installable app.

| Start screen | Offer screen |
|:---:|:---:|
| <img src="./docs/start-screen.png" width="500" alt="start"> | <img src="./docs/offer-screen.png" width="500" alt="offer"> |

## Documentation

[docs/tracking-concept.md](docs/tracking-concept.md) — the tracking
concept: SDK setup, the events and their parameters. Attachment 1
describes the app itself, screen by screen, with the technical decisions
behind it. Attachment 2 collects the open questions.

[docs/ecommerce-funnel-report.pdf](docs/ecommerce-funnel-report.pdf) — the
e-commerce funnel report: a Looker Studio dashboard built on the GA4 data
this app sends. It follows one path — the offer list is seen, an offer is
clicked, the offer screen opens, the user leaves for the shop — and
reports each step as its own e-commerce event. The numbers come from
automated emulator runs. The report itself is in German.

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

This branch is finished: the app works, and its Firebase / GA4 tracking is
implemented and checked on a device, event by event.

The app without any tracking is the common starting point for more than
one branch. Amplitude on the same functionality, and a second version of
the app with its own tracking, are planned but not started.
