package com.bird2fish.birdtalksdk.format

import android.content.Context
import android.text.SpannableStringBuilder

// Formatter used for copying text to clipboard.
class CopyFormatter(context: Context?) : FontFormatter(context, 0f) {
    // Button as '[ content ]'.
    override fun handleButton(
        ctx: Context?, content: List<SpannableStringBuilder?>?,
        data: Map<String, Any>?
    ): SpannableStringBuilder? {
        val node = SpannableStringBuilder()
        // Non-breaking space as padding in front of the button.
        node.append("\u00A0[\u00A0")
        node.append(join(content))
        node.append("\u00A0]")
        return node
    }
}
