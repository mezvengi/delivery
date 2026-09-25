package com.example.data.repository

import com.example.data.local.CustomerOrderHistoryStorage
import com.example.data.local.CustomerPastOrder
import com.example.data.local.SourDeliveryDatabase
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.ShopEntity
import com.example.data.models.OrderStatus
import com.example.data.models.SourElGhozlaneConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class DeliveryRepository(
    private val database: SourDeliveryDatabase,
    private val scope: CoroutineScope,
    private val orderHistoryStorage: CustomerOrderHistoryStorage? = null
) {
    private val shopDao = database.shopDao()
    private val productDao = database.productDao()
    private val driverDao = database.driverDao()
    private val orderDao = database.orderDao()

    private var simulationJob: Job? = null

    private val _simulatedEtaMinutes = MutableStateFlow(8)
    val simulatedEtaMinutes: StateFlow<Int> = _simulatedEtaMinutes.asStateFlow()

    init {
        scope.launch(Dispatchers.IO) {
            seedInitialDataIfNeeded()
        }
    }

    fun getAllShops(): Flow<List<ShopEntity>> = shopDao.getAllShops()
    fun getShopById(id: Long): Flow<ShopEntity?> = shopDao.getShopById(id)
    fun getAllProducts(): Flow<List<ProductEntity>> = productDao.getAllProducts()
    fun getProductsForShop(shopId: Long): Flow<List<ProductEntity>> = productDao.getProductsForShop(shopId)
    fun getAllDrivers(): Flow<List<DriverEntity>> = driverDao.getAllDrivers()
    fun getOnlineDrivers(): Flow<List<DriverEntity>> = driverDao.getOnlineDrivers()
    fun getDriverById(id: Long): Flow<DriverEntity?> = driverDao.getDriverById(id)
    fun getAllOrders(): Flow<List<OrderEntity>> = orderDao.getAllOrders()
    fun getLatestOrder(): Flow<OrderEntity?> = orderDao.getLatestOrder()
    fun getOrderById(id: Long): Flow<OrderEntity?> = orderDao.getOrderById(id)
    fun getOrdersForShop(shopId: Long): Flow<List<OrderEntity>> = orderDao.getOrdersForShop(shopId)
    fun getOrdersForDriver(driverId: Long): Flow<List<OrderEntity>> = orderDao.getOrdersForDriver(driverId)
    fun getCustomerOrderHistory(): Flow<List<CustomerPastOrder>> =
        orderHistoryStorage?.orderHistory ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun createOrder(
        customerName: String,
        customerPhone: String,
        shop: ShopEntity,
        driver: DriverEntity?,
        neighborhood: String,
        addressDescription: String,
        customerLat: Double,
        customerLon: Double,
        itemsSummary: String,
        subtotal: Int
    ): Long {
        val orderNumber = "SOUR-${Random.nextInt(1000, 9999)}"
        val order = OrderEntity(
            orderNumber = orderNumber,
            customerName = customerName,
            customerPhone = customerPhone,
            shopId = shop.id,
            shopName = shop.name,
            shopLat = shop.lat,
            shopLon = shop.lon,
            driverId = driver?.id,
            driverName = driver?.name,
            driverPhone = driver?.phone,
            neighborhood = neighborhood,
            addressDescription = addressDescription,
            customerLat = customerLat,
            customerLon = customerLon,
            status = OrderStatus.NEW.name,
            itemsSummary = itemsSummary,
            subtotal = subtotal,
            deliveryFee = SourElGhozlaneConstants.FIXED_DELIVERY_FEE,
            total = subtotal + SourElGhozlaneConstants.FIXED_DELIVERY_FEE
        )

        val newOrderId = orderDao.insertOrder(order)

        // Record in the Customer Order History local storage array
        orderHistoryStorage?.addOrder(
            orderId = newOrderId,
            orderNumber = orderNumber,
            shopName = shop.name,
            itemsSummary = itemsSummary,
            totalPrice = subtotal + SourElGhozlaneConstants.FIXED_DELIVERY_FEE,
            deliveryFee = SourElGhozlaneConstants.FIXED_DELIVERY_FEE,
            neighborhood = neighborhood,
            driverName = driver?.name,
            status = "قيد التوصيل 🛵"
        )

        if (driver != null) {
            startDriverTrackingSimulation(newOrderId, driver.id, shop.lat, shop.lon, customerLat, customerLon)
        }
        return newOrderId
    }

    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        orderDao.updateOrderStatus(orderId, status.name)
    }

    suspend fun updateDriverStatus(driverId: Long, isOnline: Boolean) {
        driverDao.updateDriverStatus(driverId, isOnline)
    }

    suspend fun updateShopOpen(shopId: Long, isOpen: Boolean) {
        shopDao.updateShopOpen(shopId, isOpen)
    }

    suspend fun assignDriver(orderId: Long, driver: DriverEntity) {
        orderDao.assignDriver(orderId, driver.id, driver.name, driver.phone)
    }

    fun startDriverTrackingSimulation(
        orderId: Long,
        driverId: Long,
        shopLat: Double,
        shopLon: Double,
        customerLat: Double,
        customerLon: Double
    ) {
        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.IO) {
            // Step 1: NEW (Wait 3 seconds)
            orderDao.updateOrderStatus(orderId, OrderStatus.NEW.name)
            driverDao.updateDriverLocation(driverId, shopLat + 0.002, shopLon - 0.002, 0.0)
            _simulatedEtaMinutes.value = 12
            delay(3000)

            // Step 2: PREPARING (Cooking at the restaurant - 4 seconds)
            orderDao.updateOrderStatus(orderId, OrderStatus.PREPARING.name)
            driverDao.updateDriverLocation(driverId, shopLat, shopLon, 0.0)
            _simulatedEtaMinutes.value = 10
            delay(4000)

            // Step 3: ON_THE_WAY (Driver moving along Sour El Ghozlane towards Customer)
            orderDao.updateOrderStatus(orderId, OrderStatus.ON_THE_WAY.name)
            val steps = 15
            for (i in 1..steps) {
                val fraction = i.toDouble() / steps
                val currentLat = shopLat + (customerLat - shopLat) * fraction
                val currentLon = shopLon + (customerLon - shopLon) * fraction
                val speed = 25.0 + Random.nextDouble(-3.0, 5.0)
                driverDao.updateDriverLocation(driverId, currentLat, currentLon, speed)

                val eta = maxOf(1, ((1.0 - fraction) * 8).toInt())
                _simulatedEtaMinutes.value = eta
                delay(1200)
            }

            // Step 4: DELIVERED (Cash on Delivery received!)
            driverDao.updateDriverLocation(driverId, customerLat, customerLon, 0.0)
            _simulatedEtaMinutes.value = 0
            orderDao.updateOrderStatus(orderId, OrderStatus.DELIVERED.name)
        }
    }

    private suspend fun seedInitialDataIfNeeded() {
        // Real data mode: Do not seed mock shops, products, or drivers.
        // Data is populated by registered merchants, drivers, and orders.
    }
}
