package com.beiying.media.opengl

import android.content.Context
import android.opengl.GLES20
/**
 * OpenGL绘制流程，要考虑三个问题：
 *      1、在什么地方进行绘制？
 *      2、绘制成什么形状？
 *      3、用什么颜色来绘制？
 * OpenGL坐标、基本图元、渲染管线、内存拷贝、顶点着色器、光栅化技术、片段着色器
 *
 * 渲染管线处理过程：
 *      读取顶点数据——执行顶点着色器——组装图元——光栅化图元——执行片段着色器——写入帧缓冲区——显示屏幕上
 *
 * 编译OpenGL程序：
 *      1、编译着色器
 *      2、创建OpenGL程序和着色器链接
 *      3、验证OpenGL程序
 *      4、确定使用OpenGL程序
 *
 * 创建一个 OpenGL 程序的通用步骤
 * */
class ShaderHelper {
    companion object {
        fun compileVertexShader(shaderCode: String): Int {
            return compileFragmentShader(shaderCode)
        }

        fun compileFragmentShader(shaderCode: String): Int {
            return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode)
        }

        /**
         * 通过glCreateShader方法创建了着色器 ID，然后通过glShaderSource连接上着色器程序内容，
         * 接下来通过glCompileShader编译着色器，最后通过glGetShaderiv验证是否失败。
         * */
        fun compileShader(type: Int, shaderCode: String): Int {
            // 根据不同的类型创建着色器ID
            val shaderObjectId: Int = GLES20.glCreateShader(type)
            if (shaderObjectId == 0) {
                return 0
            }
            //将着色器ID和着色器程序内容链接
            GLES20.glShaderSource(shaderObjectId, shaderCode)
            //在GPU中编译着色器
            GLES20.glCompileShader(shaderObjectId)

            val compileStatus = IntArray(1)
            //验证编译结果是否失败
            GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] == 0) {
                GLES20.glDeleteShader(shaderObjectId)
                return 0
            }
            return shaderObjectId
        }

        /**
         * 首先通过glCreateProgram程序创建 OpenGL 程序，然后通过glAttachShader将着色器程序 ID 添加上 OpenGL 程序，
         * 接下来通过glLinkProgram链接 OpenGL 程序，最后通过glGetProgramiv来验证链接是否失败。
         * */
        fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
            //创建OpenGL程序Id，用于统一管理顶点着色器和片元着色器
            val programObjectId: Int = GLES20.glCreateProgram()
            if (programObjectId == 0) {
                return 0
            }
            //链接上顶点着色器
            GLES20.glAttachShader(programObjectId, vertexShaderId)
            //链接上片段着色器
            GLES20.glAttachShader(programObjectId, fragmentShaderId)
            //链接着色器后，链接OpenGL程序
            GLES20.glLinkProgram(programObjectId)

            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS, linkStatus, 0)
            if (linkStatus[0] == 0) {
                GLES20.glDeleteProgram(programObjectId)
                return 0
            }
            return programObjectId
        }

        /**
         * 通过glValidateProgram函数验证，并再次通过glGetProgramiv函数验证是否失败。
         * */
        fun validateProgram(programObjectId: Int): Boolean {
            GLES20.glValidateProgram(programObjectId)
            val validateStatus = IntArray(1)
            GLES20.glGetProgramiv(programObjectId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0)
            return validateStatus[0] != 0
        }

        fun buildPrograme(context: Context, vertexShaderSource: Int, fragmentShaderSource:Int):Int {
            var program: Int
            val vertexShader = compileVertexShader(TextResourceReader.readTextFileFromResource(context, vertexShaderSource))
            var fragmentShader = compileFragmentShader(TextResourceReader.readTextFileFromResource(context, fragmentShaderSource))
            program = linkProgram(vertexShader, fragmentShader)
            validateProgram(program)
            return program
        }
    }
}