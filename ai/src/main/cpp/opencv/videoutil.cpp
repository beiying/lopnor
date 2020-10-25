#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include "opencv2/opencv.hpp"
#include "CascadeDetectorAdapter.h"

using namespace cv;
using namespace std;

//
// Created by beiying on 2020/10/23.
//
ANativeWindow *window = 0;
DetectionBasedTracker *tracker;

extern "C"
JNIEXPORT void JNICALL
Java_com_beiying_ai_VideoAI_initCascade(JNIEnv* env,jobject thiz, jstring _model) {
    const char *model = env->GetStringUTFChars(_model, 0);
    if (tracker) {
        tracker->stop();
        delete tracker;
        tracker = 0;
    }
    //创建跟踪器
    Ptr<CascadeClassifier> classifier = makePtr<CascadeClassifier>(model);
    Ptr<CascadeDetectorAdapter> mainDetector = makePtr<CascadeDetectorAdapter>(classifier);

    Ptr<CascadeClassifier> classifier1 = makePtr<CascadeClassifier>(model);
    Ptr<CascadeDetectorAdapter> trackingDetector = makePtr<CascadeDetectorAdapter>(classifier);

    DetectionBasedTracker::Parameters DetectorParams;
    tracker = new DetectionBasedTracker(mainDetector, trackingDetector, DetectorParams);

    //开启动态跟踪
    tracker->run();

    env->ReleaseStringUTFChars(_model, model);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_beiying_ai_VideoAI_setSurface(JNIEnv *env, jobject thiz, jobject surface) {
    if (window) {
        ANativeWindow_release(window);
        window = 0;
    }

    window = ANativeWindow_fromSurface(env, surface);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_beiying_ai_VideoAI_postData(JNIEnv *env, jobject thiz, jbyteArray _data, jint w, jint h,
                                     jint camera_id) {
    //传进来的是摄像头的NV21数据
    jbyte *data = env->GetByteArrayElements(_data, NULL);
    //NV2数据量：w * h + (w / 2) * (h / 2) + (w / 2) * (h / 2),即w*h*3/2
    Mat src(h + h/2, w, CV_8UC1, data);
    //颜色格式转换 nv21转BGR
    cvtColor(src, src, COLOR_YUV2BGR_NV21);
    //imwrite("/sdcard/src.jpg", src);

    if (camera_id == 1) {//前置摄像头需要逆时针旋转90度
        rotate(src, src, ROTATE_90_COUNTERCLOCKWISE);
        //flip(src, src, 1);//水平镜像
    } else {//后置摄像头
        rotate(src, src, ROTATE_90_CLOCKWISE);
    }
    //imwrite("/sdcard/src.jpg", src);

    Mat gray;
    //转换成灰度图
    cvtColor(src, gray, COLOR_BGR2GRAY);
    //增强对比度
    equalizeHist(gray, gray);

    vector<Rect> faces;
    tracker->process(gray);
    tracker->getObjects(faces);
    for (Rect face: faces) {
        //绘制人脸区域
        rectangle(src, face, Scalar(255, 0, 255));
    }

    if (window) {
        ANativeWindow_setBuffersGeometry(window, w,h, WINDOW_FORMAT_RGBA_8888);
    }

    env->ReleaseByteArrayElements(_data, data, 0);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_beiying_ai_VideoAI_releaseCascade(JNIEnv *env, jobject thiz) {
    if (tracker) {
        tracker->stop();
        delete tracker;
        tracker = 0;
    }
}