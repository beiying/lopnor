cmake_minimum_required(VERSION 3.4.1)
find_library(
        log-lib
        log
)

set(distribution_DIR ${CMAKE_SOURCE_DIR}/libs)
include_directories(libs/includes)

add_library(jpeg
        SHARED
        IMPORTED)
set_target_properties(jpeg
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libjpeg.so)

add_library(turbojpeg
        SHARED
        IMPORTED)
set_target_properties(turbojpeg
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libturbojpeg.so)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
add_library(
        jpegcompress
        SHARED
        src/main/cpp/jpeg_compress.cpp
)

target_link_libraries(
        jpegcompress
        android
        jpeg
        turbojpeg
        jnigraphics
        ${log-lib}
)