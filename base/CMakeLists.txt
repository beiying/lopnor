cmake_minimum_required(VERSION 3.4.1)
# 添加在native层log库
find_library( # Sets the name of the path variable.
        log-lib
        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)
##设置表示ffmpeg依赖库路径的系统变量
set(distribution_DIR ${CMAKE_SOURCE_DIR}/libs)
##设置依赖库的头文件夹
include_directories(libs/includes)

##引入crypt中的CMakeLists
#add_subdirectory(src/main/cpp/crypt)

# 在cmake脚本中，设置编译选项可以通过add_compile_options命令，也可以通过set命令修改CMAKE_CXX_FLAGS或CMAKE_C_FLAGS。
# 使用这两种方式在有的情况下效果是一样的，但请注意它们还是有区别的：
# add_compile_options命令添加的编译选项是针对所有编译器的(包括c和c++编译器)，而set命令设置CMAKE_C_FLAGS或CMAKE_CXX_FLAGS变量
# 则是分别只针对c和c++编译器的。
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

add_library(crypt
        SHARED
        src/main/cpp/crypt/crypt.cpp
        src/main/cpp/crypt/Md5.cpp
        )
target_link_libraries( # Specifies the target library.
        crypt
        android
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})