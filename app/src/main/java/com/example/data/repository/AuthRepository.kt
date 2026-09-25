package com.example.data.repository

import android.content.Context
import com.example.data.config.ApiConstants
import com.example.data.models.AccountStatus
import com.example.data.models.RoleType
import com.example.data.models.UserAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class AuthRepository(context: Context) {
    private val prefs = context.getSharedPreferences("sgdelivery_secure_auth_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val usersList = mutableListOf<UserAccount>()

    init {
        loadUsers()
        loadCurrentSession()
    }

    private suspend fun makePostRequest(endpoint: String, jsonBody: JSONObject): Pair<Int, JSONObject?> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${ApiConstants.BASE_URL}$endpoint")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.setRequestProperty("Accept", "application/json")
            conn.connectTimeout = 7000
            conn.readTimeout = 7000
            conn.doOutput = true

            val os = conn.outputStream
            val writer = OutputStreamWriter(os, "UTF-8")
            writer.write(jsonBody.toString())
            writer.flush()
            writer.close()

            val statusCode = conn.responseCode
            val stream = if (statusCode in 200..299) conn.inputStream else conn.errorStream
            if (stream == null) {
                conn.disconnect()
                return@withContext Pair(statusCode, null)
            }
            val reader = BufferedReader(InputStreamReader(stream, "UTF-8"))
            val response = reader.readText()
            reader.close()
            conn.disconnect()

            val json = if (response.isNotEmpty()) JSONObject(response) else null
            Pair(statusCode, json)
        } catch (e: Exception) {
            Pair(-1, null)
        }
    }

    private fun loadUsers() {
        val jsonString = prefs.getString("registered_users_list", null)
        if (jsonString.isNullOrEmpty()) {
            val defaultUsers = listOf(
                UserAccount(
                    id = "cust-01",
                    name = "أحمد بوزيد",
                    phone = "0550123456",
                    role = RoleType.CUSTOMER,
                    status = AccountStatus.APPROVED,
                    token = "token_cust_12345",
                    address = "حي الوئام، عمارة 4",
                    neighborhood = "حي الوئام"
                ),
                UserAccount(
                    id = "driv-01",
                    name = "أمين منصوري",
                    phone = "0660123456",
                    role = RoleType.DRIVER,
                    status = AccountStatus.APPROVED,
                    token = "token_driv_12345",
                    vehicleType = "دراجة نارية",
                    plateNumber = "12345-126-10",
                    idDocumentAttached = true
                ),
                UserAccount(
                    id = "stor-01",
                    name = "مطعم الأوراس",
                    phone = "0770123456",
                    role = RoleType.STORE,
                    status = AccountStatus.APPROVED,
                    token = "token_stor_12345",
                    storeName = "مطعم الأوراس للشواء والوجبات",
                    storeOwner = "كمال أوراسي",
                    storeType = "مطعم وشواء",
                    address = "شارع الاستقلال، وسط المدينة"
                )
            )
            usersList.addAll(defaultUsers)
            saveUsers()
        } else {
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    usersList.add(
                        UserAccount(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            phone = obj.getString("phone"),
                            role = RoleType.valueOf(obj.getString("role")),
                            status = AccountStatus.valueOf(obj.getString("status")),
                            token = obj.getString("token"),
                            address = obj.optString("address", ""),
                            neighborhood = obj.optString("neighborhood", "وسط المدينة"),
                            vehicleType = obj.optString("vehicleType", ""),
                            plateNumber = obj.optString("plateNumber", ""),
                            idDocumentAttached = obj.optBoolean("idDocumentAttached", false),
                            storeName = obj.optString("storeName", ""),
                            storeOwner = obj.optString("storeOwner", ""),
                            storeType = obj.optString("storeType", ""),
                            storeLat = obj.optDouble("storeLat", 36.1480),
                            storeLon = obj.optDouble("storeLon", 3.6900),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                        )
                    )
                }
            } catch (e: Exception) {
                // Fallback gracefully
            }
        }
    }

    private fun saveUsers() {
        val array = JSONArray()
        for (user in usersList) {
            val obj = JSONObject()
            obj.put("id", user.id)
            obj.put("name", user.name)
            obj.put("phone", user.phone)
            obj.put("role", user.role.name)
            obj.put("status", user.status.name)
            obj.put("token", user.token)
            obj.put("address", user.address)
            obj.put("neighborhood", user.neighborhood)
            obj.put("vehicleType", user.vehicleType)
            obj.put("plateNumber", user.plateNumber)
            obj.put("idDocumentAttached", user.idDocumentAttached)
            obj.put("storeName", user.storeName)
            obj.put("storeOwner", user.storeOwner)
            obj.put("storeType", user.storeType)
            obj.put("storeLat", user.storeLat)
            obj.put("storeLon", user.storeLon)
            obj.put("createdAt", user.createdAt)
            array.put(obj)
        }
        prefs.edit().putString("registered_users_list", array.toString()).apply()
    }

    private fun loadCurrentSession() {
        val currentUserId = prefs.getString("current_session_user_id", null)
        if (!currentUserId.isNullOrEmpty()) {
            _currentUser.value = usersList.find { it.id == currentUserId }
        }
    }

    suspend fun login(phone: String, pass: String): Result<UserAccount> {
        val cleanPhone = normalizePhone(phone)

        // 1. Try Live Backend API
        val requestBody = JSONObject().apply {
            put("phone", cleanPhone)
            put("password", pass)
        }
        val (code, json) = makePostRequest("/auth/login", requestBody)

        if (code == 200 && json != null) {
            val userObj = json.optJSONObject("user")
            val tokensObj = json.optJSONObject("tokens")
            val accessToken = tokensObj?.optString("accessToken") ?: ""
            val refreshToken = tokensObj?.optString("refreshToken") ?: ""

            val roleStr = (userObj?.optString("role") ?: "customer").lowercase()
            val statusStr = (userObj?.optString("status") ?: "active").lowercase()

            val role = when (roleStr) {
                "driver" -> RoleType.DRIVER
                "store", "shop" -> RoleType.STORE
                else -> RoleType.CUSTOMER
            }

            val status = when (statusStr) {
                "pending" -> AccountStatus.PENDING_APPROVAL
                "suspended" -> AccountStatus.REJECTED
                else -> AccountStatus.APPROVED
            }

            val userAccount = UserAccount(
                id = userObj?.optString("id") ?: UUID.randomUUID().toString(),
                name = userObj?.optString("full_name") ?: "مستخدم SGdelivery",
                phone = cleanPhone,
                role = role,
                status = status,
                token = accessToken
            )

            _currentUser.value = userAccount
            prefs.edit().putString("current_session_user_id", userAccount.id).apply()
            prefs.edit().putString("secure_access_token", accessToken).apply()
            prefs.edit().putString("secure_refresh_token", refreshToken).apply()

            // Update local cache
            usersList.removeAll { it.id == userAccount.id || normalizePhone(it.phone) == cleanPhone }
            usersList.add(userAccount)
            saveUsers()

            return Result.success(userAccount)
        } else if (code in 400..499 && json != null) {
            val errMsg = json.optString("error", "بيانات الدخول غير صحيحة")
            return Result.failure(Exception(errMsg))
        }

        // 2. Fallback to Local Offline Data if server is unreachable
        delay(300)
        val localUser = usersList.find { normalizePhone(it.phone) == cleanPhone }
        return if (localUser != null) {
            _currentUser.value = localUser
            prefs.edit().putString("current_session_user_id", localUser.id).apply()
            prefs.edit().putString("secure_access_token", localUser.token).apply()
            Result.success(localUser)
        } else {
            Result.failure(Exception("تعذر الاتصال بالخادم، ورقم الهاتف غير موجود محلياً"))
        }
    }

    suspend fun registerCustomer(
        name: String,
        phone: String,
        pass: String,
        address: String,
        neighborhood: String
    ): Result<UserAccount> {
        val cleanPhone = normalizePhone(phone)

        // 1. Try Live Backend API
        val reqBody = JSONObject().apply {
            put("phone", cleanPhone)
            put("password", pass)
            put("full_name", name.trim())
            put("address", address.trim())
        }
        val (code, json) = makePostRequest("/auth/register/customer", reqBody)

        if (code in 200..201 && json != null) {
            val userObj = json.optJSONObject("user")
            val tokensObj = json.optJSONObject("tokens")
            val token = tokensObj?.optString("accessToken") ?: "jwt_${UUID.randomUUID()}"
            val refreshToken = tokensObj?.optString("refreshToken") ?: ""

            val newUser = UserAccount(
                id = userObj?.optString("id") ?: "cust-${UUID.randomUUID().toString().take(8)}",
                name = name.trim(),
                phone = cleanPhone,
                role = RoleType.CUSTOMER,
                status = AccountStatus.APPROVED,
                token = token,
                address = address.trim(),
                neighborhood = neighborhood
            )

            usersList.add(newUser)
            saveUsers()
            _currentUser.value = newUser
            prefs.edit().putString("current_session_user_id", newUser.id).apply()
            prefs.edit().putString("secure_access_token", token).apply()
            prefs.edit().putString("secure_refresh_token", refreshToken).apply()

            return Result.success(newUser)
        } else if (code in 400..499 && json != null) {
            return Result.failure(Exception(json.optString("error", "فشل تسجيل الزبون")))
        }

        // Fallback
        val newUser = UserAccount(
            id = "cust-${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            phone = cleanPhone,
            role = RoleType.CUSTOMER,
            status = AccountStatus.APPROVED,
            token = "jwt_${UUID.randomUUID()}",
            address = address.trim(),
            neighborhood = neighborhood
        )
        usersList.add(newUser)
        saveUsers()
        _currentUser.value = newUser
        prefs.edit().putString("current_session_user_id", newUser.id).apply()
        return Result.success(newUser)
    }

    suspend fun registerDriver(
        name: String,
        phone: String,
        pass: String,
        vehicleType: String,
        plateNumber: String,
        idDocumentAttached: Boolean
    ): Result<UserAccount> {
        val cleanPhone = normalizePhone(phone)

        val reqBody = JSONObject().apply {
            put("phone", cleanPhone)
            put("password", pass)
            put("full_name", name.trim())
            put("vehicle_type", vehicleType)
            put("license_plate", plateNumber.trim())
        }
        val (code, json) = makePostRequest("/auth/register/driver", reqBody)

        if (code in 200..201 && json != null) {
            val userObj = json.optJSONObject("user")
            val newDriver = UserAccount(
                id = userObj?.optString("id") ?: "driv-${UUID.randomUUID().toString().take(8)}",
                name = name.trim(),
                phone = cleanPhone,
                role = RoleType.DRIVER,
                status = AccountStatus.PENDING_APPROVAL,
                token = "jwt_${UUID.randomUUID()}",
                vehicleType = vehicleType,
                plateNumber = plateNumber.trim(),
                idDocumentAttached = idDocumentAttached
            )
            usersList.add(newDriver)
            saveUsers()
            _currentUser.value = newDriver
            prefs.edit().putString("current_session_user_id", newDriver.id).apply()
            return Result.success(newDriver)
        } else if (code in 400..499 && json != null) {
            return Result.failure(Exception(json.optString("error", "فشل تسجيل السائق")))
        }

        // Fallback
        val newDriver = UserAccount(
            id = "driv-${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            phone = cleanPhone,
            role = RoleType.DRIVER,
            status = AccountStatus.PENDING_APPROVAL,
            token = "jwt_${UUID.randomUUID()}",
            vehicleType = vehicleType,
            plateNumber = plateNumber.trim(),
            idDocumentAttached = idDocumentAttached
        )
        usersList.add(newDriver)
        saveUsers()
        _currentUser.value = newDriver
        prefs.edit().putString("current_session_user_id", newDriver.id).apply()
        return Result.success(newDriver)
    }

    suspend fun registerStore(
        storeName: String,
        ownerName: String,
        phone: String,
        pass: String,
        address: String,
        storeType: String,
        lat: Double,
        lon: Double
    ): Result<UserAccount> {
        val cleanPhone = normalizePhone(phone)

        val reqBody = JSONObject().apply {
            put("phone", cleanPhone)
            put("password", pass)
            put("full_name", ownerName.trim())
            put("store_name", storeName.trim())
            put("category", storeType)
            put("address", address.trim())
        }
        val (code, json) = makePostRequest("/auth/register/store", reqBody)

        if (code in 200..201 && json != null) {
            val userObj = json.optJSONObject("user")
            val newStore = UserAccount(
                id = userObj?.optString("id") ?: "stor-${UUID.randomUUID().toString().take(8)}",
                name = storeName.trim(),
                phone = cleanPhone,
                role = RoleType.STORE,
                status = AccountStatus.PENDING_APPROVAL,
                token = "jwt_${UUID.randomUUID()}",
                address = address.trim(),
                storeName = storeName.trim(),
                storeOwner = ownerName.trim(),
                storeType = storeType,
                storeLat = lat,
                storeLon = lon
            )
            usersList.add(newStore)
            saveUsers()
            _currentUser.value = newStore
            prefs.edit().putString("current_session_user_id", newStore.id).apply()
            return Result.success(newStore)
        } else if (code in 400..499 && json != null) {
            return Result.failure(Exception(json.optString("error", "فشل تسجيل المتجر")))
        }

        // Fallback
        val newStore = UserAccount(
            id = "stor-${UUID.randomUUID().toString().take(8)}",
            name = storeName.trim(),
            phone = cleanPhone,
            role = RoleType.STORE,
            status = AccountStatus.PENDING_APPROVAL,
            token = "jwt_${UUID.randomUUID()}",
            address = address.trim(),
            storeName = storeName.trim(),
            storeOwner = ownerName.trim(),
            storeType = storeType,
            storeLat = lat,
            storeLon = lon
        )
        usersList.add(newStore)
        saveUsers()
        _currentUser.value = newStore
        prefs.edit().putString("current_session_user_id", newStore.id).apply()
        return Result.success(newStore)
    }

    fun logout() {
        val refreshToken = prefs.getString("secure_refresh_token", "")
        _currentUser.value = null
        prefs.edit().remove("current_session_user_id").apply()
        prefs.edit().remove("secure_access_token").apply()
        prefs.edit().remove("secure_refresh_token").apply()
    }

    fun approveAccount(userId: String) {
        val index = usersList.indexOfFirst { it.id == userId }
        if (index != -1) {
            val updated = usersList[index].copy(status = AccountStatus.APPROVED)
            usersList[index] = updated
            saveUsers()
            if (_currentUser.value?.id == userId) {
                _currentUser.value = updated
            }
        }
    }

    private fun normalizePhone(raw: String): String {
        return raw.replace(" ", "")
            .replace("-", "")
            .replace("+213", "0")
            .trim()
    }

    companion object {
        fun isValidAlgerianPhone(phone: String): Boolean {
            val clean = phone.replace(" ", "").replace("-", "").replace("+213", "0").trim()
            return clean.matches(Regex("^(05|06|07)[0-9]{8}$"))
        }

        fun isValidPassword(password: String): Boolean {
            return password.length >= 6
        }
    }
}
