package de.angebote.trackingtest

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

/** Blue = not tapped yet, grey = already tapped. */
private val BLUE = Color(0xFF1A56DB)
private val GREY = Color(0xFF9E9E9E)

private const val SHOP_URL = "https://www.google.de"

/** Number of test screens ("Testseite 1" … "Testseite 5"). */
private const val TEST_PAGES = 5

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                    App()
                }
            }
        }
    }
}

/** The two entries of the bottom navigation. */
private enum class Tab { HOME, TEST }

@Composable
private fun App() {
    // Navigation state, all in rememberSaveable so it survives an Activity
    // rebuild (rotation, or the system reclaiming the app from the background)
    // instead of dropping back to the start screen.
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var testPage by rememberSaveable { mutableIntStateOf(1) }
    // The open offer is held as an id, not the object, so it fits in a Bundle.
    var openOfferId by rememberSaveable { mutableStateOf<String?>(null) }
    // Bumped on every navigation into an offer screen. It tells one opening
    // from the next, so view_item stays one-per-opening across a rotation
    // (Ecommerce.kt, ViewItemEffect).
    var opening by rememberSaveable { mutableIntStateOf(0) }

    val ecommerce: EcommerceViewModel = viewModel()

    val current = openOfferId?.let { id -> OFFERS.firstOrNull { it.id == id } }

    // System back: from an offer, back to whatever screen opened it; from a
    // test screen, back to the start screen instead of leaving the app.
    if (current != null) {
        BackHandler { openOfferId = null }
    } else {
        BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = current == null && tab == Tab.HOME,
                    onClick = {
                        openOfferId = null
                        tab = Tab.HOME
                    },
                    icon = { Icon(imageVector = HomeIcon, contentDescription = "Home") },
                    label = { Text("Home") },
                )
                NavigationBarItem(
                    selected = current == null && tab == Tab.TEST,
                    onClick = {
                        openOfferId = null
                        tab = Tab.TEST
                        testPage = 1
                    },
                    icon = { Text("1–5", fontWeight = FontWeight.SemiBold) },
                    label = { Text("Test") },
                )
            }
        },
    ) { insets ->
        Box(Modifier.padding(insets)) {
            when {
                current != null -> OfferScreen(offer = current, opening = opening)
                tab == Tab.HOME -> StartScreen(
                    onOfferClick = { offer, list, index ->
                        selectItem(ecommerce, list, offer, index)
                        openOfferId = offer.id
                        opening++
                    },
                )
                else -> TestScreen(
                    page = testPage,
                    onPageClick = { testPage = it },
                    onOfferClick = { id ->
                        selectItemFromTestScreen(ecommerce, offerById(id))
                        openOfferId = id
                        opening++
                    },
                )
            }
        }
    }
}

@Composable
private fun StartScreen(onOfferClick: (offer: Offer, list: OfferList, index: Int) -> Unit) {
    ScreenViewEffect(
        screenName = START_SCREEN_NAME,
        currentOffer = START_CURRENT_OFFER,
        // The list is on the screen again whenever the start screen is, so
        // view_item_list is reported again — one event per block.
        onLogged = { OFFER_LISTS.forEach { trackViewItemList(it) } },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Angebote",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
        )

        OFFER_LISTS.forEach { list ->
            OfferBlock(list = list, onOfferClick = onOfferClick)
        }
    }
}

