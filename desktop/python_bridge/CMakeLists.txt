cmake_minimum_required(VERSION 3.10)
project(PythonBridgeAudit CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Source files
set(SOURCES
    src/main.cpp
)

# Output build targets
add_executable(python_bridge_audit ${SOURCES})

# Platform specific configurations
if(MSVC)
    target_compile_options(python_bridge_audit PRIVATE /W4 /WX)
else()
    target_compile_options(python_bridge_audit PRIVATE -Wall -Wextra -Werror)
endif()
