package com.beiying.media.opengl

import com.beiying.media.R
import com.beiying.media.opengl.shape.Line
import com.beiying.media.opengl.shape.Point
import com.beiying.media.opengl.shape.Triangle

class GraphActivity : BaseRenderActivity() {
    override fun initShapeClazz() {
        shapeClazzArray.put(R.id.point, Point::class.java)
        shapeClazzArray.put(R.id.line, Line::class.java)
        shapeClazzArray.put(R.id.triangle, Triangle::class.java)
    }

    override fun setMenuId() {
        setToolbarMenu(R.menu.basic_graph_menu)
    }

    override fun setInitShape() {
        super.setInitShape()
        mBaseRender.setShape(Point::class.java)
    }

}