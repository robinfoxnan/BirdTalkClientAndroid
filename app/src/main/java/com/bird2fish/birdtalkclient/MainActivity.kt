package com.bird2fish.birdtalkclient

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalkclient.databinding.ActivityMainBinding


enum class AppPageCode {
    HOME,
    CONTACTS,
    CHAT,
    PROFILE,
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
            fragmentMap[AppPageCode.CONTACTS] = FragmentContact()
            fragmentMap[AppPageCode.CHAT] = FragmentChat()
            fragmentMap[AppPageCode.PROFILE] = FragmentMe()

            // 默认加载的 Fragment
            switchFragment(AppPageCode.HOME)
        }

        // 初始化底部工具栏 tab
        setupClickHandler<TextView>(AppPageCode.HOME, R.id.b_tab_btn_main)
        setupClickHandler<TextView>(AppPageCode.CONTACTS, R.id.b_tab_btn_friends)
        setupClickHandler<TextView>(AppPageCode.CHAT, R.id.b_tab_btn_msg)
        setupClickHandler<TextView>(AppPageCode.PROFILE, R.id.b_tab_btn_me)

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
                .replace(R.id.fragment_container, it) // 在 let 代码块中，it 代表非空的 fragment
                .addToBackStack(null)
                .commit()
        }
    }


}