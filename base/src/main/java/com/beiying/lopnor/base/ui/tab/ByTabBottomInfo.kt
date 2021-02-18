package com.beiying.lopnor.base.ui.tab

import android.graphics.Bitmap

class ByTabBottomInfo  {
    enum class TabType{
        BITMAP, ICON
    }

    var name: String? = null
    var iconFont: String? = null
    var defaultBitmap: Bitmap? = null
    var selectedBitmap: Bitmap? = null
    var defaultIconName: String? = null
    var selectedIconName: String? = null
    var tabType: TabType = TabType.BITMAP
    var defaultColor: Int = 0
    var tintColor: Int = 0

    constructor(name: String, defaultBitmap: Bitmap, selectedBitmap: Bitmap) {
        this.name = name
        this.defaultBitmap = defaultBitmap
        this.selectedBitmap = selectedBitmap
        this.tabType = TabType.BITMAP
    }

    constructor(name: String, iconFont: String, defaultIconName: String, selectedIconName: String, defaultColor: Int, tintColor: Int) {
        this.name = name
        this.iconFont = iconFont
        this.defaultIconName = defaultIconName
        this.selectedIconName = selectedIconName
        this.defaultColor = defaultColor
        this.tintColor = tintColor
        this.tabType = TabType.ICON
    }


}