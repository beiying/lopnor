//
// Created by dajia on 2022/1/7.
//

#include "GLRenderContext.h"
GLRenderContext::GLRenderContext() {
    m_pCurSample = new TriangleSample();
    m_pBeforeSample = nullptr;
}

GLRenderContext::~GLRenderContext() {
    if (m_pCurSample) {
        delete m_pCurSample;
        m_pCurSample = nullptr;
    }
    if (m_pBeforeSample) {
        delete m_pBeforeSample;
        m_pBeforeSample = nullptr;
    }
}
GLRenderContext* GLRenderContext::m_pContext = nullptr;
GLRenderContext *GLRenderContext::GetInstance() {
    if (m_pContext == nullptr) {
        m_pContext = new GLRenderContext();
    }
    return m_pContext;
}
void GLRenderContext::DestroyInstance() {
    if (m_pContext) {
        delete m_pContext;
        m_pContext = nullptr;
    }
}

void GLRenderContext::OnSurfaceCreated() {
    glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
}

void GLRenderContext::OnSurfaceChanged(int width, int height) {
    glViewport(0, 0, width, height);
    m_ScreenW = width;
    m_ScreenH = height;
}

void GLRenderContext::OnDrawFrame() {
    LOGCATE("GLRenderContext::OnDrawFrame");
    glClear(GL_DEPTH_BUFFER_BIT | GL_COLOR_BUFFER_BIT);

    if (m_pBeforeSample) {
        m_pBeforeSample->Destroy();
        delete m_pBeforeSample;
        m_pBeforeSample = nullptr;
    }
    if (m_pCurSample) {
        m_pCurSample->Init();
        m_pCurSample->Draw(m_ScreenW, m_ScreenH);
    }
}


void GLRenderContext::SetImageData(int format, int width, int height, uint8_t *pData) {
    NativeImage nativeImage;
    nativeImage.format = format;
    nativeImage.width = width;
    nativeImage.height = height;
    nativeImage.ppPlane[0] = pData;
    switch (format) {
        case IMAGE_FORMAT_NV12:
        case IMAGE_FORMAT_NV21:
            nativeImage.ppPlane[1] = nativeImage.ppPlane[0] + width * height;
            break;
        case IMAGE_FORMAT_I420:
            nativeImage.ppPlane[1] = nativeImage.ppPlane[0] + width * height;
            nativeImage.ppPlane[2] = nativeImage.ppPlane[1] + width * height / 4;
            break;
    }
    if (m_pCurSample) {
        m_pCurSample->LoadImage(&nativeImage);
    }
}

void GLRenderContext::SetImageDataWithIndex(int index, int format, int width, int height,
                                            uint8_t *pData) {

}

void GLRenderContext::SetParamsInt(int paramType, int value0, int value1) {
    LOGCATE("设置Int类型参数 paramType = %d, value0 = %d, value1 = %d", paramType, value0, value1);
    if (paramType == SAMPLE_TYPE) {
        m_pBeforeSample = m_pCurSample;
        LOGCATE("GLRenderContext::SetParamsInt 0 m_pBeforeSample = %p", m_pBeforeSample);
        switch (value0) {
            case SAMPLE_TYPE_KEY_TRIANGLE:
                m_pCurSample = new TriangleSample();
                break;
            case SAMPLE_TYPE_KEY_TEXTURE_MAP:
                m_pCurSample = new TextureMapSample();
                break;
            default:
                m_pCurSample = nullptr;
                break;
        }
    }
    LOGCATE("GLRenderContext::SetParamsInt m_pBeforeSample = %p, m_pCurSample=%p", m_pBeforeSample, m_pCurSample);
}

void GLRenderContext::SetParamsFloat(int paramType, float value0, float value1) {

}

void GLRenderContext::SetParamsShortArr(short *const pShortArr, int arrSize) {

}

void
GLRenderContext::UpdateTransformMatrix(float rotateX, float rotateY, float scaleX, float scaleY) {

}
