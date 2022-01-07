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