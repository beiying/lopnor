package com.beiying.ffplayer.beauty

import android.content.Context
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

/**
 * 滤镜的抽象类，实现了：
 * 滤镜的初始化
 * 图像的绘制
 * */
abstract class AbstractFilter(context: Context, val vertexShaderId: Int, val fragmentShaderId: Int) {
    lateinit var textureBuffer: FloatBuffer //片元着色器的顶点坐标缓冲区
    private lateinit var vertexBuffer: FloatBuffer //顶点着色器的顶点坐标缓冲区
    var mProgram: Int = 0
    var vTexture: Int = 0  //纹理Id
    var vMatrix: Int = 0   //
    var vCoord: Int = 0  //
    var vPosition: Int = 0
    var mWidth: Int = 0;
    var mHeight: Int = 0
    init {
        vertexBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vertexBuffer.clear()

        val VERTEX: FloatArray = floatArrayOf(
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f,1.0f,
            1.0f, 1.0f
        )
        vertexBuffer.put(VERTEX)//将Java的顶点坐标转换为OpenGL的顶点坐标

        textureBuffer = ByteBuffer.allocateDirect(4 * 2 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        textureBuffer.clear()

        //片元着色器最终要渲染到SurfaceView中，所以顶点坐标与Android中View坐标一致
        val TEXTURE: FloatArray = floatArrayOf(
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f,0.0f,
            1.0f, 0.0f
        )
        textureBuffer.put(TEXTURE)//将Java的顶点坐标转换为OpenGL的顶点坐标
        initilize(context)
        initCoordinate()
    }

    abstract fun initCoordinate()

    private fun initilize(context: Context) {
        val vertexShader: String = TextResourceReader.readTextFileFromResource(context, vertexShaderId)
        val fragmentShader: String = TextResourceReader.readTextFileFromResource(context, fragmentShaderId)

        mProgram = ShaderHelper.loadProgram(vertexShader, fragmentShader)

        //获取着色器中的attribute变量的索引值
        vPosition = GLES20.glGetAttribLocation(mProgram, "vPosition")
        vCoord = GLES20.glGetAttribLocation(mProgram, "vCoord")

        //获取着色器中的uniform变量的索引值
        vMatrix = GLES20.glGetUniformLocation(mProgram, "vMatrix")
        vTexture = GLES20.glGetUniformLocation(mProgram, "vTexture")
    }

    //绘制摄像头采集的数据
    fun onDrawFrame(textureId: Int): Int{
        //设置显示窗口
        GLES20.glViewport(0, 0, mWidth, mHeight)

        //使用着色器
        GLES20.glUseProgram(mProgram)
        //将Java层的顶点坐标绑定到顶点着色器程序中的坐标变量,并激活对应的变量
        GLES20.glVertexAttribPointer(vPosition, 2, GLES20.GL_FLOAT, false, 0, vertexBuffer)
        GLES20.glEnableVertexAttribArray(vPosition)

        textureBuffer.position(0)
        GLES20.glVertexAttribPointer(vCoord, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)
        GLES20.glEnableVertexAttribArray(vCoord)

        //激活OpenGL中的采样器，并将SurfaceTexture与OpenGL中的vTexture绑定，才能将摄像头的图像数据传给OpenGL中采样器
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,textureId)
        //重置vTexture
        GLES20.glUniform1i(vTexture, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        return textureId
    }
}