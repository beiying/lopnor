package com.beiying.lopnor.base.recylerview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.beiying.lopnor.base.R


class LoadMoreView(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int) : LinearLayout(context, attributeSet, defStyleAttr),
    View.OnClickListener {
    private lateinit var mLoadingLl: LinearLayout
    private lateinit var mFinishLl:LinearLayout
    private lateinit var mErrorLl:LinearLayout
    private lateinit var mLoadingTv: TextView
    private lateinit var mFinishTv:TextView
    private lateinit var mErrorTv:TextView
    private var mListener: AdapterLoadMoreClickListener? = null

    init {
        init()
    }

    constructor(context: Context): this(context, null, 0)

    private fun init() {
        LayoutInflater.from(context).inflate(R.layout.layout_load_more, this)
        setView()
    }

    /**
     * 初始化数据
     */
    private fun setView() {
        mLoadingLl = findViewById<LinearLayout>(R.id.base_adapter_ll_loading)
        mFinishLl = findViewById<LinearLayout>(R.id.base_adapter_ll_finish)
        mErrorLl = findViewById<LinearLayout>(R.id.base_adapter_ll_error)
        mLoadingTv = findViewById<TextView>(R.id.base_adapter_tv_loading)
        mFinishTv = findViewById<TextView>(R.id.base_adapter_tv_finish)
        mErrorTv = findViewById<TextView>(R.id.base_adapter_tv_error)
        showLoadMore()
        mLoadingLl.setOnClickListener(this)
        mFinishLl.setOnClickListener(this)
        mErrorLl.setOnClickListener(this)
    }

    fun showLoadMore() {
        mErrorLl.setVisibility(View.GONE)
        mFinishLl.setVisibility(View.GONE)
        mLoadingLl.setVisibility(View.VISIBLE)
    }

    fun showLoadFinish() {
        mErrorLl.setVisibility(View.GONE)
        mFinishLl.setVisibility(View.VISIBLE)
        mLoadingLl.setVisibility(View.GONE)
    }

    fun showLoadError() {
        mErrorLl.setVisibility(View.VISIBLE)
        mFinishLl.setVisibility(View.GONE)
        mLoadingLl.setVisibility(View.GONE)
    }

    fun setLoadErrorTitle(str: String?) {
        mErrorTv.setText(str)
    }

    fun setLoadFinishTitle(str: String?) {
        mFinishTv.setText(str)
    }

    fun setLoadingTitle(str: String?) {
        mLoadingTv.setText(str)
    }

    /**
     * 设置点击监听
     *
     * @param listener
     */
    fun setAdapterLoadMoreClickListener(listener: AdapterLoadMoreClickListener?) {
        this.mListener = listener
    }

    override fun onClick(v: View) {
        if (v === mLoadingLl) {
            mListener?.onLoadMoreClick()
        } else if (v === mFinishLl) {
            mListener?.onLoadFinishClick()
        } else if (v === mErrorLl) {
            if (null != mListener) showLoadMore()
            mListener?.onLoadErrorClick()
        }
    }
}