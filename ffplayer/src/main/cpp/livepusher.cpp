//
// Created by dajia on 2020/9/7.
//

#include <jni.h>
#include <string>
#include <libavcodec/jni.h>
#include <linux/ptrace.h>
#include <sys/ptrace.h>
#include <pthread.h>
#include "librtmp/rtmp.h"
#include "libsx264/x264.h"
#include "VideoChannel.h"
#include "logger.h"
#include "SafeQueue.h"

extern "C" {
#include <libavcodec/jni.h>
}

#define JNIREG_CLASS "com/beiying/ffplayer/LivePusher"
static JavaVM* javaVm;

VideoChannel *videoChannel;

int isStart;//是否处于推流中
int readyPushing;//是否已经准备好推流

pthread_t pid;//多线程执行推流操作

uint32_t starTime;//用于音视频同步
SafeQueue<RTMPPacket *> packets;//x264将编码后的数据放到队列中以供推流

//编码生成NALU单元后封装到RTMPPacket中后的回调
void videoCallback(RTMPPacket *packet) {
    if (packet) {
        packet->m_nTimeStamp = RTMP_GetTime() - starTime;//设置时间戳
        //添加即将推送的队列中
        packets.put(packet);
    }
}

//释放RTMPPacket
void releasePacket(RTMPPacket *&packet) {
    if (packet) {
        RTMPPacket_Free(packet);
        delete packet;
        packet = 0;
    }
}

void *threadRun(void *args) {
    char *url = static_cast<char *>(args);

    RTMP *rtmp = 0;
    rtmp = RTMP_Alloc();
    if (!rtmp) {
        LOGE("alloc rtmp失败");
        return NULL;
    }
    RTMP_Init(rtmp);
    int ret = RTMP_SetupURL(rtmp, url);
    if (!ret) {
        LOGE("设置地址失败：%s", url);
        return NULL;
    }

    rtmp->Link.timeout = 5;//设置连接超时时间为5s
    RTMP_EnableWrite(rtmp);//设置可写入
    ret = RTMP_Connect(rtmp, 0);//通过socket连接服务器
    if (!ret) {
        LOGE("连接服务器失败：%s", url);
        return NULL;
    }

    ret = RTMP_ConnectStream(rtmp, 0);//连接流
    if (!ret) {
        LOGE("连接流失败：%s", url);
        return NULL;
    }

    starTime = RTMP_GetTime();//获取推流时间，用于音视频同步
    //可以开始推流
    readyPushing = 1;
    packets.setWork(1);
    RTMPPacket *packet = 0;
    while(readyPushing) {//不断从采集并编码后的H264图像队列中取数据
        packets.get(packet);
        LOGE("取出一帧数据");

        if (!readyPushing) {
            break;
        }

        if (!packet) {
            continue;
        }
        packet->m_nInfoField2 = rtmp->m_stream_id;//设置流类型，音频流或视频流
        ret = RTMP_SendPacket(rtmp, packet, 1);//发送数据包，内部也维护一个队列
        releasePacket(packet);
    }
    //直播结束
    isStart = 0;
    readyPushing = 0;
    packets.setWork(0);
    packets.clear();
    if (rtmp) {
        RTMP_Close(rtmp);
        RTMP_Free(rtmp);
    }
    delete (url);
    return 0;
}

JNIEXPORT void JNICALL initLivePusher() {
    videoChannel = new VideoChannel();
    videoChannel->setVideoCallback(videoCallback);
}

JNIEXPORT void JNICALL setVideoEncInfo(JNIEnv *env, jobject context, jint _width, jint _height, jint _fps, jint _bitrate) {
    if (!videoChannel) {
        return;
    }

    videoChannel->setVideoEncInfo(_width, _height, _fps, _bitrate);
}
//在子线程完成推流操作
JNIEXPORT void JNICALL startLivePusher(JNIEnv *env, jobject context, jstring _path) {
    const char *path = env->GetStringUTFChars(_path, 0);
    if (isStart) {
        return;
    }
    isStart = 1;

    char *url = new char[strlen(path) + 1];
    strcpy(url, path);

    pthread_create(&pid, 0, threadRun, url);
}

//开始推流，将摄像头采集的一帧NV21格式的数据转换为I420，并存放到队列中，由rtmpdump完成推流
JNIEXPORT void JNICALL pushVideo(JNIEnv *env, jobject context, jbyteArray _data) {
    jbyte *data = env->GetByteArrayElements(_data, NULL);

    if (!videoChannel || !readyPushing) {
        return;
    }

    videoChannel->encodeData(data);

    env->ReleaseByteArrayElements(_data, data, 0);
}

static int registerNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *jniNativeMethods, int methodsSize) {
    jclass clazz = env->FindClass(className);
    if (clazz == NULL) {
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, jniNativeMethods, methodsSize) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static JNINativeMethod jni_methods_tables[] = {
        {"initLivePusher", "()V", (void *) initLivePusher},
        {"setVideoEncInfo", "(IIII)V", (void *) setVideoEncInfo},
        {"startLivePusher", "(Ljava/lang/String;)V", (void *) startLivePusher},
        {"pushVideo", "([B)V", (void *) pushVideo}
};
// 获取数组的大小
# define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

int register_ndk_onload(JNIEnv *env) {
    return registerNativeMethods(env, JNIREG_CLASS, jni_methods_tables, NELEM(jni_methods_tables));
}

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVm = vm;
//    av_jni_set_java_vm(vm, 0);

    //这是一种比较简单的防止被调试的方案                         1
    // 有更复杂更高明的方案，比如：不用这个ptrace而是每次执行加密解密签先去判断是否被trace
    ptrace(PTRACE_TRACEME, 0, 0, 0);

    JNIEnv *env = NULL;
    jint result = -1;
    if (vm->GetEnv((void **)&env, JNI_VERSION_1_4) != JNI_OK) {
        return result;
    }
    register_ndk_onload(env);
    // 返回jni的版本
    return JNI_VERSION_1_4;
}