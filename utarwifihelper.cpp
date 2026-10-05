#pragma comment(linker,"\"/manifestdependency:type='win32' \
name='Microsoft.Windows.Common-Controls' version='6.0.0.0' \
processorArchitecture='*' publicKeyToken='6595b64144ccf1df' language='*'\"")

#ifndef UNICODE
#define UNICODE
#endif
#ifndef _UNICODE
#define _UNICODE
#endif

#include <windows.h>
#include <wininet.h>
#include <shlobj.h>
#include <shlwapi.h>
#include <shellapi.h>
#include <gdiplus.h>
#include <netlistmgr.h>
#include <wlanapi.h>
#include <string>
#include <vector>
#include <thread>
#include <chrono>
#include <sstream>
#include <fstream>
#include <iomanip>
#include <algorithm>
#include <cctype>
#include <mutex>

#pragma comment(lib, "wininet.lib")
#pragma comment(lib, "shlwapi.lib")
#pragma comment(lib, "gdi32.lib")
#pragma comment(lib, "user32.lib")
#pragma comment(lib, "gdiplus.lib")
#pragma comment(lib, "shell32.lib")
#pragma comment(lib, "ole32.lib")
#pragma comment(lib, "oleaut32.lib")
#pragma comment(lib, "wlanapi.lib")

// Window IDs
#define ID_BTN_LOGIN 101
#define ID_BTN_RETRY 102
#define ID_EDIT_USER 103
#define ID_EDIT_PASS 104
#define ID_TIMER_ANIM 105
#define ID_CHK_AUTOSTART 106
#define ID_CHK_AUTORECONNECT 107
#define ID_TIMER_POLL 108
#define ID_TRAY_RESTORE 110
#define ID_TRAY_CHECK 111
#define ID_TRAY_EXIT 112

#define WM_TRAYICON (WM_USER + 1)

// App modes
enum AppMode {
    MODE_LOGIN,
    MODE_PROGRESS
};

// Global variables
HWND g_hwnd = NULL;
HWND g_hwndUser = NULL;
HWND g_hwndPass = NULL;
HWND g_hwndLoginBtn = NULL;
HWND g_hwndRetryBtn = NULL;
HWND g_hwndStatusLabel = NULL;
HWND g_hwndFooter = NULL;
HWND g_hwndAutoStart = NULL;
HWND g_hwndAutoReconnect = NULL;

HFONT g_hFontTitle = NULL;
HFONT g_hFontNormal = NULL;
HFONT g_hFontStatus = NULL;
HFONT g_hFontButton = NULL;
HFONT g_hFontFooter = NULL;

AppMode g_mode = MODE_PROGRESS;
bool g_animating = false;
std::wstring g_statusText = L"Checking internet...";

std::wstring g_username = L"";
std::wstring g_password = L"";
std::wstring g_targetSsid = L"utarwifi";
std::wstring g_configPath = L"";

bool g_autoStart = false;
bool g_autoReconnect = true;
NOTIFYICONDATAW g_nid = { 0 };

// Forward declarations
void start_worker();

// RC4 Encryption
std::string rc4_encrypt(const std::string& key, const std::string& plaintext) {
    std::vector<unsigned char> sbox(256);
    for (int i = 0; i < 256; i++) {
        sbox[i] = i;
    }
    int j = 0;
    for (int i = 0; i < 256; i++) {
        j = (j + sbox[i] + (unsigned char)key[i % key.length()]) & 0xFF;
        std::swap(sbox[i], sbox[j]);
    }
    int i = 0;
    j = 0;
    std::string ciphertext = "";
    for (size_t k = 0; k < plaintext.length(); k++) {
        i = (i + 1) & 0xFF;
        j = (j + sbox[i]) & 0xFF;
        std::swap(sbox[i], sbox[j]);
        unsigned char K = sbox[(sbox[i] + sbox[j]) & 0xFF];
        unsigned char cipher_byte = (unsigned char)plaintext[k] ^ K;
        char hex[3];
        sprintf_s(hex, "%02x", cipher_byte);
        ciphertext += hex;
    }
    return ciphertext;
}

// URL encoding
std::string url_encode(const std::string& value) {
    std::ostringstream escaped;
    escaped.fill('0');
    escaped.setf(std::ios::hex, std::ios::basefield);
    for (char c : value) {
        if (isalnum((unsigned char)c) || c == '-' || c == '_' || c == '.' || c == '~') {
            escaped << c;
        } else {
            escaped << '%' << std::setw(2) << int((unsigned char)c);
        }
    }
    return escaped.str();
}

// Convert String Helper
std::wstring to_wstring(const std::string& str) {
    if (str.empty()) return L"";
    int size_needed = MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), NULL, 0);
    std::wstring wstrTo(size_needed, 0);
    MultiByteToWideChar(CP_UTF8, 0, &str[0], (int)str.size(), &wstrTo[0], size_needed);
    return wstrTo;
}

std::string to_string(const std::wstring& wstr) {
    if (wstr.empty()) return "";
    int size_needed = WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), NULL, 0, NULL, NULL);
    std::string strTo(size_needed, 0);
    WideCharToMultiByte(CP_UTF8, 0, &wstr[0], (int)wstr.size(), &strTo[0], size_needed, NULL, NULL);
    return strTo;
}

