package com.beiying.lopnor.base.recylerview

import android.app.Activity
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView


abstract class BaseAdapter<T>(private val activity: Activity, private var list: MutableList<T>) : RecyclerView.Adapter<BaseViewHolder>() {
    /**
     * 头部的tag
     */
    private val headerTags: MutableList<Int> = mutableListOf()

    /**
     * 头部的View集合
     */
    private val headViews: MutableList<View> = mutableListOf()

    /**
     * 底部的View集合
     */
    private val footViews: MutableList<View> = mutableListOf()

    /**
     * 底部的tag
     */
    private val footTags: MutableList<Int> = mutableListOf()

    //数据为空的Type
    private val EMPTYVIEWTYPE = -10000

    //上拉加载Type
    private val LOADMORETYPE = -40000

    /**
     * 标准头部viewType值
     */
    private val headerInt = 8000

    /**
     * 标准头部viewType值
     */
    private val footInt = 9000

    /**
     * 没有数据时的View,上拉加载更多的View
     */
    private var emptyView: View? = null,

    /**
     * 没有数据时的View,上拉加载更多的View
     */
    private var loadMoreView: View? = null

    /**
     * 是否需要上拉加载更多
     */
    private var endLoadMore = false

    /**
     * 开启上拉加载更多监听 用于上拉加载完成没有更多数据
     */
    private var openLoadMoreListener = false

    /**
     * 是否打开emptyView
     */
    private var openEmptyView = false

    //当前的位置
    protected var mCurrentPosition = -1

    /**
     * handler处理延时操作
     */
    private val handler: Handler = Handler()

    /**
     * 需要监听的子View的数组
     */
    private var childId: IntArray? = null

    private var isListenerChildLongClick = false

    private var iOnClickListener: AdapterItemOnClickListener? = null

    private var iOnLongClickListener: AdapterItemLongClickListener? = null

    private var iOnChildClickListener: AdapterChildItemOnClickListener? = null

    private var iOnChildLongClickListener: AdapterChildItemLongClickListener? = null

    private var mLoadMoreResultListener: AdapterLoadMoreClickListener? = null

    //监听加载更多的事件
    private var iLoadMoreListener: AdapterLoadMoreListener? = null

    init {
        setDefaultLoadMoreView();
    }





    /**
     * 总View个数
     *
     * @return
     */
    open fun totalItemCount(): Int {
        return headViews.size + list.size + footViews.size + loadMoreViewCount()
    }

    /**
     * 上拉加载更多View的个数
     *
     * @return
     */
    open fun loadMoreViewCount(): Int {
        return if (hasLoadMore()) 1 else 0
    }

    /**
     * 头部和list数据的个数
     *
     * @return
     */
    open fun headAndDataCount(): Int {
        return headViews.size + list.size
    }

    /**
     * 获取头部视图的个数
     *
     * @return
     */
    open fun headerCounts(): Int {
        return headViews.size
    }

    /**
     * 是否为空
     */
    open fun isEmpty(): Boolean {
        return if (emptyView != null && openEmptyView) {
            if (list != null) {
                list.size == 0
            } else {
                true
            }
        } else false
    }

    /**
     * 没有数据时显示空view
     *
     * @param openEmptyView 是否开启空view
     */
    open fun setOpenEmptyView(openEmptyView: Boolean) {
        this.openEmptyView = openEmptyView
    }

    /**
     * 没有数据时显示空view
     *
     * @param openEmptyView 是否开启空view
     * @param openNotify    是否更新
     */
    open fun setOpenEmptyView(
        openEmptyView: Boolean,
        openNotify: Boolean
    ) {
        if (openNotify) {
            if (this.openEmptyView != openEmptyView) {
                this.openEmptyView = openEmptyView
                notifyDataSetChanged()
            }
        }
    }

