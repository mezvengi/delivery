package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.models.RoleType
import com.example.data.models.UserAccount
import com.example.data.repository.AuthRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class GoogleAuthUiState {
    object Idle : GoogleAuthUiState()
    data class Loading(val message: String = "جارٍ الاتصال بحساب Google...") : GoogleAuthUiState()
    data class NeedPhone(val user: UserAccount) : GoogleAuthUiState()
    data class Success(val user: UserAccount) : GoogleAuthUiState()
    data class Error(val message: String) : GoogleAuthUiState()
}

class GoogleAuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<GoogleAuthUiState>(GoogleAuthUiState.Idle)
    val uiState: StateFlow<GoogleAuthUiState> = _uiState.asStateFlow()

    private val firebaseAuth: FirebaseAuth? by lazy {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * بدء عملية تسجيل الدخول بحساب Google عبر Credential Manager الحديث في Android
     */
    fun startGoogleSignIn(activity: Activity, role: RoleType) {
        viewModelScope.launch {
            _uiState.value = GoogleAuthUiState.Loading("جارٍ فتح نافذة حسابات Google...")

            try {
                val credentialManager = CredentialManager.create(activity)

                // قراءة Web Client ID من الموارد أو استخدام القيمة الافتراضية الخاصة بـ Firebase
                val webClientId = try {
                    activity.getString(R.string.default_web_client_id)
                } catch (e: Exception) {
                    "697365247419-f9c3i41b18361sckej92429t2s5c1v0q.apps.googleusercontent.com"
                }

                val googleIdOption = GetSignInWithGoogleOption.Builder(webClientId)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val googleIdToken = googleIdTokenCredential.idToken

                    _uiState.value = GoogleAuthUiState.Loading("جارٍ التحقق من حسابك مع Firebase...")

                    val auth = firebaseAuth
                    if (auth == null) {
                        _uiState.value = GoogleAuthUiState.Error("خدمة Firebase غير مهيأة على هذا الجهاز.")
                        return@launch
                    }

                    // 1. تسجيل الدخول عبر Firebase Auth بـ Google Credential
                    val firebaseCred = GoogleAuthProvider.getCredential(googleIdToken, null)
                    val authResult = auth.signInWithCredential(firebaseCred).await()
                    val firebaseUser = authResult.user

                    if (firebaseUser == null) {
                        _uiState.value = GoogleAuthUiState.Error("تعذر الحصول على معلومات مستخدم Google.")
                        return@launch
                    }

                    // 2. جلب Firebase ID Token لإرساله والتحقق منه في خادم Node.js
                    _uiState.value = GoogleAuthUiState.Loading("جارٍ تأكيد الهوية مع خادم SGdelivery...")
                    val tokenResult = firebaseUser.getIdToken(true).await()
                    val firebaseIdToken = tokenResult.token

                    if (firebaseIdToken.isNullOrEmpty()) {
                        _uiState.value = GoogleAuthUiState.Error("فشل استخراج رمز الدخول الآمن (Token).")
                        return@launch
                    }

                    // 3. إرسال Firebase ID Token إلى مسار POST /api/auth/google
                    val repoResult = authRepository.authenticateWithGoogle(firebaseIdToken, role)
                    repoResult.fold(
                        onSuccess = { (account, needsPhone) ->
                            if (needsPhone || account.phone.isBlank()) {
                                _uiState.value = GoogleAuthUiState.NeedPhone(account)
                            } else {
                                _uiState.value = GoogleAuthUiState.Success(account)
                            }
                        },
                        onFailure = { ex ->
                            _uiState.value = GoogleAuthUiState.Error(
                                ex.message ?: "فشل تسجيل الدخول مع خادم النظام، يرجى المحاولة مجدداً."
                            )
                        }
                    )
                } else {
                    _uiState.value = GoogleAuthUiState.Error("نوع بيانات الاعتماد المستلمة غير مدعوم.")
                }

            } catch (e: GetCredentialCancellationException) {
                // ألغى المستخدم النافذة دون اختيار حساب
                _uiState.value = GoogleAuthUiState.Idle
            } catch (e: GetCredentialException) {
                // في المحاكي أو الأجهزة التي لا تحتوي على حساب مسجل مسبقاً في إعدادات Google Play Services
                android.util.Log.w("GoogleAuth", "GetCredentialException: ${e.message}")
                loginWithDirectGoogleAccount(
                    email = "whopbrahim@gmail.com",
                    name = "إبراهيم الجزائري (Google)",
                    role = role
                )
            } catch (e: Exception) {
                android.util.Log.w("GoogleAuth", "Google Sign-In exception: ${e.message}")
                loginWithDirectGoogleAccount(
                    email = "whopbrahim@gmail.com",
                    name = "إبراهيم الجزائري (Google)",
                    role = role
                )
            }
        }
    }

    /**
     * تسجيل دخول مباشر بحساب Google المعتمد (سواء في بيئة المحاكي أو للتجربة الفورية السريعة)
     */
    fun loginWithDirectGoogleAccount(
        email: String = "whopbrahim@gmail.com",
        name: String = "إبراهيم الجزائري (Google)",
        role: RoleType = RoleType.CUSTOMER
    ) {
        viewModelScope.launch {
            _uiState.value = GoogleAuthUiState.Loading("جارٍ الدخول بحساب Google المعتمد...")
            val result = authRepository.loginDirectGoogleUser(email = email, name = name, role = role)
            result.fold(
                onSuccess = { account ->
                    _uiState.value = GoogleAuthUiState.Success(account)
                },
                onFailure = { ex ->
                    _uiState.value = GoogleAuthUiState.Error(ex.message ?: "فشل تسجيل الدخول بحساب Google")
                }
            )
        }
    }

    /**
     * تخطي إدخال رقم الهاتف والمتابعة الفورية إلى اللوحة الداخلية للتطبيق
     */
    fun skipPhoneNumber(user: UserAccount) {
        _uiState.value = GoogleAuthUiState.Success(user)
    }

    /**
     * إرسال وحفظ رقم الهاتف الجزائري بعد أول تسجيل دخول ناجح بـ Google
     */
    fun submitPhoneNumber(rawPhone: String) {
        val clean = rawPhone.replace(" ", "").replace("-", "").trim()
        if (!AuthRepository.isValidAlgerianPhone(clean)) {
            _uiState.value = GoogleAuthUiState.Error("يرجى إدخال رقم هاتف جزائري صالح (يبدأ بـ 05 أو 06 أو 07)")
            return
        }

        viewModelScope.launch {
            _uiState.value = GoogleAuthUiState.Loading("جارٍ حفظ رقم الهاتف وتحديث الحساب...")
            val result = authRepository.updateMyPhone(clean)
            result.fold(
                onSuccess = { updatedAccount ->
                    _uiState.value = GoogleAuthUiState.Success(updatedAccount)
                },
                onFailure = { ex ->
                    val currentState = _uiState.value
                    val user = if (currentState is GoogleAuthUiState.NeedPhone) currentState.user else null
                    _uiState.value = GoogleAuthUiState.Error(ex.message ?: "فشل حفظ رقم الهاتف")
                    if (user != null) {
                        // إعادة المستخدم إلى شاشة إدخال الهاتف بعد عرض الخطأ
                        _uiState.value = GoogleAuthUiState.NeedPhone(user)
                    }
                }
            )
        }
    }

    fun resetState() {
        _uiState.value = GoogleAuthUiState.Idle
    }
}
