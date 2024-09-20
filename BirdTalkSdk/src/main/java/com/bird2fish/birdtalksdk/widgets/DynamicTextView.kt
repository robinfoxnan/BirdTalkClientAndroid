package com.bird2fish.birdtalksdk.widgets

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.SystemClock
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * Text view which permits animation of embedded ImageSpans.
 */
class DynamicTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    AppCompatTextView(context, attrs, defStyleAttr) {
    override fun invalidateDrawable(who: Drawable) {
        postInvalidate()
    }

    override fun scheduleDrawable(who: Drawable, what: Runnable, `when`: Long) {
        postDelayed(what, `when` - SystemClock.uptimeMillis())
    }

    override fun unscheduleDrawable(who: Drawable, what: Runnable) {
        removeCallbacks(what)
    }
}