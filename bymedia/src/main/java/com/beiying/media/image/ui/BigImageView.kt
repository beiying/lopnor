package com.beiying.media.image.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Scroller
import java.io.InputStream
import java.lang.Exception

class BigImageView : View, GestureDetector.OnGestureListener, View.OnTouchListener,
    GestureDetector.OnDoubleTapListener {
    lateinit var showRect: Rect
    lateinit var options: BitmapFactory.Options
    lateinit var gestureDetector: GestureDetector
    lateinit var scaleGestureDetector: ScaleGestureDetector
    lateinit var scroller: Scroller
    var bitmap: Bitmap? = null
    var bitmapDecoder: BitmapRegionDecoder? = null
    var scaleMatrix: Matrix = Matrix()
    var imageWidth: Int = 0
    var imageHeight: Int = 0
    var viewWidth: Int = 0
    var viewHeight: Int = 0
    var showScale: Float = 0f  //图片大小与显示大小的比例
    var originalScale: Float = 0f  //正常显示状态下的图片大小与显示大小的比例
    var zoomScale: Float = 0f //双击图片后的放大比例

    constructor(context: Context): this(context, null)
    constructor(context: Context, attributeSet: AttributeSet?): this(context, attributeSet, 0)
    constructor(context: Context, attributeSet: AttributeSet?, defStyleAttr: Int): super(context, attributeSet, defStyleAttr) {
        showRect = Rect()
        options = BitmapFactory.Options()
        //滑动手势
        gestureDetector = GestureDetector(context, this)
        gestureDetector.setOnDoubleTapListener(this)
        //缩放手势
        scaleGestureDetector = ScaleGestureDetector(context, ScaleGestureListener())

        scroller = Scroller(context)
        setOnTouchListener(this)
    }

    //第二步：设置图片
    fun setImage(inputStream: InputStream) {
        options.inJustDecodeBounds = true

        BitmapFactory.decodeStream(inputStream, null, options)
        imageWidth = options.outWidth
        imageHeight = options.outHeight

        //开启Bitmap复用
        options.inMutable = true
        //设置图片格式
        options.inPreferredConfig = Bitmap.Config.RGB_565

        options.inJustDecodeBounds = false
        try {
            bitmapDecoder = BitmapRegionDecoder.newInstance(inputStream, false)
        } catch(e: Exception) {
            e.printStackTrace()
        }
        requestLayout()
    }

    //第三步：测量
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = measuredWidth
        viewHeight = measuredHeight

        //根据视图宽高确定加载图片的区域
        showRect.left = 0
        showRect.top = 0
        showRect.right = Math.min(imageWidth, viewWidth)
        showRect.bottom = Math.min(imageHeight, viewHeight)
        Log.e("liuyu", "onDraw viewWith: $viewWidth, rectWidth:$imageWidth")

//        //根据view的宽度计算图片的缩放因子
//        showScale = viewWidth / imageWidth.toFloat()
//        showRect.bottom = (viewHeight / showScale).roundToInt()
        Log.e("liuyu", "onMeasure right:${showRect.right}, bottom: ${showRect.bottom}")
        originalScale = viewWidth / imageWidth.toFloat()
        showScale = originalScale
    }

    //第四步：绘制
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //复用Bitmap内存，即将解码的Bitmap尺寸必须小于复用的Bitmap尺寸
        options.inBitmap = bitmap
        bitmap = bitmapDecoder?.decodeRegion(showRect, options)
        Log.e("liuyu", "onDraw viewWith: $viewWidth, rectWidth:${viewWidth / showRect.width().toFloat()}}")
        scaleMatrix.setScale(viewWidth / showRect.width().toFloat(), viewWidth / showRect.width().toFloat())
        bitmap?.let { canvas.drawBitmap(it, scaleMatrix, null) }
    }

    //第五步：处理touch事件
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        Log.e("liuyu", "x:${event.rawX}")
        gestureDetector.onTouchEvent(event)
        scaleGestureDetector.onTouchEvent(event)
        return true
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    //第六步：处理按下事件
    override fun onDown(e: MotionEvent): Boolean {
        Log.e("liuyu", "onDown x= ${e.rawX}")
        if (!scroller.isFinished) {
            scroller.forceFinished(true)
        }
        return true
    }

    /**
     * 第七步：处理滑动
     * 上下滑动的时候需要改变图片的加载区域
     * */
    override fun onScroll(
        e1: MotionEvent,//开始的手指按下事件
        e2: MotionEvent,//当前事件
        distanceX: Float,
        distanceY: Float
    ): Boolean {
        Log.e("liuyu", "onScroll Y= $distanceY")
        showRect.offset(distanceX.toInt(), distanceY.toInt())
        Log.e("liuyu", "before onScroll left= ${showRect.left}, right=${showRect.right}, width=${showRect.width()}")
        //处理临界状态，即顶部和底部
        if (showRect.bottom > imageHeight) {
            showRect.bottom = imageHeight
            showRect.top = imageHeight - (viewHeight / showScale).toInt()
        }
        if (showRect.top < 0) {
            showRect.top = 0
            showRect.bottom = (viewHeight / showScale).toInt()
        }
        if (showRect.right > imageWidth) {
            showRect.right = imageWidth
            showRect.left = imageWidth - (viewWidth / showScale).toInt()
        }
        if (showRect.left < 0) {
            showRect.left = 0
            showRect.right = (viewWidth / showScale).toInt()
        }
        Log.e("liuyu", "after onScroll left= ${showRect.left}, right=${showRect.right}, width=${showRect.width()}")
        invalidate()
        return false
    }

    /**
     * 第八步：处理惯性
     * 注意： 图片加载区域的变化方向刚好与滑动方向相反
     * */
    override fun onFling(
        e1: MotionEvent,
        e2: MotionEvent,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        Log.e("liuyu", "onFling Y= $velocityY")
        scroller.fling(showRect.left, showRect.top, -(velocityX.toInt()), -(velocityY.toInt()), 0, imageWidth - (viewWidth / showScale).toInt(), 0, imageHeight - (viewHeight / showScale).toInt())
        return false
    }

    /**
     * 处理scroller的计算结果
     * */
    override fun computeScroll() {
        if(scroller.isFinished) return
        if (scroller.computeScrollOffset()) {
            Log.e("liuyu", "computeScroll Y= ${scroller.currY}")
//            showRect.left = scroller.currX
//            showRect.right = showRect.left + (viewWidth / showScale).toInt()
            showRect.top = scroller.currY
            showRect.bottom = showRect.top + (viewHeight / showScale).toInt()
            invalidate()
        }
    }

    override fun onLongPress(e: MotionEvent) {

    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (showScale < originalScale * 1.5) {
            showScale = originalScale * 3
        } else{
            showScale = originalScale
        }
        showRect.right = showRect.left + (viewWidth / showScale).toInt()
        showRect.bottom = showRect.top + (viewHeight / showScale).toInt()
        if (showRect.bottom > imageHeight) {
            showRect.bottom = imageHeight
            showRect.top = imageHeight - (viewHeight / showScale).toInt()
        }
        if (showRect.top < 0) {
            showRect.top = 0
            showRect.bottom = (viewHeight / showScale).toInt()
        }
        if (showRect.right > imageWidth) {
            showRect.right = imageWidth
            showRect.left = imageWidth - (viewWidth / showScale).toInt()
        }
        if (showRect.left < 0) {
            showRect.left = 0;
            showRect.right = (viewWidth / showScale).toInt()
        }
        invalidate()
        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return false
    }

    inner class ScaleGestureListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var scale: Float = showScale
            Log.e("liuyu", "onScale scaleFactor= ${detector.scaleFactor}")
            scale += detector.scaleFactor - 1
            if (scale <= originalScale) {
                scale = originalScale
            } else if (scale > originalScale * 5) {
                scale = originalScale * 5
            }
            showRect.right = showRect.left + (viewWidth / scale).toInt()
            showRect.bottom = showRect.bottom + (viewHeight / scale).toInt()
            showScale = scale
            return true
        }

    }

}