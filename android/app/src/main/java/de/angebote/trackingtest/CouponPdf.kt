package de.angebote.trackingtest

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.provider.MediaStore

/**
 * Builds a one-page coupon as a PDF and saves it to the public Downloads folder.
 * Returns the file name, or null if saving failed.
 */
fun downloadCouponPdf(context: Context, offer: Offer): String? {
    val document = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 in points
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    val paint = Paint().apply { isAntiAlias = true }

    paint.textSize = 22f
    canvas.drawText(offer.shop, 60f, 100f, paint)

    paint.textSize = 16f
    canvas.drawText(offer.title, 60f, 140f, paint)

    paint.textSize = 12f
    canvas.drawText("Gutschein für die Filiale", 60f, 190f, paint)

    paint.textSize = 34f
    canvas.drawText(offer.code, 60f, 250f, paint)

    paint.textSize = 11f
    canvas.drawText("Bitte ausdrucken und in der Filiale vorlegen.", 60f, 300f, paint)

    document.finishPage(page)

    val fileName = "gutschein-${offer.id}.pdf"
    val values = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
    }

    return try {
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null
        context.contentResolver.openOutputStream(uri).use { out ->
            if (out == null) return null
            document.writeTo(out)
        }
        fileName
    } catch (e: Exception) {
        null
    } finally {
        document.close()
    }
}
