//
// Created by beiying on 2019/5/3.
//

#include <jni.h>
#include <string.h>
#include <sys/ptrace.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <__locale>
#include <unistd.h>

// 在C++环境下使用C函数的时候，常常会出现编译器无法找到obj模块中的C函数定义，
// 从而导致链接失败的情况，应该如何解决这种情况呢？
//
// 答案与分析：
// C++语言在编译的时候为了解决函数的多态问题，会将函数名和参数联合起来生成一个中间的函数名称，
// 而C语言则不会，因此会造成链接时找不到对应函数的情况，此时C函数就需要用extern “C”进行链接指定，
// 这告诉编译器，请保持我的名称，不要给我生成用于链接的中间函数名。
extern "C" {
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libavutil/frame.h>
#include <libavdevice/avdevice.h>
#include <libavfilter/avfilter.h>
#include <libavutil/log.h>
#include <libavcodec/jni.h>
#include <libswscale/swscale.h>

#include "libswresample/swresample.h"
#include "libavutil/opt.h"
#include "libavutil/imgutils.h"
#include <android/native_window.h>
#include <android/native_window_jni.h>
}

#include "logger.h"
#include "JNIEnvHelper.h"

//class JNIEnvHelper {
//        public:
//        JNIEnv *env;
//        JNIEnvHelper() {
//            needDetach = false;
//            if (javaVM)
//        }
//        private:
//        bool needDetach;
//};
#define MAX_AUDIO_FRME_SIZE 48000 * 4
static jobject classLoader;


//确保时间计量单位的分母不出现0的情况
static double r2d(AVRational r) {
    return r.num == 0 || r.den == 0 ? 0 : (double) r.num / (double) r.den;
}

//当前时间戳
long long GetNowMs() {
    struct timeval tv;
    gettimeofday(&tv, NULL);
    int sec = tv.tv_sec%360000;
    long long t = sec * 1000 + tv.tv_usec/1000;
    return t;
}


//jint setUpClassLoader(JNIEnv* env) {
//
//}

