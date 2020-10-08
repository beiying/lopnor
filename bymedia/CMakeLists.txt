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