import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/app_state.dart';
import 'package:intl/intl.dart';

class DiagnosticsScreen extends StatelessWidget {
  const DiagnosticsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();

    return Scaffold(
      appBar: AppBar(title: const Text('Network Diagnostics', style: TextStyle(fontWeight: FontWeight.bold))),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          children: [
            _buildProbeCard(context),
            const SizedBox(height: 16),
            _buildLearnedPortalsCard(context, appState),
          ],
        ),
      ),
    );
  }

  Widget _buildProbeCard(BuildContext context) {
    final appState = context.read<AppState>();
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20), side: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            const Row(
              children: [
                Icon(Icons.bug_report, color: Colors.blue),
                SizedBox(width: 8),
                Text('Portal Detection Probe', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 16),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () => appState.runProbe(context),
                child: const Text('Run Probe'),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildLearnedPortalsCard(BuildContext context, AppState state) {
    return Card(
      elevation: 0,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20), side: BorderSide(color: Theme.of(context).colorScheme.outlineVariant)),
      child: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Row(
              children: [
                Icon(Icons.router, color: Colors.purple),
                SizedBox(width: 8),
                Text('Learned Campus Portals', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
              ],
            ),
            const SizedBox(height: 8),
            const Text('The app automatically remembers the login page of every campus block you visit.', style: TextStyle(fontSize: 12, color: Colors.grey)),
            const SizedBox(height: 16),
            if (state.endpoints.isEmpty)
              const Center(child: Text('No portals learned yet.', style: TextStyle(color: Colors.grey)))
            else
              ...state.endpoints.map((e) => ListTile(
                contentPadding: EdgeInsets.zero,
                title: Text(e.url, style: const TextStyle(fontSize: 13)),
                subtitle: Text('Last seen: ${DateFormat('yyyy-MM-dd').format(DateTime.fromMillisecondsSinceEpoch(e.lastUsedTime))}', style: const TextStyle(fontSize: 10)),
                trailing: IconButton(
                  icon: const Icon(Icons.delete, color: Colors.red, size: 20),
                  onPressed: () => state.deleteEndpoint(e.url),
                ),
              )),
          ],
        ),
      ),
    );
  }
}
