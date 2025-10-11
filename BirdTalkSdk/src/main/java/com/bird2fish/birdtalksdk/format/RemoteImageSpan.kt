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
    private var mTargetWidth: Int,  // 目标显示宽度（如屏幕90%宽度）
    private var mTargetHeight: Int, // 目标显示高度（如屏幕75%高度）
    private val mCropCenter: Boolean,
    private var mDrawable: Drawable,
    private val mOnError: Drawable
) : ReplacementSpan(), Target {
    private val mParentRef = WeakReference(parent)
    private var mSource: URL? = null

    private var mOriginalWidth = 0  // 图片原始宽度
    private var mOriginalHeight = 0 // 图片原始高度
    private var mScaledWidth = 0
    private var mScaledHeight = 0


    // 初始化：给初始占位图设置bounds（关键！避免getSize()获取到0）
    init {
        if (mTargetWidth > 0 && mTargetHeight > 0) {
            mDrawable.setBounds(0, 0, mTargetWidth, mTargetHeight)
        }
        // 错误图提前设置bounds，避免加载失败时尺寸异常
        mOnError.setBounds(0, 0, mTargetWidth, mTargetHeight)
    }

    fun load(from: URL) {
        mSource = from
        val temp = Uri.parse(from.toString())
        var req = Picasso.get().load(temp)
        //.resize(mWidth, mHeight)。
        if (mCropCenter) {
            req = req.centerCrop()
        }
        req.into(this)
    }


    // 更新目标尺寸时，同步更新Drawable的bounds，旋转屏幕时候
    fun updateTargetDimensions(newMaxWidth: Int, newMaxHeight: Int) {
        if (newMaxWidth <= 0 || newMaxHeight <= 0) return
        if (Math.abs(newMaxWidth - mTargetWidth) > 10 || Math.abs(newMaxHeight - mTargetHeight) > 10) {
            mTargetWidth = newMaxWidth
            mTargetHeight = newMaxHeight
            // 同步更新当前Drawable的bounds（避免getSize()获取旧值）
            mDrawable.setBounds(0, 0, mTargetWidth, mTargetHeight)
            mOnError.setBounds(0, 0, mTargetWidth, mTargetHeight)
            // 重新加载图片
            mSource?.let { load(it) }
        }
    }

//    override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
//        val parent = mParentRef.get() ?: return
//
//        // 保存原始尺寸
//        mOriginalWidth = bitmap.width
//        mOriginalHeight = bitmap.height
//
//        if (parent != null) {
//            mDrawable = BitmapDrawable(parent.resources, bitmap)
//            mDrawable.setBounds(0, 0, bitmap.width, bitmap.height)
//            parent.postInvalidate()
//        }
//    }

    // 自定义缩放：不依赖Picasso，手动计算缩放后的Bitmap
    override fun onBitmapLoaded(bitmap: Bitmap, from: LoadedFrom) {
        val parent = mParentRef.get() ?: return

        // 1. 保存原始图片尺寸
        mOriginalWidth = bitmap.width
        mOriginalHeight = bitmap.height
        Log.d(TAG, "原始图片尺寸: ${mOriginalWidth}x${mOriginalHeight}")
        Log.d(TAG, "初始化设置尺寸: ${mTargetWidth}x${mTargetHeight}")

        // 2. 计算目标缩放尺寸（保持宽高比，避免拉伸）
        val (scaledWidth, scaledHeight) = calculateScaledSize(
            originalWidth = mOriginalWidth,
            originalHeight = mOriginalHeight,
            targetMaxWidth = mTargetWidth,
            targetMaxHeight = mTargetHeight
        )

        mScaledWidth = scaledWidth
        mScaledHeight = scaledHeight

        Log.d(TAG, "自定义缩放后尺寸: ${scaledWidth}x${scaledHeight}")

        // 3. 手动缩放Bitmap（使用createScaledBitmap，质量更优）
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            scaledWidth,
            scaledHeight,
            true // 开启抗锯齿，提升缩放后图片质量
        )

        // 4. 设置Drawable并对齐控件尺寸
        mDrawable = BitmapDrawable(parent.resources, scaledBitmap).apply {
            // 强制Drawable的 bounds 与缩放后的尺寸一致（关键！避免显示偏移）
            setBounds(0, 0, scaledWidth, scaledHeight)
        }

        // 5. 通知父View重绘，更新显示
        parent.post {
            if (parent.isAttachedToWindow) { // 防止View已销毁导致崩溃
                Log.d(TAG, "触发requestLayout+invalidate，强制重测量")
                parent.requestLayout() // 强制重新测量和布局，必然调用getSize()
                parent.invalidate()    // 确保绘制最新的Drawable
            }
        }
        //parent.postInvalidate()

        // 6. 不能回收原始Bitmap，会崩溃
    }

    // 核心：计算缩放尺寸（保持宽高比，不超过目标最大宽高）
    private fun calculateScaledSize(
        originalWidth: Int,
        originalHeight: Int,
        targetMaxWidth: Int,
        targetMaxHeight: Int
    ): Pair<Int, Int> {
        if (originalWidth <= 0 || originalHeight <= 0) {
            return Pair(targetMaxWidth, targetMaxHeight) // 异常情况用目标尺寸占位
        }

        // 计算宽高比
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()

        // 按目标宽度计算高度，若高度超过目标最大高度，则按目标高度反算宽度
        val calculatedWidth: Int
        val calculatedHeight: Int

        if (targetMaxWidth / aspectRatio <= targetMaxHeight) {
            // 宽度优先：按目标宽度缩放，高度自适应
            calculatedWidth = targetMaxWidth
            calculatedHeight = (calculatedWidth / aspectRatio).toInt()
        } else {
            // 高度优先：按目标高度缩放，宽度自适应
            calculatedHeight = targetMaxHeight
            calculatedWidth = (calculatedHeight * aspectRatio).toInt()
        }

        // 确保最终尺寸不小于1（避免异常）
        return Pair(
            maxOf(calculatedWidth, 1),
            maxOf(calculatedHeight, 1)
        )
    }

