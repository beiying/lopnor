package com.beiying.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class ByBaseFragment: Fragment() {
    lateinit var layoutView: View

    abstract fun getLayoutId(): Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        layoutView = inflater.inflate(getLayoutId(), container, false)
        return layoutView
    }
}