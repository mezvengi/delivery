package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.OrderStatus
import com.example.data.models.UserRole

@Composable
fun RoleSwitcherBar(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserRole.values().forEach { role ->
                val isSelected = role == selectedRole
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    label = "chipBg"
                )
                val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(bgColor)
                        .clickable { onRoleSelected(role) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val roleIcon = when (role) {
                        UserRole.CUSTOMER -> Icons.Default.Person
                        UserRole.SHOP -> Icons.Default.Storefront
                        UserRole.DRIVER -> Icons.Default.TwoWheeler
                        UserRole.ADMIN -> Icons.Default.Settings
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = roleIcon,
                            contentDescription = role.labelArabic,
                            tint = textColor,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                        Text(
                            text = role.labelArabic,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = textColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusStepper(
    currentStatus: OrderStatus,
    modifier: Modifier = Modifier
) {
    val currentStep = currentStatus.stepIndex

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Step 1
            StepNode(stepNumber = 1, title = "تم الطلب", isActive = currentStep >= 1, isCurrent = currentStep == 1)
            StepConnector(isActive = currentStep > 1)
            // Step 2
            StepNode(stepNumber = 2, title = "تحضير المتجر", isActive = currentStep >= 2, isCurrent = currentStep == 2)
            StepConnector(isActive = currentStep > 2)
            // Step 3
            StepNode(stepNumber = 3, title = "قيد التوصيل", isActive = currentStep >= 3, isCurrent = currentStep == 3)
            StepConnector(isActive = currentStep > 3)
            // Step 4
            StepNode(stepNumber = 4, title = "تم التسليم", isActive = currentStep >= 4, isCurrent = currentStep == 4)
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "الحالة الآن: ${currentStatus.labelArabic}",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "سور الغزلان (دفع نقدي)",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StepNode(
    stepNumber: Int,
    title: String,
    isActive: Boolean,
    isCurrent: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val circleColor by animateColorAsState(
            targetValue = when {
                isCurrent -> MaterialTheme.colorScheme.primary
                isActive -> Color(0xFF22C55E) // Completed green
                else -> MaterialTheme.colorScheme.outlineVariant
            },
            label = "circleColor"
        )

        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            if (isActive && !isCurrent) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = "$stepNumber",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
private fun StepConnector(isActive: Boolean) {
    val color by animateColorAsState(
        targetValue = if (isActive) Color(0xFF22C55E) else MaterialTheme.colorScheme.outlineVariant,
        label = "connectorColor"
    )
    Box(
        modifier = Modifier
            .width(28.dp)
            .height(3.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color)
    )
}

/**
 * Interactive Live Map of Sour El Ghozlane rendering:
 * - Town center (36.1480, 3.6900)
 * - Geofence boundary
 * - Restaurant Marker
 * - Customer Home Marker
 * - Moving Driver Scooter with live trail and speed
 */
@Composable
fun SourElGhozlaneMapCanvas(
    shopLat: Double,
    shopLon: Double,
    customerLat: Double,
    customerLon: Double,
    driverLat: Double,
    driverLon: Double,
    driverSpeed: Double,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 36f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Coordinate mapping (Sour El Ghozlane bounding box)
            val minLat = 36.1360
            val maxLat = 36.1600
            val minLon = 3.6800
            val maxLon = 3.7050

            fun mapToPoint(lat: Double, lon: Double): Offset {
                val x = ((lon - minLon) / (maxLon - minLon)).toFloat() * w
                val y = (1f - ((lat - minLat) / (maxLat - minLat)).toFloat()) * h
                return Offset(x.coerceIn(20f, w - 20f), y.coerceIn(20f, h - 20f))
            }

            // 1. Draw stylized street grid of Sour El Ghozlane
            val streetColor = Color(0xFF1E293B)
            val mainRoadColor = Color(0xFF273549)
            val strokeWidth = 2.dp.toPx()

            // Main Avenues of Sour El Ghozlane
            drawLine(mainRoadColor, Offset(0f, h * 0.45f), Offset(w, h * 0.45f), strokeWidth = 4.dp.toPx())
            drawLine(mainRoadColor, Offset(w * 0.4f, 0f), Offset(w * 0.4f, h), strokeWidth = 4.dp.toPx())
            drawLine(mainRoadColor, Offset(0f, h * 0.8f), Offset(w, h * 0.3f), strokeWidth = 3.dp.toPx())

            // Secondary Roads
            for (i in 1..5) {
                val y = h * (i / 6f)
                drawLine(streetColor, Offset(0f, y), Offset(w, y), strokeWidth = strokeWidth)
                val x = w * (i / 6f)
                drawLine(streetColor, Offset(x, 0f), Offset(x, h), strokeWidth = strokeWidth)
            }

            // 2. Geofence Boundary (8km circle indicator)
            drawCircle(
                color = Color(0x22EA580C),
                radius = minOf(w, h) * 0.48f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            val shopPos = mapToPoint(shopLat, shopLon)
            val custPos = mapToPoint(customerLat, customerLon)
            val driverPos = mapToPoint(driverLat, driverLon)

            // 3. Route trail from Shop to Customer through Driver
            drawLine(
                color = Color(0xFFF97316),
                start = shopPos,
                end = driverPos,
                strokeWidth = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
            drawLine(
                color = Color(0x66F97316),
                start = driverPos,
                end = custPos,
                strokeWidth = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )

            // 4. Shop Marker 🏪
            drawCircle(color = Color(0xFF3B82F6), radius = 10.dp.toPx(), center = shopPos)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = shopPos)

            // 5. Customer Marker 🏠
            drawCircle(color = Color(0xFF22C55E), radius = 10.dp.toPx(), center = custPos)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = custPos)

            // 6. Moving Driver Scooter Marker 🛵 with pulse
            drawCircle(
                color = Color(0x44EA580C),
                radius = pulseRadius,
                center = driverPos
            )
            drawCircle(
                color = Color(0xFFEA580C),
                radius = 12.dp.toPx(),
                center = driverPos
            )
            drawCircle(
                color = Color.White,
                radius = 5.dp.toPx(),
                center = driverPos
            )
        }

        // Overlay Badges
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xCC0F172A))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF22C55E))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "بث مباشر (WebSocket)",
                color = Color(0xFFF8FAFC),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Driver Telemetry Badge
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xDD0F172A))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛵 دراجة السائق: ${String.format("%.1f", driverSpeed)} كم/سا",
                color = Color(0xFFF97316),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
