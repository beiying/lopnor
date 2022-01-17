//
// Created by dajia on 2022/1/13.
//

#ifndef LOPNOR_VAOSAMPLE_H
#define LOPNOR_VAOSAMPLE_H


#include <ImageDef.h>
#include "GLSampleBase.h"

class VaoSample: public GLSampleBase {
public:
    VaoSample();
    virtual ~VaoSample();

    virtual void LoadImage(NativeImage *pImage);
    virtual void Init();
    virtual void Draw(int screenW, int screenH);
    virtual void Destroy();

private:
    GLuint m_VaoId;
    GLuint m_VboIds[2];

    void InitVbo();
    void InitVao();

    void DrawVbo();
    void DrawVao();
};


#endif //LOPNOR_VAOSAMPLE_H
