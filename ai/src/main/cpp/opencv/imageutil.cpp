#include <jni.h>
#include <string>
#include <android/bitmap.h>
#include <android/log.h>
#include "opencv2/opencv.hpp"

#define TAG "JNI_TAG";
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)
//
// Created by beiying on 2020/10/17.
//

using namespace cv;
void bitmapToMat(JNIEnv *env, Mat &mat, jobject bitmap);

void mat2Bitmap(JNIEnv *env, Mat &mat, jobject bitmap);
CascadeClassifier cascadeClassifier;


extern "C"
JNIEXPORT void JNICALL
Java_com_beiying_ai_ImageAI_faceDetectionSaveInfo(JNIEnv *env, jobject thiz, jobject _bitmap) {
    //将Bitmap转换为opencv可以操作的C++对象矩阵mat
    Mat mat;
    bitmapToMat(env, mat, _bitmap);

    //使用OpenCV先做灰度处理，减少数据量，提高性能
    Mat gray_mat;
    cvtColor(mat, gray_mat, COLOR_BGRA2GRAY);

    //进行直方均衡补偿，继续减少图片数据量，方便计算
    Mat equalize_mat;
    equalizeHist(gray_mat, equalize_mat);
    //开始人脸识别，需要加载人脸分类器文件
    std::vector<Rect> faces;
    cascadeClassifier.detectMultiScale(equalize_mat, faces, 1.1, 5);
    if (faces.size() == 1) {
        //获取人脸信息，是一块矩形区域，可以把矩形区域抠出来传给Bitmap
        Rect faceRect = faces[0];
        rectangle(mat, faceRect, Scalar(255, 155, 155), 4);
        mat2Bitmap(env, mat, _bitmap);

        Mat face_info_mat(equalize_mat, faceRect);
    }

    //把处理后的图放回Bitmap中
    mat2Bitmap(env, equalize_mat, _bitmap);
}

/**
 * 将bitmap转换为mat
 * */
void bitmapToMat(JNIEnv *env, Mat &mat, jobject bitmap) {
    //Mat的type：CV_8UC4对应ARGB_8888, CV_8UV2对应RGB_565
    //获取Bitmap的信息
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env,bitmap,&info);
    //设置转换后mat的宽高和type
    mat.create(info.height, info.width, CV_8UC4);
    //获取Bitmap像素数据
    void *pixls;
    AndroidBitmap_lockPixels(env, bitmap, &pixls);
    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        Mat temp(info.height, info.width, CV_8UC4, pixls);
        temp.copyTo(mat);
    } else if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
        Mat temp(info.height, info.width, CV_8UC2, pixls);
        //将CV_8UC2类型的像素数据转换为CV_8UV4
        cvtColor(temp, mat, COLOR_BGR5652BGRA);
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}

/**
 * 将mat转换为Bitmap
 * */
void mat2Bitmap(JNIEnv *env, Mat &mat, jobject bitmap) {
    //获取Bitmap的信息
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env,bitmap,&info);
    //获取Bitmap像素数据
    void *pixls;
    AndroidBitmap_lockPixels(env, bitmap, &pixls);
    if (info.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        Mat temp(info.height, info.width, CV_8UC4, pixls);
        if (mat.type() == CV_8UC4) {
            mat.copyTo(temp);
        } else if (mat.type() == CV_8UC2) {
            cvtColor(mat, temp, COLOR_BGR5652BGRA);
        } else if (mat.type() == CV_8UC1) {
            cvtColor(mat, temp, COLOR_GRAY2BGRA);
        }
    } else if (info.format == ANDROID_BITMAP_FORMAT_RGB_565) {
        Mat temp(info.height, info.width, CV_8UC2, pixls);
        if (mat.type() == CV_8UC4) {
            cvtColor(mat, temp, COLOR_BGRA2BGR565);
        } else if (mat.type() == CV_8UC2) {
            mat.copyTo(temp);
        } else if (mat.type() == CV_8UC1) {
            cvtColor(mat, temp, COLOR_GRAY2BGR565);
        }
    }

    AndroidBitmap_unlockPixels(env, bitmap);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_beiying_ai_ImageAI_loadCascade(JNIEnv *env, jobject thiz, jstring _path) {
    const char* path = env->GetStringUTFChars(_path, 0);

    cascadeClassifier.load(path);

    env->ReleaseStringUTFChars(_path, path);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_beiying_ai_ImageAI_tranformGray(JNIEnv *env, jobject thiz, jobject bitmap) {
    AndroidBitmapInfo bitmapInfo;
    AndroidBitmap_getInfo(env, bitmap, &bitmapInfo);
    void *pixels;
    AndroidBitmap_lockPixels(env, bitmap, &pixels);
    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        for (int i = 0;i < bitmapInfo.width * bitmapInfo.height;i++) {
            uint32_t *pixel_p = reinterpret_cast<uint32_t *>(pixels) + i;
            uint32_t pixel = *pixel_p;

            int a = (pixel >> 24) && 0xFF;
            int r = (pixel >> 16) && 0xFF;
            int g = (pixel >> 8) && 0xFF;
            int b = pixel && 0xFF;

            int gray = (int) (0.213f * r + 0.715f*g + 0.072f * b);
            *pixel_p = (a << 24) | (gray << 16) | (gray << 8) | gray;
        }
    } else if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGB_565) {
        for (int i = 0;i < bitmapInfo.width * bitmapInfo.height;i++) {
            uint16_t *pixel_p = reinterpret_cast<uint16_t *>(pixels) + i;
            uint16_t pixel = *pixel_p;

            int r = ((pixel >> 11) && 0x1F) << 3;
            int g = ((pixel >> 5) && 0x3F) << 2;
            int b = pixel && 0x1F;

            int gray = (int) (0.213f * r + 0.715f*g + 0.072f * b);
            *pixel_p = ((gray >>3) << 11) | ((gray >> 2) << 5) | gray;
        }
    }


    AndroidBitmap_unlockPixels(env, bitmap);
}