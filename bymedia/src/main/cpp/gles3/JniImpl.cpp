#include "jni.h"
#include "util/LogUtil.h"
#include <GLRenderContext.h>
#include "EGLRender.h"
#define NATIVE_RENDER_CLASS_NAME "com/beiying/media/opengl/render/NativeES3Render"
#define NATIVE_EGL_RENDER_CLASS_NAME "com/beiying/media/opengl/render/NativeEglRender"

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT void JNICALL native_Init(JNIEnv *env, jobject instance) {
    GLRenderContext::GetInstance();
}

JNIEXPORT void JNICALL native_UnInit(JNIEnv *env, jobject instance) {
    GLRenderContext::DestroyInstance();
}

JNIEXPORT void JNICALL native_OnSurfaceCreated(JNIEnv *env, jobject instance) {
    GLRenderContext::GetInstance()->OnSurfaceCreated();
}

JNIEXPORT void JNICALL native_OnSurfaceChanged(JNIEnv *env, jobject instance, jint width, jint height) {
    GLRenderContext::GetInstance()->OnSurfaceChanged(width, height);
}

JNIEXPORT void JNICALL native_OnDrawFrame(JNIEnv *env, jobject instance) {
    GLRenderContext::GetInstance()->OnDrawFrame();
}

JNIEXPORT void JNICALL native_SetImageData(JNIEnv *env, jobject instance,
                                           jint format, jint width, jint height, jbyteArray imageData) {

    int len = env->GetArrayLength(imageData);
    uint8_t *buf = new uint8_t[len];
    env->GetByteArrayRegion(imageData, 0, len, reinterpret_cast<jbyte *>(buf));
    GLRenderContext::GetInstance()->SetImageData(format, width, height, buf);
    delete[] buf;
    env->DeleteLocalRef(imageData);
}

JNIEXPORT void JNICALL native_SetImageDataWithIndex(JNIEnv *env, jobject instance,
                                           jint index, jint format, jint width, jint height, jbyteArray imageData) {
    int len = env->GetArrayLength(imageData);
    uint8_t *buf = new uint8_t[len];
    env->GetByteArrayRegion(imageData, 0, len, reinterpret_cast<jbyte *>(buf));
    GLRenderContext::GetInstance()->SetImageDataWithIndex(index, format, width, height, buf);
    delete[] buf;
    env->DeleteLocalRef(imageData);
}

JNIEXPORT void JNICALL native_SetParamsInt
        (JNIEnv *env, jobject instance, jint paramType, jint value0, jint value1)
{
    GLRenderContext::GetInstance()->SetParamsInt(paramType, value0, value1);
}

JNIEXPORT void JNICALL native_SetParamsFloat
        (JNIEnv *env, jobject instance, jint paramType, jfloat value0, jfloat value1)
{
    GLRenderContext::GetInstance()->SetParamsFloat(paramType, value0, value1);
}

JNIEXPORT void JNICALL native_SetAudioData
        (JNIEnv *env, jobject instance, jshortArray data)
{
    int len = env->GetArrayLength(data);
    short *pShortBuf = new short[len];
    env->GetShortArrayRegion(data, 0, len, reinterpret_cast<jshort*>(pShortBuf));
    GLRenderContext::GetInstance()->SetParamsShortArr(pShortBuf, len);
    delete[] pShortBuf;
    env->DeleteLocalRef(data);
}

JNIEXPORT void JNICALL native_UpdateTransformMatrix(JNIEnv *env, jobject instance,
                                                    jfloat rotateX, jfloat rotateY, jfloat scaleX, jfloat scaleY)
{
    GLRenderContext::GetInstance()->UpdateTransformMatrix(rotateX, rotateY, scaleX, scaleY);
}

JNIEXPORT void JNICALL native_EglRenderInit(JNIEnv *env, jobject instance) {
    EGLRender::GetInstance()->Init();
}

JNIEXPORT void JNICALL native_EglRenderSetImageData(JNIEnv *env, jobject instance, jbyteArray data, jint width, jint height) {
    int len = env->GetArrayLength(data);
    uint8_t* buff = new uint8_t[len];
    env->GetByteArrayRegion(data, 0, len, reinterpret_cast<jbyte*>(buff));
    EGLRender::GetInstance()->SetImageData(buff, width, height);
    delete[] buff;
    env->DeleteLocalRef(data);
}

