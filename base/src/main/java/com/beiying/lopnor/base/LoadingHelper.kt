package com.beiying.lopnor.base

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout

/**
 * 优雅地管理 loading 页面和标题栏
 * */
class LoadingHelper @JvmOverloads constructor(private val contentView: View, contentAdapter: ContentAdapter<*>? = null) {

    lateinit var decorView: View
        private set
    private lateinit var contentParent: ViewGroup
    private val parent: ViewGroup?
    private var currentViewHolder: ViewHolder? = null
    private var onReloadListener: OnReloadListener? = null
    private var adapters: HashMap<Any, Adapter<*>> = HashMap()
    private val viewHolders: HashMap<Any, ViewHolder> = HashMap()

    companion object {
        private var adapterPool: (AdapterPool.() -> Unit)? = null

        @JvmStatic
        fun setDefaultAdapterPool(adapterPool: AdapterPool.() -> Unit) {
            this.adapterPool = adapterPool
        }
    }

    constructor(activity: Activity, contentAdapter: ContentAdapter<*>? = null) :
            this((activity.findViewById<View>(android.R.id.content) as ViewGroup).getChildAt(0),contentAdapter)

    init {
        adapterPool?.let { AdapterPool(this).apply(it) }
        parent = contentView.parent as ViewGroup?
        register(ViewType.CONTENT, contentAdapter ?: SimpleContentAdapter())
        setDecorAdapter(LinearDecorAdapter(listOf()))
    }

    fun setDecorAdapter(decorAdapter: DecorAdapter) {
        currentViewHolder = null
        if (parent != null) {
            val index = parent.indexOfChild(contentView)
            if (index >= 0) {
                parent.removeView(contentView)
            } else {
                parent.removeView(decorView)
                (contentView.parent as ViewGroup).removeView(contentView)
            }
            decorView = decorAdapter.createDecorView()
            parent.addView(decorView)
        } else {
            decorView = decorAdapter.createDecorView()
        }
        contentParent = decorAdapter.getContentParent(decorView)
        showView(ViewType.CONTENT)
    }

    fun setDecorHeader(vararg viewType: Any) {
        val views = mutableListOf<View>()
        for (t in viewType) {
            views.add(getViewHolder(t).rootView)
        }
        setDecorAdapter(LinearDecorAdapter(views))
    }

    fun addChildDecorAdapter(decorAdapter: DecorAdapter) {
        contentParent.removeView(currentViewHolder?.rootView)
        currentViewHolder = null
        val childDecorView = decorAdapter.createDecorView()
        contentParent.addView(childDecorView)
        contentParent = decorAdapter.getContentParent(childDecorView)
        showView(ViewType.CONTENT)
    }

    fun addChildDecorHeader(vararg viewTypes: Any) {
        val views = mutableListOf<View>()
        for(viewType in viewTypes) {
            views.add(getViewHolder(viewType).rootView)
        }
        addChildDecorAdapter(LinearDecorAdapter(views))
    }

    fun showView(viewType: Any) {
        if (currentViewHolder == null) {
            addView(viewType)
        } else {
            if (viewType != currentViewHolder!!.viewType && currentViewHolder!!.rootView.parent != null) {
                contentParent.removeView(currentViewHolder!!.rootView)
                addView(viewType)
            }
        }
    }

    fun showLoadingView() = showView(ViewType.LOADING)

    fun showContentView() = showView(ViewType.CONTENT)

    fun showErrorView() = showView(ViewType.ERROR)

    fun showEmptyView() = showView(ViewType.EMPTY)

    fun setOnReloadListener(onReloadListener: OnReloadListener) {
        this.onReloadListener = onReloadListener
    }

    fun setOnReloadListener(onReload: () -> Unit) = setOnReloadListener(object : OnReloadListener {
        override fun onReload() = onReload()
    })

