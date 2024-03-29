//
// Created by beiying on 2020/9/13.
//

#ifndef LOPNOR_PLAYERCONTROLLER_H
#define LOPNOR_PLAYERCONTROLLER_H

#include <pthread.h>
#include <android/native_window.h>
#include "JavaCallHelper.h"
#include "VideoPlayChannel.h"
#include "AudioPlayChannel.h"

extern "C" {
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/time.h>
#include <libavutil/frame.h>
#include <libavdevice/avdevice.h>
#include <libavfilter/avfilter.h>
#include <libavutil/log.h>
#include <libavcodec/jni.h>
#include <libswscale/swscale.h>

#include "libswresample/swresample.h"
#include "libavutil/opt.h"
#include "libavutil/imgutils.h"
};
//播放器的控制层
class PlayerController {
public:
    PlayerController(JavaCallHelper *callHelper, const char* dataSource);
    ~PlayerController();
    void prepare();//准备工作比较耗时，在子线程执行

    void prepareFFmpeg();
    void startPlay();

    void decode();
    void setRenderCallback(RenderFrame renderFrame);

    int duration;

    int getDuration();

    void seekTo(int progress);

    void stopPlay();


    pthread_t pid_prepare;
    pthread_t pid_play;
    pthread_t pid_stop;//释放线程
    char* url;
    AVFormatContext *formatContext;
    JavaCallHelper *javaCallHelper;
    VideoPlayChannel *videoPlayChannel;
    AudioPlayChannel *audioPlayChannel;

    bool isPlaying = false;
    int isSeek = 0;

    RenderFrame renderFrame;

    pthread_mutex_t seekMutex;//由于seek操作是在子线程执行的，需要加锁同步
};


#endif //LOPNOR_PLAYERCONTROLLER_H
