package de.angebote.trackingtest

/**
 * The four button events of the offer screen, see docs/tracking-concept.md,
 * section "4. E-commerce events", "The four button events".
 *
 * One event per button. They carry no product properties: which offer the tap
 * happened on is read from screen_name, which the plugin of section 3
 * ("Screen name on every event") puts on every event. Each is sent at the moment of the tap and before the
 * side effect — a browser that fails to open or a file that fails to save is
 * technology, not behaviour.
 *
 * "Zum Shop" and "Download" also send begin_checkout (Ecommerce.kt): the
 * button event says which button was pressed, begin_checkout says the target
 * action happened on this offer and carries the item.
 */

private const val EVENT_GENERATE_CODE = "generate_code"
private const val EVENT_COPY_CODE = "copy_code"
private const val EVENT_GO_TO_SHOP = "go_to_shop"
private const val EVENT_DOWNLOAD_COUPON = "download_coupon"

private const val PROP_CODE_COPIED = "code_copied"

/** "Gutschein generieren". */
fun trackGenerateCode() {
    TrackingApp.amplitude.track(EVENT_GENERATE_CODE)
}

/** "Kopieren". */
fun trackCopyCode() {
    TrackingApp.amplitude.track(EVENT_COPY_CODE)
}

/**
 * "Zum Shop". [codeCopied] answers one question: did the person take the code
 * with them into the shop. It is the state of this visit to the offer screen —
 * the same state as the colour of a pressed button — and is reset every time
 * the screen is opened.
 *
 * The value is the string "yes" / "no", not a boolean. Amplitude would take a
 * boolean; Firebase has no boolean property type, and the two branches should
 * read the same in reports.
 */
fun trackGoToShop(codeCopied: Boolean) {
    TrackingApp.amplitude.track(
        EVENT_GO_TO_SHOP,
        mapOf(PROP_CODE_COPIED to if (codeCopied) "yes" else "no"),
    )
}

/** "Download". */
fun trackDownloadCoupon() {
    TrackingApp.amplitude.track(EVENT_DOWNLOAD_COUPON)
}
