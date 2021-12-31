package com.beiying.media.opengl.render

import android.content.Context
import android.opengl.GLES20.*
import com.beiying.media.opengl.shape.Point
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PointRender(context: Context) : BaseRender(context) {
    lateinit var mPoint: Point

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        super.onSurfaceCreated(gl, config)
        glClearColor(1.0f, 1.0f,1.0f, 1.0f)
        mPoint = Point(context)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        super.onSurfaceChanged(gl, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        super.onDrawFrame(gl)
        glClear(GL_COLOR_BUFFER_BIT)
        mPoint.onDrawFrame(gl!!)
    }
}