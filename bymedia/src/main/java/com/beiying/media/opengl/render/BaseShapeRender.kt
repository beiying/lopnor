package com.beiying.media.opengl.render

import android.content.Context
import com.beiying.media.opengl.shape.BaseShape
import com.beiying.media.opengl.shape.Point
import java.lang.Exception
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class BaseShapeRender(context: Context) : BaseRender(context) {
    lateinit var shape: BaseShape
    var clazz: Class<out BaseShape> = Point::class.java

    fun setShape(shapeClazz: Class<out BaseShape>) {
        this.clazz = shapeClazz
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        super.onSurfaceCreated(gl, config)
        try {
            var constructor = clazz.getDeclaredConstructor(Context::class.java)
            constructor.isAccessible = true
            shape = constructor.newInstance(context) as BaseShape
        } catch (e: Exception) {
            shape = Point(context)
        }
        shape.onSurfaceCreated(gl, config)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        shape.onSurfaceChanged(gl, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        shape.onDrawFrame(gl)
    }

    override fun onSurfaceDestroyed() {
        shape.onSurfaceDestroyed()
    }
}