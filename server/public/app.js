// Sour El Ghozlane Delivery Web/PWA Application
const SOUR_CENTER = [36.1480, 3.6900]; // Sour El Ghozlane Coordinates
const DELIVERY_FEE = 200; // Fixed 200 DA

// State
let cart = {}; // productId -> { product, qty }
let currentShop = null;
let selectedDriver = null;
let driversMap = null;
let liveTrackingMap = null;
let driverLiveMarker = null;
let customerLiveMarker = null;
let routePolyline = null;
let ws = null;
let trackingInterval = null;

// Dynamic Stores & Drivers Data (Populated from API or Merchants)
let SHOPS = [];
let DRIVERS = [];

// Neighborhood coordinates in Sour El Ghozlane
const NEIGHBORHOODS = {
  'وسط المدينة': [36.1485, 3.6905],
  'حي الوئام': [36.1520, 3.6960],
  'حي 114 مسكن': [36.1440, 3.6940],
  'حي ذراع البرج': [36.1550, 3.6840],
  'حي عين مريم': [36.1410, 3.6850],
  'حي باب الجزائر': [36.1495, 3.6880],
  'حي باب البوسعادة': [36.1450, 3.6910],
  'المنطقة الصناعية': [36.1380, 3.7020],
  'حي النصر': [36.1510, 3.7010]
};

// Fetch real shops and products from API
async function fetchShopsFromApi() {
  try {
    const res = await fetch('/api/shops');
    if (res.ok) {
      const data = await res.json();
      if (data.shops && data.shops.length > 0) {
        const fullShops = await Promise.all(data.shops.map(async (s) => {
          let products = [];
          try {
            const prodRes = await fetch(`/api/shops/${s.id}/products`);
            if (prodRes.ok) {
              const pData = await prodRes.json();
              products = (pData.products || []).map(p => ({
                id: p.id,
                name: p.name,
                desc: p.description || '',
                price: parseFloat(p.price_da) || 0
              }));
            }
          } catch (e) {}
          return {
            id: s.id,
            name: s.name,
            category: s.category || 'عام',
            neighborhood: s.address_description || 'سور الغزلان',
            address: s.address_description || 'سور الغزلان',
            lat: parseFloat(s.lat) || 36.148,
            lon: parseFloat(s.lng) || 3.690,
            deliveryTime: '20-30 دقيقة',
            rating: '5.0 ★',
            products
          };
        }));
        SHOPS = fullShops;
      }
    }
  } catch (err) {
    console.log('[API] Using local shops store');
  }
  renderShops();
}

// Fetch real online drivers from API
async function fetchDriversFromApi() {
  try {
    const res = await fetch('/api/drivers');
    if (res.ok) {
      const data = await res.json();
      if (data.drivers && data.drivers.length > 0) {
        DRIVERS = data.drivers.map(d => ({
          id: d.id,
          name: d.full_name || d.name || 'سائق معتمد',
          vehicle: d.vehicle_type || 'دراجة نارية',
          phone: d.phone,
          lat: parseFloat(d.lat) || 36.148,
          lon: parseFloat(d.lng) || 3.690,
          distanceKm: 1.0,
          etaMins: 5,
          rating: '5.0 ★'
        }));
      }
    }
  } catch (e) {}
}

// Initialize App
window.addEventListener('DOMContentLoaded', () => {
  initOrderHistoryStorage();
  fetchShopsFromApi();
  fetchDriversFromApi();
  initWebSocket();
});

// Customer Order History Local Storage Array
let customerOrderHistory = [];

function initOrderHistoryStorage() {
  const stored = localStorage.getItem('customer_past_orders_array');
  if (stored) {
    try {
      customerOrderHistory = JSON.parse(stored);
    } catch (e) {
      customerOrderHistory = [];
    }
  } else {
    customerOrderHistory = [];
    localStorage.setItem('customer_past_orders_array', JSON.stringify([]));
  }
  updateOrderHistoryBadge();
}

