cmake_minimum_required(VERSION 3.4.1)

message("LIUYU CMake ${CMAKE_SOURCE_DIR}")

include_directories(
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${CMAKE_CURRENT_SOURCE_DIR}/util
        ${CMAKE_CURRENT_SOURCE_DIR}/render
        ${CMAKE_CURRENT_SOURCE_DIR}/sample
)

find_library(
        log-lib
        log
)
file(GLOB src-files
        ${CMAKE_CURRENT_SOURCE_DIR}/*.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/render/*.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/sample/*.cpp
        ${CMAKE_CURRENT_SOURCE_DIR}/util/*.cpp
        )

add_library(
        native-render
        SHARED
        ${src-files}
)

target_link_libraries(
        native-render
        ${log-lib}
        android
        EGL
        GLESv3
)