jclass findClass(JNIEnv* env, const char* name) {
    if (env == NULL) return NULL;
    jclass classLoaderClass = (*env).GetObjectClass(classLoader);//Native线程如果没绑定JVM，是无法查找到相应的class的
    jmethodID loadClassMethod = (*env).GetMethodID(classLoaderClass, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");
    jclass cls = static_cast<jclass>((*env).CallObjectMethod(classLoader, loadClassMethod, (*env).NewStringUTF(name)));
    return cls;

}

static SLObjectItf  engineSL = NULL;//引擎对象
SLEngineItf createSL() {//创建SL引擎
    SLresult  re;
    SLEngineItf en;
    re = slCreateEngine(&engineSL, 0,0,0,0,0);
    if (re != SL_RESULT_SUCCESS) return NULL;
    re = (*engineSL)->Realize(engineSL, SL_BOOLEAN_FALSE);
    if (re != SL_RESULT_SUCCESS) return NULL;
    re = (*engineSL)->GetInterface(engineSL, SL_IID_ENGINE, &en);
    if (re != SL_RESULT_SUCCESS) return NULL;
    return en;
}

//音频播放的缓冲回调
void PcmCall(SLAndroidSimpleBufferQueueItf bf, const char *path) {
    LOGI("PcmCall");
    static FILE *fp = NULL;
    static char *buf = NULL;
    if (!buf) {
        buf = new char[1024*1024];
    }
    if (!fp) {
        fp = fopen(path, "rb");
    }
    if (!fp) return;
    if (feof(fp) == 0) {
        int len = fread(buf, 1, 1024, fp);
        if(len > 0) {
            (*bf)->Enqueue(bf, buf, len);
        }
    }
}

//static struct sigaction old_signalhandlers[NSIG];
static JavaVM* javaVM;
//static void android_signal_handler(int signum, siginfo_t *info, void *reserved) {
//    if (javaVM == NULL) {
//        JNIEnvHelper jniEnvHelper;
//        jclass errHandleClass = findClass(jniEnvHelper.env, "com/beiyng/core/HandlerNativeError");
//    }
//    old_signalhandlers[signum].sa_handler(signum);
//}
//
//void setUpGlobalSignalHandler() {
//    struct sigaction handler;
//    memset(&handler, 0, sizeof(struct sigaction));
//    handler.sa_sigaction = android_signal_hanlder;
//    handler.sa_flags = SA_RESETHAND;
//#define CATCHSIG(X) sigaction(X, &handler, &old_signalhandlers[X])//给信号设置一个新的异常处理函数
//    CATCHSIG(SIGQUIT);
//    CATCHSIG(SIGILL);
//    CATCHSIG(SIGABRT);
//    CATCHSIG(SIGBUS);
//    CATCHSIG(SIGFPE);
//    CATCHSIG(SIGSEGV);
//    CATCHSIG(SIGPIPE);
//    CATCHSIG(SIGTERM);
//#undef CATCHSIG
//}

// 获取数组的大小
# define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))


JNIEXPORT jstring JNICALL urlProtocolInfo(JNIEnv *env,jobject context) {
}


JNIEXPORT jstring JNICALL avFormatInfo(JNIEnv *env, jobject context) {

}

JNIEXPORT jstring JNICALL avCodecInfo(JNIEnv *env, jobject context) {

}

JNIEXPORT jstring JNICALL avFilterInfo(JNIEnv *env, jobject context) {

}

JNIEXPORT void JNICALL testPlayVideo(JNIEnv *env, jobject context, jstring videoPath_, jobject surface) {
    const char *videoPath = env->GetStringUTFChars(videoPath_, 0);
    //初始化解封装
    av_register_all();

    //初始化编解码器
    avcodec_register_all();

    //初始化网络
    avformat_network_init();

    //初始化AVFormatContext
    AVFormatContext* formatContext = avformat_alloc_context();//非so包初始化AVFormatContext，需要自己手动释放内存空间


    //打开要解封装的文件
    int ret = avformat_open_input(&formatContext, videoPath, NULL, NULL);
    if (ret != 0) {
        LOGE("解封装，打开视频失败: %s", av_err2str(ret));
        avformat_close_input(&formatContext);
        return;
    } else {
        LOGI("解封装，成功打开视频 duration = %lld, nb_streams=%d", formatContext->duration, formatContext->nb_streams);
    }

    //获取流信息，对于flv格式可以用于获取时长信息
    ret = avformat_find_stream_info(formatContext, NULL);
    if (ret != 0) {
        LOGE("获取流信息失败: %s", av_err2str(ret));
        avformat_close_input(&formatContext);
        return;
    } else {
        LOGI("获取流信息成功 duration = %lld, nb_streams=%d", formatContext->duration, formatContext->nb_streams);
    }

    //遍历每个媒体流
    int videoStreamIndex = 0;
    int audioStreamIndex = 1;
    for (int i = 0;i < formatContext->nb_streams;i++) {
        AVStream* stream = formatContext->streams[i];
        if (stream->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            videoStreamIndex = i;
        } else if (stream->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audioStreamIndex = i;
        }
    }


    //软解码视频
    AVCodec* vAvCodec = avcodec_find_decoder(formatContext->streams[videoStreamIndex]->codecpar->codec_id);
    AVCodec* aAvCodec = avcodec_find_decoder(formatContext->streams[audioStreamIndex]->codecpar->codec_id);
//    AVCodec* vAvCodec = avcodec_find_decoder_by_name("h264_mediacodec");//硬解码，需要Java虚拟机环境JavaVM

    if (vAvCodec != 0) {
        LOGE("avcodec find failed");
    }
    //视频解码器初始化
    AVCodecContext* avCodecContext = avcodec_alloc_context3(vAvCodec);
    AVCodecContext* audioAvCodecContext = avcodec_alloc_context3(aAvCodec);
    avcodec_parameters_to_context(avCodecContext, formatContext->streams[videoStreamIndex]->codecpar);
    avcodec_parameters_to_context(audioAvCodecContext, formatContext->streams[audioStreamIndex]->codecpar);
    avCodecContext->thread_count = 1;//单线程解码视频
    audioAvCodecContext->thread_count = 1;//单线程解码音频
    //avCodecContext->thread_count = 8;//多线程解码

    //打开视频解码器
    ret = avcodec_open2(avCodecContext, 0, NULL);
    if(ret != 0) {
        LOGE("avcodec_open2 failed");
    }

    //使用音频解码器,参考视频解码器
    // todo

    //在堆上申请空间，完成初始化，需要释放
    AVPacket* avPacket = av_packet_alloc();
    AVFrame* avFrame = av_frame_alloc();

    //初始化视频图像像素格式转换的上下文
    SwsContext *swsContext = NULL;
    int outWidth = 1280;
    int outHeight = 720;
    char *rgb = new char[1920*1080*4];

    //初始化音频重采样上下文
    SwrContext *swrContext = swr_alloc();
    swrContext = swr_alloc_set_opts(swrContext, av_get_default_channel_layout(2), AV_SAMPLE_FMT_S16,
            avCodecContext->sample_rate, av_get_default_channel_layout(avCodecContext->channels),
            avCodecContext->sample_fmt,avCodecContext->sample_rate,0, 0);
    ret = swr_init(swrContext);
    if (ret != 0) {
        LOGE("swr_init failed");
    } else {
        LOGI("swr_init success");
    }
    char *pcm = new char[48000*4*2];

    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface);
    //设置native window的buffer大小，可自动拉伸
    ANativeWindow_setBuffersGeometry(nativeWindow, outWidth, outWidth, WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer windowBuffer;
    ANativeWindow_lock(nativeWindow, &windowBuffer, 0);
    uint8_t *dst = (uint8_t *)windowBuffer.bits;
    memcpy(dst, rgb, outWidth * outHeight * 4);
    ANativeWindow_unlockAndPost(nativeWindow);

    long long start = GetNowMs();
    int frameCount = 0;
    for (;;) {
        if(GetNowMs() - start >= 3000) {
            LOGI("now decode fps is %d", frameCount / 3);
            start = GetNowMs();
            frameCount = 0;
        }
        ret = av_read_frame(formatContext, avPacket);
        if (ret != 0) {
            LOGI("读取到结尾处");
            av_seek_frame(formatContext, videoStreamIndex, formatContext->duration / 2, AVSEEK_FLAG_BACKWARD|AVSEEK_FLAG_FRAME);
            continue;
        }
        LOGI("获取AVPacket stream=%d,size=%d,pts=%lld,flag=%d", avPacket->stream_index, avPacket->size, avPacket->pts, avPacket->flags);

        AVCodecContext *cc = avCodecContext;
        if (avPacket->stream_index == audioStreamIndex) {
            cc = audioAvCodecContext;
        }
        //发送到线程中解码
        ret = avcodec_send_packet(cc, avPacket);
        if (ret != 0) {
            LOGI("avcodec_send_packet failed");
        }

        for(;;) {
            ret = avcodec_receive_frame(cc, avFrame);
            if (ret != 0) {
                LOGI("avcodec_receive_frame failed");
                break;
            }
            LOGI("avcodec_receive_frame frame=%lld", avFrame->pts);
            if (cc == avCodecContext) {//如果是视频帧，统计帧数
                frameCount++;
                //考虑到图像格式可能在解码之前未解析出来，而无法获取宽高的情况，最后将该方法放在解码后调用
                swsContext = sws_getCachedContext(swsContext, avFrame->width, avFrame->height,
                                                  (AVPixelFormat)avFrame->format, outWidth, outHeight,
                                                  AV_PIX_FMT_RGBA, SWS_FAST_BILINEAR, 0, 0, 0);
                if(!swsContext) {
                    LOGE("sws_context get failed");
                } else {
                    uint8_t  *data[AV_NUM_DATA_POINTERS] = {0};
                    data[0] = (uint8_t *)rgb;
                    int line[AV_NUM_DATA_POINTERS] = {0};
                    line[0] = outWidth * 4;
                    //视频帧解码后进行像素格式和尺寸转换
                    int h = sws_scale(swsContext, avFrame->data, avFrame->linesize, 0, avFrame->height, data, line);
                    LOGI("sws_scale= %d", h);
                    if (h > 0) {
                        ANativeWindow_lock(nativeWindow, &windowBuffer, 0);
                        uint8_t  *dst = (uint8_t *)windowBuffer.bits;
                        memcpy(dst, rgb, outWidth * outHeight * 4);
                        ANativeWindow_unlockAndPost(nativeWindow);
                    }
                }
            }
            if (cc == audioAvCodecContext) {//处理音频
                uint8_t *out[2] = {0};
                out[0] = (uint8_t *)pcm;
                int len = swr_convert(swrContext,out,
                                    avFrame->nb_samples,
                                    (const uint8_t **)(avFrame->data),
                                    avFrame->nb_samples);
                LOGI("swr_convert = %d", len);
            }
        }


        av_packet_unref(avPacket);
    }
    delete rgb;
    delete pcm;
    avformat_close_input(&formatContext);



}

JNIEXPORT void JNICALL playVideo(JNIEnv *env, jobject context, jstring videoPath_, jobject surface_) {
    const char *videoPath = env->GetStringUTFChars(videoPath_, 0);
    LOGE("%s", videoPath);
    if (videoPath == NULL) {
        LOGE("视频路径为空");
        av_log_set_level(AV_LOG_INFO);
        return;
    }
    //初始化解封装
//    av_register_all();
//
////    //初始化编解码器
//    avcodec_register_all();
////
////    //初始化网络
    avformat_network_init();

    AVFormatContext* formatContext = avformat_alloc_context();
    AVDictionary *opts = NULL;
    av_dict_set(&opts, "timeout", "3000000", 0);
    //打开要解封装的视频文件
    int ret = avformat_open_input(&formatContext, videoPath, NULL, &opts);
    if (ret != 0) {
        LOGE("解复用，打开视频失败%s", av_err2str(ret));
        return;
    }

    //获取流信息
    if (avformat_find_stream_info(formatContext, NULL) < 0) {
        LOGE("获取流信息失败");
        return ;
    }

    int video_stream_index = -1;
    LOGE("开始查找视频流");
    for (int i = 0;i < formatContext->nb_streams;i++) {
        if (formatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_VIDEO) {
            video_stream_index = i;
        }
    }

    if (video_stream_index == -1) {
        LOGE("没有视频流");
        return;
    }

    LOGE("查找并获取解码器");
    AVCodecParameters* codecParameters = formatContext->streams[video_stream_index]->codecpar;
    AVCodec* codec = avcodec_find_decoder(codecParameters->codec_id);
    if (codec == NULL) {
        LOGE("解码器没找到");
        return;
    }

    //根据获取到的解码器获取解码器上下文
    AVCodecContext* avCodecContext = avcodec_alloc_context3(codec);
    if (avCodecContext == NULL) {
        LOGE("创建avCodecContext失败");
        return;
    }

    // 解码器参数复制到解码器上下文中
    if (avcodec_parameters_to_context(avCodecContext, codecParameters) < 0) {
        LOGE("avcodec_parameters_to_context失败");
        return;
    }

    // 打开解码器
    if(avcodec_open2(avCodecContext, codec, NULL)) {
        LOGE("打开解码器失败");
        return ;
    }
    LOGE("打开解码器成功");

    enum AVPixelFormat  dstFormat = AV_PIX_FMT_RGBA;


    //解码前的数据封装在AVPacket中，从视频流中读取出AVPacket需要经过三个步骤：
    // 1、av_packet_alloc
    // 2、av_read_frame
    // 3、avcodec_send_packet
    AVPacket* packet = av_packet_alloc();
    if (packet == NULL) {
        return;
    }

    //视频转换器上下文
    SwsContext* swsContext = sws_getContext(avCodecContext->width, avCodecContext->height, avCodecContext->pix_fmt,//转换前的格式和宽高
                                        avCodecContext->width, avCodecContext->height, AV_PIX_FMT_RGBA, //转换后的格式和宽高
                                        SWS_BILINEAR, //转换方式：重视速度、重视质量、质量锐度等
                                        0, 0, 0);


    ANativeWindow *nativeWindow = ANativeWindow_fromSurface(env, surface_);
    ANativeWindow_setBuffersGeometry(nativeWindow, avCodecContext->width, avCodecContext->height, WINDOW_FORMAT_RGBA_8888);
    ANativeWindow_Buffer windowBuffer;
    LOGE("开始从视频流中读取数据包");
    // 从视频流中读取数据包
    while(av_read_frame(formatContext, packet) >= 0) {
        //将要解码的数据AVPacket发送到FFmpeg的解码队列中，如果解码队列满了会发送失败，需要执行avcodec_receive_frame
        avcodec_send_packet(avCodecContext, packet);
        //解码后的YUV数据封装在AVFrame中,
        AVFrame* frame = av_frame_alloc();
        //从解码成功的解码队列中取出一帧，如果没有可以解码的数据AVPacket，该方法就会报错，需要调用avcodec_send_packet发送要解码的数据
        ret = avcodec_receive_frame(avCodecContext, frame);
        LOGE("解码一帧数据");
        if (ret == AVERROR(EAGAIN)) {
            continue;
        } else if(ret < 0) {
            break;
        }

        /** 开始绘制，解码出来的是YUV数据，需要转换成RGBA数据，通过YUV和RGBA的换算公式对每一帧数据转换会比较麻烦，FFmpeg提供了转换上下文SwsContext，
         *  可以用来将YUV格式转换为RGBA格式，其中的swscale方法提供了视频原始数据（YUV420，YUV422，YUV444，RGB24...）之间的转换，分辨率变换等操作。
         *      1）将一帧的YUV数据转化为RGBA数据，RGBA数据需要有个容器用于加载存储，RGBA数据用二维数组存放，RGBA各自对应一个一维数组
         **/

        //
        uint8_t *dst_data[0];//指向R、G、B、A四个一维数组的指针，即包含4个指针的数组
        //RGBA每个通道的内存对齐的步长，即一行的对齐内存的宽度，此值大小等于图像宽度
        int dst_linesize[0];

        //申请一帧RGBA图像所占的内存，并填充一帧空的RGBA数据，对于1920 * 1080的视频，RGBA的每个通道各自对应一个1920 * 1080长度的uint8_t类型的数值，每个通道的步长是1080
        av_image_alloc(dst_data,dst_linesize, avCodecContext->width, avCodecContext->height, AV_PIX_FMT_RGBA, 1);

        LOGE("linesize0 %d", dst_data[0]);
        if (packet->stream_index == video_stream_index) {
//开始绘制
            //将AVFrame中的YUV数据转换为RGBA格式的数据
            sws_scale(swsContext,frame->data, //,输入图像YUV的每个颜色通道的数据指针
                      frame->linesize,//YUV数据每个通道的内存对齐的步长
                      0,
                      frame->height,
                      dst_data, // 转换后的RGBA数据
                      dst_linesize);
            //将RGBA数据渲染到SurfaceView上，底层绘制都是通过缓冲区绘制的，缓冲区是一个字节数组，对应大小是屏幕高度乘以宽度，绘制的时候是一行一行的复制RGBA数据到缓冲区的
            //绘制时，为防止多线程冲突，对缓冲区要加锁
            ANativeWindow_lock(nativeWindow, &windowBuffer, 0);
            LOGE("----linesize0 %d", dst_linesize[0]);
            //一行一行的绘制
            uint8_t *firstWindow = static_cast<uint8_t *>(windowBuffer.bits);
            uint8_t *src_data = dst_data[0];
            int destStride = windowBuffer.stride * 4;
            int src_linesize = dst_linesize[0];
            for(int i = 0; i < windowBuffer.height;i++) {
                //通过内存拷贝的方式进行渲染
                memcpy(firstWindow + i * destStride, src_data + i * src_linesize, destStride);//通过内存拷贝
            }
            LOGE("----linesize0 %d", dst_linesize[0]);
            //绘制完后要解锁
            ANativeWindow_unlockAndPost(nativeWindow);
            usleep(1000* 16);
            av_frame_free(&frame);
        }


    }

    ANativeWindow_release(nativeWindow);
    avcodec_close(avCodecContext);
    avformat_free_context(formatContext);
    env->ReleaseStringUTFChars(videoPath_, videoPath);

};

JNIEXPORT void JNICALL sound(JNIEnv *env, jobject context, jstring audioPath_, jstring outputPath_) {
    const char *input = env->GetStringUTFChars(audioPath_, 0);
    const char *output = env->GetStringUTFChars(outputPath_, 0);

    avformat_network_init();

    AVFormatContext *formatContext = avformat_alloc_context();
    if (avformat_open_input(&formatContext, input, NULL, NULL) != 0) {
        LOGE("无法打开音频数据");
        return;
    }

    if(avformat_find_stream_info(formatContext, NULL) < 0) {
        LOGE("无法获取输入文件信息");
        return;
    }

    int audio_stream_idx = -1;
    for (int i = 0;i < formatContext->nb_streams; i++) {
        if (formatContext->streams[i]->codecpar->codec_type == AVMEDIA_TYPE_AUDIO) {
            audio_stream_idx = i;
            break;
        }
    }
    //获取解码器参数信息
    AVCodecParameters *codecpar = formatContext->streams[audio_stream_idx]->codecpar;
    //获取解码器
    AVCodec *avCodec = avcodec_find_decoder(codecpar->codec_id);
    //根据解码器获取解码器上下文
    AVCodecContext *codecContext = avcodec_alloc_context3(avCodec);
    //根据解码器参数设置解码器上下文
    avcodec_parameters_to_context(codecContext, codecpar);


    AVPacket *avPacket = av_packet_alloc();
    //音频转换器上下文
    SwrContext *swrContext = swr_alloc();

    //输入的音频参数
    AVSampleFormat  in_sample = codecContext->sample_fmt;//输入的采样格式位数
    int in_sample_rate = codecContext->sample_rate;//输入的采样频率
    uint64_t in_ch_layout = codecContext->channel_layout;//输入的声道布局
    //输出的音频参数
    AVSampleFormat  out_sample = AV_SAMPLE_FMT_S16;
    int out_sample_rate = 44100;
    uint64_t  out_ch_layout = AV_CH_LAYOUT_STEREO;
    //设置音频转换器上下文的采样参数
    swr_alloc_set_opts(swrContext, out_ch_layout, out_sample, out_sample_rate, in_ch_layout, in_sample, in_sample_rate, 0, NULL);
    //初始化音频转换器上下文的其他参数
    swr_init(swrContext);
    uint8_t *out_buffer = (uint8_t *)(av_malloc(2 * 44100));//设置输出缓冲区大小等于声道数*采样频率
    FILE *fp_pcm = fopen(output, "wb");

    int ret = -1;
    int count= 0;
    while(av_read_frame(formatContext, avPacket) >= 0) {//从音频流中读取每一帧的压缩数据
        avcodec_send_packet(codecContext, avPacket);//将读取到一帧压缩数据压入到解码器的解码队列中
        AVFrame *frame = av_frame_alloc();
        ret = avcodec_receive_frame(codecContext, frame);//将解码器解码队列中的压缩数据转换位原始的未压缩数据，实现解码
        if (ret == AVERROR(EAGAIN)) {
            continue;
        } else if(ret < 0) {
            LOGE("完成解码");
            break;
        }
        if (avPacket->stream_index != audio_stream_idx) {
            continue;
        }
        LOGE("正在解码%d" ,count++);
        //为了统一编码格式（采样率等），需要将原始数据进行转换，生成可以设备可以识别的编码格式
        //将解码后的数据转换到缓冲区，以备输出
        swr_convert(swrContext, &out_buffer, 2*44100,
                    (const uint8_t **)(frame->data), frame->nb_samples);
        //由于每一帧的数据大小不一定相同，所以读到缓冲区的数据大小也不同，需要获取每一帧数据在缓冲区的实际大小（与声道数、采样频率、采样位数有关），实现数据对齐
        int out_buffer_size = av_samples_get_buffer_size(NULL, av_get_channel_layout_nb_channels(out_ch_layout), frame->nb_samples, out_sample, 0);
        fwrite(out_buffer, 1, out_buffer_size, fp_pcm);
    }

    fclose(fp_pcm);
    av_free(out_buffer);
    swr_free(&swrContext);
    avcodec_close(codecContext);
    avformat_close_input(&formatContext);


}

JNIEXPORT void JNICALL testPlayAudio(JNIEnv *env, jobject context, jstring audioPath_) {
    SLEngineItf engine = createSL();
    if (engine) {
        LOGI("CreateSL Success");
    } else {
        LOGE("CreateSL Failed");
    }

    //2、创建混音器
    SLObjectItf mix = NULL;
    SLresult re = (*engine)->CreateOutputMix(engine, &mix, 0,0,0);
    if (re != SL_RESULT_SUCCESS) {
        LOGI("CreateOutputMix Failed");
    }
    re = (*mix)->Realize(mix, SL_BOOLEAN_FALSE);
    if (re != SL_RESULT_SUCCESS) {
        LOGI("Mix Realize Failed");
    }
    SLDataLocator_OutputMix outmix = {SL_DATALOCATOR_OUTPUTMIX, mix};
    SLDataSink audioSink = {&outmix, 0};

    //3、配置音频信息

    //
    SLDataLocator_AndroidSimpleBufferQueue queue = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 10};
    SLDataFormat_PCM pcm = {
            SL_DATAFORMAT_PCM,
            2,
            SL_SAMPLINGRATE_44_1,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_PCMSAMPLEFORMAT_FIXED_16,
            SL_SPEAKER_FRONT_LEFT|SL_SPEAKER_FRONT_RIGHT,
            SL_BYTEORDER_LITTLEENDIAN//字节序
    };
    SLDataSource ds = {&queue, &pcm};

    //4、创建播放器
    SLObjectItf player = NULL;
    SLPlayItf  iplayer = NULL;
    SLAndroidSimpleBufferQueueItf  pcmQueue = NULL;
    const SLInterfaceID  ids[] = {SL_IID_BUFFERQUEUE};
    const SLboolean  req[] = {SL_BOOLEAN_TRUE};
    re = (*engine)->CreateAudioPlayer(engine, &player, &ds, &audioSink,
            sizeof(ids) / sizeof(SLInterfaceID),ids,req);
    if (re != SL_RESULT_SUCCESS) {
        LOGE("CreateAudioPlayer Failed");
    } else {
        LOGI("CreateAudioPlayer Success");
    }

    (*player)->Realize(player, SL_BOOLEAN_FALSE);
    re = (*player)->GetInterface(player, SL_IID_PLAY, &iplayer);//获取播放器接口
    if (re != SL_RESULT_SUCCESS) {

    }

    re = (*player)->GetInterface(player, SL_IID_BUFFERQUEUE, &pcmQueue);
    if (re != SL_RESULT_SUCCESS) {
        LOGE("GetInterface SL_IID_BUFFERQUEUE Failed");
    }

    //设置回调函数，播放队列空时调用
//    (*pcmQueue)->RegisterCallback(pcmQueue, PcmCall, 0);
    (*iplayer)->SetPlayState(iplayer, SL_PLAYSTATE_PLAYING);
    //启动队列回调
    (*pcmQueue)->Enqueue(pcmQueue, "", 1);

}

