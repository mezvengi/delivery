package com.example.data.local.daos

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.ShopEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShopDao {
    @Query("SELECT * FROM shops ORDER BY isOpen DESC, name ASC")
    fun getAllShops(): Flow<List<ShopEntity>>

    @Query("SELECT * FROM shops WHERE id = :id LIMIT 1")
    fun getShopById(id: Long): Flow<ShopEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShops(shops: List<ShopEntity>)

    @Query("UPDATE shops SET isOpen = :isOpen WHERE id = :id")
    suspend fun updateShopOpen(id: Long, isOpen: Boolean)

    @Query("SELECT COUNT(*) FROM shops")
    suspend fun getCount(): Int
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE shopId = :shopId ORDER BY category ASC, price ASC")
    fun getProductsForShop(shopId: Long): Flow<List<ProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Query("UPDATE products SET isAvailable = :isAvailable WHERE id = :id")
    suspend fun updateProductAvailable(id: Long, isAvailable: Boolean)

    @Query("SELECT COUNT(*) FROM products")
    suspend fun getCount(): Int
}

@Dao
interface DriverDao {
    @Query("SELECT * FROM drivers")
    fun getAllDrivers(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE isOnline = 1")
    fun getOnlineDrivers(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE id = :id LIMIT 1")
    fun getDriverById(id: Long): Flow<DriverEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrivers(drivers: List<DriverEntity>)

    @Query("UPDATE drivers SET isOnline = :isOnline WHERE id = :id")
    suspend fun updateDriverStatus(id: Long, isOnline: Boolean)

    @Query("UPDATE drivers SET lat = :lat, lon = :lon, speed = :speed WHERE id = :id")
    suspend fun updateDriverLocation(id: Long, lat: Double, lon: Double, speed: Double)

    @Query("SELECT COUNT(*) FROM drivers")
    suspend fun getCount(): Int
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY id DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    fun getOrderById(id: Long): Flow<OrderEntity?>

    @Query("SELECT * FROM orders ORDER BY id DESC LIMIT 1")
    fun getLatestOrder(): Flow<OrderEntity?>

    @Query("SELECT * FROM orders WHERE shopId = :shopId ORDER BY id DESC")
    fun getOrdersForShop(shopId: Long): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE driverId = :driverId OR (status = 'READY_FOR_PICKUP' AND driverId IS NULL) ORDER BY id DESC")
    fun getOrdersForDriver(driverId: Long): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long

    @Query("UPDATE orders SET status = :status WHERE id = :id")
    suspend fun updateOrderStatus(id: Long, status: String)

    @Query("UPDATE orders SET driverId = :driverId, driverName = :driverName, driverPhone = :driverPhone, status = 'ON_THE_WAY' WHERE id = :orderId")
    suspend fun assignDriver(orderId: Long, driverId: Long, driverName: String, driverPhone: String)
}
