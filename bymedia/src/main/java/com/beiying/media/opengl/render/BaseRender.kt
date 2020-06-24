package com.beiying.media.opengl.render

import android.content.Context
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

open class BaseRender(val context: Context) : GLSurfaceView.Renderer {

    val modelMatrix = FloatArray(16)

    val viewMatrix = FloatArray(16)

    val projectionMatrix = FloatArray(16)

    val mvpMatrix = FloatArray(16)


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    }

    override fun onDrawFrame(gl: GL10?) {
    }

    open fun onSurfaceDestroyed() {

    }

}