package com.beiying.lopnor.base.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.DisplayMetrics
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ListView
import java.util.*
import kotlin.collections.ArrayDeque

object ViewUtil {
    fun getPhoneWidth(context: Context): Int {
        val windowManager = context.applicationContext
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metric = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager.defaultDisplay.getRealMetrics(metric)
        } else {
            windowManager.defaultDisplay.getMetrics(metric)
        }
        return metric.widthPixels
    }

    fun getPhoneHeight(context: Context): Int {
        val windowManager = context.applicationContext
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metric = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            windowManager.defaultDisplay.getRealMetrics(metric)
        } else {
            windowManager.defaultDisplay.getMetrics(metric)
        }
        return metric.heightPixels
    }

    fun getPhoneRatio(context: Context): Float {
        return getPhoneHeight(context).toFloat() / getPhoneWidth(context).toFloat()
    }

    fun getScreenPoint(view: View?): Point? {
        if (view == null) {
            return null
        }
        val pos = IntArray(2)
        view.getLocationOnScreen(pos)
        return Point(pos[0], pos[1])
    }

    fun getPointOfParent(view: View?): Point? {
        if (view == null) {
            return null
        }
        val pos = IntArray(2)
        view.getLocationInWindow(pos)
        return Point(pos[0], pos[1])
    }

    fun getDensity(context: Context): Float {
        return context.resources.displayMetrics.density
    }

    fun getScreenWidth(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return metrics.widthPixels
    }

    fun getScreenHeight(context: Context): Int {
        val metrics = context.resources.displayMetrics
        return metrics.heightPixels
    }

    /**
     * 获取一定比例的屏幕高度
     */
    fun getScreenRatioHeight(context: Context, percent: Double): Int {
        val screenHeight: Int = getScreenHeight(context)
        return (screenHeight * percent).toInt()
    }

    fun dipToPx(context: Context, dpValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun pxToDip(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun convertSpToPixels(context: Context, sp: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            sp,
            context.resources.displayMetrics
        )
    }

    fun measure(view: View, width: Int, height: Int) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(
                width,
                View.MeasureSpec.EXACTLY
            ),
            View.MeasureSpec.makeMeasureSpec(
                height,
                View.MeasureSpec.EXACTLY
            )
        )
    }

    fun layout(view: View, offsetX: Int, offsetY: Int) {
        view.layout(
            offsetX,
            offsetY,
            offsetX + view.measuredWidth,
            offsetY + view.measuredHeight
        )
    }

    fun convert2Bitmap(view: View?): Bitmap? {
        if (view == null) {
            return null
        }
        val bitmap = Bitmap.createBitmap(
            view.width, view.height,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    fun drawable2Bitmap(drawable: Drawable): Bitmap? {
        var bitmap: Bitmap? = null
        if (drawable is BitmapDrawable) {
            val bitmapDrawable = drawable
            if (bitmapDrawable.bitmap != null) {
                bitmap =
                    bitmapDrawable.bitmap.copy(Bitmap.Config.RGB_565, true)
            }
        } else {
            bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
                Bitmap.createBitmap(
                    1,
                    1,
                    Bitmap.Config.ARGB_8888
                ) // Single color bitmap will be created of 1x1 pixel
            } else {
                Bitmap.createBitmap(
                    drawable.intrinsicWidth,
                    drawable.intrinsicHeight,
                    Bitmap.Config.ARGB_8888
                )
            }
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        }
        return bitmap
    }

    fun getTotalHeightofListView(listView: ListView): Int {
        val mAdapter = listView.adapter ?: return 0
        var totalHeight = 0
        for (i in 0 until mAdapter.count) {
            val mView = mAdapter.getView(i, null, listView)
            mView.measure(
                View.MeasureSpec.makeMeasureSpec(
                    0,
                    View.MeasureSpec.UNSPECIFIED
                ),
                View.MeasureSpec.makeMeasureSpec(
                    0,
                    View.MeasureSpec.UNSPECIFIED
                )
            )
            //mView.measure(0, 0);
            totalHeight += mView.measuredHeight
        }
        val params = listView.layoutParams
        params.height = totalHeight + listView.dividerHeight * (mAdapter.count - 1)
        listView.layoutParams = params
        listView.requestLayout()
        return totalHeight
    }

    fun getStatusbarHeight(context: Context): Int {
        var statusbarHeight = 0
        val resourceId =
            context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusbarHeight = context.resources.getDimensionPixelSize(resourceId)
        }
        return if (statusbarHeight > 0) {
            statusbarHeight
        } else statusbarHeight
    }

    fun checkZoomLevels(
        minZoom: Float,
        midZoom: Float,
        maxZoom: Float
    ) {
        require(minZoom < midZoom) { "Minimum zoom has to be less than Medium zoom. Call setMinimumZoom() with a more appropriate value" }
        require(midZoom < maxZoom) { "Medium zoom has to be less than Maximum zoom. Call setMaximumZoom() with a more appropriate value" }
    }

    fun hasDrawable(imageView: ImageView): Boolean {
        return imageView.drawable != null
    }

    fun isSupportedScaleType(scaleType: ImageView.ScaleType?): Boolean {
        if (scaleType == null) {
            return false
        }
        when (scaleType) {
            ImageView.ScaleType.MATRIX -> throw IllegalStateException("Matrix scale type is not supported")
        }
        return true
    }

    fun getPointerIndex(action: Int): Int {
        return action and MotionEvent.ACTION_POINTER_INDEX_MASK shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
    }

    /**
     * 获取指定类型的子View
     *
     * @param group viewGroup
     * @param cls   如：RecyclerView.class
     * @param <T>
     * @return 指定类型的View
    </T> */
    @ExperimentalStdlibApi
    fun <T> findTypeView(group: ViewGroup, cls: Class<T>): T? {
        if (group == null) {
            return null
        }
        val deque: ArrayDeque<View> = ArrayDeque()
        deque.add(group)
        while (!deque.isEmpty()) {
            val node: View = deque.removeFirst()
            if (cls.isInstance(node)) {
                return cls.cast(node)
            } else if (node is ViewGroup) {
                val container: ViewGroup = node as ViewGroup
                var i = 0
                val count: Int = container.childCount
                while (i < count) {
                    deque.add(container.getChildAt(i))
                    i++
                }
            }
        }
        return null
    }
}