function getDefaultPastOrders() {
  return [];
}
  return [
    {
      id: 1092,
      orderNumber: '#SG-1092',
      shopName: 'مطعم الأوراس للشواء والوجبات',
      itemsSummary: '1x شواء نصف دجاجة على الفحم، 1x شربة فريك باللحم',
      totalPrice: 1200,
      deliveryFee: 200,
      deliveryDate: '23 سبتمبر 2026 - 20:15',
      status: 'تم التسليم بنجاح ✅',
      neighborhood: 'حي الوئام',
      driverName: 'أمين بوزيد'
    },
    {
      id: 1088,
      orderNumber: '#SG-1088',
      shopName: 'بيتزا وبرغر البرج العائلي',
      itemsSummary: '1x بيتزا سوبريم سور الغزلان عائلية، 1x بيبسي عائلي',
      totalPrice: 1400,
      deliveryFee: 200,
      deliveryDate: '22 سبتمبر 2026 - 19:30',
      status: 'تم التسليم بنجاح ✅',
      neighborhood: 'حي 114 مسكن',
      driverName: 'كريم منصوري'
    },
    {
      id: 1075,
      orderNumber: '#SG-1075',
      shopName: 'فاست فود ومشاوي الوئام',
      itemsSummary: '2x تاكوس جزائري مشكل، 1x كوكاكولا',
      totalPrice: 1000,
      deliveryFee: 200,
      deliveryDate: '20 سبتمبر 2026 - 14:10',
      status: 'تم التسليم بنجاح ✅',
      neighborhood: 'وسط المدينة',
      driverName: 'ياسين خليل'
    }
  ];
}

function updateOrderHistoryBadge() {
  const badge = document.getElementById('orderHistoryBadge');
  if (badge) {
    badge.innerText = `${customerOrderHistory.length} طلبات ➔`;
  }
}

function showOrderHistoryView() {
  document.getElementById('storeListView').classList.add('hidden');
  document.getElementById('storeDetailView').classList.add('hidden');
  document.getElementById('orderHistoryView').classList.remove('hidden');
  renderOrderHistoryList();
}

function renderOrderHistoryList() {
  const container = document.getElementById('orderHistoryList');
  if (!container) return;

  if (customerOrderHistory.length === 0) {
    container.innerHTML = '<div style="padding: 30px; text-align: center; color: #94a3b8;">لا توجد طلبات سابقة بعد</div>';
    return;
  }

  container.innerHTML = customerOrderHistory.map(o => `
    <div class="shop-card" style="cursor: default; padding: 16px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
        <div>
          <span style="font-size: 16px;">🏬</span>
          <strong style="font-size: 15px; margin-right: 6px;">${o.shopName}</strong>
          <span style="font-size: 11px; color: #94a3b8; margin-right: 6px;">${o.orderNumber}</span>
        </div>
        <span style="background: #064e3b; color: #34d399; font-size: 11px; padding: 3px 8px; border-radius: 6px; font-weight: bold;">
          ${o.status}
        </span>
      </div>

      <div style="display: flex; justify-content: space-between; font-size: 12px; color: #cbd5e1; margin-bottom: 6px;">
        <span>📅 تاريخ التوصيل: <strong>${o.deliveryDate}</strong></span>
        <span>📍 ${o.neighborhood}</span>
      </div>

      ${o.driverName ? `<p style="font-size: 11px; color: #94a3b8; margin-bottom: 6px;">🛵 سائق التوصيل: ${o.driverName}</p>` : ''}

      <div style="background: #0f172a; border-radius: 8px; padding: 10px; border: 1px solid #334155; margin-bottom: 10px;">
        <span style="font-size: 11px; color: #94a3b8; display: block;">الوجبات المطلوبة:</span>
        <strong style="font-size: 13px; color: #f8fafc;">${o.itemsSummary}</strong>
      </div>

      <div style="display: flex; justify-content: space-between; align-items: center; border-top: 1px solid #334155; padding-top: 10px;">
        <div>
          <span style="font-size: 11px; color: #94a3b8; display: block;">المبلغ الإجمالي (مع 200 دج توصيل):</span>
          <strong style="font-size: 17px; color: #fb923c;">${o.totalPrice} دج</strong>
        </div>
        <button class="btn btn-primary" onclick="reorderShopByName('${o.shopName}')" style="padding: 6px 14px; font-size: 12px;">
          طلب جديد 🔁
        </button>
      </div>
    </div>
  `).join('');
}

function reorderShopByName(shopName) {
  const shop = SHOPS.find(s => s.name.trim() === shopName.trim()) || SHOPS[0];
  showStoreList();
  selectShop(shop.id);
}

