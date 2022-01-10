package com.beiying.media.opengl.render

import android.opengl.GLSurfaceView
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class GLRender: GLSurfaceView.Renderer {

    var nativeRender: NativeES3Render = NativeES3Render()

    fun init() {
        nativeRender.native_Init()
    }

    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        nativeRender.native_OnSurfaceCreated()
    }

    override fun onSurfaceChanged(p0: GL10?, width: Int, height: Int) {
        nativeRender.native_OnSurfaceChanged(width, height)
    }

    override fun onDrawFrame(p0: GL10?) {
        nativeRender.native_OnDrawFrame()
    }

    fun unInit() {
        nativeRender.native_UnInit()
    }
}