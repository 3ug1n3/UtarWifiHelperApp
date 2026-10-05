class PortalEndpoint {
  final String url;
  final int lastUsedTime;

  PortalEndpoint({
    required this.url,
    required this.lastUsedTime,
  });

  Map<String, dynamic> toMap() {
    return {
      'url': url,
      'lastUsedTime': lastUsedTime,
    };
  }

  factory PortalEndpoint.fromMap(Map<String, dynamic> map) {
    return PortalEndpoint(
      url: map['url'],
      lastUsedTime: map['lastUsedTime'],
    );
  }
}
