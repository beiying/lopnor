//
// Created by beiying on 2020/9/13.
//


#include "VideoPlayChannel.h"


VideoPlayChannel::VideoPlayChannel(int id, JavaCallHelper *callHelper, AVCodecContext *context)
        : BasePlayChannel(id, callHelper, context) {
    this->javaCallHelper = callHelper;
    this->codecContext = context;
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
        av_usleep(16 * 1000000);
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

