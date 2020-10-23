cmake_minimum_required(VERSION 3.4.1)
find_library(
        log-lib
        log
)

set(distribution_DIR ${CMAKE_SOURCE_DIR}/libs)
include_directories(libs/includes)

add_library(opencv
        SHARED
        IMPORTED)
set_target_properties(opencv
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libopencv_java4.so)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")
if (CMAKE_COMPILER_IS_GNUCXX)
    set(CMAKE_CXX_FLAGS  "-std=c++11 ${CMAKE_CXX_FLAGS}")
    message(STATUS "optional:-std=c++11")
endif(CMAKE_COMPILER_IS_GNUCXX)
add_library(
        imageutil
        SHARED
        src/main/cpp/opencv/imageutil.cpp
)

target_link_libraries(
        imageutil
        android
        opencv
        jnigraphics
        ${log-lib}
)