package com.beiying.media.opengl

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES20.*
import com.beiying.media.R
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Point(context: Context) : BaseShape(context) {
    var aColorLocation = 0
    var aPositionLocation = 0
    var pointVertex: FloatArray = floatArrayOf(0f,0f)

    init {
        mProgram = ShaderHelper.buildPrograme(context, R.raw.point_vertex_shader, R.raw.point_fragment_shader)
        glUseProgram(mProgram)
        vertexArray = VertexArray(pointVertex)
        POSITION_COMPONENT_COUNT = 2
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        super.onSurfaceCreated(gl, config)
        //通过glGetUniformLocation和glGetAttribLocation函数绑定了我们在 OpenGL 中声明的变量u_Color和a_Position
        aColorLocation = glGetUniformLocation(mProgram, U_COLOR)
        aPositionLocation = glGetAttribLocation(mProgram, A_POSITION)

        ////attribute类型变量，则需要对应顶点数据中的值.通过给POSITION_COMPONENT_COUNT变量赋值，指定每个顶点数据的个数为 2
        vertexArray.setVertexAttribPointer(0, aPositionLocation, POSITION_COMPONENT_COUNT, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)
        GLES20.glViewport(0,0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        //对于uniform类型变量，由于是固定值，所以直接调用glUniform4f方法给其赋值就好了
        glUniform4f(aColorLocation, 0f, 0f, 1.0f, 1.0f)

        glDrawArrays(GL_POINTS,0, pointVertex.size / POSITION_COMPONENT_COUNT)
    }

    override fun onSurfaceDestroyed() {
        super.onSurfaceDestroyed()
        glDeleteProgram(mProgram)
    }

    companion object {
        val U_COLOR: String = "u_color"
        val A_POSITION: String = "a_position"
    }
}