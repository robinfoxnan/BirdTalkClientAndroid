package com.bird2fish.birdtalkclient

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalkclient.databinding.ActivityMainBinding
import com.bird2fish.birdtalksdk.ui.ChatManagerFragment
import com.bird2fish.birdtalksdk.ui.ChatSessionFragment
import com.bird2fish.birdtalksdk.ui.FragmentTest
import com.bird2fish.birdtalksdk.ui.LoginCodeFragment
import com.bird2fish.birdtalksdk.ui.LoginFragment
import com.bird2fish.birdtalksdk.ui.ContactFragment
import com.bird2fish.birdtalksdk.ui.ProfileFragment
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import com.yalantis.ucrop.UCrop


enum class AppPageCode {
    HOME,
    CONTACTS,
    CHAT,
    PROFILE,
    TEST,
    LOGIN,
    LOGIN_WITH_CODE,
    CONTACT_SDK,
    CHAT_SDK,
    PROFILE_SDK,
}

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // 用map管理页面
    private val fragmentMap = mutableMapOf<AppPageCode, Fragment>()
    private  @IdRes var lastBtnId : Int = R.id.b_tab_btn_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化页面
        if (savedInstanceState == null) {
            // 初始化 Fragment 实例并存储到 Map 中
            fragmentMap[AppPageCode.HOME] = FragmentArticles()

            //fragmentMap[AppPageCode.CONTACTS] = FragmentContact()
            fragmentMap[AppPageCode.CHAT] = FragmentChat()
            fragmentMap[AppPageCode.PROFILE] = FragmentMe()
            fragmentMap[AppPageCode.TEST] = ChatSessionFragment() // FragmentTest()
            fragmentMap[AppPageCode.CONTACT_SDK] = ContactFragment()
            fragmentMap[AppPageCode.CHAT_SDK] = ChatManagerFragment()
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
        setupClickHandler<TextView>(AppPageCode.TEST, R.id.b_tab_btn_main)
        setupClickHandler<TextView>(AppPageCode.CONTACT_SDK, R.id.b_tab_btn_friends)
        setupClickHandler<TextView>(AppPageCode.CHAT_SDK, R.id.b_tab_btn_msg)
        setupClickHandler<TextView>(AppPageCode.PROFILE_SDK, R.id.b_tab_btn_me)

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


}