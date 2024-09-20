package com.bird2fish.birdtalksdk.widgets


import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.annotation.FloatRange
import com.bird2fish.birdtalksdk.R
import kotlin.math.max
import kotlin.math.min

/**
 * Drawable which visualizes sound amplitudes as a waveform.
 */
class WaveDrawable @JvmOverloads constructor(res: Resources, leftPaddingDP: Int = 0) :
    Drawable(), Runnable {
    // Duration of the audio in milliseconds.
    private var mDuration = 0

    // Current thumb position as a fraction of the total 0..1
    var position: Float = -1f
        private set

    private var mOriginal: ByteArray? = null

    // Amplitude values received from the caller and resampled to fit the screen.
    private var mBuffer: FloatArray? = null

    // Count of amplitude values actually added to the buffer.
    private var mContains = 0

    // Entry point in mBuffer (mBuffer is a circular buffer).
    private var mIndex = 0

    // Array of 4 values for each amplitude bar: startX, startY, stopX, stopY.
    private var mBars: FloatArray? = null

    // Canvas width which fits whole number of bars.
    private var mEffectiveWidth = 0

    // If the Drawable is animated.
    private var mRunning = false

    // Duration of a single animation frame: about two pixels at a time, but no shorter than MIN_FRAME_DURATION.
    private var mFrameDuration = MIN_FRAME_DURATION

    // Paints for individual components of the drawable.
    private val mBarPaint: Paint
    private val mPastBarPaint: Paint
    private val mThumbPaint: Paint

    private var mSize = Rect()

    // Padding on the left.
    private var mLeftPadding = 0

    private var mCompletionListener: CompletionListener? = null

    init {
        if (sDensity <= 0) {
            sDensity = res.displayMetrics.density
            sLineWidth = LINE_WIDTH * sDensity
            sSpacing = SPACING * sDensity
            sThumbRadius = sLineWidth * 1.5f
        }

        mLeftPadding = (leftPaddingDP * sDensity).toInt()

        // Waveform in the future.
        mBarPaint = Paint()
        mBarPaint.style = Paint.Style.STROKE
        mBarPaint.strokeWidth = sLineWidth
        mBarPaint.strokeCap = Paint.Cap.ROUND
        mBarPaint.isAntiAlias = true
        mBarPaint.color = res.getColor(R.color.waveform, null)

        // Waveform in the past.
        mPastBarPaint = Paint()
        mPastBarPaint.style = Paint.Style.STROKE
        mPastBarPaint.strokeWidth = sLineWidth
        mPastBarPaint.strokeCap = Paint.Cap.ROUND
        mPastBarPaint.isAntiAlias = true
        mPastBarPaint.color = res.getColor(R.color.waveformPast, null)

        // Seek thumb.
        mThumbPaint = Paint()
        mThumbPaint.isAntiAlias = true
        mThumbPaint.color = res.getColor(R.color.colorAccent, null)
    }

    override fun onBoundsChange(bounds: Rect) {
        mSize = Rect(bounds)

        val maxBars = ((mSize.width() - sSpacing - mLeftPadding) / (sLineWidth + sSpacing)).toInt()
        mEffectiveWidth = (maxBars * (sLineWidth + sSpacing) + sSpacing).toInt()
        mBuffer = FloatArray(maxBars)

        // Recalculate frame duration (2 pixels per frame).
        mFrameDuration =
            max((mDuration / mEffectiveWidth * 2).toDouble(), MIN_FRAME_DURATION.toDouble())
                .toInt()

        if (mOriginal != null) {
            resampleBars(mOriginal!!, mBuffer!!)
            mIndex = 0
            mContains = mBuffer!!.size
            recalcBars()
        }
        invalidateSelf()
    }

    override fun getIntrinsicWidth(): Int {
        return mSize.width()
    }

    override fun getIntrinsicHeight(): Int {
        return mSize.height()
    }

    override fun draw(canvas: Canvas) {
        if (mBars == null) {
            return
        }

        if (position >= 0) {
            // Draw past - future bars and thumb on top of them.
            val cx = seekPositionToX()

            val dividedAt = (mBars!!.size * 0.25f * position).toInt() * 4

            // Already played amplitude bars.
            canvas.drawLines(mBars!!, 0, dividedAt, mPastBarPaint)

            // Not yet played amplitude bars.
            canvas.drawLines(mBars!!, dividedAt, mBars!!.size - dividedAt, mBarPaint)

            // Draw thumb.
            canvas.drawCircle(cx, mSize.height() * 0.5f, sThumbRadius, mThumbPaint)
        } else {
            // Just plain amplitude bars in one color.
            canvas.drawLines(mBars!!, mBarPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        if (mBarPaint.alpha != alpha) {
            mBarPaint.alpha = alpha
            invalidateSelf()
        }
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        mBarPaint.setColorFilter(colorFilter)
        mThumbPaint.setColorFilter(colorFilter)
        invalidateSelf()
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun run() {
        val pos = position + mFrameDuration.toFloat() / mDuration
        if (pos < 1) {
            seekTo(pos)
            nextFrame()
        } else {
            seekTo(0f)
            mRunning = false
            if (mCompletionListener != null) {
                mCompletionListener!!.onFinished()
            }
        }
    }

    fun start() {
        if (!mRunning) {
            mRunning = true
            nextFrame()
        }
    }

    fun stop() {
        mRunning = false
        unscheduleSelf(this)
    }

    fun reset() {
        stop()
        seekTo(0f)
        if (mCompletionListener != null) {
            mCompletionListener!!.onFinished()
        }
    }

    private fun nextFrame() {
        unscheduleSelf(this)
        scheduleSelf(this, SystemClock.uptimeMillis() + mFrameDuration)
    }

    fun setDuration(millis: Int) {
        mDuration = millis
        mFrameDuration =
            max((mDuration / mEffectiveWidth * 2).toDouble(), MIN_FRAME_DURATION.toDouble())
                .toInt()
    }

    fun seekTo(@FloatRange(from = 0.0, to = 1.0) fraction: Float) {
        if (mDuration > 0 && position != fraction) {
            position = max(min(fraction.toDouble(), 1.0), 0.0).toFloat()
            invalidateSelf()
        }
    }

    // Add another bar to waveform.
    fun put(amplitude: Int) {
        if (mBuffer == null) {
            return
        }

        if (mContains < mBuffer!!.size) {
            mBuffer!![mIndex + mContains] = amplitude.toFloat()
            mContains++
        } else {
            mIndex++
            mIndex %= mBuffer!!.size
            mBuffer!![mIndex] = amplitude.toFloat()
        }
        recalcBars()
        invalidateSelf()
    }

    // Add entire waveform at once.
    fun put(amplitudes: ByteArray) {
        mOriginal = amplitudes
        if (mBuffer == null) {
            return
        }

        resampleBars(amplitudes, mBuffer!!)
        mIndex = 0
        mContains = mBuffer!!.size
        recalcBars()
        invalidateSelf()
    }

    fun setOnCompletionListener(listener: CompletionListener?) {
        mCompletionListener = listener
    }

    // Calculate vertices of amplitude bars.
    private fun recalcBars() {
        if (mBuffer!!.size == 0) {
            return
        }

        val height = mSize.height()
        if (mEffectiveWidth <= 0 || height <= 0) {
            return
        }

        // Values for scaling amplitude.
        var max = Int.MIN_VALUE.toFloat()
        for (amp in mBuffer!!) {
            if (amp > max) {
                max = amp
            }
        }
        if (max <= 0) {
            return
        }

        mBars = FloatArray(mContains * 4)
        for (i in 0 until mContains) {
            var amp = mBuffer!![(mIndex + i) % mContains]
            if (amp < 0) {
                amp = 0f
            }

            // startX, endX
            val x =
                mLeftPadding + 1.0f + i * (sLineWidth + sSpacing) + sLineWidth * 0.5f
            // Y length
            val y = amp / max * height * 0.9f + 0.01f
            // startX
            mBars!![i * 4] = x
            // startY
            mBars!![i * 4 + 1] = (height - y) * 0.5f
            // stopX
            mBars!![i * 4 + 2] = x
            // stopY
            mBars!![i * 4 + 3] = (height + y) * 0.5f
        }
    }

    // Get thumb position for level.
    private fun seekPositionToX(): Float {
        val base = mBars!!.size / 4f * (position - 0.01f)
        return mBars!![base.toInt() * 4] + (base - base.toInt()) * (sLineWidth + sSpacing)
    }

    interface CompletionListener {
        fun onFinished()
    }

    companion object {
        // Bars and spacing sizes in DP.
        private const val LINE_WIDTH = 3f
        private const val SPACING = 1f

        // Minimum time between redraws in milliseconds.
        private const val MIN_FRAME_DURATION = 50

        // Display density.
        private var sDensity = -1f

        // Bars and spacing sizes in pixels.
        private var sLineWidth = 0f
        private var sSpacing = 0f
        private var sThumbRadius = 0f

        // Quick and dirty resampling of the original preview bars into a smaller (or equal) number of bars we can display here.
        private fun resampleBars(src: ByteArray, dst: FloatArray) {
            // Resampling factor. Could be lower or higher than 1.
            val factor = src.size.toFloat() / dst.size
            var max = -1f
            // src = 100, dst = 200, factor = 0.5
            // src = 200, dst = 100, factor = 2.0
            for (i in dst.indices) {
                val lo = (i * factor).toInt() // low bound;
                val hi = ((i + 1) * factor).toInt() // high bound;
                if (hi == lo) {
                    dst[i] = src[lo].toFloat()
                } else {
                    var amp = 0f
                    for (j in lo until hi) {
                        amp += src[j].toFloat()
                    }
                    dst[i] = max(0.0, (amp / (hi - lo)).toDouble()).toFloat()
                }
                max = max(dst[i].toDouble(), max.toDouble()).toFloat()
            }

            if (max > 0) {
                for (i in dst.indices) {
                    dst[i] = dst[i] / max
                }
            }
        }
    }
}



