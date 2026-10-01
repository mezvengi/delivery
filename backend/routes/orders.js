const express = require('express');
const router = express.Router();
const db = require('../db');
const { authMiddleware } = require('../auth');
const { broadcastOrderStatus } = require('../websocket');

// Generate unique order number (e.g., SOUR-8942)
function generateOrderNumber() {
  const rand = Math.floor(1000 + Math.random() * 9000);
  return `SOUR-${rand}`;
}

// 1. Create Order (Customer)
router.post('/', async (req, res) => {
  const {
    customer_id,
    customer_name,
    customer_phone,
    shop_id,
    driver_id,
    neighborhood,
    address_description,
    customer_lat,
    customer_lon,
    items,
    notes,
  } = req.body;

  if (!customer_name || !customer_phone || !shop_id || !neighborhood || !items || items.length === 0) {
    return res.status(400).json({ error: 'يرجى استكمال بيانات الطلب والعنوان وقائمة الوجبات' });
  }

  // Calculate items subtotal
  let subtotal = 0;
  for (const item of items) {
    subtotal += item.unit_price * item.quantity;
  }
  const delivery_fee = 200; // Fixed 200 DA in Sour El Ghozlane
  const total = subtotal + delivery_fee;
  const orderNumber = generateOrderNumber();

  try {
    const client = await db.pool.connect();
    try {
      await client.query('BEGIN');

      const orderInsertQuery = `
        INSERT INTO orders (
          order_number, customer_id, customer_name, customer_phone, shop_id,
          driver_id, neighborhood, address_description, customer_lat, customer_lon,
          status, subtotal, delivery_fee, total, payment_method, notes
        ) VALUES (
          $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, 'NEW', $11, $12, $13, 'COD', $14
        ) RETURNING *`;

      const orderResult = await client.query(orderInsertQuery, [
        orderNumber,
        customer_id || null,
        customer_name,
        customer_phone,
        shop_id,
        driver_id || null,
        neighborhood,
        address_description || '',
        customer_lat || 36.1480,
        customer_lon || 3.6900,
        subtotal,
        delivery_fee,
        total,
        notes || '',
      ]);

      const createdOrder = orderResult.rows[0];

      // Insert Items
      for (const item of items) {
        await client.query(
          `INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, total_price)
           VALUES ($1, $2, $3, $4, $5, $6)`,
          [
            createdOrder.id,
            item.product_id || null,
            item.product_name,
            item.quantity,
            item.unit_price,
            item.unit_price * item.quantity,
          ]
        );
      }

      await client.query('COMMIT');

      // Notify through WebSocket
      broadcastOrderStatus(createdOrder.id, 'NEW', {
        orderNumber: createdOrder.order_number,
        shopId: createdOrder.shop_id,
        driverId: createdOrder.driver_id,
        total: createdOrder.total,
      });

      res.status(201).json(createdOrder);
    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    } finally {
      client.release();
    }
  } catch (err) {
    console.error('Create order error:', err);
    res.status(500).json({ error: 'تعذر حفظ الطلب في النظام' });
  }
});

// 2. Get Order Details (for Tracking Screen)
router.get('/:id', async (req, res) => {
  const { id } = req.params;
  try {
    const orderQuery = `
      SELECT o.*,
             s.name AS shop_name, s.phone AS shop_phone, s.lat AS shop_lat, s.lon AS shop_lon, s.neighborhood AS shop_neighborhood,
             u.name AS driver_name, u.phone AS driver_phone,
             dl.lat AS driver_lat, dl.lon AS driver_lon, dl.speed AS driver_speed, dl.vehicle_type AS driver_vehicle
      FROM orders o
      JOIN shops s ON s.id = o.shop_id
      LEFT JOIN users u ON u.id = o.driver_id
      LEFT JOIN driver_locations dl ON dl.driver_id = o.driver_id
      WHERE o.id = $1 OR o.order_number = $1`;

    const orderResult = await db.query(orderQuery, [id]);
    if (orderResult.rows.length === 0) {
      return res.status(404).json({ error: 'الطلب غير موجود' });
    }

    const order = orderResult.rows[0];

    const itemsResult = await db.query(
      `SELECT * FROM order_items WHERE order_id = $1`,
      [order.id]
    );

    res.json({
      order,
      items: itemsResult.rows,
    });
  } catch (err) {
    console.error('Fetch order detail error:', err);
    res.status(500).json({ error: 'تعذر جلب تفاصيل الطلب' });
  }
});

