package com.example.vougecycle

import android.widget.ImageView
import coil.load

/** Loads a product photo from Supabase or from the app's bundled catalogue assets. */
object ProductImages {
    fun load(imageView: ImageView, imageUrl: String?) {
        val imageSource = imageUrl?.takeIf { it.isNotBlank() }?.let { source ->
            if (source.startsWith("asset://")) {
                "file:///android_asset/${source.removePrefix("asset://")}"
            } else source
        }
        imageView.load(imageSource) {
            crossfade(true)
            placeholder(R.color.vc_grey_bg)
            error(R.color.vc_grey_bg)
        }
    }
}
