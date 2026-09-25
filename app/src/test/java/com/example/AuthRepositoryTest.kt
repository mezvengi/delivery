package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.models.AccountStatus
import com.example.data.models.RoleType
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuthRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: AuthRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("sori_secure_auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        repository = AuthRepository(context)
    }

    @Test
    fun testDefaultCustomerLoginSuccess() = runBlocking {
        val result = repository.login("0550123456", "123456")
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals(RoleType.CUSTOMER, user?.role)
        assertEquals(AccountStatus.APPROVED, user?.status)
    }

    @Test
    fun testCustomerRegistrationIsAutoApproved() = runBlocking {
        val result = repository.registerCustomer(
            name = "ياسين عماري",
            phone = "0559998877",
            pass = "secret123",
            address = "حي 114 مسكن",
            neighborhood = "حي 114 مسكن"
        )
        assertTrue(result.isSuccess)
        val user = result.getOrNull()
        assertNotNull(user)
        assertEquals(RoleType.CUSTOMER, user?.role)
        assertEquals(AccountStatus.APPROVED, user?.status)
    }

    @Test
    fun testDriverRegistrationIsPendingApproval() = runBlocking {
        val result = repository.registerDriver(
            name = "بلال قادري",
            phone = "0669998877",
            pass = "pass123",
            vehicleType = "دراجة نارية",
            plateNumber = "54321-126-10",
            idDocumentAttached = true
        )
        assertTrue(result.isSuccess)
        val driver = result.getOrNull()
        assertNotNull(driver)
        assertEquals(RoleType.DRIVER, driver?.role)
        assertEquals(AccountStatus.PENDING_APPROVAL, driver?.status)

        // Test manual approval
        repository.approveAccount(driver!!.id)
        val approved = repository.currentUser.value
        assertEquals(AccountStatus.APPROVED, approved?.status)
    }

    @Test
    fun testStoreRegistrationIsPendingApproval() = runBlocking {
        val result = repository.registerStore(
            storeName = "مخبزة وحلويات الأصيل",
            ownerName = "سفيان بن عيسى",
            phone = "0779998877",
            pass = "pass123",
            address = "شارع الاستقلال",
            storeType = "مخبزة وحلويات",
            lat = 36.1480,
            lon = 3.6900
        )
        assertTrue(result.isSuccess)
        val store = result.getOrNull()
        assertNotNull(store)
        assertEquals(RoleType.STORE, store?.role)
        assertEquals(AccountStatus.PENDING_APPROVAL, store?.status)
    }

    @Test
    fun testAlgerianPhoneValidation() {
        assertTrue(AuthRepository.isValidAlgerianPhone("0550123456"))
        assertTrue(AuthRepository.isValidAlgerianPhone("0660123456"))
        assertTrue(AuthRepository.isValidAlgerianPhone("0770123456"))
        assertTrue(AuthRepository.isValidAlgerianPhone("+213550123456"))

        assertFalse(AuthRepository.isValidAlgerianPhone("021345678")) // fixed line
        assertFalse(AuthRepository.isValidAlgerianPhone("123456"))
        assertFalse(AuthRepository.isValidAlgerianPhone("0990123456"))
    }
}
