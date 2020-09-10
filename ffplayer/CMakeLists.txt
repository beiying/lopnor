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

##引入rtmpdump中的CMakeLists
add_subdirectory(src/main/cpp/librtmp)

# FFmpeg编译出了7个库,这里添加----avutil
add_library(avutil
        SHARED
        IMPORTED)
set_target_properties(avutil
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libavutil.so)

# FFmpeg编译出了7个库,这里添加----swresample
add_library(swresample
        SHARED
        IMPORTED)
set_target_properties(swresample
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libswresample.so)

# FFmpeg编译出了7个库,这里添加----avcodec
add_library(avcodec
        SHARED
        IMPORTED)
set_target_properties(avcodec
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libavcodec.so)

# FFmpeg编译出了7个库,这里添加----avfilter
add_library(avfilter
        SHARED
        IMPORTED)
set_target_properties(avfilter
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libavfilter.so)

# FFmpeg编译出了7个库,这里添加----swscale
add_library(swscale
        SHARED
        IMPORTED)
set_target_properties(swscale
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libswscale.so)

# FFmpeg编译出了7个库,这里添加----avformat
add_library(avformat
        SHARED
        IMPORTED)
set_target_properties(avformat
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libavformat.so)
# FFmpeg编译出了7个库,这里添加----avdevice
add_library(avdevice
        SHARED
        IMPORTED)
set_target_properties(avdevice
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libavdevice.so)

# 添加----x264
add_library(x264
        SHARED
        IMPORTED)
set_target_properties(x264
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libx2641.so)

# 添加----faac
add_library(faac
        SHARED
        IMPORTED)
set_target_properties(faac
        PROPERTIES IMPORTED_LOCATION
        ${distribution_DIR}/armeabi-v7a/libfaac.so)

# 在cmake脚本中，设置编译选项可以通过add_compile_options命令，也可以通过set命令修改CMAKE_CXX_FLAGS或CMAKE_C_FLAGS。
# 使用这两种方式在有的情况下效果是一样的，但请注意它们还是有区别的：
# add_compile_options命令添加的编译选项是针对所有编译器的(包括c和c++编译器)，而set命令设置CMAKE_C_FLAGS或CMAKE_CXX_FLAGS变量
# 则是分别只针对c和c++编译器的。
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

add_library(avplayer
        SHARED
        src/main/cpp/avplayer.cpp
        src/main/cpp/media_info.c
        src/main/cpp/logger.h
        src/main/cpp/JNIEnvHelper.h
        src/main/cpp/JNIEnvHelper.cpp
        )
add_library(livepusher
        SHARED
        src/main/cpp/livepusher.cpp
        src/main/cpp/VideoChannel.h
        src/main/cpp/VideoChannel.cpp
        src/main/cpp/AudioChannel.h
        src/main/cpp/AudioChannel.cpp
        src/main/cpp/SafeQueue.h
        src/main/cpp/SafeQueue.cpp
        src/main/cpp/logger.h
        src/main/cpp/JNIEnvHelper.h
        src/main/cpp/JNIEnvHelper.cpp
        )
add_library( # Specifies the name of the library.
        algorithm

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        src/main/cpp/algorithm.cpp)

target_link_libraries( # Specifies the target library.
        avplayer
        GLESv2 EGL
        OpenSLES
        jnigraphics
        android
        rtmp
        # 这里需要注意下,下面这些ffmpeg的so库编译是有先后顺序的
        # 下面的顺序是没问题的,如果想验证编译顺序,可以自己变换顺序试试.
        avutil swresample swscale avcodec avformat avfilter avdevice
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
target_link_libraries( # Specifies the target library.
        livepusher
        GLESv2 EGL
        OpenSLES
        jnigraphics
        android
        rtmp
        # 这里需要注意下,下面这些ffmpeg的so库编译是有先后顺序的
        # 下面的顺序是没问题的,如果想验证编译顺序,可以自己变换顺序试试.
        avutil swresample swscale avcodec avformat avfilter avdevice
        x264
        faac
        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})