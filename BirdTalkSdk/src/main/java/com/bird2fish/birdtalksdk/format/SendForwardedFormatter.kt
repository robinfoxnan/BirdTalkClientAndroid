package com.bird2fish.birdtalksdk.format

import android.content.Context
import android.text.SpannableStringBuilder
import android.widget.TextView
import com.bird2fish.birdtalksdk.R

// Formatter for displaying forwarded previews before they are sent.
class SendForwardedFormatter(container: TextView) :
    QuoteFormatter(container, container.getTextSize()) {
    override fun handleMention(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return FullFormatter.handleMention_Impl(content, data)
    }

    override fun handleQuote(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        return annotatedIcon(ctx, R.drawable.ic_quote_ol, 0)!!.append(" ")
    }
}