// Silent CLI runner
std::string exec_cmd_silent(const std::wstring& cmd) {
    HANDLE hChildStd_OUT_Rd = NULL;
    HANDLE hChildStd_OUT_Wr = NULL;
    SECURITY_ATTRIBUTES saAttr;
    saAttr.nLength = sizeof(SECURITY_ATTRIBUTES);
    saAttr.bInheritHandle = TRUE;
    saAttr.lpSecurityDescriptor = NULL;

    if (!CreatePipe(&hChildStd_OUT_Rd, &hChildStd_OUT_Wr, &saAttr, 0)) return "";
    if (!SetHandleInformation(hChildStd_OUT_Rd, HANDLE_FLAG_INHERIT, 0)) return "";

    PROCESS_INFORMATION piProcInfo;
    STARTUPINFOW siStartInfo;
    ZeroMemory(&piProcInfo, sizeof(PROCESS_INFORMATION));
    ZeroMemory(&siStartInfo, sizeof(STARTUPINFOW));
    siStartInfo.cb = sizeof(STARTUPINFOW);
    siStartInfo.hStdError = hChildStd_OUT_Wr;
    siStartInfo.hStdOutput = hChildStd_OUT_Wr;
    siStartInfo.dwFlags |= STARTF_USESTDHANDLES | STARTF_USESHOWWINDOW;
    siStartInfo.wShowWindow = SW_HIDE;

    std::wstring cmdCopy = cmd;
    if (!CreateProcessW(NULL, &cmdCopy[0], NULL, NULL, TRUE, CREATE_NO_WINDOW, NULL, NULL, &siStartInfo, &piProcInfo)) {
        CloseHandle(hChildStd_OUT_Rd);
        CloseHandle(hChildStd_OUT_Wr);
        return "";
    }
    CloseHandle(hChildStd_OUT_Wr);

    std::string result = "";
    char chBuf[4096];
    DWORD dwRead;
    while (ReadFile(hChildStd_OUT_Rd, chBuf, sizeof(chBuf), &dwRead, NULL) && dwRead > 0) {
        result.append(chBuf, dwRead);
    }
    WaitForSingleObject(piProcInfo.hProcess, INFINITE);
    CloseHandle(piProcInfo.hProcess);
    CloseHandle(piProcInfo.hThread);
    CloseHandle(hChildStd_OUT_Rd);
    return result;
}

// Get current network name using Native Wifi API (WLAN) with fallback to Network List Manager
std::string get_current_ssid() {
    HANDLE hClient = NULL;
    DWORD dwCurVersion = 0;
    std::string ssid = "";

    if (WlanOpenHandle(2, NULL, &dwCurVersion, &hClient) == ERROR_SUCCESS) {
        PWLAN_INTERFACE_INFO_LIST pIfList = NULL;
        if (WlanEnumInterfaces(hClient, NULL, &pIfList) == ERROR_SUCCESS) {
            for (DWORD i = 0; i < pIfList->dwNumberOfItems; i++) {
                PWLAN_INTERFACE_INFO pIfInfo = (PWLAN_INTERFACE_INFO)&pIfList->InterfaceInfo[i];
                if (pIfInfo->isState == wlan_interface_state_connected) {
                    DWORD dwDataSize = 0;
                    PWLAN_CONNECTION_ATTRIBUTES pConnectInfo = NULL;
                    if (WlanQueryInterface(hClient, &pIfInfo->InterfaceGuid, wlan_intf_opcode_current_connection, NULL, &dwDataSize, (PVOID*)&pConnectInfo, NULL) == ERROR_SUCCESS) {
                        if (pConnectInfo->wlanAssociationAttributes.dot11Ssid.uSSIDLength > 0) {
                            ssid = std::string((char*)pConnectInfo->wlanAssociationAttributes.dot11Ssid.ucSSID, pConnectInfo->wlanAssociationAttributes.dot11Ssid.uSSIDLength);
                        }
                        WlanFreeMemory(pConnectInfo);
                    }
                }
                if (!ssid.empty()) break;
            }
            if (pIfList) WlanFreeMemory(pIfList);
        }
        WlanCloseHandle(hClient, NULL);
    }

    if (ssid.empty()) {
        HRESULT hr = CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
        bool coInit = SUCCEEDED(hr) || hr == RPC_E_CHANGED_MODE;

        INetworkListManager* pNetworkListManager = NULL;
        hr = CoCreateInstance(CLSID_NetworkListManager, NULL, CLSCTX_ALL, IID_INetworkListManager, (void**)&pNetworkListManager);

        if (SUCCEEDED(hr)) {
            IEnumNetworks* pEnumNetworks = NULL;
            hr = pNetworkListManager->GetNetworks(NLM_ENUM_NETWORK_CONNECTED, &pEnumNetworks);
            if (SUCCEEDED(hr)) {
                INetwork* pNetwork = NULL;
                ULONG cFetched = 0;
                while (pEnumNetworks->Next(1, &pNetwork, &cFetched) == S_OK) {
                    BSTR bstrName = NULL;
                    if (SUCCEEDED(pNetwork->GetName(&bstrName))) {
                        std::wstring wname(bstrName);
                        ssid = to_string(wname);
                        SysFreeString(bstrName);
                    }
                    pNetwork->Release();
                    if (!ssid.empty()) break;
                }
                if (pEnumNetworks) pEnumNetworks->Release();
            }
            pNetworkListManager->Release();
        }
        if (coInit) CoUninitialize();
    }
    return ssid;
}

std::string to_lower_copy(const std::string& value) {
    std::string lower = value;
    std::transform(lower.begin(), lower.end(), lower.begin(), [](unsigned char c) {
        return static_cast<char>(std::tolower(c));
    });
    return lower;
}

