package com.blankpages.trackify.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Splash satu Activity: geometri bookmark-book.svg, Geist, dan token tema.
 *
 * Hanya menampilkan ikon lalu nama aplikasi dalam satu baris. Typography bergerak per huruf
 * memakai satu [Animatable], tanpa timer per huruf. Tidak ada Activity kedua, `Thread.sleep`,
 * atau workload database di jalur animasi; `windowBackground` theme menutup frame pertama.
 */
@Composable
fun TrackifySplash(onFinished: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val markAlpha = remember { Animatable(0f) }
    val markScale = remember { Animatable(0.82f) }
    val reveal = remember { Animatable(0f) }
    val markPath = remember { bookmarkPath() }
    val letters = remember { "Trackify".toCharArray() }

    LaunchedEffect(Unit) {
        launch { markAlpha.animateTo(1f, tween(220)) }
        launch {
            markScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
        }
        reveal.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(
                modifier = Modifier
                    .size(46.dp)
                    .alpha(markAlpha.value)
                    .scale(markScale.value),
            ) {
                val unit = size.minDimension / 24f
                translate((size.width - 24f * unit) / 2f, (size.height - 24f * unit) / 2f) {
                    scale(unit) {
                        drawPath(
                            path = markPath,
                            color = scheme.onBackground,
                            style = Stroke(
                                width = 1.55f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Row {
                letters.forEachIndexed { index, letter ->
                    val local = ((reveal.value - index * LETTER_STEP) / LETTER_SPAN).coerceIn(0f, 1f)
                    Text(
                        text = letter.toString(),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.sp,
                        ),
                        color = scheme.onBackground,
                        modifier = Modifier
                            .alpha(local)
                            .graphicsLayer { translationY = (1f - local) * 12.dp.toPx() },
                    )
                }
            }
        }
    }
}

private const val LETTER_STEP = 0.055f
private const val LETTER_SPAN = 0.42f

/** Path SVG asli (viewBox 24) dipakai ulang supaya launcher dan splash identik. */
private fun bookmarkPath(): Path = Path().apply {
    moveTo(4f, 19f)
    lineTo(4f, 5f)
    cubicTo(4f, 3.89543f, 4.89543f, 3f, 6f, 3f)
    lineTo(19.4f, 3f)
    cubicTo(19.7314f, 3f, 20f, 3.26863f, 20f, 3.6f)
    lineTo(20f, 16.7143f)

    moveTo(8f, 3f)
    lineTo(8f, 11f)
    lineTo(10.5f, 9.4f)
    lineTo(13f, 11f)
    lineTo(13f, 3f)

    moveTo(6f, 17f)
    lineTo(20f, 17f)
    moveTo(6f, 21f)
    lineTo(20f, 21f)

    moveTo(6f, 21f)
    cubicTo(4.89543f, 21f, 4f, 20.1046f, 4f, 19f)
    cubicTo(4f, 17.8954f, 4.89543f, 17f, 6f, 17f)
}
