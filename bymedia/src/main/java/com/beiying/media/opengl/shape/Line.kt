package com.beiying.media.opengl.shape

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.opengl.Matrix
import com.beiying.media.R
import com.beiying.media.opengl.ShaderHelper
import com.beiying.media.opengl.VertexArray
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Line(context: Context): BaseShape(context) {
    val lineVertex = floatArrayOf(-0.5f, 0.5f,
                                0.5f, -0.5f)

    private var aPositionLocation: Int = 0
    private var aColorLocation: Int = 0
    private var uProjectionMatrixLocation: Int = 0
    private var uViewMatrixLocation: Int = 0
    private var uMatrixLocation: Int = 0

    companion object {
        val A_POSITION: String = "a_Position"
        val U_COLOR: String = "u_Color"
        val U_MODEL_MATRIX: String = "u_ModelMatrix"
        val U_PROJECTION_MATRIX: String = "u_ProjectionMatrix"
        val U_VIEW_MATRIX: String = "u_ViewMatrix"

    }

    init {
        mProgram = ShaderHelper.buildPrograme(context, R.raw.line_vertex_shader, R.raw.line_fragment_shader)
        glUseProgram(mProgram)

        vertexArray = VertexArray(lineVertex)
        POSITION_COMPONENT_COUNT = 2
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        aPositionLocation = GLES20.glGetAttribLocation(mProgram, A_POSITION)
        aColorLocation = GLES20.glGetUniformLocation(mProgram, U_COLOR)
        uProjectionMatrixLocation = GLES20.glGetUniformLocation(mProgram, U_PROJECTION_MATRIX)
        uViewMatrixLocation = GLES20.glGetUniformLocation(mProgram, U_VIEW_MATRIX)
        uMatrixLocation = GLES20.glGetUniformLocation(mProgram, U_MODEL_MATRIX)

        vertexArray.setVertexAttribPointer(0, aPositionLocation, POSITION_COMPONENT_COUNT, 0)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.setIdentityM(viewMatrix, 0)
        Matrix.translateM(modelMatrix, 0, 0.5f, 0f, 0f)
        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 10f, 0f, 0f, 0f, 0f, 1.0f, 0f)

    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)
        Matrix.perspectiveM(projectionMatrix, 0, 5f, width.toFloat() / height.toFloat(), 9f, 20f)
        //        float aspectRatio = width > height ? (float) width / (float) height : (float) height / (float) width;
//
//        if (width > height){
//
//            Matrix.frustumM(projectionMatrix,0,-aspectRatio,aspectRatio,-1f,1f,5f,20f);
//
//        }else {
//
//            Matrix.frustumM(projectionMatrix,0,-1f,1f,-aspectRatio,aspectRatio,5f,20f);
//
//        }
    }

    override fun onDrawFrame(gl: GL10?) {
        super.onDrawFrame(gl)
        glUniform4f(aColorLocation, 0f, 0f, 1.0f, 1.0f)


        //使用矩阵平移，将坐标X轴平移0.5个单位
        glUniformMatrix4fv(uMatrixLocation, 1, false, modelMatrix, 0)
        glUniformMatrix4fv(uProjectionMatrixLocation, 1, false, projectionMatrix, 0)
        glUniformMatrix4fv(uViewMatrixLocation, 1, false, viewMatrix, 0)
        glDrawArrays(GL_LINES, 0, 2)
    }
}