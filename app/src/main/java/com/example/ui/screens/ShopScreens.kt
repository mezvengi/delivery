package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.NeighborhoodStorage
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.ShopEntity
import com.example.data.models.OrderStatus
import com.example.data.models.SourElGhozlaneConstants
import com.example.data.repository.DeliveryRepository
import kotlinx.coroutines.launch

// نماذج لصور وجبات شهيرة نموذجية في الجزائر يمكن لصاحب المطعم اختيارها بنقرة واحدة
data class PresetFoodPhoto(
    val title: String,
    val category: String,
    val url: String
)

val PRESET_FOOD_PHOTOS = listOf(
    PresetFoodPhoto(
        title = "🛒 زيت طعام 5 لتر",
        category = "مواد غذائية",
        url = "https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "🌾 كيس سميد أو فرينة 10 كغ",
        category = "مواد غذائية",
        url = "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "🧀 جبن ومشتقات الحليب",
        category = "مواد غذائية",
        url = "https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "🎧 سماعات بلوتوث لاسلكية",
        category = "أجهزة إلكترونية وهواتف",
        url = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "🔋 باور بانك وشواحن سريعة",
        category = "أجهزة إلكترونية وهواتف",
        url = "https://images.unsplash.com/photo-1609592424368-2a2990d0b0f4?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "👕 قميص رجالي كاجوال",
        category = "ألبسة وأحذية وأزياء",
        url = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "👟 حذاء رياضي مريح للجري",
        category = "ألبسة وأحذية وأزياء",
        url = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "🍕 بيتزا إيطالية مقرمشة",
        category = "مطاعم ومأكولات",
        url = "https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "🍔 برجر لحم مشوي بالجبن",
        category = "مطاعم ومأكولات",
        url = "https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "🥩 شواء لحم مشكل على الفحم",
        category = "مطاعم ومأكولات",
        url = "https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "💊 حليب أطفال وغذاء مدعم",
        category = "صيدلية ومستلزمات صحية",
        url = "https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "💄 عطور فاخرة وكوسميتيك",
        category = "عطور ومستحضرات تجميل",
        url = "https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=500&auto=format&fit=crop&q=80"
    ),
    PresetFoodPhoto(
        title = "📚 أدوات مدرسية ومحفظة",
        category = "مكتبات وأدوات مدرسية",
        url = "https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=500&auto=format&fit=crop&q=80"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDashboardScreen(
    shop: ShopEntity,
    orders: List<OrderEntity>,
    products: List<ProductEntity> = emptyList(),
    repository: DeliveryRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Orders, 1: Products & Menu

    val context = LocalContext.current
    val neighborhoodStorage = remember { NeighborhoodStorage.getInstance(context) }
    val dynamicNeighborhoods by neighborhoodStorage.neighborhoods.collectAsState()

    // Dialog state for Manual Phone-in Orders
    var showManualOrderDialog by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var addressDesc by remember { mutableStateOf("") }
    var itemsDesc by remember { mutableStateOf("") }
    var totalAmount by remember { mutableStateOf("1200") }
    var selectedNeighborhood by remember(dynamicNeighborhoods) {
        mutableStateOf(dynamicNeighborhoods.firstOrNull()?.nameArabic ?: SourElGhozlaneConstants.NEIGHBORHOODS.first().nameArabic)
    }
    var neighborhoodExpanded by remember { mutableStateOf(false) }
    var dialogError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    // Dialog state for Adding New Product with Image & Price
    var showAddProductDialog by remember { mutableStateOf(false) }
    var newProductName by remember { mutableStateOf("") }
    var newProductPrice by remember { mutableStateOf("650") }
    var newProductCategory by remember { mutableStateOf("وجبات وسندويشات") }
    var newProductDesc by remember { mutableStateOf("") }
    var newProductImageUrl by remember { mutableStateOf("") }
    var productDialogError by remember { mutableStateOf<String?>(null) }
    var isAddingProduct by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            newProductImageUrl = uri.toString()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ==============================================================
        // 1. بطاقة المتجر العلوية وحالة الفتح / الإغلاق
        // ==============================================================
        Card(
            shape = RoundedCornerShape(0.dp, 0.dp, 16.dp, 16.dp),
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = shop.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text("📍 ${shop.neighborhood} • هاتف: ${shop.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (shop.isOpen) "المتجر مفتوح ويستقبل الطلبات ✅" else "المتجر مغلق حالياً ❌",
                            color = if (shop.isOpen) Color(0xFF22C55E) else MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Switch(
                        checked = shop.isOpen,
                        onCheckedChange = { isOpen ->
                            scope.launch {
                                repository.updateShopOpen(shop.id, isOpen)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons: تسجيل طلب هاتفي + إضافة وجبة جديدة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showManualOrderDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طلب هاتفي 🛵", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            newProductName = ""
                            newProductPrice = "650"
                            newProductDesc = ""
                            newProductImageUrl = PRESET_FOOD_PHOTOS.first().url
                            productDialogError = null
                            showAddProductDialog = true
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة وجبة ➕", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ==============================================================
        // 2. التبويبات: الطلبات الواردة vs قائمة الوجبات والأسعار
        // ==============================================================
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("الطلبات الواردة (${orders.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.RestaurantMenu, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("قائمة الوجبات (${products.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            )
        }

        // ==============================================================
        // 3. محتوى التبويب المختار
        // ==============================================================
        if (selectedTab == 0) {
            // ====================== تبويب الطلبات الواردة ======================
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentPadding = PaddingValues(bottom = 60.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (orders.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لا توجد طلبات جارية حالياً لمطعم ${shop.name}. يمكنك استقبال طلبات الزبائن عبر التطبيق أو تسجيل طلب هاتفي مباشر واستدعاء سائق فوراً.",
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
                                    text = "الزبون: ${order.customerName} (${order.customerPhone})",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "الحي: ${order.neighborhood} - ${order.addressDescription}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "الوجبات: ${order.itemsSummary}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المجموع: ${order.total} دج",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (currentStatus == OrderStatus.NEW) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        repository.updateOrderStatus(order.id, OrderStatus.PREPARING)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("قبول وتحضير", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else if (currentStatus == OrderStatus.PREPARING) {
                                            Button(
                                                onClick = {
                                                    scope.launch {
                                                        repository.updateOrderStatus(order.id, OrderStatus.READY_FOR_PICKUP)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("جاهز للتسليم 🛵", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // ====================== تبويب قائمة الوجبات والأسعار ======================
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
                            text = "الوجبات المعروضة للزبائن (${products.size}):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        OutlinedButton(
                            onClick = {
                                newProductName = ""
                                newProductPrice = "650"
                                newProductDesc = ""
                                newProductImageUrl = PRESET_FOOD_PHOTOS.first().url
                                productDialogError = null
                                showAddProductDialog = true
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة وجبة", fontSize = 12.sp)
                        }
                    }
                }

                if (products.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fastfood,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "لم تقم بإضافة أي وجبة بعد في قائمة متجرك!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "انقر على زر 'إضافة وجبة' لإدراج صور الوجبات مع الأسعار ليراها زبائن سور الغزلان ويطلبوها فوراً.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(products) { product ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // صورة الوجبة
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (product.imageUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(product.imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = product.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Fastfood,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // تفاصيل الوجبة
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = product.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        IconButton(
                                            onClick = {
                                                scope.launch {
                                                    repository.deleteProduct(product.id)
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DeleteOutline,
                                                contentDescription = "حذف الوجبة",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = product.category,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    if (product.description.isNotBlank()) {
                                        Text(
                                            text = product.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.outline,
                                            maxLines = 1,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // السعر بالدينار الجزائري
                                        Text(
                                            text = "${product.price} دج",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )

                                        // زر تبديل التوفر (متوفر / نفد)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = if (product.isAvailable) "متوفر" else "نفد",
                                                fontSize = 11.sp,
                                                color = if (product.isAvailable) Color(0xFF16A34A) else MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Switch(
                                                checked = product.isAvailable,
                                                onCheckedChange = { isAvail ->
                                                    scope.launch {
                                                        repository.updateProductAvailable(product.id, isAvail)
                                                    }
                                                },
                                                modifier = Modifier.size(36.dp)
                                            )
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

    // ==============================================================
    // نافذة إضافة وجبة جديدة مع الصورة والسعر (Add Product Dialog)
    // ==============================================================
    if (showAddProductDialog) {
        val categoriesList = listOf(
            "مواد غذائية وبقالة",
            "أجهزة إلكترونية وهواتف",
            "ألبسة وأحذية وأزياء",
            "وجبات وسندويشات",
            "بيتزا وشواء",
            "صيدلية ومستلزمات صحية",
            "عطور ومستحضرات تجميل",
            "مكتبات وأدوات مدرسية",
            "مستلزمات المنزل وخردوات",
            "حلويات ومخبوزات"
        )

        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Fastfood, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إضافة وجبة جديدة للقائمة", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // اسم الوجبة
                    OutlinedTextField(
                        value = newProductName,
                        onValueChange = { newProductName = it },
                        label = { Text("اسم الوجبة أو المنتج *") },
                        placeholder = { Text("مثال: بيتزا شاورما عائلية...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // السعر بالدينار الجزائري
                    OutlinedTextField(
                        value = newProductPrice,
                        onValueChange = { newProductPrice = it },
                        label = { Text("السعر بالدينار الجزائري (دج) *") },
                        placeholder = { Text("850") },
                        trailingIcon = { Text("دج ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // التصنيف
                    Text("تصنيف الوجبة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categoriesList) { cat ->
                            FilterChip(
                                selected = newProductCategory == cat,
                                onClick = { newProductCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    // الوصف والمكونات
                    OutlinedTextField(
                        value = newProductDesc,
                        onValueChange = { newProductDesc = it },
                        label = { Text("وصف الوجبة والمكونات") },
                        placeholder = { Text("جبن موتزاريلا، صلصة طماطم، قطع دجاج متبلة...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // اختيار الصورة
                    Text("صورة الوجبة:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                    // معاينة الصورة الحالية
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (newProductImageUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(newProductImageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "معاينة صورة الوجبة",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(imageVector = Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                Text("لم يتم اختيار صورة بعد", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }

                    // أزرار اختيار الصورة: اختيار من المعرض أو من النماذج الجاهزة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("من المعرض 📷", fontSize = 11.sp)
                        }
                    }

                    // نماذج صور جزائرية سريعة بنقرة واحدة
                    Text("أو اختر صورة جاهزة بنقرة واحدة:", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PRESET_FOOD_PHOTOS) { preset ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (newProductImageUrl == preset.url) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                border = if (newProductImageUrl == preset.url) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.clickable {
                                    newProductImageUrl = preset.url
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(preset.title, fontSize = 11.sp)
                                    if (newProductImageUrl == preset.url) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    // إدخال رابط مخصص إذا رغب صاحب المطعم
                    OutlinedTextField(
                        value = newProductImageUrl,
                        onValueChange = { newProductImageUrl = it },
                        label = { Text("أو رابط صورة مباشر (URL)") },
                        placeholder = { Text("https://example.com/image.jpg") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (productDialogError != null) {
                        Text(productDialogError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProductName.isBlank()) {
                            productDialogError = "يرجى كتابة اسم الوجبة"
                            return@Button
                        }
                        val priceInt = newProductPrice.toIntOrNull()
                        if (priceInt == null || priceInt <= 0) {
                            productDialogError = "يرجى إدخال سعر صحيح بالدينار الجزائري"
                            return@Button
                        }

                        isAddingProduct = true
                        scope.launch {
                            repository.addProduct(
                                shopId = shop.id,
                                name = newProductName.trim(),
                                description = newProductDesc.trim(),
                                priceDa = priceInt,
                                category = newProductCategory,
                                imageUrl = newProductImageUrl.trim()
                            )
                            isAddingProduct = false
                            showAddProductDialog = false
                            selectedTab = 1 // التبديل إلى تبويب الوجبات لمعاينة الوجبة فوراً
                        }
                    },
                    enabled = !isAddingProduct
                ) {
                    Text(if (isAddingProduct) "جارٍ الحفظ..." else "حفظ الوجبة وعرضها للزبائن ✅")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // ==============================================================
    // نافذة تسجيل طلب هاتفي مباشر (Manual Order Dialog)
    // ==============================================================
    if (showManualOrderDialog) {
        AlertDialog(
            onDismissRequest = { showManualOrderDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PhoneInTalk, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("تسجيل طلب هاتفي مباشر", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "أدخل بيانات الزبون الذي اتصل بك هاتفياً لإرسال الطلب فوراً إلى أقرب سائق متاح في سور الغزلان:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        label = { Text("اسم الزبون") },
                        placeholder = { Text("محمد أو كريم...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = customerPhone,
                        onValueChange = { customerPhone = it },
                        label = { Text("رقم هاتف الزبون") },
                        placeholder = { Text("0550123456") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = neighborhoodExpanded,
                        onExpandedChange = { neighborhoodExpanded = !neighborhoodExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedNeighborhood,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("الحي في سور الغزلان") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = neighborhoodExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = neighborhoodExpanded,
                            onDismissRequest = { neighborhoodExpanded = false }
                        ) {
                            dynamicNeighborhoods.forEach { n ->
                                DropdownMenuItem(
                                    text = { Text(n.nameArabic) },
                                    onClick = {
                                        selectedNeighborhood = n.nameArabic
                                        neighborhoodExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = addressDesc,
                        onValueChange = { addressDesc = it },
                        label = { Text("تفاصيل العنوان والمعلم القريب") },
                        placeholder = { Text("قرب العيادة، الطابق 2...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = itemsDesc,
                        onValueChange = { itemsDesc = it },
                        label = { Text("الوجبات والملاحظات") },
                        placeholder = { Text("2 شواء دجاج + بطاطا مقلية دبل...") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = { Text("مبلغ الوجبات الإجمالي (دج)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "رسوم التوصيل الثابتة: 200 دج • يدفعها الزبون للسائق نقداً عند الاستلام.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    if (dialogError != null) {
                        Text(dialogError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customerName.isBlank() || customerPhone.isBlank() || addressDesc.isBlank()) {
                            dialogError = "يرجى ملء جميع الحقول المطلوبة"
                            return@Button
                        }
                        val amt = totalAmount.toIntOrNull() ?: 500
                        isSubmitting = true
                        scope.launch {
                            val res = repository.createManualShopOrder(
                                shopId = shop.id,
                                customerName = customerName,
                                customerPhone = customerPhone,
                                neighborhood = selectedNeighborhood,
                                address = addressDesc,
                                itemsDesc = itemsDesc.ifBlank { "طلب هاتفي مباشر" },
                                total = amt,
                                notes = "تم الطلب عبر الهاتف"
                            )
                            isSubmitting = false
                            if (res.isSuccess) {
                                showManualOrderDialog = false
                                customerName = ""
                                customerPhone = ""
                                addressDesc = ""
                                itemsDesc = ""
                            } else {
                                dialogError = "تعذر إرسال الطلب، تأكد من الاتصال"
                            }
                        }
                    },
                    enabled = !isSubmitting
                ) {
                    Text(if (isSubmitting) "جارٍ الإرسال..." else "استدعاء السائق فوراً 🛵")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualOrderDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
