//
// Created by beiying on 2020/9/13.
//

#ifndef LOPNOR_BASEPLAYCHANNEL_H
#define LOPNOR_BASEPLAYCHANNEL_H

#include "SafeQueue.h"
#include "JavaCallHelper.h"
#include "logger.h"

extern "C" {
#include "libavcodec/avcodec.h"
#include "libavutil/frame.h"
#include "libavutil/time.h"
#include "libavutil/imgutils.h"
#include "libswscale/swscale.h"
#include "libswresample/swresample.h"
};

/**
 * 音频和视频解码时都有AVPacket和AVFrame
 * */
class BasePlayChannel {
public:
    BasePlayChannel(int id, JavaCallHelper *callHelper, AVCodecContext *context, AVRational time_base)
            : channleId(id), javaCallHelper(callHelper), codecContext(context), time_base(time_base) {

    }
    virtual ~BasePlayChannel() {
        if (codecContext) {
            avcodec_close(codecContext);
            avcodec_free_context(&codecContext);
            codecContext = 0;
        }
        pkt_queue.clear();
        frame_queue.clear();
        LOGE("释放Channel: %d %d",pkt_queue.size(), frame_queue.size());
    }

    static void releasePacket(AVPacket *packet) {
        if (packet) {
            av_packet_free(&packet);
            packet = 0;
        }
    }

    static void releaseAvFrame(AVFrame *frame) {
        if (frame) {
            av_frame_free(&frame);
            frame = 0;
        }
    }

    virtual void startPlay() = 0;
    virtual void stopPlay() = 0;

    SafeQueue<AVPacket *> pkt_queue;
    SafeQueue<AVFrame *> frame_queue;
    volatile int channleId;
    volatile bool isPlaying;
    AVCodecContext *codecContext;
    SwrContext *swrContext;
    JavaCallHelper *javaCallHelper;
    AVRational time_base;//时间刻度
    double clock = 0;//用于音视频同步，记录播放时间线，相对时间
};


#endif //LOPNOR_BASEPLAYCHANNEL_H
