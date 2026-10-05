import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'services/app_state.dart';
import 'screens/dashboard_screen.dart';
import 'screens/credentials_screen.dart';
import 'screens/logs_screen.dart';
import 'screens/diagnostics_screen.dart';
import 'widgets/onboarding_overlay.dart';

void main() {
  runApp(
    ChangeNotifierProvider(
      create: (context) => AppState(),
      child: const UtarWifiApp(),
    ),
  );
}

class UtarWifiApp extends StatelessWidget {
  const UtarWifiApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'UTAR WiFi Helper',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFF6750A4), // ProPurple
          brightness: Brightness.light,
        ),
      ),
      darkTheme: ThemeData(
        useMaterial3: true,
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xFFD0BCFF),
          brightness: Brightness.dark,
        ),
      ),
      home: const MainNavigationFrame(),
    );
  }
}

class MainNavigationFrame extends StatefulWidget {
  const MainNavigationFrame({super.key});

  @override
  State<MainNavigationFrame> createState() => _MainNavigationFrameState();
}

class _MainNavigationFrameState extends State<MainNavigationFrame> {
  int _selectedIndex = 0;

  final List<Widget> _screens = [
    const DashboardScreen(),
    const CredentialsScreen(),
    const LogsScreen(),
    const DiagnosticsScreen(),
  ];

  @override
  Widget build(BuildContext context) {
    final appState = context.watch<AppState>();

    return Stack(
      children: [
        Scaffold(
          body: _screens[_selectedIndex],
          bottomNavigationBar: NavigationBar(
            selectedIndex: _selectedIndex,
            onDestinationSelected: (index) {
              setState(() => _selectedIndex = index);
            },
            destinations: const [
              NavigationDestination(icon: Icon(Icons.dashboard), label: 'Status'),
              NavigationDestination(icon: Icon(Icons.vpn_key), label: 'Credentials'),
              NavigationDestination(icon: Icon(Icons.receipt_long), label: 'Logs'),
              NavigationDestination(icon: Icon(Icons.build), label: 'Endpoints'),
            ],
          ),
        ),
        if (appState.onboardingStep != null)
          _buildOnboarding(appState),
      ],
    );
  }

  Widget _buildOnboarding(AppState state) {
    final step = state.onboardingStep!;
    String title = "";
    String desc = "";
    
    switch (step) {
      case 1:
        title = "Welcome!";
        desc = "UTAR WiFi Helper will automatically log you into the campus network.";
        break;
      case 2:
        title = "Auto-Login";
        desc = "The app monitors your WiFi and performs a 'Blitz Push' when you connect to utarwifi.";
        break;
      case 3:
        title = "Credentials";
        desc = "Go to the 'Credentials' tab to save your Student ID and Password locally.";
        break;
      case 4:
        title = "Blitz Engine";
        desc = "Our engine probes all campus blocks simultaneously to find the fastest login path.";
        break;
      case 5:
        title = "Activity Logs";
        desc = "Check the 'Logs' tab to see the history of your connection attempts.";
        break;
      case 6:
        title = "Endpoints";
        desc = "The app learns new campus portals as you visit different blocks.";
        break;
      case 7:
        title = "Background Service";
        desc = "Keep the app in the background for a seamless connection experience.";
        break;
      case 8:
        title = "Ready to Go!";
        desc = "You're all set! Connect to utarwifi and enjoy a hassle-free connection.";
        break;
    }

    return OnboardingOverlay(
      targetRect: Rect.zero,
      title: title,
      description: desc,
      step: step,
      totalSteps: 8,
      onNext: state.nextOnboardingStep,
      onSkip: state.completeOnboarding,
    );
  }
}
