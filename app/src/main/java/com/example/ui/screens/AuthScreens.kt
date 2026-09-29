package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.platform.LocalContext
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
    onContinueToPhoneAuth: () -> Unit = {},
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

        // Public User Roles (Customer, Store, Driver - Admin hidden from regular users)
        val publicRoles = listOf(RoleType.CUSTOMER, RoleType.STORE, RoleType.DRIVER)
        publicRoles.forEach { role ->
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

        Button(
            onClick = onContinueToPhoneAuth,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(
                text = "📱 تفعيل الحساب برمز SMS (Firebase)",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }

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
    onGoToPhoneAuth: () -> Unit = {},
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
                RoleType.ADMIN -> "0555000000"
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

    var isOtpMode by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var isOtpSent by remember { mutableStateOf(false) }
    var otpMessage by remember { mutableStateOf<String?>(null) }
    var otpError by remember { mutableStateOf<String?>(null) }
    var whatsappUrl by remember { mutableStateOf<String?>(null) }
    var telegramUrl by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

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

        Spacer(modifier = Modifier.height(16.dp))

        // Tab switcher: Password vs OTP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (!isOtpMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { isOtpMode = false; generalError = null }
            ) {
                Text(
                    text = "كلمة السر",
                    fontSize = 12.sp,
                    fontWeight = if (!isOtpMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (!isOtpMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isOtpMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .clickable { isOtpMode = true; generalError = null }
            ) {
                Text(
                    text = "رمز التحقق (OTP)",
                    fontSize = 12.sp,
                    fontWeight = if (isOtpMode) FontWeight.Bold else FontWeight.Normal,
                    color = if (isOtpMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

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

        if (!isOtpMode) {
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
        } else {
            // OTP Mode
            if (isOtpSent) {
                if (!whatsappUrl.isNullOrBlank() || !telegramUrl.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (!whatsappUrl.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)))
                                    } catch (e: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366))
                            ) {
                                Text("واتساب (WhatsApp)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (!telegramUrl.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)))
                                    } catch (e: Exception) {}
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0088CC))
                            ) {
                                Text("تيليجرام (Telegram)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                AppTextField(
                    value = otpCode,
                    onValueChange = {
                        otpCode = it
                        otpError = null
                        generalError = null
                    },
                    label = "رمز التحقق المكون من 6 أرقام",
                    leadingIcon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Number,
                    errorMessage = otpError
                )

                if (otpMessage != null) {
                    Text(
                        text = otpMessage ?: "",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            } else {
                OutlinedButton(
                    onClick = {
                        if (!AuthRepository.isValidAlgerianPhone(phone)) {
                            phoneError = "يرجى إدخال رقم هاتف جزائري صحيح (مثل: 0550123456)"
                        } else {
                            isLoading = true
                            coroutineScope.launch {
                                val res = authRepository.sendOtp(phone)
                                isLoading = false
                                res.fold(
                                    onSuccess = { resp ->
                                        isOtpSent = true
                                        otpCode = resp.code ?: ""
                                        whatsappUrl = resp.whatsapp_url
                                        telegramUrl = resp.telegram_url
                                        otpMessage = "تم إنشاء رمز التحقق: ${resp.code ?: ""}"
                                    },
                                    onFailure = {
                                        generalError = "تعذر إرسال رمز التحقق"
                                    }
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("إرسال رمز التحقق (WhatsApp / Telegram)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            text = if (isOtpMode) "تأكيد الدخول بالرمز" else "دخول",
            isLoading = isLoading,
            onClick = {
                var hasError = false
                if (!AuthRepository.isValidAlgerianPhone(phone)) {
                    phoneError = "يرجى إدخال رقم هاتف جزائري صحيح (مثل: 0550123456)"
                    hasError = true
                }

                if (!isOtpMode) {
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
                } else {
                    if (otpCode.length < 4) {
                        otpError = "يرجى إدخال رمز التحقق المكون من 4 أرقام (1234)"
                        hasError = true
                    }

                    if (!hasError) {
                        isLoading = true
                        generalError = null
                        coroutineScope.launch {
                            val result = authRepository.verifyOtp(phone, otpCode, null)
                            isLoading = false
                            result.fold(
                                onSuccess = { user ->
                                    onLoginSuccess(user)
                                },
                                onFailure = { err ->
                                    generalError = err.message ?: "رمز التحقق غير صحيح"
                                }
                            )
                        }
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

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoToPhoneAuth,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("📱 أو تسجيل الدخول برمز SMS (Firebase)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

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
    onGoToPhoneAuth: () -> Unit = {},
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

    val context = LocalContext.current
    var isOtpSent by remember { mutableStateOf(false) }
    var otpCode by remember { mutableStateOf("") }
    var otpError by remember { mutableStateOf<String?>(null) }
    var whatsappUrl by remember { mutableStateOf<String?>(null) }
    var telegramUrl by remember { mutableStateOf<String?>(null) }
    var otpMessage by remember { mutableStateOf<String?>(null) }

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

            RoleType.ADMIN -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Text(
                        text = "ملاحظة: حسابات إدارة منصة سور الغزلان مخصصة لمسؤولي البلدية والنظام.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
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

        if (isOtpSent) {
            // Step 2 of Registration: Verify OTP received via WhatsApp/Telegram
            if (!whatsappUrl.isNullOrBlank() || !telegramUrl.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!whatsappUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366))
                        ) {
                            Text("فتح واتساب (WhatsApp)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!telegramUrl.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)))
                                } catch (e: Exception) {}
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0088CC))
                        ) {
                            Text("فتح تيليجرام (Telegram)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            AppTextField(
                value = otpCode,
                onValueChange = {
                    otpCode = it
                    otpError = null
                    generalError = null
                },
                label = "رمز التحقق المكون من 6 أرقام",
                leadingIcon = Icons.Default.Lock,
                keyboardType = KeyboardType.Number,
                errorMessage = otpError
            )

            if (otpMessage != null) {
                Text(
                    text = otpMessage ?: "",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppButton(
                text = "تأكيد والتحقق من الحساب (Verify OTP)",
                isLoading = isLoading,
                onClick = {
                    if (otpCode.length < 4) {
                        otpError = "يرجى إدخال رمز التحقق المكون من 6 أرقام"
                        return@AppButton
                    }
                    isLoading = true
                    generalError = null
                    coroutineScope.launch {
                        val result = authRepository.verifyOtp(
                            phone = phone,
                            code = otpCode,
                            fullName = if (role == RoleType.STORE) storeName else name,
                            role = role,
                            password = password,
                            address = address,
                            vehicleType = vehicleType,
                            licensePlate = plateNumber,
                            storeCategory = storeType
                        )
                        isLoading = false
                        result.fold(
                            onSuccess = { user ->
                                onRegisterSuccess(user)
                            },
                            onFailure = { err ->
                                generalError = err.message ?: "فشل التحقق من الرمز"
                            }
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = { isOtpSent = false },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تعديل البيانات أو إعادة طلب الرمز", fontSize = 12.sp)
            }
        } else {
            // Step 1: Send OTP via WhatsApp / Telegram
            AppButton(
                text = "إرسال رمز التحقق عبر واتساب / تيليجرام",
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
                            val res = authRepository.sendOtp(phone)
                            isLoading = false
                            res.fold(
                                onSuccess = { resp ->
                                    isOtpSent = true
                                    otpCode = resp.code ?: ""
                                    whatsappUrl = resp.whatsapp_url
                                    telegramUrl = resp.telegram_url
                                    otpMessage = resp.message ?: "تم إرسال رمز التحقق (${resp.code ?: ""})"
                                },
                                onFailure = { err ->
                                    generalError = err.message ?: "تعذر إرسال رمز التحقق"
                                }
                            )
                        }
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
            onClick = onGoToPhoneAuth,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("📱 أو التفعيل المباشر برمز SMS (Firebase)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

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
    authRepository: AuthRepository? = null,
    onActivateNowForTesting: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Server code generation state
    var isRequestingCode by remember { mutableStateOf(false) }
    var isCodeGenerated by remember { mutableStateOf(false) }
    var serverGeneratedCode by remember { mutableStateOf<String?>(null) }
    var whatsappUrl by remember { mutableStateOf<String?>(null) }
    var telegramUrl by remember { mutableStateOf<String?>(null) }
    var serverMessage by remember { mutableStateOf<String?>(null) }

    // Activation code input state
    var inputCode by remember { mutableStateOf("") }
    var isActivating by remember { mutableStateOf(false) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var activationError by remember { mutableStateOf<String?>(null) }
    var activationSuccess by remember { mutableStateOf<String?>(null) }

    // Polling / admin status check
    var isCheckingStatus by remember { mutableStateOf(false) }
    var checkStatusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "كود التفعيل",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "تفعيل حساب ${user.role.titleArabic}",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "توليد كود التفعيل من السيرفر والإرسال عبر تيليجرام أو واتساب",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Account Details Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صاحب الحساب: ${user.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = user.role.titleArabic,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "رقم الهاتف المسجل: ${user.phone}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "الحالة: بانتظار إدخال كود التفعيل (Pending)",
                    fontSize = 12.sp,
                    color = Color(0xFFD97706),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 1: Server Code Generator
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("1", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "توليد كود التفعيل من السيرفر",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "يقوم السيرفر بتوليد كود تحقق مشفر خاص بحسابك ويرسله عبر تيليجرام أو واتساب لرقمك (${user.phone}).",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                AppButton(
                    text = if (isCodeGenerated) "إعادة توليد كود جديد من السيرفر 🔄" else "طلب كود التفعيل من السيرفر 📲",
                    isLoading = isRequestingCode,
                    onClick = {
                        if (authRepository == null) return@AppButton
                        isRequestingCode = true
                        activationError = null
                        coroutineScope.launch {
                            val result = authRepository.requestActivationCode(user.phone)
                            isRequestingCode = false
                            result.fold(
                                onSuccess = { resp ->
                                    isCodeGenerated = true
                                    serverGeneratedCode = resp.code
                                    whatsappUrl = resp.whatsapp_url
                                    telegramUrl = resp.telegram_url
                                    serverMessage = resp.message ?: "تم توليد كود التفعيل من السيرفر بنجاح!"
                                },
                                onFailure = { err ->
                                    activationError = err.message ?: "فشل طلب كود التفعيل من السيرفر"
                                }
                            )
                        }
                    }
                )

                if (isCodeGenerated) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF22C55E).copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF15803D),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = serverMessage ?: "تم توليد كود التفعيل من السيرفر بنجاح!",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF15803D)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = "اضغط لفتح التطبيق واستلام الكود المسجل:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (!whatsappUrl.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl)))
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF25D366))
                                    ) {
                                        Text("فتح واتساب 🟢", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (!telegramUrl.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(telegramUrl)))
                                            } catch (e: Exception) {}
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0088CC))
                                    ) {
                                        Text("فتح تيليجرام 🔵", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (!serverGeneratedCode.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            inputCode = serverGeneratedCode ?: ""
                                            codeError = null
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "الكود الصادر من السيرفر: ${serverGeneratedCode}",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "اضغط للملء التلقائي ✍️",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Input Activation Code
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("2", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "إدخال كود التفعيل وتأكيد الحساب",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                AppTextField(
                    value = inputCode,
                    onValueChange = {
                        inputCode = it
                        codeError = null
                        activationError = null
                    },
                    label = "أدخل كود التفعيل (6 أرقام)",
                    leadingIcon = Icons.Default.Lock,
                    keyboardType = KeyboardType.Number,
                    errorMessage = codeError
                )

                if (activationError != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activationError ?: "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (activationSuccess != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activationSuccess ?: "",
                        fontSize = 12.sp,
                        color = Color(0xFF15803D),
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                AppButton(
                    text = "تأكيد الكود وتفعيل الحساب فوراً 🚀",
                    isLoading = isActivating,
                    onClick = {
                        if (inputCode.trim().length < 4) {
                            codeError = "يرجى كتابة كود التفعيل بشكل صحيح"
                            return@AppButton
                        }
                        if (authRepository == null) return@AppButton
                        isActivating = true
                        activationError = null
                        coroutineScope.launch {
                            val result = authRepository.activateWithCode(user.phone, inputCode)
                            isActivating = false
                            result.fold(
                                onSuccess = {
                                    activationSuccess = "تهانينا! تم تفعيل الحساب بنجاح، جاري فتح لوحة التحكم..."
                                },
                                onFailure = { err ->
                                    activationError = err.message ?: "كود التفعيل غير صحيح، يرجى إعادة المحاولة"
                                }
                            )
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Secondary / Alternative Options
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = {
                    if (authRepository == null) return@FilledTonalButton
                    isCheckingStatus = true
                    checkStatusMessage = null
                    coroutineScope.launch {
                        val result = authRepository.checkAuthStatus()
                        isCheckingStatus = false
                        result.onSuccess { updated ->
                            if (updated.status == AccountStatus.APPROVED) {
                                checkStatusMessage = "تم التحقق وتفعيل الحساب بنجاح!"
                            } else {
                                checkStatusMessage = "الحساب ما زال بانتظار إدخال كود التفعيل (pending)."
                            }
                        }.onFailure {
                            checkStatusMessage = "تعذر الاتصال بالخادم للتحقق من الحالة."
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isCheckingStatus) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("التحقق من حالة الحساب في الخادم (GET /api/auth/me)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (checkStatusMessage != null) {
                Text(
                    text = checkStatusMessage ?: "",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                )
            }

            FilledTonalButton(
                onClick = onActivateNowForTesting,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تفعيل تجريبي فوري (وضع الاختبار والتطوير)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("تسجيل الخروج والعودة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}
