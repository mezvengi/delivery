import 'package:flutter/material.dart';
import '../config.dart';
import '../services/api_service.dart';

class DriverModeScreen extends StatefulWidget {
  final ApiService api;
  const DriverModeScreen({Key? key, required this.api}) : super(key: key);

  @override
  State<DriverModeScreen> createState() => _DriverModeScreenState();
}

class _DriverModeScreenState extends State<DriverModeScreen> {
  final TextEditingController phoneController = TextEditingController(text: '0552222222');
  bool isOnline = false;
  List<dynamic> driverOrders = [];
  bool isLoading = false;

  Future<void> _loginDriver() async {
    setState(() => isLoading = true);
    try {
      final otp = await widget.api.sendOtp(phoneController.text);
      await widget.api.verifyOtp(
        phone: phoneController.text,
        code: otp['mock_code'] ?? '123456',
        fullName: 'أحمد السائق',
        role: 'DRIVER',
      );
      // Send location in Sour El Ghozlane
      await widget.api.updateDriverLocation(AppConfig.sourLat, AppConfig.sourLng);
      setState(() => isOnline = true);
      _loadOrders();
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(backgroundColor: Colors.green, content: Text('✅ تم تفعيل موقعك كسائق في سور الغزلان بنجاح')),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('خطأ: $e')));
    } finally {
      setState(() => isLoading = false);
    }
  }

  Future<void> _loadOrders() async {
    try {
      final ords = await widget.api.getOrders();
      setState(() => driverOrders = ords);
    } catch (e) {
      // ignore
    }
  }

  Future<void> _updateStatus(int orderId, String status) async {
    try {
      await widget.api.updateOrderStatus(orderId, status);
      _loadOrders();
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('تم تحديث حالة الطلب')));
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('خطأ: $e')));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('لوحة تحكم السائق'),
        backgroundColor: Colors.teal[700],
        foregroundColor: Colors.white,
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  children: [
                    TextField(
                      controller: phoneController,
                      decoration: const InputDecoration(labelText: 'رقم هاتف السائق', border: OutlineInputBorder()),
                    ),
                    const SizedBox(height: 12),
                    ElevatedButton.icon(
                      icon: Icon(isOnline ? Icons.location_on : Icons.location_off),
                      label: Text(isOnline ? 'موقعك نشط في سور الغزلان (اضغط للتحديث)' : 'تفعيل موقعي والظهور للزبائن'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: isOnline ? Colors.green[700] : Colors.teal[700],
                        foregroundColor: Colors.white,
                        minimumSize: const Size.fromHeight(44),
                      ),
                      onPressed: isLoading ? null : _loginDriver,
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('الطلبات المسندة إليك:', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16)),
                IconButton(icon: const Icon(Icons.refresh), onPressed: _loadOrders),
              ],
            ),
            const SizedBox(height: 8),
            if (driverOrders.isEmpty)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(32),
                  child: Text('لا توجد طلبات جديدة مسندة إليك حالياً', style: TextStyle(color: Colors.grey)),
                ),
              ),
            ...driverOrders.map((o) {
              return Card(
                margin: const EdgeInsets.only(bottom: 12),
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text('طلب #${o['id']} - ${o['customer_name'] ?? 'الزبون'}', style: const TextStyle(fontWeight: FontWeight.bold)),
                          Chip(label: Text(o['status'] ?? '')),
                        ],
                      ),
                      Text('📞 هاتف الزبون: ${o['customer_phone'] ?? ''}'),
                      Text('📍 العنوان: ${o['delivery_neighborhood']} - ${o['delivery_description']}'),
                      Text('💵 المبلغ للتحصيل نقداً: ${o['total_amount_da']} ${AppConfig.currency}'),
                      const SizedBox(height: 10),
                      if (o['status'] == 'CONFIRMED')
                        ElevatedButton.icon(
                          icon: const Icon(Icons.two_wheeler),
                          label: const Text('انطلاق للتوصيل'),
                          style: ElevatedButton.styleFrom(backgroundColor: Colors.blue[700], foregroundColor: Colors.white),
                          onPressed: () => _updateStatus(o['id'], 'OUT_FOR_DELIVERY'),
                        ),
                      if (o['status'] == 'OUT_FOR_DELIVERY')
                        ElevatedButton.icon(
                          icon: const Icon(Icons.done_all),
                          label: const Text('تم التسليم للزبون واستلام المبلغ نقداً'),
                          style: ElevatedButton.styleFrom(backgroundColor: Colors.green[700], foregroundColor: Colors.white),
                          onPressed: () => _updateStatus(o['id'], 'DELIVERED'),
                        ),
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
