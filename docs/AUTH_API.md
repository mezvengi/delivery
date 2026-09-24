# دليل واجهات المصادقة والأدوار (Authentication & RBAC API)

تطبيق توصيل سور الغزلان (Sour El Ghozlane Delivery MVP)

---

## 1. تسجيل مستخدم جديد (Register)

### أ. تسجيل زبون (Customer) - يُفعل فوراً
- **المسار:** `POST /auth/register/customer`
- **الحالة الافتراضية:** `active`
- **مثال الطلب (curl):**
```bash
curl -k -X POST https://localhost/auth/register/customer \
  -H "Content-Type: application/json" \
  --data-raw '{"phone":"0550112233","password":"password123","full_name":"عمر الزبون","address":"حي الوئام، سور الغزلان"}'
```
- **الرد الناجح (201 Created):**
```json
{
  "success": true,
  "message": "تم إنشاء حساب الزبون بنجاح",
  "user": {
    "id": 6,
    "phone": "+213550112233",
    "full_name": "عمر الزبون",
    "role": "customer",
    "status": "active",
    "zone_id": "sour_el_ghozlane"
  },
  "tokens": {
    "accessToken": "ey...",
    "refreshToken": "ey...",
    "expiresIn": "15m"
  }
}
```

---

### ب. تسجيل سائق (Driver) - بانتظار موافقة الإدارة
- **المسار:** `POST /auth/register/driver`
- **الحالة الافتراضية:** `pending`
- **مثال الطلب (curl):**
```bash
curl -k -X POST https://localhost/auth/register/driver \
  -H "Content-Type: application/json" \
  --data-raw '{"phone":"0660445566","password":"driverPass123","full_name":"سفيان السائق","vehicle_type":"moto","license_plate":"12345-126-10"}'
```
- **الرد الناجح (201 Created):**
```json
{
  "success": true,
  "message": "تم تسجيل طلب انضمام السائق بنجاح. الحساب قيد المراجعة بانتظار موافقة الإدارة.",
  "user": {
    "id": 7,
    "phone": "+213660445566",
    "full_name": "سفيان السائق",
    "role": "driver",
    "status": "pending"
  },
  "driver_profile": {
    "vehicle_type": "moto",
    "license_plate": "12345-126-10"
  }
}
```

---

### ج. تسجيل متجر / مطعم (Store) - بانتظار موافقة الإدارة
- **المسار:** `POST /auth/register/store`
- **الحالة الافتراضية:** `pending`
- **مثال الطلب (curl):**
```bash
curl -k -X POST https://localhost/auth/register/store \
  -H "Content-Type: application/json" \
  --data-raw '{"phone":"0770778899","password":"storePass123","full_name":"صالح التاجر","store_name":"محل البركة","category":"groceries","address":"وسط المدينة، سور الغزلان"}'
```
- **الرد الناجح (201 Created):**
```json
{
  "success": true,
  "message": "تم تسجيل المتجر بنجاح. الحساب قيد المراجعة بانتظار موافقة الإدارة.",
  "user": {
    "id": 8,
    "phone": "+213770778899",
    "full_name": "صالح التاجر",
    "role": "store",
    "status": "pending"
  },
  "store_profile": {
    "store_name": "محل البركة",
    "category": "groceries",
    "address": "وسط المدينة، سور الغزلان"
  }
}
```

---

## 2. تسجيل الدخول والجلسات (Login & Session)

### أ. تسجيل الدخول (Login)
- **المسار:** `POST /auth/login`
- **الحماية:** مزود بنظام Rate Limiter (بحد أقصى 5 محاولات خاطئة لكل 15 دقيقة)
- **مثال الطلب (curl):**
```bash
curl -k -X POST https://localhost/auth/login \
  -H "Content-Type: application/json" \
  --data-raw '{"phone":"+213555000000","password":"AdminSour2026!"}'
```
- **الرد الناجح (200 OK):**
```json
{
  "success": true,
  "message": "تم تسجيل الدخول بنجاح",
  "tokens": {
    "accessToken": "eyJ...",
    "refreshToken": "eyJ...",
    "expiresIn": "15m"
  },
  "user": {
    "id": 1,
    "phone": "+213555000000",
    "full_name": "مسؤول النظام (سور الغزلان)",
    "role": "admin",
    "status": "active",
    "zone_id": "sour_el_ghozlane"
  }
}
```

---

### ب. تجديد الرمز (Refresh Token Rotation)
- **المسار:** `POST /auth/refresh`
- **الوظيفة:** عند انتهاء صلاحية الـ Access Token (15 دقيقة)، يتم إرسال الـ Refresh Token للحصول على زوج جديد وإبطال الرمز القديم تلقائياً.
- **مثال الطلب (curl):**
```bash
curl -k -X POST https://localhost/auth/refresh \
  -H "Content-Type: application/json" \
  --data-raw '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

---

### ج. تسجيل الخروج وإبطال الجلسة (Logout)
- **المسار:** `POST /auth/logout`
- **مثال الطلب (curl):**
```bash
curl -k -X POST https://localhost/auth/logout \
  -H "Content-Type: application/json" \
  --data-raw '{"refreshToken":"YOUR_REFRESH_TOKEN"}'
```

---

### د. جلب بيانات المستخدم الحالي (Current User)
- **المسار:** `GET /auth/me`
- **الترويسة المطلوبة:** `Authorization: Bearer <ACCESS_TOKEN>`
- **مثال الطلب (curl):**
```bash
curl -k -X GET https://localhost/auth/me \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 3. مسارات الإدارة والموافقة (Admin Endpoints)

> ملاحظة: جميع مسارات الإدارة تتطلب إرسال `Authorization: Bearer <ADMIN_ACCESS_TOKEN>`.

### أ. عرض الحسابات المعلقة بانتظار الموافقة (Pending Users)
- **المسار:** `GET /admin/pending-users`
- **يعيد:** قائمة بجميع السائقين والمتاجر الذين بحالة `pending`.
- **مثال الطلب (curl):**
```bash
curl -k -X GET https://localhost/admin/pending-users \
  -H "Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN"
```

---

### ب. تفعيل أو تعليق حساب مستخدم (Activate / Suspend)
- **المسار:** `PUT /admin/users/:id/status`
- **الحالات المقبولة:** `active` (تفعيل) أو `suspended` (تعليق)
- عند تعليق الحساب يتم إبطال جميع جلساته النشطة فوراً ومنعه من تسجيل الدخول.
- **مثال الطلب (curl):**
```bash
curl -k -X PUT https://localhost/admin/users/7/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ADMIN_ACCESS_TOKEN" \
  --data-raw '{"status":"active"}'
```