static int registerNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *gMethods, int numMethods) {
    jclass clazz;
    clazz = env->FindClass(className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, gMethods, numMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
};

// 指定要注册的类，对应完整的java类名
#define JNIREG_CLASS "com/beiying/ffplayer/FFPlayer"

static JNINativeMethod jni_Methods_table[] = {
//        {"urlProtocolInfo", "(V;)Ljava/lang/String;", (void *) urlProtocolInfo},
//        {"avFormatInfo", "(V;)Ljava/lang/String;", (void *) avFormatInfo},
//        {"avCodecInfo", "(V;)Ljava/lang/String;", (void *) avCodecInfo},
//        {"avFilterInfo", "(V;)Ljava/lang/String;", (void *) avFilterInfo},
        {"playVideo", "(Ljava/lang/String;Ljava/lang/Object;)V", (void *) playVideo},
        {"sound", "(Ljava/lang/String;Ljava/lang/String;)V", (void *) sound}
//        {"testPlayVideo", "(Ljava/lang/String;Ljava/lang/Object;)V", (void *) testPlayVideo}
};

int register_ndk_onload(JNIEnv *env) {
    return registerNativeMethods(env, JNIREG_CLASS, jni_Methods_table, NELEM(jni_Methods_table));
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
    av_jni_set_java_vm(vm, 0);//设置硬解码的JavaVM
    //这是一种比较简单的防止被调试的方案
    // 有更复杂更高明的方案，比如：不用这个ptrace而是每次执行加密解密签先去判断是否被trace
    ptrace(PTRACE_TRACEME, 0, 0, 0);

    JNIEnv *env = NULL;
    jint result = -1;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return result;
    }
    register_ndk_onload(env);
    // 返回jni的版本
    return JNI_VERSION_1_4;
}