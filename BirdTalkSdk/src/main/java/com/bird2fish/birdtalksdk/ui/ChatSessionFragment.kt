package com.bird2fish.birdtalksdk.ui
import android.app.Activity
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.R.*
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.db.TopicDbHelper
import com.bird2fish.birdtalksdk.model.ChatSession
import com.bird2fish.birdtalksdk.model.ChatSessionManager
import com.bird2fish.birdtalksdk.model.Topic
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatMsgType
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatMsgType.*
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper


class ChatSessionFragment : Fragment()  , StatusCallback {

    private lateinit var recyclerView: SlideRecyclerView
    private lateinit var adapter: ChatSessionAdapter
    private lateinit var addBtn : ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    // 从选择的界面返回数据
    private fun handleFriendSelected(list: List<ListItem>) {
        // 处理选中的用户
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(layout.fragment_chat_session, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_sessions)

        addBtn = view.findViewById(R.id.btn_add_chat_session)


        // 点击加号按钮
        addBtn.setOnClickListener {

            CreateGroupFragment().show(parentFragmentManager, "CreateGroupDialog")

        }

        // 打开创建群的界面
        fun onCreateGroup(){

        }

        fun testSelectUser(){
            FriendSelectDialog().apply {
                setOnFriendSelectedListener { list ->
                    handleFriendSelected(list)
                }
            }.show(parentFragmentManager, "FriendSelectDialog")
        }


        // 初始化适配器
        this.adapter = ChatSessionAdapter(ChatSessionManager.rebuildDisplayList())
        adapter.setView(this)

        // 配置 RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter


        // 搜索按钮点击事件
        val searchButton: ImageButton = view.findViewById<ImageButton>(R.id.btn_search)
        searchButton.setOnClickListener { v: View? -> }

        SdkGlobalData.userCallBackManager.addCallback(this)
        return view
    }

    fun openNewChatSession(){
        //var topic = Topic()

        this.adapter?.notifyDataSetChanged()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 取消关注消息
        SdkGlobalData.userCallBackManager.removeCallback(this)
    }

    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }
    // 上传或下载事件
    // 这里是回调函数，无法操作界面
    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.FRIEND_CHAT_SESSION){
            (context as? Activity)?.runOnUiThread {
                this.adapter?.notifyDataSetChanged()
            }
        }
        // 设置了最新一条消息
        else if (eventType == MsgEventType.MSG_COMING){
            (context as? Activity)?.runOnUiThread {
                this.adapter?.notifyDataSetChanged()
            }
        }
    }

    fun testData(){
        // 创建示例数据
        var topic = Topic()
        topic.title = "群聊2"
        topic.icon = "sys:11"
        //SdkGlobalData.chatSessionList += topic

        topic = Topic()
        topic.title = "会话1"
        topic.icon = "sys:10"
        //SdkGlobalData.chatSessionList += topic
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
        testInitData()
    }

    fun testInitData(){
        //friendList?.n
    }

    // 点击了条目，这里需要跳转进入 发送信息界面
    fun switchSendMsgPage(t:ChatSession){
        // 通过消息方式通知上层界面切换到消息发送
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.APP_NOTIFY_SEND_MSG,
                0, 0, t.getSessionId(), mapOf("page" to "followedFragment" ) )

    }

    fun onClickRemove(){
        recyclerView.closeMenu()
    }

}



// 双向关注的列表，适配器
class ChatSessionAdapter(private val dataMap: MutableList<ChatSession>) : RecyclerView.Adapter<ChatSessionAdapter.ChatSessionViewHolder>() {

    private var fragment : ChatSessionFragment? = null


    fun setView(view : ChatSessionFragment?){
        this.fragment = view
    }

    // 创建 ViewHolder
    inner class ChatSessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ViewHolder 中的视图元素，例如 TextView、ImageView 等
        val imgIcon : ImageView = itemView.findViewById(id.iconTv)
        val tvNick: TextView = itemView.findViewById(id.nameTv)
        val tvDes : TextView = itemView.findViewById(id.desTv)
        val tvTime :TextView = itemView.findViewById(id.timeTv)
        val tvState:ImageView = itemView.findViewById(id.StateTv)
        val tvHide:TextView = itemView.findViewById(id.tv_remove_chat)

        var index: Int = 0
        var selectedPosition = RecyclerView.NO_POSITION

        init {
            // 在构造函数中为整个 ViewHolder 的根视图设置点击事件
            itemView.setOnClickListener {
                // 处理点击事件
                if (fragment != null){
                    val list = dataMap
                    val t = list[index]
                    fragment!!.switchSendMsgPage(t)
                }
            }
        }

    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSessionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(layout.layout_session_item, parent, false)
        return ChatSessionViewHolder(itemView)
    }


    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: ChatSessionViewHolder, position: Int) {
        val dataList = dataMap
        val item = dataList[position]
        holder.index = position

        //val id = ImagesHelper.getIconResId(item!!.icon)
        //holder.imgIcon.setImageResource(id)
        AvatarHelper.tryLoadAvatar(fragment!!.requireContext(), item.icon, holder.imgIcon, item.getGender(), item.getNick())
        holder.tvNick.setText(item.title)


        if (item.lastMsg == null){
            holder.tvDes.setText("")
            holder.tvTime.setText("")
        }else{
            // 时间
            val tm = TextHelper.millisToTime1(item.lastMsg!!.tm)
            holder.tvTime.setText(tm)

            // 最后的消息
            if (item.lastMsg!!.content == null){
                holder.tvDes.setText(item.lastMsg!!.text)
            }else{
                when (item.lastMsg!!.msgType){
                    TEXT ->  holder.tvDes.setText(item.lastMsg!!.text)
                    IMAGE ->  holder.tvDes.setText(R.string.msg_image)
                    VOICE ->  holder.tvDes.setText(R.string.msg_voice)
                    VIDEO ->  holder.tvDes.setText(R.string.msg_video)
                    FILE ->  holder.tvDes.setText(R.string.msg_file)
                    DELETE ->  holder.tvDes.setText("")
                    KEY ->  holder.tvDes.setText("")
                    PLUGIN ->  holder.tvDes.setText("")
                    UNRECOGNIZED ->  holder.tvDes.setText("未知")
                }
            }
        }

        // 根据静音状态设置图标
        if (item.mute){
            holder.tvState.setImageResource(android.R.drawable.ic_lock_silent_mode)
        }else{
            holder.tvState.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
            holder.tvState.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)

        }


        // 是否静音的代码，点击了切换是否静音
        holder.tvState.setOnClickListener{
            if (item.mute){
                item.setMute(false)
                holder.tvState.setImageResource(android.R.drawable.ic_lock_silent_mode_off)
                holder.tvState.setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN)
                TopicDbHelper.insertOrReplacePTopic(item);
            }else{
                item.setMute(true)
                holder.tvState.setImageResource(android.R.drawable.ic_lock_silent_mode)
                TopicDbHelper.insertOrReplacePTopic(item);
            }
        }

        // 点击隐藏，不显示这个对话
        holder.tvHide.setOnClickListener{
            item.setShowHide(false)
            fragment?.onClickRemove()
            TopicDbHelper.insertOrReplacePTopic(item);
            ChatSessionManager.rebuildDisplayList()
            notifyDataSetChanged()

        }

    }

    // 返回数据项数量
    override fun getItemCount(): Int {
        return dataMap.size
    }

    // 其他方法，例如添加删除项的方法，用于与 ItemTouchHelper 配合实现左滑删除
    // ...

}
