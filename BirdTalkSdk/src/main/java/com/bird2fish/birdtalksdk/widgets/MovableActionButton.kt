package com.bird2fish.birdtalksdk.widgets

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.util.AttributeSet
import android.util.ArrayMap
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * FloatingActionButton which can be dragged around.
 */
class MovableActionButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr), View.OnTouchListener {

    private val MIN_DRAG_DISTANCE = 8

    private var mDragToIgnore: Int = 0

    private var mConstraintChecker: ConstraintChecker? = null
    private var mActionListener: ActionListener? = null

    private var mActionZones: ArrayMap<Int, Rect>? = null

    // Drag started.
    private var mRawStartX: Float = 0f
    private var mRawStartY: Float = 0f

    // Distance between the button and the location of the initial DOWN click.
    private var mDiffX: Float = 0f
    private var mDiffY: Float = 0f

    init {
        initialize()
    }

    private fun initialize() {
        val density = resources.displayMetrics.density
        mDragToIgnore = (MIN_DRAG_DISTANCE * density).toInt()
        setOnTouchListener(this)
    }

    fun setConstraintChecker(checker: ConstraintChecker) {
        mConstraintChecker = checker
    }

    fun setOnActionListener(listener: ActionListener) {
        mActionListener = listener
    }

    fun addActionZone(id: Int, zone: Rect) {
        if (mActionZones == null) {
            mActionZones = ArrayMap()
        }
        mActionZones?.put(id, Rect(zone))
    }

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        val action = motionEvent.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                mRawStartX = motionEvent.rawX
                mRawStartY = motionEvent.rawY
                // Conversion from screen to view coordinates.
                mDiffX = view.x - mRawStartX
                mDiffY = view.y - mRawStartY
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dX = motionEvent.rawX - mRawStartX
                val dY = motionEvent.rawY - mRawStartY

                var putBack = false
                if (mActionListener != null) {
                    putBack = mActionListener!!.onUp(dX, dY)
                }

                // Make sure the drag was long enough.
                if (Math.abs(dX) < mDragToIgnore && Math.abs(dY) < mDragToIgnore || putBack) {
                    // Not a drag: too short. Move back and register click.
                    view.animate().x(mRawStartX + mDiffX).y(mRawStartY + mDiffY).setDuration(0).start()
                    return performClick()
                }
                // A real drag.
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                var newPos = PointF(motionEvent.rawX + mDiffX, motionEvent.rawY + mDiffY)

                // Ensure constraints.
                if (mConstraintChecker != null) {
                    val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
                    val viewParent = view.parent as View

                    val viewRect = Rect(view.left, view.top, view.right, view.bottom)
                    val parentRect = Rect(
                        layoutParams.leftMargin,
                        layoutParams.topMargin,
                        viewParent.width - layoutParams.rightMargin,
                        viewParent.height - layoutParams.bottomMargin
                    )
                    newPos = mConstraintChecker!!.check(newPos, PointF(mRawStartX + mDiffX, mRawStartY + mDiffY), viewRect, parentRect)
                }

                // Animate view to the new position.
                view.animate().x(newPos.x).y(newPos.y).setDuration(0).start()

                // Check if the center of the button is inside the action zone.
                if (mActionZones != null && mActionListener != null) {
                    val x = newPos.x + view.width * 0.5f
                    val y = newPos.y + view.height * 0.5f
                    for ((key, value) in mActionZones!!) {
                        if (value.contains(x.toInt(), y.toInt())) {
                            if (mActionListener!!.onZoneReached(key)) {
                                view.animate().x(mRawStartX + mDiffX).y(mRawStartY + mDiffY).setDuration(0).start()
                                break
                            }
                        }
                    }
                }

                return true
            }

            else -> return super.onTouchEvent(motionEvent)
        }
    }

    open class ActionListener {
        open fun onUp(dX: Float, dY: Float): Boolean {
            return false
        }

        open fun onZoneReached(id: Int): Boolean {
            return false
        }
    }

    interface ConstraintChecker {
        fun check(newPos: PointF, startPos: PointF, view: Rect, parent: Rect): PointF
    }
}


