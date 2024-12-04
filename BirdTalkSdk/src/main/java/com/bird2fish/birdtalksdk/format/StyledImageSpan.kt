package com.bird2fish.birdtalksdk.format

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.style.ImageSpan
import java.lang.ref.WeakReference

// ImageSpan with vertical alignment and padding. Only 'vertical-align: middle' is currently supported.
class StyledImageSpan(drawable: Drawable, padding: RectF?) : ImageSpan(drawable) {
    private var mDrawable: WeakReference<Drawable?>? = null
    private val mPadding = padding ?: RectF()

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: FontMetricsInt?
    ): Int {
        val drawable = cachedDrawable
        val bounds = drawable!!.bounds

        if (fm != null) {
            fm.descent = bounds.height() / 3 + mPadding.bottom.toInt()
            fm.ascent = -fm.descent * 2 - mPadding.top.toInt()

            fm.top = fm.ascent
            fm.bottom = fm.descent
        }

        return bounds.width() + (mPadding.left + mPadding.right).toInt()
    }

    override fun draw(
        canvas: Canvas, text: CharSequence,
        start: Int, end: Int, x: Float,
        top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val drawable = cachedDrawable
        canvas.save()
        val dY = top + (bottom - top) * 0.5f - drawable!!.bounds.height() * 0.5f
        canvas.translate(x + mPadding.left, dY + mPadding.top)
        drawable.draw(canvas)
        canvas.restore()
    }

    private val cachedDrawable: Drawable?
        get() {
            val ref = mDrawable
            var drawable = ref?.get()
            if (drawable == null) {
                drawable = getDrawable()
                mDrawable = WeakReference(drawable)
            }
            return drawable
        }
}
