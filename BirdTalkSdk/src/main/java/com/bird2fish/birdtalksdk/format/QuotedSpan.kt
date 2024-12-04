package com.bird2fish.birdtalksdk.format

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.text.Layout
import android.text.Spanned
import android.text.style.LeadingMarginSpan
import android.text.style.LineBackgroundSpan

// Draws a colored rounded rectangle background with a vertical stripe on the start side.
class QuotedSpan(
    private val mBackgroundColor: Int,
    private val mCornerRadius: Float,
    private val mStripeColor: Int,
    private val mStripeWidth: Float,
    private val mGapWidth: Float
) : LeadingMarginSpan, LineBackgroundSpan {
    override fun getLeadingMargin(first: Boolean): Int {
        return (mStripeWidth + mGapWidth).toInt()
    }

    override fun drawLeadingMargin(
        canvas: Canvas, paint: Paint, x: Int, dir: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, first: Boolean, layout: Layout
    ) {
        /* do nothing here */
    }

    override fun drawBackground(
        canvas: Canvas, paint: Paint,
        left: Int, right: Int, top: Int, baseline: Int, bottom: Int,
        text: CharSequence, start: Int, end: Int, lineNumber: Int
    ) {
        // Start and end of the current span within the text string.
        var myStart = -1
        var myEnd = -1
        if (text is Spanned) {
            myStart = text.getSpanStart(this)
            myEnd = text.getSpanEnd(this)
        }

        val originalColor = paint.color
        paint.color = mBackgroundColor
        if (start > myStart && end < myEnd) {
            // Lines in the middle.
            canvas.drawRect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(), paint)
            paint.color = mStripeColor
            canvas.drawRect(
                left.toFloat(),
                top.toFloat(),
                left + mStripeWidth,
                bottom.toFloat(),
                paint
            )
        } else {
            val background = Path()
            val stripe = Path()
            if (start == myStart) {
                // Fist line.
                background.addRoundRect(
                    left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(),
                    floatArrayOf(
                        mCornerRadius,
                        mCornerRadius,
                        mCornerRadius,
                        mCornerRadius,
                        0f,
                        0f,
                        0f,
                        0f
                    ),
                    Path.Direction.CW
                )
                stripe.addRoundRect(
                    left.toFloat(), top.toFloat(), left + mStripeWidth, bottom.toFloat(),
                    floatArrayOf(mCornerRadius, mCornerRadius, 0f, 0f, 0f, 0f, 0f, 0f),
                    Path.Direction.CW
                )
            } else {
                // Last line
                background.addRoundRect(
                    left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat(),
                    floatArrayOf(
                        0f,
                        0f,
                        0f,
                        0f,
                        mCornerRadius,
                        mCornerRadius,
                        mCornerRadius,
                        mCornerRadius
                    ),
                    Path.Direction.CW
                )
                stripe.addRoundRect(
                    left.toFloat(), top.toFloat(), left + mStripeWidth, bottom.toFloat(),
                    floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, mCornerRadius, mCornerRadius),
                    Path.Direction.CW
                )
            }
            canvas.drawPath(background, paint)
            paint.color = mStripeColor
            canvas.drawPath(stripe, paint)
        }
        paint.color = originalColor
    }
}
