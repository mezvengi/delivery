import 'package:flutter/material.dart';
import '../config.dart';
import '../services/api_service.dart';
import 'driver_map_screen.dart';

class HomeScreen extends StatefulWidget {
  final ApiService api;
  const HomeScreen({Key? key, required this.api}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  List<dynamic> shops = [];
  int? selectedShopId;
  List<dynamic> products = [];
  Map<int, int> cartQuantities = {};
  Map<String, dynamic>? selectedDriver;

  String selectedNeighborhood = 'حي الوئام';
  final TextEditingController descController = TextEditingController();
  List<dynamic> myOrders = [];
  bool isLoading = false;

  final List<String> neighborhoods = [
    'حي الوئام',
    'حي 114 مسكن',
    'وسط المدينة',
    'حي ذراع البرج',
    'حي عين مريم',
    'حي باب الجزائر',
    'حي باب البوسعادة',
    'المنطقة الصناعية',
    'حي النصر',
  ];

  @override
  void initState() {
    super.initState();
    _bootstrap();
  }

  Future<void> _bootstrap() async {
    setState(() => isLoading = true);
    // Auto-login removed (now handled in main.dart via LoginScreen)
    try {
      final s = await widget.api.getShops();
      setState(() {
        shops = s;
        if (s.isNotEmpty) {
          selectedShopId = s[0]['id'];
        }
      });
      if (selectedShopId != null) {
        _loadProducts(selectedShopId!);
      }
      _loadOrders();
    } catch (e) {
      // ignore
    } finally {
      setState(() => isLoading = false);
    }
  }

  Future<void> _loadProducts(int shopId) async {
    final prods = await widget.api.getProducts(shopId);
    setState(() {
      products = prods;
      cartQuantities.clear();
      for (var p in prods) {
        cartQuantities[p['id']] = 1;
      }
    });
  }

  Future<void> _loadOrders() async {
    final ords = await widget.api.getOrders();
    setState(() => myOrders = ords);
  }

  double get itemsTotal {
    double sum = 0;
    for (var p in products) {
      final qty = cartQuantities[p['id']] ?? 0;
      final price = double.tryParse(p['price_da'].toString()) ?? 0;
      sum += (price * qty);
    }
    return sum;
  }

  double get orderTotal => itemsTotal + AppConfig.fixedDeliveryFee;

  Future<void> _submitOrder() async {
    if (selectedShopId == null) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('يرجى اختيار متجر أولاً')));
      return;
    }
    if (selectedDriver == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('⚠️ يرجى النقر على "اختيار سائق من الخريطة" وتحديد سائق لتوصيل طلبك')),
      );
      return;
    }

    final items = <Map<String, dynamic>>[];
    for (var p in products) {
      final qty = cartQuantities[p['id']] ?? 0;
      if (qty > 0) {
        items.add({'product_id': p['id'], 'quantity': qty});
      }
    }

    if (items.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('السلة فارغة')));
      return;
    }

    try {
      final res = await widget.api.createOrder(
        shopId: selectedShopId!,
        driverId: selectedDriver!['id'],
        items: items,
        neighborhood: selectedNeighborhood,
        description: descController.text,
      );

      if (res['success'] == true) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: Colors.green,
            content: Text('✅ تم إرسال الطلب بنجاح برقم #${res['order']['id']} والدفع عند الاستلام'),
          ),
        );
        _loadOrders();
      } else {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(res['error'] ?? 'خطأ')));
      }
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('فشل إرسال الطلب: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    if (isLoading) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }

    return Scaffold(
      appBar: AppBar(
        title: const Text('توصيل سور الغزلان'),
        backgroundColor: Colors.blue[700],
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Driver Selection Banner
            Card(
              color: selectedDriver != null ? Colors.blue[50] : Colors.amber[50],
              elevation: 2,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Icon(
                          Icons.two_wheeler,
                          color: selectedDriver != null ? Colors.blue[700] : Colors.amber[800],
                          size: 32,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                selectedDriver != null
                                    ? 'السائق المختار: ${selectedDriver!['full_name']}'
                                    : 'لم يتم اختيار سائق بعد',
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                              ),
                              Text(
                                selectedDriver != null
                                    ? '📞 ${selectedDriver!['phone']}'
                                    : 'اختر سائقك المفضل مباشرة من الخريطة',
                                style: TextStyle(color: Colors.grey[700], fontSize: 13),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 10),
                    ElevatedButton.icon(
                      icon: const Icon(Icons.map),
                      label: Text(selectedDriver != null ? 'تغيير السائق من الخريطة' : '🗺️ عرض السائقين على الخريطة واختيار واحد'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.blue[700],
                        foregroundColor: Colors.white,
                        minimumSize: const Size.fromHeight(42),
                      ),
                      onPressed: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (ctx) => DriverMapScreen(
                              api: widget.api,
                              onDriverSelected: (d) {
                                setState(() => selectedDriver = d);
                              },
                            ),
                          ),
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 16),

            // Select Shop
            const Text('المتجر / المطعم في سور الغزلان:', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 6),
            DropdownButtonFormField<int>(
              value: selectedShopId,
              decoration: const InputDecoration(border: OutlineInputBorder(), contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8)),
              items: shops.map((s) => DropdownMenuItem<int>(value: s['id'], child: Text('${s['name']} (${s['category']})'))).toList(),
              onChanged: (val) {
                if (val != null) {
                  setState(() => selectedShopId = val);
                  _loadProducts(val);
                }
              },
            ),
            const SizedBox(height: 16),

            // Products
            const Text('قائمة المنتجات المتاحة:', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 6),
            ...products.map((p) {
              return Card(
                child: ListTile(
                  title: Text(p['name'] ?? ''),
                  subtitle: Text(p['description'] ?? ''),
                  trailing: Text(
                    '${p['price_da']} ${AppConfig.currency}',
                    style: const TextStyle(fontWeight: FontWeight.bold, color: Colors.blue, fontSize: 15),
                  ),
                ),
              );
            }).toList(),
            const SizedBox(height: 16),

            // Delivery Neighborhood
            const Text('مكان التوصيل (سور الغزلان):', style: TextStyle(fontWeight: FontWeight.bold)),
            const SizedBox(height: 6),
            DropdownButtonFormField<String>(
              value: selectedNeighborhood,
              decoration: const InputDecoration(border: OutlineInputBorder(), contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 8)),
              items: neighborhoods.map((n) => DropdownMenuItem(value: n, child: Text(n))).toList(),
              onChanged: (val) {
                if (val != null) setState(() => selectedNeighborhood = val);
              },
            ),
            const SizedBox(height: 10),
            TextField(
              controller: descController,
              decoration: const InputDecoration(
                border: OutlineInputBorder(),
                labelText: 'وصف إضافي للعنوان / رقم الباب',
                hintText: 'مثال: قرب المسجد، عمارة 4 الطابق الأول',
              ),
            ),
            const SizedBox(height: 16),

            // Summary Card
            Card(
              color: Colors.grey[100],
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('مجموع المنتجات:'),
                        Text('$itemsTotal ${AppConfig.currency}'),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('سعر التوصيل الثابت:'),
                        Text('${AppConfig.fixedDeliveryFee} ${AppConfig.currency}'),
                      ],
                    ),
                    const Divider(),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('المبلغ الإجمالي (دفع عند الاستلام):', style: TextStyle(fontWeight: FontWeight.bold)),
                        Text(
                          '$orderTotal ${AppConfig.currency}',
                          style: TextStyle(fontWeight: FontWeight.bold, color: Colors.green[700], fontSize: 16),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 14),

            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.green[700],
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
              ),
              onPressed: _submitOrder,
              child: const Text('✅ تأكيد الطلب مع السائق المختار', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            ),
            const SizedBox(height: 24),

            // Orders list
            const Text('📦 طلباتي السابقة:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
            const SizedBox(height: 8),
            ...myOrders.map((o) {
              return Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text('طلب #${o['id']} - ${o['shop_name'] ?? ''}', style: const TextStyle(fontWeight: FontWeight.bold)),
                          Chip(label: Text(o['status'] ?? '', style: const TextStyle(fontSize: 12))),
                        ],
                      ),
                      Text('🛵 السائق: ${o['driver_name'] ?? 'لم يُحدد'}'),
                      Text('📍 العنوان: ${o['delivery_neighborhood']}'),
                      Text('💵 الإجمالي: ${o['total_amount_da']} ${AppConfig.currency} (الدفع عند الاستلام)'),
                    ],
                  ),
                ),
              );
            }).toList(),
          ],
        ),
      ),
    );
  }
}