// Render Shop Cards
function renderShops(filteredList = SHOPS) {
  const container = document.getElementById('shopsGrid');
  if (filteredList.length === 0) {
    container.innerHTML = '<div style="padding: 40px 20px; text-align: center; color: #94a3b8; grid-column: 1/-1;"><span style="font-size: 32px; display: block; margin-bottom: 10px;">🏬</span><strong style="font-size: 16px; color: #f8fafc;">لا توجد متاجر أو مطاعم مضافة حالياً</strong><p style="font-size: 12px; margin-top: 6px; color: #94a3b8;">يمكن لأصحاب المتاجر والمطاعم تسجيل متاجرهم وإضافة منتجاتهم من قائمة الأدوار في الأعلى.</p></div>';
    return;
  }
  container.innerHTML = filteredList.map(s => `
    <div class="shop-card" onclick="selectShop(${s.id})">
      <span class="shop-category">${s.category}</span>
      <h4 class="shop-title">${s.name}</h4>
      <p class="shop-neighborhood">📍 ${s.neighborhood} - ${s.address}</p>
      <div class="shop-footer">
        <span>⏱ ${s.deliveryTime}</span>
        <span>${s.rating}</span>
        <strong style="color: #fb923c;">توصيل 200 دج</strong>
      </div>
    </div>
  `).join('');
}

let activeCategory = 'all';
let currentSearchQuery = '';

const CATEGORY_KEYWORDS = {
  pizza: ['بيتزا', 'pizza'],
  bbq: ['شواء', 'مشاوي', 'مشويات', 'دجاج'],
  fast_food: ['سريعة', 'برغر', 'burger', 'فاست'],
  sandwiches: ['سندويش', 'تاكوس', 'شاورما', 'كبدة', 'بانيني'],
  sweets: ['حلويات', 'مخبوزات', 'قلب اللوز', 'كرواسون', 'ميلفاي']
};

function selectCategory(catId) {
  activeCategory = catId;

  // Update pills UI
  document.querySelectorAll('.cat-pill').forEach(btn => btn.classList.remove('active'));
  const activeBtn = Array.from(document.querySelectorAll('.cat-pill')).find(btn =>
    btn.getAttribute('onclick')?.includes(`'${catId}'`)
  );
  if (activeBtn) activeBtn.classList.add('active');

  const resetBtn = document.getElementById('resetCategoryBtn');
  if (resetBtn) resetBtn.style.display = catId === 'all' ? 'none' : 'inline';

  applyFilters();
}

function handleSearchInput(query) {
  currentSearchQuery = query;
  applyFilters();
}

function applyFilters() {
  const q = currentSearchQuery.trim().toLowerCase();
  const mealsContainer = document.getElementById('mealsSearchResults');

  // Filter matching shops by category & search
  const filteredShops = SHOPS.filter(shop => {
    let matchesCat = true;
    if (activeCategory !== 'all') {
      const keywords = CATEGORY_KEYWORDS[activeCategory] || [];
      const shopMatches = keywords.some(kw =>
        shop.category.toLowerCase().includes(kw) ||
        shop.name.toLowerCase().includes(kw)
      );
      const productMatches = shop.products.some(p =>
        keywords.some(kw => p.name.toLowerCase().includes(kw) || p.desc.toLowerCase().includes(kw))
      );
      matchesCat = shopMatches || productMatches;
    }

    const matchesQuery = !q ||
      shop.name.toLowerCase().includes(q) ||
      shop.category.toLowerCase().includes(q) ||
      shop.neighborhood.toLowerCase().includes(q);

    return matchesCat && matchesQuery;
  });

  // Filter matching meals
  const matchingMeals = [];
  if (q || activeCategory !== 'all') {
    SHOPS.forEach(shop => {
      shop.products.forEach(prod => {
        let matchesCat = true;
        if (activeCategory !== 'all') {
          const keywords = CATEGORY_KEYWORDS[activeCategory] || [];
          matchesCat = keywords.some(kw => prod.name.toLowerCase().includes(kw) || prod.desc.toLowerCase().includes(kw));
        }

        const matchesQuery = !q || prod.name.toLowerCase().includes(q) || prod.desc.toLowerCase().includes(q);

        if (matchesCat && matchesQuery) {
          matchingMeals.push({ ...prod, shopId: shop.id, shopName: shop.name });
        }
      });
    });
  }

  if (matchingMeals.length > 0 && (q || activeCategory !== 'all')) {
    mealsContainer.classList.remove('hidden');
    mealsContainer.innerHTML = `
      <div style="grid-column: 1/-1; margin-bottom: 6px;">
        <strong style="color: #ea580c;">الوجبات المطابقة (${matchingMeals.length}):</strong>
      </div>
      ` + matchingMeals.map(m => `
      <div class="product-card" onclick="selectShop(${m.shopId})" style="cursor: pointer;">
        <div>
          <h4 class="product-title">${m.name}</h4>
          <span style="font-size: 11px; color: #94a3b8;">متوفر في: ${m.shopName}</span>
          <p class="product-desc" style="margin-top: 4px;">${m.desc}</p>
        </div>
        <div class="product-action-row">
          <span class="product-price">${m.price} دج</span>
          <button class="btn btn-primary" style="padding: 4px 10px; font-size: 12px;">طلب الوجبة ➔</button>
        </div>
      </div>
    `).join('');
  } else {
    mealsContainer.classList.add('hidden');
    mealsContainer.innerHTML = '';
  }

  const titleEl = document.getElementById('shopsSectionTitle');
  if (titleEl) {
    if (activeCategory !== 'all' || q) {
      titleEl.innerText = `المطاعم المطابقة للتصفية (${filteredShops.length}):`;
    } else {
      titleEl.innerText = 'المتاجر والمطاعم المتاحة الآن في سور الغزلان';
    }
  }

  renderShops(filteredShops);
}

