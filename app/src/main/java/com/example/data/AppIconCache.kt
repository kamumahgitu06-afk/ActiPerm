package com.example.data

import android.content.Context
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconCache {
    private val cache = LruCache<String, ImageBitmap>(200)

    fun getCached(packageName: String): ImageBitmap? = cache.get(packageName)

    suspend fun loadIcon(context: Context, packageName: String): ImageBitmap? {
        cache.get(packageName)?.let { return it }
        return withContext(Dispatchers.IO) {
            runCatching {
                val drawable = context.packageManager.getApplicationIcon(packageName)
                val bitmap = drawable.toBitmap(width = 128, height = 128).asImageBitmap()
                cache.put(packageName, bitmap)
                bitmap
            }.getOrNull()
        }
    }
}
