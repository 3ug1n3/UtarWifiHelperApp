import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/app_state.dart';

class CredentialsScreen extends StatefulWidget {
  const CredentialsScreen({super.key});

  @override
  State<CredentialsScreen> createState() => _CredentialsScreenState();
}

class _CredentialsScreenState extends State<CredentialsScreen> with SingleTickerProviderStateMixin {
  late TextEditingController _userController;
  late TextEditingController _passController;
  bool _isPasswordVisible = false;
  final double _saveButtonScale = 1.0;

  @override
  void initState() {
    super.initState();
    final state = context.read<AppState>();
    _userController = TextEditingController(text: state.config.username);
    _passController = TextEditingController(text: state.config.password);
  }

  @override
  void dispose() {
    _userController.dispose();
    _passController.dispose();
    super.dispose();
  }

  // Removed unused _triggerNudge method

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();

    return Scaffold(
      appBar: AppBar(title: const Text('Portal Credentials', style: TextStyle(fontWeight: FontWeight.bold))),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _buildCredentialsCard(context),
            const SizedBox(height: 16),
            _buildSettingsCard(context, appState),
            const SizedBox(height: 24),
            _buildSaveButton(context, appState),
          ],
        ),
      ),
    );
  }

  Widget _buildCredentialsCard(BuildContext context) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20), side: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('UTAR Portal Credentials', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 8),
            const Text('Your credentials are saved locally in an encrypted database.', style: TextStyle(fontSize: 13, color: Colors.grey)),
            const SizedBox(height: 20),
            TextField(
              controller: _userController,
              decoration: InputDecoration(
                prefixIcon: const Icon(Icons.person),
                labelText: 'Login ID / Student ID',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
              ),
            ),
            const SizedBox(height: 16),
            TextField(
              controller: _passController,
              obscureText: !_isPasswordVisible,
              decoration: InputDecoration(
                prefixIcon: const Icon(Icons.lock),
                labelText: 'Password',
                border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                suffixIcon: IconButton(
                  icon: Icon(_isPasswordVisible ? Icons.visibility : Icons.visibility_off),
                  onPressed: () => setState(() => _isPasswordVisible = !_isPasswordVisible),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSettingsCard(BuildContext context, AppState state) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20), side: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text('Auto-Login Behavior', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text('Background Polling'),
                Text('${state.config.pollingIntervalSeconds}s', style: const TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
            Slider(
              value: state.config.pollingIntervalSeconds.toDouble(),
              min: 10,
              max: 120,
              onChanged: (val) {
                state.saveConfig(state.config.copyWith(pollingIntervalSeconds: val.toInt()));
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSaveButton(BuildContext context, AppState state) {
    return AnimatedScale(
      scale: _saveButtonScale,
      duration: const Duration(milliseconds: 300),
      child: SizedBox(
        width: double.infinity,
        height: 50,
        child: ElevatedButton.icon(
          onPressed: () {
            state.saveConfig(state.config.copyWith(
              username: _userController.text,
              password: _passController.text,
            ));
            ScaffoldMessenger.of(context).showSnackBar(ApiResponseSnackBar(message: 'Credentials saved!'));
          },
          icon: const Icon(Icons.save),
          label: const Text('Save Credentials'),
          style: ElevatedButton.styleFrom(
            backgroundColor: Theme.of(context).colorScheme.primary,
            foregroundColor: Colors.white,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
          ),
        ),
      ),
    );
  }
}

class ApiResponseSnackBar extends SnackBar {
  final String message;
  ApiResponseSnackBar({super.key, required this.message}) : super(content: Text(message));
}
