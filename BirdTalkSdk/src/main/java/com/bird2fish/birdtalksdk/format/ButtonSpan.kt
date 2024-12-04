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

// 用于在草稿中表示可点击按钮的 Span
class ButtonSpan(
        context: Context,
        fontSize: Float,
        dipSize: Float
) : ReplacementSpan(), LineHeightSpan {

    // 常量定义
    private val RADIUS_CORNER = 2.5f // 圆角半径（DIP）
    private val SHADOW_SIZE = 2.5f // 阴影大小（DIP）
    private val MIN_BUTTON_WIDTH = 8 // 最小按钮宽度（字符）
    private val BUTTON_HEIGHT_SCALE = 2.0f // 按钮高度与字体大小的比例
    private val H_PADDING = 2 // 水平按钮内边距（字符）

    private val mPaintBackground: Paint
    private val mTextColor: Int
    private val mMinButtonWidth: Int // 最小按钮宽度（DIP）
    private val mButtonHeight: Int // 按钮高度（DIP）
    private val mDipSize: Float = 1F // DIP 转换为像素的大小
    private var mWidth: Int = 0 // 带内边距和最小宽度的按钮宽度（DIP）
    private var mWidthActual: Int = 0 // 文本实际宽度（DIP）

    init {
        // 获取主题颜色
        val attrs = intArrayOf(android.R.attr.textColorPrimary, android.R.attr.colorButtonNormal)
        val colors: TypedArray = context.obtainStyledAttributes(attrs)
        mTextColor = colors.getColor(0, 0x7bc9c2)
        @SuppressLint("ResourceType")
        val background = colors.getColor(1, 0xeeeeff)
        colors.recycle()

        // 初始化 Paint 对象
        mPaintBackground = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
            color = background
            setShadowLayer(SHADOW_SIZE * dipSize, SHADOW_SIZE * 0.5f * dipSize, SHADOW_SIZE * 0.5f * dipSize,
                    Color.argb(0x80, 0, 0, 0))
        }

        // 计算最小按钮宽度和按钮高度（DIP）
        mMinButtonWidth = (MIN_BUTTON_WIDTH * 0.6f * fontSize / dipSize).toInt()
        mButtonHeight = (BUTTON_HEIGHT_SCALE * fontSize / dipSize).toInt()
    }

    @NonNull
    override fun getSize(paint: Paint, text: CharSequence, start: Int, end: Int, fm: Paint.FontMetricsInt?): Int {
        // 计算文本实际宽度（DIP）
        mWidthActual = (paint.measureText(text, start, end) / mDipSize).toInt()
        // 确保按钮的最小宽度
        mWidth = maxOf(mWidthActual + (mMinButtonWidth / MIN_BUTTON_WIDTH) * H_PADDING * 2, mMinButtonWidth)
        // 返回像素值
        return (mWidth * mDipSize).toInt()
    }

    override fun chooseHeight(
            text: CharSequence,
            start: Int,
            end: Int,
            spanstartv: Int,
            lineHeight: Int,
            fm: Paint.FontMetricsInt
    ) {
        val diff = mButtonHeight * mDipSize - (fm.bottom - fm.top)
        // 调整高度
        fm.descent += diff as Int
        fm.bottom += diff as Int
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
        val outline = RectF(x, top.toFloat(), x + mWidth * mDipSize, top + mButtonHeight * mDipSize)
        outline.inset(SHADOW_SIZE * mDipSize, SHADOW_SIZE * mDipSize)
        // 绘制背景
        canvas.drawRoundRect(outline, RADIUS_CORNER * mDipSize, RADIUS_CORNER * mDipSize, mPaintBackground)

        // 不加下划线
        paint.isUnderlineText = false
        paint.color = mTextColor
        canvas.drawText(text, start, end,
                x + (mWidth - mWidthActual) * mDipSize * 0.5f,
                top + (mButtonHeight * mDipSize - paint.ascent() - paint.descent()) * 0.5f,
                paint)
    }
}
