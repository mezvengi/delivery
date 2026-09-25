package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shops")
data class ShopEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val category: String,
    val neighborhood: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val phone: String,
    val isOpen: Boolean = true,
    val deliveryFee: Int = 200,
    val rating: String = "4.8 ★",
    val deliveryTime: String = "20-30 دقيقة"
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: Long,
    val shopId: Long,
    val name: String,
    val description: String,
    val price: Int, // In DZD (DA)
    val category: String,
    val isAvailable: Boolean = true
)

@Entity(tableName = "drivers")
data class DriverEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val phone: String,
    val vehicleType: String,
    val isOnline: Boolean = true,
    val lat: Double,
    val lon: Double,
    val speed: Double = 0.0,
    val rating: String = "4.9 ★",
    val activeOrderId: Long? = null
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val shopId: Long,
    val shopName: String,
    val shopLat: Double,
    val shopLon: Double,
    val driverId: Long?,
    val driverName: String?,
    val driverPhone: String?,
    val neighborhood: String,
    val addressDescription: String,
    val customerLat: Double,
    val customerLon: Double,
    val status: String, // NEW, SHOP_ACCEPTED, PREPARING, READY_FOR_PICKUP, ON_THE_WAY, DELIVERED
    val itemsSummary: String,
    val subtotal: Int,
    val deliveryFee: Int = 200,
    val total: Int,
    val createdAt: Long = System.currentTimeMillis()
)
