cmake_minimum_required(VERSION 3.4.1)

set(gles3_src_DIR src/main/cpp/gles3)
add_subdirectory(
        ${gles3_src_DIR}
)
set(distribution_DIR ${CMAKE_SOURCE_DIR}/libs)

find_library(
        log-lib
        log
)

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

##CMake常用变量
#CMAKE_BINARY_DIR, PROJECT_BINARY_DIR ：这两个变量内容一致，如果是内部编译，就指的是工程的顶级目录，如果是外部编译，指的就是工程编译发生的目录。
#CMAKE_SOURCE_DIR, PROJECT_SOURCE_DIR：这两个变量内容一致，都指的是工程的顶级目录。
#CMAKE_CURRENT_BINARY_DIR：外部编译时，指的是target目录，内部编译时，指的是顶级目录
#CMAKE_CURRENT_SOURCE_DIR：CMakeList.txt所在的目录
#CMAKE_CURRENT_LIST_DIR：CMakeList.txt的完整路径
#CMAKE_CURRENT_LIST_LINE：当前所在的行
#CMAKE_MODULE_PATH：如果工程复杂，可能需要编写一些cmake模块，这里通过SET指定这个变量
#LIBRARY_OUTPUT_DIR, BINARY_OUTPUT_DIR：库和可执行的最终存放目录
#PROJECT_NAME, CMAKE_PROJECT_NAME：前者是当前CMakeList.txt里设置的project_name，后者是整个项目配置的project_name
##