    /**
     * 添加头部的视图
     *
     * @param view
     * @param isNotify 是否通知插入一个条目
     */
    open fun addHeaderView(view: View?, isNotify: Boolean) {
        if (view == null) {
            throw NullPointerException("the header view can not be null")
        }
        headViews.add(view)
        if (isNotify) {
            notifyDataSetChanged()
        }
    }

    /**
     * 删除一个视图
     *
     * @param position
     */
    open fun removeHeaderView(position: Int, isNotify: Boolean) {
        if (headViews.size > position) {
            headViews.removeAt(position)
            headerTags.removeAt(position)
            if (isNotify) {
                notifyDataSetChanged()
            }
        }
    }

    /**
     * 添加底部的视图
     *
     * @param view
     * @param isNotify 是否通知插入一个条目
     */
    open fun addFootView(view: View?, isNotify: Boolean) {
        if (view == null) {
            throw NullPointerException("the header view can not be null")
        }
        footViews.add(view)
        if (isNotify) {
            notifyDataSetChanged()
        }
    }

    /**
     * 删除一个视图
     *
     * @param position
     */
    open fun removeFootView(position: Int, isNotify: Boolean) {
        if (footViews.size > position) {
            footViews.removeAt(position)
            footTags.removeAt(position)
            if (isNotify) {
                notifyDataSetChanged()
            }
        }
    }

    /**
     * 删除所有footView
     */
    open fun removeAllHeadView() {
        headViews.clear()
        headerTags.clear()
        notifyDataSetChanged()
    }

    /**
     * 删除所有footView
     */
    open fun removeAllFootView() {
        footViews.clear()
        footTags.clear()
        notifyDataSetChanged()
    }

    /**
     * 设置空View
     *
     * @param view
     */
    open fun setEmptyView(view: View?) {
        if (view != null) emptyView = view
    }

    /**
     * 设置上拉加载View
     *
     * @param view
     */
    open fun setLoadMoreView(view: View?) {
        if (view != null) loadMoreView = view
    }

    open fun setDefaultLoadMoreView() {
        if (null != activity) {
            loadMoreView = LoadMoreView(activity)
        }
    }

    /**
     * 当前位置是否是上拉加载
     *
     * @param position
     * @return
     */
    open fun isShowLoadMore(position: Int): Boolean {
        return hasLoadMore() && position >= itemCount - 1
    }

    /**
     * 符合上拉加载的条件
     *
     * @return
     */
    open fun hasLoadMore(): Boolean {
        return loadMoreView != null && endLoadMore
    }


    override fun getItemCount(): Int {
        return if (isEmpty()) {
            headViews.size + 1 + footViews.size
        } else {
            totalItemCount()
        }
    }

    override fun getItemViewType(position: Int): Int {
        mCurrentPosition = position
        //如果是头部
        if (position < headViews.size) {
            headerTags.add(position + headerInt)
            return position + headerInt
        }
        //如果是底部
        if (position > headAndDataCount() - 1 && position < totalItemCount() - loadMoreViewCount()) {
            footTags.add(position + footInt)
            return position + footInt
        }
        //如果没有数据
        if (isEmpty()) {
            return EMPTYVIEWTYPE
        }
        //如果是上拉加载
        return if (isShowLoadMore(position)) {
            LOADMORETYPE
        } else super.getItemViewType(position)
    }

    open fun onCreateViewHolder(viewGroup: ViewGroup?, viewType: Int): BaseViewHolder? {
        var view: View? = when {
            viewType == EMPTYVIEWTYPE -> {
                emptyView
            }
            headerTags.contains(viewType) -> {
                headViews[viewType - headerInt]
            }
            footTags.contains(viewType) -> {
                footViews[viewType - footInt - headAndDataCount()]
            }
            viewType == LOADMORETYPE -> {
                loadMoreView
            }
            else -> {
                LayoutInflater.from(activity).inflate(getLayoutViewId(viewType), viewGroup, false)
            }
        }
        val viewHolder = BaseViewHolder(view)

        //视图被创建的时候调用
        viewCreated(viewHolder, viewType)
        return viewHolder
    }

