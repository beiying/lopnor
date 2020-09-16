package com.beiying.ffplayer.beauty

import android.content.Context
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.opengles.GL

class AbstractFilter(context: Context, val vertexShaderId: Int, val fragmentShaderId: Int) {
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

    private fun initCoordinate() {

    }

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
}