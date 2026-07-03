cmake_minimum_required(VERSION 3.10)
project(HardwareManager CXX)

set(CMAKE_CXX_STANDARD 17)

# Ignore MSYS2 paths when using MSVC to prevent header conflicts
if(MSVC)
    list(APPEND CMAKE_IGNORE_PATH "C:/msys64/ucrt64" "C:/msys64/mingw64" "C:/msys64/usr")
endif()

# Since we are on Windows and ZLIB isn't built-in, use FetchContent to build it from source.
include(FetchContent)
FetchContent_Declare(
    zlib
    GIT_REPOSITORY https://github.com/madler/zlib.git
    GIT_TAG        v1.3.1
)
FetchContent_MakeAvailable(zlib)

add_executable(hardware_manager main.cpp)
# zlibstatic is the target name provided by ZLIB's CMakeLists when built statically
target_link_libraries(hardware_manager PRIVATE zlibstatic)
