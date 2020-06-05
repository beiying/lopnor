package com.beiying.media.opengl

import android.content.Context
import android.opengl.GLES20.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

open class BaseShape(val context: Context) {
    lateinit var vertexArray: VertexArray
    lateinit var indexArray: VertexArray

    var mProgram: Int = 0

    val modelMatrix: FloatArray = FloatArray(16)
    val viewMatrix: FloatArray = FloatArray(16)
    val projectionMatrix: FloatArray = FloatArray(16)
    val mvpMatrix: FloatArray = FloatArray(16)

    var POSITION_COMPONENT_COUNT: Int = 0
    var TEXTURE_COORRDINATES_COMPONENT_COUNT: Int = 2
    var STRIDE: Int = 0

    open fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

    }

    open fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {

    }

    open fun onDrawFrame(gl: GL10?) {
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)
    }

    open fun onDrawFrame(gl: GL10, mvpMatrix: FloatArray) {
        glClearColor(0f, 0f, 0f, 0f)
        glClear(GL_DEPTH_BUFFER_BIT or GL_COLOR_BUFFER_BIT)
    }

    open fun onSurfaceDestroyed() {

    }
}