// Select Shop
function selectShop(shopId) {
  currentShop = SHOPS.find(s => s.id === shopId);
  document.getElementById('storeListView').classList.add('hidden');
  document.getElementById('storeDetailView').classList.remove('hidden');

  document.getElementById('storeHeaderCard').innerHTML = `
    <h2>${currentShop.name}</h2>
    <p>📍 ${currentShop.address} | 📞 ${currentShop.phone}</p>
    <div style="margin-top: 6px;">
      <span class="badge" style="background:#ea580c;color:white;padding:3px 8px;border-radius:4px;">سعر التوصيل ثابت: 200 دج</span>
    </div>
  `;

  renderProducts();
}

function showStoreList() {
  document.getElementById('storeDetailView').classList.add('hidden');
  document.getElementById('storeListView').classList.remove('hidden');
}

// Render Products
function renderProducts() {
  const container = document.getElementById('productsGrid');
  container.innerHTML = currentShop.products.map(p => {
    const qty = cart[p.id]?.qty || 0;
    return `
      <div class="product-card">
        <div>
          <h4 class="product-title">${p.name}</h4>
          <p class="product-desc">${p.desc}</p>
        </div>
        <div class="product-action-row">
          <span class="product-price">${p.price} دج</span>
          <div class="qty-control">
            <button class="qty-btn" onclick="updateQty(${p.id}, -1)">-</button>
            <span class="qty-val" id="qty-${p.id}">${qty}</span>
            <button class="qty-btn" onclick="updateQty(${p.id}, 1)">+</button>
          </div>
        </div>
      </div>
    `;
  }).join('');
}

function updateQty(productId, delta) {
  const product = currentShop.products.find(p => p.id === productId);
  if (!product) return;

  if (!cart[productId]) {
    cart[productId] = { product, qty: 0 };
  }

  cart[productId].qty += delta;
  if (cart[productId].qty <= 0) {
    delete cart[productId];
  }

  const el = document.getElementById(`qty-${productId}`);
  if (el) el.innerText = cart[productId]?.qty || 0;

  updateFloatingCart();
}

function updateFloatingCart() {
  let count = 0;
  let total = 0;
  for (const id in cart) {
    count += cart[id].qty;
    total += cart[id].qty * cart[id].product.price;
  }

  const floating = document.getElementById('floatingCart');
  if (count > 0) {
    floating.classList.remove('hidden');
    document.getElementById('cartCountBadge').innerText = count;
    document.getElementById('cartTotalSum').innerText = total;
  } else {
    floating.classList.add('hidden');
  }
}

