//
// Created by dajia on 2022/1/19.
//

#ifndef LOPNOR_EGLSHADERS_H
#define LOPNOR_EGLSHADERS_H
class EGLShaders {
public:
    //顶点着色程序
    static const char vShaderStr[];
    //普通渲染片段着色器
    static const char fShaderStr0[];
    // 马赛克
    static const char fShaderStr1[];

    // 网格
    static const char fShaderStr2[];

    // 旋转
    static const char fShaderStr3[];

    // 边缘
    static const char fShaderStr4[];
    // 放大
    static const char fShaderStr5[];

    static const char fShaderStr6[];

    // 形变
    static const char fShaderStr7[];
};
#endif //LOPNOR_EGLSHADERS_H
