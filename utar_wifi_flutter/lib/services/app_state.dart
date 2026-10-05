import 'dart:async';
import 'package:flutter/material.dart';
import 'package:network_info_plus/network_info_plus.dart';
import 'package:connectivity_plus/connectivity_plus.dart';
import '../models/user_config.dart';
import '../models/login_log.dart';
import '../models/portal_endpoint.dart';
import '../models/network_state.dart';
import 'database_service.dart';
import 'blitz_engine.dart';

class AppState extends ChangeNotifier {
  final DatabaseService _db = DatabaseService();
  final BlitzEngine _engine = BlitzEngine();
  final NetworkInfo _networkInfo = NetworkInfo();
  final Connectivity _connectivity = Connectivity();

  UserConfig _config = UserConfig();
  UserConfig get config => _config;

  List<LoginLog> _logs = [];
  List<LoginLog> get logs => _logs;

  List<PortalEndpoint> _endpoints = [];
  List<PortalEndpoint> get endpoints => _endpoints;

  int? _onboardingStep;
  int? get onboardingStep => _onboardingStep;

  final double _wifiUsageGb = 0.0;
  double get wifiUsageGb => _wifiUsageGb;

  bool _isLoginLoading = false;
  bool get isLoginLoading => _isLoginLoading;

  DateTime? _lastTriggerTime;

  AppState() {
    _init();
    _listenToConnectivity();
    _startMaintenanceTimer();
  }

  void _startMaintenanceTimer() {
    Timer.periodic(const Duration(seconds: 30), (timer) async {
      if (_config.autoLoginEnabled && !_isLoginLoading) {
        final ssid = await _networkInfo.getWifiName();
        if (ssid != null && isSsidMatch(ssid, _config.targetSsid)) {
          if (!await _engine.checkInternetAccess()) {
            performManualLogin();
          }
        }
      }
    });
  }

  Future<void> _init() async {
    _config = await _db.getConfig();
    _logs = await _db.getAllLogs();
    _endpoints = await _db.getAllEndpoints();
    if (!_config.isOnboardingCompleted) {
      _onboardingStep = 1;
    }
    notifyListeners();
  }

  void _listenToConnectivity() {
    _connectivity.onConnectivityChanged.listen((List<ConnectivityResult> results) async {
      if (results.contains(ConnectivityResult.wifi) && _config.autoLoginEnabled) {
        final now = DateTime.now();
        if (_lastTriggerTime != null && now.difference(_lastTriggerTime!).inSeconds < 10) return;
        _lastTriggerTime = now;

        final ssid = await _networkInfo.getWifiName();
        if (ssid != null && isSsidMatch(ssid, _config.targetSsid)) {
          performManualLogin();
        }
      }
    });
  }

  Future<void> saveConfig(UserConfig newConfig) async {
    _config = newConfig;
    await _db.saveConfig(_config);
    notifyListeners();
  }

  Future<void> performManualLogin() async {
    if (_isLoginLoading) return;
    _isLoginLoading = true;
    notifyListeners();
    
    try {
      // 1. Double check if internet is already active to avoid loops
      if (await _engine.checkInternetAccess()) {
        _isLoginLoading = false;
        notifyListeners();
        return;
      }

      final status = await evaluateNetworkStatus();
      String? portalUrl;
      if (status.status == NetworkStatus.captivePortalDetected) {
        portalUrl = status.portalUrl;
      }

      await _engine.performLogin(providedUrl: portalUrl);
    } finally {
      _logs = await _db.getAllLogs();
      _config = await _db.getConfig(); 
      _isLoginLoading = false;
      notifyListeners();
    }
  }

  Future<NetworkState> evaluateNetworkStatus() async {
    if (await _engine.checkInternetAccess()) {
      return NetworkState.internetActive();
    }

    final ssid = await _networkInfo.getWifiName();
    final cleanSsid = ssid?.replaceAll('"', '');
    
    final portalUrl = await _engine.detectPortalUrl();
    if (portalUrl != null) {
      return NetworkState.captivePortalDetected(portalUrl: portalUrl, ssid: cleanSsid);
    }

    if (cleanSsid != null && !isSsidMatch(cleanSsid, _config.targetSsid)) {
      return NetworkState.connectedNotTarget(currentSsid: cleanSsid, targetSsid: _config.targetSsid);
    }

    return NetworkState.disconnected();
  }

  bool isSsidMatch(String current, String target) {
    if (current.toLowerCase() == target.toLowerCase()) return true;
    if (current.toLowerCase().startsWith("${target.toLowerCase()} ")) return true;
    final cleanCurrent = current.replaceAll('"', '');
    final cleanTarget = target.replaceAll('"', '');
    return cleanCurrent.toLowerCase() == cleanTarget.toLowerCase() || 
           cleanCurrent.toLowerCase().startsWith("${cleanTarget.toLowerCase()} ");
  }

  void nextOnboardingStep() {
    if (_onboardingStep != null && _onboardingStep! < 8) {
      _onboardingStep = _onboardingStep! + 1;
    } else {
      completeOnboarding();
    }
    notifyListeners();
  }

  Future<void> completeOnboarding() async {
    _onboardingStep = null;
    _config = _config.copyWith(isOnboardingCompleted: true);
    await _db.saveConfig(_config);
    notifyListeners();
  }

  Future<void> deleteEndpoint(String url) async {
    await _db.deleteEndpoint(url);
    _endpoints = await _db.getAllEndpoints();
    notifyListeners();
  }

  Future<void> clearLogs() async {
    await _db.clearLogs();
    _logs = await _db.getAllLogs();
    notifyListeners();
  }

  Future<void> runProbe(BuildContext context) async {
    final status = await evaluateNetworkStatus();
    String message;
    
    switch (status.status) {
      case NetworkStatus.internetActive:
        message = "Internet is already active.";
        break;
      case NetworkStatus.captivePortalDetected:
        message = "Portal detected: ${status.portalUrl}";
        break;
      case NetworkStatus.connectedNotTarget:
        message = "Connected to ${status.currentSsid}, but target is ${status.targetSsid}";
        break;
      case NetworkStatus.disconnected:
        message = "Disconnected or no portal detected.";
        break;
    }

    if (context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text(message))
      );
    }
  }
}
