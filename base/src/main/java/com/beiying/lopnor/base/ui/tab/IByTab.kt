package com.beiying.lopnor.base.ui.tab

interface IByTab<D>: IByTabLayout.OnTabSelectedListener<D> {
    fun setByTabInfo(data: D)

    //动态修改单个TabItem的大小
    fun resetHeight(height: Int)
}