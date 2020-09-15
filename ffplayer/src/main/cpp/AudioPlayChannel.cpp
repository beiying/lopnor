//
// Created by beiying on 2020/9/13.
//

#include "AudioPlayChannel.h"

AudioPlayChannel::AudioPlayChannel(int id, JavaCallHelper *javaCallHelper,
                                   AVCodecContext *codecContext, AVRational time_base) : BasePlayChannel(id, javaCallHelper, codecContext, time_base) {
        out_channels = av_get_channel_layout_nb_channels(AV_CH_LAYOUT_STEREO);
        out_samplesize = av_get_bytes_per_sample(AV_SAMPLE_FMT_S16);//16位， 两个字节
        out_sample_rate = 44100;
        buffer = (uint8_t *) malloc(out_sample_rate * out_samplesize * out_channels);
}

AudioPlayChannel::~AudioPlayChannel() {

}

void *audioPlay(void *args) {
    AudioPlayChannel *audioPlayChannel = static_cast<AudioPlayChannel *>(args);
    audioPlayChannel->initOpenSL();
    return 0;//子线程执行完成后一定要返回0，否则会有异常
}

void *audioDecode(void *args) {
    AudioPlayChannel *audioPlayChannel = static_cast<AudioPlayChannel *>(args);
    audioPlayChannel->decodePcm();
    return 0;
}

void AudioPlayChannel::startPlay() {
    //用于将要播放的音频数据转换为播放器能够播放的格式
    swrContext = swr_alloc_set_opts(0, AV_CH_LAYOUT_STEREO, AV_SAMPLE_FMT_S16, out_sample_rate,//音频播放的采样率、声道数和采样位数
            codecContext->channel_layout,//要播放的音频数据的声道
            codecContext->sample_fmt,//要播放的音频数据的采样格式
            codecContext->sample_rate, 0, 0);//要播放的音频数据的采样格式
    swr_init(swrContext);

    pkt_queue.setWork(1);
    frame_queue.setWork(1);
    isPlaying = true;

    //创建OpenSL ES初始化线程
    pthread_create(&pid_audio_play, NULL, audioPlay, this);
    //创建初始化音频解码的线程
    pthread_create(&pid_audio_decode, NULL, audioDecode, this);
}

void AudioPlayChannel::stopPlay() {

}

//向音频播放器的缓冲去队列中存入解码出来的pcm数据
//系统音频播放器会从缓冲队列中取出数据开始播放声音
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context) {
    AudioPlayChannel *audioPlayChannel = static_cast<AudioPlayChannel *>(context);
    int dataSize = audioPlayChannel->getPcm();
    if (dataSize > 0) {
        (*bq)->Enqueue(bq, audioPlayChannel->buffer, dataSize);//将解码出的pcm数据压入播放器的缓冲队列中
    }
}

