//
// Created by dajia on 2022/1/6.
//

#ifndef LOPNOR_NV21TEXTUREMAPSAMPLE_H
#define LOPNOR_NV21TEXTUREMAPSAMPLE_H
#include <GLSampleBase.h>
#include <ImageDef.h>

class NV21TextureMapSample: public GLSampleBase {
public:
    NV21TextureMapSample();
    ~NV21TextureMapSample();

    virtual void LoadImage(NativeImage *pImage);
    virtual void Init();
    virtual void Draw(int screenW, int screenH);
    virtual void Destroy();

private:
    GLuint m_yTextureId;
    GLuint m_uvTextureId;

    GLint  m_ySamplerLoc;
    GLint  m_uvSamplerLoc;

    NativeImage m_RenderImage;
};
#endif //LOPNOR_NV21TEXTUREMAPSAMPLE_H
