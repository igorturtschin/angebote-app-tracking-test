package de.angebote.trackingtest

/** One offer. All data is hard-coded in the app, there is no server. */
data class Offer(
    val id: String,
    val shop: String,
    val title: String,
    val teaser: String,
    val details: List<String>,
    val code: String,
)

val OFFERS = listOf(
    Offer(
        id = "offer_01",
        shop = "Fitwerk",
        title = "20 % auf alle Sportschuhe",
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
        teaser = "Sichtbar Optik: beim Kauf einer Brille erhalten Sie das zweite Glaspaar gratis dazu.",
        details = listOf(
            "Beim Kauf einer Korrektionsbrille erhalten Sie ein zweites Paar Gläser Ihrer Wahl kostenlos. Das Angebot gilt für Einstärken- und Gleitsichtgläser.",
            "Beide Glaspaare müssen auf denselben Namen ausgestellt sein. Die Anfertigung erfolgt nach Ihren aktuellen Sehwerten.",
            "Der Gutschein ist auch in den teilnehmenden Filialen gültig. Bringen Sie dafür den ausgedruckten Gutschein mit.",
        ),
        code = "654-321",
    ),
)
