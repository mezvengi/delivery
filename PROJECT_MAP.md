# PROJECT_MAP — SGdelivery (سور الغزلان)

## هيكل المجلدات
```
SOUR/
├── server/           ← الباك إند النشط (Node 20 + Express) — يُبنى عبر Docker
├── mobile_app/       ← تطبيق Flutter للزبون والسائق
├── docs/             ← توثيق API
├── backups/          ← نسخ احتياطية SQL
├── docker-compose.yml ← يشير لـ server/
├── Caddyfile         ← Reverse proxy → backend:3000
└── test_cycle.js     ← اختبار شامل للدورة الكاملة
```

## نسخة الباك إند النشطة (`server/`)
- **Docker:** `docker-compose.yml` الأساسي
- **Auth:** `access+refresh tokens, RBAC` (دعم Google Sign-In و Firebase)
- **Schema:** `initDB()` برمجي داخل `db.js`
- **الأدوار:** `customer, driver, store, admin` + جداول ملفات شخصية لكل دور

## شاشات Flutter (`mobile_app/lib/`)
| الشاشة | الملف |
|---|---|
| Home (طلب) | `screens/home_screen.dart` |
| Driver Mode | `screens/driver_mode_screen.dart` |
| Driver Map | `screens/driver_map_screen.dart` |
| API Service | `services/api_service.dart` |
| Config | `config.dart` |

## API Endpoints — `server/src/routes/` (النشط)
| Method | Path | File |
|---|---|---|
| POST | /auth/register/customer | authRoutes.js |
| POST | /auth/register/driver | authRoutes.js |
| POST | /auth/register/store | authRoutes.js |
| POST | /auth/login | authRoutes.js |
| POST | /auth/refresh | authRoutes.js |
| POST | /auth/logout | authRoutes.js |
| GET | /auth/me | authRoutes.js |
| POST | /auth/send-otp | authRoutes.js |
| POST | /auth/verify-otp | authRoutes.js |
| POST | /auth/verify-phone | authRoutes.js |
| POST | /auth/google | authRoutes.js |
| GET | /shops | shopRoutes.js |
| GET | /shops/:id | shopRoutes.js |
| GET | /shops/:id/products | shopRoutes.js |
| POST | /shops | shopRoutes.js |
| POST | /shops/:id/products | shopRoutes.js |
| POST | /orders | orderRoutes.js |
| GET | /orders | orderRoutes.js |
| GET | /orders/:id | orderRoutes.js |
| POST | /orders/:id/select-driver | orderRoutes.js |
| POST | /orders/:id/assign-driver | orderRoutes.js |
| PATCH | /orders/:id/status | orderRoutes.js |
| POST | /drivers/location | driverRoutes.js |
| GET | /drivers/available | driverRoutes.js |
| GET | /drivers | driverRoutes.js |
| PATCH | /users/me/phone | usersRoutes.js |
| GET | /admin/pending-users | adminRoutes.js |
| GET | /admin/users | adminRoutes.js |
| PUT | /admin/users/:id/status | adminRoutes.js |
| DELETE | /admin/users/:id | adminRoutes.js |
| GET | /zones/active | zoneRoutes.js |
| GET,POST,PUT,DELETE | /neighborhoods | neighborhoodsRoutes.js |

## جداول القاعدة (server — initDB)
| الجدول | الحقول الأساسية |
|---|---|
| users | id, phone, full_name, password_hash, role, status, zone_id, firebase_uid, email, photo_url, phone_verified |
| customers | user_id, address |
| drivers | user_id, vehicle_type, license_plate, is_available |
| stores | user_id, name, category, address, phone, is_active |
| shops | id, user_id, name, category, address_description, lat, lng, zone_id, is_active |
| products | id, shop_id, name, price_da, is_available |
| orders | id, customer_id, shop_id, driver_id, status, delivery_neighborhood, items_total_da, delivery_fee_da, total_amount_da |
| order_items | order_id, product_id, product_name, unit_price_da, quantity, subtotal_da |
| driver_locations | driver_id, lat, lng, heading, is_online |
| otps | phone, code, expires_at, used |
| refresh_tokens | user_id, token_hash, expires_at, revoked |
| neighborhoods | name_arabic, name_french, lat, lon |

## تدفّق تسجيل الدخول
1. **OTP**: `POST /auth/send-otp` → كود 6 أرقام → `POST /auth/verify-otp` → JWT
2. **كلمة سر**: `POST /auth/login` → access+refresh tokens
3. **Google Sign-In**: Android يحصل على Firebase idToken → `POST /auth/google` → upsert user → JWT
4. **Firebase Phone**: Android يتحقق عبر Firebase SMS → `POST /auth/verify-phone` → JWT

## الاتصال بالخادم
- **REST**: `https://sour.serveirc.com/api/*` — كل الطلبات عبر Caddy reverse proxy
- **WebSocket**: `wss://sour.serveirc.com/ws` — تتبع السائقين + تحديث الطلبات لحظياً
  - Actions: `AUTH`, `SUBSCRIBE_ORDER`, `SUBSCRIBE_DRIVERS`, `UPDATE_DRIVER_LOCATION`, `PING`

## أوامر التشغيل
```powershell
docker compose up -d          # تشغيل (postgres + backend + caddy)
docker compose logs -f backend # سجلات
docker compose ps             # حالة الحاويات
node test_cycle.js            # اختبار الدورة الكاملة
```

## آخر التغييرات
1. [2026-10-03] إنشاء PROJECT_MAP.md
2. [2026-10-03] إصلاح #1: إزالة كود OTP من response الإنتاج (auth.js)
3. [2026-10-03] إصلاح #2: تقييد أكواد OTP التجريبية بوضع التطوير فقط (auth.js)
4. [2026-10-04] إصلاح #3: تقييد كلمات السر التجريبية بوضع التطوير فقط (auth.js)
5. [2026-10-04] إصلاح #4: إلزام JWT_SECRET من .env في الإنتاج (github_app/backend/auth.js)
6. [2026-10-04] إصلاح #5: إلزام JWT_ACCESS_SECRET و JWT_REFRESH_SECRET في الإنتاج (server/src/auth.js)
7. [2026-10-04] إصلاح #6: إضافة authMiddleware(['admin']) لـ 4 routes إدارة غير محمية (admin.js)
8. [2026-10-04] إصلاح الأخطاء (7-10, 15): إصلاح أعمدة الطلبات وإضافة حماية لإنشاء/تعديل الطلب (orders.js)
9. [2026-10-04] إصلاح #11: إزالة تحديث حقل vehicle_type من جدول users (drivers.js)
10. [2026-10-04] إصلاح #13: حظر فك تشفير توكن Firebase التجريبي في الإنتاج (auth.js)
11. [2026-10-04] إصلاح #14: إزالة كود OTP من الـ response في الخادم الأساسي (authRoutes.js)
12. [2026-10-04] إصلاح #16: إزالة قيد NOT NULL عن رقم الهاتف لتسهيل Google Sign-In (db.js)
13. [2026-10-04] إصلاح #17: إضافة شاشة تسجيل دخول حقيقية LoginScreen وحذف الدخول التلقائي التجريبي (Flutter)
14. [2026-10-04] إصلاح #18: فحص حساب المدير بناءً على role بدلاً من رقم الهاتف (admin.js)
15. [2026-10-04] مزامنة تطبيق Android (Kotlin/Compose) مع الباك إند الجديد عبر تعديل AuthRepository و SoriApiClient ليقرأ بيانات الملف الشخصي (address, vehicle_type, etc.) بشكل مسطّح ويتوافق مع TokensDto.
