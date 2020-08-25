package com.beiying.media.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.SurfaceHolder
import com.beiying.media.opengl.render.BaseRender

class BaseShapeView(context: Context,  val baseRender: BaseRender) : GLSurfaceView(context) {
    init {
        //设置OpenGL版本
        setEGLContextClientVersion(2)
        setRenderer(baseRender)

        //设置渲染模式，包括实时渲染（每个16ms渲染一次）和按需渲染（需要渲染时才渲染，效率高）
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