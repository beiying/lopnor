package com.beiying.lopnor.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar

abstract class BaseToolbarActivity : AppCompatActivity() {
    private lateinit var mToolbar: Toolbar
    private lateinit var mContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_toolbar)
        mToolbar = findViewById(R.id.toolbar)
        mContainer = findViewById(R.id.baseContainer)
    }

    private fun setContent(contentId: Int) {
        LayoutInflater.from(this).inflate(contentId, mContainer)
    }

    override fun setContentView(layoutResID: Int) {
        if (layoutResID == R.layout.activity_base_toolbar) {
            super.setContentView(layoutResID)
        } else {
            setContent(layoutResID)
        }
    }

    override fun setContentView(view: View) {
        mContainer.removeAllViews()
        mContainer.addView(view)
    }

    override fun setContentView(view: View?, params: ViewGroup.LayoutParams?) {
        mContainer.removeAllViews()
        mContainer.addView(view,params)
    }

    fun setToolbarTitle(title: String) {
        mToolbar.title = title
    }

    fun setToolbarTitle(title: Int) {
        mToolbar.setTitle(title)
    }

    fun setToolbarMenu(menu: Int) {
        mToolbar.inflateMenu(menu)
    }

    fun setToolbarMenuItemClickListener(listener: Toolbar.OnMenuItemClickListener) {
        mToolbar.setOnMenuItemClickListener(listener)
    }

    fun setToolbarLogo(logo: Int) {
        mToolbar.setLogo(logo)
    }

    fun setTitleTextColor(color: Int) {
        mToolbar.setTitleTextColor(color)
    }


    abstract fun updateToolbar()
}