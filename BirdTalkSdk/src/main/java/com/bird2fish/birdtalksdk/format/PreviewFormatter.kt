package com.bird2fish.birdtalksdk.format

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.TypedValue
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import com.bird2fish.birdtalksdk.R

// Drafty formatter for creating one-line message previews.
open class PreviewFormatter(context: Context?, private val mFontSize: Float) : AbstractDraftyFormatter<SpannableStringBuilder>(context!!) {

    override fun wrapText(text: CharSequence): SpannableStringBuilder? {
        return if (text != null) SpannableStringBuilder(text) else null
    }

    override fun handleStrong(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return assignStyle(StyleSpan(Typeface.BOLD), content)
    }




    override fun handleEmphasized(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return assignStyle(StyleSpan(Typeface.ITALIC), content)
    }

    override fun handleDeleted(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return assignStyle(StrikethroughSpan(), content)
    }

    override fun handleCode(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return assignStyle(TypefaceSpan("monospace"), content)
    }

    override fun handleHidden(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return null
    }

    override fun handleLineBreak(): SpannableStringBuilder? {
        return SpannableStringBuilder(" ")
    }

    override fun handleLink(ctx: Context?, content: List<SpannableStringBuilder?>?, data: Map<String, Any>?): SpannableStringBuilder? {
        return assignStyle(
            ForegroundColorSpan(ctx!!.resources.getColor(R.color.colorAccent)),
            content
        )
    }

    override open fun handleMention(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return join(content)
    }

    override fun handleHashtag(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return join(content)
    }

    protected fun annotatedIcon(
        ctx: Context?,
        @DrawableRes iconId: Int,
        str: String?
    ): SpannableStringBuilder? {
        var node: SpannableStringBuilder? = null
        val icon = AppCompatResources.getDrawable(ctx!!, iconId)
        if (icon != null) {
            val res = ctx.resources
            icon.setTint(res.getColor(R.color.colorDarkGray))
            //int color = ContextCompat.getColor(ctx, R.color.colorDarkGray);
            icon.setBounds(
                0,
                0,
                (mFontSize * DEFAULT_ICON_SCALE).toInt(),
                (mFontSize * DEFAULT_ICON_SCALE).toInt()
            )
            node = SpannableStringBuilder(" ")
            node.setSpan(
                ImageSpan(icon, ImageSpan.ALIGN_BOTTOM), 0, node.length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            if (str != null) {
                node.append(" ").append(str)
            }
        }
        return node
    }

    protected open fun annotatedIcon(
        ctx: Context?,
        @DrawableRes iconId: Int,
        @StringRes stringId: Int
    ): SpannableStringBuilder? {
        val str = if (stringId != 0) ctx!!.resources.getString(stringId) else null
        return annotatedIcon(ctx, iconId, str)
    }

    override fun handleAudio(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        val node = annotatedIcon(ctx, R.drawable.ic_mic_ol, 0)
        node!!.append(" ")
        var duration: Number? = null
        try {
            duration = data!!["duration"] as Number?
        } catch (ignored: NullPointerException) {
        } catch (ignored: ClassCastException) {
        }
        if (duration != null) {
            node.append(millisToTime(duration, false))
        } else {
            node.append(ctx!!.resources.getString(R.string.audio))
        }
        return node
    }

    override fun handleImage(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return annotatedIcon(ctx, R.drawable.ic_image_ol, R.string.picture)
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

        return annotatedIcon(ctx, R.drawable.ic_attach_ol, R.string.attachment)
    }

    override fun handleButton(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        val outer = SpannableStringBuilder()
        // Non-breaking space as padding in front of the button.
        outer.append("\u00A0")
        // Size of a DIP pixel.
        val dipSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 1.0f,
            ctx!!.resources.displayMetrics
        )

        val inner = join(content)
        // Make button font slightly smaller.
        inner!!.setSpan(RelativeSizeSpan(0.8f), 0, inner.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Change background color and draw a box around text.
        inner.setSpan(
            LabelSpan(ctx, mFontSize, dipSize),
            0,
            inner.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return outer.append(inner)
    }

    override fun handleFormRow(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return SpannableStringBuilder(" ").append(join(content))
    }

    override open fun handleForm(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        val node = annotatedIcon(ctx, R.drawable.ic_form_ol, R.string.form)
        return node!!.append(": ").append(join(content))
    }

    override  fun handleQuote(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        // Not showing quoted content in preview.
        return null
    }

    override fun handleVideoCall(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        if (data == null) {
            return handleUnknown(ctx, content, null)
        }

        var `val` = data["incoming"]
        val incoming = if (`val` is Boolean) `val` else false
        `val` = data["duration"]
        val duration = if (`val` is Number) `val`.toInt() else 0
        `val` = data["state"]
        val state = if (`val` is String) `val` else ""

        val success = !mutableListOf("declined", "disconnected", "missed").contains(state)
        val status =
            " " + (if (duration > 0) millisToTime(duration, false).toString() else ctx!!.getString(
                callStatus(incoming, state)
            ))
        return annotatedIcon(
            ctx,
            if (incoming) (if (success) R.drawable.ic_call_received else R.drawable.ic_call_missed) else (if (success) R.drawable.ic_call_made else R.drawable.ic_call_cancelled),
            status
        )
    }

    override fun handleUnknown(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return annotatedIcon(ctx, R.drawable.ic_unkn_type_ol, R.string.unknown)
    }

    override fun handlePlain(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
        return join(content)
    }

    companion object {
        private const val DEFAULT_ICON_SCALE = 1.3f
    }
}
