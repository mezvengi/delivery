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
        if (shopDao.getCount() == 0) {
            val defaultShops = listOf(
                ShopEntity(
                    id = 1,
                    name = "مطعم الأوراس للشواء والوجبات التقليدية",
                    category = "مشويات وأطباق تقليدية",
                    neighborhood = "وسط المدينة",
                    address = "شارع أول نوفمبر، قرب ساحة البلدية، سور الغزلان",
                    lat = 36.1485,
                    lon = 3.6905,
                    phone = "+213551111111",
                    rating = "4.8 ★",
                    deliveryTime = "20-30 دقيقة"
                ),
                ShopEntity(
                    id = 2,
                    name = "بيتزا وبرغر البرج العائلي",
                    category = "بيتزا وفاست فود",
                    neighborhood = "حي ذراع البرج",
                    address = "حي ذراع البرج، الطريق الرئيسي، سور الغزلان",
                    lat = 36.1550,
                    lon = 3.6840,
                    phone = "+213552222222",
                    rating = "4.9 ★",
                    deliveryTime = "15-25 دقيقة"
                ),
                ShopEntity(
                    id = 3,
                    name = "فاست فود ومشاوي الوئام",
                    category = "سندويشات سريعة",
                    neighborhood = "حي الوئام",
                    address = "حي الوئام، بجانب المسجد الجديد، سور الغزلان",
                    lat = 36.1520,
                    lon = 3.6960,
                    phone = "+213556666666",
                    rating = "4.6 ★",
                    deliveryTime = "25-35 دقيقة"
                ),
                ShopEntity(
                    id = 4,
                    name = "حلويات ومخبزة باب الجزائر",
                    category = "حلويات ومخبوزات",
                    neighborhood = "حي باب الجزائر",
                    address = "قرب باب الجزائر التاريخي، سور الغزلان",
                    lat = 36.1495,
                    lon = 3.6880,
                    phone = "+213557777777",
                    rating = "4.9 ★",
                    deliveryTime = "15-20 دقيقة"
                )
            )
            shopDao.insertShops(defaultShops)

            val defaultProducts = listOf(
                ProductEntity(101, 1, "شواء نصف دجاجة على الفحم", "متبل مع خبز طازج وبطاطا مقلية وصلصات حارة وثومية", 750, "مشويات"),
                ProductEntity(102, 1, "سندويش كبدة مشوية دبل", "كبدة عجل طازجة مع توابل جزائرية وسلطة وبطاطا", 400, "سندويشات"),
                ProductEntity(103, 1, "شربة فريك جزائرية بلحم العجل", "شربة فريك تقليدية غنية مع الدبشة والنعناع", 250, "أطباق تقليدية"),
                ProductEntity(104, 1, "مشروب حمود بوعلام 1 لتر", "مشروب غازي جزائري أصيل بارد ومنعش", 150, "مشروبات"),

                ProductEntity(201, 2, "بيتزا كاري كلاسيك فورماج ودبشة", "صلصة طماطم محلية، جبن أحمر، زيتون جزائري ودبشة", 450, "بيتزا"),
                ProductEntity(202, 2, "بيتزا ميغا تشيز 4 أجبان", "موزاريلا، غودا، كاممبير وصلصة بيضاء خاصة", 800, "بيتزا"),
                ProductEntity(203, 2, "برغر لحم دبل ميكس تشيز", "شريحتان لحم بقري محلي مع بطاطا وصلصة خاصة", 500, "برغر"),
                ProductEntity(204, 2, "تاكوس كوردون بلو فرماج لافاشكيري", "تاكوس محشو باللحم والكوردون بلو مع صلصة الجبن", 600, "سندويشات"),

                ProductEntity(301, 3, "سندويش شاورما دجاج مقرمش خبز صاج", "دجاج متبل مع صلصة جزائرية وبطاطا حارة", 350, "سندويشات"),
                ProductEntity(302, 3, "بانيني ميكس لحم وجبن ذائب", "مضغوط على الجريل مع جبن أحمر وسلطة", 380, "سندويشات"),

                ProductEntity(401, 4, "قلب اللوز الجزائري بالسمن والعسل (4 قطع)", "قلب اللوز تقليدي محشي باللوز البلدي مع ماء الزهر", 300, "حلويات"),
                ProductEntity(402, 4, "كرواسون وميلفاي طازج (علبة مشكلة)", "مخبوزات الصباح الفرنسية الطازجة والمورقة", 400, "مخبوزات")
            )
            productDao.insertProducts(defaultProducts)
        }

        if (driverDao.getCount() == 0) {
            val defaultDrivers = listOf(
                DriverEntity(
                    id = 1,
                    name = "أمين التوصيل (دراجة SYM 125)",
                    phone = "+213553333333",
                    vehicleType = "دراجة نارية SYM 125",
                    isOnline = true,
                    lat = 36.1482,
                    lon = 3.6912,
                    speed = 28.0,
                    rating = "4.9 ★"
                ),
                DriverEntity(
                    id = 2,
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
                    id = 3,
                    name = "ياسين التوصيل السريع",
                    phone = "+213559988776",
                    vehicleType = "دراجة نارية VMS Cuxi",
                    isOnline = true,
                    lat = 36.1510,
                    lon = 3.6940,
                    speed = 25.0,
                    rating = "4.7 ★"
                )
            )
            driverDao.insertDrivers(defaultDrivers)
        }
    }
}