// DRIVER SELECTION MODAL
function openDriverSelectionModal() {
  document.getElementById('driverModal').classList.remove('hidden');

  let subtotal = 0;
  for (const id in cart) {
    subtotal += cart[id].qty * cart[id].product.price;
  }
  document.getElementById('summarySubtotal').innerText = `${subtotal} دج`;
  document.getElementById('summaryTotal').innerText = `${subtotal + DELIVERY_FEE} دج`;

  // Render Drivers List
  selectedDriver = DRIVERS[0];
  const listContainer = document.getElementById('driversListContainer');
  listContainer.innerHTML = DRIVERS.map((d, index) => `
    <div class="driver-option-item ${index === 0 ? 'selected' : ''}" id="driver-opt-${d.id}" onclick="selectDriverOption(${d.id})">
      <span style="font-size: 24px;">🛵</span>
      <div style="flex: 1;">
        <strong>${d.name}</strong>
        <div style="font-size: 11px; color: #94a3b8;">${d.vehicle} • يبعد ${d.distanceKm} كم (${d.etaMins} دقائق)</div>
      </div>
      <span style="color: #fb923c; font-weight: bold;">${d.rating}</span>
    </div>
  `).join('');

  // Init OpenStreetMap for Drivers
  setTimeout(initDriversMap, 200);
}

function selectDriverOption(driverId) {
  selectedDriver = DRIVERS.find(d => d.id === driverId);
  document.querySelectorAll('.driver-option-item').forEach(el => el.classList.remove('selected'));
  const target = document.getElementById(`driver-opt-${driverId}`);
  if (target) target.classList.add('selected');
}

function closeDriverSelectionModal() {
  document.getElementById('driverModal').classList.add('hidden');
}

function initDriversMap() {
  if (driversMap) {
    driversMap.invalidateSize();
    return;
  }

  driversMap = L.map('driversMap').setView(SOUR_CENTER, 14);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap'
  }).addTo(driversMap);

  // Markers for online drivers
  DRIVERS.forEach(d => {
    const icon = L.divIcon({
      className: 'driver-scooter-marker',
      html: '<div style="background:#ea580c;color:white;border-radius:50%;width:32px;height:32px;display:flex;align-items:center;justify-content:center;font-size:16px;border:2px solid white;box-shadow:0 2px 6px rgba(0,0,0,0.4);">🛵</div>',
      iconSize: [32, 32]
    });
    L.marker([d.lat, d.lon], { icon }).addTo(driversMap)
      .bindPopup(`<b>${d.name}</b><br>${d.vehicle}`);
  });
}

// CONFIRM ORDER & OPEN TRACKING
function confirmOrder() {
  closeDriverSelectionModal();

  const custName = document.getElementById('custNameInput').value;
  const custNeighborhood = document.getElementById('custNeighborhoodSelect').value;
  const custAddress = document.getElementById('custAddressInput').value;
  const custCoords = NEIGHBORHOODS[custNeighborhood] || SOUR_CENTER;

  let subtotal = 0;
  for (const id in cart) {
    subtotal += cart[id].qty * cart[id].product.price;
  }
  const total = subtotal + DELIVERY_FEE;
  const itemsSummary = Object.values(cart).map(c => `${c.qty}x ${c.product.name}`).join('، ');
  const orderNum = `SOUR-${Math.floor(1000 + Math.random() * 9000)}`;

  // Save to local storage array
  const now = new Date();
  const dateStr = now.toLocaleDateString('ar-DZ', { day: 'numeric', month: 'long', year: 'numeric' }) + ' - ' + now.toLocaleTimeString('ar-DZ', { hour: '2-digit', minute: '2-digit' });

  customerOrderHistory.unshift({
    id: Date.now(),
    orderNumber: orderNum,
    shopName: currentShop.name,
    itemsSummary: itemsSummary,
    totalPrice: total,
    deliveryFee: DELIVERY_FEE,
    deliveryDate: dateStr,
    status: 'قيد التوصيل 🛵',
    neighborhood: custNeighborhood,
    driverName: selectedDriver ? selectedDriver.name : null
  });
  localStorage.setItem('customer_past_orders_array', JSON.stringify(customerOrderHistory));
  updateOrderHistoryBadge();

  // Open Live Tracking Modal
  document.getElementById('trackingModal').classList.remove('hidden');
  document.getElementById('trackOrderNum').innerText = orderNum;
  document.getElementById('trackDriverName').innerText = selectedDriver.name;
  document.getElementById('trackDriverPhone').innerText = `هاتف: ${selectedDriver.phone}`;
  document.getElementById('trackCallBtn').href = `tel:${selectedDriver.phone}`;
  document.getElementById('trackTotalCod').innerText = `${total} دج`;

  setTimeout(() => initLiveTrackingMap(custCoords, currentShop), 200);

  // Clear cart
  cart = {};
  updateFloatingCart();
  renderProducts();
}

