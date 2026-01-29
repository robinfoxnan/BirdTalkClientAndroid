package com.bird2fish.birdtalksdk.ui

import android.app.Activity
import android.media.AudioManager
import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Im
import android.provider.Settings.Global
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.view.menu.MenuPopupHelper
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.db.GroupDbHelper
import com.bird2fish.birdtalksdk.db.TopicDbHelper
import com.bird2fish.birdtalksdk.db.UserDbHelper
import com.bird2fish.birdtalksdk.model.Group
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import java.lang.reflect.Field
import java.util.LinkedList

class ChatManagerFragment : Fragment() , StatusCallback {

    private lateinit var viewPager: ViewPager2
    private lateinit var chatPagerAdapter: ChatPagerAdapter
    //private var imageviewPage = ImageViewFragment()   // 预览图片
    //private var filePreviewPage = FilePreviewFragment()  // 预览文件信息
    private lateinit var chatImage : ImageView             //
    private lateinit var chatTitle : TextView              //
//    private lateinit var  buttonNext: ImageView            // 上一页
//    private lateinit var  buttonPre: ImageView             // 下一页
    private lateinit var  buttonSetting: ImageView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_chat_manager, container, false)

        // 初始化 ViewPager2
        this.viewPager = view.findViewById(R.id.chat_pages)
        this.viewPager.setOnTouchListener { _, _ -> true }
        // 核心：彻底禁用手动滑动（替代原有单独的 setOnTouchListener）
        this.viewPager.disableSwipeCompletely()

        // 创建并设置适配器
        chatPagerAdapter = ChatPagerAdapter(this)
        this.viewPager.adapter = chatPagerAdapter

        // 设置按钮
        buttonSetting  = view.findViewById<ImageView>(R.id.btn_setting)

        chatImage = view.findViewById(R.id.header_image)
        chatTitle = view.findViewById(R.id.header_title)

        initButtons()
        SdkGlobalData.userCallBackManager.addCallback(this)
        return view
    }


    /**
     * ViewPager2 扩展方法：彻底禁用手动左右滑动（解决内部 RecyclerView 滑动穿透问题）
     */
    fun ViewPager2.disableSwipeCompletely() {
        try {
            // 反射获取 ViewPager2 内部的 mRecyclerView 字段（核心，底层滑动容器）
            val field: Field = ViewPager2::class.java.getDeclaredField("mRecyclerView")
            field.isAccessible = true // 突破私有字段访问限制
            val recyclerView = field.get(this) as android.view.View
            // 给内部 RecyclerView 设置触摸监听器，完全拦截滑动手势
            recyclerView.setOnTouchListener { _, _ -> true }
        } catch (e: Exception) {
            // 反射异常不影响核心功能，仅打印日志（兼容不同 ViewPager2 版本）
            e.printStackTrace()
        }
        // 同时保留 ViewPager2 自身的触摸拦截（双重保障）
        this.setOnTouchListener { _, _ -> true }
    }



    fun initButtons(){
        buttonSetting.setOnClickListener {
            // 1. 初始化PopupMenu，传入上下文和触发按钮
            val popupMenu = PopupMenu(requireContext(), it)
            // 2. 加载菜单资源
            popupMenu.menuInflater.inflate(R.menu.menu_chat_page_manager, popupMenu.menu)
            // 2. 根据当前音频模式，动态控制菜单项显示/隐藏（核心步骤）
            // 2.1 找到对应的菜单项
            val speakerItem = popupMenu.menu.findItem(R.id.menu_speaker_play)
            val headphoneItem = popupMenu.menu.findItem(R.id.menu_headphone_play)
            val groupSettingItem = popupMenu.menu.findItem(R.id.menu_setting_group)

            // 比如：当前是扬声器模式，就隐藏“扬声器播放”项，显示“耳机播放”项
            speakerItem.setVisible(!SdkGlobalData.useLoudSpeaker)
            headphoneItem.setVisible(SdkGlobalData.useLoudSpeaker)

            // 3. 设置菜单选项的点击监听
            popupMenu.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.menu_speaker_play -> {
                        // 处理扬声器播放的逻辑
                        switchToSpeakerPlayback()
                        true // 表示已处理该点击事件
                    }
                    R.id.menu_headphone_play -> {
                        // 处理耳机播放的逻辑
                        switchToHeadphonePlayback()
                        true
                    }
                    R.id.menu_setting_group ->{
                        // 处理群组设置的逻辑
                        openGroupSetting()
                        true
                    }
                    else -> false
                }
            }

            // 4. 群设置按钮
            if ( SdkGlobalData.currentChatSession != null){
                if (SdkGlobalData.currentChatSession!!. isGroupChat()){
                    groupSettingItem.setVisible(true)
                }else{
                    groupSettingItem.setVisible(false)
                }
            }else{
                groupSettingItem.setVisible(false)
            }



            // . 显示弹出菜单
            popupMenu.show()
        }

        //        // 设置按钮点击事件以切换页面
