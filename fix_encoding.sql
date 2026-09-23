SET client_encoding = 'UTF8';

UPDATE shops 
SET name = 'مطعم ومأكولات الوئام', 
    category = 'مطاعم وسندويتشات', 
    address_description = 'حي الوئام، طريق الجزائر، سور الغزلان' 
WHERE id = 1;

UPDATE products 
SET name = 'بيتزا كاري عائلية', 
    description = 'بيتزا بالجبن وصلصة الطماطم التقليدية' 
WHERE id = 1;

UPDATE products 
SET name = 'سندويتش شاورما دجاج', 
    description = 'شاورما مع البطاطا والصلصة الحارة' 
WHERE id = 2;

UPDATE users 
SET full_name = 'صاحب مطعم الوئام' 
WHERE id = 2;

UPDATE users 
SET full_name = 'أحمد السائق (دراجة نارية)' 
WHERE id = 3;

UPDATE users 
SET full_name = 'كريم الزبون' 
WHERE id = 4;
