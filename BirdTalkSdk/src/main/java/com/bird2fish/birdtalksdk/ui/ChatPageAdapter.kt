package com.bird2fish.birdtalksdk.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Point
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.IconMarginSpan
import android.text.style.StyleSpan
import android.util.Base64
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
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.format.FullFormatter
import com.bird2fish.birdtalksdk.format.QuoteFormatter
import com.bird2fish.birdtalksdk.model.ChatSession
import com.bird2fish.birdtalksdk.model.ChatSessionManager
import com.bird2fish.birdtalksdk.model.Drafty
import com.bird2fish.birdtalksdk.model.Drafty.Entity
import com.bird2fish.birdtalksdk.model.MessageContent
import com.bird2fish.birdtalksdk.model.MessageStatus
import com.bird2fish.birdtalksdk.net.FileDownloader
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import com.bird2fish.birdtalksdk.widgets.MediaControl
import java.io.IOException


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

    var chatSession:ChatSession? = null   // 聊天的ID，是对方的ID或者群组的ID取负数

    fun setSession(s: ChatSession){
        this.chatSession  = s
    }


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
                itemView.performClick()
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
    private var chatSession: ChatSession?= null
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

    fun setSession(s:ChatSession){
        this.chatSession = s
    }

    /**
     * 打印 Spanned 的完整信息（文本 + 所有 Span 详情）
     */
    fun logSpannedDetails(tag: String, spanned: Spanned) {
        // 1. 打印纯文本内容
        val text = spanned.toString()
        Log.d(tag, "文本内容: $text (长度: ${text.length})")

        // 2. 打印所有 Span 信息
        val allSpans = spanned.getSpans(0, spanned.length, Any::class.java)
        Log.d(tag, "包含 Span 数量: ${allSpans.size}")

        for (span in allSpans) {
            val start = spanned.getSpanStart(span)
            val end = spanned.getSpanEnd(span)
            val flags = spanned.getSpanFlags(span)
            val spanType = span.javaClass.simpleName

            // 截取 Span 作用的文本片段
            val spanText = try {
                spanned.substring(start, end)
            } catch (e: IndexOutOfBoundsException) {
                "【Span 范围错误: start=$start, end=$end, 文本长度=${text.length}】"
            }

            // 打印 Span 详情
            Log.d(
                tag,
                "Span[$spanType]: 范围=$start~$end, 标志=$flags, 作用文本: '$spanText'"
            )
        }
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
        }else{
            layoutId = R.layout.layout_message_right_single   // 后加的，防止-1
        }

        val v = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)

        // 这里计算是否需要显示日期
        if (v != null) {
            val dateBubble = v.findViewById<View>(R.id.dateDivider)
            if (dateBubble != null) {
                dateBubble.visibility =
                    if ((viewType and VIEW_TYPE_DATE) != 0) View.VISIBLE else View.GONE
            }
        }
        val holder = ChatPageViewHolder(v, viewType)
        holder.setSession(chatSession!!)
        return holder
    }





    // 计算展示条目的类型
    override fun getItemViewType(position: Int): Int {

        val msg = dataList[position]
        if (msg.inOut) {
            val result = VIEW_TYPE_AVATAR  or VIEW_TYPE_TIP or VIEW_TYPE_SIDE_LEFT
            // VIEW_TYPE_DATE
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

    // 给每个音频都设置一下消息的ID，用于播放缓存使用
    fun setAudioSeq(drafty: Drafty?, msgId:Long) : Boolean{
        if (drafty?.ent == null) {
            return false
        }

        for (entity in drafty.ent) {
            if (entity?.tp == null) continue
            if (entity.tp.equals("AU") ) {
                entity.putData("id", msgId)
                return true
            }
        }
        return true
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
//            val index = ImagesHelper.getIconResId(item.iconUrl!!)
//            holder.mAvatar.setImageResource(index)
            if (chatSession != null){
                AvatarHelper.tryLoadAvatar(this.fragment!!.requireContext(), chatSession!!.icon, holder.mAvatar, "", chatSession!!.title)
            }
        }
        // 私聊不显示用户名，群聊才显示
        if ( holder.mUserName != null){
            if (item.isP2p){
                holder.mUserName.visibility = View.GONE
            }else
            {
                holder.mUserName.text = item.nick
            }

        }

        // 这个没有用到
        if (holder.mDateDivider != null){
            //holder.mDateDivider?.visibility = View.VISIBLE
            holder.mDateDivider?.text = TextHelper.getCurrentDateString()
        }

        //holder.mRippleOverlay?.visibility = View.GONE

        // 消息的时间
        holder.mMeta.text = TextHelper.millisToTime1(item.tm)

        holder.mText.text = item.text

        val hasAttachment =  item!!.content != null &&  item.content?.getEntReferences() != null
        val uploadingAttachment = item.msgStatus == MessageStatus.UPLOADING
        val uploadFailed = hasAttachment && ( item.msgStatus === MessageStatus.FAIL)


        // 这里格式化消息
        // Normal message.
        if (item.content != null) {
            // 设置一下，然后播放
            setAudioSeq(item.content, item.msgId)

            // Disable clicker while message is processed.
            val formatter: FullFormatter = FullFormatter(
                holder.mText,
                SpanClicker()
            )
            formatter.setQuoteFormatter(QuoteFormatter(holder.mText, holder.mText.textSize))
            var text  = item.content!!.format(formatter) as Spanned
            // 打印
            //logSpannedDetails("birdtalksdk-debug text:", text)

            if (TextUtils.isEmpty(text)) {
                text =
                    if (item.msgStatus === MessageStatus.SENDING || item.msgStatus == MessageStatus.UPLOADING) {
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
        if (item.content != null && item.content!!.hasEntities(
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
                        holder.mText.callOnClick()
                        // 这里是没有办法的办法，调试时候可以，在运行时候总是得到一个cancel，估计是小米的BUG
                        //Thread.sleep(100)
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
                ImagesHelper.setMessageStatusIcon(holder.mDeliveredIcon, item.msgStatus, item.isRead, item.isRecv)
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
                //animateMessageBubble(holder, item.inOut, true)
            }
        } //end
//

    }


    private fun toggleSelectionAt(pos: Int) {
        if (mSelectedItems == null){
            Log.d("toggleSelectionAt", "toggleSelectionAt mSelectedItems = null")
            return
        }
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
    // 在小米上测试，发现毛用没有，只刷新首尾2次，不如直接设置背景色
//    private fun animateMessageBubble(vh: ChatPageViewHolder?, isMine: Boolean, light: Boolean) {
//        if (vh == null) {
//            return
//        }
//        val from: Int = vh.mMessageBubble.getResources().getColor(
//            if (isMine) R.color.colorMessageBubbleMine else R.color.colorMessageBubbleOther,
//            null
//        )
//        val to: Int = vh.mMessageBubble.getResources().getColor(
//            if (isMine) (if (light) R.color.colorMessageBubbleMineFlashingLight else R.color.colorMessageBubbleMineFlashing) else (if (light) R.color.colorMessageBubbleOtherFlashingLight else R.color.colorMessageBubbleOtherFlashing),
//            null
//        )
//        val colorAnimation = ValueAnimator.ofArgb(to, to, from)
//
//        colorAnimation.setDuration(2000)
//        //colorAnimation.setDuration((if (light) MESSAGE_BUBBLE_ANIMATION_SHORT else MESSAGE_BUBBLE_ANIMATION_LONG).toLong())
//        colorAnimation.addUpdateListener { animator: ValueAnimator ->
//            Log.d("BubbleAnim", "color=${animator.animatedValue}")
//            vh.mMessageBubble.setBackgroundTintList(ColorStateList.valueOf(animator.animatedValue as Int))
//            //vh.mMessageBubble.setBackgroundColor(animator.animatedValue as Int)
//        }
//        colorAnimation.start()
//    }

    // 返回数据项数量
    override fun getItemCount(): Int {
        return dataList.size
    }

    fun getLast():MessageContent?{
        if (dataList == null || dataList.isEmpty())
            return null
        return dataList.last()
    }

    fun getFirst():MessageContent?{
        if (dataList == null || dataList.isEmpty())
            return null
        return dataList[0]
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
            var url :String? = null
            try {
                fname = data["name"] as String?
                mimeType = data["mime"] as String?
            } catch (ignored: ClassCastException) {
            }

            url = data["ref"] as String?

            if (TextUtils.isEmpty(url)){
                TextHelper.showToast(SdkGlobalData.context!!, "路径为空，无法操作")
                return false
            }

            // 本地文件直接打开
//            *  - 本地路径 /storage/emulated/0/Download/test.pdf
//            *  - file:// URI
//            *  - content:// URI
            if (!url!!.startsWith("http", true))
            {
                val index = url!!.indexOf('/')
                if (index > -1){
                    FileDownloader.openLocalFile(SdkGlobalData.context!!, url)
                    return true
                }
            }

            var msgId :Long = 0L
            try {
                val szObj = data["msgid"]
                msgId = when (szObj) {
                    is Number -> szObj.toLong()
                    is String -> szObj.toLong()
                    else -> 0L
                }
            } catch (ignored: NullPointerException) {
            } catch (ignored: ClassCastException) {
            }
            // 尝试下载文件，下载后打开
            FileDownloader.downloadAndOpen(
                SdkGlobalData.context!!,
                url!!,
                fname!!,
                onProgress = { percent ->
                    Log.d("Download", "进度: $percent%")
                    (data as MutableMap<String, Any>)["cur"] = percent.toInt()
                    //ChatSessionManager.notifyDownloadProcess(sessionId, msgId, percent.toInt(), fname, url)
                    SdkGlobalData.invokeOnEventCallbacks(MsgEventType. MSG_DOWNLOAD_PROCESS, percent.toInt(), msgId, chatSession!!.getSessionId(), mapOf("url" to url, "name"  to fname))
                },
                onFinished = { file ->
                    Log.d("Download", "下载完成: ${file.absolutePath}")
                    (data as MutableMap<String, Any>)["cur"] = 100
                    //ChatSessionManager.notifyDownloadProcess(sessionId, msgId, 100, fname, url)
                    SdkGlobalData.invokeOnEventCallbacks(MsgEventType. MSG_DOWNLOAD_PROCESS, 100, msgId, chatSession!!.getSessionId(), mapOf("url" to url, "name"  to fname))
                },
                onError = { e ->
                    Log.e("Download", "出错: ${e.message}")
                    //ChatSessionManager.notifyDownloadProcess(sessionId, msgId, -1, fname, url)
                    (data as MutableMap<String, Any>)["cur"] = -1
                    SdkGlobalData.invokeOnEventCallbacks(MsgEventType. MSG_DOWNLOAD_PROCESS, -1, msgId, chatSession!!.getSessionId(), mapOf("url" to url, "name"  to fname))
                }
            )

            return true
        }

        // Audio play/pause.
        // 这里是条目中的那个音频播放的点击事件
        private fun clickAudio(data: Map<String, Any>?, params: Any): Boolean {

            if (data == null) {
                return false
            }

            // todo: 这里的序号应该为消息号
            val mSeqId = data?.get("id") as? Long ?: 0L
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

            var fileName = data["name"] as String?
            if (fileName == null){
                fileName = ""
            }

            val ref = data["ref"]
            if (ref != null){
                val dialog = FullscreenImageDialog(this@ChatPageAdapter.fragment!!.requireContext(), fileName, null, ref as String)
                dialog.show()
                return true
            }

            var bmpPreview: Bitmap? = null
            val imgValue = data["val"]
            if (imgValue != null) {
                val bits = if ((imgValue is String)) Base64.decode(
                    imgValue as String?,
                    Base64.DEFAULT
                ) else (imgValue as ByteArray?)!!
                bmpPreview = BitmapFactory.decodeByteArray(bits, 0, bits.size)
                if (bmpPreview != null){
                    val dialog = FullscreenImageDialog(this@ChatPageAdapter.fragment!!.requireContext(), fileName, bmpPreview, null)
                    dialog.show()
                }
            }


            return true
        }
    }

}