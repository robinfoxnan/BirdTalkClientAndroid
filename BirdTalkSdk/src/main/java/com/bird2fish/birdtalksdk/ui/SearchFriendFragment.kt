package com.bird2fish.birdtalksdk.ui

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.provider.Settings.Global
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import androidx.appcompat.widget.AppCompatImageButton
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.model.Group
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility


class SearchFriendFragment : Fragment(), StatusCallback {
    // TODO: Rename and change types of parameters

    private var friendListView: RecyclerView? = null
    private lateinit var  etSearch: EditText
    private lateinit var  btnSearchUser: AppCompatImageButton
    private lateinit var  btnSearchGroup: AppCompatImageButton

    private var searchFriend = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search_friend, container, false)


        // 获取搜索框和按钮的实例
        etSearch = view.findViewById(R.id.et_search_keyword)
        btnSearchUser = view.findViewById(R.id.btn_search_user)
        btnSearchGroup = view.findViewById(R.id.btn_search_group)


        // 搜用户按钮，设置按钮点击事件
        btnSearchUser.setOnClickListener { v: View ->
            // 获取搜索框中的文本并去除前后空格
            val keyword = etSearch.text.toString().trim { it <= ' ' }


            // 检查输入内容是否为空
            if (keyword.isEmpty()) {
                // 提示用户输入搜索内容
                Toast.makeText(requireContext(), "请输入搜索关键字", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // 隐藏软键盘
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)

            // 执行搜索操作（这里只是示例，实际应替换为你的业务逻辑）
            searchFriend = true
            performUserSearch(keyword)
        }

        // 搜群按钮，设置按钮点击事件
        btnSearchGroup.setOnClickListener { v: View ->
            // 获取搜索框中的文本并去除前后空格
            val keyword = etSearch.text.toString().trim { it <= ' ' }


            // 检查输入内容是否为空
            if (keyword.isEmpty()) {
                // 提示用户输入搜索内容
                Toast.makeText(requireContext(), "请输入搜索关键字", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            // 隐藏软键盘
            val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(v.windowToken, 0)

            // 执行搜索操作（这里只是示例，实际应替换为你的业务逻辑）
            searchFriend = false
            performGroupSearch(keyword)
        }


        // 获取列表控件
        friendListView = view.findViewById<RecyclerView>(R.id.recycler_view_friends)


        val lst = SdkGlobalData.getSearchFriendRet()
        val adapter = SearchFriendsItemAdapter(lst)
        adapter.setView(this)
        // 第三步：给listview设置适配器（view）
        friendListView?.layoutManager = LinearLayoutManager(context)
        friendListView?.setAdapter(adapter);

        // 关注消息
        SdkGlobalData.userCallBackManager.addCallback(this)
        return view
    }


    // 识别一下需要使用哪种方式查询
    fun recognizeStringType(input: String): String {


        // 检查是否为手机号（简单匹配中国大陆手机号规则）
        if (input.matches(Regex("^1[3-9]\\d{9}$"))) {
            return "phone"
        }

        // 检查是否为邮箱（基本邮箱格式验证）
        if (input.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"))) {
            return "email"
        }

        // 检查是否为数字ID（纯数字）
        if (input.matches(Regex("^\\d+$"))) {
            return "id"
        }

        // 都不是则返回name
        return "name"
    }

    // 搜索业务逻辑方法
    // "id" "name"  "email"  "phone"
    private fun performUserSearch(keyword: String) {
        // 这里实现具体的搜索逻辑，例如：
        // 1. 发起网络请求查询用户
        // 2. 从本地数据库查询数据
        // 3. 跳转到搜索结果页面

        // 示例：显示搜索内容

        //Toast.makeText(requireContext(), "正在搜索: $keyword", Toast.LENGTH_SHORT).show()
        val modeSearch = recognizeStringType(keyword)
        MsgEncocder.sendFriendFindMessage(modeSearch, keyword)

        // 实际应用中通常会跳转到结果页面
        /*
        Intent intent = new Intent(this, SearchResultActivity.class);
        intent.putExtra("keyword", keyword);
        startActivity(intent);
        */
    }


    private fun performGroupSearch(keyword: String) {
        // 这里实现具体的搜索逻辑，例如：
        // 1. 发起网络请求查询用户
        // 2. 从本地数据库查询数据
        // 3. 跳转到搜索结果页面

        // 示例：显示搜索内容

        //Toast.makeText(requireContext(), "正在搜索: $keyword", Toast.LENGTH_SHORT).show()
        MsgEncocder.sendFindGroupMessage(keyword, 0)

        // 实际应用中通常会跳转到结果页面
        /*
        Intent intent = new Intent(this, SearchResultActivity.class);
        intent.putExtra("keyword", keyword);
        startActivity(intent);
        */
    }


    // 在界面中显示提示信息
    fun showDialogInCallback(context: Context, message: String) {
        // 假设这是你的回调
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // 点击了按钮，比如关注
    fun onClickButtonFollow(f:User){
        MsgEncocder.sendFriendAddMessage(f.id)
    }

    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }
    // 上传或下载事件
    // 这里是回调函数，无法操作界面
    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.SEARCH_FRIEND_RET){
            val lst = SdkGlobalData.getSearchFriendRet()
            val info = "return user count: " + lst.size.toString()
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, info, Toast.LENGTH_SHORT).show()

                val lst = SdkGlobalData.getSearchFriendRet()
                val adapter = SearchFriendsItemAdapter(lst)
                adapter.setView(this)
                friendListView?.layoutManager = LinearLayoutManager(context)
                friendListView?.setAdapter(adapter);
            }

        }else if (eventType == MsgEventType. FRIEND_REQ_REPLY){
            // 请求关注，返回结果
            (context as? Activity)?.runOnUiThread {
                friendListView?.adapter?.notifyDataSetChanged()
            }
        }
        else if (eventType == MsgEventType.SEARCH_GROUP_RET){

            (context as? Activity)?.runOnUiThread {
                val groupLst = SdkGlobalData.getSearchGroupRet()
                val info = "return user count: " + groupLst.size.toString()
                Toast.makeText(context, info, Toast.LENGTH_SHORT).show()

                val adapter = SearchGroupsItemAdapter(groupLst)
                adapter.setView(this)
                friendListView?.layoutManager = LinearLayoutManager(context)
                friendListView?.setAdapter(adapter);
            }
        }
    }
    // 刷新的时候需要更新个人信息
    override fun onResume() {
        super.onResume()

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 取消关注消息
        SdkGlobalData.userCallBackManager.removeCallback(this)
    }
}

