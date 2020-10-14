#include <jni.h>
#include <linux/ptrace.h>
#include <sys/ptrace.h>
#include <android/bitmap.h>
#include <malloc.h>

extern "C" {
#include "libjpeg/jpeglib.h"
#include "libjpeg/turbojpeg.h"
}

//
// Created by beiying on 2020/10/11.
//

static JavaVM *javaVM;
static jobject classLoader;


//压缩图片到文件中
private void write_jpeg_file(uint8_t* data,  int w, int h, int quality, const char *path) {
    //创建JPEG压缩对象
    jpeg_compress_struct  jcs;
    //创建设置回调
    jpeg_error_mgr error;
    jcs.err = jpeg_std_error(&error);
    //创建压缩对象
    jpeg_create_compress(&jcs);
    //指定图片的存储文件
    FILE *f = fopen(path, "wb");
    jpeg_stdio_dest(&jcs, f);
    //设置压缩参数
    jcs.image_width = w;
    jcs.image_height = h;

    jcs.input_components = 3;
    jcs.in_color_space = JCS_RGB;
    jpeg_set_defaults(&jcs);

    jcs.optimize_coding = true;//开启哈夫曼压缩
    jpeg_set_quality(&jcs, quality, 1);

    //开始压缩
    jpeg_start_compress(&jcs, 1);
    //循环写入每一行数据
    int row_stride = w * 3;//一行的字节数
    JSAMPROW row[1];
    while(jcs.next_scanline < jcs.image_height) {
        uint8_t  *pixels = data + jcs.next_scanline * row_stride;
        row[0] = pixels;
        jpeg_write_scanlines(&jcs, row, 1);
    }

    //结束压缩
    jpeg_finish_compress(&jcs);
    fclose(f);
    //释放jpeg对象
    jpeg_destroy_compress(&jcs);
}

JNIEXPORT void JNICALL turboCompress(JNIEnv *env, jobject context, jobject _bitmap, jint _quality) {
    AndroidBitmapInfo info;
    AndroidBitmap_getInfo(env, _bitmap, &info);

    uint8_t *pixels;
    AndroidBitmap_lockPixels(env, _bitmap, (void **)&pixels);
    int w = info.width;
    int h = info.height;
    int color;
    //申请用来存取rgb信息
    uint8_t *data = (uint8_t *)malloc(w * h * 3);
    uint8_t *temp = data;
    uint_t r,g,b;

    for (int i = 0; i< h;i++) {
        for (int j = 0;j < w; j++) {
            color = *(int *)pixels;//将一个像素点中的ARGB值存储在一个4字节的int类型变量中
            r = (color >> 16) & 0xFF;
            g = (color >> 8) & 0xFF;
            b = color & 0xFF;
            *data = b;
            *(data + 1) = g;
            *(data + 2) = r;
            data+=3;
            pixels+=4;
        }
    }
    write_jpeg_file(temp, w, h, _quality, "path");
    AndroidBitmap_unlockPixels(env, _bitmap);
    free(data);

}

static int registerNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *gMethods,
                                 int numMethods) {
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
#define JNIREG_CLASS "com/beiying/media/jpeg/Turbo"

// 获取数组的大小
# define NELEM(x) ((int) (sizeof(x) / sizeof((x)[0])))

static JNINativeMethod jni_Methods_table[] = {
        {"turboCompress",          "(Ljava/lang/String;)V",                   (void *) turboCompress},
};

int register_ndk_onload(JNIEnv *env) {
    return registerNativeMethods(env, JNIREG_CLASS, jni_Methods_table, NELEM(jni_Methods_table));
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    javaVM = vm;
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
