package com.blankpages.trackify.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

private val TrackifyCardCorner = 20.dp
private val TrackifyCardBorder = 1.dp

/**
 * Kartu standar Trackify.
 *
 * Token final: rounded 20.dp, `scheme.surface`, border `outlineVariant` 1.dp,
 * padding konten 16.dp. Komponen baru sebaiknya memakai ini agar tema tidak
 * beda-beda antar layar.
 */
@Composable
fun TrackifyCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val cardModifier = modifier.fillMaxWidth()

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = verticalArrangement,
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(TrackifyCardCorner),
            color = scheme.surface,
            border = BorderStroke(TrackifyCardBorder, borderColor),
            modifier = cardModifier,
            content = cardContent
        )
    } else {
        Surface(
            shape = RoundedCornerShape(TrackifyCardCorner),
            color = scheme.surface,
            border = BorderStroke(TrackifyCardBorder, borderColor),
            modifier = cardModifier,
            content = cardContent
        )
    }
}

/**
 * Dialog standar Trackify.
 *
 * Memakai token yang sama dengan [TrackifyCard] supaya warna dialog tidak
 * beda dengan kartu di layar. Material3 default memakai surfaceContainer,
 * sehingga di sini dipaksa ke `scheme.surface` dengan elevation 0.
 */
@Composable
fun TrackifyDialog(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(TrackifyCardCorner),
    properties: DialogProperties = DialogProperties()
) {
    val scheme = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() } },
        text = text,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        shape = shape,
        containerColor = scheme.surface,
        titleContentColor = scheme.onSurface,
        textContentColor = scheme.onSurfaceVariant,
        tonalElevation = 0.dp,
        properties = properties
    )
}
