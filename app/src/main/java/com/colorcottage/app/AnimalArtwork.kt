package com.colorcottage.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import androidx.core.content.ContextCompat

/** Rasterize VectorDrawable at a crisp size while preserving its viewport aspect ratio. */
internal fun rasterizeAnimalArtwork(
    context: Context,
    resourceId: Int,
    vectorWidth: Int,
    vectorHeight: Int
): Bitmap {
    if (vectorWidth <= 0) {
        return BitmapFactory.decodeResource(context.resources, resourceId)
            ?: error("Unable to decode animal artwork resource $resourceId")
    }

    val drawable = ContextCompat.getDrawable(context, resourceId)
        ?: error("Unable to load animal vector resource $resourceId")

    var width = vectorWidth
    var height = vectorHeight
    if (width == 1 && height == 1) {
        // The converter may emit 24dp intrinsic sizes. Use the actual SVG viewport
        // instead so landscape and portrait animals keep their original proportions.
        val androidNs = "http://schemas.android.com/apk/res/android"
        var viewportWidth = 0f
        var viewportHeight = 0f
        context.resources.getXml(resourceId).use { parser ->
            while (parser.eventType != org.xmlpull.v1.XmlPullParser.START_TAG &&
                parser.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                parser.next()
            }
            viewportWidth = parser.getAttributeValue(androidNs, "viewportWidth")?.toFloatOrNull() ?: 0f
            viewportHeight = parser.getAttributeValue(androidNs, "viewportHeight")?.toFloatOrNull() ?: 0f
        }
        require(viewportWidth > 0f && viewportHeight > 0f) {
            "Missing viewport dimensions for animal vector $resourceId"
        }
        val maxSide = 1313f
        val scale = maxSide / maxOf(viewportWidth, viewportHeight)
        width = (viewportWidth * scale).toInt().coerceAtLeast(1)
        height = (viewportHeight * scale).toInt().coerceAtLeast(1)
    }

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    drawable.setBounds(0, 0, width, height)
    drawable.draw(Canvas(bitmap))
    return bitmap
}
