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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.models.OrderStatus
import com.example.data.repository.DeliveryRepository
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun DriverDashboardScreen(
    driver: DriverEntity,
    orders: List<OrderEntity>,
    repository: DeliveryRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Driver Telemetry & Status
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(driver.vehicleType, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (driver.isOnline) "الحالة: متصل وتبث موقعك المباشر 🛵" else "الحالة: غير متصل ❌",
                                color = if (driver.isOnline) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Switch(
                            checked = driver.isOnline,
                            onCheckedChange = { isOnline ->
                                scope.launch {
                                    repository.updateDriverStatus(driver.id, isOnline)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("📍 إحداثيات سور الغزلان:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                "${String.format("%.4f", driver.lat)}° N, ${String.format("%.4f", driver.lon)}° E",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    val newLat = driver.lat + Random.nextDouble(-0.001, 0.001)
                                    val newLon = driver.lon + Random.nextDouble(-0.001, 0.001)
                                    val speed = Random.nextDouble(20.0, 45.0)
                                    // Update location in repository
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("بث GPS لحظي 📡", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "الطلبات المسندة إليك في سور الغزلان (${orders.size}):",
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
                        text = "لا توجد طلبات معينة لك حالياً، يمكنك إنشاء طلب من حساب الزبون واختيار هذا السائق.",
                        modifier = Modifier.padding(20.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(orders) { order ->
                val currentStatus = try { OrderStatus.valueOf(order.status) } catch (e: Exception) { OrderStatus.NEW }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "طلب: ${order.orderNumber}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = currentStatus.labelArabic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "من: ${order.shopName} ➔ إلى: ${order.neighborhood}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "الزبون: ${order.customerName} (${order.customerPhone})",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "العنوان: ${order.addressDescription}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("المبلغ المطلوب استلامه (نقداً):", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${order.total} دج", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (currentStatus == OrderStatus.READY_FOR_PICKUP || currentStatus == OrderStatus.PREPARING) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository.updateOrderStatus(order.id, OrderStatus.ON_THE_WAY)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("استلام والتحرك 🛵", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else if (currentStatus == OrderStatus.ON_THE_WAY) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                repository.updateOrderStatus(order.id, OrderStatus.DELIVERED)
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("تم التسليم واستلام المبلغ ✅", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