// Helper to check if current network profile matches target SSID
bool is_matching_ssid(const std::string& current, const std::string& target) {
    if (current.empty()) return false;
    std::string clean_current = "";
    for (char c : current) {
        if (isprint((unsigned char)c)) clean_current += c;
    }
    std::string current_lower = to_lower_copy(clean_current);
    std::string target_lower = to_lower_copy(target);
    if (current_lower == target_lower) return true;
    if (current_lower.find(target_lower + " ") == 0) return true;
    return false;
}

// Connect to WiFi
bool scan_and_connect(const std::string& target_ssid) {
    exec_cmd_silent(L"netsh wlan show networks"); // triggers scan
    Sleep(2000);

    std::wstring quoted_ssid = L"\"" + to_wstring(target_ssid) + L"\"";
    std::vector<std::wstring> attempts = {
        L"netsh wlan connect name=" + quoted_ssid + L" interface=\"Wi-Fi\"",
        L"netsh wlan connect ssid=" + quoted_ssid + L" name=" + quoted_ssid + L" interface=\"Wi-Fi\"",
        L"netsh wlan connect name=" + quoted_ssid
    };

    for (const auto& cmd : attempts) {
        exec_cmd_silent(cmd);
        for (int i = 0; i < 6; i++) {
            Sleep(1000);
            std::string current = get_current_ssid();
            if (is_matching_ssid(current, target_ssid)) {
                Sleep(2000); // extra time for DHCP
                return true;
            }
        }
    }
    return false;
}

// Registry helper for Auto-Start
void set_autostart_registry(bool enable) {
    HKEY hKey;
    LONG lnRes = RegOpenKeyExW(HKEY_CURRENT_USER, L"Software\\Microsoft\\Windows\\CurrentVersion\\Run", 0, KEY_WRITE, &hKey);
    if (ERROR_SUCCESS == lnRes) {
        if (enable) {
            wchar_t szPath[MAX_PATH];
            GetModuleFileNameW(NULL, szPath, MAX_PATH);
            std::wstring cmd = L"\"" + std::wstring(szPath) + L"\" --minimized";
            RegSetValueExW(hKey, L"UtarWifiHelper", 0, REG_SZ, (BYTE*)cmd.c_str(), (DWORD)((cmd.length() + 1) * sizeof(wchar_t)));
        } else {
            RegDeleteValueW(hKey, L"UtarWifiHelper");
        }
        RegCloseKey(hKey);
    }
}

// Tray helper
void setup_tray_icon(HWND hwnd) {
    g_nid.cbSize = sizeof(NOTIFYICONDATAW);
    g_nid.hWnd = hwnd;
    g_nid.uID = 1;
    g_nid.uFlags = NIF_ICON | NIF_MESSAGE | NIF_TIP;
    g_nid.uCallbackMessage = WM_TRAYICON;
    g_nid.hIcon = LoadIconW(GetModuleHandle(NULL), MAKEINTRESOURCEW(101));
    wcscpy_s(g_nid.szTip, L"UTAR WiFi Helper");
    Shell_NotifyIconW(NIM_ADD, &g_nid);
}

void remove_tray_icon() {
    Shell_NotifyIconW(NIM_DELETE, &g_nid);
}

void show_tray_menu(HWND hwnd) {
    POINT pt;
    GetCursorPos(&pt);
    HMENU hMenu = CreatePopupMenu();
    if (hMenu) {
        InsertMenuW(hMenu, -1, MF_BYPOSITION, ID_TRAY_RESTORE, L"Open Utility");
        InsertMenuW(hMenu, -1, MF_BYPOSITION, ID_TRAY_CHECK, L"Check Connection Now");
        InsertMenuW(hMenu, -1, MF_SEPARATOR, 0, NULL);
        InsertMenuW(hMenu, -1, MF_BYPOSITION, ID_TRAY_EXIT, L"Exit Completely");

        SetForegroundWindow(hwnd);
        TrackPopupMenu(hMenu, TPM_BOTTOMALIGN | TPM_LEFTALIGN, pt.x, pt.y, 0, hwnd, NULL);
        DestroyMenu(hMenu);
    }
}

// Config utilities
std::wstring escape_json_str(const std::wstring& str) {
    std::wstring out = L"";
    for (wchar_t c : str) {
        if (c == L'\\' || c == L'"') out += L'\\';
        out += c;
    }
    return out;
}

std::wstring extract_json_value(const std::wstring& json, const std::wstring& key) {
    size_t pos = json.find(L"\"" + key + L"\"");
    if (pos == std::wstring::npos) return L"";
    pos = json.find(L":", pos);
    if (pos == std::wstring::npos) return L"";
    size_t start = json.find(L"\"", pos);
    if (start == std::wstring::npos) return L"";
    size_t end = json.find(L"\"", start + 1);
    if (end == std::wstring::npos) return L"";
    std::wstring val = json.substr(start + 1, end - start - 1);
    std::wstring unescaped = L"";
    for (size_t i = 0; i < val.length(); i++) {
        if (val[i] == L'\\' && i + 1 < val.length()) {
            if (val[i+1] == L'\\' || val[i+1] == L'"') {
                unescaped += val[i+1]; i++; continue;
            }
        }
        unescaped += val[i];
    }
    return unescaped;
}

