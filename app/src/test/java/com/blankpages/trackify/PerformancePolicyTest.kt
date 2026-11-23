package com.blankpages.trackify

import com.blankpages.trackify.domain.PerformancePolicy
import com.blankpages.trackify.domain.PerformanceTier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformancePolicyTest {

    @Test
    fun sampleSize_returnsOneForMissingDimensions() {
        assertEquals(1, PerformancePolicy.sampleSize(0, 0, 640))
        assertEquals(1, PerformancePolicy.sampleSize(100, 100, 0))
    }

    @Test
    fun sampleSize_roundsUpToPowerOfTwo() {
        assertEquals(2, PerformancePolicy.sampleSize(1200, 800, 640))
        assertEquals(4, PerformancePolicy.sampleSize(4000, 3000, 1024))
        assertEquals(1, PerformancePolicy.sampleSize(640, 480, 640))
    }

    @Test
    fun configFor_lowDeviceUsesSmallestPhotoAndSlowestGps() {
        val low = PerformancePolicy.configFor(PerformanceTier.LOW)
        val high = PerformancePolicy.configFor(PerformanceTier.HIGH)
        assertTrue(low.maxPhotoDimension < high.maxPhotoDimension)
        assertTrue(low.photoQuality < high.photoQuality)
        assertTrue(low.gpsIntervalMillis > high.gpsIntervalMillis)
        assertTrue(low.gpsMinDistanceMeters > high.gpsMinDistanceMeters)
    }

    @Test
    fun configFor_keepsPreviewSmallerThanStoredPhoto() {
        for (tier in PerformanceTier.entries) {
            val config = PerformancePolicy.configFor(tier)
            assertTrue(config.maxPreviewDimension <= config.maxPhotoDimension)
            assertTrue(config.maxPreviewItems > 0)
        }
    }
}
