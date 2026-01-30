package com.bird2fish.birdtalksdk.ui

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.model.ChatSessionManager
import com.bird2fish.birdtalksdk.model.Group
import com.bird2fish.birdtalksdk.model.GroupCache
import com.bird2fish.birdtalksdk.model.Topic
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.model.UserCache
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import java.util.LinkedList

class FriendSelectDialog :  DialogFragment() {

    // 定义一个函数引用
    private lateinit var onSelected: ((List<ListItem>) -> Unit)
    private lateinit  var friendListView:  RecyclerView
    private lateinit var etSearch:TextView
    private lateinit var btnCancel:TextView
    private lateinit var bottomView:LinearLayout
    private lateinit var btnOk :TextView
    private lateinit var adapter:FriendSelectAdapter

    private var mode = "friend"    // "friend" | "mix" | "groupMember"
    // 数据列表
    val items = LinkedList<ListItem>()
    var curGroup:Group? = null

    public fun getMode():String{
        return this.mode
    }

    companion object {
        // 定义参数Key（建议用类名+字段名，避免冲突）
        private const val ARG_GROUP = "arg_group"

        // 静态方法：创建Fragment实例并传入group
        fun newInstance(gid: Long, mod:String): FriendSelectDialog {
            val fragment = FriendSelectDialog()
            val args = Bundle()
            args.putLong("gid", gid) // 🔥 关键：Long类型用putLong
            args.putString("mod", mod)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 解析Long类型的gid参数（非空/合法性校验）
        arguments?.let {
            val gid = it.getLong("gid", 0L) // 第二个参数是默认值
            if (gid == 0L)
            {
                dismiss() // 关闭弹窗
                throw IllegalArgumentException("必须通过newInstance传入有效的gid")
            }
            this.curGroup = GroupCache.findGroupSync(gid)
            this.mode = it.getString("mod", "friend")
        }
    }

    // 检测某个用户是否已经在群里了？
    fun isUserInGroup(u:User):Boolean{
        if (this.curGroup == null)
            return false

        return this.curGroup!!.isMember(u.id)
    }

    // 外部设置一个函数引用，结束时候调用这个函数
    fun setOnFriendSelectedListener(
        listener: (List<ListItem>) -> Unit
    ) {
        onSelected = listener
    }

    fun setSelectMode(m:String){
        this.mode = m
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_friend_select_dialog, container, false)

        etSearch =  root.findViewById<TextView>(R.id.etSearch)
        btnCancel = root.findViewById<TextView>(R.id.tvBtnCancel)
        btnOk = root.findViewById<TextView>(R.id.tvBtnOk)
        bottomView = root.findViewById<LinearLayout>(R.id.cv_bottom)
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 输入前（一般不用）
            }

            override fun onTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 输入过程中
            }

            override fun afterTextChanged(s: Editable) {
                val keyword = s?.toString()?.trim() ?: ""
                setFilter(keyword)
            }
        })

        // 取消按钮
        btnCancel.setOnClickListener {
            etSearch.setText("")
            etSearch.clearFocus()
            btnCancel.visibility = View.GONE
            hideKeyboard(root)
            initData()
            adapter.notifyDataSetChanged()
        }

        btnOk.setOnClickListener{
            this.onClickOk()
        }

        // 获取列表控件
        friendListView = root.findViewById<RecyclerView>(R.id.rvMain)
        adapter = FriendSelectAdapter(initData())
        adapter.setView(this)
        // 第三步：给listview设置适配器（view）
        friendListView?.layoutManager = LinearLayoutManager(context)
        friendListView?.setAdapter(adapter);

        return root
    }


    // 隐藏键盘
    private fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    // 计算原始数据
    fun initData() : List<ListItem>{
        items.clear()
        // 如果使用这样的模式，添加一个横行，里面是最近联系过的会话列表，
        if (mode== "mix")
        {
            items.add(ListItem.RecentContacts(
                topics = ChatSessionManager.rebuildDisplayList()
            ))
        }


        items.add( ListItem.Separator(title = "好友列表"))

        val mutuals = UserCache.getMutualFollowList()
        for (u in mutuals){
            items.add(ListItem.FriendItem(u))
        }
        return items;

    }

    // 设置过滤器
    private fun setFilter(key:String){
        btnCancel.visibility = View.VISIBLE
        val mutuals = UserCache.getMutualFollowList()
        items.clear()
        for (u in mutuals){
            if (u.nick.startsWith(key))
                items.add(ListItem.FriendItem(u))
        }

        adapter.notifyDataSetChanged()
    }

    // 全屏（小红书 / 微信里很常见）
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // 去掉 padding
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            // 底部弹出（可选）
            //setGravity(Gravity.BOTTOM)
        }
    }

    // 调用了回调函数
    fun onClickOk(){
        val lst = LinkedList<ListItem>()
        for (item in this.items){
            when(item){
                is ListItem.GroupItem -> {}
                is ListItem.RecentContacts -> {}
                is ListItem.Separator -> {}
                is ListItem.FriendItem -> {
                    if (item.checked){
                        lst.add(item)
                    }
                }
            }
        }
        if (this.onSelected != null){
            this.onSelected(lst)
        }
        this.dismiss()
    }

}
//

