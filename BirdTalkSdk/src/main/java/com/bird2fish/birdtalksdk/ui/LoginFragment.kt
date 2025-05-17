package com.bird2fish.birdtalksdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.net.Session
import com.bird2fish.birdtalksdk.ui.LoginFragment
import com.bird2fish.birdtalksdk.uihelper.TextHelper


typealias ButtonClickListener = () -> Unit

class LoginFragment : Fragment() {

    private lateinit var viewSwitcher: ViewSwitcher
    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var registerEmail: EditText
    private lateinit var registerPassword: EditText
    private lateinit var registerConfirmPassword: EditText

    private lateinit var loginButton: Button
    private lateinit var registerButton: Button

    private lateinit var agreeCheckBox: CheckBox



    // 要求切换到使用验证码登录的页面的回调
    private  var onChangeLoginWithCode: ButtonClickListener? = null

    // 设置回调函数的方法
    public fun setChangeWithCodeCallback(callback: ButtonClickListener) {
        this.onChangeLoginWithCode = callback
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_login, container, false)

        viewSwitcher = root.findViewById(R.id.loginViewSwitcher)
        loginEmail = root.findViewById(R.id.loginEmail)
        loginPassword = root.findViewById(R.id.loginPassword)
        registerEmail = root.findViewById(R.id.registerEmail)
        registerPassword = root.findViewById(R.id.registerPassword)
        registerConfirmPassword = root.findViewById(R.id.registerConfirmPassword)
        agreeCheckBox = root.findViewById(R.id.agreeCheckBox)

        loginButton = root.findViewById(R.id.loginButton)
        registerButton = root.findViewById(R.id.registerButton)


        agreeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            loginButton.isEnabled = isChecked
            registerButton.isEnabled = isChecked
        }

        // 切换到注册
        root.findViewById<TextView>(R.id.switchToRegister).setOnClickListener {
            // 设置进入和退出动画：右侧进入
            viewSwitcher.inAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
            viewSwitcher.outAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_left)
            viewSwitcher.showNext()
        }

        // 切换到登录
        root.findViewById<TextView>(R.id.switchToLogin).setOnClickListener {
            // 左侧进入动画
            viewSwitcher.inAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_left)
            viewSwitcher.outAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
            viewSwitcher.showPrevious()
        }

        // 点击需要验证码登录与注册
        root.findViewById<TextView>(R.id.switchToCodeLogin).setOnClickListener {
            onChangeLoginWithCode?.invoke()
        }

        loginButton.setOnClickListener {
            handleLogin()
        }

        registerButton.setOnClickListener {
            handleRegister()
        }

        return root
    }



    //  开始登录
    private fun handleLogin() {
        if (Session.sessionState.value != Session.SessionState.WAIT){
            Toast.makeText(requireActivity(), requireContext().resources.getString(R.string.network_not_ready),
                Toast.LENGTH_SHORT).show()
            return
        }

        val email = loginEmail.text.toString()
        val password = loginPassword.text.toString()
        // 添加你的登录逻辑，例如调用API
        //Toast.makeText(requireActivity(), "Logging in with $email", Toast.LENGTH_SHORT).show()

        val mod = TextHelper.checkStringType(email)
        if (mod == "invalid"){
            val info = resources.getString(R.string.user_id_error)
            Toast.makeText(requireActivity(), info, Toast.LENGTH_SHORT).show()
        }else{
            Session.startLogin(mod, email, password)
            // 禁用按钮
            loginButton.isEnabled = false
        }

    }

    // 处理注册动作
    // 无论写不写邮件，都是匿名注册
    private fun handleRegister() {
        if (Session.sessionState.value != Session.SessionState.WAIT){
            Toast.makeText(requireActivity(), requireContext().resources.getString(R.string.network_not_ready),
                Toast.LENGTH_SHORT).show()
            return
        }

        val name = registerEmail.text.toString()
        val password = registerPassword.text.toString()
        val confirmPassword = registerConfirmPassword.text.toString()

        if (password != confirmPassword) {
            Toast.makeText(requireActivity(), requireContext().resources.getString(R.string.pwd_not_same),
                Toast.LENGTH_SHORT).show()
            return
        }

        var email = ""
        val mode = TextHelper.checkStringType(name)
        if (mode == email){
            email = name
        }
        // 添加你的注册逻辑，例如调用API
        //Toast.makeText(requireActivity(), "Registering with $email", Toast.LENGTH_SHORT).show()

        Session.startRegister("id", name, email, password)
        // 禁用按钮
        registerButton.isEnabled = false


//        val info = resources.getString(R.string.user_id_error)
//        Toast.makeText(requireActivity(), info, Toast.LENGTH_SHORT).show()
    }

    // todo： 后续需要添加注册失败的监听以及登录失败的监听，保证按钮能恢复

//    companion object {
//        // TODO: Rename and change types and number of parameters
//        fun newInstance(param1: String?, param2: String?): LoginFragment {
//            val fragment = LoginFragment()
//            val args = Bundle()
//            fragment.arguments = args
//            return fragment
//        }
//    }
}