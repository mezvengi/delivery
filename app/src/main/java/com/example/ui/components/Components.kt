package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.OrderStatus
import com.example.data.models.UserRole
import kotlin.math.abs
import kotlin.math.atan2

@Composable
fun RoleSwitcherBar(
    selectedRole: UserRole,
    onRoleSelected: (UserRole) -> Unit,
    showAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    val visibleRoles = if (showAdmin) {
        UserRole.values().toList()
    } else {
        listOf(UserRole.CUSTOMER, UserRole.SHOP, UserRole.DRIVER)
    }

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
            visibleRoles.forEach { role ->
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
 * - Moving Driver Delivery Motorcycle with live smooth interpolation, bearing, and speed
 * - Real-time WebSocket connectivity status
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
    modifier: Modifier = Modifier,
    isWebSocketConnected: Boolean = true,
    driverName: String = "أمين التوصيل"
) {
    // Smooth coordinate interpolation (glides smoothly across frames)
    val animatedLat by animateFloatAsState(
        targetValue = driverLat.toFloat(),
        animationSpec = tween(durationMillis = 850, easing = LinearEasing),
        label = "animatedLat"
    )
    val animatedLon by animateFloatAsState(
        targetValue = driverLon.toFloat(),
        animationSpec = tween(durationMillis = 850, easing = LinearEasing),
        label = "animatedLon"
    )

    // Heading Bearing Calculation and Smooth Turn Rotation
    var prevLat by remember { mutableStateOf(driverLat) }
    var prevLon by remember { mutableStateOf(driverLon) }
    var targetBearing by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(driverLat, driverLon) {
        val dLat = driverLat - prevLat
        val dLon = driverLon - prevLon
        if (abs(dLat) > 0.00002 || abs(dLon) > 0.00002) {
            val screenDx = dLon
            val screenDy = -dLat // Inverted Y in screen coordinates
            targetBearing = Math.toDegrees(atan2(screenDy, screenDx)).toFloat()
            prevLat = driverLat
            prevLon = driverLon
        }
    }

    val animatedRotation by animateFloatAsState(
        targetValue = targetBearing,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "animatedBearing"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseRadius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0B132B))
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
                return Offset(x.coerceIn(24f, w - 24f), y.coerceIn(24f, h - 24f))
            }

            // 1. Draw stylized street grid of Sour El Ghozlane
            val streetColor = Color(0xFF1E293B)
            val mainRoadColor = Color(0xFF28384D)
            val roadGlowColor = Color(0xFF1C2738)
            val strokeWidth = 2.dp.toPx()

            // Main Avenues of Sour El Ghozlane
            drawLine(roadGlowColor, Offset(0f, h * 0.45f), Offset(w, h * 0.45f), strokeWidth = 8.dp.toPx())
            drawLine(mainRoadColor, Offset(0f, h * 0.45f), Offset(w, h * 0.45f), strokeWidth = 4.dp.toPx())
            drawLine(roadGlowColor, Offset(w * 0.4f, 0f), Offset(w * 0.4f, h), strokeWidth = 8.dp.toPx())
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
                color = Color(0x25EA580C),
                radius = minOf(w, h) * 0.48f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            val shopPos = mapToPoint(shopLat, shopLon)
            val custPos = mapToPoint(customerLat, customerLon)
            val driverPos = mapToPoint(animatedLat.toDouble(), animatedLon.toDouble())

            // 3. Glowing Route trail from Shop to Driver (completed leg)
            drawLine(
                color = Color(0xFFF97316),
                start = shopPos,
                end = driverPos,
                strokeWidth = 3.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )
            // Remaining Route trail from Driver to Customer (upcoming leg)
            drawLine(
                color = Color(0x66F97316),
                start = driverPos,
                end = custPos,
                strokeWidth = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            // 4. Shop Marker 🏪
            drawCircle(color = Color(0x443B82F6), radius = 16.dp.toPx(), center = shopPos)
            drawCircle(color = Color(0xFF3B82F6), radius = 10.dp.toPx(), center = shopPos)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = shopPos)

            // 5. Customer Marker 🏠
            drawCircle(color = Color(0x4422C55E), radius = 16.dp.toPx(), center = custPos)
            drawCircle(color = Color(0xFF22C55E), radius = 10.dp.toPx(), center = custPos)
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = custPos)

            // 6. Radar pulse rings around the moving Delivery Bike
            drawCircle(
                color = Color(0xFFEA580C).copy(alpha = pulseAlpha),
                radius = pulseRadius,
                center = driverPos
            )
            drawCircle(
                color = Color(0xFFF97316).copy(alpha = pulseAlpha * 0.5f),
                radius = pulseRadius * 1.6f,
                center = driverPos
            )

            // 7. Polished Delivery Motorcycle with Heading Rotation
            rotate(degrees = animatedRotation, pivot = driverPos) {
                // Forward LED Headlight Beam
                val beamPath = Path().apply {
                    moveTo(driverPos.x + 12.dp.toPx(), driverPos.y - 3.dp.toPx())
                    lineTo(driverPos.x + 44.dp.toPx(), driverPos.y - 16.dp.toPx())
                    lineTo(driverPos.x + 44.dp.toPx(), driverPos.y + 16.dp.toPx())
                    close()
                }
                drawPath(
                    path = beamPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0x99FEF08A), Color(0x00FEF08A)),
                        startX = driverPos.x + 12.dp.toPx(),
                        endX = driverPos.x + 44.dp.toPx()
                    )
                )

                // Wheels
                val backWheelCenter = Offset(driverPos.x - 10.dp.toPx(), driverPos.y)
                val frontWheelCenter = Offset(driverPos.x + 10.dp.toPx(), driverPos.y)

                drawCircle(color = Color(0xFF0F172A), radius = 5.dp.toPx(), center = backWheelCenter)
                drawCircle(color = Color(0xFF94A3B8), radius = 2.dp.toPx(), center = backWheelCenter)

                drawCircle(color = Color(0xFF0F172A), radius = 5.dp.toPx(), center = frontWheelCenter)
                drawCircle(color = Color(0xFF94A3B8), radius = 2.dp.toPx(), center = frontWheelCenter)

                // Aerodynamic Scooter Body & Chassis
                drawRoundRect(
                    color = Color(0xFFEA580C),
                    topLeft = Offset(driverPos.x - 8.dp.toPx(), driverPos.y - 4.dp.toPx()),
                    size = Size(17.dp.toPx(), 8.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Windshield / Front fairing
                drawCircle(
                    color = Color(0xFFFB923C),
                    radius = 4.5.dp.toPx(),
                    center = Offset(driverPos.x + 8.dp.toPx(), driverPos.y)
                )
                // Headlight bulb
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = 2.5.dp.toPx(),
                    center = Offset(driverPos.x + 12.dp.toPx(), driverPos.y)
                )

                // High-visibility Courier Thermal Box (Trunk)
                drawRoundRect(
                    color = Color(0xFFC2410C),
                    topLeft = Offset(driverPos.x - 15.dp.toPx(), driverPos.y - 5.5.dp.toPx()),
                    size = Size(8.dp.toPx(), 11.dp.toPx()),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                // Thermal Box SG Logo Strap
                drawLine(
                    color = Color.White,
                    start = Offset(driverPos.x - 15.dp.toPx(), driverPos.y),
                    end = Offset(driverPos.x - 7.dp.toPx(), driverPos.y),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Driver Helmet
                drawCircle(
                    color = Color.White,
                    radius = 4.5.dp.toPx(),
                    center = Offset(driverPos.x - 1.dp.toPx(), driverPos.y)
                )
                // Helmet Visor
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = 2.dp.toPx(),
                    center = Offset(driverPos.x + 1.5.dp.toPx(), driverPos.y)
                )
            }
        }

        // Top Badges Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // City Map Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "خريطة سور الغزلان الحية 🇩🇿",
                    color = Color(0xFFCBD5E1),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // WebSocket Live Status Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xCC0F172A))
                    .border(1.dp, if (isWebSocketConnected) Color(0xFF15803D) else Color(0xFF991B1B), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isWebSocketConnected) Color(0xFF22C55E) else Color(0xFFEF4444))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isWebSocketConnected) "بث مباشر (WebSocket) ⚡" else "جارِ الاتصال بالـ WebSocket...",
                    color = Color(0xFFF8FAFC),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Bottom Driver Telemetry & Speed Badge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xEE0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🛵 دراجة $driverName: ${String.format("%.1f", driverSpeed)} كم/سا",
                    color = Color(0xFFF97316),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xCC0F172A))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 ${String.format("%.4f", animatedLat)}°N, ${String.format("%.4f", animatedLon)}°E",
                    color = Color(0xFF94A3B8),
                    fontSize = 9.sp
                )
            }
        }
    }
}
