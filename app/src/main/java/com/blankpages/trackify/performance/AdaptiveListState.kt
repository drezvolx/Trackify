package com.blankpages.trackify.performance

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.blankpages.trackify.data.performance.PerformanceRuntime

/**
 * State daftar yang mengikuti anggaran TPG. Perubahan grade frame diterima
 * lewat listener governor, jadi daftar tidak perlu berlangganan Flow baru.
 */
@Stable
class AdaptiveListState(val listState: LazyListState, val budget: AdaptiveBudget) {
    /** Saat frame memburuk, decode foto ditunda sampai scroll berhenti. */
    val deferDecode: Boolean
        get() = budget.deferDecodeWhileScrolling && listState.isScrollInProgress
}

@Composable
fun rememberAdaptiveListState(): AdaptiveListState {
    val governor = PerformanceRuntime.governor
    var budget by remember { mutableStateOf(governor.budget) }
    DisposableEffect(governor) {
        val listener: (AdaptiveBudget) -> Unit = { budget = it }
        governor.addListener(listener)
        onDispose { governor.removeListener(listener) }
    }
    val listState = rememberLazyListState()
    return remember(listState, budget) { AdaptiveListState(listState, budget) }
}
