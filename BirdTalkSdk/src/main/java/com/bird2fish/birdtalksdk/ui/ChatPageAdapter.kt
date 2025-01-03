package com.bird2fish.birdtalksdk.ui

import FullscreenImageDialog
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.IconMarginSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.SparseBooleanArray
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.format.FullFormatter
import com.bird2fish.birdtalksdk.format.QuoteFormatter
import com.bird2fish.birdtalksdk.model.MessageContent
import com.bird2fish.birdtalksdk.model.MessageStatus
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import com.bird2fish.birdtalksdk.widgets.MediaControl
import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import com.bird2fish.birdtalksdk.R


// 每个消息条目的内容布局部分
class ChatPageViewHolder  constructor(itemView: View, val mViewType: Int) : RecyclerView.ViewHolder(itemView) {
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
    var seqId: Int = 0


    // 关于每个条目的手势识别
    val mGestureDetector: GestureDetector = GestureDetector(itemView.context, object : SimpleOnGestureListener() {
            override fun onLongPress(ev: MotionEvent) {
                itemView.performLongClick()
            }

            override fun onSingleTapConfirmed(ev: MotionEvent): Boolean {
                //TextHelper.showToast(itemView.context, "click", Toast.LENGTH_SHORT)
                itemView.performClick()
               super.onSingleTapConfirmed(ev)
                return false
            }


            override  fun onSingleTapUp(ev: MotionEvent): Boolean{
                //TextHelper.showToast(itemView.context, "touch up", Toast.LENGTH_SHORT)
                return false
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
                //TextHelper.showToast(itemView.context, "touch down", Toast.LENGTH_SHORT)
                val item = IntArray(2)
                val text = IntArray(2)
                itemView.getLocationOnScreen(item)
                mText.getLocationOnScreen(text)


                // Convert ev.getX() and ev.getY() to screen coordinates.
                val x = (ev.x + item[0]).toInt()
                val y = (ev.y + item[1]).toInt()


                // Make click position available to spannable.
                mText.setTag(com.bird2fish.birdtalksdk.R.id.click_coordinates, Point(x, y))
                //itemView.performClick()
                Log.d("TouchEvent", "gesture  ACTION_DOWN")
                return true

            }
        })

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
    private val mMediaControl: MediaControl = MediaControl()

    private var mSelectedItems: SparseBooleanArray? = null
    private var mSelectionMode: ActionMode? = null

    private val MESSAGE_BUBBLE_ANIMATION_SHORT: Int = 150
    private val MESSAGE_BUBBLE_ANIMATION_LONG: Int = 600

    private val mSelectionModeCallback: ActionMode.Callback = object : ActionMode.Callback {
            override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                if (mSelectedItems == null) {
                    mSelectedItems = SparseBooleanArray()
                }
                val selected = if (mSelectedItems != null) mSelectedItems!!.size() else 0
                //menu.findItem(R.id.action_reply).setVisible(selected <= 1)
                //menu.findItem(R.id.action_forward).setVisible(selected <= 1)
                return true
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onDestroyActionMode(actionMode: ActionMode) {
                val arr = mSelectedItems!!
                mSelectedItems = null
                if (arr.size() < 6) {
                    for (i in 0 until arr.size()) {
                        notifyItemChanged(arr.keyAt(i))
                    }
                } else {
                    notifyDataSetChanged()
                }
            }

            override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                //fragment!!.requireActivity().getMenuInflater().inflate(R.menu.menu_message_selected, menu)
                //menu.findItem(R.id.action_delete).setVisible(!ComTopic.isChannel(mTopicName))
                return true
            }

            override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
                // Don't convert to switch: Android does not like it.
                val id = menuItem.itemId
//                if (id == R.id.action_delete) {
//                    sendDeleteMessages(getSelectedArray())
//                    return true
//                } else if (id == R.id.action_copy) {
//                    copyMessageText(getSelectedArray())
//                    return true
//                } else if (id == R.id.action_send_now) {
//                    // FIXME: implement resending now.
//                    Log.d(co.tinode.tindroid.MessagesAdapter.TAG, "Try re-sending selected item")
//                    return true
//                } else if (id == R.id.action_reply) {
//                    val selected: IntArray = getSelectedArray()
//                    if (selected != null) {
//                        showReplyPreview(selected[0])
//                    }
//                    return true
//                } else if (id == R.id.action_forward) {
//                    val selected: IntArray = getSelectedArray()
//                    if (selected != null) {
//                        showMessageForwardSelector(selected[0])
//                    }
//                    return true
//                }

