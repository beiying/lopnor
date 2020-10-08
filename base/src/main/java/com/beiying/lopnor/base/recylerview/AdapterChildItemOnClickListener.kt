package com.beiying.lopnor.base.recylerview

import android.view.View

/**
 * recyclerView adapter子view点击监听
 * */
interface AdapterChildItemOnClickListener {
    fun onClick(view: View?, position: Int)
}