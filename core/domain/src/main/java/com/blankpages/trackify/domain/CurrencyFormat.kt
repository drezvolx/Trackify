package com.blankpages.trackify.domain

import java.text.NumberFormat
import java.util.Locale

private val idLocale = Locale.forLanguageTag("id-ID")
private val idrFormatPool = ThreadLocal.withInitial {
    NumberFormat.getCurrencyInstance(idLocale).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }
}

fun Double.toRupiah(): String = idrFormatPool.get()?.format(this) ?: "Rp ${toLong()}"
