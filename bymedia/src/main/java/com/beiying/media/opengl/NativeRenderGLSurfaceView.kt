package com.beiying.media.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import com.beiying.media.opengl.render.GLRender

class NativeRenderGLSurfaceView(context: Context, val render: GLRender, attributeset: AttributeSet?): GLSurfaceView(context, attributeset) {
    constructor(context: Context, render: GLRender): this(context, render,null)

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 8)
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY

    }
}