package com.bird2fish.birdtalksdk.ui

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.*
import android.widget.OverScroller
import kotlin.jvm.JvmOverloads
import androidx.recyclerview.widget.RecyclerView
import android.widget.Scroller
import androidx.recyclerview.widget.LinearLayoutManager

/**
 * 支持侧滑删除的RecyclerView
 *
 *
 * Created by DavidChen on 2018/5/29.
 * 也可以用com.chauthai.swipereveallayout.SwipeRevealLayout
 */
class SlideRecyclerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    private var mVelocityTracker: VelocityTracker? = null
    private val mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var mTouchFrame: Rect? = null
    private val mScroller = OverScroller(context)

    private var mLastX = 0f
    private var mFirstX = 0f
    private var mFirstY = 0f
    private var mIsSlide = false
    private var mFlingView: ViewGroup? = null
    private var mPosition = INVALID_POSITION
    private var mMenuViewWidth = 0

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        val x = e.x.toInt()
        val y = e.y.toInt()
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (!mScroller.isFinished) mScroller.abortAnimation()
                mLastX = x.toFloat()
                mFirstX = mLastX
                mFirstY = y.toFloat()

                mPosition = pointToPosition(x, y)
                if (mPosition != INVALID_POSITION) {
                    val oldView = mFlingView
                    mFlingView = getChildAt(mPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()) as ViewGroup
                    if (oldView != null && mFlingView !== oldView && oldView.scrollX != 0) oldView.scrollTo(0, 0)
                    mMenuViewWidth = if (mFlingView!!.childCount == 2) mFlingView!!.getChildAt(1).width else INVALID_CHILD_WIDTH
                }

                obtainVelocity(e)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = x - mFirstX
                val dy = y - mFirstY
                if (Math.abs(dx) > mTouchSlop && Math.abs(dx) > Math.abs(dy)) {
                    mIsSlide = true
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> releaseVelocity()
        }
        return super.onInterceptTouchEvent(e)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (mIsSlide && mPosition != INVALID_POSITION) {
            val x = e.x
            when (e.actionMasked) {
                MotionEvent.ACTION_MOVE -> {
                    if (mMenuViewWidth != INVALID_CHILD_WIDTH) {
                        val dx = (mLastX - x).toInt()
                        mFlingView?.let {
                            val scrollX = it.scrollX + dx
                            it.scrollTo(scrollX.coerceIn(0, mMenuViewWidth), 0)
                        }
                        mLastX = x
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    mVelocityTracker?.computeCurrentVelocity(1000)
                    val scrollX = mFlingView!!.scrollX
                    val xVel = mVelocityTracker!!.xVelocity.toInt()

                    val target = when {
                        xVel < -SNAP_VELOCITY -> mMenuViewWidth
                        xVel > SNAP_VELOCITY -> 0
                        scrollX >= mMenuViewWidth / 2 -> mMenuViewWidth
                        else -> 0
                    }

                    mScroller.startScroll(scrollX, 0, target - scrollX, 0, 300)
                    postInvalidateOnAnimation()
                    mIsSlide = false
                    mPosition = INVALID_POSITION
                    releaseVelocity()
                }
            }
            return true
        }
        return super.onTouchEvent(e)
    }

    override fun computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mFlingView?.scrollTo(mScroller.currX, mScroller.currY)
            postInvalidateOnAnimation()
        }
    }

    private fun obtainVelocity(event: MotionEvent) {
        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain()
        mVelocityTracker!!.addMovement(event)
    }

    private fun releaseVelocity() {
        mVelocityTracker?.recycle()
        mVelocityTracker = null
    }

    fun pointToPosition(x: Int, y: Int): Int {
        if (layoutManager == null) return INVALID_POSITION
        val firstPosition = (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()
        val frame = mTouchFrame ?: Rect().also { mTouchFrame = it }
        for (i in childCount - 1 downTo 0) {
            val child = getChildAt(i)
            if (child.visibility == VISIBLE) {
                child.getHitRect(frame)
                if (frame.contains(x, y)) return firstPosition + i
            }
        }
        return INVALID_POSITION
    }

    fun closeMenu() {
        mFlingView?.scrollTo(0, 0)
    }

    companion object {
        private const val INVALID_POSITION = -1
        private const val INVALID_CHILD_WIDTH = -1
        private const val SNAP_VELOCITY = 600
    }
}