void load_config() {
    wchar_t appDataPath[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPathW(NULL, CSIDL_APPDATA, NULL, 0, appDataPath))) {
        g_configPath = std::wstring(appDataPath) + L"\\UtarWifiHelper\\config.json";
        std::wifstream f(g_configPath);
        if (f.is_open()) {
            std::wstringstream ss; ss << f.rdbuf();
            std::wstring json = ss.str();
            g_username = extract_json_value(json, L"username");
            g_password = extract_json_value(json, L"password");
            std::wstring ssid = extract_json_value(json, L"target_ssid");
            if (!ssid.empty()) g_targetSsid = ssid;
            std::wstring autoS = extract_json_value(json, L"auto_start");
            g_autoStart = (autoS == L"true");
            std::wstring autoR = extract_json_value(json, L"auto_reconnect");
            if (!autoR.empty()) g_autoReconnect = (autoR == L"true");
        }
    }
}

void save_config(const std::wstring& username, const std::wstring& password, const std::wstring& ssid, bool autoStart, bool autoReconnect) {
    wchar_t appDataPath[MAX_PATH];
    if (SUCCEEDED(SHGetFolderPathW(NULL, CSIDL_APPDATA, NULL, 0, appDataPath))) {
        std::wstring dir = std::wstring(appDataPath) + L"\\UtarWifiHelper";
        CreateDirectoryW(dir.c_str(), NULL);
        g_configPath = dir + L"\\config.json";
        std::wofstream f(g_configPath);
        if (f.is_open()) {
            f << L"{\n";
            f << L"    \"username\": \"" << escape_json_str(username) << L"\",\n";
            f << L"    \"password\": \"" << escape_json_str(password) << L"\",\n";
            f << L"    \"target_ssid\": \"" << escape_json_str(ssid) << L"\",\n";
            f << L"    \"auto_start\": \"" << (autoStart ? L"true" : L"false") << L"\",\n";
            f << L"    \"auto_reconnect\": \"" << (autoReconnect ? L"true" : L"false") << L"\"\n";
            f << L"}\n";
        }
    }
    set_autostart_registry(autoStart);
}

// Network logic
bool check_internet() {
    HINTERNET hSession = InternetOpenA("Mozilla/5.0", INTERNET_OPEN_TYPE_PRECONFIG, NULL, NULL, 0);
    if (!hSession) return false;
    DWORD timeout = 3000;
    InternetSetOptionA(hSession, INTERNET_OPTION_CONNECT_TIMEOUT, &timeout, sizeof(timeout));
    HINTERNET hUrl = InternetOpenUrlA(hSession, "http://connectivitycheck.gstatic.com/generate_204", NULL, 0, INTERNET_FLAG_RELOAD | INTERNET_FLAG_NO_AUTO_REDIRECT, 0);
    bool active = false;
    if (hUrl) {
        char status_code[32] = {0}; DWORD buffer_len = sizeof(status_code);
        if (HttpQueryInfoA(hUrl, HTTP_QUERY_STATUS_CODE, status_code, &buffer_len, NULL)) {
            if (atoi(status_code) == 204) active = true;
        }
        InternetCloseHandle(hUrl);
    }
    InternetCloseHandle(hSession);
    return active;
}

std::string detect_portal_url() {
    const char* test_urls[3] = { "http://connectivitycheck.gstatic.com/generate_204", "http://www.msftconnecttest.com/redirect", "http://captive.apple.com/hotspot-detect.html" };
    for (int attempt = 0; attempt < 2; attempt++) {
        if (attempt > 0) Sleep(2000);
        HINTERNET hSession = InternetOpenA("Mozilla/5.0", INTERNET_OPEN_TYPE_PRECONFIG, NULL, NULL, 0);
        if (!hSession) continue;
        for (int i = 0; i < 3; i++) {
            HINTERNET hUrl = InternetOpenUrlA(hSession, test_urls[i], NULL, 0, INTERNET_FLAG_RELOAD | INTERNET_FLAG_NO_AUTO_REDIRECT, 0);
            if (hUrl) {
                char headers[4096] = {0}; DWORD headers_len = sizeof(headers);
                if (HttpQueryInfoA(hUrl, HTTP_QUERY_RAW_HEADERS_CRLF, headers, &headers_len, NULL)) {
                    std::string h_str(headers);
                    size_t loc_pos = h_str.find("Location: ");
                    if (loc_pos == std::string::npos) loc_pos = h_str.find("location: ");
                    if (loc_pos != std::string::npos) {
                        size_t end_pos = h_str.find("\r\n", loc_pos);
                        std::string loc = h_str.substr(loc_pos + 10, end_pos - (loc_pos + 10));
                        if (!loc.empty()) {
                            InternetCloseHandle(hUrl); InternetCloseHandle(hSession); return loc;
                        }
                    }
                }
                InternetCloseHandle(hUrl);
            }
        }
        InternetCloseHandle(hSession);
    }
    return "";
}

