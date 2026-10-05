import 'dart:async';
import 'dart:convert';
import 'dart:io';
import 'package:http/http.dart' as http;
import 'package:http/io_client.dart';
import 'package:flutter/services.dart';
import '../models/portal_endpoint.dart';
import 'database_service.dart';
import '../models/login_log.dart';
import 'rc4_cipher.dart';

const String mobileUA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.36";

class BlitzEngine {
  static const platform = MethodChannel('com.example.utarwifi/engine');
  final DatabaseService _db = DatabaseService();
  late final http.Client _client;

  final List<String> _probeUrls = [
    "http://connectivitycheck.gstatic.com/generate_204",
    "http://www.msftconnecttest.com/redirect",
    "http://captive.apple.com/hotspot-detect.html"
  ];

  BlitzEngine() {
    // Ignore SSL certificate errors (common on campus portals)
    final HttpClient innerClient = HttpClient()
      ..badCertificateCallback = ((X509Certificate cert, String host, int port) => true);
    _client = IOClient(innerClient);
  }

  Future<bool> checkInternetAccess() async {
    try {
      final response = await _client.get(
        Uri.parse("http://connectivitycheck.gstatic.com/generate_204"),
        headers: {"User-Agent": mobileUA},
      ).timeout(const Duration(seconds: 2));
      return response.statusCode == 204;
    } catch (e) {
      return false;
    }
  }

  Future<String?> detectPortalUrl() async {
    if (await checkInternetAccess()) return null;

    final Completer<String?> completer = Completer<String?>();
    int finished = 0;

    for (var url in _probeUrls) {
      _client.send(http.Request('GET', Uri.parse(url))
        ..followRedirects = false
        ..headers['User-Agent'] = mobileUA)
      .then((response) {
        final location = response.headers['location'] ?? response.headers['Location'];
        if (location != null && location.contains('/ac_portal/') && !completer.isCompleted) {
          completer.complete(location);
        }
      })
      .catchError((_) {})
      .whenComplete(() {
        finished++;
        if (finished == _probeUrls.length && !completer.isCompleted) {
          completer.complete(null);
        }
      });
    }

    return completer.future.timeout(const Duration(seconds: 2), onTimeout: () => null);
  }

  Future<void> performLogin({String? providedUrl}) async {
    final config = await _db.getConfig();
    if (config.username.isEmpty || config.password.isEmpty) return;

    // 1. Native Pulse (Android only)
    try {
      await platform.invokeMethod('startPulse');
    } catch (e) {
      // Ignore pulse start errors
    }

    String? targetUrl = providedUrl ?? await detectPortalUrl();
    
    if (targetUrl == null) {
      final endpoints = await _db.getAllEndpoints();
      final List<String> fallbacks = [
        'http://10.10.0.1',
        'http://203.0.113.1',
        'http://203.0.144.1',
      ];
      
      final allUrls = [...fallbacks, ...endpoints.map((e) => e.url)];
      final List<Future<String?>> jobs = [];
      
      for (int i = 0; i < allUrls.length; i++) {
        final url = allUrls[i];
        jobs.add(() async {
          final String rckey = DateTime.now().millisecondsSinceEpoch.toString();
          final String encryptedPass = Rc4Cipher.encrypt(rckey, config.password);
          try {
            // Slight stagger to avoid flooding the local AP
            await Future.delayed(Duration(milliseconds: i * 150));
            final success = await _attemptLogin(url, config.username, encryptedPass, rckey);
            return success ? url : null;
          } catch (e) {
            return null;
          }
        }());
      }

      targetUrl = await _raceToSuccess(jobs);
    } else {
      final String rckey = DateTime.now().millisecondsSinceEpoch.toString();
      final String encryptedPass = Rc4Cipher.encrypt(rckey, config.password);
      final success = await _attemptLogin(targetUrl, config.username, encryptedPass, rckey);
      if (!success) targetUrl = null;
    }

    if (targetUrl != null) {
      await _db.saveEndpoint(PortalEndpoint(url: targetUrl, lastUsedTime: DateTime.now().millisecondsSinceEpoch));
      await _db.addLog(LoginLog(
        timestamp: DateTime.now().millisecondsSinceEpoch,
        status: LogStatus.success,
        message: "Logged in successfully at $targetUrl",
      ));
      await _db.saveConfig(config.copyWith(
        lastLoginTime: DateTime.now().millisecondsSinceEpoch,
        lastLoginSuccess: true,
        lastStatusMessage: "Blitz Success",
      ));
    } else {
       await _db.addLog(LoginLog(
        timestamp: DateTime.now().millisecondsSinceEpoch,
        status: LogStatus.error,
        message: "Blitz failed. No response from campus blocks.",
      ));
    }

    // 3. Stop Native Pulse
    try {
      await platform.invokeMethod('stopPulse');
    } catch (e) {
      // Ignore pulse stop errors
    }
  }

