package com.beiying.lopnor.base.ui.tab

import android.R
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.FrameLayout
import android.widget.ScrollView
import androidx.recyclerview.widget.RecyclerView
import com.beiying.lopnor.base.utils.ViewUtil


class ByTabBottomLayout(context: Context): FrameLayout(context), IByTabLayout<ByTabBottom, ByTabBottomInfo> {
    private val tabSelectedChangeListeners: ArrayList<IByTabLayout.OnTabSelectedListener<ByTabBottomInfo>> =
        ArrayList<IByTabLayout.OnTabSelectedListener<ByTabBottomInfo>>()
    lateinit var selectedInfo: ByTabBottomInfo
    var bottomAlpha = 1f

    //TabBottom高度
    var tabBottomHeight = 50f

    //TabBottom的头部线条高度
    var bottomLineHeight = 0.5f

    //TabBottom的头部线条颜色
    var bottomLineColor = "#dfe0e1"
    var infoList: List<ByTabBottomInfo>? = null

    private val TAG_TAB_BOTTOM = "TAG_TAB_BOTTOM"

    @ExperimentalStdlibApi
    override fun inflateInfo(infoList: List<ByTabBottomInfo>) {
        if (infoList.isEmpty()) {
            return
        }
        this.infoList = infoList
        // 移除之前已经添加的View
        // 移除之前已经添加的View
        for (i in childCount - 1 downTo 1) {
            removeViewAt(i)
        }
        addBackground()
        //清除之前添加的HiTabBottom listener，Tips：Java foreach remove问题
        val iterator: MutableIterator<IByTabLayout.OnTabSelectedListener<ByTabBottomInfo>> = tabSelectedChangeListeners.iterator()
        while (iterator.hasNext()) {
            if (iterator.next() is ByTabBottom) {
                iterator.remove()
            }
        }
        val height: Int = ViewUtil.dipToPx(context, tabBottomHeight)
        val ll = FrameLayout(context)
        val width: Int = ViewUtil.getScreenWidth(context) / infoList.size
        ll.tag = TAG_TAB_BOTTOM
        for (i in infoList.indices) {
            val info: ByTabBottomInfo = infoList[i]
            //Tips：为何不用LinearLayout：当动态改变child大小后Gravity.BOTTOM会失效
            val params = LayoutParams(width, height)
            params.gravity = Gravity.BOTTOM
            params.leftMargin = i * width
            val tabBottom = ByTabBottom(context)
            tabSelectedChangeListeners.add(tabBottom)
            tabBottom.tabInfo = info
            ll.addView(tabBottom, params)
            tabBottom.setOnClickListener(OnClickListener { onSelected(info) })
        }
        val flPrams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        flPrams.gravity = Gravity.BOTTOM
        addBottomLine()
        addView(ll, flPrams)

        fixContentView()
    }

    private fun addBackground() {
        val view: View = View(context)
        view.setBackgroundColor(Color.WHITE)
        val params = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewUtil.dipToPx(context, tabBottomHeight)
        )
        params.gravity = Gravity.BOTTOM
        addView(view, params)
        view.alpha = bottomAlpha
    }

    private fun addBottomLine() {
        val bottomLine = View(context)
        bottomLine.setBackgroundColor(Color.parseColor(bottomLineColor))
        val bottomLineParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewUtil.dipToPx(context, bottomLineHeight)
        )
        bottomLineParams.gravity = Gravity.BOTTOM
        bottomLineParams.bottomMargin = ViewUtil.dipToPx(context, tabBottomHeight - bottomLineHeight)
        addView(bottomLine, bottomLineParams)
        bottomLine.alpha = bottomAlpha
    }

    /**
     * 修复内容区域的底部Padding
     */
    @ExperimentalStdlibApi
    private fun fixContentView() {
        if (getChildAt(0) !is ViewGroup) {
            return
        }
        val rootView = getChildAt(0) as ViewGroup
        var targetView: ViewGroup? =
            ViewUtil.findTypeView(rootView, RecyclerView::class.java)
        if (targetView == null) {
            targetView = ViewUtil.findTypeView(rootView, ScrollView::class.java)
        }
        if (targetView == null) {
            targetView = ViewUtil.findTypeView(rootView, AbsListView::class.java)
        }
        if (targetView != null) {
            targetView.setPadding(0, 0, 0, ViewUtil.dipToPx(context, tabBottomHeight))
            targetView.clipToPadding = false
        }
    }

    override fun findTab(info: ByTabBottomInfo): ByTabBottom? {
        val ll = findViewWithTag<ViewGroup>(TAG_TAB_BOTTOM)
        for (i in 0 until ll.childCount) {
            val child = ll.getChildAt(i)
            if (child is ByTabBottom) {
                val tab: ByTabBottom = child as ByTabBottom
                if (tab.tabInfo === info) {
                    return tab
                }
            }
        }
        return null
    }

    override fun addOnTabSelectedListener(listener: IByTabLayout.OnTabSelectedListener<ByTabBottomInfo>) {
        tabSelectedChangeListeners.add(listener)
    }

    override fun defaultSelected(defaultInfo: ByTabBottomInfo) {
        onSelected(defaultInfo)
    }

    private fun onSelected(nextInfo: ByTabBottomInfo) {
        for (listener in tabSelectedChangeListeners) {
            listener.onTabSelectedChange(infoList!!.indexOf(nextInfo), selectedInfo, nextInfo)
        }
        selectedInfo = nextInfo
    }
}