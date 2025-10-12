package com.bird2fish.birdtalksdk.ui

import android.os.Bundle
import android.provider.Settings.Global
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import java.util.LinkedList

class ChatManagerFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var chatPagerAdapter: ChatPagerAdapter
    //private var imageviewPage = ImageViewFragment()   // 预览图片
    //private var filePreviewPage = FilePreviewFragment()  // 预览文件信息
    private lateinit var chatImage : ImageView             //
    private lateinit var chatTitle : TextView              //

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chat_manager, container, false)

        // 初始化 ViewPager2
        this.viewPager = view.findViewById(R.id.chat_pages)

        // 创建并设置适配器
        chatPagerAdapter = ChatPagerAdapter(this)
        this.viewPager.adapter = chatPagerAdapter



        // 设置按钮点击事件以切换页面
        val buttonNext = view.findViewById<Button>(R.id.btn_next_page)
        buttonNext.setOnClickListener {
            // 获取当前页面索引
            val currentItem = this.viewPager.currentItem
            // 切换到下一个页面
            this.viewPager.setCurrentItem(currentItem + 1, true)
        }

        val buttonPre = view.findViewById<Button>(R.id.btn_pre_page)
        buttonPre.setOnClickListener {
            // 获取当前页面索引
            val currentItem = this.viewPager.currentItem
            // 切换到下一个页面
            this.viewPager.setCurrentItem(currentItem - 1, true)
        }

        chatImage = view.findViewById(R.id.header_image)
        chatTitle = view.findViewById(R.id.header_title)

        return view
    }

    // 切换到某个好友的页面

    // 当 Fragment 被隐藏或显示时调用
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            switchToPage(SdkGlobalData.currentChatFid)
        } else {

        }
    }

    // 继续的时候，但是在使用隐藏以及显示模式情况下，这个函数不起用
//    override fun onResume() {
//        super.onResume()
//        view?.post {。
//            if (::viewPager.isInitialized && viewPager.adapter != null) {
//                switchToPage(SdkGlobalData.currentChatFid)
//            }
//        }
//    }

    // 初始化后才能切换
    private fun switchToPage(fid: Long){

        var index = 0
        if (this.chatPagerAdapter != null){
            index = chatPagerAdapter.findIndex(fid)
            if (index < 0){
                index = chatPagerAdapter.addChatPage(fid)
            }
        }


        // 显示好友的信息
        if (fid > 0){
            val f = SdkGlobalData.getMutualFriendLocal(fid)
            if (f != null){
                this.chatTitle.text = f.nick
                AvatarHelper.tryLoadAvatar(requireContext(), f.icon, this.chatImage, f.gender)
            }
        }
        // 显示群组信息，群使用负数表示，这样可以保证一致性
        else{

        }

        if (index >= 0 && index < chatPagerAdapter.getItemCount()){
            this.viewPager.setCurrentItem(index, true)

        }

    }

    // 打开图片的预览
//    fun openImageView(content: Uri?){
//        this.viewPager.setCurrentItem(0, true)
//        imageviewPage.showImage(content)
//
//    }

    // 自定义的 FragmentStateAdapter
    inner class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        // 会话 ID 列表
        private val chatIds = LinkedList<Long>()

        // chatId -> 索引 的映射
        private val chatIdMap = mutableMapOf<Long, Int>()

        /**
         * 添加一个会话页面
         * @return 新页面的索引
         */
        fun addChatPage(fid: Long): Int {
            synchronized(chatIds) {
                if (!chatIdMap.containsKey(fid)) {
                    chatIds.add(fid)
                    chatIdMap[fid] = chatIds.size - 1
                    notifyDataSetChanged()
                }else{

                }
                return chatIdMap[fid]!!
            }
        }



        /**
         * 查找指定会话 ID 的页面索引
         * @return 找不到返回 -1
         */
        fun findIndex(fid: Long): Int {
            synchronized(chatIds) {
                return chatIdMap[fid] ?: -1
            }
        }

        /**
         * 移除一个会话页面
         */
        fun removeChatPage(fid: Long) {
            synchronized(chatIds) {
                val index = chatIdMap.remove(fid)
                if (index != null && index >= 0 && index < chatIds.size) {
                    chatIds.removeAt(index)
                    // 重新调整索引映射
                    chatIdMap.clear()
                    chatIds.forEachIndexed { i, id -> chatIdMap[id] = i }
                }
            }
            notifyDataSetChanged()
        }

        override fun getItemCount(): Int {
            synchronized(chatIds) {
                return chatIds.size
            }
        }

        /**
         * 创建 Fragment
         */
        override fun createFragment(position: Int): Fragment {
            val fid = synchronized(chatIds) {
                chatIds[position]
            }
            // 不缓存 Fragment 实例，交给 FragmentStateAdapter 管理
            return ChatPageFragment.newInstance(fid.toString(), this@ChatManagerFragment)
        }

        /**
         * 唯一标识 Fragment
         */
        override fun getItemId(position: Int): Long {
            synchronized(chatIds) {
                return chatIds[position]
            }
        }

        /**
         * 确认 ID 是否仍然有效
         */
        override fun containsItem(itemId: Long): Boolean {
            synchronized(chatIds) {
                return chatIds.contains(itemId)
            }
        }
    }
}