@Composable
private fun OfferBlock(
    list: OfferList,
    onOfferClick: (offer: Offer, list: OfferList, index: Int) -> Unit,
) {
    Text(
        text = list.name,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
    )
    list.offerIds.forEachIndexed { index, id ->
        val offer = offerById(id)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = offer.shop + " · " + offer.category,
                    fontSize = 13.sp,
                    color = GREY,
                )
                Text(text = offer.title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text(text = offer.teaser, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                ColorButton(
                    text = "Zum Angebot",
                    used = false,
                    onClick = { onOfferClick(offer, list, index) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/**
 * One of the five test screens. They differ only by name (screen_name) and
 * carry no offer data. Used to build path and funnel charts and to open an
 * offer without a list context. See the concept, Attachment 1, "Тест-страницы".
 */
@Composable
private fun TestScreen(
    page: Int,
    onPageClick: (Int) -> Unit,
    onOfferClick: (String) -> Unit,
) {
    ScreenViewEffect(
        screenName = testScreenName(page),
        currentOffer = null,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = testScreenName(page),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )

        Text(text = "Seiten", fontSize = 13.sp, color = GREY)
        (1..TEST_PAGES).forEach { n ->
            ColorButton(
                text = "Seite $n",
                used = n == page,
                onClick = { onPageClick(n) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(text = "Angebote", fontSize = 13.sp, color = GREY)
        OFFERS.forEachIndexed { index, offer ->
            ColorButton(
                text = "Angebot ${index + 1}",
                used = false,
                onClick = { onOfferClick(offer.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OfferScreen(offer: Offer, opening: Int) {
    ScreenViewEffect(
        screenName = offer.shop,
        currentOffer = offer.title,
    )
    // Right after the screen_view, once per opening of this screen.
    ViewItemEffect(offer = offer, opening = opening)

    val context = LocalContext.current
    val ecommerce: EcommerceViewModel = viewModel()

    // Button state. The offer.id key resets it when another offer is opened;
    // rememberSaveable also keeps it across an Activity rebuild (rotation).
    val used = rememberSaveable(
        offer.id,
        saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { mutableStateListOf<String>() }
    var codeVisible by rememberSaveable(offer.id) { mutableStateOf(false) }

    fun markUsed(name: String) {
        if (name !in used) used.add(name)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = offer.shop + " · " + offer.category, fontSize = 13.sp, color = GREY)
        Text(text = offer.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        offer.details.forEach { paragraph ->
            Text(text = paragraph, fontSize = 14.sp)
        }

        Spacer(Modifier.height(8.dp))

        ColorButton(
            text = "Gutschein generieren",
            used = "generieren" in used,
            onClick = {
                markUsed("generieren")
                codeVisible = true
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (codeVisible) {
            Text(
                text = offer.code,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            ColorButton(
                text = "Kopieren",
                used = "kopieren" in used,
                onClick = {
                    markUsed("kopieren")
                    copyToClipboard(context, offer.code)
                    Toast.makeText(context, "Code kopiert", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        ColorButton(
            text = "Zum Shop",
            used = "shop" in used,
            onClick = {
                markUsed("shop")
                trackBeginCheckout(ecommerce, offer)
                openShop(context)
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Oder Gutschein für die Filiale herunterladen",
            fontSize = 14.sp,
        )

        ColorButton(
            text = "Download",
            used = "download" in used,
            onClick = {
                markUsed("download")
                trackBeginCheckout(ecommerce, offer)
                val name = downloadCouponPdf(context, offer)
                val message = if (name != null) {
                    "Gespeichert unter Downloads/" + name
                } else {
                    "Download fehlgeschlagen"
                }
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ColorButton(
    text: String,
    used: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (used) GREY else BLUE,
            contentColor = Color.White,
        ),
    ) {
        Text(text)
    }
}

/** A small house, drawn in code so the app needs no icon dependency. */
private val HomeIcon: ImageVector = ImageVector.Builder(
    name = "home",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(12f, 3.2f)
        lineTo(2.5f, 11f)
        lineTo(5f, 11f)
        lineTo(5f, 20.5f)
        lineTo(10f, 20.5f)
        lineTo(10f, 14.5f)
        lineTo(14f, 14.5f)
        lineTo(14f, 20.5f)
        lineTo(19f, 20.5f)
        lineTo(19f, 11f)
        lineTo(21.5f, 11f)
        close()
    }
}.build()

private fun copyToClipboard(context: Context, code: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("Gutscheincode", code))
}

private fun openShop(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHOP_URL))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Kein Browser gefunden", Toast.LENGTH_SHORT).show()
    }
}
