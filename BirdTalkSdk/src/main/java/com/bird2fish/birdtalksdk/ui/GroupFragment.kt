package com.bird2fish.birdtalksdk.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.model.ChatSessionManager
import com.bird2fish.birdtalksdk.model.Group
import com.bird2fish.birdtalksdk.model.GroupCache
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.model.UserCache
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper


class GroupFragment : Fragment() {
    private  lateinit var groupListView:RecyclerView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_group, container, false)

        // 获取列表控件
        groupListView = root.findViewById<RecyclerView>(R.id.rvMain)

        val lst = GroupCache.getInGroupList()
        val adapter = GroupsItemAdapter(lst)
        adapter.setView(this)
        // 第三步：给listview设置适配器（view）
        groupListView?.layoutManager = LinearLayoutManager(context)
        groupListView?.setAdapter(adapter);
        return root
    }

    // 发送信息，这里需要跳转
    fun switchSendMsgPage(g:Group){
        // 通过消息方式通知上层界面切换到消息发送
        val chatSession = ChatSessionManager.getSession(g)
        //SdkGlobalData.currentChatFid = f.id
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(
            MsgEventType.APP_NOTIFY_SEND_MSG,
            0, 0, chatSession.getSessionId(), mapOf("page" to "followedFragment" ) )
    }


}

class GroupsItemAdapter(private val dataList: List<Group>) : RecyclerView.Adapter<GroupsItemAdapter.SearchItemHolder>() {

    private var fragment : GroupFragment? = null


    fun setView(view : GroupFragment?){
        this.fragment = view
    }

    // 创建 ViewHolder
    inner class SearchItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ViewHolder 中的视图元素，例如 TextView、ImageView 等
        val imgIcon : ImageView = itemView.findViewById(R.id.iconTv)
        val tvTitle: TextView = itemView.findViewById(R.id.nameTv)
        val tvNumber : TextView = itemView.findViewById(R.id.desTv)

        var index: Int = 0
        var selectedPosition = RecyclerView.NO_POSITION
        var joinBtn: Button = itemView.findViewById(R.id.btn_join)
        var imgButton : ImageView = itemView.findViewById(R.id.btn_setting)

        init {
            // 在构造函数中为整个 ViewHolder 的根视图设置点击事件
            itemView.setOnClickListener {
                // 处理点击事件
//                if (fragment != null){
//                    fragment!!.onClickItem(index)
//                }
            }
            joinBtn.setText(R.string.send_msg)

        }

    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchItemHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_search_groups, parent, false)
        return SearchItemHolder(itemView)
    }


    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: SearchItemHolder, position: Int) {
        val item = dataList[position]
        holder.index = position

        AvatarHelper.tryLoadAvatar(fragment!!.requireContext(), item!!.icon, holder.imgIcon, "", item!!.name)

        val formattedName = "${item!!.name}[${item!!.gid}]"
        holder.tvTitle.setText(formattedName)

        holder.tvNumber.setText(item.tags)
        holder.imgButton.visibility = View.GONE   // 图片按钮，预留的
        holder.joinBtn.setOnClickListener{
            fragment?.switchSendMsgPage(item)
        }

    }

    // 返回数据项数量
    override fun getItemCount(): Int {
        return dataList.size
    }

    // 其他方法，例如添加删除项的方法，用于与 ItemTouchHelper 配合实现左滑删除
    // ...

}