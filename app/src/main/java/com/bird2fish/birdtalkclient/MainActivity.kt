package com.bird2fish.birdtalkclient

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalkclient.AppPageCode.*
import com.bird2fish.birdtalkclient.databinding.ActivityMainBinding
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.ui.ChatManagerFragment
import com.bird2fish.birdtalksdk.ui.ChatSessionFragment
import com.bird2fish.birdtalksdk.ui.ContactFragment
import com.bird2fish.birdtalksdk.ui.LoginCodeFragment
import com.bird2fish.birdtalksdk.ui.LoginFragment
import com.bird2fish.birdtalksdk.ui.ProfileFragment


enum class AppPageCode {
    HOME,
    CHAT_SESSION,
    PROFILE,
    LOGIN,
    LOGIN_WITH_CODE,
    CONTACT_SDK,
    CHAT_SDK,
    PROFILE_SDK,
    //CONTACTS,
    //TEST,
    //CHAT,
}

class MainActivity : AppCompatActivity() , StatusCallback {
    private lateinit var binding: ActivityMainBinding

    // 用map管理页面
    private val fragmentMap = mutableMapOf<AppPageCode, Fragment>()
    private  @IdRes var lastBtnId : Int = R.id.b_tab_btn_main


    // 首次初始化 FragmentMap
    private fun initFragmentMap() {
        fragmentMap[AppPageCode.HOME] = createFragment(AppPageCode.HOME)
        fragmentMap[AppPageCode.CONTACT_SDK] = createFragment(AppPageCode.CONTACT_SDK)
        fragmentMap[AppPageCode.CHAT_SESSION] = createFragment(AppPageCode.CHAT_SESSION)
        fragmentMap[AppPageCode.CHAT_SDK] = createFragment(AppPageCode.CHAT_SDK)
        fragmentMap[AppPageCode.PROFILE] = createFragment(AppPageCode.PROFILE)
        fragmentMap[AppPageCode.LOGIN] = createFragment(AppPageCode.LOGIN)
        fragmentMap[AppPageCode.LOGIN_WITH_CODE] = createFragment(AppPageCode.LOGIN_WITH_CODE)
        fragmentMap[AppPageCode.PROFILE_SDK] = createFragment(AppPageCode.PROFILE_SDK)

        // 默认加载的 Fragment
        switchFragment(AppPageCode.CONTACT_SDK)
    }

    // 重建时恢复 FragmentMap（从 FragmentManager 中查找）
    private fun restoreFragmentMap() {
        AppPageCode.values().forEach { code ->
            val fragment = supportFragmentManager.findFragmentByTag(code.name)
            if (fragment != null) {
                fragmentMap[code] = fragment // 用系统恢复的实例覆盖 map 中的旧引用
            } else {
                fragmentMap[code] = createFragment(code) // 若系统未恢复，则创建新实例
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));
        }

        // 设置状态栏文字为深色（API 23及以上支持）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // 初始化或恢复 Fragment 实例
        if (savedInstanceState == null) {
            // 首次创建：初始化所有 Fragment 到 map
            initFragmentMap()
        } else {
            // 重建时：从 FragmentManager 恢复已有实例到 map
            restoreFragmentMap()
        }

        // 初始化底部工具栏 tab
        setupClickHandler<TextView>(HOME, R.id.b_tab_btn_main)
        setupClickHandler<TextView>(CONTACT_SDK, R.id.b_tab_btn_friends)
        setupClickHandler<TextView>(CHAT_SESSION, R.id.b_tab_btn_session)
        setupClickHandler<TextView>(CHAT_SDK, R.id.b_tab_btn_msg)
        setupClickHandler<TextView>(PROFILE_SDK, R.id.b_tab_btn_me)

        // 初始化网络连接
        //GlobalData.init(getApplicationContext())
        // 关注消息
        SdkGlobalData.userCallBackManager.addCallback(this)
    }

    override fun finish() {
        // 取消关注消息
        SdkGlobalData.userCallBackManager.removeCallback(this)
        super.finish() // 必须调用父类方法完成销毁
    }

    // 用于设置TAB按钮响应函数的模板函数
    private fun <T : View> setupClickHandler(pageId: AppPageCode, @IdRes viewId: Int) {
        val view = findViewById<T>(viewId)
        view.setOnClickListener {
            switchFragment(pageId)
            setTabBtnSize(viewId)
        }
    }

    // 设置字体
    private fun setTabBtnSize(@IdRes viewId: Int) {
        // 获取上一个按钮的视图并设置字体大小为 16sp
        val lastView = findViewById<View>(lastBtnId)
        if (lastView is TextView) {
            lastView.textSize = 16f
        }

        // 获取当前按钮的视图并设置字体大小为 20sp
        val currentView = findViewById<View>(viewId)
        if (currentView is TextView) {
            currentView.textSize = 18f
        }

        // 更新最后一个按钮的 ID
        lastBtnId = viewId
    }


    // 新增：创建 Fragment 实例的方法（集中管理，便于维护）
    private fun createFragment(index: AppPageCode): Fragment {
        return when (index) {
            HOME -> FragmentArticles()
            CONTACT_SDK -> ContactFragment()
            CHAT_SESSION -> ChatSessionFragment()
            CHAT_SDK -> ChatManagerFragment()
            PROFILE -> FragmentMe()
            LOGIN -> LoginFragment().apply {
                setChangeWithCodeCallback { switchFragment(LOGIN_WITH_CODE) }
            }
            LOGIN_WITH_CODE -> LoginCodeFragment()
            PROFILE_SDK -> ProfileFragment()
            // 其他页面...

        }
    }

    fun switchFragment(index: AppPageCode) {
        // 1. 先从 FragmentManager 中查找是否已有该 Fragment
        var fragment = supportFragmentManager.findFragmentByTag(index.name)

        // 2. 如果没有，再从 map 中获取或创建新实例
        if (fragment == null) {
            fragment = fragmentMap[index] ?: createFragment(index) // createFragment 是创建新实例的方法
            fragmentMap[index] = fragment // 存入 map 缓存
        }

        // 3. 切换 Fragment（使用 tag 便于后续查找）
        fragment?.let {
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_right,
                    R.anim.slide_out_left,
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .replace(R.id.fragment_container, it, index.name) // 关键：设置 tag 为 AppPageCode 名称
                .addToBackStack(null)
                .commit()
        }
    }



    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }

    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){

        if (eventType == MsgEventType.APP_NOTIFY_SEND_MSG){
            // 关键修复：从 FragmentManager 中通过 Tag 查找当前活跃的 CHAT_SDK Fragment
            var chatFragment = supportFragmentManager.findFragmentByTag(CHAT_SDK.name) as? ChatManagerFragment
            if (chatFragment != null && !chatFragment.isDetached && !chatFragment.isRemoving) {
                // 确认 Fragment 未销毁、未移除，再执行操作
                chatFragment.switchToFriend(fid)
                switchFragment(CHAT_SDK)
            } else {
                // 若 Fragment 未创建或已销毁，先切换页面，再通过页面重建后的回调执行操作
                chatFragment = ChatManagerFragment()
                fragmentMap[AppPageCode.CHAT_SDK] = chatFragment
                chatFragment.switchToFriend(fid)
                switchFragment(CHAT_SDK)
                // 可选：通过 EventBus 或回调，在 ChatManagerFragment 重建后执行 switchToFriend
                // EventBus.getDefault().post(switchToFriendEvent(fid))
            }
        }
    }


}