// 3. Update Order Status
router.patch('/:id/status', async (req, res) => {
  const { id } = req.params;
  const { status, driver_id } = req.body;

  const validStatuses = ['NEW', 'SHOP_ACCEPTED', 'PREPARING', 'READY_FOR_PICKUP', 'ON_THE_WAY', 'DELIVERED', 'CANCELLED'];
  if (!validStatuses.includes(status)) {
    return res.status(400).json({ error: 'حالة الطلب غير صالحة' });
  }

  try {
    const result = await db.query(
      `UPDATE orders
       SET status = $1,
           driver_id = COALESCE($2, driver_id),
           updated_at = NOW()
       WHERE id = $3
       RETURNING *`,
      [status, driver_id || null, id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'الطلب غير موجود' });
    }

    const updated = result.rows[0];
    broadcastOrderStatus(updated.id, updated.status, {
      driverId: updated.driver_id,
      updatedAt: updated.updated_at,
    });

    res.json(updated);
  } catch (err) {
    console.error('Update order status error:', err);
    res.status(500).json({ error: 'تعذر تحديث حالة الطلب' });
  }
});

// Calculate distance in km
function calculateDistanceKm(lat1, lon1, lat2, lon2) {
  const R = 6371;
  const dLat = (lat2 - lat1) * (Math.PI / 180);
  const dLon = (lon2 - lon1) * (Math.PI / 180);
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(lat1 * (Math.PI / 180)) * Math.cos(lat2 * (Math.PI / 180)) *
    Math.sin(dLon / 2) * Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return Math.round(R * c * 10) / 10;
}

// 3.1 Dynamic Distance & Cost Calculation (Deliverio Feature)
router.post('/calculate-fee', async (req, res) => {
  const { shopLat, shopLon, customerLat, customerLon } = req.body;
  const sLat = parseFloat(shopLat) || 36.1485;
  const sLon = parseFloat(shopLon) || 3.6905;
  const cLat = parseFloat(customerLat) || 36.1480;
  const cLon = parseFloat(customerLon) || 3.6900;

  const distanceKm = calculateDistanceKm(sLat, sLon, cLat, cLon);
  const baseFee = 150;
  const perKmRate = 30;
  const dynamicFee = Math.max(200, Math.round(baseFee + (distanceKm * perKmRate)));
  const estimatedDurationMins = Math.max(5, Math.round(distanceKm * 4) + 10);

  res.json({
    success: true,
    distance_km: distanceKm,
    delivery_fee: dynamicFee,
    estimated_duration_min: estimatedDurationMins,
    currency: 'DZD'
  });
});

// 3.2 Shop Manual Phone-in Order Dispatch (Deliverio Feature)
router.post('/shop-manual', async (req, res) => {
  const {
    shop_id,
    customer_name,
    customer_phone,
    neighborhood,
    address_description,
    items_description,
    total_amount,
    notes
  } = req.body;

  if (!shop_id || !customer_name || !customer_phone || !address_description || !total_amount) {
    return res.status(400).json({ error: 'يرجى إكمال بيانات الزبون والعنوان والمبلغ الإجمالي' });
  }

  const orderNumber = generateOrderNumber();
  const subtotal = parseInt(total_amount, 10) || 500;
  const delivery_fee = 200;
  const total = subtotal + delivery_fee;

  try {
    const client = await db.pool.connect();
    try {
      await client.query('BEGIN');

      const insertOrder = await client.query(
        `INSERT INTO orders (
          order_number, customer_name, customer_phone, shop_id,
          neighborhood, address_description, status, subtotal, delivery_fee, total,
          notes, delivery_leg, is_manual_shop_order
        ) VALUES ($1, $2, $3, $4, $5, $6, 'READY_FOR_PICKUP', $7, $8, $9, $10, 'TO_SHOP', TRUE)
        RETURNING *`,
        [
          orderNumber, customer_name, customer_phone, shop_id,
          neighborhood || 'وسط المدينة', address_description,
          subtotal, delivery_fee, total,
          notes || items_description || 'طلب هاتفي مباشر'
        ]
      );

      const createdOrder = insertOrder.rows[0];

      // Insert line item describing the phone order
      await client.query(
        `INSERT INTO order_items (order_id, product_name, quantity, unit_price, subtotal)
         VALUES ($1, $2, 1, $3, $3)`,
        [createdOrder.id, items_description || 'وجبات طلب هاتفي', subtotal]
      );

      await client.query('COMMIT');

      // Broadcast to all online drivers immediately
      broadcastOrderStatus(createdOrder.id, 'READY_FOR_PICKUP', {
        shopId: shop_id,
        isManual: true,
        orderNumber: createdOrder.orderNumber
      });

      res.status(201).json({
        success: true,
        message: 'تم إدراج الطلب الهاتفي واستدعاء أسطول السائقين بنجاح 🛵',
        order: createdOrder
      });
    } catch (err) {
      await client.query('ROLLBACK');
      throw err;
    } finally {
      client.release();
    }
  } catch (err) {
    console.error('Manual order creation error:', err);
    res.status(500).json({ error: 'تعذر إنشاء الطلب الهاتفي' });
  }
});

