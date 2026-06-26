cmake_minimum_required(VERSION 3.10)
project(HardwareManager CXX)

set(CMAKE_CXX_STANDARD 17)

# Help CMake find ZLIB on Windows using the MSYS2 path defined in the project
if(WIN32)
    list(APPEND CMAKE_PREFIX_PATH "C:/msys64/ucrt64")
endif()

# Find ZLIB library
find_package(ZLIB REQUIRED)

add_executable(hardware_manager main.cpp)
target_link_libraries(hardware_manager PRIVATE ZLIB::ZLIB)