//        buttonNext = view.findViewById<ImageView>(R.id.btn_next_page)
//        buttonNext.setOnClickListener {
//            // 获取当前页面索引
//            val currentItem = this.viewPager.currentItem
//            // 切换到下一个页面
//            this.viewPager.setCurrentItem(currentItem + 1, true)
//        }
//
//        buttonPre = view.findViewById<ImageView>(R.id.btn_pre_page)
//        buttonPre.setOnClickListener {
//            // 获取当前页面索引
//            val currentItem = this.viewPager.currentItem
//            // 切换到下一个页面
//            this.viewPager.setCurrentItem(currentItem - 1, true)
//        }
    }

    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }
    // 上传或下载事件
    // 这里是回调函数，无法操作界面
    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.GROUP_UPDATE_INFO_OK){
            (context as? Activity)?.runOnUiThread {
                // 显示好友的信息
                val curSession = SdkGlobalData.currentChatSession ?: return@runOnUiThread
                this.chatTitle.text = curSession.title
                AvatarHelper.tryLoadAvatar(requireContext(), curSession.icon, this.chatImage, curSession.getGender(), curSession.getNick())
            }
        }
    }

    // 打开群组属性设置页面
    fun openGroupSetting(){
        val gid = SdkGlobalData.currentChatSession!!.tid
        val page = GroupSettingFragment.newInstance(gid)
        page.show(parentFragmentManager, "GroupSettingDialog")
    }

    // 切换到扬声器播放模式
    fun switchToSpeakerPlayback(){
        SdkGlobalData.useLoudSpeaker = true
        val audioManager = requireActivity().getSystemService(Activity.AUDIO_SERVICE) as? AudioManager
        audioManager?.apply {
            // 取消静音，设置为扬声器外放
            setSpeakerphoneOn(true)
            // 设置音频流为铃声模式（适配通知铃声）
            mode = AudioManager.MODE_NORMAL
        }
    }

    // 播放系统默认通知铃声（带 1 秒节流 + 音频输出控制）
    fun switchToHeadphonePlayback(){
        SdkGlobalData.useLoudSpeaker = false
        val audioManager = requireActivity().getSystemService(Activity.AUDIO_SERVICE) as? AudioManager
        audioManager?.apply {
            // 关闭扬声器，切换到听筒（无耳机时）/耳机（有耳机时）
            setSpeakerphoneOn(false)
            // 设置为通话模式（适配听筒播放）
            mode = AudioManager.MODE_IN_COMMUNICATION
        }
    }

//    fun checkShowButtons(){
//        // 1. 正确获取ViewPager的页面数量（适配标准ViewPager）
//        val pageCount = viewPager.adapter!!.itemCount ?: 0
//        // 2. 页面数<2时，直接隐藏两个按钮
//        if (pageCount < 2) {
//            buttonPre.visibility = View.GONE
//            buttonNext.visibility = View.GONE
//            return
//        }
//        // 3. 获取当前页码
//        val currentPos = viewPager.currentItem
//
//        // 4. 完整处理上一页按钮：有上一页则显示，否则隐藏
//        buttonPre.visibility = if (currentPos > 0) View.VISIBLE else View.GONE
//
//        // 5. 完整处理下一页按钮：有下一页则显示，否则隐藏
//        buttonNext.visibility = if (currentPos < pageCount - 1) View.VISIBLE else View.GONE
//    }

    // 切换到某个好友的页面

    // 当 Fragment 被隐藏或显示时调用
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            switchToPage()
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
    private fun switchToPage(){

        val curSession = SdkGlobalData.currentChatSession ?: return

        var index = 0
        if (this.chatPagerAdapter != null){
            index = chatPagerAdapter.findIndex(curSession.getSessionId())
            if (index < 0){
                index = chatPagerAdapter.addChatPage(curSession.getSessionId())
            }
        }

        // 显示好友的信息
        this.chatTitle.text = curSession.title
        AvatarHelper.tryLoadAvatar(requireContext(), curSession.icon, this.chatImage, curSession.getGender(), curSession.getNick())

        if (index >= 0 && index < chatPagerAdapter.getItemCount()){
            this.viewPager.setCurrentItem(index, true)
        }

        //checkShowButtons()

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
