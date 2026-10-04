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
 * نموذج عملية لحركة نقاط الولاء (اكتساب أو استبدال)
 */
data class LoyaltyTransaction(
    val id: String,
    val points: Int, // موجب عند الكسب، سالب عند الاستبدال
    val title: String,
    val date: String,
    val isEarned: Boolean
)

/**
 * مدير تخزين نقاط الولاء للزبائن في سور الغزلان.
 * يتيح كسب 50 نقطة مع كل طلب واستبدالها بخصم 100 دج أو 200 دج على التوصيل.
 */
class CustomerLoyaltyStorage(context: Context) {
    private val prefs = context.getSharedPreferences("customer_loyalty_storage_prefs", Context.MODE_PRIVATE)

    private val _pointsBalance = MutableStateFlow(0)
    val pointsBalance: StateFlow<Int> = _pointsBalance.asStateFlow()

    private val _transactions = MutableStateFlow<List<LoyaltyTransaction>>(emptyList())
    val transactions: StateFlow<List<LoyaltyTransaction>> = _transactions.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val savedPoints = prefs.getInt("points_balance", -1)
        if (savedPoints == -1) {
            // رصيد ترحيبي مبدئي 150 نقطة ليجرب الزبون ميزة الخصم فوراً
            _pointsBalance.value = 150
            prefs.edit().putInt("points_balance", 150).apply()

            val initialTx = listOf(
                LoyaltyTransaction(
                    id = "init_welcome",
                    points = 100,
                    title = "هدية ترحيبية للزبون الجديد 🎉",
                    date = "20 سبتمبر 2026",
                    isEarned = true
                ),
                LoyaltyTransaction(
                    id = "init_order_1",
                    points = 50,
                    title = "نقاط طلب مكتمل #SG-1092 📦",
                    date = "23 سبتمبر 2026",
                    isEarned = true
                )
            )
            _transactions.value = initialTx
            saveTransactions(initialTx)
        } else {
            _pointsBalance.value = savedPoints
            loadTransactions()
        }
    }

    private fun loadTransactions() {
        val jsonStr = prefs.getString("loyalty_tx_array", null) ?: return
        try {
            val arr = JSONArray(jsonStr)
            val list = mutableListOf<LoyaltyTransaction>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    LoyaltyTransaction(
                        id = obj.getString("id"),
                        points = obj.getInt("points"),
                        title = obj.getString("title"),
                        date = obj.getString("date"),
                        isEarned = obj.getBoolean("isEarned")
                    )
                )
            }
            _transactions.value = list
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private fun saveTransactions(list: List<LoyaltyTransaction>) {
        try {
            val arr = JSONArray()
            for (tx in list) {
                val obj = JSONObject().apply {
                    put("id", tx.id)
                    put("points", tx.points)
                    put("title", tx.title)
                    put("date", tx.date)
                    put("isEarned", tx.isEarned)
                }
                arr.put(obj)
            }
            prefs.edit().putString("loyalty_tx_array", arr.toString()).apply()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * كسب نقاط جديدة عند إكمال طلب
     */
    fun addPoints(amount: Int = 50, reason: String = "مكافأة طلب مكتمل") {
        val newBal = _pointsBalance.value + amount
        _pointsBalance.value = newBal
        prefs.edit().putInt("points_balance", newBal).apply()

        val dateStr = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale("ar")).format(Date())
        val newTx = LoyaltyTransaction(
            id = "tx_${System.currentTimeMillis()}",
            points = amount,
            title = reason,
            date = dateStr,
            isEarned = true
        )
        val updated = listOf(newTx) + _transactions.value
        _transactions.value = updated
        saveTransactions(updated)
    }

    /**
     * استبدال نقاط مقابل خصم على التوصيل
     */
    fun redeemPoints(pointsToRedeem: Int, discountDa: Int): Boolean {
        if (_pointsBalance.value < pointsToRedeem) return false

        val newBal = _pointsBalance.value - pointsToRedeem
        _pointsBalance.value = newBal
        prefs.edit().putInt("points_balance", newBal).apply()

        val dateStr = SimpleDateFormat("dd MMMM yyyy - HH:mm", Locale("ar")).format(Date())
        val newTx = LoyaltyTransaction(
            id = "tx_${System.currentTimeMillis()}",
            points = -pointsToRedeem,
            title = "خصم $discountDa دج على تكلفة التوصيل 🏷️",
            date = dateStr,
            isEarned = false
        )
        val updated = listOf(newTx) + _transactions.value
        _transactions.value = updated
        saveTransactions(updated)
        return true
    }
}
