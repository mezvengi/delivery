package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.ApiConstants
import com.example.data.local.CustomerLoyaltyStorage
import com.example.data.local.CustomerOrderHistoryStorage
import com.example.data.local.SourDeliveryDatabase
import com.example.data.local.entities.ShopEntity
import com.example.data.models.AccountStatus
import com.example.data.models.RoleType
import com.example.data.models.UserRole
import com.example.data.repository.AuthRepository
import com.example.data.repository.DeliveryRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.CustomerLiveTrackingScreen
import com.example.ui.screens.CustomerOrderHistoryScreen
import com.example.ui.screens.CustomerProfileScreen
import com.example.ui.screens.CustomerShopDetailScreen
import com.example.ui.screens.CustomerShopListScreen
import com.example.ui.screens.DriverDashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.PendingApprovalScreen
import com.example.ui.screens.RegisterScreen
import com.example.ui.screens.RoleSelectionScreen
import com.example.ui.screens.ShopDashboardScreen
import com.example.ui.screens.PhoneAuthScreen
import com.example.ui.screens.GoogleSignInScreen
import com.example.ui.theme.AppThemeMode
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeManager
import com.example.ui.viewmodel.PhoneAuthViewModel
import com.example.ui.viewmodel.GoogleAuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

enum class AuthScreenState {
    ROLE_SELECTION,
    GOOGLE_SIGN_IN,
    LOGIN,
    REGISTER,
    FIREBASE_PHONE_AUTH
}

enum class CustomerScreenState {
    SHOP_LIST,
    SHOP_DETAIL,
    LIVE_TRACKING,
    ORDER_HISTORY,
    PROFILE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        try {
            com.google.firebase.FirebaseApp.initializeApp(applicationContext)
        } catch (e: Throwable) {
            android.util.Log.w("MainActivity", "FirebaseApp init skipped: ${e.message}")
        }

        val database = SourDeliveryDatabase.getDatabase(applicationContext)

        setContent {
            val scope = rememberCoroutineScope()
            val themeManager = remember { ThemeManager(applicationContext) }
            val authRepository = remember { AuthRepository(applicationContext) }
            val loyaltyStorage = remember { CustomerLoyaltyStorage(applicationContext) }
            val orderHistoryStorage = remember { CustomerOrderHistoryStorage(applicationContext) }
            val repository = remember { DeliveryRepository(database, scope, orderHistoryStorage, loyaltyStorage) }

            val isDarkTheme = themeManager.isDarkThemeActive()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                SGdeliveryApp(
                    repository = repository,
                    authRepository = authRepository,
                    themeManager = themeManager,
                    intent = intent
                )
            }
        }
    }
}

