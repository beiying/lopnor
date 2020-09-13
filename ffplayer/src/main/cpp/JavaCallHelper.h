//
// Created by beiying on 2020/9/13.
//

#ifndef LOPNOR_JAVACALLHELPER_H
#define LOPNOR_JAVACALLHELPER_H


#include <jni.h>

class JavaCallHelper {
public:
    JavaCallHelper(JavaVM *_javaVM, JNIEnv *_env, jobject &_obj);
    ~JavaCallHelper();
    void onPrepare(int thread);
    void onError(int thread, int errorCode);
    void onProgress(int thread, int progress);

private:
    JavaVM *javaVM;
    JNIEnv *env;
    jobject obj;
    jmethodID jmid_prepare;
    jmethodID jmid_error;
    jmethodID jmid_progress;
};


#endif //LOPNOR_JAVACALLHELPER_H
