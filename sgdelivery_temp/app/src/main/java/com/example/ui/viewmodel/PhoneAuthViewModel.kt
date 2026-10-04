package com.example.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// حالات واجهة التحقق عبر الهاتف
sealed class PhoneAuthUiState {
    object Idle : PhoneAuthUiState()
    object SendingCode : PhoneAuthUiState()
    data class CodeSent(val verificationId: String, val resendToken: PhoneAuthProvider.ForceResendingToken?) : PhoneAuthUiState()
    object Verifying : PhoneAuthUiState()
    data class Success(val idToken: String, val phoneNumber: String) : PhoneAuthUiState()
    data class Error(val message: String) : PhoneAuthUiState()
}

class PhoneAuthViewModel(
    customAuth: FirebaseAuth? = null
) : ViewModel() {

    private val auth: FirebaseAuth? = customAuth ?: try {
        FirebaseAuth.getInstance()
    } catch (e: Throwable) {
        null
    }

    private val _uiState = MutableStateFlow<PhoneAuthUiState>(PhoneAuthUiState.Idle)
    val uiState: StateFlow<PhoneAuthUiState> = _uiState.asStateFlow()

    private val _resendCountdown = MutableStateFlow(0)
    val resendCountdown: StateFlow<Int> = _resendCountdown.asStateFlow()

    private var countdownJob: Job? = null
    private var storedVerificationId: String? = null
    private var storedResendToken: PhoneAuthProvider.ForceResendingToken? = null
    var formattedPhoneNumber: String = ""
        private set

    /**
     * تحويل ومعالجة صيغ أرقام الهواتف الجزائرية إلى الصيغة الدولية E.164:
     * مثلاً: 0550123456 أو 550123456 أو 00213550123456 تصبح جميعها: +213550123456
     */
    fun normalizeAlgerianPhone(input: String): String? {
        val digitsOnly = input.replace(Regex("[^0-9+]"), "").trim()
        val cleaned = when {
            digitsOnly.startsWith("00213") -> "+213" + digitsOnly.substring(5)
            digitsOnly.startsWith("213") -> "+$digitsOnly"
            digitsOnly.startsWith("+213") -> digitsOnly
            digitsOnly.startsWith("0") && digitsOnly.length == 10 && digitsOnly[1] in listOf('5', '6', '7') -> {
                "+213" + digitsOnly.substring(1)
            }
            digitsOnly.length == 9 && digitsOnly[0] in listOf('5', '6', '7') -> {
                "+213$digitsOnly"
            }
            else -> return null
        }

        // يجب أن يبدأ بـ +213 ويليه 5 أو 6 أو 7 ثم 8 أرقام (إجمالي 13 محرف)
        val regex = Regex("^\\+213[567][0-9]{8}\$")
        return if (regex.matches(cleaned)) cleaned else null
    }

    /**
     * إرسال رمز التحقق SMS عبر Firebase Phone Auth
     */
    fun sendVerificationCode(activity: Activity, rawPhone: String, isResend: Boolean = false) {
        val normalized = normalizeAlgerianPhone(rawPhone)
        if (normalized == null) {
            _uiState.value = PhoneAuthUiState.Error("يرجى إدخال رقم هاتف جزائري صالح (يبدأ بـ 05 أو 06 أو 07)")
            return
        }

        formattedPhoneNumber = normalized
        _uiState.value = PhoneAuthUiState.SendingCode

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // التحقق التلقائي السريع (Auto-retrieval)
                signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "رقم الهاتف غير صالح أو صيغته خاطئة."
                    is FirebaseTooManyRequestsException -> "تم تجاوز عدد المحاولات المسموح بها مؤقتاً. يرجى الانتظار 15 دقيقة والمحاولة مجدداً."
                    else -> "فشل إرسال رسالة SMS: ${e.localizedMessage ?: "يرجى التأكد من اتصال الإنترنت وصلاحية إعدادات Firebase"}"
                }
                _uiState.value = PhoneAuthUiState.Error(errorMessage)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                storedResendToken = token
                _uiState.value = PhoneAuthUiState.CodeSent(verificationId, token)
                startResendTimer(60)
            }
        }

        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _uiState.value = PhoneAuthUiState.Error("خدمة التحقق من الهاتف عبر Firebase غير متوفرة حالياً.")
            return
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(normalized)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (isResend && storedResendToken != null) {
            optionsBuilder.setForceResendingToken(storedResendToken!!)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    /**
     * التحقق من الرمز المدخل يدوياً من طرف المستخدم
     */
    fun verifyCode(code: String) {
        val verificationId = storedVerificationId
        if (verificationId == null) {
            _uiState.value = PhoneAuthUiState.Error("انتهت صلاحية جلسة التحقق، يرجى طلب رمز جديد.")
            return
        }

        if (code.length != 6 || !code.all { it.isDigit() }) {
            _uiState.value = PhoneAuthUiState.Error("يرجى إدخال رمز التحقق المكون من 6 أرقام كاملة.")
            return
        }

        _uiState.value = PhoneAuthUiState.Verifying
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithPhoneAuthCredential(credential)
    }

    /**
     * تسجيل الدخول وجلب Firebase ID Token لإرساله للخادم الخاص
     */
    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        val firebaseAuth = auth
        if (firebaseAuth == null) {
            _uiState.value = PhoneAuthUiState.Error("خدمة التحقق عبر Firebase غير متوفرة.")
            return
        }

        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        user.getIdToken(true).addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val idToken = tokenTask.result?.token
                                if (!idToken.isNullOrEmpty()) {
                                    _uiState.value = PhoneAuthUiState.Success(idToken, formattedPhoneNumber)
                                } else {
                                    _uiState.value = PhoneAuthUiState.Error("تعذر استخراج رمز الجلسة الآمن (Token).")
                                }
                            } else {
                                _uiState.value = PhoneAuthUiState.Error("فشل الاتصال الآمن مع خادم التوثيق.")
                            }
                        }
                    } else {
                        _uiState.value = PhoneAuthUiState.Error("لم يتم العثور على بيانات المستخدم.")
                    }
                } else {
                    val ex = task.exception
                    val msg = when (ex) {
                        is FirebaseAuthInvalidCredentialsException -> "رمز التحقق (OTP) غير صحيح أو انتهت صلاحيته."
                        else -> ex?.localizedMessage ?: "فشل التحقق من الرمز المدخل."
                    }
                    _uiState.value = PhoneAuthUiState.Error(msg)
                }
            }
    }

    private fun startResendTimer(seconds: Int) {
        countdownJob?.cancel()
        _resendCountdown.value = seconds
        countdownJob = viewModelScope.launch {
            for (i in seconds downTo 1) {
                _resendCountdown.value = i
                delay(1000)
            }
            _resendCountdown.value = 0
        }
    }

    fun resetState() {
        _uiState.value = PhoneAuthUiState.Idle
    }
}
