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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ShopEntity
import com.example.data.models.SourElGhozlaneConstants

@Composable
fun AdminDashboardScreen(
    orders: List<OrderEntity>,
    shops: List<ShopEntity>,
    drivers: List<DriverEntity>,
    modifier: Modifier = Modifier
) {
    val totalRevenue = orders.sumOf { it.total }
    val onlineDriversCount = drivers.count { it.isOnline }
    val openShopsCount = shops.count { it.isOpen }

    LazyColumn(
        modifier = modifier
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
                        text = "نطاق العمليات الجغرافي (Geofence):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• النطاق: بلدية سور الغزلان (ولاية البويرة - 10)\n" +
                               "• الإحداثيات: ${SourElGhozlaneConstants.CENTER_LAT}° N, ${SourElGhozlaneConstants.CENTER_LON}° E\n" +
                               "• نصف القطر: ${SourElGhozlaneConstants.GEOFENCE_RADIUS_KM} كم\n" +
                               "• سعر التوصيل الثابت: ${SourElGhozlaneConstants.FIXED_DELIVERY_FEE} دج\n" +
                               "• الخرائط: OpenStreetMap (مجانية ومفتوحة)",
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
