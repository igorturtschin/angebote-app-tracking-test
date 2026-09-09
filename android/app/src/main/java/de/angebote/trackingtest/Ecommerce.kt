package de.angebote.trackingtest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * E-commerce events, see docs/tracking-concept.md, section "4. E-commerce
 * события": the chain view_item_list -> select_item -> view_item ->
 * begin_checkout, and the offer context that travels along it from the list to
 * the target action.
 *
 * Amplitude has no reserved e-commerce names — event and property names are
 * ours. They match GA4 (and the Firebase branch), so the two branches read the
 * same. An event name is a plain string, so the names are declared here once.
 *
 * Shape (concept, "Форма данных: массив объектов items"):
 * - list properties (item_list_id, item_list_name) sit on the event;
 * - offer properties sit inside objects of the `items` array;
 * - view_item_list carries one object per card in the list, the other three
 *   carry exactly one — the offer the event is about.
 */

private const val EVENT_VIEW_ITEM_LIST = "view_item_list"
private const val EVENT_SELECT_ITEM = "select_item"
private const val EVENT_VIEW_ITEM = "view_item"
private const val EVENT_BEGIN_CHECKOUT = "begin_checkout"

private const val PROP_ITEM_LIST_ID = "item_list_id"
private const val PROP_ITEM_LIST_NAME = "item_list_name"
private const val PROP_ITEMS = "items"

private const val ITEM_ID = "item_id"
private const val ITEM_NAME = "item_name"
private const val ITEM_BRAND = "item_brand"
private const val ITEM_COUPON = "coupon"
private const val ITEM_INDEX = "index"

/**
 * One `items` object for an offer. `index` is the position of the card inside
 * its list; it is left out when there is no list (an offer opened from a test
 * screen) — see the concept, Values. `index` is a number, the rest are strings.
 *
 * item_name and item_brand carry the same value: an offer has no name of its
 * own, the card is known by its shop (concept, Values).
 */
private fun itemObject(offer: Offer, index: Int?): Map<String, Any> = buildMap {
    put(ITEM_ID, offer.id)
    put(ITEM_NAME, offer.shop)
    put(ITEM_BRAND, offer.shop)
    put(ITEM_COUPON, offer.title)
    if (index != null) put(ITEM_INDEX, index)
}

/**
 * The source the current offer was opened from. The app holds exactly one:
 * only one offer is open at a time (concept, "attribution_context").
 *
 * - Written on select_item, with the values that went into the event.
 * - Written without list properties when the offer was opened from a test
 *   screen: there was a choice, but no list.
 * - Overwritten on every next real choice, left untouched by back-navigation,
 *   bottom-nav, and returning from the browser.
 * - Lives until it is overwritten or the process dies (a ViewModel: it
 *   survives a rotation, not a cold start).
 */
data class AttributionContext(
    val listId: String?,
    val listName: String?,
    val item: Map<String, Any>,
) {
    val itemId: String? get() = item[ITEM_ID] as? String
}

class EcommerceViewModel : ViewModel() {
    var attribution: AttributionContext? = null

    /** The opening of an offer screen the last view_item was sent for. Keeps
     *  view_item one-per-opening across a rotation, which rebuilds the screen
     *  the user never left. */
    var lastViewItemOpening: Int = -1
}

/** view_item_list for one block of the start screen. One object per card,
 *  index = position of the card inside this block. */
fun trackViewItemList(list: OfferList) {
    val items = list.offerIds.mapIndexed { index, id -> itemObject(offerById(id), index) }
    TrackingApp.amplitude.track(
        EVENT_VIEW_ITEM_LIST,
        mapOf(
            PROP_ITEM_LIST_ID to list.id,
            PROP_ITEM_LIST_NAME to list.name,
            PROP_ITEMS to items,
        ),
    )
}

/**
 * select_item for a tap on "Zum Angebot" in a card, and the matching write of
 * the attribution context. The array holds the one card that was tapped.
 */
fun selectItem(vm: EcommerceViewModel, list: OfferList, offer: Offer, index: Int) {
    val item = itemObject(offer, index)
    TrackingApp.amplitude.track(
        EVENT_SELECT_ITEM,
        mapOf(
            PROP_ITEM_LIST_ID to list.id,
            PROP_ITEM_LIST_NAME to list.name,
            PROP_ITEMS to listOf(item),
        ),
    )
    vm.attribution = AttributionContext(list.id, list.name, item)
}

/**
 * An offer opened straight from a test screen: a choice was made, no list was
 * involved. Writes the context without list properties and without `index`,
 * and sends no select_item (concept, select_item Trigger).
 */
fun selectItemFromTestScreen(vm: EcommerceViewModel, offer: Offer) {
    vm.attribution = AttributionContext(null, null, itemObject(offer, index = null))
}

/**
 * Event properties for view_item / begin_checkout. If the stored context is
 * about this offer and has a list, the event carries the list and the stored
 * object; otherwise just the offer on screen, with no list and no `index`
 * (concept, "attribution_context": the offer is always known, the list is not).
 */
private fun offerEventProps(offer: Offer, ctx: AttributionContext?): Map<String, Any> {
    return if (ctx != null && ctx.itemId == offer.id && ctx.listId != null) {
        mapOf(
            PROP_ITEM_LIST_ID to ctx.listId,
            PROP_ITEM_LIST_NAME to (ctx.listName ?: ""),
            PROP_ITEMS to listOf(ctx.item),
        )
    } else {
        mapOf(PROP_ITEMS to listOf(itemObject(offer, index = null)))
    }
}

/** begin_checkout for a tap on "Zum Shop" or "Download", sent before the side
 *  effect. Same properties as the view_item of this opening. One per tap. */
fun trackBeginCheckout(vm: EcommerceViewModel, offer: Offer) {
    TrackingApp.amplitude.track(EVENT_BEGIN_CHECKOUT, offerEventProps(offer, vm.attribution))
}

/**
 * Sends view_item once per opening of an offer screen, right after its
 * screen_view. Not sent again on a return from the browser or the background,
 * or on a rotation: the opening is the same one.
 *
 * [opening] is a counter bumped by MainActivity on every navigation into an
 * offer. A rotation recreates the composition and re-runs this effect, but the
 * counter is unchanged and the ViewModel remembers it, so nothing is sent.
 * Reopening the same offer from the list bumps the counter, so view_item goes
 * out again — which the concept asks for.
 */
@Composable
fun ViewItemEffect(offer: Offer, opening: Int) {
    val vm: EcommerceViewModel = viewModel()
    LaunchedEffect(opening) {
        if (opening != vm.lastViewItemOpening) {
            TrackingApp.amplitude.track(EVENT_VIEW_ITEM, offerEventProps(offer, vm.attribution))
            vm.lastViewItemOpening = opening
        }
    }
}
