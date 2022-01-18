//
// Created by dajia on 2022/1/13.
//
#include "VaoSample.h"
#include "GLUtils.h"

/**
 * 一、VBO（Vertex Buffer Object）是指顶点缓冲区对象，而 EBO（Element Buffer Object）是指图元索引缓冲区对象，VAO 和 EBO 实际上是对同一类 Buffer 按照用途的不同称呼。
 * VBO和EBO出现的原因：
 *      OpenGLES2.0 编程中，用于绘制的顶点数组数据首先保存在 CPU 内存，在调用 glDrawArrays 或者 glDrawElements 等进行绘制时，需要将顶点数组数据从 CPU 内存拷贝到显存。
 *      但是很多时候我们没必要每次绘制的时候都去进行内存拷贝，如果可以在显存中缓存这些数据，就可以在很大程度上降低内存拷贝带来的开销。OpenGLES3.0 VBO 和 EBO 的出现就是为了解决这个问题。
 *
 * VBO 和 EBO 的作用是在显存中提前开辟好一块内存，用于缓存顶点数据或者图元索引数据，从而避免每次绘制时的 CPU 与 GPU 之间的内存拷贝，可以改进渲染性能，降低内存带宽和功耗。
 *
 * OpenGLES3.0 支持两类缓冲区对象：顶点数组缓冲区对象、图元索引缓冲区对象。GL_ARRAY_BUFFER 标志指定的缓冲区对象用于保存顶点数组，GL_ELEMENT_ARRAY_BUFFER 标志指定的缓存区对象用于保存图元索引。
 *
 * 二、VAO（Vertex Array Object）是指顶点数组对象，VAO 的主要作用是用于管理 VBO 或 EBO ，减少 glBindBuffer 、glEnableVertexAttribArray、 glVertexAttribPointer 这些调用操作，高效地实现在顶点数组配置之间切换。
 *
 * */
#define VERTEX_POS_SIZE       3 // x, y and z
#define VERTEX_COLOR_SIZE     4 // r, g, b, and a

#define VERTEX_POS_INDX       0 //shader layout loc
#define VERTEX_COLOR_INDX     1 //shader layout loc

#define VERTEX_STRIDE         (sizeof(GLfloat)*(VERTEX_POS_SIZE+VERTEX_COLOR_SIZE))

VaoSample::VaoSample() {

}

VaoSample::~VaoSample() {

}

void VaoSample::LoadImage(NativeImage *pImage) {

}

void VaoSample::Init() {
    const char vShaderStr[] =
            "#version 300 es                            \n"
            "layout(location = 0) in vec4 a_position;   \n"
            "layout(location = 1) in vec4 a_color;      \n"
            "out vec4 v_color;                          \n"
            "out vec4 v_position;                       \n"
            "void main()                                \n"
            "{                                          \n"
            "    v_color = a_color;                     \n"
            "    gl_Position = a_position;              \n"
            "    v_position = a_position;               \n"
            "}";

    const char fShaderStr[] =
            "#version 300 es\n"
            "precision mediump float;\n"
            "in vec4 v_color;\n"
            "in vec4 v_position;\n"
            "out vec4 o_fragColor;\n"
            "void main()\n"
            "{\n"
            "    float n = 10.0;\n"
            "    float span = 1.0 / n;\n"
            "    int i = int((v_position.x + 0.5)/span);\n"
            "    int j = int((v_position.y + 0.5)/span);\n"
            "\n"
            "    int grayColor = int(mod(float(i+j), 2.0));\n"
            "    if(grayColor == 1)\n"
            "    {\n"
            "        float luminance = v_color.r*0.299 + v_color.g*0.587 + v_color.b*0.114;\n"
            "        o_fragColor = vec4(vec3(luminance), v_color.a);\n"
            "    }\n"
            "    else\n"
            "    {\n"
            "        o_fragColor = v_color;\n"
            "    }\n"
            "}";

    m_ProgramObj = GLUtils::CreateProgram(vShaderStr, fShaderStr, m_VertexShader, m_FragmentShader);

    InitVbo();

    InitVao();
}

