package com.bird2fish.birdtalksdk.format;

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.LineHeightSpan
import android.text.style.ReplacementSpan
import androidx.annotation.NonNull
import android.util.TypedValue



// 用于在草稿中表示可点击按钮的 Span
class ButtonSpan(
    context: Context,
    fontSize: Float, // 字体大小（像素）
    private val mContext: Context
) : ReplacementSpan(), LineHeightSpan {

    // 常量定义（统一用DIP单位）
    private val RADIUS_CORNER_DP = 8f
    private val SHADOW_SIZE_DP = 2f
    private val MIN_BUTTON_WIDTH_DP = 60f
    private val BUTTON_HEIGHT_SCALE = 2.0f
    private val H_PADDING_DP = 12f
    // 新增：底部额外预留空间（用于阴影+避免裁切）
    private val BOTTOM_EXTRA_PADDING_DP = 4f

    private val mPaintBackground: Paint
    private val mTextColor: Int
    private val mButtonHeightPx: Int
    private val mMinButtonWidthPx: Int
    private val mHPaddingPx: Int
    private val mRadiusCornerPx: Float
    private val mShadowSizePx: Float
    // 新增：底部额外预留像素
    private val mBottomExtraPaddingPx: Int
    private var mButtonWidthPx: Int = 0
    private var mTextWidthPx: Float = 0f

    init {
        // 工具方法：DIP转像素
        fun dp2px(dp: Float): Int {
            return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                mContext.resources.displayMetrics
            ).toInt()
        }

        // 1. 转换常量为像素单位
        mMinButtonWidthPx = dp2px(MIN_BUTTON_WIDTH_DP)
        mHPaddingPx = dp2px(H_PADDING_DP)
        mRadiusCornerPx = dp2px(RADIUS_CORNER_DP).toFloat()
        mShadowSizePx = dp2px(SHADOW_SIZE_DP).toFloat()
        mBottomExtraPaddingPx = dp2px(BOTTOM_EXTRA_PADDING_DP) // 初始化底部额外空间
        mButtonHeightPx = (fontSize * BUTTON_HEIGHT_SCALE).toInt()

        // 2. 获取主题颜色
        val attrs = intArrayOf(android.R.attr.textColorPrimary, android.R.attr.colorButtonNormal)
        val colors: TypedArray = mContext.obtainStyledAttributes(attrs)
        mTextColor = colors.getColor(0, 0x7bc9c2)
        val background = colors.getColor(1, 0xeeeeff)
        colors.recycle()

        // 3. 初始化背景画笔
        mPaintBackground = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = background
            setShadowLayer(
                mShadowSizePx,
                mShadowSizePx * 0.5f,
                mShadowSizePx * 0.5f,
                Color.argb(0x80, 0, 0, 0)
            )
        }
    }

    @NonNull
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        // 计算文本实际宽度（像素）
        mTextWidthPx = paint.measureText(text, start, end)
        // 计算按钮宽度：文本宽度 + 左右内边距，且不小于最小宽度
        val textWithPadding = mTextWidthPx + 2 * mHPaddingPx
        mButtonWidthPx = maxOf(textWithPadding.toInt(), mMinButtonWidthPx)

        // 关键修复1：如果传入了FontMetrics，提前预留底部额外空间
        fm?.let {
            val totalExtra = mShadowSizePx.toInt() + mBottomExtraPaddingPx
            it.bottom += totalExtra
            it.descent += totalExtra
        }

        return mButtonWidthPx
    }

    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        lineHeight: Int,
        fm: Paint.FontMetricsInt
    ) {
        // 修复行高：调整整个字体Metrics，适配按钮高度+底部额外空间
        val fontHeight = fm.bottom - fm.top
        // 总高度 = 按钮高度 + 阴影高度 + 底部额外预留空间
        val totalButtonHeight = mButtonHeightPx + mShadowSizePx.toInt() + mBottomExtraPaddingPx
        val diff = totalButtonHeight - fontHeight

        if (diff > 0) {
            // 上下均分差值，保证文本垂直居中，同时预留底部空间
            fm.ascent -= diff / 2
            fm.top -= diff / 2
            fm.descent += diff - diff / 2
            fm.bottom += diff - diff / 2
        }
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
        // 关键修复2：调整按钮底部边界，避免超出绘制区域
        // 按钮顶部：留出阴影空间
        val buttonTop = top.toFloat() + mShadowSizePx
        // 按钮底部：不超过当前行的bottom - 底部额外预留（避免裁切）
        val maxButtonBottom = bottom - mBottomExtraPaddingPx
        val buttonBottom = minOf(buttonTop + mButtonHeightPx, maxButtonBottom.toFloat())

        // 绘制按钮背景（限制底部边界）
        val outline = RectF(
            x + mShadowSizePx,
            buttonTop,
            x + mButtonWidthPx - mShadowSizePx,
            buttonBottom - mShadowSizePx // 减去阴影高度，避免阴影裁切
        )
        canvas.drawRoundRect(outline, mRadiusCornerPx, mRadiusCornerPx, mPaintBackground)

        // 绘制文本（保证居中且不超出按钮）
        paint.run {
            isUnderlineText = false
            color = mTextColor
            val textX = x + (mButtonWidthPx - mTextWidthPx) / 2
            // 调整文本垂直位置，适配新的按钮底部边界
            val textY = buttonTop + (buttonBottom - buttonTop - ascent() - descent()) / 2
            canvas.drawText(text, start, end, textX, textY, this)
        }
    }

    // 新增：可选方法，用于外部获取按钮总高度（方便TextView设置底部内边距）
    fun getTotalButtonHeight(): Int {
        return mButtonHeightPx + mShadowSizePx.toInt() + mBottomExtraPaddingPx
    }
}


