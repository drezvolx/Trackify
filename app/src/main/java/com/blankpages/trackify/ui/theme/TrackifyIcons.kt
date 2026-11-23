package com.blankpages.trackify.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object TrackifyIcons {
    private fun icon(
        name: String,
        width: Float = 1.85f,
        block: PathBuilder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = width,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block
        )
    }.build()

    val Home: ImageVector by lazy {
        icon("Home") {
            moveTo(3f, 9.5f)
            lineTo(12f, 3f)
            lineTo(21f, 9.5f)
            verticalLineTo(20f)
            arcTo(1f, 1f, 0f, false, true, 20f, 21f)
            horizontalLineTo(15f)
            verticalLineTo(14f)
            horizontalLineTo(9f)
            verticalLineTo(21f)
            horizontalLineTo(4f)
            arcTo(1f, 1f, 0f, false, true, 3f, 20f)
            close()
        }
    }

    val Table: ImageVector by lazy {
        icon("Table") {
            moveTo(3f, 5f)
            arcTo(2f, 2f, 0f, false, true, 5f, 3f)
            horizontalLineTo(19f)
            arcTo(2f, 2f, 0f, false, true, 21f, 5f)
            verticalLineTo(19f)
            arcTo(2f, 2f, 0f, false, true, 19f, 21f)
            horizontalLineTo(5f)
            arcTo(2f, 2f, 0f, false, true, 3f, 19f)
            close()
            moveTo(3f, 9f)
            horizontalLineTo(21f)
            moveTo(3f, 15f)
            horizontalLineTo(21f)
            moveTo(9f, 9f)
            verticalLineTo(21f)
        }
    }

    val MapPin: ImageVector by lazy {
        icon("MapPin") {
            moveTo(12f, 21f)
            curveTo(12f, 21f, 5f, 14.5f, 5f, 9.5f)
            arcTo(7f, 7f, 0f, true, true, 19f, 9.5f)
            curveTo(19f, 14.5f, 12f, 21f, 12f, 21f)
            close()
            moveTo(12f, 12f)
            arcTo(2.5f, 2.5f, 0f, true, false, 12f, 7f)
            arcTo(2.5f, 2.5f, 0f, true, false, 12f, 12f)
            close()
        }
    }

    val PageText: ImageVector by lazy {
        icon("PageText") {
            moveTo(6f, 3f)
            horizontalLineTo(14f)
            lineTo(19f, 8f)
            verticalLineTo(20f)
            arcTo(1f, 1f, 0f, false, true, 18f, 21f)
            horizontalLineTo(6f)
            arcTo(1f, 1f, 0f, false, true, 5f, 20f)
            verticalLineTo(4f)
            arcTo(1f, 1f, 0f, false, true, 6f, 3f)
            close()
            moveTo(14f, 3f)
            verticalLineTo(8f)
            horizontalLineTo(19f)
            moveTo(8f, 12f)
            horizontalLineTo(16f)
            moveTo(8f, 16f)
            horizontalLineTo(13f)
        }
    }

    val PackageBox: ImageVector by lazy {
        icon("PackageBox") {
            moveTo(12f, 2.5f)
            lineTo(20.5f, 7.2f)
            verticalLineTo(16.8f)
            lineTo(12f, 21.5f)
            lineTo(3.5f, 16.8f)
            verticalLineTo(7.2f)
            close()
            moveTo(3.5f, 7.2f)
            lineTo(12f, 12f)
            lineTo(20.5f, 7.2f)
            moveTo(12f, 12f)
            verticalLineTo(21.5f)
            moveTo(7.8f, 4.8f)
            lineTo(16.2f, 9.6f)
        }
    }

    val Sliders: ImageVector by lazy {
        icon("Sliders") {
            moveTo(4f, 6f)
            horizontalLineTo(20f)
            moveTo(4f, 12f)
            horizontalLineTo(20f)
            moveTo(4f, 18f)
            horizontalLineTo(20f)
            moveTo(9f, 4f)
            verticalLineTo(8f)
            moveTo(15f, 10f)
            verticalLineTo(14f)
            moveTo(10f, 16f)
            verticalLineTo(20f)
        }
    }

    val Settings: ImageVector by lazy { Sliders }

    val Plus: ImageVector by lazy {
        icon("Plus", 1.9f) {
            moveTo(12f, 5f)
            verticalLineTo(19f)
            moveTo(5f, 12f)
            horizontalLineTo(19f)
        }
    }

    val Check: ImageVector by lazy {
        icon("Check", 2.0f) {
            moveTo(5f, 12.5f)
            lineTo(9.5f, 17f)
            lineTo(19f, 7f)
        }
    }

    val Download: ImageVector by lazy {
        icon("Download") {
            moveTo(12f, 3f)
            verticalLineTo(15f)
            moveTo(7f, 11f)
            lineTo(12f, 16f)
            lineTo(17f, 11f)
            moveTo(4f, 19f)
            horizontalLineTo(20f)
        }
    }

    val Search: ImageVector by lazy {
        icon("Search") {
            moveTo(11f, 18f)
            arcTo(7f, 7f, 0f, true, false, 11f, 4f)
            arcTo(7f, 7f, 0f, true, false, 11f, 18f)
            close()
            moveTo(16.2f, 16.2f)
            lineTo(20.5f, 20.5f)
        }
    }

    val CompassArrow: ImageVector by lazy {
        icon("CompassArrow") {
            moveTo(3f, 11f)
            lineTo(21f, 3f)
            lineTo(13f, 21f)
            lineTo(11f, 13f)
            close()
        }
    }

    val ShareNode: ImageVector by lazy {
        icon("ShareNode") {
            moveTo(4f, 12f)
            verticalLineTo(19f)
            arcTo(1f, 1f, 0f, false, false, 5f, 20f)
            horizontalLineTo(19f)
            arcTo(1f, 1f, 0f, false, false, 20f, 19f)
            verticalLineTo(12f)
            moveTo(16f, 6f)
            lineTo(12f, 2f)
            lineTo(8f, 6f)
            moveTo(12f, 2f)
            verticalLineTo(15f)
        }
    }

    val Trash: ImageVector by lazy {
        icon("Trash") {
            moveTo(4f, 6f)
            horizontalLineTo(20f)
            moveTo(9f, 6f)
            verticalLineTo(4f)
            horizontalLineTo(15f)
            verticalLineTo(6f)
            moveTo(6f, 6f)
            lineTo(7f, 20f)
            horizontalLineTo(17f)
            lineTo(18f, 6f)
        }
    }

    val PinTilt: ImageVector by lazy {
        icon("PinTilt") {
            moveTo(14f, 4f)
            lineTo(20f, 10f)
            lineTo(16f, 14f)
            lineTo(15f, 17f)
            lineTo(7f, 9f)
            lineTo(10f, 8f)
            close()
            moveTo(11f, 13f)
            lineTo(4f, 20f)
        }
    }

    val Calendar: ImageVector by lazy {
        icon("Calendar") {
            moveTo(4f, 5f)
            horizontalLineTo(20f)
            arcTo(1f, 1f, 0f, false, true, 21f, 6f)
            verticalLineTo(20f)
            arcTo(1f, 1f, 0f, false, true, 20f, 21f)
            horizontalLineTo(4f)
            arcTo(1f, 1f, 0f, false, true, 3f, 20f)
            verticalLineTo(6f)
            arcTo(1f, 1f, 0f, false, true, 4f, 5f)
            close()
            moveTo(16f, 3f)
            verticalLineTo(7f)
            moveTo(8f, 3f)
            verticalLineTo(7f)
            moveTo(3f, 10f)
            horizontalLineTo(21f)
        }
    }

    val ArrowLeft: ImageVector by lazy {
        icon("ArrowLeft", 1.8f) {
            moveTo(19f, 12f)
            horizontalLineTo(5f)
            moveTo(11f, 6f)
            lineTo(5f, 12f)
            lineTo(11f, 18f)
        }
    }

    val ChevronLeft: ImageVector by lazy {
        icon("ChevronLeft", 1.8f) {
            moveTo(15f, 6f)
            lineTo(9f, 12f)
            lineTo(15f, 18f)
        }
    }

    val ChevronRight: ImageVector by lazy {
        icon("ChevronRight", 1.8f) {
            moveTo(9f, 6f)
            lineTo(15f, 12f)
            lineTo(9f, 18f)
        }
    }

    val Gps: ImageVector by lazy {
        icon("Gps") {
            moveTo(12f, 2f)
            verticalLineTo(5f)
            moveTo(12f, 19f)
            verticalLineTo(22f)
            moveTo(2f, 12f)
            horizontalLineTo(5f)
            moveTo(19f, 12f)
            horizontalLineTo(22f)
            moveTo(12f, 17f)
            arcTo(5f, 5f, 0f, true, false, 12f, 7f)
            arcTo(5f, 5f, 0f, true, false, 12f, 17f)
            close()
            moveTo(12f, 14f)
            arcTo(2f, 2f, 0f, true, false, 12f, 10f)
            arcTo(2f, 2f, 0f, true, false, 12f, 14f)
            close()
        }
    }

    val AddLocation: ImageVector by lazy { MapPin }
    val Navigation: ImageVector by lazy { CompassArrow }
    val Edit: ImageVector by lazy { PageText }
    val Add: ImageVector by lazy { Plus }
    val PinFilled: ImageVector by lazy { PinTilt }
    val PinOutlined: ImageVector by lazy { PinTilt }
}