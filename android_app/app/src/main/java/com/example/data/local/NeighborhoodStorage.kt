package com.example.data.local

import android.content.Context
import com.example.data.models.Neighborhood
import com.example.data.models.SourElGhozlaneConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * مدير تخزين الأحياء في بلدية سور الغزلان مع دعم الإضافة والتعديل والحذف.
 */
class NeighborhoodStorage(context: Context) {
    private val prefs = context.getSharedPreferences("sour_neighborhoods_prefs", Context.MODE_PRIVATE)

    private val _neighborhoods = MutableStateFlow<List<Neighborhood>>(emptyList())
    val neighborhoods: StateFlow<List<Neighborhood>> = _neighborhoods.asStateFlow()

    init {
        loadNeighborhoods()
    }

    private fun loadNeighborhoods() {
        val jsonStr = prefs.getString("neighborhoods_list", null)
        if (jsonStr.isNullOrEmpty()) {
            _neighborhoods.value = SourElGhozlaneConstants.NEIGHBORHOODS
            saveNeighborhoods(SourElGhozlaneConstants.NEIGHBORHOODS)
        } else {
            try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<Neighborhood>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        Neighborhood(
                            nameArabic = obj.getString("nameArabic"),
                            nameFrench = obj.optString("nameFrench", ""),
                            lat = obj.optDouble("lat", SourElGhozlaneConstants.CENTER_LAT),
                            lon = obj.optDouble("lon", SourElGhozlaneConstants.CENTER_LON)
                        )
                    )
                }
                _neighborhoods.value = list
            } catch (e: Exception) {
                _neighborhoods.value = SourElGhozlaneConstants.NEIGHBORHOODS
            }
        }
    }

    fun addNeighborhood(nameArabic: String, nameFrench: String, lat: Double, lon: Double): Boolean {
        if (nameArabic.isBlank()) return false
        val current = _neighborhoods.value.toMutableList()
        if (current.any { it.nameArabic.trim() == nameArabic.trim() }) {
            return false // Already exists
        }
        val newN = Neighborhood(nameArabic.trim(), nameFrench.trim(), lat, lon)
        current.add(newN)
        saveNeighborhoods(current)
        return true
    }

    fun updateNeighborhood(
        originalNameArabic: String,
        newNameArabic: String,
        newNameFrench: String,
        lat: Double,
        lon: Double
    ): Boolean {
        if (newNameArabic.isBlank()) return false
        val current = _neighborhoods.value.toMutableList()
        val index = current.indexOfFirst { it.nameArabic == originalNameArabic }
        if (index == -1) return false
        current[index] = Neighborhood(newNameArabic.trim(), newNameFrench.trim(), lat, lon)
        saveNeighborhoods(current)
        return true
    }

    fun deleteNeighborhood(nameArabic: String): Boolean {
        val current = _neighborhoods.value.toMutableList()
        val removed = current.removeAll { it.nameArabic == nameArabic }
        if (removed) {
            saveNeighborhoods(current)
        }
        return removed
    }

    fun resetToDefaults() {
        saveNeighborhoods(SourElGhozlaneConstants.NEIGHBORHOODS)
    }

    private fun saveNeighborhoods(list: List<Neighborhood>) {
        _neighborhoods.value = list
        try {
            val array = JSONArray()
            for (item in list) {
                val obj = JSONObject()
                obj.put("nameArabic", item.nameArabic)
                obj.put("nameFrench", item.nameFrench)
                obj.put("lat", item.lat)
                obj.put("lon", item.lon)
                array.put(obj)
            }
            prefs.edit().putString("neighborhoods_list", array.toString()).apply()
        } catch (e: Exception) {
            // ignore
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: NeighborhoodStorage? = null

        fun getInstance(context: Context): NeighborhoodStorage {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NeighborhoodStorage(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
