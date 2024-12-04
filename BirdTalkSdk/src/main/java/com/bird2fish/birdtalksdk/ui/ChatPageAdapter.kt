package com.bird2fish.birdtalksdk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.IconMarginSpan
import android.text.style.StyleSpan
import android.util.Base64
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.format.FullFormatter
import com.bird2fish.birdtalksdk.format.QuoteFormatter
import com.bird2fish.birdtalksdk.model.MessageContent
import com.bird2fish.birdtalksdk.model.MessageStatus
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL


// 每个消息条目的内容布局部分
class ChatPageViewHolder internal constructor(itemView: View, val mViewType: Int) :
    RecyclerView.ViewHolder(itemView) {
    val mIcon: ImageView? = itemView.findViewById(R.id.icon)                                 // meta才有图标
    val mAvatar: ImageView? = itemView.findViewById(R.id.avatar)                             // 对方消息不一定有图标
    val mMessageBubble: View = itemView.findViewById(R.id.messageBubble)                     // 消息主题，这个都有
    val mDeliveredIcon: AppCompatImageView? = itemView.findViewById(R.id.messageViewedIcon)   // 自己的消息是否被查看，右侧才有
    val mDateDivider: TextView? = itemView.findViewById(R.id.dateDivider)                     // meta 似乎没有
    val mText: TextView = itemView.findViewById(R.id.messageText)                             // 消息主体，动态控件
    val mMeta: TextView = itemView.findViewById(R.id.messageMeta)                             // 附加消息：时间戳等
    val mUserName: TextView? = itemView.findViewById(R.id.userName)                           // 群聊一般才有
    val mSelected: View? = itemView.findViewById(R.id.selected)                                // meta 没有
    val mRippleOverlay: View? = itemView.findViewById(R.id.overlay)                            // meta 没有
    val mProgressContainer: View? = itemView.findViewById(R.id.progressContainer)              // 右侧的才有进度条容器页面
    val mProgressBar: ProgressBar? = itemView.findViewById(R.id.attachmentProgressBar)         // 右侧才有的进度条
    val mCancelProgress: AppCompatImageButton? = itemView.findViewById(R.id.attachmentProgressCancel)// 右侧才有的取消发送按钮
    val mProgress: View? = itemView.findViewById(R.id.progressPanel)                           // 右侧的才有进度条容器页面，根节点
    val mProgressResult: View? = itemView.findViewById(R.id.progressResult)                    // 右侧才有的进度结果
    val mGestureDetector: GestureDetector = GestureDetector(itemView.context, object : SimpleOnGestureListener() {
            override fun onLongPress(ev: MotionEvent) {
                itemView.performLongClick()
            }

            override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
                itemView.performClick()
                return super.onSingleTapConfirmed(ev)
            }

           // 通过叠加一层，方便显示按的效果
            override fun onShowPress(ev: MotionEvent) {
                if (mRippleOverlay != null) {
                    mRippleOverlay.isPressed = true
                    mRippleOverlay.postDelayed(Runnable { mRippleOverlay.isPressed = false }, 250)
                }
            }

            override fun onDown(ev: MotionEvent): Boolean {
                // Convert click coordinates in itemView to TexView.
                val item = IntArray(2)
                val text = IntArray(2)
                itemView.getLocationOnScreen(item)
                mText.getLocationOnScreen(text)

                val x = ev.x.toInt()
                val y = ev.y.toInt()

                // Make click position available to spannable.
               // mText.setTag(R.id.click_coordinates, Point(x, y))
                return super.onDown(ev)
            }
        })
    var seqId: Int = 0
}
class ChatPageAdapter(private val dataList: List<MessageContent>) : RecyclerView.Adapter<ChatPageViewHolder>() {

    companion object {
        const val VIEW_TYPE_SIDE_CENTER = 0b000001
        const val VIEW_TYPE_SIDE_LEFT   = 0b000010
        const val VIEW_TYPE_SIDE_RIGHT  = 0b000100
        const val VIEW_TYPE_TIP         = 0b001000
        const val VIEW_TYPE_AVATAR      = 0b010000
        const val VIEW_TYPE_DATE        = 0b100000
        const val VIEW_TYPE_INVALID     = 0b000000
    }
    private var fragment : ChatPageFragment? = null


