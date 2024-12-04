package com.bird2fish.birdtalksdk.format

import android.content.Context
import android.text.SpannableStringBuilder
import android.widget.TextView

// Formatter for displaying reply previews before they are sent.
class SendReplyFormatter(container: TextView) : QuoteFormatter(container, container.textSize) {
    override fun handleQuote(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        // The entire preview is wrapped into a quote, so format content as if it's standalone (not quoted).
        return FullFormatter.handleQuote_Impl(ctx, content)
    }
}

