//
// Created by dajia on 2022/1/19.
//

#ifndef LOPNOR_COORDSYSTEMSAMPLE_H
#define LOPNOR_COORDSYSTEMSAMPLE_H


#include "GLSampleBase.h"
#include "detail/type_mat.hpp"
#include "detail/type_mat4x4.hpp"

class CoordSystemSample: public GLSampleBase {
public:
    CoordSystemSample();

    virtual ~CoordSystemSample();

    virtual void LoadImage(NativeImage *pImage);

    virtual void Init();
    virtual void Draw(int screenW, int screenH);

    virtual void Destroy();

    virtual void UpdateTransformMatrix(float rotateX, float rotateY, float scaleX, float scaleY);

    void UpdateMVPMatrix(glm::mat4 &mvpMatrix, int angleX, int angleY, float ratio);
private:
    GLuint m_TextureId;
    GLint m_SamplerLoc;
    GLint m_MVPMatLoc;
    GLuint m_VaoId;
    GLuint m_VboIds[3];
    NativeImage m_RenderImage;
    glm::mat4 m_MVPMatrix;

    int m_AngleX;
    int m_AngleY;
    float m_ScaleX;
    float m_ScaleY;
};


#endif //LOPNOR_COORDSYSTEMSAMPLE_H
