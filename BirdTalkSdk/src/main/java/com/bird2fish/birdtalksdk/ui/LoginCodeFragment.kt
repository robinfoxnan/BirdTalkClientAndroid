package com.bird2fish.birdtalksdk.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.ViewSwitcher
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.net.Session
import com.bird2fish.birdtalksdk.uihelper.TextHelper

// 这个页面没有注册选项，只有登录
// 注册成功后，不用登录，返回loginok
// 这个页面主要是不记得口令的时候可以手机验证登录
class LoginCodeFragment : Fragment() {
    // 跳转回普通页面
    private  var onChangeLoginWithCode: ButtonClickListener? = null

    private lateinit var requestCodeButton: Button   // 申请发送代码
    private lateinit var submitCodeButton: Button    // 提交验证码

    private lateinit var loginEmail: EditText
    private lateinit var loginCode: EditText

    private lateinit var backText: TextView

    private lateinit var viewSwitcher: ViewSwitcher

    private lateinit var agreeCheckBox: CheckBox     // 同意协议

    private var email = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    fun setChangeCallback(callback: ButtonClickListener) {
        this.onChangeLoginWithCode = callback
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_login_code, container, false)

        viewSwitcher = root.findViewById(R.id.verificationViewSwitcher)  // 切换器

        loginEmail = root.findViewById(R.id.emailEditText)
        loginCode = root.findViewById(R.id.verificationCodeEditText)
        backText = root.findViewById(R.id.switchToSubmitEmail)

        requestCodeButton = root.findViewById(R.id.submitEmailButton)
        submitCodeButton = root.findViewById(R.id.submitVerificationCodeButton)

        requestCodeButton.setOnClickListener {
            handleRequstCode()
        }

        submitCodeButton.setOnClickListener {
            handleSubmitCode()
        }

        // 后退到邮件的页面
        backText.setOnClickListener {
            // 左侧进入动画
            viewSwitcher.inAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_left)
            viewSwitcher.outAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_right)
            viewSwitcher.showPrevious()
        }
        return root
    }

    fun showNextPage(){
        // 设置进入和退出动画：右侧进入
        viewSwitcher.inAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right)
        viewSwitcher.outAnimation = AnimationUtils.loadAnimation(context, R.anim.slide_out_left)
        viewSwitcher.showNext()
    }
    // 发送邮件，申请验证码
    fun handleRequstCode(){
        // 检查网络状态
        if (Session.sessionState.value != Session.SessionState.WAIT){
            Toast.makeText(requireActivity(), requireContext().resources.getString(R.string.network_not_ready),
                Toast.LENGTH_SHORT).show()
            return
        }

        email = loginEmail.text.toString()

        val mod = TextHelper.checkStringType(email)
        if (mod != "email"){
            val info = resources.getString(R.string.user_id_error)
            Toast.makeText(requireActivity(), info, Toast.LENGTH_SHORT).show()
            return
        }

       // 发送登录以及注册请求
        Session.startLogin("email", email, "")

        // 跳转
        showNextPage()
    }

    // 提交验证码
    fun handleSubmitCode(){
        // 检查网络状态
        if (Session.sessionState.value != Session.SessionState.WAIT){
            Toast.makeText(requireActivity(), requireContext().resources.getString(R.string.network_not_ready),
                Toast.LENGTH_SHORT).show()
            return
        }
        val code = loginCode.text.toString()

        Session.sendLoginCode("email", email, code)
        submitCodeButton.isEnabled = false

    }

}