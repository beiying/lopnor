//
// Created by dajia on 2022/1/7.
//

#ifndef LOPNOR_TRIANGLESAMPLE_H
#define LOPNOR_TRIANGLESAMPLE_H
#include "GLSampleBase.h"
class TriangleSample: public GLSampleBase {
public:
    TriangleSample();
    virtual ~TriangleSample();

    virtual void LoadImage(NativeImage *pImage);

    virtual void Init();

    virtual void Draw(int screenW, int screenH);

    virtual void Destroy();

private:
    GLfloat vVertices[9] = {
            0.0f, 0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f
    };
};
#endif //LOPNOR_TRIANGLESAMPLE_H