function closeTrackingModal() {
  document.getElementById('trackingModal').classList.add('hidden');
  if (trackingInterval) clearInterval(trackingInterval);
}

// LIVE TRACKING MAP WITH STEPPER & MOVING DRIVER
function initLiveTrackingMap(customerCoords, shop) {
  if (liveTrackingMap) {
    liveTrackingMap.remove();
  }

  liveTrackingMap = L.map('liveTrackingMap').setView(SOUR_CENTER, 14);
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap'
  }).addTo(liveTrackingMap);

  // 1. Customer Marker 🏠
  const custIcon = L.divIcon({
    className: 'cust-marker',
    html: '<div style="background:#22c55e;color:white;border-radius:50%;width:34px;height:34px;display:flex;align-items:center;justify-content:center;font-size:18px;border:2px solid white;">🏠</div>',
    iconSize: [34, 34]
  });
  customerLiveMarker = L.marker(customerCoords, { icon: custIcon }).addTo(liveTrackingMap)
    .bindPopup('<b>موقع الزبون (منزلك)</b>');

  // 2. Shop Marker 🏪
  const shopIcon = L.divIcon({
    className: 'shop-marker',
    html: '<div style="background:#3b82f6;color:white;border-radius:50%;width:34px;height:34px;display:flex;align-items:center;justify-content:center;font-size:18px;border:2px solid white;">🏪</div>',
    iconSize: [34, 34]
  });
  const shopCoords = [shop.lat, shop.lon];
  L.marker(shopCoords, { icon: shopIcon }).addTo(liveTrackingMap)
    .bindPopup(`<b>${shop.name}</b>`);

  // 3. Driver Marker 🛵
  let driverLat = selectedDriver.lat;
  let driverLon = selectedDriver.lon;
  const driverIcon = L.divIcon({
    className: 'driver-live-marker',
    html: '<div style="background:#ea580c;color:white;border-radius:50%;width:38px;height:38px;display:flex;align-items:center;justify-content:center;font-size:20px;border:3px solid white;box-shadow:0 0 12px rgba(234,88,12,0.8);">🛵</div>',
    iconSize: [38, 38]
  });
  driverLiveMarker = L.marker([driverLat, driverLon], { icon: driverIcon }).addTo(liveTrackingMap)
    .bindPopup(`<b>${selectedDriver.name}</b><br>قيد التوصيل الآن`);

  // Route Polyline
  routePolyline = L.polyline([[driverLat, driverLon], shopCoords, customerCoords], {
    color: '#ea580c',
    weight: 4,
    dashArray: '8, 8'
  }).addTo(liveTrackingMap);

  liveTrackingMap.fitBounds([customerCoords, shopCoords, [driverLat, driverLon]], { padding: [30, 30] });

  // Simulate Live Movement along Sour El Ghozlane streets
  startDriverMovementSimulation(shopCoords, customerCoords);
}

function startDriverMovementSimulation(shopCoords, customerCoords) {
  if (trackingInterval) clearInterval(trackingInterval);

  let step = 0;
  const totalSteps = 100;
  // Step 1: NEW -> Step 2: PREPARING (at step 10) -> Step 3: ON_THE_WAY (at step 30) -> Step 4: DELIVERED (at step 100)

  updateStepper(1);

  trackingInterval = setInterval(() => {
    step++;

    if (step === 10) {
      updateStepper(2); // Shop preparing
    } else if (step === 30) {
      updateStepper(3); // Driver on the way
    } else if (step >= totalSteps) {
      updateStepper(4); // Delivered!
      clearInterval(trackingInterval);
      return;
    }

    // Interpolate driver position towards customer
    const t = step / totalSteps;
    const currentLat = selectedDriver.lat + (customerCoords[0] - selectedDriver.lat) * t;
    const currentLon = selectedDriver.lon + (customerCoords[1] - selectedDriver.lon) * t;

    if (driverLiveMarker) {
      driverLiveMarker.setLatLng([currentLat, currentLon]);
    }

    const remainingMins = Math.max(1, Math.round((1 - t) * 10));
    const etaEl = document.getElementById('trackEta');
    if (etaEl) etaEl.innerText = `${remainingMins} دقائق`;

  }, 1000);
}