    fun setView(view : ChatPageFragment?){
        this.fragment = view
    }



    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatPageViewHolder {


        // 创建一个新消息的泡泡
        var layoutId = -1
        if ((viewType and VIEW_TYPE_SIDE_CENTER) != 0) {
            layoutId = R.layout.layout_message_meta
        }
        else if ((viewType and VIEW_TYPE_SIDE_LEFT) != 0) {
            if ((viewType and VIEW_TYPE_AVATAR) != 0 && (viewType and VIEW_TYPE_TIP) != 0) {
                layoutId = R.layout.layout_message_left_single_avatar
            } else if ((viewType and VIEW_TYPE_TIP) != 0) {
                layoutId = R.layout.layout_message_left_single
            } else if ((viewType and VIEW_TYPE_AVATAR) != 0) {
                layoutId = R.layout.layout_message_left_avatar
            } else {
                layoutId = R.layout.layout_message_left
            }
        } else if ((viewType and VIEW_TYPE_SIDE_RIGHT) != 0) {
            if ((viewType and VIEW_TYPE_TIP) != 0) {
                layoutId = R.layout.layout_message_right_single
            } else {
                layoutId = R.layout.layout_message_right
            }
        }

        val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        if (v != null) {
            val dateBubble = v.findViewById<View>(R.id.dateDivider)
            if (dateBubble != null) {
                dateBubble.visibility =
                    if ((viewType and VIEW_TYPE_DATE) != 0) View.VISIBLE else View.GONE
            }
        }
        return ChatPageViewHolder(v, viewType)
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ChatPageViewHolder, position: Int, payload: List<Any>) {
        if (payload.isNotEmpty()) {
            val progress = payload[0] as Float
            holder.mProgressBar?.progress = (progress * 100).toInt()
            return
        }

        onBindViewHolder(holder, position)
    }