//////////////////////////////////////////////////////////////////////////////////////////////////

// 搜索结果，用户或者群聊，适配器
class SearchFriendsItemAdapter(private val dataList: List<User>) : RecyclerView.Adapter<SearchFriendsItemAdapter.SearchFriendsItemHolder>() {

    private var fragment : SearchFriendFragment? = null


    fun setView(view : SearchFriendFragment?){
        this.fragment = view
    }

    // 创建 ViewHolder
    inner class SearchFriendsItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ViewHolder 中的视图元素，例如 TextView、ImageView 等
        val imgIcon : ImageView = itemView.findViewById(R.id.iconTv)
        //val tvUid : TextView =  itemView.findViewById(R.id.tv_fav_uid)
        val tvNick: TextView = itemView.findViewById(R.id.nameTv)
        //val tvDate : TextView = itemView.findViewById(R.id.tv_fav_date)
        //val tvTitle : TextView = itemView.findViewById(R.id.tv_fav_title)
        val tvDes : TextView = itemView.findViewById(R.id.desTv)
        //val tvDelete : TextView = itemView.findViewById(R.id.tv_fav_share)
        var index: Int = 0
        var selectedPosition = RecyclerView.NO_POSITION
        var followBtn: Button = itemView.findViewById(R.id.btn_follow)
        var imgButton :ImageView = itemView.findViewById(R.id.btn_setting)

