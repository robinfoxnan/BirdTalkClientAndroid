package com.bird2fish.birdtalksdk.format

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.RectF
import android.text.style.ReplacementSpan
import com.bird2fish.birdtalksdk.R

// Fills background and draws a rounded border around text.
class LabelSpan internal constructor(ctx: Context, fontSize: Float, private val mDipSize: Float) :
    ReplacementSpan() {
    // Approximate width of a '0' char.
    private val mCharWidth = 0.6f * fontSize / mDipSize
    private val mPaintFrame = Paint()
    private val mPaintBackground: Paint

    // Width of the label with padding added, in DIPs.
    private var mWidth = 0

    // Actual width of the text in DIPs.
    private var mWidthActual = 0

    /**
     * Create formatter for text which appears as a label with background and a border.
     *
     * @param ctx      Context (activity) which uses this formatter.
     * @param fontSize font size in device (unscaled) pixels as returned by view.getTextSize().
     * @param dipSize  size of the DIP unit in unscaled pixels.
     */
    init {
        mPaintFrame.style = Paint.Style.STROKE
        mPaintFrame.isAntiAlias = true
        mPaintFrame.color = ctx.resources.getColor(R.color.colorChipBorder)

        mPaintBackground = Paint()
        mPaintBackground.style = Paint.Style.FILL
        mPaintBackground.isAntiAlias = true
        mPaintBackground.color = ctx.resources.getColor(R.color.colorChipBackground)
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: FontMetricsInt?
    ): Int {
        // Actual text width in DIPs.
        mWidthActual = (paint.measureText(text, start, end) / mDipSize).toInt()
        // Ensure minimum width of the button: actual width + 2 characters on each side.
        mWidth = (mWidthActual + mCharWidth * 2).toInt()
        // The result must be in pixels.
        return (mWidth * mDipSize).toInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val outline = RectF(
            x,
            top + PADDING_TOP * mDipSize,
            x + mWidth * mDipSize - 1,
            (bottom - 1).toFloat()
        )
        // Draw background.
        canvas.drawRoundRect(
            outline,
            RADIUS_CORNER * mDipSize,
            RADIUS_CORNER * mDipSize,
            mPaintBackground
        )
        // Draw frame.
        canvas.drawRoundRect(
            outline,
            RADIUS_CORNER * mDipSize,
            RADIUS_CORNER * mDipSize,
            mPaintFrame
        )
        // Vertical padding between the button boundary and text.
        val padding = (outline.height() - paint.descent() + paint.ascent()) / 2f
        canvas.drawText(
            text, start, end,
            x + (mWidth - mWidthActual - 1) * mDipSize * 0.5f,
            top + PADDING_TOP * mDipSize + padding - paint.ascent(),
            paint
        )
    }

    companion object {
        // All sizes are in DIPs.
        private const val RADIUS_CORNER = 1.5f
        private const val PADDING_TOP = 2.0f
    }
}
