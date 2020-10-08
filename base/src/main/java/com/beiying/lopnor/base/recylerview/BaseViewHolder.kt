package com.beiying.lopnor.base.recylerview

import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView


class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val mViewMap: MutableMap<Int, View> = HashMap()

    /**
     * 获取设置的view
     *
     * @param id
     * @return_transfer
     */
    fun <T : View?> getView(id: Int): T? {
        var view = mViewMap[id]
        if (null == view) {
            view = itemView.findViewById(id)
            mViewMap[id] = view
        }
        return view as T?
    }

    fun getTextView(id: Int): TextView? {
        return getView<View>(id) as TextView?
    }

    fun getImageView(id: Int): ImageView? {
        return getView<View>(id) as ImageView?
    }

    fun getButton(id: Int): Button? {
        return getView<View>(id) as Button?
    }

    fun getEditText(id: Int): EditText? {
        return getView<View>(id) as EditText?
    }
}