package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var completedOrdersCount by remember { mutableIntStateOf(14) }
    var licensePlate by remember { mutableStateOf("00123-116-10") }
    var driversLicense by remember { mutableStateOf("DL-2024-DZ") }

    // Fetch live driver stats from backend
    LaunchedEffect(driver.id) {
        scope.launch {
            val statsRes = repository.getDriverStats(driver.id)
            statsRes.onSuccess { stats ->
                completedOrdersCount = stats.completed_orders
                if (!stats.license_plate.isNullOrEmpty()) licensePlate = stats.license_plate
                if (!stats.drivers_license.isNullOrEmpty()) driversLicense = stats.drivers_license
            }
        }
    }

    val activeOrders = orders.filter { it.status == "READY_FOR_PICKUP" || it.status == "ON_THE_WAY" || it.status == "PREPARING" }
    val activeOrdersCount = activeOrders.size
    val isCapacityFull = activeOrdersCount >= 2

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ==============================================================
        // 1. بطاقة معلومات وهوية السائق والمركبة (مستوحى من Deliverio)
        // ==============================================================
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsBike,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(driver.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text(
                                    text = "${driver.vehicleType} • ترقيم: $licensePlate",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
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

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    // إحصائيات السائق: عدد الطلبات المكتملة + سعة الطلبات النشطة (2 كحد أقصى)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // شارة السعة النشطة
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCapacityFull) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, if (isCapacityFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "الطلبات النشطة: $activeOrdersCount/2",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (isCapacityFull) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                                if (isCapacityFull) {
                                    Text(" (الحد الأقصى)", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        // شارة الطلبات المكتملة
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFEF3C7)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$completedOrdersCount طلب مكتمل",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF92400E)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 إحداثيات GPS: ${String.format("%.4f", driver.lat)}°, ${String.format("%.4f", driver.lon)}°",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

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
                            Text("تحديث الموقع 📡", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "قائمة الطلبات الجارية في سور الغزلان (${orders.size}):",
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
                        text = "لا توجد طلبات معينة لك حالياً. سيصلك إشعار فوري عند صدور طلب جديد من أحد مطاعم سور الغزلان 🛵",
                        modifier = Modifier.padding(20.dp),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(orders) { order ->
                val currentStatus = try { OrderStatus.valueOf(order.status) } catch (e: Exception) { OrderStatus.NEW }
                val isStageTwo = currentStatus == OrderStatus.ON_THE_WAY

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {

                        // Header: Order Number + Status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "طلب: ${order.orderNumber}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isStageTwo) Color(0xFFE0F2FE) else MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = if (isStageTwo) "في الطريق للزبون 🛵" else "بانتظار الاستلام 🏬",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isStageTwo) Color(0xFF0369A1) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ==============================================================
                        // 2. نظام التوجيه المرحلي ثنائي المراحل للسائق (Deliverio Port)
                        // ==============================================================
                        if (!isStageTwo) {
                            // المرحلة 1: الذهاب إلى المتجر لاستلام الوجبة
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Store,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "المرحلة الأولى: الاستلام من المتجر",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "المتجر: ${order.shopName}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الموقع: وسط مدينة سور الغزلان (قرب ساحة الشهداء)",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = "المسافة المقدرة: ~ 1.5 كم • الوقت المتوقع: 5 دقائق",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        } else {
                            // المرحلة 2: التوجه إلى عنوان الزبون للتسليم
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF15803D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "المرحلة الثانية: التوصيل لعنوان الزبون",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = Color(0xFF15803D)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "الزبون: ${order.customerName} (${order.customerPhone})",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "العنوان: ${order.neighborhood} - ${order.addressDescription}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "المسافة المتبقية: ~ 2.1 كم • وقت الوصول: ~ 7 دقائق",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF15803D),
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // المبلغ المطلوب تحصيله نقداً
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("المبلغ المطلوب تحصيله نقداً (COD):", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                Text("${order.total} دج", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                            }

                            // أزرار الاتصال الهاتفي السريع
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        val targetPhone = if (isStageTwo) order.customerPhone else "+213551111111"
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$targetPhone"))
                                        context.startActivity(dialIntent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isStageTwo) "اتصال بالزبون" else "اتصال بالمتجر", fontSize = 11.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // زر التقدم في مراحل الرحلة
                        if (!isStageTwo) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.updateOrderStatus(order.id, OrderStatus.ON_THE_WAY)
                                        repository.updateDeliveryLeg(order.id, "TO_CUSTOMER", driver.id)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تم استلام الوجبة ➔ التوجه للزبون", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Button(
                                onClick = {
                                    scope.launch {
                                        repository.updateOrderStatus(order.id, OrderStatus.DELIVERED)
                                        repository.updateDeliveryLeg(order.id, "FINISHED", driver.id)
                                        completedOrdersCount += 1
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                modifier = Modifier.fillMaxWidth().height(44.dp)
                            ) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("تم التسليم وتحصيل ${order.total} دج بنجاح ✅", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
