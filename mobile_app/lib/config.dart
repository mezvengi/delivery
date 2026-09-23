class AppConfig {
  static const String domain = 'sour.serveirc.com';
  static const String baseUrl = 'https://$domain/api';
  static const String wsUrl = 'wss://$domain/ws';

  // Sour El Ghozlane Geolocation Center
  static const double sourLat = 36.1480;
  static const double sourLng = 3.6900;
  static const String defaultZoneId = 'sour_el_ghozlane';

  // Economy & Policies
  static const String currency = 'دج';
  static const double fixedDeliveryFee = 200.0;
  static const String paymentMethod = 'الدفع عند الاستلام';
}
