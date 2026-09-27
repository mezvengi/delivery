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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
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

    LaunchedEffect(Unit) {
        refreshPendingUsers()
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
