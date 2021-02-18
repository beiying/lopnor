package com.beiying.lopnor.base.ui.tab

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.ColorInt
import com.beiying.lopnor.base.R

class ByTabBottom(context: Context, attrs: AttributeSet?, defStyleAttr: Int): RelativeLayout(context, attrs, defStyleAttr), IByTab<ByTabBottomInfo>{
    lateinit var tabInfo: ByTabBottomInfo
    lateinit var tabImageView: ImageView
    lateinit var tabIconView: TextView
    lateinit var tabNameView: TextView
    constructor(context: Context): this(context, null, 0) {

    }
    constructor(context: Context, attrs: AttributeSet): this(context, attrs, 0) {

    }
    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.layout_tab_bottom, this)
        tabImageView = findViewById<ImageView>(R.id.iv_image)
        tabIconView = findViewById<TextView>(R.id.tv_icon)
        tabNameView = findViewById<TextView>(R.id.tv_name)
    }

    override fun setByTabInfo(tabBottomInfo: ByTabBottomInfo) {
        tabInfo = tabBottomInfo
        inflateInfo(selected = false, init = true)
    }

    private fun inflateInfo(selected: Boolean, init: Boolean) {
        if (tabInfo.tabType === ByTabBottomInfo.TabType.ICON) {
            if (init) {
                tabImageView.visibility = View.GONE
                tabIconView.visibility = View.VISIBLE
                val typeface =
                    Typeface.createFromAsset(context.assets, tabInfo.iconFont)
                tabIconView.setTypeface(typeface)
                if (!TextUtils.isEmpty(tabInfo.name)) {
                    tabNameView.text = tabInfo.name
                }
            }
            if (selected) {
                tabIconView.text =
                    if (TextUtils.isEmpty(tabInfo.selectedIconName)) tabInfo.defaultIconName else tabInfo.selectedIconName
                tabIconView.setTextColor(getTextColor(tabInfo.tintColor))
                tabNameView.setTextColor(getTextColor(tabInfo.tintColor))
            } else {
                tabIconView.text = tabInfo.defaultIconName
                tabIconView.setTextColor(getTextColor(tabInfo.defaultColor))
                tabNameView.setTextColor(getTextColor(tabInfo.defaultColor))
            }
        } else if (tabInfo.tabType === ByTabBottomInfo.TabType.BITMAP) {
            if (init) {
                tabImageView.visibility = View.VISIBLE
                tabIconView.visibility = View.GONE
                if (!TextUtils.isEmpty(tabInfo.name)) {
                    tabNameView.text = tabInfo.name
                }
            }
            if (selected) {
                tabImageView.setImageBitmap(tabInfo.selectedBitmap)
            } else {
                tabImageView.setImageBitmap(tabInfo.defaultBitmap)
            }
        }
    }

    @ColorInt
    private fun getTextColor(color: Any): Int {
        return if (color is String) {
            Color.parseColor(color)
        } else {
            color as Int
        }
    }

    override fun resetHeight(height: Int) {
        val layoutParams = layoutParams
        layoutParams.height = height
        setLayoutParams(layoutParams)
        tabNameView.visibility = View.GONE
    }

    override fun onTabSelectedChange(index: Int, preInfo: ByTabBottomInfo, nextInfo: ByTabBottomInfo) {
        if (preInfo !== tabInfo && nextInfo !== tabInfo || preInfo === nextInfo) {
            return
        }
        if (preInfo === tabInfo) {
            inflateInfo(false, false)
        } else {
            inflateInfo(true, false)
        }
    }
}