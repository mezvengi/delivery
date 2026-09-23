// Automated full delivery lifecycle verification test
const API_URL = 'http://localhost:3000/api';

async function request(endpoint, options = {}) {
  const url = `${API_URL}${endpoint}`;
  const res = await fetch(url, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    }
  });
  const data = await res.json();
  if (!res.ok) {
    throw new Error(`HTTP ${res.status} on ${endpoint}: ${JSON.stringify(data)}`);
  }
  return data;
}

async function runCycle() {
  console.log('--- بدء اختبار الدورة الكاملة لتطبيق توصيل سور الغزلان ---');

  // 1. تسجيل دخول الإدارة
  console.log('1. اختبار تسجيل دخول الإدارة...');
  const adminLogin = await request('/auth/login-password', {
    method: 'POST',
    body: JSON.stringify({
      phone: '+213555000000',
      password: process.env.ADMIN_PASSWORD || 'AdminSour2026!'
    })
  });
  console.log('   ✅ نجح تسجيل دخول الإدارة. Token ID:', adminLogin.user.id);
  const adminToken = adminLogin.token;

  // 2. تسجيل متجر جديد
  console.log('2. تسجيل حساب متجر (مطعم الوئام)...');
  const shopOtp = await request('/auth/send-otp', {
    method: 'POST',
    body: JSON.stringify({ phone: '0551111111' })
  });
  const shopAuth = await request('/auth/verify-otp', {
    method: 'POST',
    body: JSON.stringify({
      phone: '0551111111',
      code: shopOtp.mock_code,
      full_name: 'صاحب مطعم الوئام',
      role: 'SHOP'
    })
  });
  console.log('   ✅ تم التحقق وتوثيق المتجر:', shopAuth.user.phone);

  const shopCreate = await request('/shops', {
    method: 'POST',
    headers: { Authorization: `Bearer ${shopAuth.token}` },
    body: JSON.stringify({
      name: 'مطعم ومأكولات الوئام',
      category: 'مطاعم وسندويتشات',
      address_description: 'حي الوئام، طريق الجزائر، سور الغزلان',
      lat: 36.1492,
      lng: 3.6915,
      zone_id: 'sour_el_ghozlane'
    })
  });
  const shopId = shopCreate.shop.id;
  console.log(`   ✅ تم إنشاء المتجر (ID: ${shopId}): ${shopCreate.shop.name}`);

  // 3. إضافة منتجات للمتجر
  console.log('3. إضافة منتجات للمتجر...');
  const p1 = await request(`/shops/${shopId}/products`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${shopAuth.token}` },
    body: JSON.stringify({
      name: 'بيتزا كاري عائلية',
      description: 'بيتزا بالجبن وصلصة الطماطم التقليدية',
      price_da: 800
    })
  });
  const p2 = await request(`/shops/${shopId}/products`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${shopAuth.token}` },
    body: JSON.stringify({
      name: 'سندويتش شاورما دجاج',
      description: 'شاورما مع البطاطا والصلصة الحارة',
      price_da: 350
    })
  });
  console.log(`   ✅ تمت إضافة منتجين: ${p1.product.name} (${p1.product.price_da} دج) و ${p2.product.name} (${p2.product.price_da} دج)`);

  // 4. تسجيل سائق
  console.log('4. تسجيل سائق توصيل جديد...');
  const driverOtp = await request('/auth/send-otp', {
    method: 'POST',
    body: JSON.stringify({ phone: '0552222222' })
  });
  const driverAuth = await request('/auth/verify-otp', {
    method: 'POST',
    body: JSON.stringify({
      phone: '0552222222',
      code: driverOtp.mock_code,
      full_name: 'أحمد السائق (دراجة نارية)',
      role: 'DRIVER'
    })
  });
  const driverId = driverAuth.user.id;
  console.log(`   ✅ تم تسجيل السائق: ${driverAuth.user.full_name} (ID: ${driverId})`);

  // السائق يرسل موقعه المباشر داخل سور الغزلان
  await request('/drivers/location', {
    method: 'POST',
    headers: { Authorization: `Bearer ${driverAuth.token}` },
    body: JSON.stringify({
      lat: 36.1478,
      lng: 3.6895,
      heading: 90
    })
  });
  console.log('   ✅ تم إرسال إحداثيات السائق الحالية بنجاح عبر GPS');

  // 5. تسجيل زبون وإنشاء طلب
  console.log('5. تسجيل زبون وإجراء طلب شراء...');
  const custOtp = await request('/auth/send-otp', {
    method: 'POST',
    body: JSON.stringify({ phone: '0553333333' })
  });
  const custAuth = await request('/auth/verify-otp', {
    method: 'POST',
    body: JSON.stringify({
      phone: '0553333333',
      code: custOtp.mock_code,
      full_name: 'كريم الزبون',
      role: 'CUSTOMER'
    })
  });

  const orderCreate = await request('/orders', {
    method: 'POST',
    headers: { Authorization: `Bearer ${custAuth.token}` },
    body: JSON.stringify({
      shop_id: shopId,
      items: [
        { product_id: p1.product.id, quantity: 1 },
        { product_id: p2.product.id, quantity: 2 }
      ],
      delivery_neighborhood: 'حي 114 مسكن',
      delivery_description: 'قرب المسجد، عمارة ب، الطابق الثاني',
      delivery_lat: 36.151,
      delivery_lng: 3.693,
      notes: 'الرجاء الاتصال قبل الصعود'
    })
  });
  const orderId = orderCreate.order.id;
  console.log(`   ✅ تم إنشاء الطلب رقم #${orderId}`);
  console.log(`      - مجموع المنتجات: ${orderCreate.order.items_total_da} دج`);
  console.log(`      - سعر التوصيل الثابت: ${orderCreate.order.delivery_fee_da} دج`);
  console.log(`      - الإجمالي للدفع عند الاستلام (COD): ${orderCreate.order.total_amount_da} دج`);

  // 6. الإدارة ترى الطلبات وتعين السائق
  console.log('6. الإدارة تقوم بتعيين السائق للطلب...');
  const assignRes = await request(`/orders/${orderId}/assign-driver`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${adminToken}` },
    body: JSON.stringify({ driver_id: driverId })
  });
  console.log(`   ✅ تم تعيين السائق ${driverId} للطلب #${orderId}. الحالة الحالية: ${assignRes.order.status}`);

  // 7. السائق يستلم الطلب وينطلق (قيد التوصيل)
  console.log('7. السائق ينطلق لتوصيل الطلب (OUT_FOR_DELIVERY)...');
  const deliveringRes = await request(`/orders/${orderId}/status`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${driverAuth.token}` },
    body: JSON.stringify({ status: 'OUT_FOR_DELIVERY' })
  });
  console.log(`   ✅ تم تحديث الحالة إلى: ${deliveringRes.order.status}`);

  // 8. السائق يسلّم الطلب ويستلم المبلغ نقدياً (DELIVERED)
  console.log('8. السائق يسلّم الطلب للزبون ويستلم الدفع (DELIVERED)...');
  const deliveredRes = await request(`/orders/${orderId}/status`, {
    method: 'PATCH',
    headers: { Authorization: `Bearer ${driverAuth.token}` },
    body: JSON.stringify({ status: 'DELIVERED' })
  });
  console.log(`   ✅ تم إنهاء الطلب بنجاح: ${deliveredRes.order.status}`);

  // 9. الزبون يتحقق من قائمة طلباته
  console.log('9. التحقق النهائي من جانب الزبون...');
  const myOrders = await request('/orders', {
    headers: { Authorization: `Bearer ${custAuth.token}` }
  });
  console.log(`   ✅ الزبون يرى طلبه #${myOrders.orders[0].id} بالحالة: ${myOrders.orders[0].status}`);

  console.log('\n🎉 اكتمل اختبار الدورة الكاملة بنجاح 100% دون أي أخطاء!');
}

runCycle().catch(err => {
  console.error('\n❌ فشل الاختبار:', err.message);
  process.exit(1);
});
