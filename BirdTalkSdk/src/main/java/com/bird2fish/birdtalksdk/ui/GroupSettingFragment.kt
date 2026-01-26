package com.bird2fish.birdtalksdk.ui

import android.app.Activity
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.airbnb.lottie.LottieAnimationView
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.model.Group
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper

class GroupSettingFragment :  DialogFragment(), StatusCallback {


    private lateinit var avatarView: ImageView
    private lateinit var nameView: EditText
    private lateinit var tagView: EditText
    private lateinit var desView: EditText
    private lateinit var radioGVisibility: RadioGroup
    private lateinit var radioGJoin: RadioGroup
    private lateinit var createButton: Button
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var joinQuestion:EditText
    private lateinit var joinAnswer:EditText
    private lateinit var joinQuestionLabel:TextView
    private lateinit var joinAnswerLabel:TextView
    private lateinit var radioQuestion: RadioButton

    private lateinit var cancelButton : TextView


    private var avatarUuid:String = ""
    private var curGroup : Group? = null

    // 设置当前的组信息
    fun setGroup(g:Group){
        this.curGroup = g
        if (g == null)
            return
        this.avatarUuid = curGroup!!.icon
    }

    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }

    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.GROUP_CREATE_FAIL){
            (context as? Activity)?.runOnUiThread {
                TextHelper.showToast(this.requireContext(), getString(R.string.group_create_fail))
                //enableControls()
            }
        }else if (eventType == MsgEventType.GROUP_CREATE_OK){
            (context as? Activity)?.runOnUiThread {
                TextHelper.showToast(
                    this.requireContext(),
                    getString(R.string.group_create_success)
                )
                this.dismiss()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_group_setting, container, false)

        avatarView = root.findViewById(R.id.ivAvatar)
        nameView = root.findViewById(R.id.etGroupName)
        tagView =  root.findViewById(R.id.etGroupTag)
        desView =  root.findViewById(R.id.etGroupDesc)
        radioGVisibility =  root.findViewById(R.id.rgGroupType)
        radioGVisibility.check(R.id.rbPublic)
        createButton = root.findViewById(R.id.btnCreateGroup)
        radioGJoin = root.findViewById(R.id.rgJoinType)
        radioGJoin.check(R.id.rbJoinDirect)
        loadingAnimation = root.findViewById(R.id.loadingAnimation)
        cancelButton = root.findViewById(R.id.btnCancel)
        joinAnswer = root.findViewById(R.id.etJoinAnswer)
        joinAnswerLabel = root.findViewById(R.id.etJoinAnswerLabel)
        joinQuestion = root.findViewById(R.id.etJoinQuestion)
        joinQuestionLabel = root.findViewById(R.id.etJoinQuestionLabel)


        radioQuestion = root.findViewById(R.id.rbJoinQuestion)


        val bitmap2 = ImagesHelper.generateDefaultAvatar(getString(R.string.create_group), 2)
        avatarView.setImageBitmap(bitmap2)

        createButton.setOnClickListener {
            //disableControls()
            //createGroup()
        }

        initDefaultValue()

        return root
    }

    // 这一段主要是为了自动填入一些信息
    private var defaultNameSet = false
    private var defaultTagSet = false
    private var defaultDesSet = false
    fun initDefaultValue(){
        nameView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // 文本变化前调用，可留空
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 文本正在变化时调用
                // s: 当前输入内容
                println("当前输入：$s")
            }

            override fun afterTextChanged(s: Editable?) {
                // 文本变化后调用
                // 可以在这里处理最终文本
                val text = s?.toString() ?: ""
                if (avatarUuid == ""){
                    if (text == ""){
                        val bitmap = ImagesHelper.generateDefaultAvatar(getString(R.string.create_group), 2)
                        avatarView.setImageBitmap(bitmap)
                    }else{
                        val bitmap = ImagesHelper.generateDefaultAvatar(text, 2)
                        avatarView.setImageBitmap(bitmap)
                    }

                }
            }
        })


        nameView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus && nameView.text.isNullOrEmpty() && !defaultNameSet) {
                nameView.setText("momo")
                // 可选：把光标移到末尾
                nameView.setSelection(nameView.text.length)
                defaultNameSet = true
            }
        }

        tagView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus && tagView.text.isNullOrEmpty() && !defaultTagSet) {
                tagView.setText("闲聊/八卦")
                // 可选：把光标移到末尾
                tagView.setSelection(tagView.text.length)
                defaultTagSet = true
            }
        }

        desView.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus && desView.text.isNullOrEmpty() && !defaultDesSet) {
                desView.setText("群主很懒，目前还没有介绍")
                // 可选：把光标移到末尾
                desView.setSelection(desView.text.length)
                defaultDesSet = true
            }
        }

        cancelButton.setOnClickListener{
            this.dismiss()
        }

        // 根据群组的信息设置
        if (curGroup != null){
            nameView.setText(curGroup!!.name)
            tagView.setText(curGroup!!.brief)
            desView.setText(curGroup!!.tags)
            val bitmap = ImagesHelper.generateDefaultAvatar(curGroup!!.name, 2)
            avatarView.setImageBitmap(bitmap)

            if (curGroup!!.visibleType == "public"){
                radioGVisibility.check(R.id.rbPublic)
            }else{
                radioGVisibility.check(R.id.rbPrivate)
            }


            joinAnswer.setText(curGroup!!.answer)
            joinQuestion.setText(curGroup!!.question)

            radioGJoin.setOnCheckedChangeListener { group, checkedId ->
                when (checkedId) {
                    R.id.rbJoinQuestion->{
                        showQuestion(View.VISIBLE)
                    }
                    else ->{
                        showQuestion(View.GONE)
                    }
                }
            }

            when (curGroup!!.joinType) {
                "direct" -> {
                    radioGJoin.check(R.id.rbJoinDirect)
                    showQuestion(View.GONE)
                }
                "auth" -> {
                    radioGJoin.check(R.id.rbJoinAuth)
                    showQuestion(View.GONE)
                }
                "invite" -> {
                    radioGJoin.check(R.id.rbJoinInvite)
                    showQuestion(View.GONE)
                }
                else -> {
                    radioGJoin.check(R.id.rbJoinQuestion)
                    showQuestion(View.VISIBLE)
                }
            }
        }

    }

    private fun showQuestion(status:Int){
        joinAnswer.visibility = status
        joinQuestion.visibility = status
        joinAnswerLabel.visibility = status
        joinQuestionLabel.visibility = status
    }

    // 全屏（小红书 / 微信里很常见）
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // 去掉 padding
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setFlags(
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            )
            // 底部弹出（可选）
            //setGravity(Gravity.BOTTOM)
        }
        //SdkGlobalData.userCallBackManager.addCallback(this)
    }


    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        // Dialog 真正被关闭
        Log.d("CreateGroupFragment", "dialog dismissed")
        //SdkGlobalData.userCallBackManager.removeCallback(this)
    }

    fun updateGroup(){
        val name = nameView.text.toString()
        val tag = tagView.text.toString()
        val des = desView.text.toString()

        val joinType :String =
            when (radioGJoin.checkedRadioButtonId){
                R.id.rbJoinDirect -> "direct"
                R.id.rbJoinInvite -> "invite"
                R.id.rbJoinAuth -> "auth"
                else -> "direct"
            }

        // 计算群的属性，是否公开
        val groupVisibility: Boolean =
            when (radioGVisibility.checkedRadioButtonId) {
                R.id.rbPublic -> true
                R.id.rbPrivate -> false
                else -> true
            }
        val tags = TextHelper.splitTags(tag)

       // MsgEncocder.sendCrateGroupMessage(name, tags, des, avatarUuid, groupVisibility, joinType)
    }

}