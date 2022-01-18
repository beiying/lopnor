//
// Created by dajia on 2022/1/18.
//

#ifndef LOPNOR_FBOSAMPLE_H
#define LOPNOR_FBOSAMPLE_H

#include "GLSampleBase.h"

class FBOSample: public GLSampleBase {
public:
    FBOSample();
    virtual ~FBOSample();

    virtual void LoadImage(NativeImage *pImage);
    virtual void Init();
    virtual void Draw(int screenW, int screenH);
    virtual void Destroy();
    bool CreateFBO();

private:
    GLuint m_ImageTextureId;
    GLuint m_FboTextureId;//纹理Id
    GLuint m_FboId;//FBO Id
    GLuint m_VaoIds[2];
    GLuint m_VboIds[4];
    GLuint m_SamplerLoc;
    NativeImage m_RenderImage;
    GLuint m_FboProgramObj;
    GLuint m_FboVertexShader;
    GLuint m_FboFragmentShader;
    GLuint m_FboSamplerLoc;
};


#endif //LOPNOR_FBOSAMPLE_H
