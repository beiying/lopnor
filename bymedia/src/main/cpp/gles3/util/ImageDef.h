//
// Created by dajia on 2022/1/6.
//

#ifndef LOPNOR_IMAGEDEF_H
#define LOPNOR_IMAGEDEF_H
#include <malloc.h>
#include <string.h>
#include <unistd.h>
#include "stdio.h"
#include "sys/stat.h"
#include "stdint.h"
#include "LogUtil.h"

typedef struct NativeRectF {
    float left;
    float top;
    float right;
    float bottom;
    NativeRectF() {
        left = top = right = bottom = 0.0f;
    }
} RectF;

struct SizeF {
    float width;
    float height;
    SizeF() {
        width = height = 0.0f;
    }
};

struct NativeImage{
    int width;
    int height;
    int format;
    uint8_t *ppPlane[3];
    NativeImage() {
        width = height = format = 0;
        ppPlane[0] = nullptr;
        ppPlane[1] = nullptr;
        ppPlane[2] = nullptr;
    }
};
#endif //LOPNOR_IMAGEDEF_H
