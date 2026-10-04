package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.CustomerLoyaltyStorage
import com.example.data.local.CustomerOrderHistoryStorage
import com.example.data.local.CustomerPastOrder
import com.example.data.local.LoyaltyTransaction
import com.example.data.local.entities.DriverEntity
import com.example.data.local.entities.OrderEntity
import com.example.data.local.entities.ProductEntity
import com.example.data.local.entities.ShopEntity
import com.example.data.models.OrderStatus
import com.example.data.models.SourElGhozlaneConstants
import com.example.data.network.OrderItemDto
import com.example.data.repository.DeliveryRepository
import com.example.ui.components.OrderStatusStepper
import com.example.ui.components.SourElGhozlaneMapCanvas
import kotlinx.coroutines.launch

data class StoreCategory(
    val id: String,
    val nameArabic: String,
    val icon: String,
    val description: String = "",
    val keywords: List<String>
)

typealias FoodCategory = StoreCategory

val defaultStoreCategories = listOf(
    StoreCategory("all", "الكل", "🌟", "جميع المتاجر في سور الغزلان", emptyList()),
    StoreCategory("groceries", "مواد غذائية وسوبرماركت", "🛒", "بقالة، معلبات، خضر، أجبان، مشروبات", listOf("غذائية", "سوبرماركت", "بقالة", "مواد", "ماركت", "استهلاكية", "حبوب", "superette", "alimentation")),
    StoreCategory("restaurants", "مطاعم ومأكولات", "🍽️", "بيتزا، شواء، برجر، سندويشات، تقليدي", listOf("مطعم", "بيتزا", "شواء", "مشاوي", "فاست", "برغر", "سندويش", "أكل", "وجبات", "restaurant")),
    StoreCategory("clothing", "ألبسة وأحذية وأزياء", "👕", "ملابس رجالية، نسائية، أطفال، أحذية", listOf("ألبسة", "ملابس", "حذاء", "أحذية", "بوتيك", "أزياء", "vestimentaire", "vetement")),
    StoreCategory("electronics", "أجهزة إلكترونية وهواتف", "📱", "هواتف، ملحقات، شواحن، إلكترونيات", listOf("إلكترونيات", "هاتف", "هواتف", "شاحن", "كهرومنزلية", "إلكتروني", "phone", "tech")),
    StoreCategory("pharmacy", "صيدلية ومستلزمات صحية", "💊", "شبه صيدلانية، حليب أطفال، عناية", listOf("صيدلية", "شبه", "دواء", "أطفال", "عناية", "صحة", "pharmacie")),
    StoreCategory("cosmetics", "عطور ومستحضرات تجميل", "💄", "كوسميتيك، عطور، عناية شخصية", listOf("كوسميتيك", "عطر", "عطور", "تجميل", "مكياج", "cosmetique")),
    StoreCategory("stationery", "مكتبات وأدوات مدرسية", "📚", "كتب، كراريس، أدوات مدرسية، هدايا", listOf("مكتبة", "أدوات", "كراريس", "كتب", "مدرسية", "papeterie")),
    StoreCategory("home", "مستلزمات منزلية وخردوات", "🏠", "خردوات، أدوات المطبخ، تنظيف", listOf("منزل", "خردوات", "تنظيف", "أواني", "quincaillerie")),
    StoreCategory("sweets", "حلويات ومخبوزات", "🥐", "حلويات تقليدية، قلب اللوز، باتيسري", listOf("حلويات", "مخبزة", "باتيسري", "قلب اللوز", "كرواسون"))
)

val defaultFoodCategories = defaultStoreCategories

