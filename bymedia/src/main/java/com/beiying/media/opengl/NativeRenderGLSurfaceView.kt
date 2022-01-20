package com.beiying.media.opengl

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import com.beiying.media.opengl.render.GLRender
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_3D_MODEL
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_BASIC_LIGHTING
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_COORD_SYSTEM
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_DEPTH_TESTING
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_FBO_LEG
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_INSTANCING
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_KEY_SCRATCH_CARD
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_KEY_SHOCK_WAVE
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_KEY_TEXT_RENDER
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_KEY_UBO
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_KEY_VISUALIZE_AUDIO
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_MULTI_LIGHTS
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_PARTICLES
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_PBO
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_SKYBOX
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_STENCIL_TESTING
import com.beiying.media.opengl.render.GLRender.Companion.SAMPLE_TYPE_TRANS_FEEDBACK
import kotlin.math.max
import kotlin.math.min

class NativeRenderGLSurfaceView(context: Context, val render: GLRender, attributeset: AttributeSet?): GLSurfaceView(context, attributeset),
    ScaleGestureDetector.OnScaleGestureListener {
    constructor(context: Context, render: GLRender): this(context, render,null)

    lateinit var mScaleGestureDetector: ScaleGestureDetector
    var mRationWidth: Int = 0
    var mRationHeight: Int = 0
    var mPreScale: Float = 1.0f
    var mCurScale: Float = 1.0f

    var mPreviousY: Float = 0.0f
    var mPreviousX: Float = 0.0f
    var mXAngle: Int = 0 //绕X轴旋转度数
    var mYAngle: Int = 0 //绕Y轴旋转度数
    var mLastTouchTime: Long = 0
    val TOUCH_SCALE_FACTOR: Float = 180.0f / 320
    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 8)
        setRenderer(render)
        renderMode = RENDERMODE_WHEN_DIRTY

        mScaleGestureDetector = ScaleGestureDetector(context, this)
    }

    companion object {
        const val IMAGE_FORMAT_RGBA = 0x01
        const val IMAGE_FORMAT_NV21 = 0x02
        const val IMAGE_FORMAT_NV12 = 0x03
        const val IMAGE_FORMAT_I420 = 0x04
        const val IMAGE_FORMAT_YUYV = 0x05
        const val IMAGE_FORMAT_GARY = 0x06
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.pointerCount == 1) {
            consumeTouchEvent(event)
            var currentTouchTime = System.currentTimeMillis()
            if (currentTouchTime - mLastTouchTime > 200) {
                var x: Float = event.x
                var y: Float = event.y
                when(event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        var dx: Float = x - mPreviousX
                        var dy: Float = y - mPreviousY
                        mXAngle += (dy * TOUCH_SCALE_FACTOR).toInt()
                        mYAngle += (dx * TOUCH_SCALE_FACTOR).toInt()
                    }
                }
                mPreviousX = x
                mPreviousY = y
                when (render.getSampleType()) {
                    SAMPLE_TYPE_FBO_LEG, SAMPLE_TYPE_COORD_SYSTEM, SAMPLE_TYPE_BASIC_LIGHTING, SAMPLE_TYPE_TRANS_FEEDBACK,
                    SAMPLE_TYPE_MULTI_LIGHTS, SAMPLE_TYPE_DEPTH_TESTING, SAMPLE_TYPE_INSTANCING, SAMPLE_TYPE_STENCIL_TESTING,
                    SAMPLE_TYPE_PARTICLES, SAMPLE_TYPE_SKYBOX, SAMPLE_TYPE_3D_MODEL, SAMPLE_TYPE_PBO, SAMPLE_TYPE_KEY_VISUALIZE_AUDIO,
                    SAMPLE_TYPE_KEY_UBO, SAMPLE_TYPE_KEY_TEXT_RENDER -> {
                        render.updateTransformMatrix(mXAngle.toFloat(),
                            mYAngle.toFloat(), mCurScale, mCurScale)
                        requestRender()
                    }
                    else -> {}
                }
            }
        } else {
            mScaleGestureDetector.onTouchEvent(event)
        }
        return true
    }

    fun consumeTouchEvent(event: MotionEvent) {
        var touchX: Float = -1f
        var touchY: Float = -1f
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
            }
            MotionEvent.ACTION_UP -> {
                touchX = -1f
                touchY = -1f
                if (render.getSampleType() == SAMPLE_TYPE_KEY_SHOCK_WAVE) {
                    touchX = event.x
                    touchY = event.y
                    render.setTouchLoc(touchX, touchY)
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                touchX = -1f
                touchY = -1f
            }
            else -> {}
        }
        when(render.getSampleType()) {
            SAMPLE_TYPE_KEY_SCRATCH_CARD -> {
                render.setTouchLoc(touchX, touchY)
                requestRender()
            }
        }
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        when (render.getSampleType()) {
            SAMPLE_TYPE_COORD_SYSTEM, SAMPLE_TYPE_BASIC_LIGHTING, SAMPLE_TYPE_INSTANCING,
            SAMPLE_TYPE_3D_MODEL, SAMPLE_TYPE_KEY_VISUALIZE_AUDIO, SAMPLE_TYPE_KEY_TEXT_RENDER -> {
                val preSpan = detector.previousSpan
                val curSpan = detector.currentSpan
                mCurScale = if (curSpan < preSpan) {
                    mPreScale - (preSpan - curSpan) / 200
                } else {
                    mPreScale + (curSpan - preSpan) / 200
                }
                mCurScale = max(0.05f, min(mCurScale, 80.0f))
                render.updateTransformMatrix(mXAngle.toFloat(), mYAngle.toFloat(), mCurScale, mCurScale)
                requestRender()
            }
            else -> {}
        }
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector?) {
        mPreScale = mCurScale
        mLastTouchTime = System.currentTimeMillis()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)

        if (0 == mRationWidth || 0 == mRationHeight) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * mRationWidth / mRationHeight) {
                setMeasuredDimension(width, width * mRationHeight / mRationWidth)
            } else {
                setMeasuredDimension(height * mRationWidth / mRationHeight, height)
            }
        }

    }

}