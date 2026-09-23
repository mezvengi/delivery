import 'dart:convert';
import 'package:http/http.dart' as http;
import '../config.dart';

class ApiService {
  String? _token;

  void setToken(String? token) {
    _token = token;
  }

  Map<String, String> _headers() {
    final headers = {'Content-Type': 'application/json'};
    if (_token != null) {
      headers['Authorization'] = 'Bearer $_token';
    }
    return headers;
  }

  // Auth: Send OTP
  Future<Map<String, dynamic>> sendOtp(String phone) async {
    final res = await http.post(
      Uri.parse('${AppConfig.baseUrl}/auth/send-otp'),
      headers: _headers(),
      body: jsonEncode({'phone': phone}),
    );
    return jsonDecode(res.body);
  }

  // Auth: Verify OTP
  Future<Map<String, dynamic>> verifyOtp({
    required String phone,
    required String code,
    required String fullName,
    required String role,
  }) async {
    final res = await http.post(
      Uri.parse('${AppConfig.baseUrl}/auth/verify-otp'),
      headers: _headers(),
      body: jsonEncode({
        'phone': phone,
        'code': code,
        'full_name': fullName,
        'role': role,
      }),
    );
    final data = jsonDecode(res.body);
    if (data['success'] == true && data['token'] != null) {
      setToken(data['token']);
    }
    return data;
  }

  // Shops
  Future<List<dynamic>> getShops() async {
    final res = await http.get(Uri.parse('${AppConfig.baseUrl}/shops'), headers: _headers());
    final data = jsonDecode(res.body);
    return data['shops'] ?? [];
  }

  // Products
  Future<List<dynamic>> getProducts(int shopId) async {
    final res = await http.get(Uri.parse('${AppConfig.baseUrl}/shops/$shopId/products'), headers: _headers());
    final data = jsonDecode(res.body);
    return data['products'] ?? [];
  }

  // Available Drivers on Map
  Future<List<dynamic>> getAvailableDrivers() async {
    final res = await http.get(
      Uri.parse('${AppConfig.baseUrl}/drivers/available?zone_id=${AppConfig.defaultZoneId}'),
      headers: _headers(),
    );
    final data = jsonDecode(res.body);
    return data['drivers'] ?? [];
  }

  // Create Order (Customer chooses driver)
  Future<Map<String, dynamic>> createOrder({
    required int shopId,
    required int driverId,
    required List<Map<String, dynamic>> items,
    required String neighborhood,
    required String description,
  }) async {
    final res = await http.post(
      Uri.parse('${AppConfig.baseUrl}/orders'),
      headers: _headers(),
      body: jsonEncode({
        'shop_id': shopId,
        'driver_id': driverId,
        'items': items,
        'delivery_neighborhood': neighborhood,
        'delivery_description': description,
      }),
    );
    return jsonDecode(res.body);
  }

  // List Orders
  Future<List<dynamic>> getOrders() async {
    final res = await http.get(Uri.parse('${AppConfig.baseUrl}/orders'), headers: _headers());
    final data = jsonDecode(res.body);
    return data['orders'] ?? [];
  }

  // Update Order Status (Driver)
  Future<Map<String, dynamic>> updateOrderStatus(int orderId, String status) async {
    final res = await http.patch(
      Uri.parse('${AppConfig.baseUrl}/orders/$orderId/status'),
      headers: _headers(),
      body: jsonEncode({'status': status}),
    );
    return jsonDecode(res.body);
  }

  // Driver: update live GPS location
  Future<void> updateDriverLocation(double lat, double lng) async {
    await http.post(
      Uri.parse('${AppConfig.baseUrl}/drivers/location'),
      headers: _headers(),
      body: jsonEncode({'lat': lat, 'lng': lng, 'heading': 0}),
    );
  }
}
