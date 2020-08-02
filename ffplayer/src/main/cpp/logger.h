//
// Created by beiying on 18/10/9.
//

#ifndef TALKMOMENTAPK_LOGGER_H
#define TALKMOMENTAPK_LOGGER_H

#include <android/log.h>
#define LOG_TAG "LiuYu"
#define LOGW(...)  __android_log_write(ANDROID_LOG_WARN,LOG_TAG,__VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)

#endif //TALKMOMENTAPK_LOGGER_H
