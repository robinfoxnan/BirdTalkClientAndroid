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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper

class FollowedFragment : Fragment() , StatusCallback {

    private var _binding: FollowedFragment? = null
    private val binding get() = _binding!!
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
        val view = inflater.inflate(R.layout.fragment_followed, container, false)

        // 获取列表控件
        friendList = view.findViewById<RecyclerView>(R.id.friend_list)
        val adapter = FollowedItemAdapter(SdkGlobalData.getMutualFollowList())
        adapter.setView(this)
        // 第三步：给listview设置适配器（view）

        friendList?.layoutManager = LinearLayoutManager(context)
        friendList?.setAdapter(adapter);

        // 关注消息
        SdkGlobalData.userCallBackManager.addCallback(this)
        //this.friendList?.adapter?.notifyDataSetChanged()
        return view
    }



    // 发送信息，这里需要跳转
    fun sendMsgTo(f: User){
        // 通过消息方式通知上层界面切换到消息发送
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.APP_NOTIFY_SEND_MSG,
            0, 0, f.id, mapOf("page" to "followedFragment" ) )
    }


    fun onClickItem(index: Int){

    }

    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }
    // 上传或下载事件
    // 这里是回调函数，无法操作界面
    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.FRIEND_LIST_FOLLOW || eventType == MsgEventType.FRIEND_LIST_FAN){

            (context as? Activity)?.runOnUiThread {

                val adapter = FollowedItemAdapter(SdkGlobalData.getMutualFollowList())
                adapter.setView(this)
                friendList?.layoutManager = LinearLayoutManager(context)
                friendList?.setAdapter(adapter);
            }

        }
    }
    // 刷新的时候需要更新个人信息
//    override fun onResume() {
//        super.onResume()
//
//    }



    override fun onDestroyView() {
        super.onDestroyView()
        // 取消关注消息
        SdkGlobalData.userCallBackManager.removeCallback(this)
    }

}

// 双向关注的列表，适配器
class FollowedItemAdapter(private val dataList: List<User>) : RecyclerView.Adapter<FollowedItemAdapter.FollowedViewHolder>() {

    private var fragment : FollowedFragment? = null


    fun setView(view : FollowedFragment?){
        this.fragment = view
    }

    // 创建 ViewHolder
    inner class FollowedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
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
        var btnSendMsg : Button = itemView.findViewById(R.id.btn_send_msg)

        init {
            // 在构造函数中为整个 ViewHolder 的根视图设置点击事件
            itemView.setOnClickListener {
                // 处理点击事件，双向好友，可以从这里跳转到发送消息界面
                if (fragment != null){
                    fragment!!.onClickItem(index)
                }
            }
        }

    }

    // 创建 ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowedViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_followed_item, parent, false)
        return FollowedViewHolder(itemView)
    }


    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: FollowedViewHolder, position: Int) {
        val item = dataList[position]
        holder.index = position

        //val id = ImagesHelper.getIconResId(item!!.icon)
        //holder.imgIcon.setImageResource(id)
        AvatarHelper.tryLoadAvatar(fragment!!.requireContext(), item!!.icon, holder.imgIcon, item!!.gender)

        val formattedName = "${item!!.nick}(${item!!.id})"
        holder.tvNick.setText(formattedName)
        holder.tvDes.setText(item!!.introduction)

        // 可以添加其他逻辑...
        holder.btnSendMsg.setOnClickListener{
            if (fragment != null){
                fragment!!.sendMsgTo(item)
            }
        }
        holder.btnSendMsg.isEnabled = true


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