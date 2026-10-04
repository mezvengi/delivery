package com.example.data.network

import android.util.Log
import com.example.data.config.ApiConstants
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// ==============================================================================
// 1. DTOs: Authentication & Phone Verification (OTP & Passwords)
// ==============================================================================

data class SendOtpRequest(
    val phone: String
)

data class SendOtpResponse(
    val success: Boolean = true,
    val code: String? = null,
    val whatsapp_url: String? = null,
    val telegram_url: String? = null,
    val message: String? = null
)

data class VerifyOtpDtoRequest(
    val phone: String,
    val code: String,
    val full_name: String,
    val role: String,
    val password: String? = null,
    val address: String? = null,
    val vehicle_type: String? = null,
    val license_plate: String? = null,
    val store_category: String? = null
)

data class VerifyPhoneRequest(
    val idToken: String,
    val name: String? = null,
    val full_name: String? = null,
    val role: String? = "customer",
    val password: String? = null,
    val address: String? = null,
    val vehicle_type: String? = null,
    val license_plate: String? = null,
    val store_category: String? = null
)

data class TokensDto(
    val accessToken: String,
    val refreshToken: String? = null
)

data class ApiUserDto(
    val id: Long? = null,
    val phone: String? = null,
    val email: String? = null,
    val firebase_uid: String? = null,
    val photo_url: String? = null,
    val full_name: String? = null,
    val name: String? = null,
    val role: String? = null,
    val status: String? = null, // "active" | "pending"
    val phone_verified: Boolean? = false,
    val address: String? = null,
    val vehicle_type: String? = null,
    val license_plate: String? = null,
    val store_category: String? = null,
    val profile: Map<String, Any?>? = null
) {
    val flatAddress: String? get() = address ?: profile?.get("address") as? String
    val flatVehicleType: String? get() = vehicle_type ?: profile?.get("vehicle_type") as? String
    val flatLicensePlate: String? get() = license_plate ?: profile?.get("license_plate") as? String
    val flatStoreCategory: String? get() = store_category ?: profile?.get("category") as? String
}

data class GoogleAuthRequest(
    val idToken: String,
    val role: String? = "customer"
)

data class GoogleAuthResponse(
    val success: Boolean = true,
    val token: String? = null,
    val needs_phone: Boolean = false,
    val user: ApiUserDto? = null,
    val message: String? = null,
    val error: String? = null
)

data class UpdatePhoneRequest(
    val phone: String
)

data class UpdatePhoneResponse(
    val success: Boolean = true,
    val token: String? = null,
    val message: String? = null,
    val user: ApiUserDto? = null,
    val error: String? = null
)

data class VerifyOtpDtoResponse(
    val success: Boolean = true,
    val tokens: TokensDto? = null,
    val user: ApiUserDto? = null,
    val error: String? = null
)

data class LoginDtoRequest(
    val phone: String,
    val password: String
)

data class MeResponse(
    val success: Boolean = true,
    val user: ApiUserDto? = null,
    val error: String? = null
)

data class RefreshDtoRequest(
    val refreshToken: String
)

// Legacy alias compatibility
typealias RequestOtpRequest = SendOtpRequest
typealias RequestOtpResponse = SendOtpResponse
typealias PasswordLoginRequest = LoginDtoRequest

data class VerifyOtpRequest(
    val phone: String,
    val code: String,
    val name: String? = null
)

data class AuthUserDto(
    val id: Long,
    val name: String,
    val phone: String,
    val role: String
)

data class AuthResponse(
    val success: Boolean,
    val token: String? = null,
    val user: AuthUserDto? = null,
    val error: String? = null
)

// ==============================================================================
// 2. DTOs: Stores & Products
// ==============================================================================

data class ShopDto(
    val id: Long,
    val name: String,
    val category: String,
    val neighborhood: String? = null,
    val address: String? = null,
    val address_description: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val lon: Double? = null,
    val phone: String? = null,
    val is_open: Boolean? = true,
    val delivery_fee: Int? = 200
)

data class ShopsResponse(
    val shops: List<ShopDto>? = null
)

data class ProductDto(
    val id: Long,
    val shop_id: Long? = null,
    val name: String,
    val description: String? = null,
    val price_da: Int? = null,
    val price: Int? = null,
    val category: String? = null,
    val is_available: Boolean? = true,
    val image_url: String? = null
)

