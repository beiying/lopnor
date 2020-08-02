//
// Created by beiying on 2019-07-21.
//

#ifndef TALKMOMENTAPK_JNIENVHELPER_H
#define TALKMOMENTAPK_JNIENVHELPER_H


#include "jni.h"
#include "stdint.h"

class JNIEnvHelper {
public:
    JNIEnv *env;
    JavaVM *javaVM;
    JNIEnvHelper() {
        needDetach = false;
        if (javaVM->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
            //如果当前Native层运行的线程是Native层创建，那么Java层并不知道该线程的存在，需要绑定JVM才能拿到JNIEnv;
            if (javaVM->AttachCurrentThread(&env, NULL) == JNI_OK) {
                needDetach = true;
            }
        }
    }
    ~JNIEnvHelper() {
        //如果是Native线程，只有解绑是才会清理期间创建的JVM对象；
        if(needDetach) javaVM->DetachCurrentThread();
    }
private:
    bool needDetach;
};


#endif //TALKMOMENTAPK_JNIENVHELPER_H
