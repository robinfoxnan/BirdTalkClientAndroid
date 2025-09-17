package com.bird2fish.birdtalksdk.ui
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
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.model.Topic
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper


class ChatSessionFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatSessionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chat_session, container, false)

        recyclerView = view.findViewById(R.id.recycler_view_sessions)

        // 创建示例数据
        var topic = Topic()
        topic.title = "群聊2"
        topic.icon = "sys:11"
        SdkGlobalData.chatSessionList += topic

        topic = Topic()
        topic.title = "会话1"
        topic.icon = "sys:10"
        SdkGlobalData.chatSessionList += topic

        // 初始化适配器
        val adapter = ChatSessionAdapter(SdkGlobalData.chatSessionList)
        adapter.setView(this)

        // 配置 RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter


        // 搜索按钮点击事件
        val searchButton: ImageButton = view.findViewById<ImageButton>(com.bird2fish.birdtalksdk.R.id.btn_search)
        searchButton.setOnClickListener { v: View? -> }

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
        testInitData()
    }

    fun testInitData(){
        //friendList?.n
    }


    fun onClickItem(index: Int){

    }
}



// 双向关注的列表，适配器
class ChatSessionAdapter(private val dataList: List<Topic>) : RecyclerView.Adapter<ChatSessionAdapter.ChatSessionViewHolder>() {

    private var fragment : ChatSessionFragment? = null


    fun setView(view : ChatSessionFragment?){
        this.fragment = view
    }

    // 创建 ViewHolder
    inner class ChatSessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ViewHolder 中的视图元素，例如 TextView、ImageView 等
        val imgIcon : ImageView = itemView.findViewById(R.id.iconTv)
        val tvNick: TextView = itemView.findViewById(R.id.nameTv)
        val tvDes : TextView = itemView.findViewById(R.id.desTv)
        val tvTime :TextView = itemView.findViewById(R.id.timeTv)
        val tvState:ImageView = itemView.findViewById(R.id.StateTv)

        var index: Int = 0
        var selectedPosition = RecyclerView.NO_POSITION

        init {
            // 在构造函数中为整个 ViewHolder 的根视图设置点击事件
            itemView.setOnClickListener {
                // 处理点击事件
                if (fragment != null){
                    fragment!!.onClickItem(index)
                }
            }
        }

    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatSessionViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_session_item, parent, false)
        return ChatSessionViewHolder(itemView)
    }


    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: ChatSessionViewHolder, position: Int) {
        val item = dataList[position]
        holder.index = position

        val id = ImagesHelper.getIconResId(item!!.icon)
        holder.imgIcon.setImageResource(id)
        holder.tvNick.setText(item!!.title)
        holder.tvDes.setText("[图片]")
        holder.tvState.setImageResource(android.R.drawable.ic_lock_silent_mode)

        // 可以添加其他逻辑...
//        holder.tvDelete.setOnClickListener{
//            if (fragment != null){
//                fragment!!.onClickItemShare(holder.tvDelete.tag as Int)
//            }
//        }

        // 根据选中状态更新背景
        holder.itemView.isSelected = (position == holder.selectedPosition)

        holder.itemView.setOnClickListener {
            // 更新选中状态
            notifyItemChanged(holder.selectedPosition)
            holder.selectedPosition = holder.adapterPosition
            notifyItemChanged(holder.selectedPosition)
        }
    }

    // 返回数据项数量
    override fun getItemCount(): Int {
        return dataList.size
    }

    // 其他方法，例如添加删除项的方法，用于与 ItemTouchHelper 配合实现左滑删除
    // ...

}
