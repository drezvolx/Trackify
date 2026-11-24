package com.blankpages.trackify

import com.blankpages.trackify.domain.LocationShareText
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationShareTextTest {

    @Test
    fun buildUsesApprovedLayout() {
        val text = LocationShareText.build(
            customerName = "Bud",
            landmarkNote = "Depan gang",
            latitude = -6.2,
            longitude = 106.8,
        )
        assertEquals(
            listOf(
                "Nama Penerima : Bud",
                "Detail/Patokan : Depan gang",
                "Maps : https://www.google.com/maps/search/?api=1&query=-6.200000,106.800000",
                "by Trackify.",
            ).joinToString("\n"),
            text,
        )
    }

    @Test
    fun buildKeepsEmptyDetailAsEmptyValue() {
        val text = LocationShareText.build("  ", "", 0.0, 0.0)
        val lines = text.lines()
        assertEquals(4, lines.size)
        assertEquals("Nama Penerima : ", lines[0])
        assertEquals("Detail/Patokan : ", lines[1])
        assertEquals("Maps : https://www.google.com/maps/search/?api=1&query=0.000000,0.000000", lines[2])
        assertEquals("by Trackify.", lines[3])
    }
}
