package com.bird2fish.birdtalksdk.ui

import android.annotation.SuppressLint
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.model.MessageContent
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper


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

        holder.mText.text = item.text
        holder.mMeta.text = "19:36"

    }

    // 返回数据项数量
    override fun getItemCount(): Int {
        return dataList.size
    }

    // 其他方法，例如添加删除项的方法，用于与 ItemTouchHelper 配合实现左滑删除
    // ...

}