//    override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
//        Log.w(TAG, "Failed to get image: " + e.message + " (" + mSource + ")")
//        val parent = mParentRef.get()
//        if (parent != null) {
//            mDrawable = mOnError
//            parent.postInvalidate()
//        }
//    }
//
//    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
//    }
// 错误处理：错误图也按目标尺寸缩放
    override fun onBitmapFailed(e: Exception, errorDrawable: Drawable?) {
        Log.w(TAG, "图片加载失败: ${e.message} (来源: $mSource)")
        val parent = mParentRef.get() ?: return

        // 错误图按目标尺寸缩放
        mOnError.setBounds(0, 0, mTargetWidth, mTargetHeight)
        mDrawable = mOnError
        parent.postInvalidate()
    }

    // 占位图：加载前按目标尺寸显示
    override fun onPrepareLoad(placeHolderDrawable: Drawable?) {
        placeHolderDrawable?.let {
            it.setBounds(0, 0, mTargetWidth, mTargetHeight)
            mDrawable = it
        }
    }

//    override fun getSize(
//        paint: Paint, text: CharSequence,
//        start: Int, end: Int, fm: FontMetricsInt?
//    ): Int {
//        if (fm != null) {
//            fm.descent = mHeight / 3
//            fm.ascent = -fm.descent * 2
//
//            fm.top = fm.ascent
//            fm.bottom = fm.descent
//        }
//        return mWidth
//    }
    // 控制Span的尺寸（与缩放后的图片尺寸对齐）
    override fun getSize(
        paint: Paint, text: CharSequence,
        start: Int, end: Int, fm: FontMetricsInt?
    ): Int {
        // 优先用缩放后真实宽度，兜底用200px
        val spanWidth = if (mScaledWidth > 0) mScaledWidth else mTargetWidth
        // 垂直占位高度：与缩放后真实高度一致，避免垂直留白
        if (fm != null) {
            val spanHeight = if (mScaledHeight > 0) mScaledHeight else mTargetHeight
            fm.ascent = -spanHeight // 顶部对齐：向上预留完整高度
            fm.descent = 0          // 底部对齐：向下不留额外空间
            fm.top = fm.ascent      // 顶部与ascent一致
            fm.bottom = fm.descent  // 底部与descent一致
        }
        Log.d(TAG, "getSize() - Span宽度: $spanWidth, 图片宽度: ${mScaledWidth}")
        return spanWidth
    }

    // This has to be overridden because of brain-damaged design of DynamicDrawableSpan:
    // it caches Drawable and the cache cannot be invalidated.
    // 开始时候是绘制一个下载中，如果正确替换可绘制的资源，如果错误也是替换资源
//    override fun draw(
//        canvas: Canvas, text: CharSequence,
//        @IntRange(from = 0) start: Int, @IntRange(from = 0) end: Int, x: Float,
//        top: Int, y: Int, bottom: Int, paint: Paint
//    ) {
//        val b = mDrawable
//        canvas.save()
//        canvas.translate(x, (bottom - b.bounds.bottom).toFloat())
//        b.draw(canvas)
//        canvas.restore()
//    }
    // 绘制图片（确保图片在Span中垂直居中）
    override fun draw(
        canvas: Canvas, text: CharSequence,
        @IntRange(from = 0) start: Int, @IntRange(from = 0) end: Int, x: Float,
        top: Int, y: Int, bottom: Int, paint: Paint
    ) {
        val drawable = mDrawable
        // 计算垂直居中偏移量（让图片在Span的高度范围内居中）
        val verticalOffset = (bottom - top - drawable.bounds.height()) / 2

        canvas.save()
        // 平移绘制位置：x（水平起点） + 垂直居中偏移（top + verticalOffset）
        canvas.translate(x, (top + verticalOffset).toFloat())
        drawable.draw(canvas)
        canvas.restore()
    }

    companion object {
        private const val TAG = "RemoteImageSpan"
    }
}
