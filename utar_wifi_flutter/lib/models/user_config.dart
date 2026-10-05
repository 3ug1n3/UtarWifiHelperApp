class UserConfig {
  final String username;
  final String password;
  final String targetSsid;
  final bool autoLoginEnabled;
  final int pollingIntervalSeconds;
  final bool isOnboardingCompleted;
  final int lastLoginTime;
  final bool lastLoginSuccess;
  final String lastStatusMessage;

  UserConfig({
    this.username = '',
    this.password = '',
    this.targetSsid = 'utarwifi',
    this.autoLoginEnabled = true,
    this.pollingIntervalSeconds = 30,
    this.isOnboardingCompleted = false,
    this.lastLoginTime = 0,
    this.lastLoginSuccess = false,
    this.lastStatusMessage = 'Never logged in',
  });

  Map<String, dynamic> toMap() {
    return {
      'id': 1, // Always one config
      'username': username,
      'password': password,
      'targetSsid': targetSsid,
      'autoLoginEnabled': autoLoginEnabled ? 1 : 0,
      'pollingIntervalSeconds': pollingIntervalSeconds,
      'isOnboardingCompleted': isOnboardingCompleted ? 1 : 0,
      'lastLoginTime': lastLoginTime,
      'lastLoginSuccess': lastLoginSuccess ? 1 : 0,
      'lastStatusMessage': lastStatusMessage,
    };
  }

  factory UserConfig.fromMap(Map<String, dynamic> map) {
    return UserConfig(
      username: map['username'] ?? '',
      password: map['password'] ?? '',
      targetSsid: map['targetSsid'] ?? 'utarwifi',
      autoLoginEnabled: map['autoLoginEnabled'] == 1,
      pollingIntervalSeconds: map['pollingIntervalSeconds'] ?? 30,
      isOnboardingCompleted: map['isOnboardingCompleted'] == 1,
      lastLoginTime: map['lastLoginTime'] ?? 0,
      lastLoginSuccess: map['lastLoginSuccess'] == 1,
      lastStatusMessage: map['lastStatusMessage'] ?? '',
    );
  }

  UserConfig copyWith({
    String? username,
    String? password,
    String? targetSsid,
    bool? autoLoginEnabled,
    int? pollingIntervalSeconds,
    bool? isOnboardingCompleted,
    int? lastLoginTime,
    bool? lastLoginSuccess,
    String? lastStatusMessage,
  }) {
    return UserConfig(
      username: username ?? this.username,
      password: password ?? this.password,
      targetSsid: targetSsid ?? this.targetSsid,
      autoLoginEnabled: autoLoginEnabled ?? this.autoLoginEnabled,
      pollingIntervalSeconds: pollingIntervalSeconds ?? this.pollingIntervalSeconds,
      isOnboardingCompleted: isOnboardingCompleted ?? this.isOnboardingCompleted,
      lastLoginTime: lastLoginTime ?? this.lastLoginTime,
      lastLoginSuccess: lastLoginSuccess ?? this.lastLoginSuccess,
      lastStatusMessage: lastStatusMessage ?? this.lastStatusMessage,
    );
  }
}
