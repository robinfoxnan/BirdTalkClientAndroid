package com.bird2fish.birdtalkclient

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toolbar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalksdk.ui.LoginCodeFragment
import com.bird2fish.birdtalksdk.ui.LoginFragment


enum class AppLoginPageCode {
    LOGIN_PAGE,
    LOGIN_PAGE_WITH_CODE,
}
class LoinActivity : AppCompatActivity() {

    // 用map管理页面
    private val fragmentMap = mutableMapOf<AppLoginPageCode, Fragment>()
    private val viewModel: LoginViewMode by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_loin)

        // 设置状态栏颜色
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(ContextCompat.getColor(this, android.R.color.white));
        }

        // 设置状态栏文字为深色（API 23及以上支持）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }


        // 找到Toolbar
        val actionBar = supportActionBar
        supportActionBar?.hide()
        // 设置背景颜色（示例：浅灰色）
        actionBar?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#F0F0F0")))

        // 也可以使用资源文件中定义的颜色
        // actionBar?.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.your_color, theme)))


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 当点击页面切换到验证码登录时候，这里切换页面；
        val loginPage = LoginFragment()
        loginPage.setChangeWithCodeCallback {
            switchFragment(AppLoginPageCode.LOGIN_PAGE_WITH_CODE)
        }

        // 设置从验证码登录页返回登录页的回调
        val loginCodePage = LoginCodeFragment()
        loginCodePage.setChangeCallback {
            switchFragment(AppLoginPageCode.LOGIN_PAGE_WITH_CODE)
        }

        fragmentMap[AppLoginPageCode.LOGIN_PAGE] = loginPage
        fragmentMap[AppLoginPageCode.LOGIN_PAGE_WITH_CODE] = LoginCodeFragment()


        switchFragment(AppLoginPageCode.LOGIN_PAGE)

        //this.viewModel = ViewModelProvider(this).get(LoginViewMode::class.java)
        viewModel.sendMessage("init")

        // 观察LiveData，这里登录成功后需要跳转到主页面
        viewModel.message.observe(this) { message ->
            if (message == "loginok") {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }

        // 设置调佣关系
        GlobalData.loginActivity = this

        // 初始化网络连接
        GlobalData.init(applicationContext)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // 配置 ActionBar 选项
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)




    }

    // 用于切换 Fragment 的方法
    fun switchFragment(index:  AppLoginPageCode) {
        val fragment = fragmentMap[index]
        fragment?.let { // 如果 fragment 不为 null，则执行 let 代码块
            val transaction =supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                .replace(R.id.login_fragment_container, it) // 在 let 代码块中，it 代表非空的 fragment


            // 只有从登录页切换到验证码登录页时才添加到回退栈
            if (index == AppLoginPageCode.LOGIN_PAGE_WITH_CODE) {
                transaction.addToBackStack(null)
            }

            transaction.commit()
        }
    }
}