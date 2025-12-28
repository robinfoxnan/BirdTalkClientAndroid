package com.bird2fish.birdtalksdk.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import org.w3c.dom.Text


class CreateGroupFragment : DialogFragment() {

    private lateinit var avatarView:ImageView
    private lateinit var nameView: EditText
    private lateinit var tagView:EditText
    private lateinit var desView:EditText
    private lateinit var radioGVisibility: RadioGroup
    private lateinit var radioGJoin: RadioGroup

    private var avatarUuid:String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_create_group, container, false)

        avatarView = root.findViewById(R.id.ivAvatar)
        nameView = root.findViewById(R.id.etGroupName)
        tagView =  root.findViewById(R.id.etGroupTag)
        desView =  root.findViewById(R.id.etGroupDesc)
        radioGVisibility =  root.findViewById(R.id.rgGroupType)
        radioGVisibility.check(R.id.rbPublic)

        radioGJoin = root.findViewById(R.id.rgJoinType)
        radioGJoin.check(R.id.rbJoinDirect)

//        val bitmap = ImagesHelper.generateDefaultAvatar("飞鸟", 1)
//        avatarView.setImageBitmap(bitmap)

        val bitmap2 = ImagesHelper.generateDefaultAvatar("RobinFox", 2)
        avatarView.setImageBitmap(bitmap2)
        initDefaultValue()

        return root
    }

    private var defaultNameSet = false
    private var defaultTagSet = false
    private var defaultDesSet = false
    fun initDefaultValue(){

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

    }

    fun createG(){

        // 计算群的属性，是否公开
        val groupType: String =
            when (radioGVisibility.checkedRadioButtonId) {
                R.id.rbPublic -> "public"
                R.id.rbPrivate -> "private"
                else ->"public"
            }
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
    }

}