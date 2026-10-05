import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/app_state.dart';

class DashboardScreen extends StatelessWidget {
  const DashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('UTAR WiFi Helper', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          Padding(
            padding: const EdgeInsets.only(right: 16),
            child: Icon(Icons.circle, color: appState.config.autoLoginEnabled ? Colors.green : Colors.grey, size: 12),
          ),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _buildStatusHero(context, appState),
            const SizedBox(height: 16),
            _buildQuickStats(context, appState),
            const SizedBox(height: 16),
            _buildAutoReconnectCard(context, appState),
            const SizedBox(height: 16),
            _buildManualLoginButton(context, appState),
          ],
        ),
      ),
    );
  }

  Widget _buildStatusHero(BuildContext context, AppState state) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Theme.of(context).colorScheme.primaryContainer,
        borderRadius: BorderRadius.circular(24),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text('Current Connection', style: TextStyle(fontSize: 13, fontWeight: FontWeight.w500)),
              Container(
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12)),
                child: const Text(' WIFI ACTIVE ', style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold)),
              )
            ],
          ),
          const SizedBox(height: 4),
          const Text('utarwifi', style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold)),
          const SizedBox(height: 12),
          Row(
            children: [
              Icon(Icons.router, size: 18, color: Theme.of(context).colorScheme.onPrimaryContainer.withValues(alpha: 0.7)),
              const SizedBox(width: 8),
              const Text('Connected (Waiting for portal detection...)', style: TextStyle(fontSize: 12)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildQuickStats(BuildContext context, AppState state) {
    return Row(
      children: [
        Expanded(child: _buildStatCard(context, Icons.sync, 'Polling Rate', '${state.config.pollingIntervalSeconds}s')),
        const SizedBox(width: 12),
        Expanded(child: _buildStatCard(context, Icons.wifi, 'WiFi Usage', '0.00 GB')),
      ],
    );
  }

  Widget _buildStatCard(BuildContext context, IconData icon, String label, String value) {
    return Card(
      elevation: 0,
      color: Theme.of(context).colorScheme.surfaceContainerHighest,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Icon(icon, color: Theme.of(context).colorScheme.primary, size: 22),
            const SizedBox(height: 8),
            Text(label, style: const TextStyle(fontSize: 11)),
            Text(value, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          ],
        ),
      ),
    );
  }

  Widget _buildAutoReconnectCard(BuildContext context, AppState state) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20), side: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Auto-Reconnect', style: TextStyle(fontWeight: FontWeight.bold)),
                Text('Silent background login service', style: TextStyle(fontSize: 12)),
              ],
            ),
            Switch(
              value: state.config.autoLoginEnabled,
              onChanged: (val) {
                state.saveConfig(state.config.copyWith(autoLoginEnabled: val));
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildManualLoginButton(BuildContext context, AppState state) {
    return SizedBox(
      width: double.infinity,
      height: 50,
      child: ElevatedButton.icon(
        onPressed: state.isLoginLoading ? null : () => state.performManualLogin(),
        icon: state.isLoginLoading 
          ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2)) 
          : const Icon(Icons.login),
        label: Text(state.isLoginLoading ? 'Authenticating...' : 'Manual Login Push'),
        style: ElevatedButton.styleFrom(
          backgroundColor: Theme.of(context).colorScheme.primary,
          foregroundColor: Colors.white,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        ),
      ),
    );
  }
}
