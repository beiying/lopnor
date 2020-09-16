//
// Created by beiying on 2020/9/13.
//


#include "VideoPlayChannel.h"

void dropPacket(queue<AVPacket *> &q) {
    while(!q.empty()) {
        LOGE("丢弃视频-------");
        AVPacket *packet = q.front();
        if (packet->flags != AV_PKT_FLAG_KEY) {
            q.pop();
            BasePlayChannel::releasePacket(packet);
        } else {
            continue;
        }
    }
}

void dropFrame(queue<AVFrame *> &q) {
    while(!q.empty()) {
        LOGE("丢弃视频帧数据-------");
        AVFrame *frame = q.front();
        q.pop();
        BasePlayChannel::releaseAvFrame(frame);
    }
}

VideoPlayChannel::VideoPlayChannel(int id, JavaCallHelper *callHelper, AVCodecContext *context, AVRational time_base)
        : BasePlayChannel(id, callHelper, context, time_base) {
    this->javaCallHelper = callHelper;
    this->codecContext = context;
    frame_queue.setReleaseHandle(releaseAvFrame);
    frame_queue.setSyncHandle(dropFrame);
}

VideoPlayChannel::~VideoPlayChannel() {

}

void *decode(void *args) {
    VideoPlayChannel *videoPlayChannel = static_cast<VideoPlayChannel *>(args);
    videoPlayChannel->decodePacket();
    return 0;
}

void *realPlay(void *args) {
    VideoPlayChannel *videoPlayChannel = static_cast<VideoPlayChannel *>(args);
    videoPlayChannel->playContent();
    return 0;
}

void VideoPlayChannel::startPlay() {
    pkt_queue.setWork(1);
    frame_queue.setWork(1);
    isPlaying = true;

    pthread_create(&pid_video_play, NULL, decode, this);
    pthread_create(&pid_synchronize, NULL, realPlay, this);
}

void VideoPlayChannel::stopPlay() {
    isPlaying = false;
}

//解码线程执行过程
void VideoPlayChannel::decodePacket() {
    AVPacket *packet = 0;
    while (isPlaying) {
        int ret = pkt_queue.get(packet);
        if (!isPlaying) {
            break;
        }
        if (!ret) {
            continue;
        }
        ret = avcodec_send_packet(codecContext, packet);
        releasePacket(packet);
        if (ret == AVERROR(EAGAIN)) {//需要更多数据
            continue;
        } else if (ret < 0) {
            break;
        }
        AVFrame *frame = av_frame_alloc();
        ret = avcodec_receive_frame(codecContext, frame);
        frame_queue.put(frame);
        while (frame_queue.size() > 100 && isPlaying) {
            av_usleep(1000 * 10);
            continue;
        }
    }
    releasePacket(packet);
}

//渲染线程的执行过程
void VideoPlayChannel::playContent() {
    //将解码出来的YUV数据转换成SurfaceView可以渲染显示的RGBA数据
    SwsContext *swsContext = sws_getContext(codecContext->width, codecContext->height,
                                            codecContext->pix_fmt,
                                            codecContext->width, codecContext->height,
                                            AV_PIX_FMT_RGBA, SWS_BILINEAR, 0, 0, 0);
    uint8_t *dst_data[4];
    int dst_linesize[4];
    av_image_alloc(dst_data, dst_linesize, codecContext->width, codecContext->height, AV_PIX_FMT_RGBA, 1);
    AVFrame *frame = 0;
    while (isPlaying) {
        int ret = frame_queue.get(frame);
        if (!isPlaying) {
            break;
        }
        if (!ret) {
            continue;
        }
        sws_scale(swsContext, reinterpret_cast<const uint8_t *const *>(frame->data), frame->linesize, 0, frame->height, dst_data, dst_linesize);
        //渲染回调
        renderFrame(dst_data[0], dst_linesize[0],codecContext->width, codecContext->height);
        LOGE("解码一帧视频 %d", frame_queue.size());

        clock = frame->pts * av_q2d(time_base);//视频的同步时间，需要跟音频的同步时间对比
        double extra_delay = frame->repeat_pict / (2 * fps);//视频的画面的显示要考虑解码时间，通过repeate_pict计算额外的解码时间
        double frame_delay = 1.0 / fps; //根据视频的帧率计算帧与帧之间的间隔时间
        double delay = extra_delay + frame_delay;//计算出每一帧之间真正的间隔时间，需要考虑解码时间
        double audioClock = audioPlayChannel->clock;//单位都是毫秒
        double diffClock = clock - audioClock;
        LOGE("--------相差--------- %d", diffClock);
        //TODO，这里的音视频同步有待研究
        if (clock > audioClock) {//视频播放超前了，帧与帧之间间隔时间适当延长
            if (diffClock > 1) {//如果视频过于超前，延迟时间更长
                av_usleep((delay * 2) * 1000);
            } else {
                av_usleep((delay + diffClock) * 1000);
            }
        } else {//视频延后， 音频超前
            if (diffClock > 1) {//视频落后比较多
                //不延迟，直接继续播放下一帧
            } else if (diffClock >= 0.05) {//视频落后不算太多，采取丢帧的操作
                releaseAvFrame(frame);
                frame_queue.sync();
            }
        }
        releaseAvFrame(frame);
    }
    av_freep(&dst_data[0]);
    isPlaying = false;
    releaseAvFrame(frame);
    sws_freeContext(swsContext);

}

void VideoPlayChannel::setRenderFrame(RenderFrame renderFrame) {
    this->renderFrame = renderFrame;
}

void VideoPlayChannel::setFps(int fps) {
    this->fps = fps;
}

