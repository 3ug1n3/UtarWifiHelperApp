enum LogStatus { info, success, warning, error }

class LoginLog {
  final int timestamp;
  final LogStatus status;
  final String message;
  final String? ssid;
  final String? portalUrl;

  LoginLog({
    required this.timestamp,
    required this.status,
    required this.message,
    this.ssid,
    this.portalUrl,
  });

  Map<String, dynamic> toMap() {
    return {
      'timestamp': timestamp,
      'status': status.name,
      'message': message,
      'ssid': ssid,
      'portalUrl': portalUrl,
    };
  }

  factory LoginLog.fromMap(Map<String, dynamic> map) {
    return LoginLog(
      timestamp: map['timestamp'],
      status: LogStatus.values.firstWhere((e) => e.name == map['status']),
      message: map['message'],
      ssid: map['ssid'],
      portalUrl: map['portalUrl'],
    );
  }
}
