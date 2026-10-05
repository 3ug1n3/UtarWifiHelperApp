import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../services/app_state.dart';
import '../models/login_log.dart';
import 'package:intl/intl.dart';

class LogsScreen extends StatelessWidget {
  const LogsScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();

    return Scaffold(
      appBar: AppBar(
        title: const Text('Activity Logs', style: TextStyle(fontWeight: FontWeight.bold)),
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_sweep),
            onPressed: () => _confirmClear(context, appState),
          )
        ],
      ),
      body: appState.logs.isEmpty 
        ? const Center(child: Text('No logs yet.'))
        : ListView.builder(
            itemCount: appState.logs.length,
            itemBuilder: (context, index) {
              final log = appState.logs[index];
              return ListTile(
                leading: _getIcon(log.status),
                title: Text(log.message, style: const TextStyle(fontSize: 14)),
                subtitle: Text(DateFormat('yyyy-MM-dd HH:mm:ss').format(DateTime.fromMillisecondsSinceEpoch(log.timestamp)), style: const TextStyle(fontSize: 11)),
              );
            },
          ),
    );
  }

  Widget _getIcon(LogStatus status) {
    switch (status) {
      case LogStatus.success: return const Icon(Icons.check_circle, color: Colors.green);
      case LogStatus.warning: return const Icon(Icons.warning, color: Colors.orange);
      case LogStatus.error: return const Icon(Icons.error, color: Colors.red);
      case LogStatus.info: return const Icon(Icons.info, color: Colors.blue);
    }
  }

  void _confirmClear(BuildContext context, AppState state) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Clear Logs?'),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context), child: const Text('Cancel')),
          TextButton(onPressed: () {
             state.clearLogs();
             Navigator.pop(context);
          }, child: const Text('Clear', style: TextStyle(color: Colors.red))),
        ],
      )
    );
  }
}
