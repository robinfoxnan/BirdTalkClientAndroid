package com.bird2fish.birdtalksdk.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bird2fish.birdtalksdk.GlobalData
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.R

class FollowedFragment : Fragment() {

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

        var user = User()
        user.nick = "飞鸟真人"
        user.icon = "sys:1"
        user.region = "这是一个作者的简介……"
        GlobalData.followedList += user

        user = User()
        user.nick = "天涯歌女"
        user.icon = "sys:2"
        user.region = "人生弱智如初见，何时秋风悲画扇"
        GlobalData.followedList += user



        val adapter = FollowedItemAdapter(GlobalData.followedList)
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
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowedViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.layout_followed_item, parent, false)
        return FollowedViewHolder(itemView)
    }


    // 绑定数据到 ViewHolder
    override fun onBindViewHolder(holder: FollowedViewHolder, position: Int) {
        val item = dataList[position]
        holder.index = position

        val id = ImagesHelper.getIconResId(item!!.icon)
        holder.imgIcon.setImageResource(id)
        holder.tvNick.setText(item!!.nick)
        holder.tvDes.setText(item!!.region)

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