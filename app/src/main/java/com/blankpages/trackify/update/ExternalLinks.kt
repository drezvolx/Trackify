package com.blankpages.trackify.update

import android.content.Context
import android.content.Intent
import android.net.Uri

/** Membuka tautan eksternal lewat browser. Tidak mengubah data aplikasi. */
fun openExternalUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
