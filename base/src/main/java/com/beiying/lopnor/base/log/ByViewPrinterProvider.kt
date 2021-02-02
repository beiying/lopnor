package com.beiying.lopnor.base.log

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.beiying.lopnor.base.utils.ViewUtils

/**
 * 通过ViewPrinterProvider控制log视图的展示和隐藏
 * */
class ByViewPrinterProvider(val rootView: FrameLayout, val recyclerView: RecyclerView) {
    private val TAG_FLOATING_VIEW = "TAG_FLOATING_VIEW"
    private val TAG_LOG_VIEW = "TAG_LOG_VIEW"

    var floatingView: View? = null
    var logView: View? = null

    var isOpen: Boolean = false

    fun showFloatView() {
        if (rootView.findViewWithTag<View>(TAG_FLOATING_VIEW) != null) return

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.BOTTOM or Gravity.END
        val floatingView: View = genFloatingView()
        floatingView.tag = TAG_FLOATING_VIEW
        floatingView.setBackgroundColor(Color.BLACK)
        floatingView.alpha = 0.8f
        params.bottomMargin = ViewUtils.dipToPx(rootView.context,100f)
        rootView.addView(genFloatingView(), params)
    }

    /**
     * 展示LogView
     */
    fun showLogView() {
        if (rootView.findViewWithTag<View?>(TAG_LOG_VIEW) != null) {
            return
        }
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewUtils.dipToPx(rootView.context,160f))
        params.gravity = Gravity.BOTTOM
        val logView = genLogView()
        logView.tag = TAG_LOG_VIEW
        rootView.addView(genLogView(), params)
        isOpen = true
    }

    fun genFloatingView(): View {
        if (floatingView != null) {
            return floatingView as View
        }
        val textView: TextView = TextView(rootView.context)
        textView.setOnClickListener {view ->
            if (!isOpen) {
                showLogView()
            }
        }
        textView.text = "ByLog"
        floatingView = textView
        return floatingView as View
    }

    fun genLogView(): View {
        if (logView != null) {
            return logView as View
        }
        val logContent = FrameLayout(rootView.context)
        logContent.setBackgroundColor(Color.BLACK)
        logContent.addView(recyclerView)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.END
        val closeView = TextView(rootView.context)
        closeView.setOnClickListener { closeLogView() }
        closeView.text = "Close"
        logContent.addView(closeView, params)
        logView = logContent
        return logView as View
    }


    /**
     * 关闭LogView
     */
    fun closeLogView() {
        isOpen = false
        rootView.removeView(genLogView())
    }

    /**
     * 展示Log 悬浮按钮
     */
    fun closeFloatingView() {
        rootView.removeView(genFloatingView())
    }


}