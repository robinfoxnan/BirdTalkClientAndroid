package com.bird2fish.birdtalkclient

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
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
    //private val fragmentMap = mutableMapOf<AppPageCode, Fragment>()
    private  @IdRes var lastBtnId : Int = R.id.b_tab_btn_main
    private  var lastPageIndex :AppPageCode = HOME


    fun createOrFindFragment(pageIndex:AppPageCode): Fragment {
        val fm: FragmentManager = supportFragmentManager

        var fragment = fm.findFragmentByTag(pageIndex.name);
        if (fragment != null){
            return fragment
        }
        when (pageIndex){

            AppPageCode.HOME -> {
                fragment = FragmentArticles()
            }
            AppPageCode.CONTACT_SDK -> {fragment =ContactFragment()}
            AppPageCode.CHAT_SESSION -> {fragment =ChatSessionFragment()}
            AppPageCode.CHAT_SDK -> {fragment = ChatManagerFragment()}
            AppPageCode.PROFILE -> {fragment = FragmentMe()}
            AppPageCode.LOGIN -> {
                fragment = LoginFragment()
                fragment.setChangeWithCodeCallback { switchFragment(AppPageCode.LOGIN_WITH_CODE) }

                }
            AppPageCode.LOGIN_WITH_CODE -> {fragment =LoginCodeFragment()}
            AppPageCode.PROFILE_SDK -> {fragment =ProfileFragment()}
        }
        //fragmentMap[pageIndex] = fragment!!

        // 初始化添加
        val ft: FragmentTransaction = fm.beginTransaction()
        ft.add(R.id.fragment_container, fragment!!, pageIndex.name); // 绑定Tag
        if (pageIndex != lastPageIndex)
        {ft.hide(fragment)}
        ft.commit()

        return fragment!!
    }

    fun switchFragment(index: AppPageCode) {

        if (index == CHAT_SDK && SdkGlobalData.currentChatFid == 0L){
            switchFragment(CONTACT_SDK)
            return
        }

        if (index == lastPageIndex){
            createOrFindFragment(index)
            return
        }


        val curFrame = createOrFindFragment(index)
        val lastFrame = createOrFindFragment(lastPageIndex)

        val fm: FragmentManager = supportFragmentManager

        // 执行切换
        val ft: FragmentTransaction = fm.beginTransaction()
        ft.show(curFrame)
        ft.hide(lastFrame)

        ft.commit()
        lastPageIndex = index


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

        // 初始化底部工具栏 tab
        setupClickHandler<TextView>(HOME, R.id.b_tab_btn_main)
        setupClickHandler<TextView>(CONTACT_SDK, R.id.b_tab_btn_friends)
        setupClickHandler<TextView>(CHAT_SESSION, R.id.b_tab_btn_session)
        setupClickHandler<TextView>(CHAT_SDK, R.id.b_tab_btn_msg)
        setupClickHandler<TextView>(PROFILE_SDK, R.id.b_tab_btn_me)

        // 关注消息
        SdkGlobalData.userCallBackManager.addCallback(this)

        lastPageIndex = HOME
        AppPageCode.values().forEach {
            code->createOrFindFragment(code)
        }

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


    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }

    fun showText(txt: String){
        this.runOnUiThread {
            Toast.makeText(this, txt, Toast.LENGTH_LONG).show()
        }
    }

    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){

        if (eventType == MsgEventType.APP_NOTIFY_SEND_MSG){
                SdkGlobalData.currentChatFid = fid
                switchFragment(CHAT_SDK)

        }else if (eventType == MsgEventType.RECONNECTING){
            if (msgType == 0){
               showText("链接超时，准备重链接")
            }else if (msgType == 1){
                showText("无法链接服务器，准备重链接")
            }else if (msgType == 2){
                showText("网络异常，准备重链接")
            }else if (msgType == 3){
                showText("服务器关闭了链接，准备重链接")
            }else {
                showText("链接异常关闭，准备重链接")
            }
        }else if (eventType == MsgEventType.CONNECTED){
            showText("服务器重连完毕")
        }
    }// end onEvent


}