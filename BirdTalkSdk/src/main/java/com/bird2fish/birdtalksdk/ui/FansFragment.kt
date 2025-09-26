package com.bird2fish.birdtalksdk.ui

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData

import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.R

class FansFragment : Fragment() , StatusCallback {

    private var friendList: RecyclerView? = null

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
        val view = inflater.inflate(R.layout.fragment_fans, container, false)

        // 获取列表控件
        friendList = view.findViewById<RecyclerView>(R.id.friend_list)

//        var user = User()
//        user.nick = "赵四"
//        user.icon = "sys:5"
//        user.region = "这是一个作者的简介……"
//        SdkGlobalData.fanList += user
//
//        user = User()
//        user.nick = "钱五"
//        user.icon = "sys:6"
//        user.region = "人生弱智如初见，何时秋风悲画扇"
//        SdkGlobalData.fanList += user



        val adapter = FansItemAdapter(SdkGlobalData.getFanList())
        adapter.setView(this)
        // 第三步：给listview设置适配器（view）

        friendList?.layoutManager = LinearLayoutManager(context)
        friendList?.setAdapter(adapter);

//        this.changed.observe(requireActivity(), Observer {
//            adapter.notifyDataSetChanged()
//        })
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

    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }
    // 上传或下载事件
    // 这里是回调函数，无法操作界面
    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.FRIEND_LIST_FAN){

            (context as? Activity)?.runOnUiThread {


                val lst = SdkGlobalData.getFanList()
                val adapter = FansItemAdapter(lst)
                adapter.setView(this)
                friendList?.layoutManager = LinearLayoutManager(context)
                friendList?.setAdapter(adapter);
            }

        }
    }
    // 刷新的时候需要更新个人信息
    override fun onResume() {
        super.onResume()
        // 关注消息
        SdkGlobalData.userCallBackManager.addCallback(this)
    }

    override fun onPause() {
        super.onPause()
        // 取消关注消息
        SdkGlobalData.userCallBackManager.removeCallback(this)
    }

}



// 双向关注的列表，适配器
class FansItemAdapter(private val dataList: List<User>) : RecyclerView.Adapter<FansItemAdapter.FansViewHolder>() {

    private var fragment : FansFragment? = null


    fun setView(view : FansFragment?){
        this.fragment = view
    }

    // 创建 ViewHolder
    inner class FansViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        var btnFollow : Button = itemView.findViewById(R.id.btn_follow)
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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FansViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_fan_item, parent, false)
        return FansViewHolder(itemView)
    }


    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: FansViewHolder, position: Int) {
        val item = dataList[position]
        holder.index = position

//        val id = ImagesHelper.getIconResId(item!!.icon)
//        holder.imgIcon.setImageResource(id)
        AvatarHelper.tryLoadAvatar(fragment!!.requireContext(), item!!.icon, holder.imgIcon, item!!.gender)
        val formattedName = "${item!!.nick}(${item!!.id})"
        holder.tvNick.setText(formattedName)
        holder.tvDes.setText(item!!.introduction)

        // 可以添加其他逻辑...
//        holder.tvDelete.setOnClickListener{
//            if (fragment != null){
//                fragment!!.onClickItemShare(holder.tvDelete.tag as Int)
//            }
//        }

        if (SdkGlobalData.isMutualfollowing(item!!.id)){
            val stringFromRes = fragment!!.getString(R.string.mutual_following)
            holder.btnFollow.text = stringFromRes
            holder.btnFollow.isEnabled = false
        }else{
            val stringFromRes = fragment!!.getString(R.string.follow_back)
            holder.btnFollow.text = stringFromRes
            holder.btnFollow.isEnabled = true
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