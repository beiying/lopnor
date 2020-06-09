package com.beiying.media.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.SurfaceHolder
import com.beiying.media.opengl.render.BaseRender

class BaseShapeView(context: Context,  val baseRender: BaseRender) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        setRenderer(baseRender)
        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    override fun surfaceCreated(holder: SurfaceHolder?) {
        super.surfaceCreated(holder)
    }

    override fun surfaceChanged(holder: SurfaceHolder?, format: Int, w: Int, h: Int) {
        super.surfaceChanged(holder, format, w, h)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder?) {
        super.surfaceDestroyed(holder)
        baseRender.onSurfaceDestroyed()
    }
}