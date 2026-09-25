package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.CustomerOrderHistoryStorage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CustomerOrderHistoryStorageTest {

    private lateinit var context: Context
    private lateinit var storage: CustomerOrderHistoryStorage

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("customer_order_history_storage_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        storage = CustomerOrderHistoryStorage(context)
    }

    @Test
    fun testInitialOrderHistorySeeded() {
        val history = storage.orderHistory.value
        assertTrue("Initial history should not be empty", history.isNotEmpty())
        assertEquals(3, history.size)
        val firstOrder = history[0]
        assertNotNull(firstOrder.orderNumber)
        assertNotNull(firstOrder.shopName)
        assertTrue(firstOrder.totalPrice > 0)
        assertEquals(200, firstOrder.deliveryFee)
        assertNotNull(firstOrder.deliveryDate)
    }

    @Test
    fun testAddOrderToLocalStorageArray() {
        val initialCount = storage.orderHistory.value.size
        storage.addOrder(
            orderId = 9999,
            orderNumber = "SOUR-9999",
            shopName = "مطعم الأوراس",
            itemsSummary = "1x شواء دجاج",
            totalPrice = 950,
            deliveryFee = 200,
            neighborhood = "حي الوئام",
            driverName = "أمين",
            status = "قيد التوصيل 🛵"
        )

        val updated = storage.orderHistory.value
        assertEquals(initialCount + 1, updated.size)
        val latest = updated.first()
        assertEquals("SOUR-9999", latest.orderNumber)
        assertEquals("مطعم الأوراس", latest.shopName)
        assertEquals(950, latest.totalPrice)
        assertEquals("حي الوئام", latest.neighborhood)
        assertEquals("أمين", latest.driverName)
    }
}
