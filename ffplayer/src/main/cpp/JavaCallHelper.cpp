//
// Created by beiying on 2020/9/13.
//

#include "JavaCallHelper.h"
#include "macro.h"

JavaCallHelper::JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_obj) {
    this->javaVM = _javaVM;
    this->env = _env;
    obj = env->NewGlobalRef(_obj);

    jclass jclazz = env->GetObjectClass(obj);
    jmid_prepare = env->GetMethodID(jclazz, "onPrepare", "()V");
    jmid_error = env->GetMethodID(jclazz, "onError", "(I)V");
    jmid_progress = env->GetMethodID(jclazz, "onProgress","(I)V");
}

JavaCallHelper::~JavaCallHelper() {

}

void JavaCallHelper::onPrepare(int thread) {

}

void JavaCallHelper::onError(int thread, int errorCode) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;//子线程的JNIEnv
        //子线程要绑定JavaVM获取对应的JNIEnv
        if (javaVM->AttachCurrentThread(&env,0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(obj, jmid_error, errorCode);
        javaVM->DetachCurrentThread();
    } else if (thread == THREAD_MAIN) {
        env->CallVoidMethod(obj, jmid_error, errorCode);
    }
}

void JavaCallHelper::onProgress(int thread, int progress) {
    if (thread == THREAD_CHILD) {
        JNIEnv *jniEnv;//子线程的JNIEnv
        //子线程要绑定JavaVM获取对应的JNIEnv
        if (javaVM->AttachCurrentThread(&env,0) != JNI_OK) {
            return;
        }
        jniEnv->CallVoidMethod(obj, jmid_progress, progress);
        javaVM->DetachCurrentThread();
    } else if (thread == THREAD_MAIN) {
        env->CallVoidMethod(obj, jmid_progress, progress);
    }
}
