package com.beiying.media.opengl

import android.content.Context
import android.opengl.GLES20.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PointRender(context: Context) : BaseRender(context) {
    lateinit var mPoint: Point

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        super.onSurfaceCreated(gl, config)
        glClearColor(0.0f, 0.0f,0.0f, 0.0f)
        mPoint = Point(context)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)
        glViewport(0,0,width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        super.onDrawFrame(gl)
        glClear(GL_COLOR_BUFFER_BIT)
        mPoint.onDrawFrame(gl!!)
    }
}