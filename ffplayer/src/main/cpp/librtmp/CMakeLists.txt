cmake_minimum_required(VERSION 3.4.1)
##针对加密模块hashswf.c和rtmp.h中的宏定义判断，传入参数NO_CRYPTO，禁用加密算法
##rtmpDump是C语言开发，需要使用C语言的编译器编译
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DNO_CRYPTO")
add_library(
        rtmp
        STATIC
        amf.c
        hashswf.c
        log.c
        parseurl.c
        rtmp.c
)