data class ProductsResponse(
    val products: List<ProductDto>? = null
)

data class AddProductRequest(
    val name: String,
    val description: String? = null,
    val price: Int,
    val price_da: Int = price,
    val category: String? = "وجبات",
    val image_url: String? = null,
    val shop_id: Long? = null
)

// ==============================================================================
// 3. DTOs: Drivers & GPS Tracking
// ==============================================================================

data class DriverDto(
    val id: Long? = null,
    val driver_id: Long? = null,
    val driver_name: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val vehicle_type: String? = null,
    val is_online: Boolean? = true,
    val lat: Double? = null,
    val lng: Double? = null,
    val lon: Double? = null,
    val distance_km: Double? = null,
    val estimated_pickup_mins: Int? = null
)

data class DriversResponse(
    val drivers: List<DriverDto>? = null
)

data class DriverLocationRequest(
    val lat: Double,
    val lng: Double
)

// ==============================================================================
// 4. DTOs: Orders & Checkout (COD - 200 DA)
// ==============================================================================

data class OrderItemDto(
    val product_id: Long,
    val quantity: Int,
    val price_da: Int
)

data class OrderItemRequest(
    val product_id: Long?,
    val product_name: String,
    val quantity: Int,
    val unit_price: Int
)

data class CreateOrderRequest(
    val shop_id: Long,
    val customer_name: String,
    val customer_phone: String,
    val delivery_address: String,
    val neighborhood: String,
    val items: List<OrderItemDto>,
    val driver_id: Long? = null,
    val notes: String? = null,
    val customer_lat: Double? = ApiConstants.CENTER_LAT,
    val customer_lon: Double? = ApiConstants.CENTER_LNG
)

data class OrderDetailDto(
    val id: Long,
    val order_number: String? = null,
    val customer_name: String? = null,
    val customer_phone: String? = null,
    val shop_id: Long? = null,
    val shop_name: String? = null,
    val shop_neighborhood: String? = null,
    val shop_phone: String? = null,
    val driver_id: Long? = null,
    val neighborhood: String? = null,
    val delivery_address: String? = null,
    val address_description: String? = null,
    val customer_lat: Double? = null,
    val customer_lon: Double? = null,
    val status: String = "CONFIRMED",
    val delivery_leg: String? = "TO_SHOP", // "TO_SHOP" | "TO_CUSTOMER" | "DELIVERED"
    val is_manual_shop_order: Boolean? = false,
    val estimated_distance_km: Double? = null,
    val estimated_duration_min: Int? = null,
    val subtotal: Int = 0,
    val delivery_fee: Int = 200,
    val total: Int = 200,
    val payment_method: String? = "COD",
    val notes: String? = null,
    val created_at: String? = null
)

data class CalculateFeeRequest(
    val shopLat: Double,
    val shopLon: Double,
    val customerLat: Double,
    val customerLon: Double
)

data class CalculateFeeResponse(
    val success: Boolean = true,
    val distance_km: Double = 0.0,
    val delivery_fee: Int = 200,
    val estimated_duration_min: Int = 15,
    val currency: String = "DZD"
)

data class ManualShopOrderRequest(
    val shop_id: Long,
    val customer_name: String,
    val customer_phone: String,
    val neighborhood: String,
    val address_description: String,
    val items_description: String,
    val total_amount: Int,
    val notes: String? = null
)

data class AcceptOrderRequest(
    val driver_id: Long
)

data class AcceptOrderResponse(
    val success: Boolean = true,
    val message: String? = null,
    val order: OrderDetailDto? = null,
    val error: String? = null
)

data class UpdateDeliveryLegRequest(
    val leg: String, // "TO_SHOP" | "TO_CUSTOMER" | "FINISHED"
    val driver_id: Long
)

data class DriverStatsResponse(
    val success: Boolean = true,
    val driver_id: Long = 0,
    val name: String? = null,
    val phone: String? = null,
    val vehicle_type: String? = null,
    val license_plate: String? = null,
    val drivers_license: String? = null,
    val completed_orders: Int = 0,
    val active_orders: Int = 0,
    val max_active_orders: Int = 2
)

data class CreateOrderResponse(
    val success: Boolean? = true,
    val order: OrderDetailDto? = null,
    val order_number: String? = null,
    val id: Long? = null
)

data class UpdateStatusRequest(
    val status: String // "CONFIRMED" | "PREPARING" | "OUT_FOR_DELIVERY" | "DELIVERED"
)