function updateStepper(stepNum) {
  for (let i = 1; i <= 4; i++) {
    const item = document.getElementById(`step${i}`);
    if (item) {
      if (i <= stepNum) item.classList.add('active');
      else item.classList.remove('active');
    }
  }
  for (let i = 1; i <= 3; i++) {
    const line = document.getElementById(`line${i}`);
    if (line) {
      if (i < stepNum) line.classList.add('active');
      else line.classList.remove('active');
    }
  }
}

// ROLE SWITCHER
function switchRole(role) {
  document.querySelectorAll('.view-section').forEach(s => s.classList.add('hidden'));
  document.getElementById(`${role}View`).classList.remove('hidden');

  if (role === 'shop') renderShopDashboard();
  if (role === 'driver') renderDriverDashboard();
  if (role === 'admin') renderAdminDashboard();
}

function renderShopDashboard() {
  document.getElementById('shopOrdersList').innerHTML = `
    <div class="product-card" style="margin-bottom:12px;">
      <div style="display:flex;justify-content:space-between;align-items:center;">
        <strong>طلب جديد: SOUR-4512</strong>
        <span class="badge" style="background:#fbbf24;color:black;">جديد • تحضير</span>
      </div>
      <p style="font-size:12px;color:#94a3b8;margin:6px 0;">الزبون: أمين (حي الوئام) | 📞 0550112233</p>
      <div style="font-size:13px;margin-bottom:10px;">
        • 2x شواء نصف دجاجة على الفحم (1,500 دج)<br>
        • 1x مشروب حمود بوعلام (150 دج)
      </div>
      <div style="display:flex;gap:8px;">
        <button class="btn btn-primary" onclick="alert('تم قبول الطلب وجاري تحضيره في المطبخ')">قبول الطلب وبدء التحضير</button>
        <button class="btn btn-secondary" onclick="alert('تم تجهيز الوجبة وبانتظار استلام السائق')">جاهز للتسليم للسائق</button>
      </div>
    </div>
  `;
}

function renderDriverDashboard() {
  document.getElementById('driverOrdersList').innerHTML = `
    <div class="product-card">
      <div style="display:flex;justify-content:space-between;">
        <strong>طلب جاهز للاستلام: SOUR-8921</strong>
        <span style="color:#fb923c;font-weight:bold;">عمولة التوصيل: 200 دج</span>
      </div>
      <p style="font-size:12px;color:#94a3b8;margin:6px 0;">من: مطعم الأوراس (وسط المدينة) ➔ إلى: حي 114 مسكن</p>
      <div style="display:flex;gap:8px;margin-top:10px;">
        <button class="btn btn-primary" onclick="alert('تم استلام الطلب وأنت الآن في الطريق للزبون')">استلام والتحرك للزبون 🛵</button>
        <button class="btn btn-secondary" onclick="alert('تم تسليم الطلب واستلام المبلغ نقداً!')">تم التسليم واستلام المبلغ ✅</button>
      </div>
    </div>
  `;
}

function renderAdminDashboard() {
  document.getElementById('adminOrdersList').innerHTML = `
    <div class="product-card" style="font-size:13px;">
      <div style="display:flex;justify-content:space-between;">
        <span>#SOUR-8921</span>
        <span>مطعم الأوراس</span>
        <span>السائق: أمين (SYM 125)</span>
        <span style="color:#22c55e;">قيد التوصيل 🛵</span>
        <strong>1,150 دج (COD)</strong>
      </div>
    </div>
  `;
}

function toggleDriverOnline(isOnline) {
  console.log('Driver status toggled:', isOnline);
}

function simulateDriverMovement() {
  alert('تم بث إحداثيات GPS جديدة عبر WebSocket بنجاح!');
}

function initWebSocket() {
  try {
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    ws = new WebSocket(`${protocol}//${window.location.host}/ws`);
    ws.onmessage = (event) => {
      const msg = JSON.parse(event.data);
      console.log('[WebSocket Message Received]:', msg);
    };
  } catch (err) {
    console.log('WebSocket connection error (mock mode active):', err);
  }
}
