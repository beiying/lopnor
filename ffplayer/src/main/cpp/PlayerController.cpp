//
// Created by beiying on 2020/9/13.
//

#include "PlayerController.h"
#include "macro.h"

void *preparePlayer(void *args) {
    PlayerController *controller = static_cast<PlayerController *>(args);
    controller->prepareFFmpeg();
    return 0;
}

void *startDecode(void *args) {
    PlayerController *controller = static_cast<PlayerController *>(args);
    controller->decode();
    return 0;
}

PlayerController::PlayerController(JavaCallHelper *callHelper, const char *dataSource) {
    this->javaCallHelper = callHelper;
    url = new char[strlen(dataSource) + 1];
    strcpy(url, dataSource);
}

PlayerController::~PlayerController() {

}


void PlayerController::prepare() {
    pthread_create(&pid_prepare, NULL, preparePlayer, this);//创建子线程
}


/**
 * 在子线程执行ffmpeg的准备工作
 * */
void PlayerController::prepareFFmpeg() {
    avformat_network_init();

    formatContext = avformat_alloc_context();
    AVDictionary *opts = NULL;
    av_dict_set(&opts, "timeout", "3000000", 0);

    int ret = avformat_open_input(&formatContext, url, NULL, &opts);

    //需要应对打开失败的情况，即ret不为0的情况
    if (ret != 0) {
        javaCallHelper->onError(THREAD_CHILD, FFMPEG_CAN_NOT_OPEN_URL);
        return;
    }

    ret = avformat_find_stream_info(formatContext, NULL);
    if (ret != 0) {
        javaCallHelper->onError(THREAD_CHILD, FFMPEG_CAN_NOT_FIND_STREAMS);
        return;
    }

    for (int i = 0; i < formatContext->nb_streams; i++) {
        AVCodecParameters *codecParameters = formatContext->streams[i]->codecpar;
        AVCodec *dec = avcodec_find_decoder(codecParameters->codec_id);
        if (!dec) {//找不到解码器
            javaCallHelper->onError(THREAD_CHILD, FFMPEG_FIND_DECODER_FAIL);
            return;
        }
        //创建解码器上下文
        AVCodecContext *codecContext = avcodec_alloc_context3(dec);
        if (!codecContext) {
            javaCallHelper->onError(THREAD_CHILD, FFMPEG_ALLOC_CODEC_CONTEXT_FAIL);
            return;
        }
        //复制参数
        ret = avcodec_parameters_to_context(codecContext, codecParameters);
        if (ret != 0) {
            javaCallHelper->onError(THREAD_CHILD, FFMPEG_CODEC_CONTEXT_PARAMETERS_FAIL);
            return;
        }
        //打开解码器
        ret = avcodec_open2(codecContext, dec, 0);
        if (ret != 0) {
            javaCallHelper->onError(THREAD_CHILD, FFMPEG_OPEN_DECODER_FAIL);
            return;
        }

        if (codecParameters->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioPlayChannel = new AudioPlayChannel(i, javaCallHelper,
                                                    codecContext);
        } else if (codecParameters->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoPlayChannel = new VideoPlayChannel(i, javaCallHelper, codecContext);
            videoPlayChannel->setRenderFrame(renderFrame);
        }

    }

    if (!audioPlayChannel && !videoPlayChannel) {
        javaCallHelper->onError(THREAD_CHILD, FFMPEG_NOMEDIA);
        return;
    }
    javaCallHelper->onPrepare(THREAD_CHILD);
    return;
}

//在子线程开始播放
void PlayerController::startPlay() {
    isPlaying = true;
    if (audioPlayChannel) {
        audioPlayChannel->startPlay();
    }
    if (videoPlayChannel) {
        videoPlayChannel->startPlay();
    }

    pthread_create(&pid_play, NULL, startDecode, this);
}

void PlayerController::decode() {
    int ret = -1;
    while (isPlaying) {
        //解码速度远远大于渲染速度，需要控制解码队列
        if (audioPlayChannel && audioPlayChannel->pkt_queue.size() > 100) {
            av_usleep(1000 * 10);
            continue;
        }
        if (videoPlayChannel && videoPlayChannel->pkt_queue.size() > 100) {
            av_usleep(1000 * 10);
            continue;
        }
        AVPacket *packet = av_packet_alloc();
        ret = av_read_frame(formatContext, packet);

        if (ret == 0) {
            if (audioPlayChannel && packet->stream_index == audioPlayChannel->channleId) {
                audioPlayChannel->pkt_queue.put(packet);
            } else if (videoPlayChannel && packet->stream_index == videoPlayChannel->channleId) {
                videoPlayChannel->pkt_queue.put(packet);
            }
        } else if (ret == AVERROR_EOF) {
            if (audioPlayChannel->pkt_queue.empty() && audioPlayChannel->frame_queue.empty() &&
                videoPlayChannel->pkt_queue.empty() && videoPlayChannel->frame_queue.empty()) {
                LOGE("播放完成....");
                break;
            }
        } else {
            break;
        }
    }
    isPlaying = 0;
    audioPlayChannel->stopPlay();
    videoPlayChannel->stopPlay();
}

void PlayerController::setRenderCallback(RenderFrame renderFrame) {
    this->renderFrame = renderFrame;
}