  Future<bool> _attemptLogin(String portalUrl, String user, String encryptedPass, String rckey) async {
    Uri? parsed;
    try {
      parsed = Uri.parse(portalUrl);
    } catch (e) {
      return false;
    }

    final List<String> paths = ["/ac_portal/login.php", "/login.php"];

    for (var path in paths) {
      // 0. Include auth_tag in URL just in case (some versions of the portal require it)
      final loginUrl = "${parsed.scheme}://${parsed.host}:${parsed.port}$path?auth_tag=$rckey";
      
      try {
        String? cookie;
        try {
          final preResponse = await _client.get(Uri.parse(portalUrl)).timeout(const Duration(seconds: 2));
          cookie = preResponse.headers['set-cookie'];
        } catch (e) {}

        final response = await _client.post(
          Uri.parse(loginUrl),
          body: {
            'opr': 'pwdLogin',
            'userName': user,
            'pwd': encryptedPass,
            'auth_tag': rckey,
            'rememberPwd': '0',
          },
          headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
            'User-Agent': 'Mozilla/5.0',
            'Referer': portalUrl,
            if (cookie != null) 'Cookie': cookie,
          }
        ).timeout(const Duration(seconds: 5));

        final responseBody = response.body;
        final bool isSuccess = responseBody.contains('"success":true') || responseBody.contains('"success": true');

        if (isSuccess) {
          // 1. Tell Android IMMEDIATELY that we have internet (suppress popup)
          try { await platform.invokeMethod('reportConnected'); } catch (e) {}

          // 2. Handle redirect if portal requires it to finalize session
          try {
            final json = jsonDecode(responseBody);
            if (json['location'] != null) {
              String redirectUrl = json['location'].toString().replaceAll('\\/', '/');
              final fullRedirect = parsed.resolve(redirectUrl);
              await _client.get(fullRedirect, headers: {'User-Agent': 'Mozilla/5.0'}).timeout(const Duration(seconds: 3));
            }
          } catch (e) {}
          
          // 3. Robust Internet Verification
          for (int i = 0; i < 4; i++) {
            await Future.delayed(Duration(milliseconds: 800 + (i * 400)));
            if (await checkInternetAccess()) {
              try { await platform.invokeMethod('reportConnected'); } catch (e) {}
              return true;
            }
          }
          return true; // Return true because credentials were valid anyway
        }
      } catch (e) {
        continue;
      }
    }
    return false;
  }

  Future<String?> _raceToSuccess(List<Future<String?>> jobs) async {
    if (jobs.isEmpty) return null;
    Completer<String?> completer = Completer<String?>();
    int finished = 0;

    for (var job in jobs) {
      job.then((url) {
        if (url != null && !completer.isCompleted) {
          completer.complete(url);
        }
        finished++;
        if (finished == jobs.length && !completer.isCompleted) {
          completer.complete(null);
        }
      }).catchError((_) {
        finished++;
        if (finished == jobs.length && !completer.isCompleted) {
          completer.complete(null);
        }
      });
    }

    return completer.future;
  }
}
