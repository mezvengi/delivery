-- PostgreSQL Schema for Sour El Ghozlane Delivery
-- Encoding: UTF8

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USERS TABLE (Roles: customer, shop, driver, admin)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'customer' CHECK (role IN ('customer', 'shop', 'driver', 'admin')),
    password_hash VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

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
(1, 2, 'مطعم الأوراس التقليدي والمشاوي', 'مطاعم ومشويات', 'وسط المدينة', 'شارع أول نوفمبر، قرب ساحة البلدية، سور الغزلان', 36.1485, 3.6905, '+213551111111', 200),
(2, 3, 'بيتزا وبرغر البرج العائلي', 'بيتزا وفاست فود', 'حي ذراع البرج', 'حي ذراع البرج، الطريق الرئيسي، سور الغزلان', 36.1550, 3.6840, '+213552222222', 200),
(3, NULL, 'فاست فود ومشاوي الوئام', 'سندويشات سريعة', 'حي الوئام', 'حي الوئام بجانب المسجد الجديد', 36.1520, 3.6960, '+213556666666', 200),
(4, NULL, 'حلويات ومخبزة باب الجزائر', 'حلويات ومخبوزات', 'حي باب الجزائر', 'قرب باب الجزائر التاريخي', 36.1495, 3.6880, '+213557777777', 200)
ON CONFLICT (id) DO NOTHING;

-- 3. Insert Products
INSERT INTO products (shop_id, name, description, price, category) VALUES
(1, 'شواء دجاج مشوي على الفحم (نصف دجاجة)', 'متبل مع خبز طازج وبطاطا مقلية وصلصة حارة وثومية', 750, 'مشويات'),
(1, 'سندويش كبدة مشوية دبل', 'كبدة عجل طازجة مع توابل جزائرية وسلطة وبطاطا', 400, 'سندويشات'),
(1, 'شربة فريك جزائرية بلحم العجل', 'شربة فريك تقليدية غنية مع الدبشة والنعناع', 250, 'أطباق تقليدية'),
(1, 'مشروب غازي كوكاكولا / حمود بوعلام 1 لتر', 'بارد ومنعش', 150, 'مشروبات'),
(2, 'بيتزا كاري كلاسيك فورماج ودبشة', 'صلصة طماطم محلية، جبن أحمر، زيتون جزائري ودبشة', 450, 'بيتزا'),
(2, 'بيتزا ميغا تشيز 4 أجبان', 'موزاريلا، غودا، جبن كاممبير وصلصة بيضاء', 800, 'بيتزا'),
(2, 'برغر لحم دبل ميكس تشيز', 'شريحتان لحم بقري محلي مع بطاطا وصلصة خاصة', 500, 'برغر'),
(2, 'تاكوس كوردون بلو فرماج لافاشكيري', 'تاكوس محشو باللحم المفروم والكوردون بلو مع صلصة الجبن', 600, 'سندويشات'),
(3, 'سندويش شوارما دجاج مقرمش خبز صاج', 'دجاج متبل مع صلصة جزائرية وبطاطا حارة', 350, 'سندويشات'),
(3, 'بانيني ميكس لحم وجبن', 'مضغوط على الجريل مع جبن ذائب', 380, 'سندويشات'),
(4, 'قلب اللوز الجزائري بالسمن والعسل (علبة 4 قطع)', 'قلب اللوز تقليدي محشي باللوز', 300, 'حلويات'),
(4, 'كرواسون وميلفاي طازج (علبة مشكلة)', 'مخبوزات الصباح الفرنسية الطازجة', 400, 'مخبوزات')
ON CONFLICT DO NOTHING;

-- 4. Insert Driver Initial Positions
INSERT INTO driver_locations (driver_id, driver_name, phone, vehicle_type, is_online, lat, lon, speed, heading, zone_id) VALUES
(4, 'أمين التوصيل (دراجة نارية SYM)', '+213553333333', 'دراجة نارية SYM 125', TRUE, 36.1482, 3.6912, 28.5, 45.0, 'sour_el_ghozlane'),
(5, 'كريم السريع (سكوتر فوري)', '+213554444444', 'سكوتر Peugeot Tweet', TRUE, 36.1465, 3.6890, 32.0, 180.0, 'sour_el_ghozlane')
ON CONFLICT (driver_id) DO NOTHING;