bool login(const std::string& portal_url, const std::string& username, const std::string& password) {
    URL_COMPONENTSA urlComp = {0}; urlComp.dwStructSize = sizeof(urlComp);
    char host[256] = {0}, path[2048] = {0};
    urlComp.lpszHostName = host; urlComp.dwHostNameLength = sizeof(host);
    urlComp.lpszUrlPath = path; urlComp.dwUrlPathLength = sizeof(path);
    if (!InternetCrackUrlA(portal_url.c_str(), 0, 0, &urlComp)) return false;

    // We try both common paths
    std::string paths[2] = { "/ac_portal/login.php", "/login.php" };

    for (int i = 0; i < 2; i++) {
        HINTERNET hSession = InternetOpenA("UtarWifiHelper", INTERNET_OPEN_TYPE_PRECONFIG, NULL, NULL, 0);
        if (!hSession) continue;

        DWORD flags = SECURITY_FLAG_IGNORE_UNKNOWN_CA | SECURITY_FLAG_IGNORE_WRONG_USAGE | SECURITY_FLAG_IGNORE_CERT_CN_INVALID | SECURITY_FLAG_IGNORE_CERT_DATE_INVALID | SECURITY_FLAG_IGNORE_REVOCATION;
        InternetSetOptionA(hSession, INTERNET_OPTION_SECURITY_FLAGS, &flags, sizeof(flags));

        HINTERNET hConnect = InternetConnectA(hSession, host, urlComp.nPort, NULL, NULL, INTERNET_SERVICE_HTTP, 0, 0);
        if (!hConnect) { InternetCloseHandle(hSession); continue; }

        DWORD reqFlags = INTERNET_FLAG_RELOAD | INTERNET_FLAG_IGNORE_CERT_CN_INVALID | INTERNET_FLAG_IGNORE_CERT_DATE_INVALID;
        if (urlComp.nScheme == INTERNET_SCHEME_HTTPS) reqFlags |= INTERNET_FLAG_SECURE;

        HINTERNET hRequest = HttpOpenRequestA(hConnect, "POST", paths[i].c_str(), NULL, portal_url.c_str(), NULL, reqFlags, 0);
        if (!hRequest) { InternetCloseHandle(hConnect); InternetCloseHandle(hSession); continue; }

        std::string rckey = std::to_string(std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::system_clock::now().time_since_epoch()).count());
        std::string payload = "opr=pwdLogin&userName=" + url_encode(username) + "&pwd=" + url_encode(rc4_encrypt(rckey, password)) + "&auth_tag=" + url_encode(rckey) + "&rememberPwd=0";
        const char* headers = "Content-Type: application/x-www-form-urlencoded\r\nUser-Agent: Mozilla/5.0\r\n";

        if (HttpSendRequestA(hRequest, headers, (DWORD)-1, (void*)payload.c_str(), (DWORD)payload.length())) {
            std::string response_data = ""; char buffer[1024]; DWORD read = 0;
            while (InternetReadFile(hRequest, buffer, sizeof(buffer), &read) && read > 0) response_data.append(buffer, read);

            if (response_data.find("\"success\":true") != std::string::npos || response_data.find("\"success\": true") != std::string::npos) {
                InternetCloseHandle(hRequest); InternetCloseHandle(hConnect); InternetCloseHandle(hSession);
                return true;
            }
        }
        InternetCloseHandle(hRequest); InternetCloseHandle(hConnect); InternetCloseHandle(hSession);
    }
    return false;
}

// UI State modifiers
void set_status(const std::wstring& text) {
    g_statusText = text;
    if (g_hwndStatusLabel) SetWindowTextW(g_hwndStatusLabel, text.c_str());
}

void switch_mode(AppMode mode) {
    g_mode = mode;
    int show = (mode == MODE_LOGIN) ? SW_SHOW : SW_HIDE;
    int hide = (mode == MODE_LOGIN) ? SW_HIDE : SW_SHOW;
    ShowWindow(g_hwndUser, show); ShowWindow(g_hwndPass, show); ShowWindow(g_hwndLoginBtn, show);
    ShowWindow(g_hwndAutoStart, show); ShowWindow(g_hwndAutoReconnect, show);
    ShowWindow(g_hwndStatusLabel, hide);
    ShowWindow(g_hwndRetryBtn, SW_HIDE);
    g_animating = (mode == MODE_PROGRESS);
    InvalidateRect(g_hwnd, NULL, TRUE);
}

void show_retry(const std::wstring& err_msg) {
    g_animating = false; set_status(err_msg);
    ShowWindow(g_hwndRetryBtn, SW_SHOW);
    InvalidateRect(g_hwnd, NULL, TRUE);
}

// Global for blitz coordination
std::string g_winner_portal = "";
std::mutex g_blitz_mutex;

void probe_thread(std::string url, std::string user, std::string pass) {
    if (login(url, user, pass)) {
        std::lock_guard<std::mutex> lock(g_blitz_mutex);
        if (g_winner_portal.empty()) {
            g_winner_portal = url;
        }
    }
}

