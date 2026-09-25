package com.example.data.models

enum class RoleType(val titleArabic: String, val descriptionArabic: String) {
    CUSTOMER("زبون", "طلب وجبات وتوصيلها إلى باب منزلك"),
    DRIVER("سائق", "توصيل الطلبات وتحقيق دخل يومي إضافي"),
    STORE("متجر", "عرض منتجات متجرك واستقبال طلبات الزبائن")
}

enum class AccountStatus {
    APPROVED,
    PENDING_APPROVAL,
    REJECTED
}

data class UserAccount(
    val id: String,
    val name: String,
    val phone: String,
    val role: RoleType,
    val status: AccountStatus,
    val token: String,
    val address: String = "",
    val neighborhood: String = "وسط المدينة",
    // Driver specific
    val vehicleType: String = "",
    val plateNumber: String = "",
    val idDocumentAttached: Boolean = false,
    // Store specific
    val storeName: String = "",
    val storeOwner: String = "",
    val storeType: String = "",
    val storeLat: Double = SourElGhozlaneConstants.CENTER_LAT,
    val storeLon: Double = SourElGhozlaneConstants.CENTER_LON,
    val createdAt: Long = System.currentTimeMillis()
)
