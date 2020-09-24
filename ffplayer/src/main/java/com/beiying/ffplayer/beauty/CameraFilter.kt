package com.beiying.ffplayer.beauty

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import com.beiying.ffplayer.R
import javax.microedition.khronos.opengles.GL

/**
 * 获取摄像头数据的滤镜，会创建FBO，并在FBO中添加特效
 * 如果使用后置摄像头，需要先将采集到的摄像头图像逆时针旋转90度，然后进行镜像处理
 */
class CameraFilter(context: Context) : AbstractFilter(context, R.raw.camera_vertex, R.raw.camera_frag) {
    var frameBuffer: IntArray = IntArray(1)
    var frameBufferTexture: IntArray = IntArray(1)
    var matrix: FloatArray? = null
    override fun initCoordinate() {
        textureBuffer.clear()
//        val TEXTURE: FloatArray = floatArrayOf(
//            0.0f, 0.0f,//左上顶点
//            1.0f, 0.0f,//右上顶点
//            0.0f, 1.0f,//左下顶点
//            1.0f, 1.0f //右下顶点
//        )
        //需要旋转90度
        val TEXTURE: FloatArray = floatArrayOf(
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
        )

        textureBuffer.put(TEXTURE)
    }

    override fun onSurfaceReady(width: Int, height: Int) {
        super.onSurfaceReady(width, height)
        //生成FBO
        GLES20.glGenBuffers(1, frameBuffer, 0)

        //生成一个纹理，并将纹理与FBO绑定，对纹理的操作就是对FBO的操作
        GLES20.glGenTextures(1, frameBufferTexture, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[0])
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mWidth, mHeight, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameBufferTexture[0], 0)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    fun glGenTextures(textures: IntArray) {
        GLES20.glGenTextures(textures.size, textures, 0)
        for (id in textures) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)

            //纹理环绕方向, GL_TEXTURE_WRAP_S表示x方向，GL_TEXTURE_WRAP_T表示y方向
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)//相当于解绑
        }
    }

    override fun onDrawFrame(textureId: Int): Int {
        //设置显示窗口
        GLES20.glViewport(0, 0, mWidth, mHeight)
        //将纹理绘制到FBO中，而不是屏幕上，后续的操作都是基于FBO实现的
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
        //使用着色器
        GLES20.glUseProgram(mProgram)
        //将Java层的顶点坐标绑定到顶点着色器程序中的坐标变量,并激活对应的变量
        vertexBuffer.position(0)
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(vPosition)

        //先将纹理坐标传到顶点着色器，在OpenGL中传递给片元着色器
        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glEnableVertexAttribArray(vCoord)

        //设置摄像头的矩阵
        GLES20.glUniformMatrix4fv(vMatrix, 1, false, matrix, 0)

        //激活采样器，并将SurfaceTexture与OpenGL中的vTexture绑定，才能将摄像头的图像数据传给OpenGL中采样器
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,textureId)//绘制的帧数据是摄像头的原始数据，所以采样器要用扩展采样器

        //重置vTexture
        GLES20.glUniform1i(vTexture, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        return frameBufferTexture[0]
    }

}