// Polling Logic
void run_logic() {
    if (check_internet()) {
        set_status(L"[✓] Internet is active.");
        Sleep(1000);
        if (!g_autoReconnect) PostMessage(g_hwnd, WM_CLOSE, 0, 0);
        else {
            set_status(L"Connected. Monitoring...");
            ShowWindow(g_hwnd, SW_HIDE);
        }
        return;
    }
    std::string target_str = to_string(g_targetSsid);
    if (!is_matching_ssid(get_current_ssid(), target_str)) {
        set_status(L"Scanning for 'utarwifi'...");
        if (!scan_and_connect(target_str)) {
            if (!is_matching_ssid(get_current_ssid(), target_str)) {
                show_retry(L"Could not connect automatically."); return;
            }
        }
    }

    set_status(L"Blitz-Probing all campus blocks...");
    {
        std::lock_guard<std::mutex> lock(g_blitz_mutex);
        g_winner_portal = "";
    }

    std::string user = to_string(g_username);
    std::string pass = to_string(g_password);

    // Launch all attempts in parallel
    std::vector<std::string> fallbacks = { "http://203.0.113.1", "http://203.0.144.1", "http://10.10.0.1" };
    std::string detected = detect_portal_url();
    if (!detected.empty()) fallbacks.push_back(detected);

    std::vector<std::thread> threads;
    for (size_t i = 0; i < fallbacks.size(); i++) {
        threads.emplace_back(probe_thread, fallbacks[i], user, pass);
    }

    // Wait up to 3 seconds for a winner
    for (int i = 0; i < 30; i++) {
        {
            std::lock_guard<std::mutex> lock(g_blitz_mutex);
            if (!g_winner_portal.empty()) break;
        }
        Sleep(100);
    }

    for (auto& t : threads) t.detach();

    std::string winner;
    {
        std::lock_guard<std::mutex> lock(g_blitz_mutex);
        winner = g_winner_portal;
    }

    if (!winner.empty()) {
        set_status(L"[✓] Blitz Success!");
        save_config(g_username, g_password, g_targetSsid, g_autoStart, g_autoReconnect);
        Sleep(2000);
        if (!g_autoReconnect) PostMessage(g_hwnd, WM_CLOSE, 0, 0);
        else {
            set_status(L"Connected. Monitoring...");
            ShowWindow(g_hwnd, SW_HIDE);
        }
    } else {
        if (check_internet()) {
            set_status(L"[✓] Internet active.");
            ShowWindow(g_hwnd, SW_HIDE);
            return;
        }
        show_retry(L"Blitz failed. No response from campus.");
    }
}

void start_worker() {
    switch_mode(MODE_PROGRESS);
    set_status(L"Checking internet...");
    std::thread(run_logic).detach();
}

// Time helper
double GetTimeSeconds() {
    auto now = std::chrono::high_resolution_clock::now();
    auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(now.time_since_epoch()).count();
    return ms / 1000.0;
}

// Rounded polygon points computation
std::vector<Gdiplus::PointF> GetRoundedSquarePoints(double cx, double cy, double size, double radius, double angle_deg) {
    std::vector<Gdiplus::PointF> points;
    double angle_rad = angle_deg * 3.1415926535 / 180.0;
    double cos_a = cos(angle_rad), sin_a = sin(angle_rad);
    struct Corner { double cx_c, cy_c, start_angle; };
    Corner corners[4] = { { size - radius, size - radius, 0.0 }, { -size + radius, size - radius, 90.0 }, { -size + radius, -size + radius, 180.0 }, { size - radius, -size + radius, 270.0 } };
    for (int c = 0; c < 4; c++) {
        for (int i = 0; i <= 6; i++) {
            double theta = (corners[c].start_angle + (i * 90.0 / 6)) * 3.1415926535 / 180.0;
            double x = corners[c].cx_c + radius * cos(theta), y = corners[c].cy_c + radius * sin(theta);
            double rx = x * cos_a - y * sin_a, ry = x * sin_a + y * cos_a;
            points.push_back(Gdiplus::PointF((float)(cx + rx), (float)(cy + ry)));
        }
    }
    return points;
}

