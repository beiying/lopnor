package com.beiying.media.opengl.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

open class BaseRender(val context: Context) : GLSurfaceView.Renderer {

    val modelMatrix = FloatArray(16)

    val viewMatrix = FloatArray(16)

    val projectionMatrix = FloatArray(16)

    val mvpMatrix = FloatArray(16)


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        //Surface创建的时候重置画布为黑色
        GLES20.glClearColor(1.0f,1.0f,1.0f,1.0f)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
    }

    open fun onSurfaceDestroyed() {

    }

}