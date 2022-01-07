//
// Created by dajia on 2022/1/7.
//

#ifndef LOPNOR_GLUTILS_H
#define LOPNOR_GLUTILS_H
#include <GLES3/gl3.h>

class GLUtils {
public:
    static GLuint LoadShader(GLenum shaderType, const char *pSource);

    static GLuint CreateProgram(const char *pVertexShaderSource, const char *pFragShaderSource,
                                GLuint &vertexShaderHandle, GLuint &fragShaderHandle);

    static void DeleteProgram(GLuint &program);
    static void CheckGLError(const char *pGLOperation);
};


#endif //LOPNOR_GLUTILS_H
