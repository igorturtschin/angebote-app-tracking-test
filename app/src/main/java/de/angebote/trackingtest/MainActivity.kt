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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Blau = noch nicht angetippt, Grau = bereits angetippt. */
private val BLAU = Color(0xFF1A56DB)
private val GRAU = Color(0xFF9E9E9E)

private const val SHOP_URL = "https://www.google.de"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    App()
                }
            }
        }
    }
}

@Composable
private fun App() {
    var offeneOffer by remember { mutableStateOf<Offer?>(null) }

    val current = offeneOffer
    if (current == null) {
        StartScreen(onOfferClick = { offeneOffer = it })
    } else {
        OfferScreen(
            offer = current,
            onBack = { offeneOffer = null },
        )
        BackHandler { offeneOffer = null }
    }
}

@Composable
private fun StartScreen(onOfferClick: (Offer) -> Unit) {
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

        OFFERS.forEach { offer ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = offer.shop, fontSize = 13.sp, color = GRAU)
                    Text(text = offer.title, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = offer.teaser, fontSize = 14.sp)
                    Spacer(Modifier.height(4.dp))
                    FarbButton(
                        text = "Zum Angebot",
                        benutzt = false,
                        onClick = { onOfferClick(offer) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun OfferScreen(offer: Offer, onBack: () -> Unit) {
    val context = LocalContext.current

    // Zustand der Schaltflaechen. remember(offer.id) => beim erneuten Oeffnen zurueckgesetzt.
    val benutzt = remember(offer.id) { mutableStateListOf<String>() }
    var codeSichtbar by remember(offer.id) { mutableStateOf(false) }

    fun markiere(name: String) {
        if (name !in benutzt) benutzt.add(name)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = onBack) {
            Text("Zur Startseite")
        }

        Text(text = offer.shop, fontSize = 13.sp, color = GRAU)
        Text(text = offer.title, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        offer.details.forEach { absatz ->
            Text(text = absatz, fontSize = 14.sp)
        }

        Spacer(Modifier.height(8.dp))

        FarbButton(
            text = "Gutschein generieren",
            benutzt = "generieren" in benutzt,
            onClick = {
                markiere("generieren")
                codeSichtbar = true
            },
            modifier = Modifier.fillMaxWidth(),
        )

        if (codeSichtbar) {
            Text(
                text = offer.code,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
            FarbButton(
                text = "Kopieren",
                benutzt = "kopieren" in benutzt,
                onClick = {
                    markiere("kopieren")
                    kopiereInZwischenablage(context, offer.code)
                    Toast.makeText(context, "Code kopiert", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        FarbButton(
            text = "Zum Shop",
            benutzt = "shop" in benutzt,
            onClick = {
                markiere("shop")
                oeffneShop(context)
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Oder Gutschein für die Filiale herunterladen",
            fontSize = 14.sp,
        )

        FarbButton(
            text = "Download",
            benutzt = "download" in benutzt,
            onClick = {
                markiere("download")
                val name = downloadGutscheinPdf(context, offer)
                val meldung = if (name != null) {
                    "Gespeichert unter Downloads/" + name
                } else {
                    "Download fehlgeschlagen"
                }
                Toast.makeText(context, meldung, Toast.LENGTH_LONG).show()
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun FarbButton(
    text: String,
    benutzt: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (benutzt) GRAU else BLAU,
            contentColor = Color.White,
        ),
    ) {
        Text(text)
    }
}

private fun kopiereInZwischenablage(context: Context, code: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText("Gutscheincode", code))
}

private fun oeffneShop(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(SHOP_URL))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Kein Browser gefunden", Toast.LENGTH_SHORT).show()
    }
}