    /**
     * 视图被创建的时候调用
     *
     * @param viewHolder
     * @param viewType
     */
    open fun viewCreated(viewHolder: BaseViewHolder, viewType: Int) {}

    /**
     * @param viewType 返回值就是根据这个值进行判断返回的对头部不起作用
     * @return
     */
    protected abstract fun getLayoutViewId(viewType: Int): Int

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        if (getItemViewType(position) == EMPTYVIEWTYPE) {
            return
        }
        if (position < headViews.size) {
            //如果是头部处理头部数据
            convertHeadViewData(holder, position)
        } else if (position > headAndDataCount() - 1 && position < totalItemCount() - loadMoreViewCount()) {
            //如果是底部处理底部数据
            convertFootViewData(holder, position - headAndDataCount() - 1)
        } else if (isShowLoadMore(position)) {
            if (null != loadMoreView) {
                if (loadMoreView is LoadMoreView) {
                    (loadMoreView as LoadMoreView).setAdapterLoadMoreClickListener(
                        mLoadMoreResultListener
                    )
                }
            }
            if (openLoadMoreListener) {
                //如果是上拉加载
                if (iLoadMoreListener != null) {
                    //防止Cannot call this method while RecyclerView is computing a layout or scrolling
                    handler.postDelayed(Runnable { iLoadMoreListener?.onLoadMore() }, 800)
                }
            }
        } else {
            //如果是数据的话
            val dataPosition = position - headViews.size
            if (null != iOnClickListener) {
                holder.itemView.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View) {
                        iOnClickListener?.onClick(v, dataPosition)
                    }
                })
            }
            if (null != iOnLongClickListener) {
                holder.itemView.setOnLongClickListener(object : View.OnLongClickListener {
                    override fun onLongClick(v: View?): Boolean {
                        iOnLongClickListener?.onClick(v, dataPosition)
                        return false
                    }
                })
            }


            //监听子View
            if (null != childId) {
                if (childId!!.size > 0) {
                    for (integer in childId!!) {
                        if (null != holder.getView(integer)) {
                            if (null != iOnChildClickListener) {
                                holder.getView(integer)
                                    .setOnClickListener(object : View.OnClickListener {
                                        override fun onClick(v: View) {
                                            iOnChildClickListener?.onClick(v, dataPosition)
                                        }
                                    })
                            }
                            if (null != iOnChildClickListener && isListenerChildLongClick) {
                                holder.getView(integer)
                                    .setOnLongClickListener(object : View.OnLongClickListener {
                                        override fun onLongClick(v: View): Boolean {
                                            iOnChildLongClickListener?.onClick(v, dataPosition)
                                            return false
                                        }
                                    })
                            }
                        }
                    }
                }
            }
            convertData(holder, list.get(dataPosition), position)
        }
    }

    /**
     * 设置动态监听子View
     *
     * @param iOnChildClickListener     子View 点击监听
     * @param iOnChildLongClickListener 子View 按钮长按监听
     * @param listenerId                子View的Id
     */
    open fun setNeedListenerChildId(
        iOnChildClickListener: AdapterChildItemOnClickListener?
        ,
        iOnChildLongClickListener: AdapterChildItemLongClickListener?,
        listenerId: IntArray?
    ) {
        isListenerChildLongClick = true
        childId = listenerId
        this.iOnChildClickListener = iOnChildClickListener
        this.iOnChildLongClickListener = iOnChildLongClickListener
    }

    /**
     * 设置动态监听子View
     *
     * @param iOnChildClickListener
     * @param listenerId
     */
    open fun setNeedListenerChildId(
        iOnChildClickListener: AdapterChildItemOnClickListener?,
        listenerId: IntArray?
    ) {
        childId = listenerId
        this.iOnChildClickListener = iOnChildClickListener
    }

    /**
     * 实现列表的显示
     *
     * @param holder   RecycleView的ViewHolder
     * @param entity   实体对象
     * @param position 当前的下标
     */
    protected abstract fun convertData(
        holder: BaseViewHolder?,
        entity: T,
        position: Int
    )

    /**
     * 处理HeadView数据
     *
     * @param holder   RecycleView的ViewHolder
     * @param position 当前的下标
     */
    protected open fun convertHeadViewData(
        holder: BaseViewHolder?,
        position: Int
    ) {
    }

    /**
     * 处理View数据
     *
     * @param holder   RecycleView的ViewHolder
     * @param position 当前的下标
     */
    protected open fun convertFootViewData(
        holder: BaseViewHolder?,
        position: Int
    ) {
    }

    /**
     * item 点击监听
     *
     * @param iOnClickListener
     */
    open fun setAdapterItemOnClickListener(iOnClickListener: AdapterItemOnClickListener?) {
        this.iOnClickListener = iOnClickListener
    }

    /**
     * item 长按监听
     *
     * @param iOnLongClickListener
     */
    open fun setiOnLongClickListener(iOnLongClickListener: AdapterItemLongClickListener?) {
        this.iOnLongClickListener = iOnLongClickListener
    }

    /**
     * 上拉加载监听
     *
     * @param iLoadMoreListener
     */
    open fun setAdapterLoadMoreListener(iLoadMoreListener: AdapterLoadMoreListener?) {
        this.iLoadMoreListener = iLoadMoreListener
    }

    /**
     * 添加数据
     *
     * @param list
     */
    open fun addData(list: List<T>?) {
        if (null != list) {
            this.list.addAll(list)
            notifyDataSetChanged()
        }
    }

    /**
     * 更新数据
     *
     * @param list
     */
    open fun upData(list: MutableList<T>?) {
        if (null != list) {
            this.list = list
            notifyDataSetChanged()
        }
    }

    /**
     * 获取列表数据个数（不包含head和foot）
     *
     * @return
     */
    open fun getListCount(): Int {
        return if (null != list) {
            list.size
        } else 0
    }

    /**
     * 移除指定item
     *
     * @param position
     */
    open fun removeItem(position: Int) {
        if (null != this.list) {
            this.list.removeAt(position)
            notifyItemRemoved(position)
            notifyDataSetChanged()
        }
    }

    /**
     * 移除全部item
     *
     * @param
     */
    open fun removeAllItem() {
        if (null != this.list) {
            for (i in this.list.indices) {
                this.list.removeAt(i)
                notifyItemRemoved(i)
            }
        }
        notifyDataSetChanged()
    }

    /**
     * 关闭上拉加载未更新适配器
     */
    open fun endLoadMore() {
        endLoadMore = false
        openLoadMoreListener = false
    }

    /**
     * 打开上拉加载未更新适配器
     */
    open fun startLoadMore() {
        if (null != loadMoreView) {
            if (loadMoreView is LoadMoreView) {
                (loadMoreView as LoadMoreView).showLoadMore()
            }
        }
        endLoadMore = true
        openLoadMoreListener = true
    }

    //上拉数据加载完成显示loadmoreview,但关闭监听
    open fun finishLoadMore() {
        if (null != loadMoreView) {
            if (loadMoreView is LoadMoreView) {
                (loadMoreView as LoadMoreView).showLoadFinish()
            }
        }
        endLoadMore = true
        openLoadMoreListener = false
    }

    /**
     * 上拉数据加载错误显示loadmoreview,但关闭监听
     */
    open fun errorLoadMore() {
        if (null != loadMoreView) {
            if (loadMoreView is LoadMoreView) {
                (loadMoreView as LoadMoreView).showLoadError()
            }
        }
        endLoadMore = true
        openLoadMoreListener = false
    }

    /**
     * 设置点击上拉加载View监听
     *
     * @param mLoadMoreResultListener
     */
    open fun setLoadMoreResultListener(mLoadMoreResultListener: AdapterLoadMoreClickListener?) {
        this.mLoadMoreResultListener = mLoadMoreResultListener
    }
}