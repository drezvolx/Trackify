package com.blankpages.trackify.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object TrackifyIcons {
    private fun vector(
        name: String,
        fillType: PathFillType = PathFillType.NonZero,
        vararg blocks: PathBuilder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        blocks.forEach { block ->
            path(
                fill = SolidColor(Color.Black),
                pathFillType = fillType,
                pathBuilder = block,
            )
        }
    }.build()

    private fun solid(name: String, vararg blocks: PathBuilder.() -> Unit): ImageVector =
        vector(name, PathFillType.NonZero, *blocks)

    private fun solidCut(name: String, vararg blocks: PathBuilder.() -> Unit): ImageVector =
        vector(name, PathFillType.EvenOdd, *blocks)

    private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
        moveTo(cx, cy - r)
        arcTo(r, r, 0f, true, false, cx, cy + r)
        arcTo(r, r, 0f, true, false, cx, cy - r)
        close()
    }

    private fun PathBuilder.rect(x: Float, y: Float, w: Float, h: Float) {
        moveTo(x, y)
        lineTo(x + w, y)
        lineTo(x + w, y + h)
        lineTo(x, y + h)
        close()
    }

    val Home: ImageVector by lazy {
        solidCut("Home", {
            moveTo(12f, 2.6f)
            lineTo(21.2f, 9.4f)
            lineTo(21.2f, 19.8f)
            curveTo(21.2f, 20.8f, 20.4f, 21.6f, 19.4f, 21.6f)
            lineTo(4.6f, 21.6f)
            curveTo(3.6f, 21.6f, 2.8f, 20.8f, 2.8f, 19.8f)
            lineTo(2.8f, 9.4f)
            close()

            moveTo(9.3f, 21.6f)
            lineTo(9.3f, 15.2f)
            curveTo(9.3f, 14.1f, 10.2f, 13.2f, 11.3f, 13.2f)
            lineTo(12.7f, 13.2f)
            curveTo(13.8f, 13.2f, 14.7f, 14.1f, 14.7f, 15.2f)
            lineTo(14.7f, 21.6f)
            close()
        })
    }

    val Table: ImageVector by lazy {
        solidCut("Table", {
            moveTo(5f, 3f)
            lineTo(19f, 3f)
            curveTo(20.1f, 3f, 21f, 3.9f, 21f, 5f)
            lineTo(21f, 19f)
            curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
            lineTo(5f, 21f)
            curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
            lineTo(3f, 5f)
            curveTo(3f, 3.9f, 3.9f, 3f, 5f, 3f)
            close()

            rect(4.6f, 7.2f, 14.8f, 1.2f)
            rect(10.9f, 8.6f, 1.2f, 5f)
            rect(3.2f, 14f, 17.6f, 1.2f)
            rect(10.9f, 15.4f, 1.2f, 5f)
        })
    }

    val MapPin: ImageVector by lazy {
        solidCut("MapPin", {
            moveTo(12f, 21.6f)
            curveTo(10.8f, 20f, 4.5f, 14.2f, 4.5f, 9.3f)
            curveTo(4.5f, 5.6f, 7.8f, 2.5f, 12f, 2.5f)
            curveTo(16.2f, 2.5f, 19.5f, 5.6f, 19.5f, 9.3f)
            curveTo(19.5f, 14.2f, 13.2f, 20f, 12f, 21.6f)
            close()

            circle(12f, 9.3f, 2.8f)
        })
    }

    val PageText: ImageVector by lazy {
        solidCut("PageText", {
            moveTo(5.6f, 2.8f)
            lineTo(14f, 2.8f)
            lineTo(19f, 7.8f)
            lineTo(19f, 20.4f)
            curveTo(19f, 21.4f, 18.3f, 22f, 17.3f, 22f)
            lineTo(6.7f, 22f)
            curveTo(5.7f, 22f, 5f, 21.4f, 5f, 20.4f)
            lineTo(5f, 3.4f)
            curveTo(5f, 3.1f, 5.2f, 2.8f, 5.6f, 2.8f)
            close()

            rect(8f, 11f, 8f, 1.2f)
            rect(8f, 14.6f, 8f, 1.2f)
            rect(8f, 18.2f, 5.4f, 1.2f)
        })
    }

    val PackageBox: ImageVector by lazy {
        solidCut("PackageBox", {
            moveTo(12f, 2.2f)
            lineTo(20.8f, 7.1f)
            lineTo(20.8f, 16.9f)
            lineTo(12f, 21.8f)
            lineTo(3.2f, 16.9f)
            lineTo(3.2f, 7.1f)
            close()

            moveTo(3.2f, 6.2f)
            lineTo(12f, 11.1f)
            lineTo(20.8f, 6.2f)
            lineTo(20.8f, 7.4f)
            lineTo(12f, 12.3f)
            lineTo(3.2f, 7.4f)
            close()

            rect(11.3f, 12.6f, 1.4f, 8.6f)
        })
    }

    val Sliders: ImageVector by lazy {
        solid(
            "Sliders",
            {
                rect(4f, 5.2f, 3.4f, 1.6f)
                circle(9f, 6f, 2.4f)
                rect(10.6f, 5.2f, 9.4f, 1.6f)
            },
            {
                rect(4f, 11.2f, 9.2f, 1.6f)
                circle(15f, 12f, 2.4f)
                rect(16.8f, 11.2f, 3.2f, 1.6f)
            },
            {
                rect(4f, 17.2f, 4.2f, 1.6f)
                circle(10f, 18f, 2.4f)
                rect(11.8f, 17.2f, 8.2f, 1.6f)
            },
        )
    }

    val Settings: ImageVector by lazy { Sliders }

    val Plus: ImageVector by lazy {
        solid("Plus", {
            moveTo(10.2f, 3.4f)
            lineTo(13.8f, 3.4f)
            lineTo(13.8f, 10.2f)
            lineTo(20.6f, 10.2f)
            lineTo(20.6f, 13.8f)
            lineTo(13.8f, 13.8f)
            lineTo(13.8f, 20.6f)
            lineTo(10.2f, 20.6f)
            lineTo(10.2f, 13.8f)
            lineTo(3.4f, 13.8f)
            lineTo(3.4f, 10.2f)
            lineTo(10.2f, 10.2f)
            close()
        })
    }

    val Check: ImageVector by lazy {
        solid("Check", {
            moveTo(9.2f, 16.6f)
            lineTo(4.6f, 12f)
            lineTo(6.9f, 9.7f)
            lineTo(9.2f, 12f)
            lineTo(17.1f, 4.1f)
            lineTo(19.4f, 6.4f)
            close()
        })
    }

    val Download: ImageVector by lazy {
        solid(
            "Download",
            {
                moveTo(10.4f, 3f)
                lineTo(13.6f, 3f)
                lineTo(13.6f, 13.4f)
                lineTo(17.2f, 13.4f)
                lineTo(12f, 18.6f)
                lineTo(6.8f, 13.4f)
                lineTo(10.4f, 13.4f)
                close()
            },
            {
                rect(4f, 19.6f, 16f, 1.6f)
            },
        )
    }

    val Search: ImageVector by lazy {
        vector(
            "Search",
            PathFillType.EvenOdd,
            {
                circle(10.5f, 10.5f, 6.8f)
                circle(10.5f, 10.5f, 4.3f)
            },
            {
                moveTo(14.53f, 16.09f)
                lineTo(16.09f, 14.53f)
                lineTo(21.28f, 19.72f)
                lineTo(19.72f, 21.28f)
                close()
            },
        )
    }

    val CompassArrow: ImageVector by lazy {
        solid("CompassArrow", {
            moveTo(21f, 3f)
            lineTo(13f, 21f)
            lineTo(11f, 13f)
            lineTo(3f, 11f)
            close()
        })
    }

    val ShareNode: ImageVector by lazy {
        solid(
            "ShareNode",
            { circle(18f, 5f, 2.6f) },
            { circle(6f, 12f, 2.6f) },
            { circle(18f, 19f, 2.6f) },
            {
                moveTo(7.17f, 11.69f)
                lineTo(8.03f, 10.11f)
                lineTo(16.83f, 5.31f)
                lineTo(15.97f, 6.89f)
                close()
            },
            {
                moveTo(8.03f, 12.31f)
                lineTo(7.17f, 13.89f)
                lineTo(15.97f, 18.69f)
                lineTo(16.83f, 17.11f)
                close()
            },
        )
    }

    val Trash: ImageVector by lazy {
        vector(
            "Trash",
            PathFillType.EvenOdd,
            {
                moveTo(6.2f, 7.6f)
                lineTo(17.8f, 7.6f)
                lineTo(16.9f, 20.1f)
                curveTo(16.8f, 21.1f, 16f, 21.8f, 15f, 21.8f)
                lineTo(9f, 21.8f)
                curveTo(8f, 21.8f, 7.2f, 21.1f, 7.1f, 20.1f)
                close()

                rect(9.6f, 10.2f, 1.2f, 8.8f)
                rect(13.2f, 10.2f, 1.2f, 8.8f)
            },
            { rect(4.2f, 5.2f, 15.6f, 1.8f) },
            { rect(9.2f, 3.2f, 5.6f, 1.6f) },
        )
    }

    val PinTilt: ImageVector by lazy {
        vector(
            "PinTilt",
            PathFillType.EvenOdd,
            {
                moveTo(13.2f, 3.2f)
                lineTo(20.8f, 10.8f)
                lineTo(17.1f, 14.5f)
                lineTo(15.4f, 17.4f)
                lineTo(6.6f, 8.6f)
                lineTo(9.5f, 6.9f)
                close()

                circle(14.6f, 9.4f, 1.4f)
            },
            {
                moveTo(10.96f, 14.16f)
                lineTo(9.84f, 13.04f)
                lineTo(3.24f, 19.64f)
                lineTo(4.36f, 20.76f)
                close()
            },
        )
    }

    val Calendar: ImageVector by lazy {
        solidCut("Calendar", {
            moveTo(5f, 5f)
            lineTo(19f, 5f)
            curveTo(20.1f, 5f, 21f, 5.9f, 21f, 7f)
            lineTo(21f, 19.5f)
            curveTo(21f, 20.6f, 20.1f, 21.5f, 19f, 21.5f)
            lineTo(5f, 21.5f)
            curveTo(3.9f, 21.5f, 3f, 20.6f, 3f, 19.5f)
            lineTo(3f, 7f)
            curveTo(3f, 5.9f, 3.9f, 5f, 5f, 5f)
            close()

            rect(3.4f, 9.2f, 17.2f, 1.3f)
            circle(7.5f, 13.6f, 0.9f)
            circle(12f, 13.6f, 0.9f)
            circle(16.5f, 13.6f, 0.9f)
            circle(7.5f, 17.4f, 0.9f)
            circle(12f, 17.4f, 0.9f)
            circle(16.5f, 17.4f, 0.9f)
        })
    }

    val ArrowLeft: ImageVector by lazy {
        solid("ArrowLeft", {
            moveTo(11.5f, 4.6f)
            lineTo(11.5f, 8.2f)
            lineTo(19f, 8.2f)
            lineTo(19f, 15.8f)
            lineTo(11.5f, 15.8f)
            lineTo(11.5f, 19.4f)
            lineTo(3.4f, 12f)
            close()
        })
    }

    val ChevronLeft: ImageVector by lazy {
        solid("ChevronLeft", {
            moveTo(15f, 4.5f)
            lineTo(17.2f, 6.7f)
            lineTo(10.9f, 13f)
            lineTo(17.2f, 19.3f)
            lineTo(15f, 21.5f)
            lineTo(6.5f, 13f)
            close()
        })
    }

    val ChevronRight: ImageVector by lazy {
        solid("ChevronRight", {
            moveTo(9f, 4.5f)
            lineTo(6.8f, 6.7f)
            lineTo(13.1f, 13f)
            lineTo(6.8f, 19.3f)
            lineTo(9f, 21.5f)
            lineTo(17.5f, 13f)
            close()
        })
    }

    val Gps: ImageVector by lazy {
        vector(
            "Gps",
            PathFillType.EvenOdd,
            {
                circle(12f, 12f, 7.2f)
                circle(12f, 12f, 5.4f)
            },
            { circle(12f, 12f, 2.2f) },
            { rect(11.2f, 1.8f, 1.6f, 3.2f) },
            { rect(11.2f, 19f, 1.6f, 3.2f) },
            { rect(1.8f, 11.2f, 3.2f, 1.6f) },
            { rect(19f, 11.2f, 3.2f, 1.6f) },
        )
    }

    val Bell: ImageVector by lazy {
        solid(
            "Bell",
            {
                moveTo(12f, 2.8f)
                curveTo(8.2f, 2.8f, 6f, 5.6f, 6f, 9.2f)
                lineTo(6f, 14.5f)
                lineTo(4f, 17.5f)
                lineTo(20f, 17.5f)
                lineTo(18f, 14.5f)
                lineTo(18f, 9.2f)
                curveTo(18f, 5.6f, 15.8f, 2.8f, 12f, 2.8f)
                close()
            },
            { circle(12f, 20.2f, 2f) },
            { circle(12f, 2.2f, 1.2f) },
        )
    }

    val Target: ImageVector by lazy {
        vector(
            "Target",
            PathFillType.EvenOdd,
            {
                circle(12f, 12f, 8.8f)
                circle(12f, 12f, 5.8f)
            },
            { circle(12f, 12f, 2.8f) },
        )
    }

    val Wallet: ImageVector by lazy {
        solidCut("Wallet", {
            moveTo(5f, 5.5f)
            lineTo(19f, 5.5f)
            curveTo(20.1f, 5.5f, 21f, 6.4f, 21f, 7.5f)
            lineTo(21f, 18f)
            curveTo(21f, 19.1f, 20.1f, 20f, 19f, 20f)
            lineTo(5f, 20f)
            curveTo(3.9f, 20f, 3f, 19.1f, 3f, 18f)
            lineTo(3f, 7.5f)
            curveTo(3f, 6.4f, 3.9f, 5.5f, 5f, 5.5f)
            close()

            rect(3.4f, 9f, 17.2f, 1.2f)
            rect(6f, 13f, 8f, 3.6f)
            circle(18f, 14.8f, 1.1f)
        })
    }

    val ChartBar: ImageVector by lazy {
        solid(
            "ChartBar",
            { rect(4f, 12.5f, 3f, 8.5f) },
            { rect(9.5f, 8.5f, 3f, 12.5f) },
            { rect(15f, 4.5f, 3f, 16.5f) },
            { rect(3.2f, 20.4f, 17.6f, 1.4f) },
        )
    }

    val Route: ImageVector by lazy {
        solid(
            "Route",
            { rect(4.2f, 9f, 1.6f, 9.6f) },
            { rect(4.2f, 9.2f, 8.6f, 1.6f) },
            { rect(11.2f, 5.6f, 1.6f, 5.2f) },
            { rect(11.2f, 5.6f, 8.6f, 1.6f) },
            { circle(5f, 18.6f, 2.6f) },
            { circle(19f, 6.4f, 2.6f) },
        )
    }

    val Link: ImageVector by lazy {
        vector(
            "Link",
            PathFillType.EvenOdd,
            {
                moveTo(2.28f, 14.72f)
                lineTo(4.12f, 12.88f)
                lineTo(13.12f, 3.88f)
                lineTo(11.28f, 5.72f)
                close()

                moveTo(5.08f, 12.92f)
                lineTo(5.72f, 12.28f)
                lineTo(10.92f, 7.08f)
                lineTo(10.28f, 7.72f)
                close()
            },
            {
                moveTo(10.88f, 20.72f)
                lineTo(12.72f, 18.88f)
                lineTo(21.72f, 9.88f)
                lineTo(19.88f, 11.72f)
                close()

                moveTo(13.68f, 18.32f)
                lineTo(14.32f, 17.68f)
                lineTo(19.32f, 12.68f)
                lineTo(18.68f, 13.32f)
                close()
            },
        )
    }

    val Info: ImageVector by lazy {
        solidCut("Info", {
            circle(12f, 12f, 9f)
            circle(12f, 7.6f, 1.2f)
            rect(11.1f, 10.2f, 1.8f, 7.2f)
        })
    }

    val Warning: ImageVector by lazy {
        solidCut("Warning", {
            moveTo(10.9f, 4.1f)
            curveTo(11.5f, 3f, 12.5f, 3f, 13.1f, 4.1f)
            lineTo(21.1f, 18.1f)
            curveTo(21.7f, 19.2f, 20.9f, 20.6f, 19.6f, 20.6f)
            lineTo(4.4f, 20.6f)
            curveTo(3.1f, 20.6f, 2.3f, 19.2f, 2.9f, 18.1f)
            close()

            rect(11.1f, 8.6f, 1.8f, 5.6f)
            circle(12f, 16.8f, 1.1f)
        })
    }

    val Close: ImageVector by lazy {
        solid("Close", {
            moveTo(5.6f, 4.2f)
            lineTo(12f, 10.6f)
            lineTo(18.4f, 4.2f)
            lineTo(19.8f, 5.6f)
            lineTo(13.4f, 12f)
            lineTo(19.8f, 18.4f)
            lineTo(18.4f, 19.8f)
            lineTo(12f, 13.4f)
            lineTo(5.6f, 19.8f)
            lineTo(4.2f, 18.4f)
            lineTo(10.6f, 12f)
            lineTo(4.2f, 5.6f)
            close()
        })
    }

    val Refresh: ImageVector by lazy {
        solid(
            "Refresh",
            {
                moveTo(20.4f, 11.6f)
                curveTo(19.8f, 16.8f, 16.1f, 20.4f, 11.5f, 20.4f)
                curveTo(8.3f, 20.4f, 5.6f, 18.7f, 4.2f, 16.1f)
                lineTo(6.9f, 14.7f)
                curveTo(8f, 16.6f, 9.7f, 17.7f, 11.6f, 17.7f)
                curveTo(14.9f, 17.7f, 17.6f, 15f, 17.6f, 11.6f)
                close()
            },
            {
                moveTo(18.6f, 8.2f)
                lineTo(22.4f, 11.6f)
                lineTo(18.6f, 15f)
                close()
            },
        )
    }

    val Backup: ImageVector by lazy {
        vector(
            "Backup",
            PathFillType.EvenOdd,
            {
                moveTo(5f, 4f)
                lineTo(19f, 4f)
                curveTo(20.1f, 4f, 21f, 4.9f, 21f, 6f)
                lineTo(21f, 14f)
                curveTo(21f, 15.1f, 20.1f, 16f, 19f, 16f)
                lineTo(5f, 16f)
                curveTo(3.9f, 16f, 3f, 15.1f, 3f, 14f)
                lineTo(3f, 6f)
                curveTo(3f, 4.9f, 3.9f, 4f, 5f, 4f)
                close()

                rect(3.6f, 6.8f, 16.8f, 1.1f)
                rect(3.6f, 10.6f, 16.8f, 1.1f)
            },
            {
                rect(11.2f, 14.5f, 1.6f, 4.2f)
                moveTo(8.4f, 17.8f)
                lineTo(15.6f, 17.8f)
                lineTo(12f, 21.8f)
                close()
            },
        )
    }

    val Restore: ImageVector by lazy {
        vector(
            "Restore",
            PathFillType.EvenOdd,
            {
                moveTo(5f, 8f)
                lineTo(19f, 8f)
                curveTo(20.1f, 8f, 21f, 8.9f, 21f, 10f)
                lineTo(21f, 18f)
                curveTo(21f, 19.1f, 20.1f, 20f, 19f, 20f)
                lineTo(5f, 20f)
                curveTo(3.9f, 20f, 3f, 19.1f, 3f, 18f)
                lineTo(3f, 10f)
                curveTo(3f, 8.9f, 3.9f, 8f, 5f, 8f)
                close()

                rect(3.6f, 10.8f, 16.8f, 1.1f)
                rect(3.6f, 14.6f, 16.8f, 1.1f)
            },
            {
                rect(11.2f, 3.2f, 1.6f, 6.2f)
                moveTo(8.4f, 6.8f)
                lineTo(15.6f, 6.8f)
                lineTo(12f, 2.4f)
                close()
            },
        )
    }

    val TemplateTitipan: ImageVector by lazy {
        solidCut("TemplateTitipan", {
            moveTo(5f, 5f)
            lineTo(19f, 5f)
            curveTo(20.1f, 5f, 21f, 5.9f, 21f, 7f)
            lineTo(21f, 17f)
            curveTo(21f, 18.1f, 20.1f, 19f, 19f, 19f)
            lineTo(5f, 19f)
            curveTo(3.9f, 19f, 3f, 18.1f, 3f, 17f)
            lineTo(3f, 7f)
            curveTo(3f, 5.9f, 3.9f, 5f, 5f, 5f)
            close()

            rect(7f, 11.2f, 7f, 1.6f)
            moveTo(13.9f, 8.7f)
            lineTo(17.7f, 12f)
            lineTo(13.9f, 15.3f)
            close()
        })
    }

    val TemplateKendala: ImageVector by lazy {
        solidCut("TemplateKendala", {
            moveTo(10.9f, 4.1f)
            curveTo(11.5f, 3f, 12.5f, 3f, 13.1f, 4.1f)
            lineTo(21.1f, 18.1f)
            curveTo(21.7f, 19.2f, 20.9f, 20.6f, 19.6f, 20.6f)
            lineTo(4.4f, 20.6f)
            curveTo(3.1f, 20.6f, 2.3f, 19.2f, 2.9f, 18.1f)
            close()

            moveTo(8.8f, 9.8f)
            lineTo(10.2f, 8.4f)
            lineTo(15.2f, 13.4f)
            lineTo(13.8f, 14.8f)
            close()

            moveTo(15.2f, 8.4f)
            lineTo(16.6f, 9.8f)
            lineTo(11.6f, 14.8f)
            lineTo(10.2f, 13.4f)
            close()
        })
    }

    val AddLocation: ImageVector by lazy { MapPin }
    val Navigation: ImageVector by lazy { CompassArrow }
    val Edit: ImageVector by lazy { PageText }
    val Add: ImageVector by lazy { Plus }
    val PinFilled: ImageVector by lazy { PinTilt }
    val PinOutlined: ImageVector by lazy { PinTilt }
}
