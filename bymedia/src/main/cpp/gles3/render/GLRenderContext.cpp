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

}

void GLRenderContext::SetImageDataWithIndex(int index, int format, int width, int height,
                                            uint8_t *pData) {

}

void GLRenderContext::SetParamsInt(int paramType, int value0, int value1) {

}

void GLRenderContext::SetParamsFloat(int paramType, float value0, float value1) {

}

void GLRenderContext::SetParamsShortArr(short *const pShortArr, int arrSize) {

}

void
GLRenderContext::UpdateTransformMatrix(float rotateX, float rotateY, float scaleX, float scaleY) {

}
