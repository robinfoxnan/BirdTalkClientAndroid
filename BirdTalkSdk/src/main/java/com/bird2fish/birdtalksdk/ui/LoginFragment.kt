package com.bird2fish.birdtalksdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.ui.LoginFragment


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
    fun setChangeWithCodeCallback(callback: ButtonClickListener) {
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

    private fun handleLogin() {
        val email = loginEmail.text.toString()
        val password = loginPassword.text.toString()
        // 添加你的登录逻辑，例如调用API
        Toast.makeText(requireActivity(), "Logging in with $email", Toast.LENGTH_SHORT).show()
    }

    private fun handleRegister() {
        val email = registerEmail.text.toString()
        val password = registerPassword.text.toString()
        val confirmPassword = registerConfirmPassword.text.toString()

        if (password != confirmPassword) {
            Toast.makeText(requireActivity(), "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // 添加你的注册逻辑，例如调用API
        Toast.makeText(requireActivity(), "Registering with $email", Toast.LENGTH_SHORT).show()
    }

    companion object {
        // TODO: Rename and change types and number of parameters
        fun newInstance(param1: String?, param2: String?): LoginFragment {
            val fragment = LoginFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}