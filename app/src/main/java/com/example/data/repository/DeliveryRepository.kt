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

    private suspend fun seedInitialDataIfNeeded() {
        if (shopDao.getCount() == 0) {
            val initialShops = listOf(
                ShopEntity(
                    id = 1L,
                    name = "مطعم الأوراس للشواء والوجبات",
                    category = "مطاعم ومأكولات",
                    neighborhood = "وسط المدينة",
                    address = "شارع أول نوفمبر، قرب ساحة البلدية، سور الغزلان",
                    lat = 36.1485,
                    lon = 3.6905,
                    phone = "+213551111111",
                    isOpen = true,
                    deliveryFee = 200,
                    rating = "4.9 ★",
                    deliveryTime = "15-25 دقيقة"
                ),
                ShopEntity(
                    id = 2L,
                    name = "بيتزا وبرغر البرج العائلي",
                    category = "مطاعم ومأكولات",
                    neighborhood = "حي ذراع البرج",
                    address = "حي ذراع البرج، الطريق الرئيسي، سور الغزلان",
                    lat = 36.1550,
                    lon = 3.6840,
                    phone = "+213552222222",
                    isOpen = true,
                    deliveryFee = 200,
                    rating = "4.8 ★",
                    deliveryTime = "20-30 دقيقة"
                ),
                ShopEntity(
                    id = 3L,
                    name = "برغر سيتي & تاكوس فاست فود",
                    category = "وجبات سريعة",
                    neighborhood = "حي 114 مسكن",
                    address = "المجمع السكني 114 مسكن، سور الغزلان",
                    lat = 36.1442,
                    lon = 3.6945,
                    phone = "+213553333331",
                    isOpen = true,
                    deliveryFee = 200,
                    rating = "4.7 ★",
                    deliveryTime = "15-25 دقيقة"
                ),
                ShopEntity(
                    id = 4L,
                    name = "سوبرماركت النور للمواد الغذائية",
                    category = "مواد غذائية وسوبرماركت",
                    neighborhood = "وسط المدينة",
                    address = "نهج الاستقلال قرب البريد المركزي",
                    lat = 36.1478,
                    lon = 3.6915,
                    phone = "+213558888888",
                    isOpen = true,
                    deliveryFee = 200,
                    rating = "4.8 ★",
                    deliveryTime = "20-35 دقيقة"
                ),
                ShopEntity(
                    id = 5L,
                    name = "مخبزة وحلويات الورود البهية",
                    category = "حلويات ومخبوزات",
                    neighborhood = "حي باب الجزائر",
                    address = "قرب مدخل باب الجزائر، سور الغزلان",
                    lat = 36.1498,
                    lon = 3.6885,
                    phone = "+213557777771",
                    isOpen = true,
                    deliveryFee = 200,
                    rating = "5.0 ★",
                    deliveryTime = "15-20 دقيقة"
                ),
                ShopEntity(
                    id = 6L,
                    name = "ساندويشات وكبدة عين مريم",
                    category = "سندويشات ومأكولات",
                    neighborhood = "حي عين مريم",
                    address = "الشارع الرئيسي، حي عين مريم، سور الغزلان",
                    lat = 36.1415,
                    lon = 3.6855,
                    phone = "+213556666661",
                    isOpen = true,
                    deliveryFee = 200,
                    rating = "4.8 ★",
                    deliveryTime = "15-20 دقيقة"
                ),
                ShopEntity(
                    id = 7L,
                    name = "إلكترونيات سيتي للهواتف والتجهيزات",
                    category = "أجهزة إلكترونية وهواتف",
                    neighborhood = "حي ذراع البرج",
                    address = "مقابل الثانوية الجديدة، سور الغزلان",
                    lat = 36.1535,
                    lon = 3.6860,
                    phone = "+213559999999",
                    isOpen = true,
                    deliveryFee = 200,
                    rating = "4.9 ★",
                    deliveryTime = "25-35 دقيقة"
                ),
                ShopEntity(
                    id = 8L,
                    name = "صيدلية الشفاء والمستلزمات الطبية",
                    category = "صيدلية ومستلزمات صحية",
                    neighborhood = "حي 500 مسكن",
                    address = "قرب العيادة متعددة الخدمات، سور الغزلان",
                    lat = 36.1450,
                    lon = 3.6930,
                    phone = "+213556666666",
                    isOpen = true,
                    deliveryFee = 200,
                    rating = "4.9 ★",
                    deliveryTime = "15-25 دقيقة"
                )
            )
            shopDao.insertShops(initialShops)
        }

        if (productDao.getCount() == 0) {
            val initialProducts = listOf(
                // Shop 1 - مطعم الأوراس للشواء
                ProductEntity(
                    id = 101L,
                    shopId = 1L,
                    name = "نصف دجاجة شواء على الفحم",
                    description = "مع بطاطا مقلية وسلاطة مشوية وخبز تقليدي طازج",
                    price = 750,
                    category = "مشويات"
                ),
                ProductEntity(
                    id = 102L,
                    shopId = 1L,
                    name = "طبق شواء لحم خروف بلدي",
                    description = "قطع لحم طازجة مشوية مع التوابل الحارة وسلطة",
                    price = 1200,
                    category = "مشويات"
                ),
                ProductEntity(
                    id = 103L,
                    shopId = 1L,
                    name = "سندويش كبدة مشوية دبل",
                    description = "كبدة عجل طازجة مع توابل جزائرية وسلطة وبطاطا",
                    price = 400,
                    category = "سندويشات"
                ),
                ProductEntity(
                    id = 104L,
                    shopId = 1L,
                    name = "مشروب حمود بوعلام 1 لتر",
                    description = "سيلكتو أو ليمون مثلج منعش",
                    price = 150,
                    category = "مشروبات"
                ),

                // Shop 2 - بيتزا وبرغر البرج
                ProductEntity(
                    id = 201L,
                    shopId = 2L,
                    name = "بيتزا ميغا تشيز 4 أجبان",
                    description = "موزاريلا، غودا، جبن كاممبير وصلصة بيضاء فاخرة",
                    price = 750,
                    category = "بيتزا"
                ),
                ProductEntity(
                    id = 202L,
                    shopId = 2L,
                    name = "بيتزا كاري دجاج إيطالية",
                    description = "صلصة طماطم، جبن موزاريلا، دجاج متبل بصلصة الكاري",
                    price = 650,
                    category = "بيتزا"
                ),
                ProductEntity(
                    id = 203L,
                    shopId = 2L,
                    name = "برغر دبل تشيز بريميوم",
                    description = "لحم بقري صافي محلي مع شيدر دبل وصوص المايونيز",
                    price = 550,
                    category = "برغر"
                ),
                ProductEntity(
                    id = 204L,
                    shopId = 2L,
                    name = "بيتزا مارغريتا كلاسيك",
                    description = "جبن موزاريلا وطماطم وريحان طبيعي مع زيت الزيتون",
                    price = 450,
                    category = "بيتزا"
                ),

                // Shop 3 - برغر سيتي & تاكوس
                ProductEntity(
                    id = 301L,
                    shopId = 3L,
                    name = "تاكوس لارج دجاج ولحم مفروم",
                    description = "مع بطاطا مقلية وصلصة الجبن الذائبة وصوص ألجيريان",
                    price = 650,
                    category = "وجبات سريعة"
                ),
                ProductEntity(
                    id = 302L,
                    shopId = 3L,
                    name = "برغر لحم دبل ميكس تشيز",
                    description = "شريحتان لحم بقري محلي مع بطاطا وصلصة خاصة",
                    price = 500,
                    category = "وجبات سريعة"
                ),
                ProductEntity(
                    id = 303L,
                    shopId = 3L,
                    name = "علبة بطاطا مقلية عائلية مقرمشة",
                    description = "بطاطا مقرمشة ذهبية مع صوص الجبن والكاتشب",
                    price = 200,
                    category = "مقبلات"
                ),

                // Shop 4 - سوبرماركت النور
                ProductEntity(
                    id = 401L,
                    shopId = 4L,
                    name = "زيت المائدة إيليو 5 لتر",
                    description = "زيت نباتي صافي للقلي والطبخ",
                    price = 650,
                    category = "مواد غذائية"
                ),
                ProductEntity(
                    id = 402L,
                    shopId = 4L,
                    name = "كيس سميد سيم ممتاز 10 كلغ",
                    description = "سميد متوسط عالي الجودة للكسكسي والخبز",
                    price = 450,
                    category = "مواد غذائية"
                ),
                ProductEntity(
                    id = 403L,
                    shopId = 4L,
                    name = "جبن مثلثات لافاش كيري علبة 24 قطعة",
                    description = "جبن طري غني بالكالسيوم",
                    price = 380,
                    category = "مشتقات الحليب"
                ),

                // Shop 5 - مخبزة الورود البهية
                ProductEntity(
                    id = 501L,
                    shopId = 5L,
                    name = "تشكيلة حلويات شرقية فاخرة 1 كغ",
                    description = "بقلاوة، مقروط العسل، وقريوش محلي أصيل",
                    price = 950,
                    category = "حلويات"
                ),
                ProductEntity(
                    id = 502L,
                    shopId = 5L,
                    name = "علبة كرواسون وبينيه شوكولا (6 قطع)",
                    description = "مخبوزات طازجة محشوة شوكولا نوتيلا",
                    price = 400,
                    category = "مخبوزات"
                ),
                ProductEntity(
                    id = 503L,
                    shopId = 5L,
                    name = "تارت فراولة وموز طازجة",
                    description = "كريمة باتيسيير فرنسية مع فواكه الموسم",
                    price = 450,
                    category = "حلويات"
                ),

                // Shop 6 - ساندويشات عين مريم
                ProductEntity(
                    id = 601L,
                    shopId = 6L,
                    name = "كاسكروط كبدة حار مع الفريت",
                    description = "خبز باقيت طازج مع كبدة مقلية وسلاطة وهريسة حارة",
                    price = 350,
                    category = "سندويشات"
                ),
                ProductEntity(
                    id = 602L,
                    shopId = 6L,
                    name = "ساندويش سكالوب مشوي مايونيز",
                    description = "صدر دجاج مشوي متبل مع صلصة الثوم والجبن الذائب",
                    price = 400,
                    category = "سندويشات"
                ),

                // Shop 7 - إلكترونيات سيتي
                ProductEntity(
                    id = 701L,
                    shopId = 7L,
                    name = "سماعات بلوتوث لاسلكية عازلة للضوضاء",
                    description = "بطارية تدوم 24 ساعة مع علبة شحن سريعة",
                    price = 2800,
                    category = "أجهزة إلكترونية"
                ),
                ProductEntity(
                    id = 702L,
                    shopId = 7L,
                    name = "باور بانك 20000 ميلي أمبير شحن فائق",
                    description = "منفذان Type-C و USB شحن سريع أصلي",
                    price = 3200,
                    category = "أجهزة إلكترونية"
                ),

                // Shop 8 - صيدلية الشفاء
                ProductEntity(
                    id = 801L,
                    shopId = 8L,
                    name = "حليب أطفال سيريلاك غني بالفيتامينات 400غ",
                    description = "غذاء مكمل مدعم بالحديد والزنك للرضع",
                    price = 580,
                    category = "مستلزمات صحية"
                ),
                ProductEntity(
                    id = 802L,
                    shopId = 8L,
                    name = "جهاز قياس ضغط الدم إلكتروني دقيق",
                    description = "شاشة رقمية واضحة مع قياس نبضات القلب",
                    price = 3500,
                    category = "مستلزمات صحية"
                )
            )
            productDao.insertProducts(initialProducts)
        }

        if (driverDao.getCount() == 0) {
            val initialDrivers = listOf(
                DriverEntity(
                    id = 1L,
                    name = "أمين التوصيل (دراجة نارية SYM)",
                    phone = "+213553333333",
                    vehicleType = "دراجة نارية SYM 125",
                    isOnline = true,
                    lat = 36.1482,
                    lon = 3.6912,
                    speed = 28.5,
                    rating = "4.9 ★"
                ),
                DriverEntity(
                    id = 2L,
                    name = "كريم السريع (سكوتر فوري)",
                    phone = "+213554444444",
                    vehicleType = "سكوتر Peugeot Tweet",
                    isOnline = true,
                    lat = 36.1465,
                    lon = 3.6890,
                    speed = 32.0,
                    rating = "4.8 ★"
                ),
                DriverEntity(
                    id = 3L,
                    name = "ياسين ديليفري (دراجة Cuxi)",
                    phone = "+213556667788",
                    vehicleType = "دراجة VMS Cuxi",
                    isOnline = true,
                    lat = 36.1510,
                    lon = 3.6950,
                    speed = 29.0,
                    rating = "4.9 ★"
                )
            )
            driverDao.insertDrivers(initialDrivers)
        }

        // Preload an active order for real-time tracking demonstration if empty
        if (orderDao.getLatestOrder() == null) {
            orderDao.insertOrder(
                OrderEntity(
                    orderNumber = "SOUR-1092",
                    customerName = "أحمد بوزيد",
                    customerPhone = "0550123456",
                    shopId = 1L,
                    shopName = "مطعم الأوراس للشواء والوجبات",
                    shopLat = 36.1485,
                    shopLon = 3.6905,
                    driverId = 1L,
                    driverName = "أمين التوصيل (دراجة نارية SYM)",
                    driverPhone = "+213553333333",
                    neighborhood = "حي الوئام",
                    addressDescription = "عمارة 4، الطابق 2، سور الغزلان",
                    customerLat = 36.1520,
                    customerLon = 3.6960,
                    status = OrderStatus.ON_THE_WAY.name,
                    itemsSummary = "1x نصف دجاجة شواء على الفحم، 1x مشروب حمود بوعلام",
                    subtotal = 900,
                    deliveryFee = 200,
                    total = 1100,
                    createdAt = System.currentTimeMillis() - 600000
                )
            )
        }
    }
}
