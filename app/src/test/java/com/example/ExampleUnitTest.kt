package com.example

import com.example.data.models.OrderStatus
import com.example.data.models.SourElGhozlaneConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testSourElGhozlaneConstants() {
        assertEquals(36.1480, SourElGhozlaneConstants.CENTER_LAT, 0.0001)
        assertEquals(3.6900, SourElGhozlaneConstants.CENTER_LON, 0.0001)
        assertEquals(200, SourElGhozlaneConstants.FIXED_DELIVERY_FEE)
        assertEquals(8.0, SourElGhozlaneConstants.GEOFENCE_RADIUS_KM, 0.01)
        assertTrue(SourElGhozlaneConstants.NEIGHBORHOODS.size >= 8)
    }

    @Test
    fun testOrderStatusSteps() {
        assertEquals(1, OrderStatus.NEW.stepIndex)
        assertEquals(2, OrderStatus.PREPARING.stepIndex)
        assertEquals(3, OrderStatus.ON_THE_WAY.stepIndex)
        assertEquals(4, OrderStatus.DELIVERED.stepIndex)
    }
}
