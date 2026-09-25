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
