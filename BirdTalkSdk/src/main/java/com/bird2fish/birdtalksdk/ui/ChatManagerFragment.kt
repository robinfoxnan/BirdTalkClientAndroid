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
    fun switchToFriend(fid: Long){
        SdkGlobalData.currentChatFid = fid

    }

    // 继续的时候
    override fun onResume() {
        super.onResume()
        switchToPage(SdkGlobalData.currentChatFid)
    }

    private fun switchToPage(fid: Long){
        var index = chatPagerAdapter.findIndex(fid)
        if (index == -1){
            index = chatPagerAdapter.addChatPage(fid)
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


        this.viewPager.setCurrentItem(index, true)
    }

    // 打开图片的预览
//    fun openImageView(content: Uri?){
//        this.viewPager.setCurrentItem(0, true)
//        imageviewPage.showImage(content)
//
//    }

    // 自定义的 FragmentStateAdapter
    inner class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        // 假设有 4 个会话，实际可以根据需要动态生成
        private val chatIds = LinkedList<Long>()
        private val chatIdMap = mutableMapOf<Long, Fragment>()

        // 添加页面
        fun addChatPage(fid:Long):Int{
            synchronized(chatIds){
                chatIds.add(fid)
                return chatIds.size - 1
            }
        }

        // 找不到就返回-1
        fun findIndex(fid:Long):Int{
            synchronized(chatIds){
                val firstIndex: Int = chatIds.indexOf(100L)
                return firstIndex
            }
        }

        fun removeChatPage(fid:Long){
            synchronized(chatIds){
                chatIds.remove(fid)
            }
        }

        override fun getItemCount(): Int {
            synchronized(chatIds){
             return chatIds.size
            }
        }

        override fun createFragment(position: Int): Fragment {
            // 创建 ChatFragment 实例，并传递对应的 chatId
            var fid = 0L
            synchronized(chatIds){
                fid = chatIds[position]
            }

            synchronized(chatIdMap){
                if (chatIdMap.containsKey(fid)){
                    return chatIdMap[fid]!!
                }else{
                    val fragment = ChatPageFragment.newInstance(fid.toString(), this@ChatManagerFragment)
                    fragment.setChatPeer(fid)
                    chatIdMap[fid] = fragment
                    return fragment
                }
            }

        }
    }
}
