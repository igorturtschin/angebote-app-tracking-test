---
title: Angebote — App Description
---

# Overview

What the app does, screen by screen, and the decisions behind it. This is
the app **before any tracking**: it works, and it carries no analytics SDK.
Every tracking branch grows from here.

# Purpose

**Angebote** is a prototype. The goal is the smallest app on which mobile
tracking can be set up and measured.

The app is not a product. It is not published on Google Play, but it is
built as a normal, installable app. The user interface is in German, the
documentation is in English. The shops and offers are invented and do not
refer to real companies.

# Screens

Two screen types, 5 screens in total: one start screen and one offer
screen per offer (four offers).

![The start screen: the Highlights list with four offer cards](./start-screen.png)

![An offer screen, with the coupon code already generated](./offer-screen.png)

**Start screen.** Four offers one under another, fixed order, no sorting
and no filtering. Each offer is a card: shop, title, teaser, and a
**"Zum Angebot"** button that opens the offer screen.

**Offer screen.** A **"Zur Startseite"** button at the top (the system
back button works too). Below it, top to bottom:

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
`654-321` is the same for every offer.

# The four offers

The list is fixed and hard-coded; there is no server.

| Position | Shop | Offer | App offer id |
|---|---|---|---|
| 0 | Fitwerk | 20 % auf alle Sportschuhe | `offer_01` |
| 1 | Nordlicht Wohnen | 15 € Rabatt ab 75 € Bestellwert | `offer_02` |
| 2 | Kaffeekontor | Versandkostenfrei bestellen | `offer_03` |
| 3 | Sichtbar Optik | 2 für 1 auf Brillengläser | `offer_04` |

The start screen carries the heading **Angebote**.

# Behaviour

**A tapped button changes colour:** blue → grey. The colour resets to blue
when the offer screen is opened again.

**Navigation** only goes forward through "Zum Angebot" and back to the
start screen.

**The app has no:** login, search, search history, Merkzettel, map,
profile, settings, category list, cart.

# Coupon PDF

The **"Download"** button builds a one-page PDF inside the app: shop,
offer title, the code in large type, and a line asking the user to print
it and show it in the shop.

The file is saved to the public Downloads folder as
`gutschein-<offer-id>.pdf`, for example `gutschein-offer_01.pdf`. It can
be opened, printed or shared like any other file in Downloads.

# Known problems

**A rotation resets the app to the start screen.** With an offer screen
open, turning the device brings the app back to the list, and the open
offer is lost. Android destroys the screen on a rotation and builds it
again, and the app holds "which offer is open" in memory that is cleared
at that moment, so it starts from an empty state — the list. Reproduced on
an emulator on 2026-09-01.

The fix is small: the open offer has to be kept in the storage that
survives the rebuild. It is not done. Whoever does it should check the
tracking branches as well — a screen that survives a rotation changes when
the events of that screen are sent.

# Technical decisions

| Item | Value |
|---|---|
| App name on the phone | Angebote |
| Application ID | `de.angebote.trackingtest` |
| Language and UI | Kotlin, Jetpack Compose |
| Lowest Android | 10 (API 29) |
| Built against | API 37 |
| Analytics SDK | none on this branch |

**Why Android 10 as the lowest version.** Writing a file into the public
Downloads folder in a clean way needs API 29. Phones older than Android 10
are rare in Europe, so this is not a real limit for a test app.

**Why no SDK here.** The app was built first, without any analytics. The
point of the app is to measure what it costs to add tracking to code that
is already done. If an SDK went in with the first build, that cost could
not be seen. Each SDK therefore lives on its own branch, and this branch
stays clean.

# Status

The app is finished for its first version. It was built, installed and
checked by hand on a real Pixel 10a (Android 17) and on several emulators,
including a tablet.

Checked by hand: start screen, offer screen, code generation, the colour
change of used buttons, the PDF download into Downloads, and the click-out
into the browser.