void AudioPlayChannel::initOpenSL() {
    //音频引擎
    SLEngineItf engineInterface = NULL;
    //音频对象
    SLObjectItf engineObject = NULL;

    //混音器
    SLObjectItf outputMixObject = NULL;

    //播放器
    SLObjectItf bqPlayerObject = NULL;
    //回调接口
    SLPlayItf bqPlayerInterface = NULL;
    //缓冲队列
    SLAndroidSimpleBufferQueueItf  bqPlayerBufferQueue = NULL;

    //1、创建并初始化音频播放引擎
    SLresult result;
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    if (SL_RESULT_SUCCESS != result) {
        return;
    }

    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);//初始化播放器引擎
    if (SL_RESULT_SUCCESS != result) {
        return;
    }

    //音频接口，相当于SurfaceHolder
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineInterface);
    if (SL_RESULT_SUCCESS != result) {
        return;
    }

    //2、创建并初始化混音器
    result = (*engineInterface)->CreateOutputMix(engineInterface, &outputMixObject, 0, 0, 0);
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    if (SL_RESULT_SUCCESS != result) {
        return;
    }
    //3、创建并初始化播放器
    SLDataLocator_AndroidSimpleBufferQueue android_queue = {//音频队列
            SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE,
            2
    };
    //设置pcm数据格式
    SLDataFormat_PCM pcm = {SL_DATAFORMAT_PCM,//播放pcm格式的数据
                            2,//2个声道，立体声
                            SL_SAMPLINGRATE_44_1,//44100hz的频率
                            SL_PCMSAMPLEFORMAT_FIXED_16,//位数，16位
                            SL_PCMSAMPLEFORMAT_FIXED_16,//和位数一直
                            SL_SPEAKER_FRONT_LEFT | SL_SPEAKER_FRONT_RIGHT,//立体声（前左前右）
                            SL_BYTEORDER_LITTLEENDIAN //小端模式
    };
    SLDataLocator_OutputMix outputMix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};//混音器的队列
    SLDataSink audioSnk = {&outputMix, NULL};
    const SLInterfaceID ids[1] = {SL_IID_BUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};

    SLDataSource slDataSource = {&android_queue, &pcm};
    result = (*engineInterface)->CreateAudioPlayer(engineInterface,
            &bqPlayerObject,//播放器
            &slDataSource,//播放器参数：播放缓冲队列类型，播放格式
            &audioSnk,//播放缓冲区
            1,//播放接口回调个数
            ids,//设置播放器队列ID
            req//是否采用内置的播放队列
            );
    if (SL_RESULT_SUCCESS != result) {
        return;
    }
    (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);//初始化播放器

    //4.获取播放器接口和缓冲队列，并设置缓冲队列和回调函数
    (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerInterface);//获取播放器接口
    (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE, &bqPlayerBufferQueue);//获取播放器缓冲区队列
    (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue, bqPlayerCallback, this);//为缓冲区队列注册回调接口，回调接口开始执行后会不停执行该回调

    //5.设置播放状态
    (*bqPlayerInterface)->SetPlayState(bqPlayerInterface, SL_PLAYSTATE_PLAYING);
    //6.启动回调函数,会触发系统不停回调该函数
    bqPlayerCallback(bqPlayerBufferQueue, this);
    LOGE("-----手动调用播放 packet:%d", this->pkt_queue.size());
}

void AudioPlayChannel::decodePcm() {
    AVPacket *packet = 0;
    while(isPlaying) {
        int ret = pkt_queue.get(packet);
        if (!isPlaying) {
            break;
        }
        if (!ret) {
            continue;
        }

        ret = avcodec_send_packet(codecContext, packet);
        releasePacket(packet);
        if (ret == AVERROR(EAGAIN)) {
            continue;
        } else if (ret < 0) {
            break;
        }

        AVFrame *frame = av_frame_alloc();
        ret = avcodec_receive_frame(codecContext, frame);
        if (ret == AVERROR(EAGAIN)) {
            continue;
        } else if (ret < 0) {
            break;
        }
        while(frame_queue.size() > 100 && isPlaying) {
            av_usleep(1000 * 10);
            continue;
        }
        frame_queue.put(frame);
        //解码出来的frame有很多中格式，需要转换成统一的格式交给喇叭使用

    }
}

int AudioPlayChannel::getPcm() {
    AVFrame *frame = 0;
    int data_size = 0;
    while(isPlaying) {
        int ret = frame_queue.get(frame);
        if (!isPlaying) {
            break;
        }
        if (!ret) {
            continue;
        }
        //设置转换参数
        uint64_t dst_nb_samples = av_rescale_rnd(
                swr_get_delay(swrContext, frame->sample_rate) + frame->nb_samples,
                out_sample_rate,
                frame->sample_rate,
                AV_ROUND_UP);
        int nb = swr_convert(swrContext, &buffer, dst_nb_samples, (const uint8_t **)frame->data, frame->nb_samples);//返回采样位数
        data_size = nb * out_channels * out_samplesize;//转换后实际数据大小
        clock = frame->pts * av_q2d(time_base);//设置音频的同步时间，用于音视频同步
        break;
    }
    releaseAvFrame(frame);
    return data_size;
}