@Composable
fun SGdeliveryApp(
    repository: DeliveryRepository,
    authRepository: AuthRepository,
    themeManager: ThemeManager,
    intent: Intent? = null
) {
    val currentUser by authRepository.currentUser.collectAsState()
    val currentThemeMode by themeManager.themeMode.collectAsState()

    val isAdminDeepLink = remember(intent) {
        val uriStr = intent?.data?.toString() ?: ""
        uriStr.contains("admin", ignoreCase = true)
    }

    var authScreenState by remember(isAdminDeepLink) {
        mutableStateOf(if (isAdminDeepLink) AuthScreenState.LOGIN else AuthScreenState.ROLE_SELECTION)
    }
    var selectedAuthRole by remember(isAdminDeepLink) {
        mutableStateOf(if (isAdminDeepLink) RoleType.ADMIN else RoleType.CUSTOMER)
    }
    val scope = rememberCoroutineScope()
    val phoneAuthViewModel: PhoneAuthViewModel = viewModel()
    val googleAuthViewModel = remember { GoogleAuthViewModel(authRepository) }

    // If not logged in, show Auth Flow
    if (currentUser == null) {
        when (authScreenState) {
            AuthScreenState.ROLE_SELECTION -> {
                RoleSelectionScreen(
                    selectedRole = selectedAuthRole,
                    onRoleSelected = { selectedAuthRole = it },
                    onContinueToLogin = { authScreenState = AuthScreenState.LOGIN },
                    onContinueToRegister = { authScreenState = AuthScreenState.REGISTER },
                    onContinueToGoogle = { authScreenState = AuthScreenState.GOOGLE_SIGN_IN },
                    onContinueToPhoneAuth = { authScreenState = AuthScreenState.FIREBASE_PHONE_AUTH },
                    onQuickDemoLogin = { role ->
                        authRepository.loginWithDemoRole(role)
                    },
                    currentThemeMode = currentThemeMode,
                    onThemeModeChanged = { themeManager.setThemeMode(it) }
                )
            }

            AuthScreenState.GOOGLE_SIGN_IN -> {
                GoogleSignInScreen(
                    role = selectedAuthRole,
                    viewModel = googleAuthViewModel,
                    onAuthSuccess = { user ->
                        // Automatically routed via currentUser StateFlow in AuthRepository
                    },
                    onBackClick = {
                        googleAuthViewModel.resetState()
                        authScreenState = AuthScreenState.ROLE_SELECTION
                    }
                )
            }

            AuthScreenState.LOGIN -> {
                LoginScreen(
                    role = selectedAuthRole,
                    authRepository = authRepository,
                    onLoginSuccess = { user ->
                        // Automatically routed via currentUser state flow
                    },
                    onGoToRegister = { authScreenState = AuthScreenState.REGISTER },
                    onGoToGoogle = { authScreenState = AuthScreenState.GOOGLE_SIGN_IN },
                    onGoToPhoneAuth = { authScreenState = AuthScreenState.FIREBASE_PHONE_AUTH },
                    onBack = { authScreenState = AuthScreenState.ROLE_SELECTION }
                )
            }

            AuthScreenState.REGISTER -> {
                RegisterScreen(
                    role = selectedAuthRole,
                    authRepository = authRepository,
                    onRegisterSuccess = { user ->
                        // Automatically routed via currentUser state flow
                    },
                    onGoToLogin = { authScreenState = AuthScreenState.LOGIN },
                    onGoToGoogle = { authScreenState = AuthScreenState.GOOGLE_SIGN_IN },
                    onGoToPhoneAuth = { authScreenState = AuthScreenState.FIREBASE_PHONE_AUTH },
                    onBack = { authScreenState = AuthScreenState.ROLE_SELECTION }
                )
            }

            AuthScreenState.FIREBASE_PHONE_AUTH -> {
                PhoneAuthScreen(
                    viewModel = phoneAuthViewModel,
                    onVerificationSuccess = { idToken, verifiedPhone ->
                        scope.launch {
                            authRepository.verifyPhoneWithFirebase(
                                idToken = idToken,
                                verifiedPhone = verifiedPhone,
                                role = selectedAuthRole
                            )
                        }
                    },
                    onBackClick = {
                        phoneAuthViewModel.resetState()
                        authScreenState = AuthScreenState.ROLE_SELECTION
                    }
                )
            }
        }
        return
    }

    val user = currentUser!!

    // Pending Approval check for new Drivers and Stores
    if (user.status == AccountStatus.PENDING_APPROVAL) {
        PendingApprovalScreen(
            user = user,
            authRepository = authRepository,
            onActivateNowForTesting = {
                authRepository.approveAccount(user.id)
            },
            onLogout = {
                authRepository.logout(this@MainActivity)
                authScreenState = AuthScreenState.ROLE_SELECTION
            }
        )
        return
    }

    // Main App: Strictly bound to the user's logged-in role chosen at login
    val currentRole = when (user.role) {
        RoleType.CUSTOMER -> UserRole.CUSTOMER
        RoleType.DRIVER -> UserRole.DRIVER
        RoleType.STORE -> UserRole.SHOP
        RoleType.ADMIN -> UserRole.ADMIN
    }

    var customerScreenState by remember { mutableStateOf(CustomerScreenState.SHOP_LIST) }
    var selectedShop by remember { mutableStateOf<ShopEntity?>(null) }
    var activeTrackingOrderId by remember { mutableStateOf<Long?>(null) }

    val shops by repository.getAllShops().collectAsState(initial = emptyList())
    val products by repository.getAllProducts().collectAsState(initial = emptyList())
    val drivers by repository.getAllDrivers().collectAsState(initial = emptyList())
    val orders by repository.getAllOrders().collectAsState(initial = emptyList())
    val latestOrder by repository.getLatestOrder().collectAsState(initial = null)
    val simulatedEta by repository.simulatedEtaMinutes.collectAsState()
    val isWebSocketConnected by repository.isWebSocketConnected.collectAsState()
    val orderHistory by repository.getCustomerOrderHistory().collectAsState(initial = emptyList())
    val loyaltyPoints by (repository.loyaltyStorage?.pointsBalance ?: kotlinx.coroutines.flow.MutableStateFlow(0)).collectAsState()

    val trackingOrder = orders.find { it.id == activeTrackingOrderId } ?: latestOrder

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                // Header with App Identity, User Profile, Theme toggle, and Logout
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = when (user.role) {
                                            RoleType.CUSTOMER -> Icons.Default.Person
                                            RoleType.DRIVER -> Icons.Default.TwoWheeler
                                            RoleType.STORE -> Icons.Default.Storefront
                                            RoleType.ADMIN -> Icons.Default.Settings
                                        },
                                        contentDescription = ApiConstants.APP_NAME,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = ApiConstants.APP_NAME,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = user.role.titleArabic,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${user.name} • ${ApiConstants.MUNICIPALITY}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (currentRole == UserRole.CUSTOMER) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFFFFBEB),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.clickable {
                                        customerScreenState = CustomerScreenState.PROFILE
                                    }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("⭐", fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "$loyaltyPoints نقطة",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Quick Theme Mode Toggle
                            IconButton(onClick = {
                                val nextMode = when (currentThemeMode) {
                                    AppThemeMode.SYSTEM -> AppThemeMode.DARK
                                    AppThemeMode.DARK -> AppThemeMode.LIGHT
                                    AppThemeMode.LIGHT -> AppThemeMode.DARK
                                }
                                themeManager.setThemeMode(nextMode)
                            }) {
                                Icon(
                                    imageVector = when (currentThemeMode) {
                                        AppThemeMode.DARK -> Icons.Default.DarkMode
                                        AppThemeMode.LIGHT -> Icons.Default.LightMode
                                        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                    },
                                    contentDescription = "تبديل المظهر",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Logout Button
                            IconButton(onClick = {
                                authRepository.logout(this@MainActivity)
                                authScreenState = AuthScreenState.ROLE_SELECTION
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = "تسجيل الخروج",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentRole) {
                UserRole.CUSTOMER -> {
                    when (customerScreenState) {
                        CustomerScreenState.SHOP_LIST -> {
                            CustomerShopListScreen(
                                shops = shops,
                                products = products,
                                onShopSelected = { shop ->
                                    selectedShop = shop
                                    customerScreenState = CustomerScreenState.SHOP_DETAIL
                                },
                                onOpenOrderHistory = {
                                    customerScreenState = CustomerScreenState.ORDER_HISTORY
                                },
                                onOpenProfile = {
                                    customerScreenState = CustomerScreenState.PROFILE
                                },
                                orderHistoryCount = orderHistory.size,
                                loyaltyPoints = loyaltyPoints
                            )
                        }

                        CustomerScreenState.ORDER_HISTORY -> {
                            CustomerOrderHistoryScreen(
                                orderHistory = orderHistory,
                                shops = shops,
                                onBack = {
                                    customerScreenState = CustomerScreenState.SHOP_LIST
                                },
                                onReorderShop = { shop ->
                                    selectedShop = shop
                                    customerScreenState = CustomerScreenState.SHOP_DETAIL
                                },
                                onTrackOrder = { orderId ->
                                    activeTrackingOrderId = orderId
                                    customerScreenState = CustomerScreenState.LIVE_TRACKING
                                }
                            )
                        }

                        CustomerScreenState.SHOP_DETAIL -> {
                            val activeShop = selectedShop ?: shops.firstOrNull()
                            if (activeShop != null) {
                                val shopProducts by repository.getProductsForShop(activeShop.id)
                                    .collectAsState(initial = emptyList())

                                CustomerShopDetailScreen(
                                    shop = activeShop,
                                    products = shopProducts,
                                    drivers = drivers.filter { it.isOnline },
                                    repository = repository,
                                    onBack = {
                                        customerScreenState = CustomerScreenState.SHOP_LIST
                                    },
                                    onOrderPlaced = { newOrderId ->
                                        activeTrackingOrderId = newOrderId
                                        customerScreenState = CustomerScreenState.LIVE_TRACKING
                                    }
                                )
                            } else {
                                CustomerShopListScreen(
                                    shops = shops,
                                    products = products,
                                    onShopSelected = { shop ->
                                        selectedShop = shop
                                        customerScreenState = CustomerScreenState.SHOP_DETAIL
                                    },
                                    onOpenOrderHistory = {
                                        customerScreenState = CustomerScreenState.ORDER_HISTORY
                                    },
                                    onOpenProfile = {
                                        customerScreenState = CustomerScreenState.PROFILE
                                    },
                                    orderHistoryCount = orderHistory.size,
                                    loyaltyPoints = loyaltyPoints
                                )
                            }
                        }

                        CustomerScreenState.LIVE_TRACKING -> {
                            if (trackingOrder != null) {
                                CustomerLiveTrackingScreen(
                                    order = trackingOrder,
                                    driver = drivers.find { it.id == trackingOrder.driverId },
                                    etaMinutes = simulatedEta,
                                    isWebSocketConnected = isWebSocketConnected,
                                    onSimulateMovement = {
                                        val driverId = trackingOrder.driverId ?: drivers.firstOrNull()?.id ?: 1L
                                        repository.startDriverTrackingSimulation(
                                            orderId = trackingOrder.id,
                                            driverId = driverId,
                                            shopLat = trackingOrder.shopLat,
                                            shopLon = trackingOrder.shopLon,
                                            customerLat = trackingOrder.customerLat,
                                            customerLon = trackingOrder.customerLon
                                        )
                                    },
                                    onBack = {
                                        customerScreenState = CustomerScreenState.SHOP_LIST
                                    }
                                )
                            } else {
                                CustomerShopListScreen(
                                    shops = shops,
                                    products = products,
                                    onShopSelected = { shop ->
                                        selectedShop = shop
                                        customerScreenState = CustomerScreenState.SHOP_DETAIL
                                    },
                                    onOpenOrderHistory = {
                                        customerScreenState = CustomerScreenState.ORDER_HISTORY
                                    },
                                    onOpenProfile = {
                                        customerScreenState = CustomerScreenState.PROFILE
                                    },
                                    orderHistoryCount = orderHistory.size,
                                    loyaltyPoints = loyaltyPoints
                                )
                            }
                        }

                        CustomerScreenState.PROFILE -> {
                            CustomerProfileScreen(
                                customerName = user.name.ifEmpty { "محمد - زبون سور الغزلان" },
                                customerPhone = user.phone.ifEmpty { "0550123456" },
                                neighborhood = user.neighborhood.ifEmpty { "وسط المدينة، سور الغزلان" },
                                loyaltyStorage = repository.loyaltyStorage,
                                orderHistoryCount = orderHistory.size,
                                onBack = {
                                    customerScreenState = CustomerScreenState.SHOP_LIST
                                },
                                onOpenOrderHistory = {
                                    customerScreenState = CustomerScreenState.ORDER_HISTORY
                                }
                            )
                        }
                    }
                }

                UserRole.SHOP -> {
                    val activeShop = selectedShop ?: shops.firstOrNull()
                    if (activeShop != null) {
                        val shopOrders by repository.getOrdersForShop(activeShop.id)
                            .collectAsState(initial = emptyList())
                        val shopProducts by repository.getProductsForShop(activeShop.id)
                            .collectAsState(initial = emptyList())

                        ShopDashboardScreen(
                            shop = activeShop,
                            orders = shopOrders,
                            products = shopProducts,
                            repository = repository
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("جاري تحميل بيانات المتجر...")
                        }
                    }
                }

                UserRole.DRIVER -> {
                    val activeDriver = drivers.firstOrNull()
                    if (activeDriver != null) {
                        val driverOrders by repository.getOrdersForDriver(activeDriver.id)
                            .collectAsState(initial = emptyList())

                        DriverDashboardScreen(
                            driver = activeDriver,
                            orders = driverOrders,
                            repository = repository
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("جاري تحميل بيانات السائق...")
                        }
                    }
                }

                UserRole.ADMIN -> {
                    AdminDashboardScreen(
                        shops = shops,
                        drivers = drivers,
                        orders = orders,
                        authRepository = authRepository
                    )
                }
            }
        }
    }
}

