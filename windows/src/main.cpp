#include <iostream>
#include "../../shared/src/auth/login_logic.h"
#include "../../shared/src/config/config_manager.h"

int main() {
    utarwifi::LoginConfig config;
    config.targetUrl = "https://example.com/login";

    utarwifi::SavedConfig saved = utarwifi::ConfigManager::load("config.txt");
    config.username = saved.username;
    config.password = saved.password;

    if (config.username.empty() || config.password.empty()) {
        std::cout << "Enter username and password first." << std::endl;
        return 1;
    }

    utarwifi::LoginResult result = utarwifi::LoginLogic::performLogin(config);
    std::cout << "status=" << result.statusCode << "\n";
    std::cout << result.body << std::endl;
    return 0;
}