        init {
            // 在构造函数中为整个 ViewHolder 的根视图设置点击事件
            itemView.setOnClickListener {
                // 处理点击事件
//                if (fragment != null){
//                    fragment!!.onClickItem(index)
//                }
            }
        }

    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchFriendsItemHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_search_friends, parent, false)
        return SearchFriendsItemHolder(itemView)
    }


    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: SearchFriendsItemHolder, position: Int) {
        val item = dataList[position]
        holder.index = position


        //val id = ImagesHelper.getIconResId(item!!.icon)
        //holder.imgIcon.setImageResource(id)
        AvatarHelper.tryLoadAvatar(fragment!!.requireContext(), item!!.icon, holder.imgIcon, item!!.gender, item!!.nick)

        val formattedName = "${item!!.nick}(${item!!.id})"
        holder.tvNick.setText(formattedName)
        holder.tvDes.setText(item!!.introduction)
        holder.imgButton.visibility = View.GONE   // 图片按钮，预留的

        // 可以添加其他逻辑...
//        holder.tvDelete.setOnClickListener{
//            if (fragment != null){
//                fragment!!.onClickItemShare(holder.tvDelete.tag as Int)
//            }
//        }



        // 如果搜到了自己，不显示
        if (item!!.id == SdkGlobalData.selfUserinfo.id){
            holder.followBtn.visibility = View.GONE
        }


        // 双向关注
        else if (SdkGlobalData.isMutualfollowing(item!!.id)){
            holder.followBtn.isEnabled = false

            val stringFromRes = fragment!!.getString(R.string.mutual_following)
            holder.followBtn.text = stringFromRes
        } else if (SdkGlobalData.isFollowing(item!!.id)){
            holder.followBtn.isEnabled = false

            val stringFromRes = fragment!!.getString(R.string.followed)     // 已经关注
            holder.followBtn.text = stringFromRes
        } else if (SdkGlobalData.isMutualfollowing(item!!.id)){
            holder.followBtn.isEnabled = true

            val stringFromRes = fragment!!.getString(R.string.follow_back)    // 粉丝
            holder.followBtn.text = stringFromRes
            // 点击按钮
            holder.followBtn.setOnClickListener{
                if (fragment != null){
                    fragment!!.onClickButtonFollow(item!!)
                }
            }
        }else{
            holder.followBtn.isEnabled = true

            val stringFromRes = fragment!!.getString(R.string.follow)
            holder.followBtn.text = stringFromRes
            // 点击按钮
            holder.followBtn.setOnClickListener{
                if (fragment != null){
                    fragment!!.onClickButtonFollow(item!!)
                }
            }
        }

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
//////////////////////////////////////////////////////////////////////////////////////
// 搜索结果，用户或者群聊，适配器
class SearchGroupsItemAdapter(private val dataList: List<Group>) : RecyclerView.Adapter<SearchGroupsItemAdapter.SearchItemHolder>() {

    private var fragment : SearchFriendFragment? = null


    fun setView(view : SearchFriendFragment?){
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
        var imgButton :ImageView = itemView.findViewById(R.id.btn_setting)

        init {
            // 在构造函数中为整个 ViewHolder 的根视图设置点击事件
            itemView.setOnClickListener {
                // 处理点击事件
//                if (fragment != null){
//                    fragment!!.onClickItem(index)
//                }
            }
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

        AvatarHelper.tryLoadAvatar(fragment!!.requireContext(), item!!.icon, holder.imgIcon, "", item!!.title)

        val formattedName = "${item!!.title}[${item!!.tid}]"
        holder.tvTitle.setText(formattedName)

        holder.tvNumber.setText(item.tags)
        holder.imgButton.visibility = View.GONE   // 图片按钮，预留的

    }

    // 返回数据项数量
    override fun getItemCount(): Int {
        return dataList.size
    }

    // 其他方法，例如添加删除项的方法，用于与 ItemTouchHelper 配合实现左滑删除
    // ...

}