// Window procedure
LRESULT CALLBACK WndProc(HWND hwnd, UINT message, WPARAM wParam, LPARAM lParam) {
    switch (message) {
    case WM_CREATE: {
        g_hFontTitle = CreateFontW(22, 0, 0, 0, FW_BOLD, FALSE, FALSE, FALSE, ANSI_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, DEFAULT_QUALITY, DEFAULT_PITCH | FF_SWISS, L"Segoe UI");
        g_hFontNormal = CreateFontW(16, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, ANSI_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, DEFAULT_QUALITY, DEFAULT_PITCH | FF_SWISS, L"Segoe UI");
        g_hFontStatus = CreateFontW(19, 0, 0, 0, FW_MEDIUM, FALSE, FALSE, FALSE, ANSI_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, DEFAULT_QUALITY, DEFAULT_PITCH | FF_SWISS, L"Segoe UI");
        g_hFontButton = CreateFontW(16, 0, 0, 0, FW_BOLD, FALSE, FALSE, FALSE, ANSI_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, DEFAULT_QUALITY, DEFAULT_PITCH | FF_SWISS, L"Segoe UI");
        g_hFontFooter = CreateFontW(12, 0, 0, 0, FW_NORMAL, FALSE, FALSE, FALSE, ANSI_CHARSET, OUT_DEFAULT_PRECIS, CLIP_DEFAULT_PRECIS, DEFAULT_QUALITY, DEFAULT_PITCH | FF_SWISS, L"Segoe UI");

        g_hwndUser = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_BORDER | ES_AUTOHSCROLL, 40, 110, 264, 32, hwnd, (HMENU)ID_EDIT_USER, NULL, NULL);
        g_hwndPass = CreateWindowExW(0, L"EDIT", L"", WS_CHILD | WS_BORDER | ES_AUTOHSCROLL | ES_PASSWORD, 40, 155, 264, 32, hwnd, (HMENU)ID_EDIT_PASS, NULL, NULL);
        SendMessageW(g_hwndUser, WM_SETFONT, (WPARAM)g_hFontNormal, TRUE); SendMessageW(g_hwndPass, WM_SETFONT, (WPARAM)g_hFontNormal, TRUE);
        SendMessageW(g_hwndUser, 0x1501, FALSE, (LPARAM)L"Login ID"); SendMessageW(g_hwndPass, 0x1501, FALSE, (LPARAM)L"Password");

        g_hwndLoginBtn = CreateWindowExW(0, L"BUTTON", L"Log In", WS_CHILD | BS_OWNERDRAW, 40, 200, 264, 36, hwnd, (HMENU)ID_BTN_LOGIN, NULL, NULL);

        g_hwndAutoStart = CreateWindowExW(0, L"BUTTON", L"Start with Windows", WS_CHILD | BS_AUTOCHECKBOX, 40, 250, 264, 20, hwnd, (HMENU)ID_CHK_AUTOSTART, NULL, NULL);
        g_hwndAutoReconnect = CreateWindowExW(0, L"BUTTON", L"Auto-Reconnect in Background", WS_CHILD | BS_AUTOCHECKBOX, 40, 275, 264, 20, hwnd, (HMENU)ID_CHK_AUTORECONNECT, NULL, NULL);
        SendMessageW(g_hwndAutoStart, WM_SETFONT, (WPARAM)g_hFontNormal, TRUE); SendMessageW(g_hwndAutoReconnect, WM_SETFONT, (WPARAM)g_hFontNormal, TRUE);
        SendMessageW(g_hwndAutoStart, BM_SETCHECK, g_autoStart ? BST_CHECKED : BST_UNCHECKED, 0);
        SendMessageW(g_hwndAutoReconnect, BM_SETCHECK, g_autoReconnect ? BST_CHECKED : BST_UNCHECKED, 0);

        g_hwndRetryBtn = CreateWindowExW(0, L"BUTTON", L"Retry Now", WS_CHILD | BS_OWNERDRAW, 100, 310, 144, 36, hwnd, (HMENU)ID_BTN_RETRY, NULL, NULL);
        g_hwndStatusLabel = CreateWindowExW(0, L"STATIC", L"Checking...", WS_CHILD | SS_CENTER, 20, 180, 304, 80, hwnd, NULL, NULL, NULL);
        SendMessageW(g_hwndStatusLabel, WM_SETFONT, (WPARAM)g_hFontStatus, TRUE);

        g_hwndFooter = CreateWindowExW(0, L"STATIC", L"Developed by @khor0000 • Not affiliated with UTAR", WS_CHILD | WS_VISIBLE | SS_CENTER, 0, 365, 344, 20, hwnd, NULL, NULL, NULL);
        SendMessageW(g_hwndFooter, WM_SETFONT, (WPARAM)g_hFontFooter, TRUE);

        if (!g_username.empty()) SetWindowTextW(g_hwndUser, g_username.c_str());
        if (!g_password.empty()) SetWindowTextW(g_hwndPass, g_password.c_str());

        setup_tray_icon(hwnd);
        SetTimer(hwnd, ID_TIMER_ANIM, 16, NULL);
        SetTimer(hwnd, ID_TIMER_POLL, 60000, NULL);

        if (g_username.empty() || g_password.empty()) switch_mode(MODE_LOGIN);
        else start_worker();
        break;
    }
    case WM_TIMER:
        if (wParam == ID_TIMER_ANIM && g_animating && IsWindowVisible(hwnd)) {
            RECT r = { 100, 80, 240, 180 };
            InvalidateRect(hwnd, &r, FALSE);
        }
        else if (wParam == ID_TIMER_POLL && g_autoReconnect && !g_animating) {
            std::thread(run_logic).detach();
        }
        break;
    case WM_POWERBROADCAST:
        if (wParam == PBT_ACSUSPEND) {
            // System is suspending
            g_animating = false;
        } else if (wParam == PBT_ACSRESUMESUSPEND || wParam == PBT_ACSRESUMECRITICAL) {
            // System is resuming from sleep/hibernate
            if (g_autoReconnect && !g_username.empty() && !g_password.empty()) {
                Sleep(2000); // Give network a moment to stabilize
                std::thread(run_logic).detach();
            }
        }
        break;
    case WM_TRAYICON:
        if (lParam == WM_LBUTTONDBLCLK) { ShowWindow(hwnd, SW_RESTORE); SetForegroundWindow(hwnd); }
        else if (lParam == WM_RBUTTONUP) show_tray_menu(hwnd);
        break;
    case WM_SYSCOMMAND:
        if ((wParam & 0xFFF0) == SC_MINIMIZE) { ShowWindow(hwnd, SW_HIDE); return 0; }
        return DefWindowProc(hwnd, message, wParam, lParam);
    case WM_COMMAND:
        if (LOWORD(wParam) == ID_BTN_LOGIN) {
            wchar_t u[128], p[128]; GetWindowTextW(g_hwndUser, u, 128); GetWindowTextW(g_hwndPass, p, 128);
            g_username = u; g_password = p;
            g_autoStart = (SendMessageW(g_hwndAutoStart, BM_GETCHECK, 0, 0) == BST_CHECKED);
            g_autoReconnect = (SendMessageW(g_hwndAutoReconnect, BM_GETCHECK, 0, 0) == BST_CHECKED);
            save_config(g_username, g_password, g_targetSsid, g_autoStart, g_autoReconnect);
            start_worker();
        } else if (LOWORD(wParam) == ID_BTN_RETRY || LOWORD(wParam) == ID_TRAY_CHECK) start_worker();
        else if (LOWORD(wParam) == ID_TRAY_RESTORE) { ShowWindow(hwnd, SW_RESTORE); SetForegroundWindow(hwnd); }
        else if (LOWORD(wParam) == ID_TRAY_EXIT) { remove_tray_icon(); PostQuitMessage(0); }
        break;
    case WM_PAINT: {
        PAINTSTRUCT ps; HDC hdc = BeginPaint(hwnd, &ps);
        RECT rect; GetClientRect(hwnd, &rect); int w = rect.right, h = rect.bottom;
        HDC memDC = CreateCompatibleDC(hdc); HBITMAP memBM = CreateCompatibleBitmap(hdc, w, h); SelectObject(memDC, memBM);
        FillRect(memDC, &rect, (HBRUSH)GetStockObject(WHITE_BRUSH));
        SelectObject(memDC, g_hFontTitle); SetTextColor(memDC, RGB(26, 26, 26)); SetBkMode(memDC, TRANSPARENT);
        RECT rTitle = { 0, 15, w, 50 }; DrawTextW(memDC, L"UTAR WiFi Helper", -1, &rTitle, DT_CENTER);
        if (g_mode == MODE_PROGRESS && g_animating) {
            Gdiplus::Graphics g(memDC); g.SetSmoothingMode(Gdiplus::SmoothingModeAntiAlias);
            double t = GetTimeSeconds(), p = fmod(t, 2.0) / 2.0;
            double eased = (1.0 - cos(3.14159 * p)) / 2.0;
            auto pts = GetRoundedSquarePoints(w / 2.0, 110.0, 12.0, 12.0 * (0.06 + 0.44 * sin(3.14159 * eased)), eased * 360.0);
            Gdiplus::SolidBrush br(Gdiplus::Color(255, 0x1D, 0x4B, 0x9E)); g.FillPolygon(&br, pts.data(), (int)pts.size());
        }
        BitBlt(hdc, 0, 0, w, h, memDC, 0, 0, SRCCOPY); DeleteObject(memBM); DeleteDC(memDC); EndPaint(hwnd, &ps);
        break;
    }
    case WM_DRAWITEM: {
        LPDRAWITEMSTRUCT di = (LPDRAWITEMSTRUCT)lParam;
        if (di->CtlType == ODT_BUTTON) {
            HBRUSH br = CreateSolidBrush((di->itemState & ODS_SELECTED) ? RGB(46, 95, 181) : RGB(29, 75, 158));
            FillRect(di->hDC, &di->rcItem, br); DeleteObject(br);
            SetTextColor(di->hDC, RGB(255, 255, 255)); SetBkMode(di->hDC, TRANSPARENT);
            wchar_t txt[128]; GetWindowTextW(di->hwndItem, txt, 128); SelectObject(di->hDC, g_hFontButton);
            DrawTextW(di->hDC, txt, -1, &di->rcItem, DT_CENTER | DT_VCENTER | DT_SINGLELINE);
            return TRUE;
        }
        break;
    }
    case WM_CTLCOLORSTATIC: {
        SetBkMode((HDC)wParam, TRANSPARENT);
        return (LRESULT)GetStockObject(WHITE_BRUSH);
    }
    case WM_CLOSE: ShowWindow(hwnd, SW_HIDE); break;
    case WM_DESTROY: remove_tray_icon(); PostQuitMessage(0); break;
    default: return DefWindowProc(hwnd, message, wParam, lParam);
    }
    return 0;
}

