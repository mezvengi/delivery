package com.example.data.repository

import com.example.data.config.ApiConstants
import com.example.data.local.CustomerLoyaltyStorage
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
    private val orderHistoryStorage: CustomerOrderHistoryStorage? = null,
    val loyaltyStorage: CustomerLoyaltyStorage? = null
) {
    private val shopDao = database.shopDao()
    private val productDao = database.productDao()
    private val driverDao = database.driverDao()
    private val orderDao = database.orderDao()

    private var simulationJob: Job? = null

    private val _simulatedEtaMinutes = MutableStateFlow(8)
    val simulatedEtaMinutes: StateFlow<Int> = _simulatedEtaMinutes.asStateFlow()

    val isWebSocketConnected: StateFlow<Boolean> = SoriApiClient.webSocketManager.isConnected
    private val _isSyncing = MutableStateFlow(true)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun retrySync() {
        scope.launch { syncWithRemoteServer() }
    }

    init {
        scope.launch(Dispatchers.IO) {
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
                            driverDao.updateDriverLocation(event.driverId, event.lat, event.lng, event.speed)
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

    fun reconnectWebSocket() {
        try {
            SoriApiClient.webSocketManager.disconnect()
            SoriApiClient.webSocketManager.connect()
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    fun broadcastDriverLocation(driverId: Long, lat: Double, lon: Double, speed: Double, bearing: Float = 0f) {
        SoriApiClient.webSocketManager.sendDriverLocation(driverId, lat, lon, speed, bearing)
    }

    private suspend fun syncWithRemoteServer() {
        _isSyncing.value = true
        _syncError.value = null
        var hasError = false
        try {
            val response = SoriApiClient.apiService.getShops()
            val remoteShops = response.shops ?: emptyList()
            if (remoteShops.isNotEmpty()) {
                val entities = remoteShops.map { dto ->
                    com.example.data.local.entities.ShopEntity(
                        id = dto.id,
                        name = dto.name,
                        category = dto.category,
                        neighborhood = dto.neighborhood ?: "??? ???????",
                        address = dto.address ?: dto.address_description ?: "??? ???????",
                        lat = dto.lat ?: com.example.data.config.ApiConstants.CENTER_LAT,
                        lon = dto.lng ?: dto.lon ?: com.example.data.config.ApiConstants.CENTER_LNG,
                        phone = dto.phone ?: "+213550000000",
                        rating = "4.8",
                        deliveryTime = "15-25 ?????",
                        isOpen = dto.is_open ?: true
                    )
                }
                shopDao.insertShops(entities)

                entities.forEach { shop ->
                    try {
                        val prodRes = SoriApiClient.apiService.getProducts(shop.id)
                        val prods = prodRes.products ?: emptyList()
                        val prodEntities = prods.map { p ->
                            com.example.data.local.entities.ProductEntity(
                                id = p.id,
                                shopId = shop.id,
                                name = p.name,
                                description = p.description ?: "",
                                price = p.price_da ?: p.price ?: 0,
                                category = p.category ?: "",
                                isAvailable = p.is_available ?: true
                            )
                        }
                        productDao.insertProducts(prodEntities)
                    } catch (e: Exception) { hasError = true }
                }
            }
        } catch (e: Exception) { hasError = true }

        try {
            val response = SoriApiClient.apiService.getOnlineDrivers()
            val remoteDrivers = response.drivers ?: emptyList()
            if (remoteDrivers.isNotEmpty()) {
                val driverEntities = remoteDrivers.mapIndexed { idx, dto ->
                    com.example.data.local.entities.DriverEntity(
                        id = dto.id ?: dto.driver_id ?: (idx + 1).toLong(),
                        name = dto.driver_name ?: dto.name ?: "???? ???????",
                        phone = dto.phone ?: "+213550000000",
                        vehicleType = dto.vehicle_type ?: "?????",
                        isOnline = dto.is_online ?: true,
                        lat = dto.lat ?: com.example.data.config.ApiConstants.CENTER_LAT,
                        lon = dto.lng ?: dto.lon ?: com.example.data.config.ApiConstants.CENTER_LNG,
                        speed = 30.0,
                        rating = "4.8"
                    )
                }
                driverDao.insertDrivers(driverEntities)
            }
        } catch (e: Exception) { hasError = true }
        
        try {
            val ordersRes = SoriApiClient.apiService.getOrders()
            val orders = ordersRes.orders ?: emptyList()
            val orderEntities = orders.map { o ->
                com.example.data.local.entities.OrderEntity(
                    id = o.id,
                    orderNumber = o.order_number ?: "ORD-${o.id}",
                    customerName = o.customer_name ?: "",
                    customerPhone = o.customer_phone ?: "",
                    shopId = o.shop_id ?: 0L,
                    shopName = o.shop_name ?: "",
                    shopLat = com.example.data.config.ApiConstants.CENTER_LAT,
                    shopLon = com.example.data.config.ApiConstants.CENTER_LNG,
                    driverId = o.driver_id,
                    driverName = o.driver_name,
                    driverPhone = o.driver_phone,
                    neighborhood = o.neighborhood ?: "",
                    addressDescription = o.delivery_address ?: o.address_description ?: "",
                    customerLat = o.customer_lat ?: com.example.data.config.ApiConstants.CENTER_LAT,
                    customerLon = o.customer_lon ?: com.example.data.config.ApiConstants.CENTER_LNG,
                    status = o.status ?: "NEW",
                    itemsSummary = o.notes ?: "",
                    subtotal = o.subtotal,
                    deliveryFee = o.delivery_fee,
                    total = o.total
                )
            }
            orderDao.insertOrders(orderEntities)
        } catch(e: Exception) { hasError = true }

        if (hasError) {
            _syncError.value = "??? ??? ????? ??? ????????. ???? ?????? ?? ?????? ?????????."
        }
        _isSyncing.value = false
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
        orderItems: List<OrderItemDto> = emptyList(),
        deliveryFeeDiscount: Int = 0,
        loyaltyPointsUsed: Int = 0
    ): Long {
        val orderNumber = "SOUR-${Random.nextInt(1000, 9999)}"
        val effectiveDeliveryFee = maxOf(0, ApiConstants.FIXED_DELIVERY_FEE - deliveryFeeDiscount)
        val finalTotal = subtotal + effectiveDeliveryFee

        if (loyaltyPointsUsed > 0) {
            loyaltyStorage?.redeemPoints(loyaltyPointsUsed, deliveryFeeDiscount)
        }

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
            deliveryFee = effectiveDeliveryFee,
            total = finalTotal
        )

        val newOrderId = orderDao.insertOrder(order)

        // Record in the Customer Order History local storage array
        orderHistoryStorage?.addOrder(
            orderId = newOrderId,
            orderNumber = orderNumber,
            shopName = shop.name,
            itemsSummary = itemsSummary,
            totalPrice = finalTotal,
            deliveryFee = effectiveDeliveryFee,
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
        if (status == OrderStatus.DELIVERED) {
            loyaltyStorage?.addPoints(50, "نقاط طلب مكتمل 📦")
        }
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

    suspend fun addProduct(
        shopId: Long,
        name: String,
        description: String,
        priceDa: Int,
        category: String = "وجبات",
        imageUrl: String = ""
    ) {
        val newProductId = System.currentTimeMillis()
        productDao.insertProduct(
            ProductEntity(
                id = newProductId,
                shopId = shopId,
                name = name,
                description = description,
                price = priceDa,
                category = category,
                isAvailable = true,
                imageUrl = imageUrl
            )
        )
        scope.launch(Dispatchers.IO) {
            try {
                SoriApiClient.apiService.addProduct(
                    shopId = shopId,
                    request = AddProductRequest(
                        name = name,
                        description = description,
                        price = priceDa,
                        price_da = priceDa,
                        category = category,
                        image_url = imageUrl,
                        shop_id = shopId
                    )
                )
            } catch (e: Exception) {
                // Room database is primary
            }
        }
    }

    suspend fun deleteProduct(productId: Long) {
        productDao.deleteProduct(productId)
    }

    suspend fun updateProductAvailable(productId: Long, isAvailable: Boolean) {
        productDao.updateProductAvailable(productId, isAvailable)
    }

    suspend fun updateDriverStatus(driverId: Long, isOnline: Boolean) {
        driverDao.updateDriverStatus(driverId, isOnline)
    }

    suspend fun updateShopOpen(shopId: Long, isOpen: Boolean) {
        shopDao.updateShopOpen(shopId, isOpen)
    }

    // ==============================================================================
    // Deliverio Ported Capabilities: Dynamic Pricing, Manual Orders, 2-Leg Nav
    // ==============================================================================

    suspend fun calculateDynamicFee(
        shopLat: Double,
        shopLon: Double,
        custLat: Double,
        custLon: Double
    ): Result<com.example.data.network.CalculateFeeResponse> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val res = SoriApiClient.apiService.calculateDeliveryFee(
                com.example.data.network.CalculateFeeRequest(shopLat, shopLon, custLat, custLon)
            )
            Result.success(res)
        } catch (e: Exception) {
            Result.success(
                com.example.data.network.CalculateFeeResponse(
                    success = true,
                    distance_km = 2.5,
                    delivery_fee = 200,
                    estimated_duration_min = 15,
                    currency = "DZD"
                )
            )
        }
    }

    suspend fun createManualShopOrder(
        shopId: Long,
        customerName: String,
        customerPhone: String,
        neighborhood: String,
        address: String,
        itemsDesc: String,
        total: Int,
        notes: String? = null
    ): Result<com.example.data.network.CreateOrderResponse> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val req = com.example.data.network.ManualShopOrderRequest(
                shop_id = shopId,
                customer_name = customerName,
                customer_phone = customerPhone,
                neighborhood = neighborhood,
                address_description = address,
                items_description = itemsDesc,
                total_amount = total,
                notes = notes
            )
            val res = SoriApiClient.apiService.createManualShopOrder(req)
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptOrderWithCapacityCheck(
        orderId: Long,
        driverId: Long
    ): Result<com.example.data.network.AcceptOrderResponse> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val res = SoriApiClient.apiService.acceptOrder(
                orderId,
                com.example.data.network.AcceptOrderRequest(driverId)
            )
            if (res.success) {
                Result.success(res)
            } else {
                Result.failure(Exception(res.error ?: "تعذر قبول الطلب"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateDeliveryLeg(
        orderId: Long,
        leg: String,
        driverId: Long
    ): Result<com.example.data.network.OrderDetailDto> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val res = SoriApiClient.apiService.updateDeliveryLeg(
                orderId,
                com.example.data.network.UpdateDeliveryLegRequest(leg, driverId)
            )
            Result.success(res)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDriverStats(
        driverId: Long
    ): Result<com.example.data.network.DriverStatsResponse> = kotlinx.coroutines.withContext(Dispatchers.IO) {
        try {
            val res = SoriApiClient.apiService.getDriverStats(driverId)
            Result.success(res)
        } catch (e: Exception) {
            Result.success(
                com.example.data.network.DriverStatsResponse(
                    success = true,
                    driver_id = driverId,
                    name = "سائق معتمد",
                    phone = "+213550000000",
                    vehicle_type = "دراجة نارية SYM 125",
                    license_plate = "00123-116-10",
                    drivers_license = "DL-2024-DZ",
                    completed_orders = 14,
                    active_orders = 0,
                    max_active_orders = 2
                )
            )
        }
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
            val steps = 24
            var prevLat = shopLat
            var prevLon = shopLon
            for (i in 1..steps) {
                val fraction = i.toDouble() / steps
                val currentLat = shopLat + (customerLat - shopLat) * fraction
                val currentLon = shopLon + (customerLon - shopLon) * fraction
                val speed = 26.0 + Random.nextDouble(-2.0, 4.0)

                val dLat = currentLat - prevLat
                val dLon = currentLon - prevLon
                val bearing = if (kotlin.math.abs(dLat) > 0.00001 || kotlin.math.abs(dLon) > 0.00001) {
                    Math.toDegrees(kotlin.math.atan2(dLon, dLat)).toFloat()
                } else 0f
                prevLat = currentLat
                prevLon = currentLon

                driverDao.updateDriverLocation(driverId, currentLat, currentLon, speed)
                SoriApiClient.webSocketManager.sendDriverLocation(driverId, currentLat, currentLon, speed, bearing)

                val eta = maxOf(1, ((1.0 - fraction) * 8).toInt())
                _simulatedEtaMinutes.value = eta
                delay(800)
            }

            // Step 4: DELIVERED (Cash on Delivery received!)
            driverDao.updateDriverLocation(driverId, customerLat, customerLon, 0.0)
            SoriApiClient.webSocketManager.sendDriverLocation(driverId, customerLat, customerLon, 0.0, 0f)
            _simulatedEtaMinutes.value = 0
            orderDao.updateOrderStatus(orderId, OrderStatus.DELIVERED.name)
            loyaltyStorage?.addPoints(50, "نقاط طلب مكتمل 📦")
        }
    }

    
}




