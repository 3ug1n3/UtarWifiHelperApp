# Implementation Plan - Porting Native Logic to Flutter

Move all functions from the previous native Android implementation (`UtarPortalEngine.kt` and `UtarRepository.kt`) to the current Flutter project.

## User Review Required

> [!IMPORTANT]
> The native Android WiFi toggle (`ensureWifiEnabled`) will be limited by Flutter's plugin capabilities. I will use `connectivity_plus` to monitor state but full programmatic WiFi toggling might require platform-specific code which I will attempt to integrate via the existing `MethodChannel`.

## Proposed Changes

### Service Layer

#### [MODIFY] [blitz_engine.dart](file:///C:/Users/HUAWEI/Downloads/utarwifiloginhelper-1.0.2/utarwifiloginhelper-1.0.2/utar_wifi_flutter/lib/services/blitz_engine.dart)
- Add `checkInternetAccess()` method.
- Add `detectPortalUrl()` method with probe list and redirect following logic.
- Enhance `_attemptLogin` to follow redirects if the JSON response contains a location.
- Update `performLogin()` to use the new detection logic.

#### [MODIFY] [app_state.dart](file:///C:/Users/HUAWEI/Downloads/utarwifiloginhelper-1.0.2/utarwifiloginhelper-1.0.2/utar_wifi_flutter/lib/services/app_state.dart)
- Implement `evaluateNetworkStatus()` logic from the native repository.
- Update `performManualLogin()` to be more robust.
- Add SSID matching logic.

### Model Layer

#### [NEW] [network_state.dart](file:///C:/Users/HUAWEI/Downloads/utarwifiloginhelper-1.0.2/utarwifiloginhelper-1.0.2/utar_wifi_flutter/lib/models/network_state.dart)
- Define `NetworkState` enum/class to match native implementation (InternetActive, CaptivePortalDetected, etc.).

## Verification Plan

### Automated Tests
- Run `flutter analyze` to ensure no syntax errors in the ported code.

### Manual Verification
- The user can verify by running the app and checking if portal detection works automatically when connected to UTAR WiFi.
