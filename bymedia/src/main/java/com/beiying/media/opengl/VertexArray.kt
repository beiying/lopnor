package com.beiying.media.opengl

import android.opengl.GLES20.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class VertexArray(vertexData: FloatArray) {
    lateinit var floatBuffer: FloatBuffer
    init {//初始化顶点数据
        floatBuffer = ByteBuffer.allocateDirect(vertexData.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertexData)
    }

    /**
     * 使顶点某个数据可用
     * */
    fun setVertexAttribPointer(dataOffset: Int, attributeLocation: Int, componentCount: Int, stride: Int) {
        floatBuffer.position(dataOffset)

        glVertexAttribPointer(attributeLocation, componentCount, GL_FLOAT, false, stride, floatBuffer)
        glEnableVertexAttribArray(attributeLocation)

    }
}