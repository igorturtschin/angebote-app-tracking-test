# Angebote — test app specification

An Android app used as a test ground for mobile tracking. It is not a
product. It will not be published on Google Play, but it is built as a
normal, installable app.

The user interface is in German, because the project is shown to a
German-speaking audience. All documentation is in English.

## Screens

The app has two screens. Nothing more is needed at this stage.

### 1. Start screen

Four offers are built into the app. There is no server. Each offer is one
block with a shop name, a title, a short text and a button
**"Zum Angebot"** that opens the offer screen.

The order is fixed. There is no sorting and no filtering.

| # | Shop | Title |
|---|---|---|
| 1 | Fitwerk | 20 % auf alle Sportschuhe |
| 2 | Nordlicht Wohnen | 15 € Rabatt ab 75 € Bestellwert |
| 3 | Kaffeekontor | Versandkostenfrei bestellen |
| 4 | Sichtbar Optik | 2 für 1 auf Brillengläser |

The shops and the offers are invented. They do not refer to real
companies.

### 2. Offer screen

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

## General behaviour

**A button that was tapped changes colour.** Normal is blue, already
tapped is grey. The colour goes back to blue when the offer screen is
opened again.

**Navigation** works only forward through "Zum Angebot" and back to the
start screen.

## What the app does not have

No login, no search, no search history, no Merkzettel, no map, no
profile, no settings, no category list, no shopping cart.

## The coupon PDF

The "Download" button builds a one-page PDF inside the app. The page
shows the shop name, the offer title and the code in large type, with a
line asking the user to print it and show it in the shop.

The file is saved to the public Downloads folder as
`gutschein-<offer-id>.pdf`, for example `gutschein-offer_01.pdf`. The user
can open it, print it or share it like any other download.

## Technical decisions

| Item | Value |
|---|---|
| App name on the phone | Angebote |
| Application ID | `de.angebote.trackingtest` |
| Language and UI toolkit | Kotlin, Jetpack Compose |
| Lowest supported Android | 10 (API 29) |
| Built against | API 37 |
| Analytics SDK | none yet |

**Why Android 10 as the lowest version.** Saving a file into the public
Downloads folder in a clean way needs API 29. Phones older than Android
10 are rare in Europe, so this is not a real limit for a test app.

**Why no analytics SDK yet.** The SDKs are added in a separate, later
step. The point of this app is to measure how much effort each change
costs *after* the app already exists. If the SDK went in together with
the first build, that effort could not be seen.

## State

The app was built, installed and used on two devices:

- emulator Pixel 10a, Android 17
- a real Pixel 10a, Android 17

Checked by hand: start screen, offer screen, code generation, the colour
change of used buttons, the PDF download into the Downloads folder, and
the click-out into the browser.
