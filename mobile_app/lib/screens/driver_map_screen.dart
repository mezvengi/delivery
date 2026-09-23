import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import '../config.dart';
import '../services/api_service.dart';

class DriverMapScreen extends StatefulWidget {
  final ApiService api;
  final Function(Map<String, dynamic> driver) onDriverSelected;

  const DriverMapScreen({Key? key, required this.api, required this.onDriverSelected}) : super(key: key);

  @override
  State<DriverMapScreen> createState() => _DriverMapScreenState();
}

class _DriverMapScreenState extends State<DriverMapScreen> {
  List<dynamic> drivers = [];
  bool isLoading = true;
  final MapController mapController = MapController();

  @override
  void initState() {
    super.initState();
    loadDrivers();
  }

  Future<void> loadDrivers() async {
    setState(() => isLoading = true);
    try {
      final list = await widget.api.getAvailableDrivers();
      setState(() {
        drivers = list;
        isLoading = false;
      });
    } catch (e) {
      setState(() => isLoading = false);
    }
  }

  void _showDriverSheet(Map<String, dynamic> driver) {
    showModalBottomSheet(
      context: context,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
      ),
      builder: (ctx) {
        return Container(
          padding: const EdgeInsets.all(20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                children: [
                  const CircleAvatar(
                    backgroundColor: Colors.blue,
                    child: Icon(Icons.two_wheeler, color: Colors.white),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          driver['full_name'] ?? 'سائق توصيل',
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                        ),
                        Text(
                          '📞 ${driver['phone'] ?? ''}',
                          style: TextStyle(color: Colors.grey[600]),
                        ),
                      ],
                    ),
                  ),
                  const Chip(
                    label: Text('متوفر الآن', style: TextStyle(color: Colors.green, fontSize: 12)),
                    backgroundColor: Color(0xFFE8F5E9),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              ElevatedButton.icon(
                icon: const Icon(Icons.check_circle),
                label: const Text('اختيار هذا السائق لتوصيل طلبي'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.blue[700],
                  foregroundColor: Colors.white,
                  padding: const EdgeInsets.symmetric(vertical: 12),
                ),
                onPressed: () {
                  Navigator.pop(ctx);
                  widget.onDriverSelected(driver);
                  Navigator.pop(context); // return to order
                },
              ),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('اختر سائقاً من الخريطة'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: loadDrivers,
            tooltip: 'تحديث السائقين',
          ),
        ],
      ),
      body: Stack(
        children: [
          FlutterMap(
            mapController: mapController,
            options: MapOptions(
              initialCenter: const LatLng(AppConfig.sourLat, AppConfig.sourLng),
              initialZoom: 14.0,
            ),
            children: [
              TileLayer(
                urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                userAgentPackageName: 'com.sour.delivery',
              ),
              CircleLayer(
                circles: [
                  CircleMarker(
                    point: const LatLng(AppConfig.sourLat, AppConfig.sourLng),
                    color: Colors.blue.withOpacity(0.15),
                    borderColor: Colors.blue,
                    borderStrokeWidth: 2,
                    useRadiusInMeter: true,
                    radius: 3500, // 3.5km municipal coverage
                  ),
                ],
              ),
              MarkerLayer(
                markers: drivers.map((d) {
                  final lat = double.tryParse(d['lat'].toString()) ?? AppConfig.sourLat;
                  final lng = double.tryParse(d['lng'].toString()) ?? AppConfig.sourLng;
                  return Marker(
                    point: LatLng(lat, lng),
                    width: 44,
                    height: 44,
                    child: GestureDetector(
                      onTap: () => _showDriverSheet(d),
                      child: Container(
                        decoration: BoxDecoration(
                          color: Colors.blue[700],
                          shape: BoxShape.circle,
                          border: Border.all(color: Colors.white, width: 2),
                          boxShadow: const [
                            BoxShadow(color: Colors.black26, blurRadius: 4, offset: Offset(0, 2))
                          ],
                        ),
                        child: const Icon(Icons.two_wheeler, color: Colors.white, size: 24),
                      ),
                    ),
                  );
                }).toList(),
              ),
            ],
          ),
          if (isLoading)
            const Center(child: CircularProgressIndicator())
          else if (drivers.isEmpty)
            Positioned(
              top: 16,
              left: 16,
              right: 16,
              child: Card(
                color: Colors.orange[50],
                child: const Padding(
                  padding: EdgeInsets.all(12),
                  child: Text(
                    '⚠️ لا يوجد سائقون متصلون حالياً في سور الغزلان. يمكنك التبديل إلى "وضع السائق" بالأسفل لتسجيل دخول سائق تجريبي.',
                    textAlign: TextAlign.center,
                    style: TextStyle(color: Colors.brown, fontSize: 13),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}
