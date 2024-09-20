package com.bird2fish.birdtalksdk.widgets

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.util.AttributeSet
import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bird2fish.birdtalksdk.R

/**
 * 该类用于显示圆形进度指示器（无限循环的加载进度条）。
 * 它匹配 SwipeRefreshLayout 中的进度条样式，并支持夜间模式。
 * 这个类是基于 androidx.swiperefreshlayout.widget.circleimageview.java 修改而来。
 */
class CircleProgressView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    // 最小显示时间(ms)
    private val MIN_SHOW_TIME = 300
    // 最小延迟时间(ms)
    private val MIN_DELAY = 500
    // 阴影半径和阴影提升值
    private val SHADOW_RADIUS = 3.5f
    private val SHADOW_ELEVATION = 4

    // 动画缩小持续时间(ms)
    private val SCALE_DOWN_DURATION = 150
    private var mShadowRadius = 0
    private var mStartTime: Long = -1
    private var mPostedHide = false
    private var mPostedShow = false
    private var mDismissed = false
    private var mMediumAnimationDuration = 0
    private lateinit var mProgress: CircularProgressDrawable

    // 动画监听器
    private val mProgressStartListener = object : AnimationEndListener() {
        override fun onAnimationEnd(animation: Animation?) {
            // mProgressView 已经可见，启动加载动画
            mProgress.start()
        }
    }

    private val mProgressStopListener = object : AnimationEndListener() {
        override fun onAnimationEnd(animation: Animation?) {
            clearAnimation()
            mProgress.stop()
            visibility = View.GONE
            setAnimationProgress(0f)
        }
    }

    private var mListener: Animation.AnimationListener? = null

    // 延迟隐藏的任务
    private val mDelayedHide = Runnable {
        mPostedHide = false
        mStartTime = -1
        stop()
    }

    // 延迟显示的任务
    private val mDelayedShow = Runnable {
        mPostedShow = false
        if (!mDismissed) {
            mStartTime = System.currentTimeMillis()
            start()
        }
    }

    init {
        init(context)
    }

    private fun init(context: Context) {
        val density = resources.displayMetrics.density
        val bgColor = ContextCompat.getColor(context, R.color.circularProgressBg)
        val fgColor = ContextCompat.getColor(context, R.color.circularProgressFg)

        mShadowRadius = (density * SHADOW_RADIUS).toInt()

        // 圆形背景
        val circle = ShapeDrawable(OvalShape())
        ViewCompat.setElevation(this, SHADOW_ELEVATION * density)
        circle.paint.color = bgColor
        ViewCompat.setBackground(this, circle)

        // 获取系统的中等动画时间
        mMediumAnimationDuration = resources.getInteger(android.R.integer.config_mediumAnimTime)

        // 初始化 CircularProgressDrawable 作为进度条
        mProgress = CircularProgressDrawable(context).apply {
            setStyle(CircularProgressDrawable.DEFAULT)
            setBackgroundColor(bgColor)
            setColorSchemeColors(fgColor)
        }
        setImageDrawable(mProgress)
    }

    /**
     * 隐藏进度条。如果进度条已经显示，确保显示时间超过最小显示时间。
     * 如果进度条还没显示，则取消显示。
     */
    @Synchronized
    fun hide() {
        mDismissed = true
        removeCallbacks(mDelayedShow)
        mPostedShow = false
        val diff = System.currentTimeMillis() - mStartTime
        if (diff >= MIN_SHOW_TIME || mStartTime == -1L) {
            if (visibility == View.VISIBLE) {
                stop()
            }
        } else if (!mPostedHide) {
            mPostedHide = true
            postDelayed(mDelayedHide, MIN_SHOW_TIME - diff)
        }
    }

    /**
     * 延迟显示进度条。如果在延迟期间调用了 hide()，则进度条不会显示。
     */
    @Synchronized
    fun show() {
        mStartTime = -1
        mDismissed = false
        removeCallbacks(mDelayedHide)
        mPostedHide = false
        if (!mPostedShow) {
            postDelayed(mDelayedShow, MIN_DELAY.toLong())
            mPostedShow = true
        }
    }

    /**
     * 立即开始进度条动画：从 0 缩放到 1，然后启动加载动画。
     */
    private fun start() {
        visibility = View.VISIBLE
        val scale = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                setAnimationProgress(interpolatedTime)
            }
        }
        scale.duration = mMediumAnimationDuration.toLong()
        setAnimationListener(mProgressStartListener)
        clearAnimation()
        startAnimation(scale)
    }

    /**
     * 立即停止进度条动画：从 1 缩放到 0。
     */
    private fun stop() {
        val down = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                setAnimationProgress(1 - interpolatedTime)
            }
        }
        down.duration = SCALE_DOWN_DURATION.toLong()
        setAnimationListener(mProgressStopListener)
        clearAnimation()
        startAnimation(down)
    }

    fun setAnimationListener(listener: Animation.AnimationListener?) {
        mListener = listener
    }

    override fun onAnimationStart() {
        super.onAnimationStart()
        mListener?.onAnimationStart(animation)
    }

    override fun onAnimationEnd() {
        super.onAnimationEnd()
        mListener?.onAnimationEnd(animation)
    }

    override fun setBackgroundColor(color: Int) {
        if (background is ShapeDrawable) {
            (background as ShapeDrawable).paint.color = color
        }
    }

    // 设置动画进度
    private fun setAnimationProgress(progress: Float) {
        scaleX = progress
        scaleY = progress
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(mDelayedHide)
        removeCallbacks(mDelayedShow)
    }

    // 一个简化的动画结束监听器
    private abstract class AnimationEndListener : Animation.AnimationListener {
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationRepeat(animation: Animation?) {}
    }
}
