package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.ApiConstants
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ShopEntity
import com.example.data.network.PendingUserDto
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    orders: List<OrderEntity>,
    shops: List<ShopEntity>,
    drivers: List<DriverEntity>,
    authRepository: AuthRepository? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) }
    var pendingUsers by remember { mutableStateOf<List<PendingUserDto>>(emptyList()) }
    var isLoadingPending by remember { mutableStateOf(false) }

    var allUsers by remember { mutableStateOf<List<com.example.data.network.AdminUserDto>>(emptyList()) }
    var isLoadingAllUsers by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("all") }
    var userToDelete by remember { mutableStateOf<com.example.data.network.AdminUserDto?>(null) }

    fun refreshPendingUsers() {
        if (authRepository == null) return
        isLoadingPending = true
        coroutineScope.launch {
            val result = authRepository.getPendingUsers()
            isLoadingPending = false
            result.onSuccess { list ->
                pendingUsers = list
            }
        }
    }

    fun refreshAllUsers() {
        if (authRepository == null) return
        isLoadingAllUsers = true
        coroutineScope.launch {
            val result = authRepository.getAllUsers()
            isLoadingAllUsers = false
            result.onSuccess { list ->
                allUsers = list
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshPendingUsers()
        refreshAllUsers()
    }

    val totalRevenue = orders.sumOf { it.total }
    val onlineDriversCount = drivers.count { it.isOnline }
    val openShopsCount = shops.count { it.isOpen }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("لوحة العمليات والطلبات", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    refreshPendingUsers()
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("طلبات التفعيل", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (pendingUsers.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${pendingUsers.size})",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    selectedTab = 2
                    refreshAllUsers()
                },
                text = {
                    Text("إدارة الحسابات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            )
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // KPI Grid
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "مجموع الطلبات",
                                value = "${orders.size}",
                                subtitle = "في سور الغزلان",
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "إجمالي المبيعات (COD)",
                                value = "$totalRevenue دج",
                                subtitle = "الدفع نقداً",
                                highlight = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiCard(
                                title = "السائقين المتصلين",
                                value = "$onlineDriversCount",
                                subtitle = "دراجات نارية 🛵",
                                modifier = Modifier.weight(1f)
                            )
                            KpiCard(
                                title = "المتاجر المفتوحة",
                                value = "$openShopsCount",
                                subtitle = "مطاعم ومخابز",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Geofence & System Info Card
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "نطاق العمليات والربط البرمجي:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "• النطاق: بلدية ${ApiConstants.MUNICIPALITY} (منطقة ${ApiConstants.TARGET_ZONE})\n" +
                                       "• إحداثيات المركز: ${ApiConstants.CENTER_LAT}° N, ${ApiConstants.CENTER_LNG}° E\n" +
                                       "• الخادم الرئيسي (REST API): ${ApiConstants.BASE_URL}\n" +
                                       "• التتبع المباشر (WebSocket): ${ApiConstants.WS_URL}\n" +
                                       "• تسعيرة التوصيل الثابتة: ${ApiConstants.FIXED_DELIVERY_FEE} دج (دفع عند الاستلام)",
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = "سجل كافة الطلبات والعمليات المباشرة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                if (orders.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لا توجد عمليات مسجلة حتى الآن.",
                                modifier = Modifier.padding(16.dp),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(orders) { order ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(order.orderNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(order.status, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${order.shopName} ➔ ${order.neighborhood} (${order.customerName})",
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "السائق: ${order.driverName ?: "غير معين"}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("المبلغ: ${order.total} دج", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("دفع عند الاستلام (COD)", fontSize = 11.sp, color = Color(0xFF22C55E))
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Tab 1: Pending Approvals (/api/admin/pending-users)
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "طلبات الحسابات المعلقة (متاجر وسائقين):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { refreshPendingUsers() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "تحديث القائمة")
                        }
                    }
                }

                if (pendingUsers.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (isLoadingPending) "جاري جلب الطلبات المعلقة..." else "لا توجد حسابات معلقة بانتظار الموافقة حالياً.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                } else {
                    items(pendingUsers) { pUser ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = when (pUser.role.lowercase()) {
                                                "driver" -> Icons.Default.TwoWheeler
                                                "store", "shop" -> Icons.Default.Store
                                                else -> Icons.Default.Person
                                            },
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = pUser.full_name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Text(
                                        text = when (pUser.role.lowercase()) {
                                            "driver" -> "سائق توصيل"
                                            "store", "shop" -> "متجر / مطعم"
                                            else -> pUser.role
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (!pUser.phone.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الهاتف: ${pUser.phone}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                pUser.profile?.let { prof ->
                                    val vehicle = prof["vehicle_type"]?.toString()
                                    val plate = prof["license_plate"]?.toString()
                                    val store = prof["store_name"]?.toString()

                                    if (!vehicle.isNullOrBlank() || !plate.isNullOrBlank() || !store.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = listOfNotNull(
                                                vehicle?.let { "المركبة: $it" },
                                                plate?.let { "اللوحة: $it" },
                                                store?.let { "المتجر: $it" }
                                            ).joinToString(" • "),
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                authRepository?.updateUserStatus(pUser.id, "active")
                                                refreshPendingUsers()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = Color(0xFF22C55E).copy(alpha = 0.15f),
                                            contentColor = Color(0xFF15803D)
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("قبول وتفعيل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                authRepository?.updateUserStatus(pUser.id, "suspended")
                                                refreshPendingUsers()
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("رفض / تعليق", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (selectedTab == 2) {
            val filteredUsers = allUsers.filter { u ->
                val matchesRole = when (selectedRoleFilter) {
                    "customer" -> u.role.equals("customer", ignoreCase = true)
                    "shop" -> u.role.equals("shop", ignoreCase = true) || u.role.equals("store", ignoreCase = true)
                    "driver" -> u.role.equals("driver", ignoreCase = true)
                    "suspended" -> !u.is_active
                    else -> true
                }
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    u.name.contains(searchQuery, ignoreCase = true) ||
                    (u.phone?.contains(searchQuery) == true) ||
                    u.role.contains(searchQuery, ignoreCase = true)
                }
                matchesRole && matchesSearch
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "إدارة وحظر وحذف الحسابات (${filteredUsers.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            IconButton(onClick = { refreshAllUsers() }) {
                                Icon(Icons.Default.Refresh, contentDescription = "تحديث")
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("بحث بالاسم أو رقم الهاتف...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("all" to "الكل", "customer" to "زبائن", "shop" to "متاجر", "driver" to "سائقين", "suspended" to "معلقين").forEach { (key, label) ->
                                FilterChip(
                                    selected = selectedRoleFilter == key,
                                    onClick = { selectedRoleFilter = key },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }
                    }
                }

                if (filteredUsers.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لا توجد حسابات مطابقة لمعايير البحث.",
                                modifier = Modifier.padding(24.dp),
                                color = MaterialTheme.colorScheme.outline,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(filteredUsers, key = { it.id }) { u ->
                        val isMainAdmin = u.phone == "0555000000" || u.role.equals("admin", ignoreCase = true)
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val icon = when (u.role.lowercase()) {
                                            "driver" -> Icons.Default.TwoWheeler
                                            "shop", "store" -> Icons.Default.Store
                                            else -> Icons.Default.Person
                                        }
                                        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(u.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    val statusColor = if (u.is_active) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    val statusText = if (u.is_active) "مفعّل" else "معلّق"
                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "الهاتف: ${u.phone ?: "غير متوفر"} | الدور: ${u.role}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                if (!isMainAdmin) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalButton(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val newStatus = if (u.is_active) "suspended" else "active"
                                                    authRepository?.updateUserStatus(u.id, newStatus)
                                                    refreshAllUsers()
                                                }
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = if (u.is_active) Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF22C55E).copy(alpha = 0.15f),
                                                contentColor = if (u.is_active) Color(0xFFB45309) else Color(0xFF15803D)
                                            )
                                        ) {
                                            Text(if (u.is_active) "🛑 تعليق" else "✅ تفعيل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }

                                        OutlinedButton(
                                            onClick = { userToDelete = u },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("حذف", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        userToDelete?.let { targetUser ->
            AlertDialog(
                onDismissRequest = { userToDelete = null },
                title = { Text("تأكيد حذف الحساب", fontWeight = FontWeight.Bold) },
                text = { Text("هل أنت متأكد تماماً من رغبتك في حذف حساب (${targetUser.name}) نهائياً من قاعدة البيانات؟ لا يمكن التراجع عن هذا الإجراء.") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                authRepository?.deleteUser(targetUser.id)
                                userToDelete = null
                                refreshAllUsers()
                            }
                        }
                    ) {
                        Text("نعم، حذف نهائياً", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToDelete = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