int WINAPI WinMain(HINSTANCE hInst, HINSTANCE hPrev, LPSTR lpCmd, int nShow) {
    SetProcessDPIAware();
    Gdiplus::GdiplusStartupInput gsi; ULONG_PTR gToken; Gdiplus::GdiplusStartup(&gToken, &gsi, NULL);
    load_config();

    WNDCLASSEXW wcex = { 0 };
    wcex.cbSize = sizeof(WNDCLASSEXW);
    wcex.style = CS_HREDRAW | CS_VREDRAW;
    wcex.lpfnWndProc = WndProc;
    wcex.hInstance = hInst;
    wcex.hIcon = LoadIconW(hInst, MAKEINTRESOURCEW(101));
    wcex.hCursor = LoadCursor(NULL, IDC_ARROW);
    wcex.hbrBackground = (HBRUSH)GetStockObject(WHITE_BRUSH);
    wcex.lpszClassName = L"UtarWifiHelperWindow";
    wcex.hIconSm = LoadIconW(hInst, MAKEINTRESOURCEW(101));

    if (!RegisterClassExW(&wcex)) return 1;

    RECT r = { 0, 0, 344, 400 };
    AdjustWindowRectEx(&r, WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX, FALSE, 0);

    g_hwnd = CreateWindowExW(0, L"UtarWifiHelperWindow", L"UTAR WiFi Helper",
        WS_OVERLAPPED | WS_CAPTION | WS_SYSMENU | WS_MINIMIZEBOX,
        (GetSystemMetrics(SM_CXSCREEN) - 344) / 2,
        (GetSystemMetrics(SM_CYSCREEN) - 400) / 2,
        r.right - r.left, r.bottom - r.top, NULL, NULL, hInst, NULL);

    if (!g_hwnd) return 1;

    bool startMinimized = (std::string(lpCmd).find("--minimized") != std::string::npos);
    ShowWindow(g_hwnd, startMinimized ? SW_HIDE : nShow);
    UpdateWindow(g_hwnd);

    MSG msg;
    while (GetMessage(&msg, NULL, 0, 0)) {
        TranslateMessage(&msg);
        DispatchMessage(&msg);
    }

    Gdiplus::GdiplusShutdown(gToken);
    return (int)msg.wParam;
}
