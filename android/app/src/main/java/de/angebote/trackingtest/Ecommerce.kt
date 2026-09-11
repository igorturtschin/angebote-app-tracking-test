package de.angebote.trackingtest

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * E-commerce events, see docs/tracking-concept.md, section "4. E-commerce
 * events": the chain view_item_list -> select_item -> view_item ->
 * begin_checkout, and the offer context that travels along it from the list to
 * the target action.
 *
 * Amplitude has no reserved e-commerce names — event and property names are
 * ours. The names match GA4 (and the Firebase branch), so a reader of both
 * branches sees the same vocabulary. An event name is a plain string, so the
 * names are declared here once.
 *
 * Shape (concept, "Data shape: flat event properties"): every property —
 * list and offer alike — sits directly on the event, there is no `items`
 * array. Amplitude on this plan cannot split an array into child properties,
 * and a property it cannot split is not a dimension: no group by, no filter,
 * no hold constant. Flat properties are all of that for free.
 *
 * That is why view_item_list is one event **per card**: a flat event carries
 * exactly one item_id, so the only way to keep the per-offer breakdown of
 * impressions is one event per offer. The Firebase branch keeps the GA4
 * `items` array and one view_item_list per list; the two branches differ on
 * purpose — concept, "What the flat schema costs".
 */

private const val EVENT_VIEW_ITEM_LIST = "view_item_list"
private const val EVENT_SELECT_ITEM = "select_item"
private const val EVENT_VIEW_ITEM = "view_item"
private const val EVENT_BEGIN_CHECKOUT = "begin_checkout"

private const val PROP_ITEM_LIST_ID = "item_list_id"
private const val PROP_ITEM_LIST_NAME = "item_list_name"

private const val ITEM_ID = "item_id"
private const val ITEM_NAME = "item_name"
private const val ITEM_BRAND = "item_brand"
private const val ITEM_COUPON = "coupon"
private const val ITEM_INDEX = "index"

/**
 * The offer properties of one event. `index` is the position of the card
 * inside its list; it is left out when there is no list (an offer opened from
 * a test screen) — see the concept, Values. `index` is a number, the rest are
 * strings.
 *
 * item_name and item_brand carry the same value: an offer has no name of its
 * own, the card is known by its shop (concept, Values).
 */
private fun offerProps(offer: Offer, index: Int?): Map<String, Any> = buildMap {
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
 * - Overwritten on every next choice made in a list: another card, the same
 *   offer from another list, the same card again.
 * - Left untouched by back-navigation, bottom-nav, a walk through a test
 *   screen, and going to the browser and back: nothing was chosen there.
 * - Lives until it is overwritten or the process dies (a ViewModel: it
 *   survives a rotation, not a cold start).
 */
data class AttributionContext(
    val listId: String?,
    val listName: String?,
    val offer: Map<String, Any>,
) {
    val itemId: String? get() = offer[ITEM_ID] as? String
}

class EcommerceViewModel : ViewModel() {
    var attribution: AttributionContext? = null

    /** The opening of an offer screen the last view_item was sent for. Keeps
     *  view_item one-per-opening across a rotation, which rebuilds the screen
     *  the user never left. */
    var lastViewItemOpening: Int = -1
}

/** view_item_list for one block of the start screen: one event per card,
 *  index = position of the card inside this block. Four cards in Highlights
 *  and six in Neustarter make ten events per start-screen view. */
fun trackViewItemList(list: OfferList) {
    list.offerIds.forEachIndexed { index, id ->
        TrackingApp.amplitude.track(
            EVENT_VIEW_ITEM_LIST,
            mapOf(
                PROP_ITEM_LIST_ID to list.id,
                PROP_ITEM_LIST_NAME to list.name,
            ) + offerProps(offerById(id), index),
        )
    }
}

/**
 * select_item for a tap on "Zum Angebot" in a card, and the matching write of
 * the attribution context.
 */
fun selectItem(vm: EcommerceViewModel, list: OfferList, offer: Offer, index: Int) {
    val props = offerProps(offer, index)
    TrackingApp.amplitude.track(
        EVENT_SELECT_ITEM,
        mapOf(
            PROP_ITEM_LIST_ID to list.id,
            PROP_ITEM_LIST_NAME to list.name,
        ) + props,
    )
    vm.attribution = AttributionContext(list.id, list.name, props)
}

/**
 * Event properties for view_item / begin_checkout. If the stored context is
 * about this offer and has a list, the event carries the list and the stored
 * offer properties; otherwise just the offer on screen, with no list and no
 * `index` (concept, "attribution_context": the offer is always known, the list
 * is not).
 *
 * This is what makes a walk through a test screen harmless. Opening an offer
 * from a test screen writes nothing, so an offer chosen in a list earlier and
 * opened again from a test button still reports that list — the user chose it
 * there and never chose anywhere else. An offer that was never chosen in a
 * list has no context of its own, and the event goes out with the offer only.
 */
private fun offerEventProps(offer: Offer, ctx: AttributionContext?): Map<String, Any> {
    return if (ctx != null && ctx.itemId == offer.id && ctx.listId != null) {
        mapOf(
            PROP_ITEM_LIST_ID to ctx.listId,
            PROP_ITEM_LIST_NAME to (ctx.listName ?: ""),
        ) + ctx.offer
    } else {
        offerProps(offer, index = null)
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
