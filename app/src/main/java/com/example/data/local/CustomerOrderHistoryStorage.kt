package com.example.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Data model for customer's past orders stored in the local storage array.
 */
data class CustomerPastOrder(
    val id: Long,
    val orderNumber: String,
    val shopName: String,
    val itemsSummary: String,
    val totalPrice: Int,
    val deliveryFee: Int = 200,
    val deliveryDate: String,
    val status: String,
    val neighborhood: String,
    val driverName: String? = null
)

/**
 * Local storage array manager for Customer Order History.
 * Maintains a persistent array of past orders with total prices and delivery dates.
 */
class CustomerOrderHistoryStorage(context: Context) {
    private val prefs = context.getSharedPreferences("customer_order_history_storage_prefs", Context.MODE_PRIVATE)
    private val _orderHistory = MutableStateFlow<List<CustomerPastOrder>>(emptyList())
    val orderHistory: StateFlow<List<CustomerPastOrder>> = _orderHistory.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        val jsonString = prefs.getString("past_orders_array", null)
        if (jsonString.isNullOrEmpty()) {
            _orderHistory.value = emptyList()
        } else {
            try {
                val list = mutableListOf<CustomerPastOrder>()
                val jsonArray = JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        CustomerPastOrder(
                            id = obj.optLong("id", System.currentTimeMillis()),
                            orderNumber = obj.optString("orderNumber", "#SG-0000"),
                            shopName = obj.optString("shopName", "مطعم"),
                            itemsSummary = obj.optString("itemsSummary", ""),
                            totalPrice = obj.optInt("totalPrice", 0),
                            deliveryFee = obj.optInt("deliveryFee", 200),
                            deliveryDate = obj.optString("deliveryDate", "اليوم"),
                            status = obj.optString("status", "تم التسليم بنجاح ✅"),
                            neighborhood = obj.optString("neighborhood", "سور الغزلان"),
                            driverName = if (obj.has("driverName") && !obj.isNull("driverName")) obj.optString("driverName") else null
                        )
                    )
                }
                _orderHistory.value = list
            } catch (e: Exception) {
                _orderHistory.value = emptyList()
            }
        }
    }

    fun addOrder(
        orderId: Long,
        orderNumber: String,
        shopName: String,
        itemsSummary: String,
        totalPrice: Int,
        deliveryFee: Int = 200,
        neighborhood: String,
        driverName: String? = null,
        status: String = "قيد التوصيل 🛵"
    ) {
        val sdf = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale("ar", "DZ"))
        val currentDate = sdf.format(Date())

        val newOrder = CustomerPastOrder(
            id = orderId,
            orderNumber = orderNumber,
            shopName = shopName,
            itemsSummary = itemsSummary,
            totalPrice = totalPrice,
            deliveryFee = deliveryFee,
            deliveryDate = currentDate,
            status = status,
            neighborhood = neighborhood,
            driverName = driverName
        )

        val updatedList = listOf(newOrder) + _orderHistory.value
        saveHistory(updatedList)
    }

    private fun saveHistory(list: List<CustomerPastOrder>) {
        _orderHistory.value = list
        val jsonArray = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("orderNumber", item.orderNumber)
            obj.put("shopName", item.shopName)
            obj.put("itemsSummary", item.itemsSummary)
            obj.put("totalPrice", item.totalPrice)
            obj.put("deliveryFee", item.deliveryFee)
            obj.put("deliveryDate", item.deliveryDate)
            obj.put("status", item.status)
            obj.put("neighborhood", item.neighborhood)
            if (item.driverName != null) {
                obj.put("driverName", item.driverName)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("past_orders_array", jsonArray.toString()).apply()
    }
}
