//
// Created by dajia on 2022/1/6.
//

#ifndef LOPNOR_TEXTUREMAPSAMPLE_H
#define LOPNOR_TEXTUREMAPSAMPLE_H
#include "GLSampleBase.h"
#include "ImageDef.h"

class TextureMapSample: public GLSampleBase {
public:
    TextureMapSample();
    virtual ~TextureMapSample();

    void LoadImage(NativeImage *pImage);

    virtual void Init();

    virtual void Draw(int screenW, int screenH);

    virtual void Destroy();

private:
    GLuint m_TextureId;
    GLint m_SamplerLoc;
    NativeImage m_RenderImage;
};
#endif //LOPNOR_TEXTUREMAPSAMPLE_H