                return false
            }
        }



    fun setView(view : ChatPageFragment?){
        this.fragment = view
        this.mMediaControl.setActivity(this.fragment!!.requireActivity())
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ChatPageViewHolder, position: Int, payload: List<Any>) {
        if (payload.isNotEmpty()) {
            val progress = payload[0] as Float
            holder.mProgressBar?.progress = (progress * 100).toInt()
            return
        }

        onBindViewHolder(holder, position)
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

        //holder.mRippleOverlay?.visibility = View.GONE

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
                SpanClicker()
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


        // 声音，按钮，链接，提及，TAG，图片，设置点击的事件转发
        if (item.content != null && item.content.hasEntities(
                mutableListOf(
                    "AU",
                    "BN",
                    "LN",
                    "MN",
                    "HT",
                    "IM",
                    "EX"
                )
            )
        ) {

            //TextHelper.showToast(this@ChatPageAdapter.fragment!!.requireContext(), "find clickable", Toast.LENGTH_SHORT)
            // Some spans are clickable.
            holder.mText.setOnTouchListener { v: View?, ev: MotionEvent? ->

                when (ev?.action) {
                    MotionEvent.ACTION_DOWN -> {
                        Log.d("TouchEvent", "ACTION_DOWN")
                        //holder.mText.callOnClick()
                        // 这里是没有办法的办法，调试时候可以，在运行时候总是得到一个cancel，估计是小米的BUG
                        Thread.sleep(100)
                    }
                    MotionEvent.ACTION_UP -> Log.d("TouchEvent", "ACTION_UP")
                    MotionEvent.ACTION_MOVE -> Log.d("TouchEvent", "ACTION_MOVE")
                    MotionEvent.ACTION_CANCEL -> {
                        Log.d("TouchEvent", "ACTION_CANCEL")
                        //Thread.dumpStack()

                    }
                }
                holder.mGestureDetector.onTouchEvent(ev)
                false
            }
            holder.mText.movementMethod = LinkMovementMethod.getInstance()
            holder.mText.linksClickable = true
            holder.mText.isFocusable = true
            holder.mText.isClickable = true
        } else {
            holder.mText.setOnTouchListener(null)
            holder.mText.movementMethod = null
            holder.mText.linksClickable = false
            holder.mText.isFocusable = false
            holder.mText.isClickable = false
            holder.mText.autoLinkMask = 0
        }


        // 发送成功图标
        if (holder.mDeliveredIcon != null) {
            if ((holder.mViewType and VIEW_TYPE_SIDE_RIGHT) != 0) {
                ImagesHelper.setMessageStatusIcon(
                    holder.mDeliveredIcon, item.msgStatus,
                    item.bRead, item.bRecv
                )
            }
        }





        // 设置条目的事件处理
        holder.itemView.setOnLongClickListener { v: View? ->
            val pos: Int = holder.getBindingAdapterPosition()
            if (mSelectedItems == null) {
                //mSelectionMode = fragment!!.startSupportActionMode(mSelectionModeCallback)
            }

            toggleSelectionAt(pos)
            notifyItemChanged(pos)
            updateSelectionMode()
            true
        }



        holder.itemView.setOnClickListener { v: View? ->
            if (mSelectedItems != null) {
                val pos: Int = holder.getBindingAdapterPosition()
                toggleSelectionAt(pos)
                notifyItemChanged(pos)
                updateSelectionMode()
            } else {
                animateMessageBubble(holder, item.inOut, true)
//                val replySeq: Int = UiUtils.parseSeqReference(m.getStringHeader("reply"))
//                if (replySeq > 0) {
//                    // A reply message was clicked. Scroll original into view and animate.
//                    val pos: Int =
//                        co.tinode.tindroid.MessagesAdapter.findInCursor(mCursor, replySeq)
//                    if (pos >= 0) {
//                        val mm: StoredMessage = getMessage(pos)
//                        if (mm != null) {
//                            val lm =
//                                mRecyclerView.getLayoutManager() as LinearLayoutManager
//                            if (lm != null && pos >= lm.findFirstCompletelyVisibleItemPosition() && pos <= lm.findLastCompletelyVisibleItemPosition()
//                            ) {
//                                // Completely visible, animate now.
//                                animateMessageBubble(
//                                    mRecyclerView.findViewHolderForAdapterPosition(pos) as co.tinode.tindroid.MessagesAdapter.ViewHolder,
//                                    mm.isMine(), false
//                                )
//                            } else {
//                                // Scroll then animate.
//                                mRecyclerView.clearOnScrollListeners()
//                                mRecyclerView.addOnScrollListener(object :
//                                    RecyclerView.OnScrollListener() {
//                                    override fun onScrollStateChanged(
//                                        recyclerView: RecyclerView,
//                                        newState: Int
//                                    ) {
//                                        super.onScrollStateChanged(recyclerView, newState)
//                                        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                                            recyclerView.removeOnScrollListener(this)
//                                            animateMessageBubble(
//                                                mRecyclerView.findViewHolderForAdapterPosition(pos) as co.tinode.tindroid.MessagesAdapter.ViewHolder,
//                                                mm.isMine(), false
//                                            )
//                                        }
//                                    }
//                                })
//                                mRecyclerView.smoothScrollToPosition(pos)
//                            }
//                        }
//                    }
//                }
            }
        } //end
//

    }


    private fun toggleSelectionAt(pos: Int) {
        if (mSelectedItems!![pos]) {
            mSelectedItems!!.delete(pos)
        } else {
            mSelectedItems!!.put(pos, true)
        }
    }

    private fun updateSelectionMode() {
        if (mSelectionMode != null) {
            val selected = mSelectedItems!!.size()
            if (selected == 0) {
                mSelectionMode?.finish()
                mSelectionMode = null
            } else {
                mSelectionMode?.setTitle(selected.toString())
                val menu: Menu = mSelectionMode!!.getMenu()
               // menu.findItem(R.id.action_reply).setVisible(selected == 1)
                //menu.findItem(R.id.action_forward).setVisible(selected == 1)
            }
        }
    }

    // 动画显示消息气泡
    private fun animateMessageBubble(vh: ChatPageViewHolder?, isMine: Boolean, light: Boolean) {
        if (vh == null) {
            return
        }
        val from: Int = vh.mMessageBubble.getResources().getColor(
            if (isMine) R.color.colorMessageBubbleMine else R.color.colorMessageBubbleOther,
            null
        )
        val to: Int = vh.mMessageBubble.getResources().getColor(
            if (isMine) (if (light) R.color.colorMessageBubbleMineFlashingLight else R.color.colorMessageBubbleMineFlashing) else (if (light) R.color.colorMessageBubbleOtherFlashingLight else R.color.colorMessageBubbleOtherFlashing),
            null
        )
        val colorAnimation = ValueAnimator.ofArgb(from, to, from)
        colorAnimation.setDuration((if (light) MESSAGE_BUBBLE_ANIMATION_SHORT else MESSAGE_BUBBLE_ANIMATION_LONG).toLong())
        colorAnimation.addUpdateListener { animator: ValueAnimator ->
            vh.mMessageBubble.setBackgroundTintList(
                ColorStateList.valueOf(animator.animatedValue as Int)
            )
        }
        colorAnimation.start()
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


    inner class SpanClicker internal constructor() : FullFormatter.ClickListener {
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
        // 这里是条目中的那个音频播放的点击事件
        private fun clickAudio(data: Map<String, Any>?, params: Any): Boolean {

            if (data == null) {
                return false
            }

            val mSeqId = 1
            try {
                val aca: FullFormatter.AudioClickAction = params as FullFormatter.AudioClickAction
                if (aca.action === FullFormatter.AudioClickAction.Action.PLAY) {
                    this@ChatPageAdapter.mMediaControl.ensurePlayerReady(mSeqId, data, aca.control!!)
                    this@ChatPageAdapter.mMediaControl.playWhenReady()
                } else if (aca.action === FullFormatter.AudioClickAction.Action.PAUSE) {
                    mMediaControl.pause()
                } else if (aca.seekTo != null) {
                    mMediaControl.ensurePlayerReady(mSeqId, data, aca.control!!)
                    mMediaControl.seekToWhenReady(aca.seekTo!!)
                }
            } catch (ignored: IOException) {
                Toast.makeText(this@ChatPageAdapter.fragment!!.requireContext(), R.string.unable_to_play_audio, Toast.LENGTH_SHORT).show()
                return false
            } catch (ignored: ClassCastException) {
                Toast.makeText(this@ChatPageAdapter.fragment!!.requireContext(), R.string.unable_to_play_audio, Toast.LENGTH_SHORT).show()
                return false
            }

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

        // 点击图片时候，显示一个对话框
        private fun clickImage(data: Map<String, Any>?): Boolean {
            if (data == null) {
                return false
            }

            //Toast.makeText(this@ChatPageAdapter.fragment!!.requireContext(), "image click", Toast.LENGTH_SHORT).show()

            val ref = data["ref"]
            val url = TextHelper.toAbsoluteURL(ref as String)

            val dialog = FullscreenImageDialog(this@ChatPageAdapter.fragment!!.requireContext(), null, url)
            dialog.show()


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


            return true
        }
    }

}