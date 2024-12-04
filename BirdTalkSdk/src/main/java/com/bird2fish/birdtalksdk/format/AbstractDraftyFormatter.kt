package com.bird2fish.birdtalksdk.format

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.Spanned
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.model.Drafty
import java.util.Stack
import kotlin.math.floor

abstract class AbstractDraftyFormatter<T : Spanned?> protected constructor(protected val mContext: Context) :
    Drafty.Formatter<T> {
    protected abstract fun handleStrong(content: List<T?>?): T?

    protected abstract fun handleEmphasized(content: List<T?>?): T?

    protected abstract fun handleDeleted(content: List<T?>?): T?

    protected abstract fun handleCode(content: List<T?>?): T?

    protected abstract fun handleHidden(content: List<T?>?): T?

    protected abstract fun handleLineBreak(): T?

    // URL.
    protected abstract fun handleLink(ctx: Context?, content: List<T?>?, data: Map<String, Any>?): T?

    // Mention @user.
    protected abstract fun handleMention(
        ctx: Context?,
        content: List<T?>?,
        data: Map<String, Any>?
    ): T?

    // Hashtag #searchterm.
    protected abstract fun handleHashtag(
        ctx: Context?,
        content: List<T?>?,
        data: Map<String, Any>?
    ): T?

    // Embedded voice mail.
    protected abstract fun handleAudio(ctx: Context?, content: List<T?>?, data: Map<String, Any>?): T?

    // Embedded image.
    protected abstract fun handleImage(ctx: Context?, content: List<T?>?, data: Map<String, Any>?): T?

    // File attachment.
    protected abstract fun handleAttachment(ctx: Context?, data: Map<String, Any>?): T?

    // Button: clickable form element.
    protected abstract fun handleButton(
        ctx: Context?,
        content: List<T?>?,
        data: Map<String, Any>?
    ): T?

    // Grouping of form elements.
    protected abstract fun handleFormRow(
        ctx: Context?,
        content: List<T?>?,
        data: Map<String, Any>?
    ): T?

    // Interactive form.
    protected abstract fun handleForm(ctx: Context?, content: List<T?>?, data: Map<String, Any>?): T?

    // Quoted block.
    protected abstract fun handleQuote(ctx: Context?, content: List<T?>?, data: Map<String, Any>?): T?

    // Video call.
    protected abstract fun handleVideoCall(
        ctx: Context?,
        content: List<T?>?,
        data: Map<String, Any>?
    ): T?

    // Unknown or unsupported element.
    protected abstract fun handleUnknown(
        ctx: Context?,
        content: List<T?>?,
        data: Map<String, Any>?
    ): T?

    // Unstyled content
    protected abstract fun handlePlain(content: List<T?>?): T?

    abstract override fun wrapText(text: CharSequence): T?

    override fun apply(tp: String?, data: Map<String, Any>?, content: List<T?>?, context: Stack<String>): T? {
        if (tp != null) {
            val span = when (tp) {
                "ST" -> handleStrong(content)
                "EM" -> handleEmphasized(content)
                "DL" -> handleDeleted(content)
                "CO" -> handleCode(content)
                "HD" ->                     // Hidden text
                    handleHidden(content)

                "BR" -> handleLineBreak()
                "LN" -> handleLink(mContext, content, data)
                "MN" -> handleMention(mContext, content, data)
                "HT" -> handleHashtag(mContext, content, data)
                "AU" ->                     // Audio player.
                    handleAudio(mContext, content, data)

                "IM" ->                     // Additional processing for images
                    handleImage(mContext, content, data)

                "EX" ->                     // Attachments; attachments cannot have sub-elements.
                    handleAttachment(mContext, data)

                "BN" ->                     // Button
                    handleButton(mContext, content, data)

                "FM" ->                     // Form
                    handleForm(mContext, content, data)

                "RW" ->                     // Form element formatting is dependent on element content.
                    handleFormRow(mContext, content, data)

                "QQ" ->                     // Quoted block.
                    handleQuote(mContext, content, data)

                "VC" ->                     // Video call.
                    handleVideoCall(mContext, content, data)

                else ->                     // Unknown element
                    handleUnknown(mContext, content, data)
            }
            return span
        }
        return handlePlain(content)
    }

    companion object {
        @JvmStatic
        protected fun join(content: List<SpannableStringBuilder?>?): SpannableStringBuilder? {
            var ssb: SpannableStringBuilder? = null
            if (content != null) {
                val it = content.iterator()
                ssb = it.next()
                while (it.hasNext()) {
                    ssb!!.append(it.next())
                }
            }
            return ssb
        }

        @JvmStatic
        protected fun assignStyle(
            style: Any,
            content: List<SpannableStringBuilder?>?
        ): SpannableStringBuilder? {
            val ssb = join(content)
            ssb?.setSpan(style, 0, ssb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            return ssb
        }

        // Convert milliseconds to '00:00' format.
        @JvmStatic
        protected fun millisToTime(millis: Number, fixedMin: Boolean): StringBuilder {
            val sb = StringBuilder()
            val duration = millis.toFloat() / 1000
            val min = floor((duration / 60f).toDouble()).toInt()
            if (fixedMin && min < 10) {
                sb.append("0")
            }
            sb.append(min).append(":")
            val sec = (duration % 60f).toInt()
            if (sec < 10) {
                sb.append("0")
            }
            return sb.append(sec)
        }

        @JvmStatic
        protected fun callStatus(incoming: Boolean, event: String?): Int {
            var comment = 0
            comment = when (event) {
                "declined" -> R.string.declined_call
                "missed" -> if (incoming) R.string.missed_call else R.string.cancelled_call
                "started" -> R.string.connecting_call
                "accepted" -> R.string.in_progress_call
                else -> R.string.disconnected_call
            }
            return comment
        }
    }
}
