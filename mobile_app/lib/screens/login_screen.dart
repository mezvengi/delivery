import 'package:flutter/material.dart';
import '../services/api_service.dart';
import 'home_screen.dart';
import 'driver_mode_screen.dart';

class LoginScreen extends StatefulWidget {
  final ApiService api;
  final VoidCallback onLoginSuccess;

  const LoginScreen({Key? key, required this.api, required this.onLoginSuccess}) : super(key: key);

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final _phoneController = TextEditingController();
  final _codeController = TextEditingController();
  bool _codeSent = false;
  bool _isLoading = false;
  String _role = 'CUSTOMER';

  Future<void> _sendOtp() async {
    setState(() => _isLoading = true);
    try {
      await widget.api.sendOtp(_phoneController.text);
      setState(() => _codeSent = true);
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('خطأ: $e')));
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _verifyOtp() async {
    setState(() => _isLoading = true);
    try {
      await widget.api.verifyOtp(
        phone: _phoneController.text,
        code: _codeController.text,
        fullName: 'مستخدم جديد',
        role: _role,
      );
      widget.onLoginSuccess();
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text('خطأ: $e')));
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('تسجيل الدخول')),
      body: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            TextField(
              controller: _phoneController,
              decoration: const InputDecoration(labelText: 'رقم الهاتف', border: OutlineInputBorder()),
              keyboardType: TextInputType.phone,
            ),
            const SizedBox(height: 16),
            if (!_codeSent) ...[
              DropdownButton<String>(
                value: _role,
                items: const [
                  DropdownMenuItem(value: 'CUSTOMER', child: Text('زبون')),
                  DropdownMenuItem(value: 'DRIVER', child: Text('سائق')),
                ],
                onChanged: (val) => setState(() => _role = val!),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _isLoading ? null : _sendOtp,
                child: _isLoading ? const CircularProgressIndicator() : const Text('إرسال كود التحقق'),
              ),
            ] else ...[
              TextField(
                controller: _codeController,
                decoration: const InputDecoration(labelText: 'كود التحقق', border: OutlineInputBorder()),
                keyboardType: TextInputType.number,
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _isLoading ? null : _verifyOtp,
                child: _isLoading ? const CircularProgressIndicator() : const Text('تحقق ودخول'),
              ),
            ]
          ],
        ),
      ),
    );
  }
}
