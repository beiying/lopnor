//
// Created by beiying on 2020/9/13.
//

#ifndef LOPNOR_VIDEOPLAYCHANNEL_H
#define LOPNOR_VIDEOPLAYCHANNEL_H


#include "JavaCallHelper.h"
#include "BasePlayChannel.h"
#include "AudioPlayChannel.h"

//定义一个渲染接口
typedef void (*RenderFrame)(uint8_t *, int, int, int);
class VideoPlayChannel:public BasePlayChannel {
public:
    VideoPlayChannel(int id, JavaCallHelper *callHelper, AVCodecContext *context, AVRational time_base);

    ~VideoPlayChannel();

    virtual void startPlay();
    virtual void stopPlay();

    void decodePacket();

    void playContent();

    void setRenderFrame(RenderFrame renderFrame);

    void setFps(int fps);

    AudioPlayChannel *audioPlayChannel;

private:
    pthread_t pid_video_play;//解码线程
    pthread_t pid_synchronize;//音视频同步播放线程
    RenderFrame renderFrame;
    int fps;
};


#endif //LOPNOR_VIDEOPLAYCHANNEL_H
