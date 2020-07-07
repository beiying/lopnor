package com.beiying.media.opengl.shape

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.opengl.Matrix.setIdentityM
import com.beiying.media.R
import com.beiying.media.opengl.ShaderHelper
import com.beiying.media.opengl.VertexArray
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL
import javax.microedition.khronos.opengles.GL10

class Triangle(context: Context): BaseShape(context) {
    companion object {
        val U_COLOR:String = "u_Color"
        val A_POSITION:String = "a_Position"
        val U_MATRIX:String = "u_Matrix"
        val U_PRO_MATRIX: String = "u_ProMatrix"
    }

    private var aColorLocation: Int = 0
    private var aPositionLocation: Int = 0
    private var uMatrixLocation: Int = 0

    private var uMatrixLocation1: Int = 0
    private var uMatrixLocation2: Int = 0
    private var uMatrixLocation3: Int = 0
    private var uMatrixLocation4: Int = 0

    private var uProMatrixLocation: Int = 0

    val triangleVertex: FloatArray = floatArrayOf(//顺时针
        0.5f, 0f,  //1
        0f, 1.0f,  //2
        1.0f, 1.0f //3
    )

    val cubeVertex: FloatArray = floatArrayOf(
        -0.5f, 0.5f, 0.5f,
        0.5f, 0.5f, 0.5f,
        -0.5f, -0.5f, 0.5f,
        0.5f, -0.5f, 0.5f,

        -0.5f, 0.5f, -0.5f,
        0.5f, 0.5f, -0.5f,
        -0.5f, -0.5f, -0.5f,
        0.5f, -0.5f, -0.5f
    )

    val position: ByteArray = byteArrayOf(
        // Front
        1, 3, 0,
        0, 3, 2,

        // Back
        4, 6, 5,
        5, 6, 7,

        // Left
        0, 2, 4,
        4, 2, 6,

        // Right
        5, 7, 1,
        1, 7, 3,

        // Top
        5, 1, 4,
        4, 1, 0,

        // Bottom
        6, 2, 7,
        7, 2, 3
    )

    val index: ByteArray = byteArrayOf(
        0, 1, 2,
        3, 0, 2
    )

    val vec1: FloatArray = floatArrayOf(1.0f, 0.0f, 0.0f, 0.0f)
    val vec2: FloatArray = floatArrayOf(0.0f, 1.0f, 0.0f, 0.0f)
    val vec3: FloatArray = floatArrayOf(0.0f, 0.0f, 1.0f, 0.0f)
    val vec4: FloatArray = floatArrayOf(0.0f, 0.0f, 0.0f, 1.0f)

    private lateinit var vertexArray1: VertexArray
    private lateinit var vertexArray2: VertexArray
    private lateinit var vertexArray3: VertexArray
    private lateinit var vertexArray4: VertexArray

    private lateinit var byteBuffer: ByteBuffer

    init {
        mProgram = ShaderHelper.buildPrograme(context, R.raw.triangle_vertex_shader, R.raw.triangle_fragment_shader)
        GLES20.glUseProgram(mProgram)

        vertexArray = VertexArray(triangleVertex)
        vertexArray1 = VertexArray(vec1)
        vertexArray2 = VertexArray(vec2)
        vertexArray3 = VertexArray(vec3)
        vertexArray4 = VertexArray(vec4)

        byteBuffer = ByteBuffer.allocateDirect(position.size).put(position)
        byteBuffer.position(0)
        POSITION_COMPONENT_COUNT = 2
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        aPositionLocation = GLES20.glGetAttribLocation(mProgram, A_POSITION)
        aColorLocation = GLES20.glGetUniformLocation(mProgram, U_COLOR)
        uMatrixLocation = GLES20.glGetUniformLocation(mProgram, U_MATRIX)
        uProMatrixLocation = GLES20.glGetUniformLocation(mProgram, U_PRO_MATRIX)

        vertexArray.setVertexAttribPointer(0, aPositionLocation, POSITION_COMPONENT_COUNT, 0)

        setIdentityM(modelMatrix, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)

        val aspectRatio: Float = if (width > height) width.toFloat() / height.toFloat() else height.toFloat() / width.toFloat()
        //生成正交投影矩阵，保证近平面的宽高比和视口的宽高比一致
        if (width > height) {
            Matrix.orthoM(projectionMatrix, 0, -aspectRatio, aspectRatio, -1f, 1f, 0f, 10f)
        } else {
            Matrix.orthoM(projectionMatrix, 0, -1f, 1f, -aspectRatio, aspectRatio, 0f, 10f)
        }

    }

    override fun onSurfaceDestroyed() {
        super.onSurfaceDestroyed()
    }

    override fun onDrawFrame(gl: GL10?) {
        super.onDrawFrame(gl)
        
        GLES20.glUniform4f(aColorLocation, 0.0f, 1.0f, 1.0f, 1.0f)
        
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uProMatrixLocation, 1, false, projectionMatrix, 0)
        // 使用 glDrawArrays方式绘图
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
        
    }

    override fun onDrawFrame(gl: GL10, mvpMatrix: FloatArray) {
        super.onDrawFrame(gl, mvpMatrix)

        GLES20.glUniform4f(aColorLocation, 0.0f, 1.0f, 1.0f, 1.0f)
        GLES20.glUniformMatrix4fv(uMatrixLocation, 1, false, mvpMatrix, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 3)
    }


}