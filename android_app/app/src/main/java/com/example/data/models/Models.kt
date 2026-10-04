package com.example.data.models

enum class UserRole(val labelArabic: String, val icon: String) {
    CUSTOMER("الزبون", ""),
    SHOP("المتجر", ""),
    DRIVER("السائق", ""),
    ADMIN("الإدارة", "")
}

enum class OrderStatus(val labelArabic: String, val stepIndex: Int) {
    NEW("تم إرسال الطلب", 1),
    SHOP_ACCEPTED("المتجر قبل الطلب", 2),
    PREPARING("قيد التحضير في المطبخ", 2),
    READY_FOR_PICKUP("جاهز للاستلام من السائق", 2),
    ON_THE_WAY("السائق في الطريق إليك", 3),
    DELIVERED("تم التسليم واستلام المبلغ بنجاح", 4),
    CANCELLED("ملغي", 0)
}

data class Neighborhood(
    val nameArabic: String,
    val nameFrench: String,
    val lat: Double,
    val lon: Double
)

object SourElGhozlaneConstants {
    const val CENTER_LAT = 36.1480
    const val CENTER_LON = 3.6900
    const val GEOFENCE_RADIUS_KM = 8.0
    const val FIXED_DELIVERY_FEE = 200 // 200 DA
    const val ZONE_ID = "sour_el_ghozlane"

    val NEIGHBORHOODS = listOf(
        Neighborhood("وسط المدينة", "Centre Ville", 36.1485, 3.6905),
        Neighborhood("حي الوئام", "Hai El Wiame", 36.1520, 3.6960),
        Neighborhood("حي 114 مسكن", "114 Logements", 36.1440, 3.6940),
        Neighborhood("حي ذراع البرج", "Draa El Bordj", 36.1550, 3.6840),
        Neighborhood("حي عين مريم", "Ain Meriem", 36.1410, 3.6850),
        Neighborhood("حي باب الجزائر", "Bab El Djazair", 36.1495, 3.6880),
        Neighborhood("حي باب البوسعادة", "Bab Boussaada", 36.1450, 3.6910),
        Neighborhood("المنطقة الصناعية", "Zone Industrielle", 36.1380, 3.7020),
        Neighborhood("حي النصر", "Hai En-Nasr", 36.1510, 3.7010)
    )
}
