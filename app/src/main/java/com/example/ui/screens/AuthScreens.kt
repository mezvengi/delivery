package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.config.ApiConstants
import com.example.data.models.AccountStatus
import com.example.data.models.RoleType
import com.example.data.models.SourElGhozlaneConstants
import com.example.data.models.UserAccount
import com.example.data.repository.AuthRepository
import com.example.ui.components.AppButton
import com.example.ui.components.AppTextField
import com.example.ui.components.RoleCard
import com.example.ui.components.ThemeToggleRow
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.launch

@Composable
fun RoleSelectionScreen(
    selectedRole: RoleType,
    onRoleSelected: (RoleType) -> Unit,
    onContinueToLogin: () -> Unit,
    onContinueToRegister: () -> Unit,
    currentThemeMode: AppThemeMode,
    onThemeModeChanged: (AppThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Theme mode switcher at top
        ThemeToggleRow(
            currentMode = currentThemeMode,
            onModeSelected = onThemeModeChanged
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Branding Header
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.TwoWheeler,
                    contentDescription = ApiConstants.APP_NAME,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "تطبيق ${ApiConstants.APP_NAME}",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "منظومة التوصيل الموحدة في بلدية سور الغزلان",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "اختر صفتك للمتابعة:",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Three Role Cards
        RoleType.values().forEach { role ->
            RoleCard(
                role = role,
                isSelected = role == selectedRole,
                onClick = { onRoleSelected(role) },
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        AppButton(
            text = "تسجيل الدخول كـ ${selectedRole.titleArabic}",
            onClick = onContinueToLogin
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onContinueToRegister,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "إنشاء حساب جديد كـ ${selectedRole.titleArabic}",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "الاتصال بالخادم: ${ApiConstants.BASE_URL}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun LoginScreen(
    role: RoleType,
    authRepository: AuthRepository,
    onLoginSuccess: (UserAccount) -> Unit,
    onGoToRegister: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var phone by remember {
        mutableStateOf(
            when (role) {
                RoleType.CUSTOMER -> "0550123456"
                RoleType.DRIVER -> "0660123456"
                RoleType.STORE -> "0770123456"
            }
        )
    }
    var password by remember { mutableStateOf("123456") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "تسجيل الدخول",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "مرحباً بك في سوري - تسجيل دخول حساب ${role.titleArabic}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Phone Input
        AppTextField(
            value = phone,
            onValueChange = {
                phone = it
                phoneError = null
                generalError = null
            },
            label = "رقم الهاتف (الجزائر +213)",
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone,
            errorMessage = phoneError
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Password Input
        AppTextField(
            value = password,
            onValueChange = {
                password = it
                passwordError = null
                generalError = null
            },
            label = "كلمة السر",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            errorMessage = passwordError
        )

        // Forgot Password Link
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            TextButton(onClick = { showForgotPasswordDialog = true }) {
                Text(
                    text = "نسيت كلمة السر؟",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        AnimatedVisibility(visible = generalError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = generalError ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppButton(
            text = "دخول",
            isLoading = isLoading,
            onClick = {
                var hasError = false
                if (!AuthRepository.isValidAlgerianPhone(phone)) {
                    phoneError = "يرجى إدخال رقم هاتف جزائري صحيح (مثل: 0550123456)"
                    hasError = true
                }
                if (!AuthRepository.isValidPassword(password)) {
                    passwordError = "كلمة السر يجب أن تحتوي على 6 أحرف على الأقل"
                    hasError = true
                }

                if (!hasError) {
                    isLoading = true
                    generalError = null
                    coroutineScope.launch {
                        val result = authRepository.login(phone, password)
                        isLoading = false
                        result.fold(
                            onSuccess = { user ->
                                onLoginSuccess(user)
                            },
                            onFailure = { err ->
                                generalError = err.message ?: "فشل تسجيل الدخول"
                            }
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Demo hint card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "ملاحظة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "بيانات تجريبية سريعة للاختبار:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "الهاتف الافتراضي: $phone • كلمة السر: 123456",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ليس لديك حساب؟",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onGoToRegister) {
                Text(
                    text = "إنشاء حساب جديد",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            title = {
                Text("استرجاع كلمة السر", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("لإعادة تعيين كلمة السر الخاصة بحسابك في بلدية سور الغزلان، يرجى التواصل مع الدعم الفني أو سيصلك رمز تأكيد عبر رسالة SMS على رقم هاتفك المسجل.")
            },
            confirmButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text("حسناً")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    role: RoleType,
    authRepository: AuthRepository,
    onRegisterSuccess: (UserAccount) -> Unit,
    onGoToLogin: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Customer specific
    var address by remember { mutableStateOf("") }
    var selectedNeighborhood by remember { mutableStateOf(SourElGhozlaneConstants.NEIGHBORHOODS.first().nameArabic) }

    // Driver specific
    var vehicleType by remember { mutableStateOf("دراجة نارية") }
    var plateNumber by remember { mutableStateOf("") }
    var idDocumentAttached by remember { mutableStateOf(false) }

    // Store specific
    var storeName by remember { mutableStateOf("") }
    var storeOwner by remember { mutableStateOf("") }
    var storeType by remember { mutableStateOf("مطعم ومأكولات") }

    // Errors & Loading
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "إنشاء حساب ${role.titleArabic}",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "انضم إلى شبكة سوري في بلدية سور الغزلان",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // General fields
        if (role != RoleType.STORE) {
            AppTextField(
                value = name,
                onValueChange = { name = it; nameError = null },
                label = "الاسم الكامل",
                leadingIcon = Icons.Default.Person,
                errorMessage = nameError
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        AppTextField(
            value = phone,
            onValueChange = { phone = it; phoneError = null },
            label = "رقم الهاتف (الجزائر +213)",
            leadingIcon = Icons.Default.Phone,
            keyboardType = KeyboardType.Phone,
            errorMessage = phoneError
        )

        Spacer(modifier = Modifier.height(12.dp))

        AppTextField(
            value = password,
            onValueChange = { password = it; passwordError = null },
            label = "كلمة السر (6 أحرف أو أرقام على الأقل)",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onTogglePasswordVisibility = { isPasswordVisible = !isPasswordVisible },
            errorMessage = passwordError
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Role-Specific Fields
        when (role) {
            RoleType.CUSTOMER -> {
                AppTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "العنوان التقريبي (الحي، رقم الباب أو العمارة)",
                    leadingIcon = Icons.Default.LocationOn
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "الحي في سور الغزلان:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SourElGhozlaneConstants.NEIGHBORHOODS.take(5).forEach { nh ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedNeighborhood = nh.nameArabic }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = selectedNeighborhood == nh.nameArabic,
                                onCheckedChange = { selectedNeighborhood = nh.nameArabic }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = nh.nameArabic,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            RoleType.DRIVER -> {
                AppTextField(
                    value = vehicleType,
                    onValueChange = { vehicleType = it },
                    label = "نوع المركبة (دراجة نارية / سيارة / فان)",
                    leadingIcon = Icons.Default.TwoWheeler
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = plateNumber,
                    onValueChange = { plateNumber = it },
                    label = "رقم لوحة الترقيم (Matricule)",
                    leadingIcon = Icons.Default.Badge
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { idDocumentAttached = !idDocumentAttached }
                        .padding(vertical = 6.dp)
                ) {
                    Checkbox(
                        checked = idDocumentAttached,
                        onCheckedChange = { idDocumentAttached = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "أؤكد توفر بطاقة الهوية الوطنية ورخصة السياقة السارية (اختيارية الآن)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "ملاحظة: حسابات السائقين تخضع لمراجعة وتفعيل الإدارة في سور الغزلان قبل بدء استقبال الطلبات.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            RoleType.STORE -> {
                AppTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = "اسم المتجر أو المطعم",
                    leadingIcon = Icons.Default.Store
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = storeOwner,
                    onValueChange = { storeOwner = it },
                    label = "اسم مالك المتجر أو المسؤول",
                    leadingIcon = Icons.Default.Person
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = "عنوان المحل في سور الغزلان",
                    leadingIcon = Icons.Default.LocationOn
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = storeType,
                    onValueChange = { storeType = it },
                    label = "نوع النشاط (مطعم / فاست فود / بقالة / حلويات)",
                    leadingIcon = Icons.Default.Store
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "ملاحظة: حسابات المتاجر تخضع لمراجعة وتفعيل الإدارة وتحديد الإحداثيات على الخريطة قبل عرضها للزبائن.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }

        AnimatedVisibility(visible = generalError != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = generalError ?: "",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AppButton(
            text = "تأكيد إنشاء الحساب",
            isLoading = isLoading,
            onClick = {
                var hasError = false
                if (role != RoleType.STORE && name.isBlank()) {
                    nameError = "يرجى كتابة الاسم"
                    hasError = true
                }
                if (role == RoleType.STORE && storeName.isBlank()) {
                    nameError = "يرجى كتابة اسم المتجر"
                    hasError = true
                }
                if (!AuthRepository.isValidAlgerianPhone(phone)) {
                    phoneError = "يرجى إدخال رقم هاتف جزائري صحيح (مثل: 0550123456)"
                    hasError = true
                }
                if (!AuthRepository.isValidPassword(password)) {
                    passwordError = "كلمة السر يجب أن لا تقل عن 6 أحرف"
                    hasError = true
                }

                if (!hasError) {
                    isLoading = true
                    generalError = null
                    coroutineScope.launch {
                        val result = when (role) {
                            RoleType.CUSTOMER -> {
                                authRepository.registerCustomer(
                                    name = name,
                                    phone = phone,
                                    pass = password,
                                    address = address,
                                    neighborhood = selectedNeighborhood
                                )
                            }
                            RoleType.DRIVER -> {
                                authRepository.registerDriver(
                                    name = name,
                                    phone = phone,
                                    pass = password,
                                    vehicleType = vehicleType,
                                    plateNumber = plateNumber,
                                    idDocumentAttached = idDocumentAttached
                                )
                            }
                            RoleType.STORE -> {
                                authRepository.registerStore(
                                    storeName = storeName,
                                    ownerName = storeOwner,
                                    phone = phone,
                                    pass = password,
                                    address = address,
                                    storeType = storeType,
                                    lat = SourElGhozlaneConstants.CENTER_LAT,
                                    lon = SourElGhozlaneConstants.CENTER_LON
                                )
                            }
                        }
                        isLoading = false
                        result.fold(
                            onSuccess = { user ->
                                onRegisterSuccess(user)
                            },
                            onFailure = { err ->
                                generalError = err.message ?: "فشل إنشاء الحساب"
                            }
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "لديك حساب بالفعل؟",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onGoToLogin) {
                Text(
                    text = "تسجيل الدخول",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun PendingApprovalScreen(
    user: UserAccount,
    onActivateNowForTesting: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.HourglassTop,
                    contentDescription = "قيد الانتظار",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "بانتظار موافقة الإدارة",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "مرحباً بك ${user.name} في تطبيق سوري.\nتم استلام طلب تسجيل حسابك كـ (${user.role.titleArabic}) وهو حالياً قيد المراجعة والتحقق من طرف مسؤولي بلدية سور الغزلان.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "بيانات الحساب:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "رقم الهاتف: ${user.phone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "الدور: ${user.role.titleArabic}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "الحالة: في انتظار الاعتماد", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Instant approval for testing mode
        AppButton(
            text = "تفعيل الحساب فوراً (وضع المعاينة والاختبار)",
            onClick = onActivateNowForTesting
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onLogout,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تسجيل الخروج والعودة", fontWeight = FontWeight.Bold)
        }
    }
}
