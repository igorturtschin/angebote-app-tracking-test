package de.angebote.trackingtest

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

/**
 * Screen tracking, see docs/tracking-concept.md, block 2.
 *
 * The automatic screen_view of Firebase is switched off in the manifest.
 * Every screen sends its own event instead.
 */

/** Values of the start screen, taken from the screen table of the concept. */
const val START_SCREEN_NAME = "Startseite"
const val START_CURRENT_OFFER = "Neustarter & Highlights"

/**
 * The e-commerce event a screen sends together with its screen_view, at the
 * same ON_RESUME moment. See tracking-concept.md, block 3.
 */
sealed interface ScreenItems {
    /** Start screen: view_item_list with the whole Highlights list. */
    data object OfferList : ScreenItems

    /** Offer screen: view_item with the one open offer. */
    data class SingleOffer(val offer: Offer) : ScreenItems
}

/**
 * Sends screen_view every time the screen is resumed.
 *
 * The screens of this app are Compose state inside one Activity, so there
 * is no onResume method to override. The lifecycle observer gives the same
 * moment: it fires when the screen comes to the front, and again on every
 * return to it — back from an offer, or back from the browser.
 *
 * [screenName] is also the key of the effect. When the user opens another
 * offer, the same composable stays on screen with a new name, and the
 * effect runs again.
 */
@Composable
fun ScreenViewEffect(screenName: String, currentOffer: String, items: ScreenItems) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, screenName) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
                    param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    // SCREEN_CLASS is not sent. All screens are one Activity,
                    // so the value would say nothing about the screen.
                    param("current_offer", currentOffer)
                }
                // Same ON_RESUME, one place: the list view or the item view.
                when (items) {
                    ScreenItems.OfferList -> logViewItemList()
                    is ScreenItems.SingleOffer -> logViewItem(items.offer)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}

/**
 * e-commerce events for the Highlights offer list, see tracking-concept.md
 * block 3. Chain: view_item_list -> select_item -> view_item ->
 * begin_checkout (the target action, sent on "Zum Shop" and "Download").
 *
 * Next to the chain: four custom events, one per button on the offer
 * screen. They carry no e-commerce parameters — a custom event cannot hold
 * the items array (firebase_error 21). They are linked to the offer by the
 * screen name, which Firebase attaches to every event automatically.
 *
 * value and currency are not sent. code_copied is an event-scope parameter
 * of go_to_shop only.
 */
private const val ITEM_LIST_ID = "home_highlights"
private const val ITEM_LIST_NAME = "Highlights"

/**
 * Builds the item bundle for one offer (concept 3.2).
 *
 * [withListInfo] adds item_list_id / item_list_name inside the item. That is
 * needed for view_item and begin_checkout; view_item_list and select_item
 * carry those two on event level instead.
 */
private fun Offer.toItemBundle(withListInfo: Boolean): Bundle = Bundle().apply {
    if (withListInfo) {
        putString(FirebaseAnalytics.Param.ITEM_LIST_ID, ITEM_LIST_ID)
        putString(FirebaseAnalytics.Param.ITEM_LIST_NAME, ITEM_LIST_NAME)
    }
    putString(FirebaseAnalytics.Param.ITEM_ID, id)
    putString(FirebaseAnalytics.Param.ITEM_NAME, shop)
    putString(FirebaseAnalytics.Param.ITEM_BRAND, shop)
    putString(FirebaseAnalytics.Param.COUPON, title)
    putLong(FirebaseAnalytics.Param.INDEX, OFFERS.indexOf(this@toItemBundle).toLong())
}

fun logViewItemList() {
    val items = OFFERS.map { it.toItemBundle(withListInfo = false) }.toTypedArray()
    Firebase.analytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM_LIST) {
        param(FirebaseAnalytics.Param.ITEM_LIST_ID, ITEM_LIST_ID)
        param(FirebaseAnalytics.Param.ITEM_LIST_NAME, ITEM_LIST_NAME)
        param(FirebaseAnalytics.Param.ITEMS, items)
    }
}

fun logSelectItem(offer: Offer) {
    Firebase.analytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
        param(FirebaseAnalytics.Param.ITEM_LIST_ID, ITEM_LIST_ID)
        param(FirebaseAnalytics.Param.ITEM_LIST_NAME, ITEM_LIST_NAME)
        param(FirebaseAnalytics.Param.ITEMS, arrayOf(offer.toItemBundle(withListInfo = false)))
    }
}

private fun logOfferItemEvent(event: String, offer: Offer) {
    Firebase.analytics.logEvent(event) {
        param(FirebaseAnalytics.Param.ITEMS, arrayOf(offer.toItemBundle(withListInfo = true)))
    }
}

fun logViewItem(offer: Offer) = logOfferItemEvent(FirebaseAnalytics.Event.VIEW_ITEM, offer)

/** The target action. Sent on the tap on "Zum Shop" or "Download". */
fun logBeginCheckout(offer: Offer) = logOfferItemEvent(FirebaseAnalytics.Event.BEGIN_CHECKOUT, offer)

fun logGenerateCode() = Firebase.analytics.logEvent("generate_code") {}

fun logCopyCode() = Firebase.analytics.logEvent("copy_code") {}

fun logDownloadCoupon() = Firebase.analytics.logEvent("download_coupon") {}

/**
 * [codeCopied] is true when "Kopieren" was tapped on this offer screen visit
 * before the click-out. Same state as the button colour (used in OfferScreen),
 * it resets when the offer screen is opened again. String "yes" / "no":
 * Firebase has no boolean type.
 */
fun logGoToShop(codeCopied: Boolean) {
    Firebase.analytics.logEvent("go_to_shop") {
        param("code_copied", if (codeCopied) "yes" else "no")
    }
}
