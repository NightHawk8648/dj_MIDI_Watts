#include <iostream>
#include <string>
#include <cstdlib>
#include <vector>
#include <fstream>

#if defined(_WIN32)
    #define PLATFORM_NAME "Windows"
    #define WHICH_CMD "where"
    #define NULL_REDIRECT " > nul 2>&1"
#elif defined(__APPLE__)
    #define PLATFORM_NAME "macOS"
    #define WHICH_CMD "which"
    #define NULL_REDIRECT " > /dev/null 2>&1"
#elif defined(__linux__)
    #define PLATFORM_NAME "Linux"
    #define WHICH_CMD "which"
    #define NULL_REDIRECT " > /dev/null 2>&1"
#else
    #define PLATFORM_NAME "Unknown"
    #define WHICH_CMD "which"
    #define NULL_REDIRECT " > /dev/null 2>&1"
#endif

// Determine architecture
std::string get_architecture() {
#if defined(_M_X64) || defined(__x86_64__)
    return "x64 (64-bit)";
#elif defined(_M_IX86) || defined(__i386__)
    return "x86 (32-bit)";
#elif defined(_M_ARM64) || defined(__aarch64__)
    return "ARM64";
#elif defined(_M_ARM) || defined(__arm__)
    return "ARM";
#else
    return "Unknown Architecture";
#endif
}

// Check if Linux is Debian/Ubuntu based
bool is_debian_linux() {
#if defined(__linux__)
    std::ifstream file("/etc/os-release");
    if (file.is_open()) {
        std::string line;
        while (std::getline(file, line)) {
            if (line.find("debian") != std::string::npos || line.find("ubuntu") != std::string::npos) {
                return true;
            }
        }
    }
#endif
    return false;
}

// Check command path existence
bool command_exists(const std::string& command) {
    std::string check_cmd = std::string(WHICH_CMD) + " " + command + NULL_REDIRECT;
    int result = std::system(check_cmd.c_str());
    return (result == 0);
}

int main() {
    std::cout << "=========================================================\n";
    std::cout << "        ULTIMA-GRID SYSTEM PARITY AUDIT UTILITY          \n";
    std::cout << "=========================================================\n";

    // 1. OS & Architecture Detection
    std::string os_info = PLATFORM_NAME;
    if (os_info == "Linux" && is_debian_linux()) {
        os_info += " (Debian/Ubuntu-based)";
    }
    
    std::cout << "[INFO] Operating System : " << os_info << "\n";
    std::cout << "[INFO] CPU Architecture : " << get_architecture() << "\n";
    std::cout << "---------------------------------------------------------\n";

    // 2. Dependency checks
    std::cout << "[AUDIT] Checking dependency binaries in PATH...\n";
    
    std::vector<std::string> tools = {"php", "npm", "node", "python", "uv"};
    bool all_found = true;

    for (const auto& tool : tools) {
        bool exists = command_exists(tool);
        std::cout << "  -> " << tool << " : ";
        if (exists) {
            std::cout << "\033[32mFOUND\033[0m\n";
        } else {
            std::cout << "\033[31mMISSING\033[0m\n";
            if (tool == "php" || tool == "npm" || tool == "python") {
                all_found = false;
            }
        }
    }

    std::cout << "---------------------------------------------------------\n";

    if (!all_found) {
        std::cout << "[WARNING] Some critical dependencies (PHP, NPM, or Python) are missing.\n";
        std::cout << "          Please install missing items to ensure full workspace sync.\n";
    } else {
        std::cout << "[SUCCESS] Core runtime dependencies verified.\n";
    }

    // 3. Spawning Python orchestrator setup
    std::cout << "[BOOTSTRAP] Launching setup_env.py to configure virtual environment...\n";
    
    std::string python_cmd = "python";
    if (!command_exists("python") && command_exists("python3")) {
        python_cmd = "python3";
    }

    // Execute setup_env.py script
#if defined(_WIN32)
    std::string script_run = python_cmd + " scripts\\setup_env.py";
#else
    std::string script_run = python_cmd + " scripts/setup_env.py";
#endif

    int run_result = std::system(script_run.c_str());
    if (run_result == 0) {
        std::cout << "[SUCCESS] Python environment configuration finished successfully.\n";
    } else {
        std::cout << "[ERROR] Python environment configuration script failed.\n";
    }

    std::cout << "=========================================================\n";
    return run_result;
}