// ==============================================================================
// 5. DTOs: Admin Approvals & Management
// ==============================================================================

data class AdminUserDto(
    val id: Long,
    val name: String,
    val phone: String? = null,
    val role: String,
    val is_active: Boolean = true,
    val status: String = "active",
    val created_at: String? = null
)

data class AllUsersResponse(
    val success: Boolean = true,
    val users: List<AdminUserDto>? = null
)

data class PendingUserDto(
    val id: Long,
    val full_name: String,
    val phone: String? = null,
    val role: String,
    val status: String, // "pending"
    val profile: Map<String, Any?>? = null
)

data class PendingUsersResponse(
    val users: List<PendingUserDto>? = null
)

data class UpdateUserStatusRequest(
    val status: String // "active" | "suspended"
)

// ==============================================================================
// 6. Retrofit Service Interface
// ==============================================================================

interface SoriApiService {
    // 1. Auth & Verification
    @POST("/api/auth/send-otp")
    suspend fun sendOtp(@Body request: SendOtpRequest): SendOtpResponse

    @POST("/api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpDtoRequest): VerifyOtpDtoResponse

    @POST("/api/auth/verify-phone")
    suspend fun verifyPhone(@Body request: VerifyPhoneRequest): VerifyOtpDtoResponse

    @POST("/api/auth/google")
    suspend fun authenticateWithGoogle(@Body request: GoogleAuthRequest): GoogleAuthResponse

    @PATCH("/api/users/me/phone")
    suspend fun updatePhone(@Body request: UpdatePhoneRequest): UpdatePhoneResponse

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginDtoRequest): VerifyOtpDtoResponse

    @GET("/api/auth/me")
    suspend fun getMe(): MeResponse

    @POST("/api/auth/refresh")
    suspend fun refreshToken(@Body request: RefreshDtoRequest): VerifyOtpDtoResponse

    // 2. Stores & Menu
    @GET("/api/shops")
    suspend fun getShops(): ShopsResponse

    @GET("/api/shops/{shopId}/products")
    suspend fun getProductsForShop(@Path("shopId") shopId: Long): ProductsResponse

    @POST("/api/shops/{shopId}/products")
    suspend fun addProduct(
        @Path("shopId") shopId: Long,
        @Body request: AddProductRequest
    ): ProductDto

    // 3. Drivers & GPS
    @GET("/api/drivers")
    suspend fun getOnlineDrivers(): DriversResponse

    @POST("/api/drivers/location")
    suspend fun broadcastDriverLocation(@Body request: DriverLocationRequest): retrofit2.Response<Unit>

