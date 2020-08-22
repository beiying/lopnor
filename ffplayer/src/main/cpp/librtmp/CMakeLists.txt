cmake_minimum_required(VERSION 3.4.1)
##针对宏定义判断，传入参数NO_CRYPTO，禁用加密算法
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