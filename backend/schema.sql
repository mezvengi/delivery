-- PostgreSQL Schema for Sour El Ghozlane Delivery
-- Encoding: UTF8

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USERS TABLE (Roles: customer, shop, driver, admin)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) UNIQUE,
    email VARCHAR(255),
    firebase_uid VARCHAR(128) UNIQUE,
    photo_url TEXT,
    role VARCHAR(20) NOT NULL DEFAULT 'customer' CHECK (role IN ('customer', 'shop', 'driver', 'admin')),
    password_hash VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    phone_verified BOOLEAN DEFAULT FALSE,
    phone_verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Migration support for existing databases (Safe ALTER TABLE IF NOT EXISTS)
ALTER TABLE users ALTER COLUMN phone DROP NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS firebase_uid VARCHAR(128);
ALTER TABLE users ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS photo_url TEXT;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone_verified_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS license_plate VARCHAR(45);
ALTER TABLE users ADD COLUMN IF NOT EXISTS drivers_license VARCHAR(60);
ALTER TABLE users ADD COLUMN IF NOT EXISTS completed_orders_count INT DEFAULT 0;
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_firebase_uid ON users(firebase_uid);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_phone_unique ON users(phone);

-- 2. SHOPS TABLE
CREATE TABLE IF NOT EXISTS shops (
    id SERIAL PRIMARY KEY,
    user_id INT REFERENCES users(id) ON DELETE SET NULL,
    name VARCHAR(150) NOT NULL,
    category VARCHAR(60) NOT NULL,
    neighborhood VARCHAR(100) NOT NULL,
    address_description TEXT,
    lat NUMERIC(9, 6) DEFAULT 36.1480,
    lon NUMERIC(9, 6) DEFAULT 3.6900,
    phone VARCHAR(20),
    is_open BOOLEAN DEFAULT TRUE,
    delivery_fee INT DEFAULT 200, -- Fixed 200 DA
    image_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. PRODUCTS TABLE
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    shop_id INT REFERENCES shops(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    price INT NOT NULL, -- In Algerian Dinar (DA)
    category VARCHAR(60) DEFAULT 'وجبات',
    is_available BOOLEAN DEFAULT TRUE,
    image_url TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. DRIVER LOCATIONS & STATUS
CREATE TABLE IF NOT EXISTS driver_locations (
    id SERIAL PRIMARY KEY,
    driver_id INT UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    driver_name VARCHAR(120),
    phone VARCHAR(20),
    vehicle_type VARCHAR(60) DEFAULT 'دراجة نارية', -- Scooter/Motorbike
    is_online BOOLEAN DEFAULT FALSE,
    lat NUMERIC(9, 6) DEFAULT 36.1480,
    lon NUMERIC(9, 6) DEFAULT 3.6900,
    speed NUMERIC(5, 2) DEFAULT 0,
    heading NUMERIC(5, 2) DEFAULT 0,
    zone_id VARCHAR(50) DEFAULT 'sour_el_ghozlane',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. ORDERS TABLE (Cash On Delivery - COD)
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    order_number VARCHAR(30) UNIQUE NOT NULL,
    customer_id INT REFERENCES users(id) ON DELETE SET NULL,
    customer_name VARCHAR(120) NOT NULL,
    customer_phone VARCHAR(20) NOT NULL,
    shop_id INT REFERENCES shops(id) ON DELETE RESTRICT,
    driver_id INT REFERENCES users(id) ON DELETE SET NULL,
    neighborhood VARCHAR(100) NOT NULL,
    address_description TEXT NOT NULL,
    customer_lat NUMERIC(9, 6),
    customer_lon NUMERIC(9, 6),
    status VARCHAR(30) NOT NULL DEFAULT 'NEW' CHECK (status IN ('NEW', 'SHOP_ACCEPTED', 'PREPARING', 'READY_FOR_PICKUP', 'ON_THE_WAY', 'DELIVERED', 'CANCELLED')),
    subtotal INT NOT NULL, -- Items total in DA
    delivery_fee INT NOT NULL DEFAULT 200, -- 200 DA
    total INT NOT NULL, -- Subtotal + 200 DA
    payment_method VARCHAR(20) DEFAULT 'COD', -- Cash on Delivery
    notes TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. ORDER ITEMS
CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(id) ON DELETE CASCADE,
    product_id INT REFERENCES products(id) ON DELETE SET NULL,
    product_name VARCHAR(150) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    unit_price INT NOT NULL,
    total_price INT NOT NULL
);

-- 7. OTPS TABLE (Temporary verification tokens)
CREATE TABLE IF NOT EXISTS otps (
    id SERIAL PRIMARY KEY,
    phone VARCHAR(20) NOT NULL,
    code VARCHAR(6) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    is_used BOOLEAN DEFAULT FALSE
);

-- Indexes for lightning fast queries
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_orders_shop ON orders(shop_id);
CREATE INDEX IF NOT EXISTS idx_orders_driver ON orders(driver_id);
CREATE INDEX IF NOT EXISTS idx_driver_locations_online ON driver_locations(is_online);

-- ==============================================================================
-- INITIAL SEED DATA FOR SOUR EL GHOZLANE (سور الغزلان - ولاية البويرة)
-- ==============================================================================

-- 1. Insert Initial Users
INSERT INTO users (name, phone, role, password_hash) VALUES
('إدارة التوصيل سور الغزلان', '+213550000000', 'admin', '$2a$10$w3qW5eI7n1Wc7G3bMvI.g.L68OwhTjG9V36d.aE4v2o1h3F0r7zGy'), -- pass: admin123
('مطعم الأوراس للشواء والوجبات', '+213551111111', 'shop', '$2a$10$w3qW5eI7n1Wc7G3bMvI.g.L68OwhTjG9V36d.aE4v2o1h3F0r7zGy'),
('بيتزا وسندويشات البرج', '+213552222222', 'shop', '$2a$10$w3qW5eI7n1Wc7G3bMvI.g.L68OwhTjG9V36d.aE4v2o1h3F0r7zGy'),
('أمين التوصيل (دراجة نارية)', '+213553333333', 'driver', '$2a$10$w3qW5eI7n1Wc7G3bMvI.g.L68OwhTjG9V36d.aE4v2o1h3F0r7zGy'),
('كريم السريع (سكوتر فوري)', '+213554444444', 'driver', '$2a$10$w3qW5eI7n1Wc7G3bMvI.g.L68OwhTjG9V36d.aE4v2o1h3F0r7zGy'),
('زبون تجريبي (سور الغزلان)', '+213555555555', 'customer', '$2a$10$w3qW5eI7n1Wc7G3bMvI.g.L68OwhTjG9V36d.aE4v2o1h3F0r7zGy')
ON CONFLICT (phone) DO NOTHING;

-- 2. Insert Shops in Sour El Ghozlane
INSERT INTO shops (id, user_id, name, category, neighborhood, address_description, lat, lon, phone, delivery_fee) VALUES
(1, 2, 'مطعم الأوراس التقليدي والمشاوي', 'مطاعم ومأكولات', 'وسط المدينة', 'شارع أول نوفمبر، قرب ساحة البلدية، سور الغزلان', 36.1485, 3.6905, '+213551111111', 200),
(2, 3, 'بيتزا وبرغر البرج العائلي', 'مطاعم ومأكولات', 'حي ذراع البرج', 'حي ذراع البرج، الطريق الرئيسي، سور الغزلان', 36.1550, 3.6840, '+213552222222', 200),
(3, NULL, 'سوبرماركت النور للمواد الغذائية', 'مواد غذائية وسوبرماركت', 'وسط المدينة', 'نهج الاستقلال قرب البريد المركزي', 36.1478, 3.6915, '+213558888888', 200),
(4, NULL, 'إلكترونيات سيتي للهواتف والتجهيزات', 'أجهزة إلكترونية وهواتف', 'حي ذراع البرج', 'مقابل الثانوية الجديدة، سور الغزلان', 36.1535, 3.6860, '+213559999999', 200),
(5, NULL, 'بوتيك الأناقة للألبسة والأزياء', 'ألبسة وأحذية وأزياء', 'حي باب الجزائر', 'شارع التجارة، باب الجزائر، سور الغزلان', 36.1490, 3.6875, '+213557777777', 200),
(6, NULL, 'صيدلية الشفاء والمستلزمات الطبية', 'صيدلية ومستلزمات صحية', 'حي 500 مسكن', 'قرب العيادة متعددة الخدمات، سور الغزلان', 36.1450, 3.6930, '+213556666666', 200),
(7, NULL, 'كوسميتيك وعطور الياسمين', 'عطور ومستحضرات تجميل', 'حي الغريبة', 'قرب محطة الحافلات القديمة', 36.1420, 3.6950, '+213554444445', 200),
(8, NULL, 'مكتبة النجاح للكتب والأدوات المدرسية', 'مكتبات وأدوات مدرسية', 'حي الوئام', 'بجانب مدرسة ابن خلدون، سور الغزلان', 36.1510, 3.6970, '+213553333334', 200)
ON CONFLICT (id) DO UPDATE SET category = EXCLUDED.category, name = EXCLUDED.name;

-- 3. Insert Products
INSERT INTO products (shop_id, name, description, price, category, image_url) VALUES
(1, 'شواء دجاج مشوي على الفحم (نصف دجاجة)', 'متبل مع خبز طازج وبطاطا مقلية وصلصة حارة وثومية', 750, 'مشويات', 'https://images.unsplash.com/photo-1598515214211-89d3c73ae83b?w=500&auto=format&fit=crop&q=80'),
(1, 'سندويش كبدة مشوية دبل', 'كبدة عجل طازجة مع توابل جزائرية وسلطة وبطاطا', 400, 'سندويشات', 'https://images.unsplash.com/photo-1544025162-d76694265947?w=500&auto=format&fit=crop&q=80'),
(2, 'بيتزا ميغا تشيز 4 أجبان', 'موزاريلا، غودا، جبن كاممبير وصلصة بيضاء', 800, 'بيتزا', 'https://images.unsplash.com/photo-1513104890138-7c749659a591?w=500&auto=format&fit=crop&q=80'),
(2, 'برغر لحم دبل ميكس تشيز', 'شريحتان لحم بقري محلي مع بطاطا وصلصة خاصة', 500, 'برغر', 'https://images.unsplash.com/photo-1568901346375-23c9450c58cd?w=500&auto=format&fit=crop&q=80'),
(3, 'زيت المائدة إيليو 5 لتر', 'زيت نباتي صافي للقلي والطبخ', 650, 'مواد استهلاكية', 'https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?w=500&auto=format&fit=crop&q=80'),
(3, 'كيس سميد سيم ممتاز 10 كلغ', 'سميد متوسط عالي الجودة للكسكسي والخبز', 450, 'حبوب وبقوليات', 'https://images.unsplash.com/photo-1509440159596-0249088772ff?w=500&auto=format&fit=crop&q=80'),
(3, 'جبن مثلثات لافاش كيري علبة 24 قطعة', 'جبن طري غني بالكالسيوم', 380, 'مشتقات الحليب', 'https://images.unsplash.com/photo-1486297678162-eb2a19b0a32d?w=500&auto=format&fit=crop&q=80'),
(4, 'سماعات بلوتوث لاسلكية عازلة للضوضاء', 'بطارية تدوم 24 ساعة مع علبة شحن سريعة', 2800, 'ملحقات هواتف', 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500&auto=format&fit=crop&q=80'),
(4, 'باور بانك 20000 ميلي أمبير شحن فائق', 'منفذان Type-C و USB شحن سريع أصلي', 3200, 'شواحن وبطاريات', 'https://images.unsplash.com/photo-1609592424368-2a2990d0b0f4?w=500&auto=format&fit=crop&q=80'),
(5, 'قميص رجالي قطني صيفي كاجوال', 'قطن 100% أنيق ومريح متوفر بعدة مقاسات', 2200, 'ألبسة رجالية', 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=500&auto=format&fit=crop&q=80'),
(5, 'حذاء رياضي مريح للجري والمشي', 'نعل طبي مضاد للانزلاق عالي الجودة', 3500, 'أحذية', 'https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500&auto=format&fit=crop&q=80'),
(6, 'حليب أطفال سيريلاك غني بالفيتامينات 400غ', 'غذاء مكمل مدعم بالحديد والزنك للرضع', 580, 'تغذية الأطفال', 'https://images.unsplash.com/photo-1584308666744-24d5c474f2ae?w=500&auto=format&fit=crop&q=80'),
(7, 'عطر رجالي شرقي فاخر مسك وعنبر 100 مل', 'ثبات يدوم 48 ساعة برائحة جذابة راقية', 2900, 'عطور', 'https://images.unsplash.com/photo-1523293182086-7651a899d37f?w=500&auto=format&fit=crop&q=80'),
(8, 'طقم أدوات مدرسية ومحفظة متكاملة', 'أقلام، دفاتر، مساطر وألوان عالية الجودة', 1800, 'أدوات مدرسية', 'https://images.unsplash.com/photo-1456513080510-7bf3a84b82f8?w=500&auto=format&fit=crop&q=80')
ON CONFLICT DO NOTHING;

-- 4. Insert Driver Initial Positions
INSERT INTO driver_locations (driver_id, driver_name, phone, vehicle_type, is_online, lat, lon, speed, heading, zone_id) VALUES
(4, 'أمين التوصيل (دراجة نارية SYM)', '+213553333333', 'دراجة نارية SYM 125', TRUE, 36.1482, 3.6912, 28.5, 45.0, 'sour_el_ghozlane'),
(5, 'كريم السريع (سكوتر فوري)', '+213554444444', 'سكوتر Peugeot Tweet', TRUE, 36.1465, 3.6890, 32.0, 180.0, 'sour_el_ghozlane')
ON CONFLICT (driver_id) DO NOTHING;

-- Dynamic Delivery & Driver Navigation Enhancements (Deliverio Port)
ALTER TABLE driver_locations ADD COLUMN IF NOT EXISTS license_plate VARCHAR(45);
ALTER TABLE driver_locations ADD COLUMN IF NOT EXISTS completed_orders_count INT DEFAULT 0;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS delivery_leg VARCHAR(30) DEFAULT 'TO_SHOP';
ALTER TABLE orders ADD COLUMN IF NOT EXISTS is_manual_shop_order BOOLEAN DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS estimated_distance_km NUMERIC(5, 2);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS estimated_duration_min INT;

