package com.bird2fish.birdtalksdk.format

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.FontMetricsInt
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.style.ReplacementSpan
import android.util.Log
import android.view.View
import androidx.annotation.IntRange
import com.squareup.picasso.Picasso
import com.squareup.picasso.Picasso.LoadedFrom
import com.squareup.picasso.Target
import java.lang.ref.WeakReference
import java.net.URL

/* Spannable which updates associated image as it's loaded from the given URL */
class RemoteImageSpan(
    parent: View,
    private val mWidth: Int,
    private val mHeight: Int,
    private val mCropCenter: Boolean,
    private var mDrawable: Drawable,
    private val mOnError: Drawable
) : ReplacementSpan(), Target {
    private val mParentRef = WeakReference(parent)
    private var mSource: URL? = null

    fun load(from: URL) {
        mSource = from
        val temp = Uri.parse(from.toString())
        var req = Picasso.get().load(temp).resize(mWidth, mHeight)
        if (mCropCenter) {
            req = req.centerCrop()
        }
        req.into(this)
    }

    override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
        val parent = mParentRef.get()
        if (parent != null) {
            mDrawable = BitmapDrawable(parent.resources, bitmap)
            mDrawable.setBounds(0, 0, bitmap.width, bitmap.height)
            parent.postInvalidate()
        }
    }

    override fun onBitmapFailed(e: Exception, errorDrawable: Drawable) {
        Log.w(TAG, "Failed to get image: " + e.message + " (" + mSource + ")")
        val parent = mParentRef.get()
        if (parent != null) {
            mDrawable = mOnError
            parent.postInvalidate()
        }
    }

    override fun onPrepareLoad(placeHolderDrawable: Drawable) {
    }

    override fun getSize(
        paint: Paint, text: CharSequence,
        start: Int, end: Int, fm: FontMetricsInt?
    ): Int {
        if (fm != null) {
            fm.descent = mHeight / 3
            fm.ascent = -fm.descent * 2

            fm.top = fm.ascent
            fm.bottom = fm.descent
        }
        return mWidth
    }

    // This has to be overridden because of brain-damaged design of DynamicDrawableSpan:
    // it caches Drawable and the cache cannot be invalidated.
    // 开始时候是绘制一个下载中，如果正确替换可绘制的资源，如果错误也是替换资源
    override fun draw(
        canvas: Canvas, text: CharSequence,
        @IntRange(from = 0) start: Int, @IntRange(from = 0) end: Int, x: Float,
        top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val b = mDrawable
        canvas.save()
        canvas.translate(x, (bottom - b.bounds.bottom).toFloat())
        b.draw(canvas)
        canvas.restore()
    }

    companion object {
        private const val TAG = "RemoteImageSpan"
    }
}