    private fun addView(viewType: Any) {
        val viewHolder = getViewHolder(viewType)
        val rootView = viewHolder.rootView
        if (rootView.parent != null) {
            (rootView.parent as ViewGroup).removeView(rootView)
        }
        contentParent.addView(rootView)
        currentViewHolder = viewHolder
    }

    private fun getViewHolder(viewType: Any): ViewHolder {
        if (viewHolders[viewType] != null) {
            addViewHolder(viewType)
        }
        return viewHolders[viewType] as ViewHolder
    }

    private fun getViewType(targetAdapter: Adapter<*>): Any? {
        for (entry in adapters.entries) {
            if (entry.value == targetAdapter) {
                return entry.key
            }
        }
        return null
    }

    fun <T: Adapter<ViewHolder>> getAdapter(viewType: Any) = adapters[viewType] as T

    private fun addViewHolder(viewType: Any) {
        val adapter: Adapter<ViewHolder> = getAdapter(viewType)
        val viewHolder = if (adapter is ContentAdapter<*>) {
            adapter.onCreateViewHolder(contentView)
        } else {
            adapter.onCreateViewHolder(LayoutInflater.from(contentParent.context), contentParent)
        }

        viewHolder.viewType = viewType
        viewHolder.onReloadListener = onReloadListener
        viewHolders[viewType] = viewHolder
        adapter.onBindViewHolder(viewHolder)
        adapter.listener = this::notifyDataSetChanged
    }

    private fun notifyDataSetChanged(adapter: Adapter<ViewHolder>) = adapter.onBindViewHolder(getViewHolder(getViewType(adapter)!!))

    fun register(viewType: Any, adapter: Adapter<*>) {
        adapters[viewType] = adapter
    }



    abstract class Adapter<VH: ViewHolder> {
        internal lateinit var listener: (adapter: Adapter<ViewHolder>) -> Unit
        abstract fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup): VH
        abstract fun onBindViewHolder(holder: VH)

        fun notifyDataSetChanged() = listener.invoke(this as Adapter<ViewHolder>)
    }

    abstract class ContentAdapter<VH : ViewHolder> : Adapter<VH>() {
        override fun onCreateViewHolder(inflater: LayoutInflater, parent: ViewGroup) = onCreateViewHolder(View(parent.context))

        abstract fun onCreateViewHolder(contentView: View): VH
    }

    private class SimpleContentAdapter: ContentAdapter<ViewHolder>() {
        override fun onBindViewHolder(holder: ViewHolder)  = Unit

        override fun onCreateViewHolder(contentView: View): ViewHolder = ViewHolder(contentView)

    }
    
    open class ViewHolder(val rootView: View) {
        internal var viewType: Any? = null
        var onReloadListener: OnReloadListener? = null
            internal set

    }

    abstract class DecorAdapter {
        abstract fun onCreateDecorView(inflater: LayoutInflater): View

        abstract fun getContentParent(decorView: View): ViewGroup
    }

    private fun DecorAdapter.createDecorView() = onCreateDecorView(LayoutInflater.from(contentView.context)).also {decorView ->
        if (contentView.layoutParams != null) {
            decorView.layoutParams = contentView.layoutParams
        }
    }

    private class LinearDecorAdapter(private val views: List<View>): DecorAdapter() {
        override fun onCreateDecorView(inflater: LayoutInflater) = LinearLayout(inflater.context).apply {
            orientation = LinearLayout.VERTICAL
            for(view in views) {
                addView(view)
            }
        }

        override fun getContentParent(decorView: View) = decorView as ViewGroup
    }

    class AdapterPool internal constructor(private val helper: LoadingHelper) {
        fun register(viewType: Any, adapter: Adapter<*>) {
            helper.register(viewType, adapter)
        }
    }
    
    interface OnReloadListener {
        fun onReload()
    }

    enum class ViewType {
        TITLE, LOADING, CONTENT, ERROR, EMPTY
    }

}