@Composable
fun CustomerShopListScreen(
    shops: List<ShopEntity>,
    products: List<ProductEntity> = emptyList(),
    onShopSelected: (ShopEntity) -> Unit,
    onOpenOrderHistory: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    orderHistoryCount: Int = 0,
    loyaltyPoints: Int = 150,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("all") }

    val trimmedQuery = searchQuery.trim()

    val matchingShops = remember(trimmedQuery, selectedCategoryId, shops, products) {
        val selectedCat = defaultFoodCategories.find { it.id == selectedCategoryId } ?: defaultFoodCategories.first()
        shops.filter { shop ->
            val matchesCategory = if (selectedCat.id == "all") {
                true
            } else {
                val shopMatches = selectedCat.keywords.any { kw ->
                    shop.category.contains(kw, ignoreCase = true) ||
                            shop.name.contains(kw, ignoreCase = true)
                }
                val hasMatchingProduct = products.any { p ->
                    p.shopId == shop.id && selectedCat.keywords.any { kw ->
                        p.name.contains(kw, ignoreCase = true) ||
                                p.category.contains(kw, ignoreCase = true)
                    }
                }
                shopMatches || hasMatchingProduct
            }

            val matchesQuery = trimmedQuery.isEmpty() ||
                    shop.name.contains(trimmedQuery, ignoreCase = true) ||
                    shop.category.contains(trimmedQuery, ignoreCase = true) ||
                    shop.neighborhood.contains(trimmedQuery, ignoreCase = true) ||
                    shop.address.contains(trimmedQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val matchingProducts = remember(trimmedQuery, selectedCategoryId, products) {
        val selectedCat = defaultFoodCategories.find { it.id == selectedCategoryId } ?: defaultFoodCategories.first()
        if (trimmedQuery.isEmpty() && selectedCat.id == "all") {
            emptyList()
        } else {
            products.filter { product ->
                val matchesCat = if (selectedCat.id == "all") true else {
                    selectedCat.keywords.any { kw ->
                        product.name.contains(kw, ignoreCase = true) ||
                                product.category.contains(kw, ignoreCase = true)
                    }
                }
                val matchesQuery = trimmedQuery.isEmpty() ||
                        product.name.contains(trimmedQuery, ignoreCase = true) ||
                        product.description.contains(trimmedQuery, ignoreCase = true) ||
                        product.category.contains(trimmedQuery, ignoreCase = true)
                matchesCat && matchesQuery
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "توصيل طلبات سور الغزلان 🛵",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "توصيل 200 دج",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "توصيل فوري من كافة متاجر ومطاعم سور الغزلان (مواد غذائية، ألبسة، هواتف، صيدلية، مطاعم)، أقرب سائق دراجة نارية أو سكوتر إليك.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Quick Navigation: Loyalty Points Card & Order History Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Loyalty Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenProfile() }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("⭐ نقاط الولاء", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF92400E))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Text("خصم 🏷️", fontSize = 10.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$loyaltyPoints نقطة",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color(0xFFB45309)
                        )
                        Text(
                            text = "استبدلها بخصم التوصيل ➔",
                            fontSize = 10.sp,
                            color = Color(0xFF78350F),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                // Order History Card
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenOrderHistory() }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📋 طلباتي", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text("$orderHistoryCount طلبات", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "سجل الطلبات",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "عرض التواريخ وتتبعها ➔",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Search Input Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "ابحث عن متجر أو منتج (مواد غذائية، هواتف، ألبسة، شواء، صيدلية...)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "مسح البحث",
                                tint = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Category Filter Bar (شريط التصنيفات أعلى قائمة المتاجر)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "التصنيفات والأنواع:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (selectedCategoryId != "all") {
                        Text(
                            text = "إلغاء التصفية ✕",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { selectedCategoryId = "all" }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(defaultFoodCategories) { cat ->
                        val isSelected = selectedCategoryId == cat.id
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                            border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)) else null,
                            shadowElevation = if (isSelected) 3.dp else 1.dp,
                            modifier = Modifier
                                .clickable {
                                    selectedCategoryId = if (isSelected && cat.id != "all") "all" else cat.id
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            ) {
                                Text(
                                    text = cat.icon,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = cat.nameArabic,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Matching Meals Section (if user searched for a specific dish)
        if (matchingProducts.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "الوجبات المطابقة للبحث (${matchingProducts.size}):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(matchingProducts) { product ->
                val parentShop = shops.find { it.id == product.shopId }
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (parentShop != null) {
                                onShopSelected(parentShop)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = product.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "متوفر في: ${parentShop?.name ?: "المتجر"} (📍 ${parentShop?.neighborhood ?: "سور الغزلان"})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            if (product.description.isNotEmpty()) {
                                Text(
                                    text = product.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = "${product.price} دج",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider()
            }
        }

        // Section Title for Shops
        item {
            Text(
                text = if (trimmedQuery.isNotEmpty()) "المطاعم المطابقة (${matchingShops.size}):" else "المطاعم والمتاجر في سور الغزلان:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        // Empty Search Results State
        if (matchingShops.isEmpty() && matchingProducts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "🔍",
                            fontSize = 32.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "لا توجد نتائج مطابقة لـ \"$trimmedQuery\"",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "جرب البحث باسم مطعم مثل \"الأوراس\" أو \"البرج\" أو وجبة مثل \"شواء\"، \"بيتزا\"، \"شاورما\".",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        } else {
            // Shop Cards
            items(matchingShops) { shop ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShopSelected(shop) }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = shop.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFEF3C7)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = shop.rating,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF92400E)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "📍 ${shop.neighborhood} • ${shop.address}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱ وقت التوصيل: ${shop.deliveryTime}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "توصيل ثابت 200 دج",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerShopDetailScreen(
    shop: ShopEntity,
    products: List<ProductEntity>,
    drivers: List<DriverEntity>,
    repository: DeliveryRepository,
    onBack: () -> Unit,
    onOrderPlaced: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val cart = remember { mutableStateMapOf<Long, Int>() }
    var showDriverModal by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val totalItems = cart.values.sum()
    val subtotal = products.sumOf { (cart[it.id] ?: 0) * it.price }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Bar
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                    Column {
                        Text(shop.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("📍 ${shop.neighborhood} • هاتف: ${shop.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }

            // Products List
            items(products) { product ->
                val qty = cart[product.id] ?: 0
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (product.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(product.imageUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = product.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(product.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                product.description,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${product.price} دج",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Quantity Controller
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        val cur = cart[product.id] ?: 0
                                        if (cur > 0) {
                                            if (cur == 1) cart.remove(product.id)
                                            else cart[product.id] = cur - 1
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }

                            Text(
                                text = "$qty",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        val cur = cart[product.id] ?: 0
                                        cart[product.id] = cur + 1
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        // Floating Cart Bar
        AnimatedVisibility(
            visible = totalItems > 0,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp,
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$totalItems", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "المجموع: $subtotal دج",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Text(
                            text = "+ 200 دج توصيل (دفع عند الاستلام)",
                            fontSize = 11.sp,
                            color = Color(0xFF22C55E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Button(
                        onClick = { showDriverModal = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("إتمام واختيار السائق 🛵", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Driver Selection & Checkout Sheet Overlay
        AnimatedVisibility(
            visible = showDriverModal,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically(initialOffsetY = { it / 2 }),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically(targetOffsetY = { it / 2 })
        ) {
            DriverSelectionSheet(
                shop = shop,
                products = products,
                cart = cart,
                subtotal = subtotal,
                drivers = drivers,
                repository = repository,
                onDismiss = { showDriverModal = false },
                onConfirmOrder = { customerName, customerPhone, neighborhood, address, selectedDriver, deliveryDiscount, pointsUsed ->
                    scope.launch {
                        val itemsSummary = products
                            .filter { (cart[it.id] ?: 0) > 0 }
                            .joinToString("، ") { "${cart[it.id]}x ${it.name}" }

                        val orderItemsList = products
                            .filter { (cart[it.id] ?: 0) > 0 }
                            .map {
                                OrderItemDto(
                                    product_id = it.id,
                                    quantity = cart[it.id] ?: 1,
                                    price_da = it.price
                                )
                            }

                        val nCoord = com.example.data.local.NeighborhoodStorage.getInstance(context).neighborhoods.value.find { it.nameArabic == neighborhood }
                            ?: SourElGhozlaneConstants.NEIGHBORHOODS.find { it.nameArabic == neighborhood }
                        val custLat = nCoord?.lat ?: SourElGhozlaneConstants.CENTER_LAT
                        val custLon = nCoord?.lon ?: SourElGhozlaneConstants.CENTER_LON

                        val orderId = repository.createOrder(
                            customerName = customerName,
                            customerPhone = customerPhone,
                            shop = shop,
                            driver = selectedDriver,
                            neighborhood = neighborhood,
                            addressDescription = address,
                            customerLat = custLat,
                            customerLon = custLon,
                            itemsSummary = itemsSummary,
                            subtotal = subtotal,
                            orderItems = orderItemsList,
                            deliveryFeeDiscount = deliveryDiscount,
                            loyaltyPointsUsed = pointsUsed
                        )

                        showDriverModal = false
                        onOrderPlaced(orderId)
                    }
                }
            )
        }
    }
}

@Composable
fun DriverSelectionSheet(
    shop: ShopEntity,
    products: List<ProductEntity>,
    cart: Map<Long, Int>,
    subtotal: Int,
    drivers: List<DriverEntity>,
    repository: DeliveryRepository,
    onDismiss: () -> Unit,
    onConfirmOrder: (name: String, phone: String, neighborhood: String, address: String, driver: DriverEntity?, deliveryDiscount: Int, pointsUsed: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val neighborhoodStorage = remember { com.example.data.local.NeighborhoodStorage.getInstance(context) }
    val dynamicNeighborhoods by neighborhoodStorage.neighborhoods.collectAsState()

    var customerName by remember { mutableStateOf("محمد - زبون سور الغزلان") }
    var customerPhone by remember { mutableStateOf("0550123456") }
    var selectedNeighborhood by remember(dynamicNeighborhoods) {
        mutableStateOf(dynamicNeighborhoods.firstOrNull()?.nameArabic ?: SourElGhozlaneConstants.NEIGHBORHOODS.first().nameArabic)
    }
    var addressDescription by remember { mutableStateOf("مقابل صيدلية الأمل، العمارة ب، الطابق 2") }
    var selectedDriver by remember { mutableStateOf(drivers.firstOrNull()) }

    // Loyalty Points state
    val pointsBalance by (repository.loyaltyStorage?.pointsBalance ?: kotlinx.coroutines.flow.MutableStateFlow(0)).collectAsState()
    var selectedDiscountPoints by remember { mutableIntStateOf(0) }
    val deliveryDiscount = when (selectedDiscountPoints) {
        100 -> minOf(100, 200)
        200 -> 200
        else -> 0
    }
    val effectiveDeliveryFee = maxOf(0, 200 - deliveryDiscount)
    val totalWithDelivery = subtotal + effectiveDeliveryFee

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
                .clickable(enabled = false) {}, // absorb clicks inside sheet
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛵 اختيار السائق وإتمام الطلب",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Name & Algerian Phone
                    item {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { customerName = it },
                            label = { Text("اسم الزبون الكريم") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("رقم الهاتف الجزائري (05 / 06 / 07)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Neighborhood Selector with Chips
                    item {
                        Text(
                            text = "حي التوصيل في سور الغزلان:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(dynamicNeighborhoods) { n ->
                                val isSelected = selectedNeighborhood == n.nameArabic
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedNeighborhood = n.nameArabic },
                                    label = { Text(n.nameArabic, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = addressDescription,
                            onValueChange = { addressDescription = it },
                            label = { Text("وصف العنوان (معلم قريب أو رقم العمارة)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }

                    // Live Online Drivers in Sour El Ghozlane
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اختر سائق التوصيل الأقرب إليك 🛵:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(drivers) { d ->
                        val isSelected = d.id == selectedDriver?.id
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = if (isSelected) ButtonDefaults.outlinedButtonBorder else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedDriver = d }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🛵", fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(d.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text("${d.vehicleType} • متصل الآن", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                Text(d.rating, fontWeight = FontWeight.Bold, color = Color(0xFFD97706), fontSize = 12.sp)
                            }
                        }
                    }

                    // ==============================================================
                    // بطاقة نقاط الولاء وخصم التوصيل
                    // ==============================================================
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⭐", fontSize = 18.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "نقاط الولاء وخصم التوصيل",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color(0xFF92400E)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFFEF3C7)
                                    ) {
                                        Text(
                                            text = "رصيدك: $pointsBalance نقطة",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                if (pointsBalance >= 100) {
                                    Text(
                                        text = "اختر خصم التوصيل المتاح لرصيدك:",
                                        fontSize = 11.sp,
                                        color = Color(0xFF78350F)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        item {
                                            FilterChip(
                                                selected = selectedDiscountPoints == 0,
                                                onClick = { selectedDiscountPoints = 0 },
                                                label = { Text("بدون خصم", fontSize = 11.sp) }
                                            )
                                        }

                                        item {
                                            FilterChip(
                                                selected = selectedDiscountPoints == 100,
                                                onClick = { selectedDiscountPoints = 100 },
                                                label = { Text("خصم 100 دج (-100 نقطة) 🏷️", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                            )
                                        }

                                        if (pointsBalance >= 200) {
                                            item {
                                                FilterChip(
                                                    selected = selectedDiscountPoints == 200,
                                                    onClick = { selectedDiscountPoints = 200 },
                                                    label = { Text("توصيل مجاني 0 دج! (-200 نقطة) 🎉", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    Text(
                                        text = "ستكسب +50 نقطة فور إتمام هذا الطلب! اجمع 100 نقطة للحصول على خصم 100 دج على تكلفة التوصيل القادمة.",
                                        fontSize = 11.sp,
                                        color = Color(0xFF92400E),
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Summary
                    item {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ثمن الوجبات / المنتجات:", fontSize = 12.sp)
                                    Text("$subtotal دج", fontSize = 12.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("سعر التوصيل:", fontSize = 12.sp)
                                    if (deliveryDiscount > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "200 دج ",
                                                fontSize = 11.sp,
                                                style = TextStyle(textDecoration = TextDecoration.LineThrough),
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (effectiveDeliveryFee == 0) "مجاني 0 دج (خصم $deliveryDiscount دج) 🎉" else "$effectiveDeliveryFee دج (خصم $deliveryDiscount دج)",
                                                fontSize = 12.sp,
                                                color = Color(0xFF16A34A),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    } else {
                                        Text("200 دج", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("المجموع الكلي (عند الاستلام):", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("$totalWithDelivery دج", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onConfirmOrder(
                            customerName,
                            customerPhone,
                            selectedNeighborhood,
                            addressDescription,
                            selectedDriver,
                            deliveryDiscount,
                            selectedDiscountPoints
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (deliveryDiscount > 0) "تأكيد الطلب بخصم $deliveryDiscount دج (الدفع $totalWithDelivery دج) ✅" else "تأكيد الطلب الآن (الدفع عند الاستلام $totalWithDelivery دج) ✅",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerLiveTrackingScreen(
    order: OrderEntity,
    driver: DriverEntity?,
    etaMinutes: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isWebSocketConnected: Boolean = true,
    onSimulateMovement: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentStatus = try {
        OrderStatus.valueOf(order.status)
    } catch (e: Exception) {
        OrderStatus.ON_THE_WAY
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("تتبع الطلب المباشر 🛵", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("رقم الطلب: ${order.orderNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
                Box(modifier = Modifier.size(40.dp)) // Placeholder for balance
            }
        }

        // Stepper
        item {
            OrderStatusStepper(currentStatus = currentStatus)
        }

        // Live OpenStreetMap Canvas with Smooth Motorcycle Animation & WebSocket Updates
        item {
            SourElGhozlaneMapCanvas(
                shopLat = order.shopLat,
                shopLon = order.shopLon,
                customerLat = order.customerLat,
                customerLon = order.customerLon,
                driverLat = driver?.lat ?: order.shopLat,
                driverLon = driver?.lon ?: order.shopLon,
                driverSpeed = driver?.speed ?: 26.0,
                isWebSocketConnected = isWebSocketConnected,
                driverName = driver?.name ?: order.driverName ?: "أمين التوصيل"
            )
        }

        // Driver Info Card with Call button
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🛵", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = order.driverName ?: "أمين التوصيل",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = driver?.vehicleType ?: "دراجة نارية SYM 125",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Text(
                                text = "الوقت المقدر: $etaMinutes دقيقة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Call Button
                    Button(
                        onClick = {
                            val phone = order.driverPhone ?: "+213553333333"
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "اتصال", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اتصال", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Order Summary & COD Notice
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("تفاصيل الفاتورة (سور الغزلان)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("الوجبات: ${order.itemsSummary}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("عنوان التوصيل: ${order.neighborhood} - ${order.addressDescription}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المبلغ المطلوب تسليمه نقداً (COD):", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(
                            "${order.total} دج",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Action to trigger/replay live movement simulation with WebSocket broadcast
        if (onSimulateMovement != null) {
            item {
                OutlinedButton(
                    onClick = onSimulateMovement,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "محاكاة انطلاق دراجة التوصيل عبر WebSocket ⚡",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

/**
 * Screen displaying the customer's Order History from local storage array.
 * Shows past orders, their total prices, delivery dates, and delivery addresses.
 */
@Composable
fun CustomerOrderHistoryScreen(
    orderHistory: List<CustomerPastOrder>,
    shops: List<ShopEntity>,
    onBack: () -> Unit,
    onReorderShop: (ShopEntity) -> Unit,
    onTrackOrder: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSpent = orderHistory.sumOf { it.totalPrice }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "📋 سجل الطلبات السابقة",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ولاية البويرة • بلدية سور الغزلان (مخزنة محلياً)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Summary Stats Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "إجمالي الطلبات",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${orderHistory.size} طلبات",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "إجمالي المصروف",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$totalSpent دج",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "سعر التوصيل",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "200 دج ثابت",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "قائمة الطلبات السابقة ومواعيد التسليم:",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Empty state or Order cards
        if (orderHistory.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🛍️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا توجد طلبات سابقة بعد",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اختر مطعمك المفضل في سور الغزلان واستمتع بأشهى الوجبات بتوصيل 200 دج فقط.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تصفح المطاعم الآن ➔", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(orderHistory) { order ->
                val matchingShop = shops.find { it.name.trim() == order.shopName.trim() }

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Top row: Shop name & Order status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🏬", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = order.shopName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "رقم الطلب: ${order.orderNumber}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Status badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (order.status.contains("نجاح") || order.status.contains("التسليم")) {
                                    Color(0xFFDCFCE7)
                                } else {
                                    Color(0xFFFFEDD5)
                                }
                            ) {
                                Text(
                                    text = order.status,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (order.status.contains("نجاح") || order.status.contains("التسليم")) {
                                        Color(0xFF166534)
                                    } else {
                                        Color(0xFFC2410C)
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Delivery Date and Neighborhood
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📅 تاريخ التوصيل: ${order.deliveryDate}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "📍 ${order.neighborhood}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        if (!order.driverName.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "🛵 سائق التوصيل: ${order.driverName}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(8.dp))

                        // Meals summary
                        Text(
                            text = "الوجبات المطلوبة:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = order.itemsSummary,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Total Price and Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "المبلغ الإجمالي (مع 200 دج توصيل):",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = "${order.totalPrice} دج",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (matchingShop != null) {
                                    Button(
                                        onClick = { onReorderShop(matchingShop) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("طلب جديد 🔁", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
fun CustomerProfileScreen(
    customerName: String = "محمد - زبون سور الغزلان",
    customerPhone: String = "0550123456",
    neighborhood: String = "وسط المدينة، سور الغزلان",
    loyaltyStorage: CustomerLoyaltyStorage?,
    orderHistoryCount: Int = 0,
    onBack: () -> Unit,
    onOpenOrderHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val pointsBalance by (loyaltyStorage?.pointsBalance ?: kotlinx.coroutines.flow.MutableStateFlow(0)).collectAsState()
    val transactions by (loyaltyStorage?.transactions ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()

    val tierTitle = when {
        pointsBalance >= 500 -> "المستوى الذهبي 🥇"
        pointsBalance >= 200 -> "المستوى الفضي 🥈"
        else -> "المستوى البرونزي 🥉"
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top Navigation Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "الرجوع"
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "الملف الشخصي ونقاط الولاء ⭐",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "اكسب نقاطاً مع كل طلب واستبدلها بخصم التوصيل",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // 1. بطاقة نقاط الولاء الذهبية الفاخرة (Golden Loyalty Card)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD97706)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFB45309))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌟", fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "بطاقة ولاء SGdelivery",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                    Text(
                                        text = "مدينة سور الغزلان",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color.White.copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = tierTitle,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "رصيدك الحالي من النقاط:",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$pointsBalance",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "نقطة ولاء",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "القيمة: تعادل $pointsBalance دج خصم مباشر على التوصيل",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                text = "نشطة ومتاحة ✅",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFEF08A)
                            )
                        }
                    }
                }
            }
        }

        // 2. بطاقة بيانات المستخدم
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(46.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(customerName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("📱 $customerPhone • 📍 $neighborhood", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "إجمالي الطلبات السابقة: $orderHistoryCount طلبات",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        TextButton(onClick = onOpenOrderHistory) {
                            Text("فتح سجل الطلبات 📋", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 3. كيفية عمل برنامج نقاط الولاء
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "كيف يعمل نظام نقاط الولاء في سور الغزلان؟",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📦", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "اكسب 50 نقطة تلقائياً مع كل طلب مكتمل تستلمه بنجاح.",
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🏷️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "استبدل 100 نقطة بخصم 100 دج على تكلفة التوصيل (يصبح 100 دج بدل 200 دج).",
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🛵", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "استبدل 200 نقطة بتوصيل مجاني تماماً 0 دج لأي متجر أو مطعم!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16A34A)
                        )
                    }
                }
            }
        }

        // 4. سجل حركات النقاط (Transactions History)
        item {
            Text(
                text = "سجل حركات النقاط (${transactions.size}):",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        if (transactions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا توجد حركات نقاط مسجلة بعد. سيتم تسجيل نقاطك المكتسبة والمستبدلة هنا.",
                        modifier = Modifier.padding(16.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(transactions) { tx ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = if (tx.isEarned) Color(0xFFDCFCE7) else Color(0xFFFFEDD5),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(if (tx.isEarned) "⭐" else "🏷️", fontSize = 16.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(tx.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("📅 ${tx.date}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (tx.isEarned) Color(0xFFDCFCE7) else Color(0xFFFFEDD5)
                        ) {
                            Text(
                                text = if (tx.isEarned) "+${tx.points} نقطة" else "${tx.points} نقطة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (tx.isEarned) Color(0xFF166534) else Color(0xFFC2410C),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Return button
        item {
            Button(
                onClick = onBack,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Text("العودة للتسوق واكتساب النقاط 🛍️", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