// 3.3 Driver Accept Order with 2-Order Limiter (Deliverio Feature)
router.post('/:id/accept', async (req, res) => {
  const { id } = req.params;
  const { driver_id } = req.body;

  if (!driver_id) {
    return res.status(400).json({ error: 'معرف السائق مطلوب' });
  }

  try {
    // Check active orders limit (max 2 active orders)
    const activeCheck = await db.query(
      `SELECT COUNT(*) FROM orders 
       WHERE driver_id = $1 AND status IN ('READY_FOR_PICKUP', 'ON_THE_WAY')`,
      [driver_id]
    );

    const activeCount = parseInt(activeCheck.rows[0].count, 10);
    if (activeCount >= 2) {
      return res.status(400).json({
        error: 'لديك طلبان نشطان حالياً. يرجى إنهاء تسليمهما أولاً لضمان وصول الطعام ساخناً للزبائن 🛵'
      });
    }

    const result = await db.query(
      `UPDATE orders
       SET driver_id = $1,
           status = 'READY_FOR_PICKUP',
           delivery_leg = 'TO_SHOP',
           updated_at = NOW()
       WHERE id = $2 AND (driver_id IS NULL OR driver_id = $1)
       RETURNING *`,
      [driver_id, id]
    );

    if (result.rows.length === 0) {
      return res.status(409).json({ error: 'عذراً، تم قبول هذا الطلب من قِبل سائق آخر' });
    }

    const updated = result.rows[0];
    broadcastOrderStatus(updated.id, 'READY_FOR_PICKUP', {
      driverId: driver_id,
      deliveryLeg: 'TO_SHOP',
      updatedAt: updated.updated_at
    });

    res.json({
      success: true,
      message: 'تم استلام وتأكيد الطلب! توجه الآن إلى المطعم للاستلام 🏬',
      order: updated
    });
  } catch (err) {
    console.error('Driver accept error:', err);
    res.status(500).json({ error: 'تعذر قبول الطلب' });
  }
});

// 3.4 Driver Two-Leg Step Advancement (Pick up -> Picked up -> Finished)
router.patch('/:id/leg', async (req, res) => {
  const { id } = req.params;
  const { leg, driver_id } = req.body; // 'TO_SHOP' | 'TO_CUSTOMER' | 'FINISHED'

  try {
    let newStatus = 'ON_THE_WAY';
    let newLeg = leg;

    if (leg === 'TO_CUSTOMER') {
      newStatus = 'ON_THE_WAY';
      newLeg = 'TO_CUSTOMER';
    } else if (leg === 'FINISHED') {
      newStatus = 'DELIVERED';
      newLeg = 'DELIVERED';
    }

    const result = await db.query(
      `UPDATE orders
       SET delivery_leg = $1,
           status = $2,
           updated_at = NOW()
       WHERE id = $3 AND driver_id = $4
       RETURNING *`,
      [newLeg, newStatus, id, driver_id]
    );

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'الطلب غير موجود أو لا يتبع لهذا السائق' });
    }

    const updated = result.rows[0];

    // If finished, increment driver completed orders counter!
    if (newLeg === 'DELIVERED') {
      await db.query(
        `UPDATE users SET completed_orders_count = COALESCE(completed_orders_count, 0) + 1 WHERE id = $1`,
        [driver_id]
      );
      await db.query(
        `UPDATE driver_locations SET completed_orders_count = COALESCE(completed_orders_count, 0) + 1 WHERE driver_id = $1`,
        [driver_id]
      );
    }

    broadcastOrderStatus(updated.id, updated.status, {
      driverId: driver_id,
      deliveryLeg: newLeg,
      updatedAt: updated.updated_at
    });

    res.json({
      success: true,
      order: updated
    });
  } catch (err) {
    console.error('Update delivery leg error:', err);
    res.status(500).json({ error: 'تعذر تحديث مرحلة التوصيل' });
  }
});

// 4. List orders by Shop
router.get('/shop/:shopId', async (req, res) => {
  const { shopId } = req.params;
  try {
    const result = await db.query(
      `SELECT * FROM orders WHERE shop_id = $1 ORDER BY created_at DESC LIMIT 50`,
      [shopId]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'تعذر جلب طلبات المتجر' });
  }
});

// 5. List orders by Driver
router.get('/driver/:driverId', async (req, res) => {
  const { driverId } = req.params;
  try {
    const result = await db.query(
      `SELECT o.*, s.name AS shop_name, s.neighborhood AS shop_neighborhood
       FROM orders o
       JOIN shops s ON s.id = o.shop_id
       WHERE o.driver_id = $1 OR (o.status = 'READY_FOR_PICKUP' AND o.driver_id IS NULL)
       ORDER BY o.created_at DESC LIMIT 50`,
      [driverId]
    );
    res.json(result.rows);
  } catch (err) {
    res.status(500).json({ error: 'تعذر جلب طلبات السائق' });
  }
});

module.exports = router;
