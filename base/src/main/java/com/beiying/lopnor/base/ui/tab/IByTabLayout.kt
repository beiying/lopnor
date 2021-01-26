package com.beiying.lopnor.base.ui.tab

import android.view.ViewGroup

interface IByTabLayout<Tab : ViewGroup, D> {
    fun findTab(data: D): Tab

    fun addOnTabSelectedListener(listener: OnTabSelectedListener<D>)

    fun defaultSelected(defaultInfo: D)

    fun inflateInfo(infoList: List<D>)

    interface OnTabSelectedListener<D> {
        fun onTabSelectedChange(index: Int, preInfo: D, nextInfo: D)
    }
}