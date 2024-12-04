package com.bird2fish.birdtalksdk.format

import android.content.Context
import android.content.res.TypedArray
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Log
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper

// Display quoted content.
open class QuoteFormatter(private val mParent: View, fontSize: Float) : PreviewFormatter(mParent.context, fontSize) {
    init {
        val res = mParent.resources
        if (sColorsDark == null) {
            sColorsDark = res.obtainTypedArray(R.array.letter_tile_colors_dark)
            sTextColor = res.getColor(R.color.colorReplyText)
        }
    }

    override fun handleLineBreak(): SpannableStringBuilder? {
        return SpannableStringBuilder("\n")
    }

    override fun handleMention(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return FullFormatter.handleMention_Impl(content, data)
    }

    override fun handleImage(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        if (data == null) {
            return null
        }

        val res = ctx!!.resources

        // Using fixed dimensions for the image.
        val metrics = res.displayMetrics
        val size = (ImagesHelper.REPLY_THUMBNAIL_DIM * metrics.density) as Int

        var filename = data["name"]
        filename = if (filename is String) {
            shortenFileName(filename)
        } else {
            res.getString(R.string.picture)
        }

        // If the image cannot be decoded for whatever reason, show a 'broken image' icon.
        var broken = AppCompatResources.getDrawable(ctx, R.drawable.ic_broken_image)
        broken!!.setBounds(0, 0, broken.intrinsicWidth, broken.intrinsicHeight)
        broken = ImagesHelper.getPlaceholder(ctx, broken, null, size, size)

        val node = SpannableStringBuilder()
        var span: CharacterStyle? = null

        var value: Any

        // Trying to use in-band image first: we don't need the full image at "ref" to generate a tiny preview.
        if ((data["val"].also { value = it!! }) != null) {
            // Inline image.
            var thumbnail: Drawable? = null
            try {
                // If the message is not yet sent, the bits could be raw byte[] as opposed to
                // base64-encoded.
                val bits = if ((value is String)) Base64.decode(
                    value as String,
                    Base64.DEFAULT
                ) else (value as ByteArray)
                val bmp = BitmapFactory.decodeByteArray(bits, 0, bits.size)
                if (bmp != null) {
                    thumbnail = BitmapDrawable(res, ImagesHelper.scaleSquareBitmap(bmp, size))
                    thumbnail!!.setBounds(0, 0, size, size)
                    span = StyledImageSpan(
                        thumbnail,
                        RectF(
                            IMAGE_PADDING * metrics.density,
                            IMAGE_PADDING * metrics.density,
                            IMAGE_PADDING * metrics.density,
                            IMAGE_PADDING * metrics.density
                        )
                    )
                }
            } catch (ex: Exception) {
                Log.w(TAG, "Broken image preview", ex)
            }
        } else if (data["ref"].also { value = it!! } is String) {
            // If small in-band image is not available, get the large one and shrink.
            Log.i(TAG, "Out-of-band $value")

            span = RemoteImageSpan(
                mParent, size, size, true,
                AppCompatResources.getDrawable(ctx, R.drawable.ic_image)!!, broken
            )
            span.load(TextHelper.toAbsoluteURL(value as String))
        }

        if (span == null) {
            span = StyledImageSpan(
                broken,
                RectF(
                    IMAGE_PADDING * metrics.density,
                    IMAGE_PADDING * metrics.density,
                    IMAGE_PADDING * metrics.density,
                    IMAGE_PADDING * metrics.density
                )
            )
        }

        node.append(" ", span, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        node.append(" ").append(filename as String?)

        return node
    }

    override fun handleQuote(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        // Quote within quote is not displayed;
        return null
    }

    override fun handlePlain(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        val node = join(content)
        if (node != null && node.getSpans(0, node.length, Any::class.java).size == 0) {
            // Use default text color for plain text strings.
            node.setSpan(
                ForegroundColorSpan(sTextColor), 0, node.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        return node
    }

    companion object {
        private const val TAG = "QuoteFormatter"

        private const val IMAGE_PADDING = 2 //dip
        private const val MAX_FILE_NAME_LENGTH = 16

        private var sColorsDark: TypedArray? = null
        private var sTextColor = 0

        private fun shortenFileName(filename: String): String {
            val len = filename.length
            return if (len > MAX_FILE_NAME_LENGTH) (filename.substring(
                0,
                MAX_FILE_NAME_LENGTH / 2 - 1
            ) + '…'
                    + filename.substring(len - MAX_FILE_NAME_LENGTH / 2 + 1)) else filename
        }
    }
}