void VaoSample::InitVbo() {
    //结构数组的方式存储每个顶点的属性数据，可以顺序方式读取，提高内存访问模式
    GLfloat vertices[4 *(VERTEX_POS_SIZE + VERTEX_COLOR_SIZE )] = {
            -0.5f,  0.5f, 0.0f,       // v0
            1.0f,  0.0f, 0.0f, 1.0f,  // c0
            -0.5f, -0.5f, 0.0f,       // v1
            0.0f,  1.0f, 0.0f, 1.0f,  // c1
            0.5f, -0.5f, 0.0f,        // v2
            0.0f,  0.0f, 1.0f, 1.0f,  // c2
            0.5f,  0.5f, 0.0f,        // v3
            0.5f,  1.0f, 1.0f, 1.0f,  // c3
    };
    GLushort indices[6] = { 0, 1, 2, 0, 2, 3};
    glGenBuffers(2, m_VboIds);
    glBindBuffer(GL_ARRAY_BUFFER, m_VboIds[0]);
    glBufferData(GL_ARRAY_BUFFER, sizeof(vertices), vertices, GL_STATIC_DRAW);

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_VboIds[1]);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, sizeof(indices), indices, GL_STATIC_DRAW);
}

void VaoSample::InitVao() {
    glGenVertexArrays(1, &m_VaoId);
    glBindVertexArray(m_VaoId);

    glBindBuffer(GL_ARRAY_BUFFER, m_VboIds[0]);
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_VboIds[1]);

    //指定顶点数组中某个顶点的位置属性
    glEnableVertexAttribArray(VERTEX_POS_INDX);
    glVertexAttribPointer(VERTEX_POS_INDX, VERTEX_POS_SIZE, GL_FLOAT, GL_FALSE, VERTEX_STRIDE, (const void*)0);

    //指定顶点数组中某个顶点的颜色属性
    glEnableVertexAttribArray(VERTEX_COLOR_INDX);
    glVertexAttribPointer(VERTEX_COLOR_INDX, VERTEX_COLOR_SIZE, GL_FLOAT, GL_FALSE, VERTEX_STRIDE, (const void *)(VERTEX_POS_SIZE *sizeof(GLfloat)));


    glBindVertexArray(GL_NONE);
}


void VaoSample::Draw(int screenW, int screenH) {
    if (m_ProgramObj == 0) return;
    glUseProgram(m_ProgramObj);

//    DrawVbo();

    DrawVao();
}

void VaoSample::DrawVbo() {
    //指定顶点数组
    glBindBuffer(GL_ARRAY_BUFFER, m_VboIds[0]);
    glEnableVertexAttribArray(VERTEX_POS_INDX);
    glVertexAttribPointer(VERTEX_POS_INDX, VERTEX_POS_SIZE, GL_FLOAT, GL_FALSE, VERTEX_STRIDE, (const void*)0);

    glEnableVertexAttribArray(VERTEX_COLOR_INDX);
    glVertexAttribPointer(VERTEX_COLOR_INDX, VERTEX_COLOR_SIZE, GL_FLOAT, GL_FALSE, VERTEX_STRIDE, (const void *)(VERTEX_POS_SIZE *sizeof(GLfloat)));

    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, m_VboIds[1]);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, (const void*)0);
}

void VaoSample::DrawVao() {
    glBindVertexArray(m_VaoId);
    glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_SHORT, (const void*)0);
    glBindVertexArray(GL_NONE);
}

void VaoSample::Destroy() {
    if (m_ProgramObj) {
        glDeleteProgram(m_ProgramObj);
        glDeleteBuffers(2, m_VboIds);
        glDeleteVertexArrays(1, &m_VaoId);
        m_ProgramObj = GL_NONE;
    }
}