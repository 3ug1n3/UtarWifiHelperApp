enum NetworkStatus {
  disconnected,
  internetActive,
  captivePortalDetected,
  connectedNotTarget,
}

class NetworkState {
  final NetworkStatus status;
  final String? portalUrl;
  final String? currentSsid;
  final String? targetSsid;
  final String? message;

  NetworkState({
    required this.status,
    this.portalUrl,
    this.currentSsid,
    this.targetSsid,
    this.message,
  });

  factory NetworkState.internetActive() => NetworkState(status: NetworkStatus.internetActive);
  
  factory NetworkState.disconnected() => NetworkState(status: NetworkStatus.disconnected);

  factory NetworkState.captivePortalDetected({required String portalUrl, String? ssid}) => 
    NetworkState(status: NetworkStatus.captivePortalDetected, portalUrl: portalUrl, currentSsid: ssid);

  factory NetworkState.connectedNotTarget({required String currentSsid, required String targetSsid}) => 
    NetworkState(status: NetworkStatus.connectedNotTarget, currentSsid: currentSsid, targetSsid: targetSsid);
}