    // 4. Orders
    @POST("/api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): CreateOrderResponse

    @POST("/api/orders/calculate-fee")
    suspend fun calculateDeliveryFee(@Body request: CalculateFeeRequest): CalculateFeeResponse

    @POST("/api/orders/shop-manual")
    suspend fun createManualShopOrder(@Body request: ManualShopOrderRequest): CreateOrderResponse

    @POST("/api/orders/{orderId}/accept")
    suspend fun acceptOrder(
        @Path("orderId") orderId: Long,
        @Body request: AcceptOrderRequest
    ): AcceptOrderResponse

    @PATCH("/api/orders/{orderId}/leg")
    suspend fun updateDeliveryLeg(
        @Path("orderId") orderId: Long,
        @Body request: UpdateDeliveryLegRequest
    ): OrderDetailDto

    @GET("/api/drivers/{driverId}/stats")
    suspend fun getDriverStats(@Path("driverId") driverId: Long): DriverStatsResponse

    @GET("/api/orders/{orderId}")
    suspend fun getOrderStatus(@Path("orderId") orderId: Long): OrderDetailDto

    @PATCH("/api/orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: Long,
        @Body request: UpdateStatusRequest
    ): retrofit2.Response<OrderDetailDto>

    // 5. Admin
    @GET("/api/admin/pending-users")
    suspend fun getPendingUsers(): PendingUsersResponse

    @PUT("/api/admin/users/{userId}/status")
    suspend fun updateUserStatus(
        @Path("userId") userId: Long,
        @Body request: UpdateUserStatusRequest
    ): retrofit2.Response<Unit>

    @GET("/api/admin/users")
    suspend fun getAllUsers(
        @retrofit2.http.Query("role") role: String? = null,
        @retrofit2.http.Query("status") status: String? = null,
        @retrofit2.http.Query("search") search: String? = null
    ): AllUsersResponse

    @retrofit2.http.DELETE("/api/admin/users/{userId}")
    suspend fun deleteUser(
        @Path("userId") userId: Long
    ): retrofit2.Response<Unit>

    // Compatibility endpoints
    @POST("/api/auth/request-otp")
    suspend fun requestOtpLegacy(@Body request: SendOtpRequest): SendOtpResponse

    @POST("/api/auth/verify-otp")
    suspend fun verifyOtpLegacy(@Body request: VerifyOtpRequest): AuthResponse

    @POST("/api/auth/login")
    suspend fun loginLegacy(@Body request: PasswordLoginRequest): AuthResponse
}

// ==============================================================================
// 7. WebSocket Live Updates Client (wss://sour.serveirc.com/ws)
// ==============================================================================

sealed class SoriWebSocketEvent {
    data class DriverLocationUpdated(
        val driverId: Long,
        val lat: Double,
        val lng: Double,
        val speed: Double = 25.0,
        val bearing: Float = 0f
    ) : SoriWebSocketEvent()
    data class OrderStatusUpdated(val orderId: Long, val status: String) : SoriWebSocketEvent()
    data class ConnectionState(val isConnected: Boolean) : SoriWebSocketEvent()
}

class SoriWebSocketManager(private val okHttpClient: OkHttpClient) {
    private var webSocket: WebSocket? = null
    private val _events = MutableSharedFlow<SoriWebSocketEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<SoriWebSocketEvent> = _events.asSharedFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    fun connect() {
        if (webSocket != null) return
        val request = Request.Builder()
            .url(ApiConstants.WS_URL)
            .build()

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("SoriWS", "Connected to ${ApiConstants.WS_URL}")
                _isConnected.value = true
                _events.tryEmit(SoriWebSocketEvent.ConnectionState(true))
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "driver_location" -> {
                            val driverId = json.optLong("driver_id", json.optLong("id"))
                            val lat = json.optDouble("lat")
                            val lng = json.optDouble("lng", json.optDouble("lon"))
                            val speed = json.optDouble("speed", 25.0)
                            val bearing = json.optDouble("bearing", 0.0).toFloat()
                            if (lat != 0.0 && lng != 0.0) {
                                _events.tryEmit(SoriWebSocketEvent.DriverLocationUpdated(driverId, lat, lng, speed, bearing))
                            }
                        }
                        "order_status" -> {
                            val orderId = json.optLong("order_id", json.optLong("id"))
                            val status = json.optString("status")
                            if (status.isNotBlank()) {
                                _events.tryEmit(SoriWebSocketEvent.OrderStatusUpdated(orderId, status))
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w("SoriWS", "Failed to parse message: $text", e)
                }
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _isConnected.value = false
                _events.tryEmit(SoriWebSocketEvent.ConnectionState(false))
                this@SoriWebSocketManager.webSocket = null
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("SoriWS", "WebSocket failure: ${t.message}")
                _isConnected.value = false
                _events.tryEmit(SoriWebSocketEvent.ConnectionState(false))
                this@SoriWebSocketManager.webSocket = null
            }
        })
    }

    fun sendDriverLocation(driverId: Long, lat: Double, lng: Double, speed: Double = 25.0, bearing: Float = 0f): Boolean {
        return try {
            val payload = JSONObject().apply {
                put("type", "driver_location")
                put("driver_id", driverId)
                put("lat", lat)
                put("lng", lng)
                put("speed", speed)
                put("bearing", bearing.toDouble())
            }.toString()
            webSocket?.send(payload) ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun disconnect() {
        webSocket?.close(1000, "App closed")
        webSocket = null
        _isConnected.value = false
        _events.tryEmit(SoriWebSocketEvent.ConnectionState(false))
    }
}

// ==============================================================================
// 8. Singleton Client Provider with Token Interceptor
// ==============================================================================

object SoriApiClient {
    @Volatile
    var accessToken: String? = null

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
            accessToken?.let { token ->
                if (token.isNotBlank() && !original.headers.names().contains("Authorization")) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
            }
            chain.proceed(requestBuilder.build())
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: SoriApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SoriApiService::class.java)
    }

    val webSocketManager: SoriWebSocketManager by lazy {
        SoriWebSocketManager(okHttpClient)
    }
}
