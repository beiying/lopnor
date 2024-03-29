package com.beiying.media.opengl.shape

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES20.*
import com.beiying.media.R
import com.beiying.media.opengl.ShaderHelper
import com.beiying.media.opengl.VertexArray
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class Point(context: Context) : BaseShape(context) {
    var aColorLocation = 0
    var aPositionLocation = 0
    var pointVertex: FloatArray = floatArrayOf(
        0.1f, 0.1f, 0f,
        -0.1f, 0.1f, 0f,
        -0.1f, -0.1f, 0f,
        0.1f, -0.1f, 0f
    )

    var rectangleVertex: FloatArray = floatArrayOf(
        0f, 0f, 0f,
        0f, 0.5f, 0f,
        0.75f, 0.5f, 0f,
        0.75f, 0.5f, 0f,
        0.75f, 0f, 0f,
        0f, 0f, 0f
    )
    lateinit var rectangleVertexArray: VertexArray

    init {
        mProgram = ShaderHelper.buildPrograme(
            context,
            R.raw.point_vertex_shader,
            R.raw.point_fragment_shader
        )
        glUseProgram(mProgram)
        vertexArray = VertexArray(pointVertex)
        rectangleVertexArray = VertexArray(rectangleVertex)
        POSITION_COMPONENT_COUNT = 3
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        super.onSurfaceCreated(gl, config)
        //通过glGetUniformLocation和glGetAttribLocation函数绑定了我们在 OpenGL 中声明的变量u_Color和a_Position
        aColorLocation = glGetUniformLocation(mProgram,
            U_COLOR
        )
        //获取顶点着色器中属性a_Postion的位置
        aPositionLocation = glGetAttribLocation(mProgram,
            A_POSITION
        )

        ////attribute类型变量，则需要对应顶点数据中的值.通过给POSITION_COMPONENT_COUNT变量赋值，指定每个顶点数据的个数为 3
//        vertexArray.setVertexAttribPointer(0, aPositionLocation, POSITION_COMPONENT_COUNT, 0)
        rectangleVertexArray.setVertexAttribPointer(0, aPositionLocation, POSITION_COMPONENT_COUNT, 0)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        //对于uniform类型变量，由于是固定值，所以直接调用glUniform4f方法给其赋值就好了
        glUniform4f(aColorLocation, 1.0f, 0f, 0f, 1.0f)

        glDrawArrays(GL_POINTS,0, 2)

        glLineWidth(6f)
        glDrawArrays(GL_LINES, 2, 2)

        glDrawArrays(GL_TRIANGLES, 0, 6)
    }

    override fun onSurfaceDestroyed() {
        super.onSurfaceDestroyed()
        glDeleteProgram(mProgram)
    }

    companion object {
        val U_COLOR: String = "u_Color"
        val A_POSITION: String = "a_Position"
    }
}