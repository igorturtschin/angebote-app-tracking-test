package de.angebote.trackingtest

/** One offer. All data is hard-coded in the app, there is no server. */
data class Offer(
    val id: String,
    val shop: String,
    val title: String,
    /** German category shown on the card. Not sent to Amplitude yet; it
     *  becomes item_category in section 4 of the tracking concept. */
    val category: String,
    val teaser: String,
    val details: List<String>,
    val code: String,
)

/**
 * A block of offers on the start screen. `id` / `name` are item_list_id /
 * item_list_name for section 4 of the concept; they are not sent yet.
 * An offer id may appear in more than one list (Fitwerk and Kaffeekontor do).
 */
data class OfferList(
    val id: String,
    val name: String,
    val offerIds: List<String>,
)

val OFFERS = listOf(
    Offer(
        id = "offer_01",
        shop = "Fitwerk",
        title = "20 % auf alle Sportschuhe",
        category = "Bekleidung",
        teaser = "Bei Fitwerk sparen Sie 20 % auf das gesamte Schuhsortiment. Gültig auch auf reduzierte Ware.",
        details = listOf(
            "Mit diesem Gutschein erhalten Sie 20 % Rabatt auf alle Sportschuhe im Fitwerk Online-Shop. Der Rabatt gilt für Damen-, Herren- und Kindermodelle sowie für bereits reduzierte Artikel.",
            "Der Gutschein ist einmal pro Kundenkonto einlösbar und nicht mit anderen Aktionen kombinierbar. Eine Barauszahlung ist ausgeschlossen.",
            "Aktionszeitraum: bis zum Ende des laufenden Monats. Der Rabatt wird im Warenkorb nach Eingabe des Codes abgezogen.",
        ),
        code = "654-321",
    ),
    Offer(
        id = "offer_02",
        shop = "Nordlicht Wohnen",
        title = "15 € Rabatt ab 75 € Bestellwert",
        category = "Möbel",
        teaser = "Nordlicht Wohnen gewährt 15 € Nachlass auf Möbel und Wohnaccessoires ab einem Bestellwert von 75 €.",
        details = listOf(
            "Ab einem Mindestbestellwert von 75 € ziehen Sie mit diesem Code 15 € vom Gesamtpreis ab. Der Gutschein gilt für das komplette Sortiment an Möbeln, Textilien und Wohnaccessoires.",
            "Ausgenommen sind Geschenkgutscheine und Artikel von Fremdanbietern. Versandkosten werden bei der Berechnung des Mindestbestellwerts nicht berücksichtigt.",
            "Pro Bestellung kann nur ein Gutschein eingelöst werden. Bei einer Retoure verfällt der Rabattanteil anteilig.",
        ),
        code = "654-321",
    ),
    Offer(
        id = "offer_03",
        shop = "Kaffeekontor",
        title = "Versandkostenfrei bestellen",
        category = "Lebensmittel",
        teaser = "Kaffeekontor liefert alle Bestellungen ohne Versandkosten – ohne Mindestbestellwert.",
        details = listOf(
            "Mit diesem Code entfallen die Versandkosten für Ihre gesamte Bestellung. Es gibt keinen Mindestbestellwert, der Vorteil gilt bereits ab der ersten Packung Kaffee.",
            "Die Lieferung erfolgt innerhalb Deutschlands. Für Expresslieferungen und Sperrgut gilt die Aktion nicht.",
            "Der Code ist mehrfach einlösbar, solange die Aktion läuft.",
        ),
        code = "654-321",
    ),
    Offer(
        id = "offer_04",
        shop = "Sichtbar Optik",
        title = "2 für 1 auf Brillengläser",
        category = "Optik",
        teaser = "Sichtbar Optik: beim Kauf einer Brille erhalten Sie das zweite Glaspaar gratis dazu.",
        details = listOf(
            "Beim Kauf einer Korrektionsbrille erhalten Sie ein zweites Paar Gläser Ihrer Wahl kostenlos. Das Angebot gilt für Einstärken- und Gleitsichtgläser.",
            "Beide Glaspaare müssen auf denselben Namen ausgestellt sein. Die Anfertigung erfolgt nach Ihren aktuellen Sehwerten.",
            "Der Gutschein ist auch in den teilnehmenden Filialen gültig. Bringen Sie dafür den ausgedruckten Gutschein mit.",
        ),
        code = "654-321",
    ),
    Offer(
        id = "offer_05",
        shop = "Fadenwerk",
        title = "30 % auf die Herbstkollektion",
        category = "Bekleidung",
        teaser = "Fadenwerk reduziert die komplette Herbstkollektion um 30 % – Damen und Herren.",
        details = listOf(
            "Mit diesem Gutschein erhalten Sie 30 % Rabatt auf alle Teile der aktuellen Herbstkollektion. Der Rabatt gilt für Damen- und Herrenmode, nicht für Basics und Accessoires.",
            "Der Code ist einmal pro Kundenkonto einlösbar und nicht mit anderen Gutscheinen kombinierbar. Eine Barauszahlung ist ausgeschlossen.",
            "Aktionszeitraum: bis zum Ende des laufenden Monats. Der Rabatt wird im Warenkorb nach Eingabe des Codes abgezogen.",
        ),
        code = "654-321",
    ),
    Offer(
        id = "offer_06",
        shop = "Polstermanufaktur",
        title = "10 % auf alle Polstermöbel",
        category = "Möbel",
        teaser = "Die Polstermanufaktur gibt 10 % Rabatt auf Sofas, Sessel und Betten aus eigener Fertigung.",
        details = listOf(
            "Mit diesem Code sparen Sie 10 % auf alle Polstermöbel: Sofas, Sessel, Hocker und Polsterbetten. Der Rabatt gilt auch auf individuell konfigurierte Stücke.",
            "Ausgenommen sind Auslieferung und Montage vor Ort. Der Gutschein ist pro Bestellung einmal einlösbar.",
            "Die Lieferzeit für gepolsterte Möbel beträgt in der Regel sechs bis acht Wochen. Der Rabatt bleibt bei Teillieferungen erhalten.",
        ),
        code = "654-321",
    ),
    Offer(
        id = "offer_07",
        shop = "Hofkiste",
        title = "Bio-Gemüsekiste zum Kennenlernpreis",
        category = "Lebensmittel",
        teaser = "Hofkiste liefert die erste Bio-Gemüsekiste zum halben Preis – regionale Ware der Saison.",
        details = listOf(
            "Neukundinnen und Neukunden erhalten die erste Gemüsekiste mit diesem Code zum halben Preis. Der Inhalt richtet sich nach der Saison und wird wöchentlich zusammengestellt.",
            "Die Lieferung erfolgt in mehreren Regionen. Ein Abo ist nicht nötig, die erste Kiste kann einzeln bestellt werden.",
            "Der Code ist einmal pro Haushalt gültig. Eine Barauszahlung ist ausgeschlossen.",
        ),
        code = "654-321",
    ),
    Offer(
        id = "offer_08",
        shop = "Klarblick",
        title = "Kostenlose Sehanalyse und 20 % auf Sonnenbrillen",
        category = "Optik",
        teaser = "Klarblick: kostenlose Sehanalyse in der Filiale und 20 % auf alle Sonnenbrillen.",
        details = listOf(
            "Mit diesem Gutschein ist die ausführliche Sehanalyse in jeder Klarblick-Filiale kostenlos. Zusätzlich erhalten Sie 20 % Rabatt auf alle Sonnenbrillen mit und ohne Sehstärke.",
            "Die Sehanalyse dauert etwa 30 Minuten. Eine Terminvereinbarung wird empfohlen, ist aber nicht zwingend.",
            "Der Rabatt auf Sonnenbrillen ist nicht mit anderen Aktionen kombinierbar und einmal pro Person einlösbar.",
        ),
        code = "654-321",
    ),
)

/** First block on the start screen: four offers. */
val HIGHLIGHTS = OfferList(
    id = "home_highlights",
    name = "Highlights",
    offerIds = listOf("offer_01", "offer_02", "offer_03", "offer_04"),
)

/** Second block on the start screen: the four new offers plus Fitwerk and
 *  Kaffeekontor, which therefore stand in both lists. */
val NEUSTARTER = OfferList(
    id = "home_neustarter",
    name = "Neustarter",
    offerIds = listOf("offer_05", "offer_06", "offer_07", "offer_08", "offer_01", "offer_03"),
)

val OFFER_LISTS = listOf(HIGHLIGHTS, NEUSTARTER)

fun offerById(id: String): Offer = OFFERS.first { it.id == id }
