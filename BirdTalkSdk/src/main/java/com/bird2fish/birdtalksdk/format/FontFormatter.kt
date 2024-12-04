package com.bird2fish.birdtalksdk.format

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.annotation.StringRes
import com.bird2fish.birdtalksdk.R

// Drafty formatter for creating message previews in push notifications.
// Push notifications don't support ImageSpan or TypefaceSpan, consequently, using Unicode chars instead of icons.
open class FontFormatter(context: Context?, fontSize: Float) : PreviewFormatter(context, fontSize) {
    override fun annotatedIcon(
        ctx: Context?,
        charIndex: Int,
        @StringRes stringId: Int
    ): SpannableStringBuilder? {
        val node = SpannableStringBuilder(UNICODE_STRINGS[charIndex])
        return node.append(" ").append(ctx?.resources?.getString(stringId))
    }

    override fun handleAudio(
        ctx: Context?,
        content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return annotatedIcon(ctx!!, AUDIO, R.string.audio)
    }

    override fun handleImage(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return annotatedIcon(ctx!!, IMAGE, R.string.picture)
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
        return annotatedIcon(ctx!!, ATTACHMENT, R.string.attachment)
    }

    override fun handleForm(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        val node = annotatedIcon(ctx!!, FORM, R.string.form)
        if (node != null){
            return node.append(": ").append(join(content))
        }
        return null
    }

    override fun handleVideoCall(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return annotatedIcon(ctx!!, CALL, R.string.incoming_call)
    }

    override fun handleUnknown(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return annotatedIcon(ctx!!, UNKNOWN, R.string.unknown)
    }

    companion object {
        // Emoji characters from the stock font: Microphone 🎤 (audio), Camera 📷 (image), Paperclip 📎 (attachment),
        // Memo 📝 (form), 📞 (video call), Question-Mark ❓ (unknown).
        // These characters are present in Android 5 and up.
        private val UNICODE_STRINGS = arrayOf(
            "\uD83C\uDFA4", "\uD83D\uDCF7",
            "\uD83D\uDCCE", "\uD83D\uDCDD", "\uD83D\uDCDE", "\u2753"
        )

        // Index into character sets.
        private const val AUDIO = 0
        private const val IMAGE = 1
        private const val ATTACHMENT = 2
        private const val FORM = 3
        private const val CALL = 4
        private const val UNKNOWN = 5
    }
}
