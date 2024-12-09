package com.bird2fish.birdtalksdk.format

import android.content.Context
import android.content.res.TypedArray
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.StateListDrawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextUtils
import android.text.style.CharacterStyle
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.LeadingMarginSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.SubscriptSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import com.bird2fish.birdtalksdk.widgets.WaveDrawable
import com.bird2fish.birdtalksdk.widgets.WaveDrawable.CompletionListener
import java.net.URL
import java.util.Stack
import kotlin.math.abs
import kotlin.math.min
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper

/**
 * Convert Drafty object into a Spanned object with full support for all features.
 */
class FullFormatter(private val mContainer: TextView, private val mClicker: ClickListener?) :
    AbstractDraftyFormatter<SpannableStringBuilder?>(mContainer.context) {
    // Maximum width of the container TextView. Max height is maxWidth * 0.75.
    private val mViewport = mContainer.maxWidth - mContainer.paddingLeft - mContainer.paddingRight
    private val mFontSize = mContainer.textSize
    private var mQuoteFormatter: QuoteFormatter? = null

    init {
        val res = mContainer.resources
        if (sColorsDark == null) {
            sColorsDark = res.obtainTypedArray(R.array.letter_tile_colors_dark)
            sDefaultColor = res.getColor(R.color.grey, null)
        }
    }

    override fun apply(
        tp: String?,
        data: Map<String, Any>?,
        content: List<SpannableStringBuilder?> ?,
        context: Stack<String>
    ): SpannableStringBuilder? {
        if (context != null && context.contains("QQ") && mQuoteFormatter != null) {
            return mQuoteFormatter!!.apply(tp, data, content, context)
        }

        return super.apply(tp, data, content, context)
    }

    override fun wrapText(text: CharSequence): SpannableStringBuilder? {
        return if (text != null) SpannableStringBuilder(text) else null
    }

    fun setQuoteFormatter(quoteFormatter: QuoteFormatter?) {
        mQuoteFormatter = quoteFormatter
    }

    override  fun handleStrong(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return assignStyle(StyleSpan(Typeface.BOLD), content)
    }


    override  fun handleEmphasized(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return assignStyle(StyleSpan(Typeface.ITALIC), content)
    }

    override  fun handleDeleted(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return assignStyle(StrikethroughSpan(), content)
    }

    override fun handleCode(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return assignStyle(TypefaceSpan("monospace"), content)
    }

    override fun handleHidden(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return null
    }

    override fun handleLineBreak(): SpannableStringBuilder? {
        return SpannableStringBuilder("\n")
    }

    override fun handleLink(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        try {
            // We don't need to specify an URL for URLSpan
            // as it's not going to be used.
            val span = SpannableStringBuilder(join(content))
            span.setSpan(object : URLSpan("") {
                override fun onClick(widget: View) {
                    mClicker?.onClick("LN", data, null)
                }
            }, 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            return span
        } catch (ignored: ClassCastException) {
        } catch (ignored: NullPointerException) {
        }
        return null
    }

    override fun handleMention(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return handleMention_Impl(content, data)
    }

    override fun handleHashtag(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return null
    }

    override fun handleAudio(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        if (data == null) {
            return null
        }

        mContainer.highlightColor = Color.TRANSPARENT
        val res = ctx!!.resources
        val waveDrawable: WaveDrawable?
        val result = SpannableStringBuilder()

        // Initialize Play icon，这里是其实是2个图片，可以切换
        val play =
            AppCompatResources.getDrawable(ctx, R.drawable.ic_play_pause) as StateListDrawable?
        play!!.setBounds(0, 0, play.intrinsicWidth * 3 / 2, play.intrinsicHeight * 3 / 2)
        play.setTint(res.getColor(R.color.colorAccent, null))
        val span = ImageSpan(play, ImageSpan.ALIGN_BOTTOM)
        val bounds = span.drawable.bounds
        result.append(" ", span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        result.append(" ")

        var duration: Number? = null
        try {
            duration = data["duration"] as Number?
        } catch (ignored: ClassCastException) {
        }

        // Initialize and insert waveform drawable.
        val value = data["preview"]
        var preview: ByteArray? = null
        if (value is String) {
            preview = Base64.decode(value as String?, Base64.DEFAULT)
        }else if (value is ByteArray){
            preview = value
        }

        if (preview != null && preview.size > MIN_AUDIO_PREVIEW_LENGTH) {
            val metrics = ctx.resources.displayMetrics
            val width = mViewport * 0.8f - bounds.width() - 4 * IMAGE_H_PADDING * metrics.density
            waveDrawable = WaveDrawable(res)
            waveDrawable.bounds = Rect(0, 0, width.toInt(), (bounds.height() * 0.9f).toInt())
            waveDrawable.callback = mContainer
            if (duration != null) {
                waveDrawable.setDuration(duration.toInt())
            }
            waveDrawable.put(preview)
        } else {
            waveDrawable = null
            result.append(res.getText(R.string.unavailable))
        }

        val aControl: AudioControlCallback = object : AudioControlCallback {
            override fun reset() {
                waveDrawable?.reset()
            }

            override fun pause() {
                play.setState(intArrayOf())
                mContainer.postInvalidate()
                waveDrawable?.stop()
            }

            override fun resume() {
                waveDrawable?.start()
            }
        }

        if (waveDrawable != null) {
            val wave = ImageSpan(waveDrawable, ImageSpan.ALIGN_BASELINE)
            result.append(SpannableStringBuilder().append(
                " ",
                wave,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            ),
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        var clickAt = -1
                        val tag = widget.getTag(R.id.click_coordinates)
                        if (tag is Point) {
                            clickAt = tag.x - widget.paddingLeft
                        }
                        widget.tag = null

                        val tv = widget as TextView
                        val tvl = tv.layout
                        val fullText = tv.text as Spanned
                        val startX = tvl.getPrimaryHorizontal(fullText.getSpanStart(wave))
                        val endX = tvl.getPrimaryHorizontal(fullText.getSpanEnd(wave))
                        val seekPosition = (clickAt - startX) / (endX - startX)
                        waveDrawable.seekTo(seekPosition)
                        mContainer.postInvalidate()
                        mClicker?.onClick("AU", data, AudioClickAction(seekPosition, aControl))
                    }

                    override fun updateDrawState(ds: TextPaint) {}
                }, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            waveDrawable.seekTo(0f)
            waveDrawable.setOnCompletionListener {
                play.setState(intArrayOf())
                mContainer.postInvalidate()
            }
        }

        if (mClicker != null) {
            // Make image clickable by wrapping ImageSpan into a ClickableSpan.
            result.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val state = play.state
                    val action: AudioClickAction.Action
                    if (state.size > 0 && state[0] == android.R.attr.state_checked) {
                        play.setState(intArrayOf())
                        action = AudioClickAction.Action.PAUSE
                        waveDrawable?.stop()
                    } else {
                        play.setState(intArrayOf(android.R.attr.state_checked))
                        action = AudioClickAction.Action.PLAY
                        waveDrawable?.start()
                    }
                    mClicker.onClick("AU", data, AudioClickAction(action, aControl))
                    mContainer.postInvalidate()
                }

                // Ignored.
                override fun updateDrawState(ds: TextPaint) {}
            }, 0, result.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        // Insert duration on the next line as small text.
        result.append("\n")

        var strDur = if (duration != null) " " + millisToTime(duration, true) else null
        if (TextUtils.isEmpty(strDur)) {
            strDur = " -:--"
        }

        val small = SpannableStringBuilder()
            .append(strDur, RelativeSizeSpan(0.8f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Add space on the left to make time appear under the waveform.
        result.append(
            small,
            LeadingMarginSpan.Standard(bounds.width()),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return result
    }

    override fun handleImage(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        if (data == null) {
            return null
        }

        var result: SpannableStringBuilder? = null
        // Bitmap dimensions specified by the sender.
        var width = 0
        var height = 0
        var tmp: Any
        if (data["width"].also { tmp = it!! } is Number) {
            width = (tmp as Number).toInt()
        }
        if (data["height"].also { tmp = it!! } is Number) {
            height = (tmp as Number).toInt()
        }

        // Calculate scaling factor for images to fit into the viewport.
        val metrics = ctx!!.resources.displayMetrics
        var scale = scaleBitmap(width, height, mViewport, metrics.density)
        // Bitmap dimensions specified by the sender converted to viewport size in display pixels.
        var scaledWidth = 0
        var scaledHeight = 0
        if (scale > 0) {
            scaledWidth = (width * scale * metrics.density).toInt()
            scaledHeight = (height * scale * metrics.density).toInt()
        }

        var span: CharacterStyle? = null
        var bmpPreview: Bitmap? = null

        // Inline image.
        var value = data["val"]
        if (value != null) {
            try {
                // True if inline image is only a preview: try to use out of band image (default).
                var isPreviewOnly = true
                // If the message is not yet sent, the bits could be raw byte[] as opposed to
                // base64-encoded.
                val bits = if ((value is String)) Base64.decode(
                    value as String?,
                    Base64.DEFAULT
                ) else (value as ByteArray?)!!
                bmpPreview = BitmapFactory.decodeByteArray(bits, 0, bits.size)
                if (bmpPreview != null) {
                    // Check if the inline bitmap is big enough to be used as primary image.
                    val previewWidth = bmpPreview.width
                    val previewHeight = bmpPreview.height
                    if (scale == 0f) {
                        // If dimensions are not specified in the attachment metadata, try to use bitmap dimensions.
                        scale = scaleBitmap(previewWidth, previewHeight, mViewport, metrics.density)
                        if (scale != 0f) {
                            // Because sender-provided dimensions are unknown or invalid we have to use
                            // this inline image as the primary one (out of band image is ignored).
                            isPreviewOnly = false
                            scaledWidth = (previewWidth * scale * metrics.density).toInt()
                            scaledHeight = (previewHeight * scale * metrics.density).toInt()
                        }
                    }

                    val oldBmp: Bitmap = bmpPreview
                    if (scale == 0f) {
                        // Can't scale the image. There must be something wrong with it.
                        bmpPreview = null
                    } else {
                        bmpPreview =
                            Bitmap.createScaledBitmap(bmpPreview, scaledWidth, scaledHeight, true)
                        // Check if the image is big enough to use as the primary one (ignoring possible full-size
                        // out-of-band image). If it's not already suitable for preview don't bother.
                        isPreviewOnly =
                            isPreviewOnly && previewWidth * metrics.density < scaledWidth * 0.35f
                    }
                    oldBmp.recycle()
                } else {
                    Log.w(TAG, "Failed to decode preview bitmap")
                }

                if (bmpPreview != null && !isPreviewOnly) {
                    span = ImageSpan(ctx, bmpPreview)
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Broken image preview", ex)
            }
        }

        // Out of band image.
        if (span == null && data["ref"].also { value = it } is String) {
            val ref = value as String?
            val url: URL = TextHelper.toAbsoluteURL(ref!!)
            if (url != null) {
                var fg: Drawable?
                var bg: Drawable? = null

                // "Image loading" placeholder.
                val placeholder: Drawable
                if (bmpPreview != null) {
                    placeholder = BitmapDrawable(ctx.resources, bmpPreview)
                    bg = placeholder
                } else {
                    fg = AppCompatResources.getDrawable(ctx, R.drawable.ic_image)
                    fg?.setBounds(0, 0, fg.intrinsicWidth, fg.intrinsicHeight)
                    placeholder = ImagesHelper.getPlaceholder(ctx, fg!!, null, scaledWidth, scaledHeight)
                }

                // "Failed to load image" placeholder.
                fg = AppCompatResources.getDrawable(ctx, R.drawable.ic_broken_image)
                fg?.setBounds(0, 0, fg.intrinsicWidth, fg.intrinsicHeight)
                val onError: Drawable =
                    ImagesHelper.getPlaceholder(ctx, fg!!, bg, scaledWidth, scaledHeight)

                // robin add
                // 如果宽高为 0，动态计算
                if (width == 0 || height == 0) {
                    val metrics = ctx!!.resources.displayMetrics
                    val screenWidth = metrics.widthPixels
                    val screenHeight = metrics.heightPixels

                    // 默认比例 (16:9)
                    val aspectRatio = 16f / 9f
                    val calculatedWidth = screenWidth
                    val calculatedHeight = (calculatedWidth / aspectRatio).toInt()

                    // 限制高度，避免超出屏幕
                    val maxAllowedHeight = (screenHeight * 0.75).toInt()
                    scaledWidth = calculatedWidth
                    scaledHeight = minOf(calculatedHeight, maxAllowedHeight)
                }
                // 自动加载远端图片的控件
                span = RemoteImageSpan(
                    mContainer,
                    scaledWidth,
                    scaledHeight,
                    false,
                    placeholder,
                    onError
                )
                span.load(url)
            }
        }



        if (span == null) {
            // If the image cannot be decoded for whatever reason, show a 'broken image' icon.
            val broken = AppCompatResources.getDrawable(ctx, R.drawable.ic_broken_image)
            if (broken != null) {
                broken.setBounds(0, 0, broken.intrinsicWidth, broken.intrinsicHeight)
                span =
                    ImageSpan(ImagesHelper.getPlaceholder(ctx, broken, null, scaledWidth, scaledHeight))
                result = assignStyle(span!!, content)
            }
        } else if (mClicker != null) {
            // Make image clickable by wrapping ImageSpan into a ClickableSpan.
            result = assignStyle(span, content)
            result!!.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    mClicker.onClick("IM", data, null)
                }
            }, 0, result.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            result = assignStyle(span, content)
        }

        return result
    }

    override fun handleFormRow(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return join(content)
    }

    override  fun handleForm(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        if (content == null || content.size == 0) {
            return null
        }

        // Add line breaks between form elements.
        val span = SpannableStringBuilder()
        for (ssb in content) {
            span.append(ssb).append("\n")
        }
        mContainer.setLineSpacing(0f, FORM_LINE_SPACING)
        return span
    }

    override fun handleAttachment(ctx: Context?, data: Map<String, Any>?): SpannableStringBuilder? {
        if (data == null) {
            return null
        }

        try {
            if ("application/json" == data["mime"]) {
                // Skip JSON attachments. They are not meant to be user-visible.
                return null
            }
        } catch (ignored: ClassCastException) {
        }

        val result = SpannableStringBuilder()
        // Insert document icon
        var icon = AppCompatResources.getDrawable(ctx!!, R.drawable.ic_file)
        icon!!.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        val span = ImageSpan(icon, ImageSpan.ALIGN_BOTTOM)
        val bounds = span.drawable.bounds
        result.append(" ", span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        result.setSpan(SubscriptSpan(), 0, result.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        // Insert document's file name
        var fname: String? = null
        try {
            fname = data["name"] as String?
        } catch (ignored: NullPointerException) {
        } catch (ignored: ClassCastException) {
        }
        if (TextUtils.isEmpty(fname)) {
            fname = ctx.resources.getString(R.string.default_attachment_name)
        } else
            if (fname!!.length > MAX_FILE_LENGTH) {
                fname = fname.substring(0, MAX_FILE_LENGTH / 2 - 1) + "…" + fname.substring(
                    fname.length - MAX_FILE_LENGTH / 2
                )
            }

        result.append(fname, TypefaceSpan("monospace"), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        try {
            val size: String = TextHelper.bytesToHumanSize(data["size"] as Long)
            if (!TextUtils.isEmpty(size)) {
                result.append(
                    "\u2009($size)",
                    ForegroundColorSpan(Color.GRAY),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        } catch (ignored: NullPointerException) {
        } catch (ignored: ClassCastException) {
        }

        if (mClicker == null) {
            return result
        }

        // Add download link.

        // Do we have attachment bits out-of-band or in-band?
        val valid = (data["ref"] is String) || (data["val"] != null)

        // Insert linebreak then a clickable [↓ save] or [(!) unavailable] line.
        result.append("\n")
        val saveLink = SpannableStringBuilder()
        // Add 'download file' icon
        icon = AppCompatResources.getDrawable(
            ctx,
            if (valid) R.drawable.ic_download_link else R.drawable.ic_error_gray
        )
        val metrics = ctx.resources.displayMetrics
        icon!!.setBounds(
            0, 0,
            (ICON_SIZE_DP * metrics.density).toInt(),
            (ICON_SIZE_DP * metrics.density).toInt()
        )
        saveLink.append(
            " ",
            ImageSpan(icon, ImageSpan.ALIGN_BOTTOM),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        if (valid) {
            // Clickable "save".
            saveLink.append(
                ctx.resources.getString(R.string.download_attachment),
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        mClicker.onClick("EX", data, null)
                    }
                }, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        } else {
            // Grayed-out "unavailable".
            saveLink.append(
                " " + ctx.resources.getString(R.string.unavailable),
                ForegroundColorSpan(Color.GRAY), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // Add space on the left to make the link appear under the file name.
        result.append(
            saveLink,
            LeadingMarginSpan.Standard(bounds.width()),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Append thin space after the link, otherwise the whole line to the right is clickable.
        result.append('\u2009')
        return result
    }

    // Button: URLSpan wrapped into LineHeightSpan and then BorderedSpan.
    override  fun handleButton(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        // This is needed for button shadows.
        mContainer.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        val metrics = ctx!!.resources.displayMetrics
        // Size of a DIP pixel.
        val dipSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1.0f, metrics)

        // Make button clickable.
        val node = SpannableStringBuilder()
        node.append(join(content), object : ClickableSpan() {
            override fun onClick(widget: View) {
                mClicker?.onClick("BN", data, null)
            }

            // Ignored.
            override fun updateDrawState(ds: TextPaint) {}
        }, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // URLSpan into ButtonSpan.
        node.setSpan(
            ButtonSpan(ctx, mFontSize, dipSize), 0, node.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Append a thin space after a button, otherwise the whole line after the button
        // becomes clickable if the button is the last element in a line.
        return node.append('\u2009')
    }

    override  fun handleQuote(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        val node = handleQuote_Impl(ctx, content)
        // Increase spacing between the quote and the subsequent text.
        return node.append("\n\n", RelativeSizeSpan(0.3f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    override  fun handleVideoCall(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        if (data == null) {
            return handleUnknown(ctx, content, null)
        }

        val result = SpannableStringBuilder()
        // Insert document icon
        var icon = AppCompatResources.getDrawable(ctx!!, R.drawable.ic_call)
        icon!!.setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
        val span = ImageSpan(icon, ImageSpan.ALIGN_BOTTOM)
        val bounds = span.drawable.bounds
        result.append(" ", span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        result.setSpan(SubscriptSpan(), 0, result.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        var `val` = data["incoming"]
        val incoming = if (`val` is Boolean) `val` else false
        result.append(
            ctx.getString(if (incoming) R.string.incoming_call else R.string.outgoing_call),
            RelativeSizeSpan(1.15f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        `val` = data["duration"]
        val duration = if (`val` is Number) `val`.toInt() else 0

        `val` = data["state"]
        val state = if (`val` is String) `val` else ""

        result.append("\n")

        val second = SpannableStringBuilder()
        val success = !mutableListOf("declined", "disconnected", "missed").contains(state)
        icon = AppCompatResources.getDrawable(
            ctx,
            if (incoming) (if (success) R.drawable.ic_arrow_sw else R.drawable.ic_arrow_missed) else (if (success) R.drawable.ic_arrow_ne else R.drawable.ic_arrow_cancelled)
        )
        icon!!.setBounds(
            0,
            0,
            (icon.intrinsicWidth * 0.67).toInt(),
            (icon.intrinsicHeight * 0.67).toInt()
        )
        icon.setTint(if (success) -0xcc77cd else -0x66cccd)
        second.append(
            " ",
            ImageSpan(icon, ImageSpan.ALIGN_BOTTOM),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        second.append(" ")
        if (duration > 0) {
            second.append(millisToTime(duration, false))
        } else {
            second.append(ctx.getString(callStatus(incoming, state)))
        }
        // Shift second line to the right.
        result.append(
            second,
            LeadingMarginSpan.Standard(bounds.width()),
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        return result
    }

    // Unknown or unsupported element.
    override  fun handleUnknown(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        val result: SpannableStringBuilder?
        val unkn = AppCompatResources.getDrawable(ctx!!, R.drawable.ic_unkn_type)
        unkn!!.setBounds(0, 0, unkn.intrinsicWidth, unkn.intrinsicHeight)

        if (data != null) {
            // Does object have viewport dimensions?
            var width = 0
            var height = 0
            var tmp: Any
            if (data["width"].also { tmp = it!! } is Number) {
                width = (tmp as Number).toInt()
            }
            if (data["height"].also { tmp = it!! } is Number) {
                height = (tmp as Number).toInt()
            }

            if (width <= 0 || height <= 0) {
                return handleAttachment(ctx, data)
            }

            // Calculate scaling factor for images to fit into the viewport.
            val metrics = ctx.resources.displayMetrics
            val scale = scaleBitmap(width, height, mViewport, metrics.density)
            // Bitmap dimensions specified by the sender converted to viewport size in display pixels.
            var scaledWidth = 0
            var scaledHeight = 0
            if (scale > 0) {
                scaledWidth = (width * scale * metrics.density).toInt()
                scaledHeight = (height * scale * metrics.density).toInt()
            }

            val span: CharacterStyle =
                ImageSpan(ImagesHelper.getPlaceholder(ctx, unkn, null, scaledWidth, scaledHeight))
            result = assignStyle(span, content)
        } else {
            result = SpannableStringBuilder()
            result.append(
                " ",
                ImageSpan(unkn, ImageSpan.ALIGN_BOTTOM),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            result.append(" ")
            if (content != null) {
                result.append(join(content))
            } else {
                result.append(ctx.getString(R.string.unknown))
            }
        }

        return result
    }

    // Plain (unstyled) content.
    override fun handlePlain(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return join(content)
    }

    interface ClickListener {
        fun onClick(type: String?, data: Map<String, Any>?, params: Any?): Boolean
    }

    interface AudioControlCallback {
        fun reset()
        fun pause()
        fun resume()
    }

    class AudioClickAction {
        enum class Action {
            PLAY, PAUSE, SEEK
        }

        var seekTo: Float?
        var action: Action

        var control: AudioControlCallback? = null

        private constructor(action: Action) {
            this.action = action
            seekTo = null
        }

        constructor(action: Action, callback: AudioControlCallback?) : this(action) {
            control = callback
        }

        constructor(seekTo: Float, callback: AudioControlCallback?) {
            action = Action.SEEK
            this.seekTo = seekTo
            control = callback
        }
    }

    companion object {
        private const val TAG = "FullFormatter"

        private const val FORM_LINE_SPACING = 1.2f

        // Additional horizontal padding otherwise images sometimes fail to render.
        private const val IMAGE_H_PADDING = 8

        // Size of Download and Error icons in DP.
        private const val ICON_SIZE_DP = 16

        // Formatting parameters of the quoted text;
        private const val CORNER_RADIUS_DP = 4
        private const val QUOTE_STRIPE_WIDTH_DP = 4
        private const val STRIPE_GAP_DP = 8

        private const val MAX_FILE_LENGTH = 28

        private const val MIN_AUDIO_PREVIEW_LENGTH = 16

        private var sColorsDark: TypedArray? = null
        private var sDefaultColor = 0

        fun handleMention_Impl(
            content: List<SpannableStringBuilder?>?,
            data: Map<String, Any>?
        ): SpannableStringBuilder? {
            var color = sDefaultColor

            if (data != null) {
                try {
                    color = colorMention(data["val"] as String?)
                } catch (ignored: ClassCastException) {
                }
            }

            return assignStyle(ForegroundColorSpan(color), content)
        }

        private fun colorMention(uid: String?): Int {
            return if (TextUtils.isEmpty(uid)) sDefaultColor else sColorsDark!!.getColor(
                (abs(uid.hashCode().toDouble()) % sColorsDark!!.length()).toInt(), sDefaultColor
            )
        }

        // Scale image dimensions to fit under the given viewport size.
        protected fun scaleBitmap(
            srcWidth: Int,
            srcHeight: Int,
            viewportWidth: Int,
            density: Float
        ): Float {
            if (srcWidth == 0 || srcHeight == 0) {
                return 0f
            }

            // Convert DP to pixels.
            val width = srcWidth * density
            val height = srcHeight * density
            val maxWidth = viewportWidth - IMAGE_H_PADDING * density

            // Make sure the scaled bitmap is no bigger than the viewport size;
            val scaleX = (min(width.toDouble(), maxWidth.toDouble()) / width).toFloat()
            val scaleY = (min(height.toDouble(), (maxWidth * 0.75f).toDouble()) / height).toFloat()
            return min(scaleX.toDouble(), scaleY.toDouble()).toFloat()
        }

        fun handleQuote_Impl(
            ctx: Context?,
            content: List<SpannableStringBuilder?>?
        ): SpannableStringBuilder {
            val outer = SpannableStringBuilder()
            val inner = SpannableStringBuilder()
            inner.append("\n", RelativeSizeSpan(0.25f), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            inner.append(join(content))
            // Adding a line break with some non-breaking white space around it to create extra padding.
            inner.append(
                "\u00A0\u00A0\u00A0\u00A0\n\u00A0", RelativeSizeSpan(0.2f),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            val res = ctx!!.resources
            val metrics = res.displayMetrics
            val style = QuotedSpan(
                res.getColor(R.color.colorReplyBubble, null),
                CORNER_RADIUS_DP * metrics.density,
                res.getColor(R.color.colorAccent, null),
                QUOTE_STRIPE_WIDTH_DP * metrics.density,
                STRIPE_GAP_DP * metrics.density
            )
            return outer.append(inner, style, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }
}
