package com.example.ui.components

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object SampledBitmapCache {
    private val cache = LruCache<String, ImageBitmap>(8)

    fun get(key: String): ImageBitmap? = synchronized(cache) { cache.get(key) }

    fun put(key: String, bitmap: ImageBitmap) {
        synchronized(cache) { cache.put(key, bitmap) }
    }
}

/**
 * Decodes and downsamples a drawable resource on Dispatchers.IO so the Main Thread
 * never stalls during first-frame SurfaceSyncGroup transactions.
 */
@Composable
fun AsyncDrawableImage(
    @DrawableRes resId: Int,
    contentDescription: String?,
    targetWidthPx: Int,
    targetHeightPx: Int,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val context = LocalContext.current
    val cacheKey = "${resId}_${targetWidthPx}x${targetHeightPx}"
    val cached = SampledBitmapCache.get(cacheKey)

    val bitmap by produceState<ImageBitmap?>(initialValue = cached, key1 = cacheKey) {
        if (value == null) {
            val decoded = withContext(Dispatchers.IO) {
                decodeSampledImageBitmap(
                    resources = context.resources,
                    resId = resId,
                    reqWidth = targetWidthPx,
                    reqHeight = targetHeightPx
                )
            }
            if (decoded != null) {
                SampledBitmapCache.put(cacheKey, decoded)
                value = decoded
            }
        }
    }

    val readyBitmap = bitmap
    if (readyBitmap != null) {
        Image(
            bitmap = readyBitmap,
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Box(modifier = modifier.background(placeholderColor))
    }
}

private fun decodeSampledImageBitmap(
    resources: Resources,
    @DrawableRes resId: Int,
    reqWidth: Int,
    reqHeight: Int
): ImageBitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeResource(resources, resId, bounds)

    val options = BitmapFactory.Options().apply {
        inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight, reqWidth, reqHeight)
    }
    BitmapFactory.decodeResource(resources, resId, options)?.asImageBitmap()
}.getOrNull()

private fun calculateInSampleSize(
    rawWidth: Int,
    rawHeight: Int,
    reqWidth: Int,
    reqHeight: Int
): Int {
    var inSampleSize = 1
    if (rawHeight > reqHeight || rawWidth > reqWidth) {
        val halfHeight = rawHeight / 2
        val halfWidth = rawWidth / 2
        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize.coerceAtLeast(1)
}
