package com.example.data.network

import com.example.data.config.ApiConstants
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Network Data Transfer Objects (DTOs) matching PostgreSQL backend at https://sour.serveirc.com

data class ShopDto(
    val id: Long,
    val name: String,
    val category: String,
    val neighborhood: String,
    val address_description: String?,
    val lat: Double?,
    val lon: Double?,
    val phone: String?,
    val is_open: Boolean?,
    val delivery_fee: Int?
)

data class ProductDto(
    val id: Long,
    val shop_id: Long,
    val name: String,
    val description: String?,
    val price: Int,
    val category: String?,
    val is_available: Boolean?
)

data class DriverDto(
    val id: Long? = null,
    val driver_id: Long? = null,
    val driver_name: String? = null,
    val name: String? = null,
    val phone: String? = null,
    val vehicle_type: String? = null,
    val is_online: Boolean? = true,
    val lat: Double? = null,
    val lon: Double? = null,
    val distance_km: Double? = null,
    val estimated_pickup_mins: Int? = null
)

data class OrderItemRequest(
    val product_id: Long?,
    val product_name: String,
    val quantity: Int,
    val unit_price: Int
)

data class CreateOrderRequest(
    val customer_id: Long? = null,
    val customer_name: String,
    val customer_phone: String,
    val shop_id: Long,
    val driver_id: Long?,
    val neighborhood: String,
    val address_description: String,
    val customer_lat: Double,
    val customer_lon: Double,
    val items: List<OrderItemRequest>,
    val notes: String? = null
)

data class CreateOrderResponse(
    val success: Boolean?,
    val order: OrderDetailDto?,
    val order_number: String?,
    val id: Long?
)

data class OrderDetailDto(
    val id: Long,
    val order_number: String,
    val customer_name: String,
    val customer_phone: String,
    val shop_id: Long,
    val driver_id: Long?,
    val neighborhood: String,
    val address_description: String,
    val customer_lat: Double?,
    val customer_lon: Double?,
    val status: String,
    val subtotal: Int,
    val delivery_fee: Int,
    val total: Int,
    val payment_method: String?,
    val notes: String?,
    val created_at: String?
)

data class RequestOtpRequest(
    val phone: String
)

data class RequestOtpResponse(
    val success: Boolean,
    val message: String?,
    val phone: String?,
    val mockCode: String?
)

data class VerifyOtpRequest(
    val phone: String,
    val code: String,
    val name: String?
)

data class AuthUserDto(
    val id: Long,
    val name: String,
    val phone: String,
    val role: String
)

data class AuthResponse(
    val success: Boolean,
    val token: String?,
    val user: AuthUserDto?,
    val error: String?
)

data class PasswordLoginRequest(
    val phone: String,
    val password: String
)

data class UpdateStatusRequest(
    val status: String
)

interface SoriApiService {
    @GET("/api/shops")
    suspend fun getShops(): List<ShopDto>

    @GET("/api/products")
    suspend fun getProducts(@Query("shop_id") shopId: Long? = null): List<ProductDto>

    @GET("/api/drivers/active")
    suspend fun getActiveDrivers(
        @Query("userLat") userLat: Double? = null,
        @Query("userLon") userLon: Double? = null
    ): List<DriverDto>

    @POST("/api/orders")
    suspend fun createOrder(@Body request: CreateOrderRequest): CreateOrderResponse

    @GET("/api/orders")
    suspend fun getOrders(
        @Query("shop_id") shopId: Long? = null,
        @Query("driver_id") driverId: Long? = null,
        @Query("customer_id") customerId: Long? = null
    ): List<OrderDetailDto>

    @PATCH("/api/orders/{id}/status")
    suspend fun updateOrderStatus(
        @Path("id") orderId: Long,
        @Body request: UpdateStatusRequest
    ): Response<OrderDetailDto>

    @POST("/api/auth/request-otp")
    suspend fun requestOtp(@Body request: RequestOtpRequest): RequestOtpResponse

    @POST("/api/auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): AuthResponse

    @POST("/api/auth/login")
    suspend fun loginWithPassword(@Body request: PasswordLoginRequest): AuthResponse
}

object SoriApiClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val apiService: SoriApiService by lazy {
        Retrofit.Builder()
            .baseUrl(ApiConstants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SoriApiService::class.java)
    }
}