/**
RecyclerView（纵向）
├─ TYPE_HEADER_SEARCH
├─ TYPE_RECENT_HORIZONTAL   ← 里面再嵌一个横向 RV
├─ TYPE_SECTION_TITLE（作者）
├─ TYPE_USER_ITEM
├─ TYPE_SECTION_TITLE（最近聊天）
├─ TYPE_USER_ITEM
├─ TYPE_USER_ITEM
└─ ...
 */
// 定义一个封闭类表示列表条目
// 列表条目封闭类
sealed class ListItem {
    // 类型1：最近联系人
    data class RecentContacts(
        val topics: List<Topic>
    ) : ListItem()

    // 类型2：分隔条
    data class Separator(
        val title: String
    ) : ListItem()

    // 类型3：好友
    data class FriendItem(
        val user: User,
        var checked:Boolean = false
    ) : ListItem()

    // 类型4：群组
    data class GroupItem(
        val group: Group,
        var checked:Boolean = false
    ):ListItem()
}

// 双向关注的列表，适配器
class FriendSelectAdapter(private val dataList: List<ListItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var fragment: FriendSelectDialog? = null

    fun setView(view: FriendSelectDialog?) {
        this.fragment = view
    }

    // 确定类型
    override fun getItemViewType(position: Int): Int {
        return when (dataList[position]) {
            is ListItem.RecentContacts -> 1
            is ListItem.Separator -> 2
            is ListItem.FriendItem -> 3
            is ListItem.GroupItem -> 4
        }
    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            1 -> {
                val view = inflater.inflate(R.layout.item_recent_container, parent, false)
                RecentContactsViewHolder(view)
            }
            2 -> {
                val view = inflater.inflate(R.layout.item_seperator, parent, false)
                SeparatorViewHolder(view)
            }
            3 -> {
                val view = inflater.inflate(R.layout.item_user_select, parent, false)
                FriendViewHolder(view)
            }
            else -> throw IllegalArgumentException("未知 viewType $viewType")
        }
    }

    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = dataList[position]
        when {
            holder is RecentContactsViewHolder && item is ListItem.RecentContacts -> {
                holder.bind(item)
            }
            holder is SeparatorViewHolder && item is ListItem.Separator -> {
                holder.bind(item)
            }
            holder is FriendViewHolder && item is ListItem.FriendItem -> {
                holder.bind(item)
            }
        }
    }

    // 返回数据项数量
    override fun getItemCount(): Int = dataList.size

    ////////////////////////////////////////////////////////////////////////////////////////
    // 创建 ViewHolder
    inner class RecentContactsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private var recentTopicListView:  RecyclerView? = itemView.findViewById(R.id.rvRecent)

        fun bind(item: ListItem.RecentContacts) {
            val adapter = RecentTopicSelectAdapter(item.topics)
            recentTopicListView?.layoutManager = LinearLayoutManager(itemView.context, LinearLayoutManager.HORIZONTAL, false)
            recentTopicListView?.setAdapter(adapter);
        }
    }

    inner class SeparatorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title :TextView = itemView.findViewById(R.id.tv_s_title)
        fun bind(item: ListItem.Separator) {
            title.setText(item.title)
        }
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgIcon : ImageView = itemView.findViewById(R.id.ivAvatar)
        val nick :TextView = itemView.findViewById(R.id.tvName)
        val radio : ImageView = itemView.findViewById(R.id.ivCheck)
        fun bind(item: ListItem.FriendItem) {
            AvatarHelper.tryLoadAvatar(itemView.context, item.user.icon, imgIcon, item.user.gender, item.user.nick)
            nick.text = item.user.nick
            radio.setOnClickListener{
                item.checked = !item.checked
                if (item.checked){
                    radio.setImageResource(R.drawable.ic_radio_checked)
                }else{
                    radio.setImageResource(R.drawable.ic_radio_unchecked)
                }
            }

            // 如果这样的模式，需要禁用已经加入到
            if (fragment!!.getMode() == "groupMember"){
                if (fragment!!.isUserInGroup(item.user)){
                    radio.visibility = View.GONE
                }
            }
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////////
}

// 第1行的横向的联系人滚动框
class RecentTopicSelectAdapter(
    private val dataList: List<Topic>
) : RecyclerView.Adapter<RecentTopicSelectAdapter.RecentTopicViewHolder>() {

    // 点击事件回调
    var onItemClick: ((Topic) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecentTopicViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.item_recent_topic, parent, false)
        return RecentTopicViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecentTopicViewHolder, position: Int) {
        val topic = dataList[position]
        holder.bind(topic)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(topic)
        }
    }

    override fun getItemCount(): Int = dataList.size

    ////////////////////////////////////////////////////////////////////////////////////
    // ViewHolder
    inner class RecentTopicViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)

        fun bind(topic: Topic) {
            // TODO: 填充头像和名称
            // ivAvatar.setImageResource(...) 或用 Glide/Picasso 加载
            AvatarHelper.tryLoadAvatar(SdkGlobalData.context!!, topic.icon, ivAvatar, "", topic.title)
            tvName.text = topic.title
        }
    }
    ////////////////////////////////////////////////////////////////////////////////////
}
