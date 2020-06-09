package com.beiying.media.opengl

import com.beiying.media.R
import com.beiying.media.opengl.shape.Point

class GraphActivity : BaseRenderActivity() {
    override fun initShapeClazz() {
        shapeClazzArray.put(R.id.point, Point::class.java)
    }

    override fun setMenuId() {
        setToolbarMenu(R.menu.basic_graph_menu)
    }

    override fun setInitShape() {
        super.setInitShape()
        mBaseRender.setShape(Point::class.java)
    }

}