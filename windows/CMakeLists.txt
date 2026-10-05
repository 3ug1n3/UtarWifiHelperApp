cmake_minimum_required(VERSION 3.16)
project(utarwifihelper_windows LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_executable(utarwifihelper_windows
    src/main.cpp
    ../shared/src/auth/login_logic.cpp
    ../shared/src/config/config_manager.cpp
    ../shared/src/network/http_client.cpp
    ../shared/src/utils/string_utils.cpp
)

target_include_directories(utarwifihelper_windows PRIVATE ../shared/src)
target_link_libraries(utarwifihelper_windows PRIVATE wininet)
