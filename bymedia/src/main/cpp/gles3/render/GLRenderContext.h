//
// Created by dajia on 2022/1/6.
//

#ifndef LOPNOR_GLRENDERCONTEXT_H
#define LOPNOR_GLRENDERCONTEXT_H
#include "stdint.h"
#include <GLES3/gl3.h>
#include <TextureMapSample.h>
#include <NV21TextureMapSample.h>
#include <TriangleSample.h>

class GLRenderContext {
    GLRenderContext();
    ~GLRenderContext();

public:
    void SetImageData(int format, int width, int height, uint8_t *pData);

    void SetImageDataWithIndex(int index, int format, int width, int height, uint8_t *pData);

    void SetParamsInt(int paramType, int value0, int value1);

    void SetParamsFloat(int paramType, float value0, float value1);

    void SetParamsShortArr(short *const pShortArr, int arrSize);

    void UpdateTransformMatrix(float rotateX, float rotateY, float scaleX, float scaleY);

    void OnSurfaceCreated();

    void OnSurfaceChanged(int width, int height);

    void OnDrawFrame();

    static GLRenderContext* GetInstance();

    static void DestroyInstance();

private:
    static GLRenderContext *m_pContext;
    GLSampleBase *m_pBeforeSample;
    GLSampleBase *m_pCurSample;
    int m_ScreenW;
    int m_ScreenH;
};
#endif //LOPNOR_GLRENDERCONTEXT_H

