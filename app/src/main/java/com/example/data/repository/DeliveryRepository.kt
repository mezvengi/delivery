package com.example.data.repository

import com.example.data.config.ApiConstants
import com.example.data.local.CustomerOrderHistoryStorage
import com.example.data.local.CustomerPastOrder
import com.example.data.local.SourDeliveryDatabase
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.ShopEntity
import com.example.data.models.OrderStatus
import com.example.data.models.SourElGhozlaneConstants
import com.example.data.network.AddProductRequest
import com.example.data.network.CreateOrderRequest
import com.example.data.network.DriverLocationRequest
import com.example.data.network.OrderItemDto
import com.example.data.network.SoriApiClient
import com.example.data.network.SoriWebSocketEvent
import com.example.data.network.UpdateStatusRequest
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
            syncWithRemoteServer()
            initWebSocketListener()
        }
    }

    private fun initWebSocketListener() {
        try {
            SoriApiClient.webSocketManager.connect()
            scope.launch(Dispatchers.IO) {
                SoriApiClient.webSocketManager.events.collect { event ->
                    when (event) {
                        is SoriWebSocketEvent.DriverLocationUpdated -> {
                            driverDao.updateDriverLocation(event.driverId, event.lat, event.lng, 30.0)
                        }
                        is SoriWebSocketEvent.OrderStatusUpdated -> {
                            val mappedStatus = when (event.status.uppercase()) {
                                "CONFIRMED" -> OrderStatus.NEW.name
                                "PREPARING" -> OrderStatus.PREPARING.name
                                "OUT_FOR_DELIVERY" -> OrderStatus.ON_THE_WAY.name
                                "DELIVERED" -> OrderStatus.DELIVERED.name
                                else -> event.status
                            }
                            orderDao.updateOrderStatus(event.orderId, mappedStatus)
                        }
                        is SoriWebSocketEvent.ConnectionState -> {
                            // Handled
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    private suspend fun syncWithRemoteServer() {
        try {
            val response = SoriApiClient.apiService.getShops()
            val remoteShops = response.shops ?: emptyList()
            if (remoteShops.isNotEmpty()) {
                val entities = remoteShops.map { dto ->
                    ShopEntity(
                        id = dto.id,
                        name = dto.name,
                        category = dto.category,
                        neighborhood = dto.neighborhood ?: "وسط المدينة",
                        address = dto.address ?: dto.address_description ?: "سور الغزلان",
                        lat = dto.lat ?: ApiConstants.CENTER_LAT,
                        lon = dto.lng ?: dto.lon ?: ApiConstants.CENTER_LNG,
                        phone = dto.phone ?: "+213550000000",
                        rating = "4.8",
                        deliveryTime = "20-30 دقيقة",
                        isOpen = dto.is_open ?: true
                    )
                }
                shopDao.insertShops(entities)
            }
        } catch (e: Exception) {
            // Graceful fallback to local Room SQLite data
        }

        try {
            val response = SoriApiClient.apiService.getOnlineDrivers()
            val remoteDrivers = response.drivers ?: emptyList()
            if (remoteDrivers.isNotEmpty()) {
                val driverEntities = remoteDrivers.mapIndexed { idx, dto ->
                    DriverEntity(
                        id = dto.id ?: dto.driver_id ?: (idx + 1).toLong(),
                        name = dto.driver_name ?: dto.name ?: "سائق التوصيل",
                        phone = dto.phone ?: "+213550000000",
                        vehicleType = dto.vehicle_type ?: "دراجة SYM 125",
                        isOnline = dto.is_online ?: true,
                        lat = dto.lat ?: ApiConstants.CENTER_LAT,
                        lon = dto.lng ?: dto.lon ?: ApiConstants.CENTER_LNG,
                        speed = 30.0,
                        rating = "4.8"
                    )
                }
                driverDao.insertDrivers(driverEntities)
            }
        } catch (e: Exception) {
            // Graceful fallback
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
        subtotal: Int,
        orderItems: List<OrderItemDto> = emptyList()
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
            deliveryFee = ApiConstants.FIXED_DELIVERY_FEE,
            total = subtotal + ApiConstants.FIXED_DELIVERY_FEE
        )

        val newOrderId = orderDao.insertOrder(order)

        // Record in the Customer Order History local storage array
        orderHistoryStorage?.addOrder(
            orderId = newOrderId,
            orderNumber = orderNumber,
            shopName = shop.name,
            itemsSummary = itemsSummary,
            totalPrice = subtotal + ApiConstants.FIXED_DELIVERY_FEE,
            deliveryFee = ApiConstants.FIXED_DELIVERY_FEE,
            neighborhood = neighborhood,
            driverName = driver?.name,
            status = "قيد التوصيل"
        )

        // Asynchronously sync with remote server https://sour.serveirc.com/api/orders
        scope.launch(Dispatchers.IO) {
            try {
                val apiItems = if (orderItems.isNotEmpty()) {
                    orderItems
                } else {
                    listOf(
                        OrderItemDto(
                            product_id = 1,
                            quantity = 1,
                            price_da = subtotal
                        )
                    )
                }
                SoriApiClient.apiService.createOrder(
                    CreateOrderRequest(
                        shop_id = shop.id,
                        customer_name = customerName,
                        customer_phone = customerPhone,
                        delivery_address = addressDescription,
                        neighborhood = neighborhood,
                        items = apiItems,
                        driver_id = driver?.id,
                        notes = "طلب عبر تطبيق SGdelivery - سور الغزلان (دفع عند الاستلام)",
                        customer_lat = customerLat,
                        customer_lon = customerLon
                    )
                )
            } catch (e: Exception) {
                // Kept safely in local Room database
            }
        }

        if (driver != null) {
            startDriverTrackingSimulation(newOrderId, driver.id, shop.lat, shop.lon, customerLat, customerLon)
        }
        return newOrderId
    }

    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus) {
        orderDao.updateOrderStatus(orderId, status.name)
        scope.launch(Dispatchers.IO) {
            try {
                val apiStatusStr = when (status) {
                    OrderStatus.NEW -> "CONFIRMED"
                    OrderStatus.SHOP_ACCEPTED, OrderStatus.PREPARING -> "PREPARING"
                    OrderStatus.READY_FOR_PICKUP, OrderStatus.ON_THE_WAY -> "OUT_FOR_DELIVERY"
                    OrderStatus.DELIVERED -> "DELIVERED"
                    OrderStatus.CANCELLED -> "CANCELLED"
                }
                SoriApiClient.apiService.updateOrderStatus(orderId, UpdateStatusRequest(apiStatusStr))
            } catch (e: Exception) {
                // Room database is the primary source of truth
            }
        }
    }

    suspend fun broadcastDriverLocation(lat: Double, lng: Double) {
        scope.launch(Dispatchers.IO) {
            try {
                SoriApiClient.apiService.broadcastDriverLocation(DriverLocationRequest(lat = lat, lng = lng))
            } catch (e: Exception) {
                // Fallback
            }
        }
    }

    suspend fun addProduct(shopId: Long, name: String, description: String, priceDa: Int) {
        val newProductId = System.currentTimeMillis()
        productDao.insertProduct(
            ProductEntity(
                id = newProductId,
                shopId = shopId,
                name = name,
                description = description,
                price = priceDa,
                category = "قائمة الطعام",
                isAvailable = true
            )
        )
        scope.launch(Dispatchers.IO) {
            try {
                SoriApiClient.apiService.addProduct(
                    shopId = shopId,
                    request = AddProductRequest(
                        name = name,
                        description = description,
                        price_da = priceDa
                    )
                )
            } catch (e: Exception) {
                // Room database is primary
            }
        }
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
        // Real data mode: Data is populated from production backend and merchants/drivers.
    }
}
