package com.beiying.media.opengl

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.SparseArray
import androidx.appcompat.widget.Toolbar
import com.beiying.lopnor.base.BaseToolbarActivity
import com.beiying.media.opengl.render.BaseShapeRender
import com.beiying.media.opengl.shape.BaseShape
import com.beiying.media.opengl.shape.Point

abstract class BaseRenderActivity : BaseToolbarActivity() {
    lateinit var mBaseShapeView: BaseShapeView
    lateinit var mBaseRender: BaseShapeRender

    var shapeClazzArray = SparseArray<Class<out BaseShape>>(4)
    var clazz: Class<out BaseShape> = Point::class.java

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBaseRender = BaseShapeRender(this)
        mBaseShapeView = BaseShapeView(this, mBaseRender)
        mBaseShapeView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        if (savedInstanceState != null) {
            mBaseRender.setShape(savedInstanceState.getSerializable(RENDER_SHAPE) as Class<out BaseShape>)
        } else {
            setInitShape()
        }

        initShapeClazz()

        setContentView(mBaseShapeView)
    }

    open fun setInitShape() {

    }

    override fun updateToolbar() {
//        setToolbarTitle(intent.getStringExtra(ACTIVITY_TITLE))
        setMenuId()
        setToolbarMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
            updateShape(item.itemId)
            true
        })
    }
    private fun updateShape(itemId: Int) {
        clazz = shapeClazzArray.get(itemId)
        recreate()
    }

    abstract fun initShapeClazz()

    abstract fun setMenuId()
}