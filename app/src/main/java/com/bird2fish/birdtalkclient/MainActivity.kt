package com.bird2fish.birdtalkclient

import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
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
    CONTACTS,
    CHAT_SESSION,
    CHAT,
    PROFILE,
    TEST,
    LOGIN,
    LOGIN_WITH_CODE,
    CONTACT_SDK,
    CHAT_SDK,
    PROFILE_SDK,
}

class MainActivity : AppCompatActivity() , StatusCallback {
    private lateinit var binding: ActivityMainBinding

    // 用map管理页面
    private val fragmentMap = mutableMapOf<AppPageCode, Fragment>()
    private  @IdRes var lastBtnId : Int = R.id.b_tab_btn_main

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

        // 初始化页面
        if (savedInstanceState == null) {
            // 初始化 Fragment 实例并存储到 Map 中
            fragmentMap[AppPageCode.HOME] = FragmentArticles()
            fragmentMap[AppPageCode.CONTACT_SDK] = ContactFragment()
            fragmentMap[AppPageCode.CHAT_SESSION] = ChatSessionFragment() // FragmentTest()
            fragmentMap[AppPageCode.CHAT_SDK] = ChatManagerFragment()
            fragmentMap[AppPageCode.PROFILE] = FragmentMe()

            //fragmentMap[AppPageCode.CONTACTS] = FragmentContact()
            //fragmentMap[AppPageCode.CHAT] = FragmentChat()
            // 当点击页面切换到验证码登录时候，这里切换页面；
            val loginPage = LoginFragment()
            loginPage.setChangeWithCodeCallback {
                switchFragment(AppPageCode.LOGIN_WITH_CODE)
            }
            fragmentMap[AppPageCode.LOGIN] = loginPage
            fragmentMap[AppPageCode.LOGIN_WITH_CODE] = LoginCodeFragment()
            fragmentMap[AppPageCode.PROFILE_SDK] = ProfileFragment()

            // 默认加载的 Fragment
            switchFragment(AppPageCode.CONTACT_SDK)
        }

        // 初始化底部工具栏 tab
        setupClickHandler<TextView>(AppPageCode.HOME, R.id.b_tab_btn_main)
        setupClickHandler<TextView>(AppPageCode.CONTACT_SDK, R.id.b_tab_btn_friends)
        setupClickHandler<TextView>(AppPageCode.CHAT_SESSION, R.id.b_tab_btn_session)
        setupClickHandler<TextView>(AppPageCode.CHAT_SDK, R.id.b_tab_btn_msg)
        setupClickHandler<TextView>(AppPageCode.PROFILE_SDK, R.id.b_tab_btn_me)

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

    // 用于切换 Fragment 的方法
    fun switchFragment(index:  AppPageCode) {
        val fragment = fragmentMap[index]
        fragment?.let { // 如果 fragment 不为 null，则执行 let 代码块
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.fragment_container, it) // 在 let 代码块中，it 代表非空的 fragment
                .addToBackStack(null)
                .commit()
        }
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
//        fragment?.onActivityResult(requestCode, resultCode, data)
//
//        // Ensure UCrop result is handled correctly
//        if (requestCode == UCrop.REQUEST_CROP ) {
//            val uCropFragment = supportFragmentManager.findFragmentById(R.id.fragment_container) as? Fragment
//            uCropFragment?.let {
//                it.onActivityResult(requestCode, resultCode, data)
//            }
//        }
//
//    }

    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }

    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){

        if (eventType == MsgEventType.APP_NOTIFY_SEND_MSG){// 切换都聊天界面
            (fragmentMap[AppPageCode.CHAT_SDK] as ChatManagerFragment).switchToFriend(fid)
            switchFragment(AppPageCode.CHAT_SDK)


            // 会话列表中应该确保有这个会话
        }
    }


}