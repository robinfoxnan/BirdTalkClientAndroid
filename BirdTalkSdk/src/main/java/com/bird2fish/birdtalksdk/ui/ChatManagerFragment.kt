package com.bird2fish.birdtalksdk.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bird2fish.birdtalksdk.R

class ChatManagerFragment : Fragment() {

    private lateinit var viewPager: ViewPager2
    private lateinit var chatPagerAdapter: ChatPagerAdapter

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

        return view
    }

    // 自定义的 FragmentStateAdapter
    inner class ChatPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {
        // 假设有 4 个会话，实际可以根据需要动态生成
        private val chatIds = listOf("chat1", "chat2", "chat3", "chat4")

        override fun getItemCount(): Int {
            return chatIds.size
        }

        override fun createFragment(position: Int): Fragment {
            // 创建 ChatFragment 实例，并传递对应的 chatId
            return ChatPageFragment.newInstance(chatIds[position])
        }
    }
}