JNIEXPORT void JNICALL native_EglRenderSetIntParams(JNIEnv *env, jobject instance, jint paramType, jint value) {
    EGLRender::GetInstance()->SetIntParams(paramType, value);
}

JNIEXPORT void JNICALL native_EglRenderDraw(JNIEnv *env, jobject instance) {
    EGLRender::GetInstance()->Draw();
}

JNIEXPORT void JNICALL native_EglRenderUnInit(JNIEnv *env, jobject instance) {
    EGLRender::GetInstance()->UnInit();
}

#ifdef __cplusplus
}
#endif


static JNINativeMethod g_RenderMethods[] = {
        {"native_Init",                      "()V",       (void *)(native_Init)},
        {"native_UnInit",                    "()V",       (void *)(native_UnInit)},
        {"native_SetImageData",              "(III[B)V",  (void *)(native_SetImageData)},
        {"native_SetImageDataWithIndex",     "(IIII[B)V", (void *)(native_SetImageDataWithIndex)},
        {"native_SetParamsInt",              "(III)V",    (void *)(native_SetParamsInt)},
        {"native_SetParamsFloat",            "(IFF)V",    (void *)(native_SetParamsFloat)},
        {"native_SetAudioData",              "([S)V",     (void *)(native_SetAudioData)},
        {"native_UpdateTransformMatrix",     "(FFFF)V",   (void *)(native_UpdateTransformMatrix)},
        {"native_OnSurfaceCreated",          "()V",       (void *)(native_OnSurfaceCreated)},
        {"native_OnSurfaceChanged",          "(II)V",     (void *)(native_OnSurfaceChanged)},
        {"native_OnDrawFrame",               "()V",       (void *)(native_OnDrawFrame)},
};

static JNINativeMethod g_EglBgRenderMethods[] = {
        {"native_EglRenderInit",                            "()V",           (void *)(native_EglRenderInit)},
        {"native_EglRenderSetImageData",                    "([BII)V",       (void *)(native_EglRenderSetImageData)},
        {"native_EglRenderSetIntParams",                    "(II)V",         (void *)(native_EglRenderSetIntParams)},
        {"native_EglRenderDraw",                            "()V",           (void *)(native_EglRenderDraw)},
        {"native_EglRenderUnInit",                          "()V",           (void *)(native_EglRenderUnInit)},
};

static int RegisterNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *methods, int methodNum) {
    LOGCATE("RegisterNativeMethods");
    jclass clazz = env->FindClass(className);
    if (clazz == NULL) {
        LOGCATE("RegisterNativeMethods fail. clazz == NULL");
        return JNI_FALSE;
    }
    if (env->RegisterNatives(clazz, methods, methodNum) < 0) {
        LOGCATE("RegisterNativeMethods fail");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static void UnregisterNativeMethods(JNIEnv *env, const char *className)
{
    LOGCATE("UnregisterNativeMethods");
    jclass clazz = env->FindClass(className);
    if (clazz == NULL)
    {
        LOGCATE("UnregisterNativeMethods fail. clazz == NULL");
        return;
    }
    if (env != NULL)
    {
        env->UnregisterNatives(clazz);
    }
}

extern "C" jint JNI_OnLoad(JavaVM *jvm, void *p) {
    LOGCATE("====== JNI_OnLoad ======");
    jint jniRet = JNI_ERR;
    JNIEnv *env = NULL;
    if (jvm->GetEnv((void **)(&env), JNI_VERSION_1_6) != JNI_OK) {
        return jniRet;
    }
    jint regRet = RegisterNativeMethods(env, NATIVE_RENDER_CLASS_NAME, g_RenderMethods,
                                        sizeof(g_RenderMethods) /sizeof(g_RenderMethods[0]));
    if (regRet != JNI_TRUE)
    {
        return JNI_ERR;
    }

    regRet = RegisterNativeMethods(env, NATIVE_EGL_RENDER_CLASS_NAME, g_EglBgRenderMethods,
                                   sizeof(g_EglBgRenderMethods) / sizeof(g_EglBgRenderMethods[0]));
    if (regRet != JNI_TRUE)
    {
        return JNI_ERR;
    }
    return JNI_VERSION_1_6;
}