    // 计算展示条目的类型
    override fun getItemViewType(position: Int): Int {

        val msg = dataList[position]
        if (msg.inOut) {
            val result = VIEW_TYPE_AVATAR  or VIEW_TYPE_TIP or VIEW_TYPE_SIDE_LEFT
            return result
        }
        else{
            val result = VIEW_TYPE_AVATAR  or VIEW_TYPE_TIP or VIEW_TYPE_SIDE_RIGHT
            return result
        }
    }

    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: ChatPageViewHolder, position: Int) {
        val item = dataList[position]
        //holder.seqId = m.seq

        // 如果没有内容了
        if (holder.mIcon != null) {
            // Meta bubble in the center of the screen
            holder.mIcon.visibility = View.VISIBLE
            holder.mText.setText(R.string.content_deleted)
            return
        }

        if (holder.mAvatar != null){
            val index = ImagesHelper.getIconResId(item.iconUrl!!)
            holder.mAvatar.setImageResource(index)
        }
        if ( holder.mUserName != null){
            holder.mUserName.text = item.nick
        }

        if (holder.mDateDivider != null){
            //holder.mDateDivider?.visibility = View.VISIBLE
            holder.mDateDivider?.text = "2024 10 21"
        }


        holder.mMeta.text = "19:36"

        holder.mText.text = item.text

        val hasAttachment =  item!!.content != null &&  item.content?.getEntReferences() != null
        val uploadingAttachment = hasAttachment &&  item.isPending()
        val uploadFailed = hasAttachment && ( item.msgStatus === MessageStatus.FAIL)


        // 这里格式化消息
        // Normal message.
        if (item.content != null) {
            // Disable clicker while message is processed.
            val formatter: FullFormatter = FullFormatter(
                holder.mText,
                if (uploadingAttachment) null else SpanClicker()
            )
            formatter.setQuoteFormatter(QuoteFormatter(holder.mText, holder.mText.textSize))
            var text  = item.content.format(formatter) as Spanned
            if (TextUtils.isEmpty(text)) {
                text =
                    if (item.msgStatus === MessageStatus.DRAFT || item.msgStatus === MessageStatus.QUEUED || item.msgStatus === MessageStatus.SENDING) {
                        serviceContentSpanned(
                            this.fragment!!.requireContext(),
                            R.drawable.ic_schedule_gray,
                            R.string.processing
                        )
                    } else if (item.msgStatus === MessageStatus.FAIL) {
                        serviceContentSpanned(
                            this.fragment!!.requireContext(),
                            R.drawable.ic_error_gray,
                            R.string.failed
                        )
                    } else {
                        serviceContentSpanned(
                            this.fragment!!.requireContext(),
                            R.drawable.ic_warning_gray,
                            R.string.invalid_content
                        )
                    }
            }
            holder.mText.text = text
        }

    }

    // 返回数据项数量
    override fun getItemCount(): Int {
        return dataList.size
    }

    // 其他方法，例如添加删除项的方法，用于与 ItemTouchHelper 配合实现左滑删除
    // ...

    // Generates formatted content:
    //  - "( ! ) invalid content"
    //  - "( <) processing ..."
    //  - "( ! ) failed"
    private fun serviceContentSpanned(ctx: Context, iconId: Int, messageId: Int): SpannableString {
        val span = SpannableString(ctx.getString(messageId))
        span.setSpan(StyleSpan(Typeface.ITALIC), 0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        span.setSpan(
            ForegroundColorSpan(Color.rgb(0x75, 0x75, 0x75)),
            0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val icon = AppCompatResources.getDrawable(ctx, iconId)
        span.setSpan(
            IconMarginSpan(ImagesHelper.bitmapFromDrawable(icon!!), 24),
            0, span.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        return span
    }


    class SpanClicker internal constructor() : FullFormatter.ClickListener {
        override fun onClick(type: String?, data: Map<String, Any>?, params: Any?): Boolean {
//            if (mSelectedItems != null) {
//                return false
//            }

            when (type) {
                "AU" ->                     // Pause/resume audio.
                    return clickAudio(data, params!!)

                "LN" ->                     // Click on an URL
                    return clickLink(data)

                "IM" ->                     // Image
                    return clickImage(data)

                "EX" ->                     // Attachment
                    return clickAttachment(data)

                "BN" ->                     // Button
                    return clickButton(data)
            }
            return false
        }

        private fun clickAttachment(data: Map<String, Any>?): Boolean {
            if (data == null) {
                return false
            }

            var fname: String? = null
            var mimeType: String? = null
            try {
                fname = data["name"] as String?
                mimeType = data["mime"] as String?
            } catch (ignored: ClassCastException) {
            }

            // Try to extract file name from reference.
            if (TextUtils.isEmpty(fname)) {
                val ref = data["ref"]
                if (ref is String) {
                    try {
                        val url = URL(ref as String?)
                        fname = url.file
                    } catch (ignored: MalformedURLException) {
                    }
                }
            }

//            if (TextUtils.isEmpty(fname)) {
//                fname = mActivity.getString(R.string.default_attachment_name)
//            }
//
//            AttachmentHandler.enqueueDownloadAttachment(mActivity, data, fname, mimeType)
            return true
        }

        // Audio play/pause.
        private fun clickAudio(data: Map<String, Any>?, params: Any): Boolean {
            if (data == null) {
                return false
            }

//            try {
//                val aca: AudioClickAction = params as AudioClickAction
//                if (aca.action === FullFormatter.AudioClickAction.Action.PLAY) {
//                    mMediaControl.ensurePlayerReady(mSeqId, data, aca.control)
//                    mMediaControl.playWhenReady()
//                } else if (aca.action === FullFormatter.AudioClickAction.Action.PAUSE) {
//                    mMediaControl.pause()
//                } else if (aca.seekTo != null) {
//                    mMediaControl.ensurePlayerReady(mSeqId, data, aca.control)
//                    mMediaControl.seekToWhenReady(aca.seekTo)
//                }
//            } catch (ignored: IOException) {
//                Toast.makeText(mActivity, R.string.unable_to_play_audio, Toast.LENGTH_SHORT).show()
//                return false
//            } catch (ignored: ClassCastException) {
//                Toast.makeText(mActivity, R.string.unable_to_play_audio, Toast.LENGTH_SHORT).show()
//                return false
//            }

            return true
        }

        // Button click.
        private fun clickButton(data: Map<String, Any>?): Boolean {
            if (data == null) {
                return false
            }

//            try {
//                val actionType = data["act"] as String?
//                val actionValue = data["val"] as String?
//                val name = data["name"] as String?
//                // StoredMessage msg = getMessage(mPosition);
//                if ("pub" == actionType) {
//                    val newMsg: Drafty = Drafty(data["title"] as String?)
//                    val json: MutableMap<String, Any> = HashMap()
//                    // {"seq":6,"resp":{"yes":1}}
//                    if (!TextUtils.isEmpty(name)) {
//                        val resp: MutableMap<String?, Any> = HashMap()
//                        // noinspection
//                        resp[name] = if (TextUtils.isEmpty(actionValue)) 1 else actionValue
//                        json["resp"] = resp
//                    }
//
//                    json["seq"] = "" + mSeqId
//                    newMsg.attachJSON(json)
//                    mActivity.sendMessage(newMsg, -1)
//                } else if ("url" == actionType) {
//                    val url = URL(Cache.getTinode().getBaseUrl(), data["ref"] as String?)
//                    val scheme = url.protocol
//                    // As a security measure refuse to follow URLs with non-http(s) protocols.
//                    if ("http" == scheme || "https" == scheme) {
//                        val uri = Uri.parse(url.toString())
//                        var builder = uri.buildUpon()
//                        if (!TextUtils.isEmpty(name)) {
//                            builder = builder.appendQueryParameter(
//                                name,
//                                if (TextUtils.isEmpty(actionValue)) "1" else actionValue
//                            )
//                        }
//                        builder = builder
//                            .appendQueryParameter("seq", "" + mSeqId)
//                            .appendQueryParameter("uid", Cache.getTinode().getMyId())
//                        mActivity.startActivity(Intent(Intent.ACTION_VIEW, builder.build()))
//                    }
//                }
//            } catch (ignored: ClassCastException) {
//                return false
//            } catch (ignored: MalformedURLException) {
//                return false
//            } catch (ignored: NullPointerException) {
//                return false
//            }

            return true
        }

        private fun clickLink(data: Map<String, Any>?): Boolean {
            if (data == null) {
                return false
            }

//            try {
//                val url = URL(Cache.getTinode().getBaseUrl(), data["url"] as String?)
//                val scheme = url.protocol
//                if ("http" == scheme || "https" == scheme) {
//                    // As a security measure refuse to follow URLs with non-http(s) protocols.
//                    mActivity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url.toString())))
//                }
//            } catch (ignored: ClassCastException) {
//                return false
//            } catch (ignored: MalformedURLException) {
//                return false
//            } catch (ignored: NullPointerException) {
//                return false
//            }
            return true
        }

        private fun clickImage(data: Map<String, Any>?): Boolean {
            if (data == null) {
                return false
            }
            var args: Bundle? = null
            var `val`: Any?
//            if (data["ref"].also { `val` = it } is String) {
//                val url: URL = Cache.getTinode().toAbsoluteURL(`val` as String?)
//                // URL is null when the image is not sent yet.
//                if (url != null) {
//                    args = Bundle()
//                    args.putParcelable(AttachmentHandler.ARG_REMOTE_URI, Uri.parse(url.toString()))
//                }
//            }
//
//            if (args == null && (data["val"].also { `val` = it }) != null) {
//                val bytes = if (`val` is String) Base64.decode(
//                    `val` as String?,
//                    Base64.DEFAULT
//                ) else if (`val` is ByteArray) `val` as ByteArray? else null
//                if (bytes != null) {
//                    args = Bundle()
//                    args.putByteArray(AttachmentHandler.ARG_SRC_BYTES, bytes)
//                }
//            }
//
//            if (args != null) {
//                try {
//                    args.putString(AttachmentHandler.ARG_MIME_TYPE, data["mime"] as String?)
//                    args.putString(AttachmentHandler.ARG_FILE_NAME, data["name"] as String?)
//                    args.putInt(AttachmentHandler.ARG_IMAGE_WIDTH, data["width"] as Int)
//                    args.putInt(AttachmentHandler.ARG_IMAGE_HEIGHT, data["height"] as Int)
//                } catch (ex: NullPointerException) {
//                    Log.w(
//                        co.tinode.tindroid.MessagesAdapter.TAG,
//                        "Invalid type of image parameters",
//                        ex
//                    )
//                } catch (ex: ClassCastException) {
//                    Log.w(
//                        co.tinode.tindroid.MessagesAdapter.TAG,
//                        "Invalid type of image parameters",
//                        ex
//                    )
//                }
//            }
//
//            if (args != null) {
//                mActivity.showFragment(MessageActivity.FRAGMENT_VIEW_IMAGE, args, true)
//            } else {
//                Toast.makeText(mActivity, R.string.broken_image, Toast.LENGTH_SHORT).show()
//            }

            return true
        }
    }

}