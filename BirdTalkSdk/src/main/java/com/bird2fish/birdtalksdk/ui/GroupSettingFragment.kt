package com.bird2fish.birdtalksdk.ui

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.util.TypedValue
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.model.Group
import com.bird2fish.birdtalksdk.model.GroupCache
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.net.Session
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import com.bird2fish.birdtalksdk.uihelper.AvatarUploadHelper
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import com.yalantis.ucrop.UCrop
import java.io.File


class GroupSettingFragment :  DialogFragment(), StatusCallback {

    private lateinit var groupOwnerImage:ImageView
    private lateinit var groupOwnerNick:TextView
    private lateinit var groupIdLabel:TextView

    private lateinit var avatarView: ImageView
    private lateinit var nameView: EditText
    private lateinit var tagView: EditText
    private lateinit var desView: EditText
    private lateinit var radioGVisibility: RadioGroup
    private lateinit var radioGJoin: RadioGroup
    private lateinit var loadingAnimation: LottieAnimationView
    private lateinit var joinQuestion:EditText
    private lateinit var joinAnswer:EditText
    private lateinit var joinQuestionLabel:TextView
    private lateinit var joinAnswerLabel:TextView
    private lateinit var radioQuestion: RadioButton

    private lateinit var cancelButton : Button
    private lateinit var saveButton :Button

    private lateinit var membersView:RecyclerView
    private lateinit var adminsView:RecyclerView


    private lateinit var avatarUpload:AvatarUploadHelper
    private var photoUri: Uri? = null
    private var localUploadName : String? = ""

    private var avatarUuid:String = ""
    private var curGroup : Group? = null


    // 🔥 核心：静态工厂方法（替代自定义构造函数）
    companion object {
        // 定义参数Key（建议用类名+字段名，避免冲突）
        private const val ARG_GROUP = "arg_group"

        // 静态方法：创建Fragment实例并传入group
        fun newInstance(gid: Long): GroupSettingFragment {
            val fragment = GroupSettingFragment()
            val args = Bundle()
            args.putLong("gid", gid) // 🔥 关键：Long类型用putLong
            fragment.arguments = args
            return fragment
        }
    }

    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }

    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.GROUP_UPDATE_INFO_FAIL){
            (context as? Activity)?.runOnUiThread {
                TextHelper.showToast(this.requireContext(), getString(R.string.group_update_fail))
                //enableControls()
            }
        }else if (eventType == MsgEventType.GROUP_UPDATE_INFO_OK){
            (context as? Activity)?.runOnUiThread {
                TextHelper.showToast(
                    this.requireContext(),
                    getString(R.string.group_update_success)
                )
                this.dismiss()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 解析Long类型的gid参数（非空/合法性校验）
        arguments?.let {
            val gid = it.getLong("gid", 0L) // 第二个参数是默认值
            if (gid == 0L)
            {
                dismiss() // 关闭弹窗
                throw IllegalArgumentException("必须通过newInstance传入有效的gid")
            }
            this.curGroup = GroupCache.findGroupSync(gid)
            this.avatarUuid = curGroup!!.icon
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val root = inflater.inflate(R.layout.fragment_group_setting, container, false)

        groupIdLabel = root.findViewById(R.id.tvGroupId)
        groupOwnerImage = root.findViewById(R.id.ivOwnerIcon)
        groupOwnerNick = root.findViewById(R.id.tvOwnerNick)
        avatarView = root.findViewById(R.id.ivAvatar)
        nameView = root.findViewById(R.id.etGroupName)
        tagView =  root.findViewById(R.id.etGroupTag)
        desView =  root.findViewById(R.id.etGroupDesc)
        radioGVisibility =  root.findViewById(R.id.rgGroupType)
        radioGVisibility.check(R.id.rbPublic)

        radioGJoin = root.findViewById(R.id.rgJoinType)
        radioGJoin.check(R.id.rbJoinDirect)
        loadingAnimation = root.findViewById(R.id.loadingAnimation)
        cancelButton = root.findViewById(R.id.btnCancel)
        saveButton = root.findViewById(R.id.btnSaveGroup)

        joinAnswer = root.findViewById(R.id.etJoinAnswer)
        joinAnswerLabel = root.findViewById(R.id.etJoinAnswerLabel)
        joinQuestion = root.findViewById(R.id.etJoinQuestion)
        joinQuestionLabel = root.findViewById(R.id.etJoinQuestionLabel)
        radioQuestion = root.findViewById(R.id.rbJoinQuestion)

        membersView = root.findViewById(R.id.rvMembers)
        adminsView = root.findViewById(R.id.rvAdmins)

        // 上传头像的控件
        avatarUpload = AvatarUploadHelper(this)
        avatarUpload.initHelper(avatarView)
        // 成功回调（省略参数类型）
        avatarUpload.onUploadOk = { localUri, localName, uuidName ->
            this.photoUri = localUri
            this.localUploadName = localName
            this.avatarUuid = uuidName
            // 设置
            saveGroupImage(this.avatarUuid)
        }

        // 失败回调（省略参数类型）
        avatarUpload.onUploadErr = { localUri, localName, uuidName ->
            TextHelper.showToast(this.requireContext(), "upload image error")
        }


        this.avatarUuid = curGroup!!.icon

        if (curGroup != null){
            if (curGroup!!.owner!= null){
                groupOwnerNick.setText(curGroup!!.owner!!.nick)
                AvatarHelper.tryLoadAvatar(requireContext(), curGroup!!.owner!!.icon, groupOwnerImage, curGroup!!.owner!!.gender, curGroup!!.owner!!.nick)
            }
        }


        saveButton.setOnClickListener {
            //disableControls()
            //createGroup()
        }

        initDefaultValue()
        loadMembers()
        loadAdmins()

        disableEdit()
        return root
    }


    // 保存头像信息
    fun saveGroupImage(uuid:String){
        MsgEncocder.sendSetGroupIcon(this.curGroup!!.gid, uuid)
    }

    fun saveGroupInfo(){
        MsgEncocder.sendSetGroupInfo(this.curGroup!!)
    }


    // 如果不是管理员，则禁用保存
    fun disableEdit(){
        if (this.curGroup == null)
            return
        // 管理员才可以设置
        if (this.curGroup!!.isAdmin(SdkGlobalData.selfUserinfo.id)){

            // 点击头像，
            avatarView.setOnClickListener{
                avatarUpload.openGallery()
            }

            return
        }

        // 1）禁用保存
        saveButton?.visibility = View.GONE

    }

    fun loadAdmins(){
        if (this.curGroup == null){
            return
        }
        val lst = this.curGroup!!.getAdmins()
        val adapter = UserAdapter(lst)
        // 4. 设置点击事件
        adapter.onItemClick = { position, user ->
            // 处理Item点击（如跳转详情页）
            println("点击了第$position 项，用户名：${user.nick}")
        }

        adapter.onRemoveClick = { position, user ->
            // 处理移除按钮点击（如删除Item）
            println("移除第$position 项，用户ID：${user.id}")
            adapter.removeItem(position)
        }
        // 第三步：给listview设置适配器（view）
        // 替换原来的 LinearLayoutManager 为 GridLayoutManager
        adminsView?.layoutManager = GridLayoutManager(context, 5) // 第二个参数5表示每行显示5列

       // 可选：如果需要Item宽高一致（正方形格子），可添加ItemDecoration调整间距
        val spacingInDp = 4 // 格子间距（单位：dp）
        val spacingInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            spacingInDp.toFloat(),
            resources.displayMetrics
        ).toInt()
        //adminsView?.addItemDecoration(GridSpacingItemDecoration(5, spacingInPx, true))
        adminsView?.setAdapter(adapter);
        showViewLines(adminsView, 1, 5)

    }

    /**
     * dp转px，适配不同屏幕
     * @param dp 要转换的dp值
     * @return 转换后的px值
     */
    private fun dp2px(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density + 0.5f).toInt()
    }

    // 基础配置（可根据你的设计稿修改）
    private val ITEM_HEIGHT_DP = 70    // 单个网格Item的高度（dp），对应你之前的头像+名称布局
    private val GRID_SPACING_DP = 4     // 网格Item之间的间距（dp），和之前的GridSpacingItemDecoration一致


    private fun showViewLines(view:RecyclerView, nRow:Int, nCol:Int){
        // 1. 初始化网格布局（5列）
        view?.layoutManager = GridLayoutManager(context, nCol)
        // 2. （可选）添加网格间距（和之前的装饰器一致，保证高度计算准确）
        val spacingPx = dp2px(GRID_SPACING_DP)
       // view  ?.addItemDecoration(GridSpacingItemDecoration(nCol, spacingPx, true))

        // 3. 计算3行网格的总高度（核心公式）
        val itemHeightPx = dp2px(ITEM_HEIGHT_DP)
        val totalHeightPx = itemHeightPx * nRow + spacingPx * (nRow + 1)
        // 公式说明：总高度 = 单Item高度×行数 + 间距×(行数+1)（间距包含上下边缘）

        // 4. 设置RecyclerView固定高度
        view?.layoutParams = adminsView?.layoutParams?.apply {
            height = totalHeightPx // 固定高度为3行的总高度
        }
    }

    fun loadMembers(){
        if (this.curGroup == null){
            return
        }
        val lst = this.curGroup!!.getMembers()
        val adapter = UserAdapter(lst)
        // 4. 设置点击事件
        adapter.onItemClick = { position, user ->
            // 处理Item点击（如跳转详情页）
            println("点击了第$position 项，用户名：${user.nick}")
        }

        adapter.onRemoveClick = { position, user ->
            // 处理移除按钮点击（如删除Item）
            println("移除第$position 项，用户ID：${user.id}")
            adapter.removeItem(position)
        }
        // 第三步：给listview设置适配器（view）
//        membersView?.layoutManager = LinearLayoutManager(context)
        // 替换原来的 LinearLayoutManager 为 GridLayoutManager
        membersView?.layoutManager = GridLayoutManager(context, 5) // 第二个参数5表示每行显示5列

// 可选：如果需要Item宽高一致（正方形格子），可添加ItemDecoration调整间距
        val spacingInDp = 4 // 格子间距（单位：dp）
        val spacingInPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            spacingInDp.toFloat(),
            resources.displayMetrics
        ).toInt()
       // membersView?.addItemDecoration(GridSpacingItemDecoration(5, spacingInPx, true))
        membersView?.setAdapter(adapter);
        if (lst.size >= 15){
            showViewLines(membersView, 3, 5)
        }else if (lst.size >= 10){
            showViewLines(membersView, 2, 5)
        }else{
            showViewLines(membersView, 1, 5)
        }

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
            groupIdLabel.setText(curGroup!!.gid.toString())
            nameView.setText(curGroup!!.name)
            tagView.setText(curGroup!!.brief)
            desView.setText(curGroup!!.tags)
            if (TextUtils.isEmpty(curGroup!!.icon))
            {
                val bitmap = ImagesHelper.generateDefaultAvatar(curGroup!!.name, 2)
                avatarView.setImageBitmap(bitmap)
            }else{
                AvatarHelper.tryLoadAvatar(requireContext(), this.curGroup!!.icon, avatarView,"male", curGroup!!.name)
            }


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
//        SdkGlobalData.invokeOnEventCallbacks(
//            MsgEventType.GROUP_UPDATE_INFO_OK, 0, 0, curGroup!!.gid,
//            mapOf("group" to  curGroup!!.name))
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

/**
 * RecyclerView Adapter 适配 Map<Long, User> 数据源
 * 绑定你提供的布局（包含ivAvatar/ivAdd/ivRemove/tvName）
 */
class UserAdapter(private var userList: MutableList<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    // 点击事件回调（Kotlin用lambda更简洁）
    var onItemClick: ((position: Int, user: User) -> Unit)? = null
    var onRemoveClick: ((position: Int, user: User) -> Unit)? = null

    // 自定义ViewHolder（绑定布局控件）
    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivAvatar: ImageView = itemView.findViewById(R.id.ivAvatar)
        val ivAdd: ImageView = itemView.findViewById(R.id.ivAdd)
        val ivRemove: ImageView = itemView.findViewById(R.id.ivRemove)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        // 加载你的布局文件（替换为实际布局名，如R.layout.item_user）
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_setting_member, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = userList[position] ?: return

        // 1. 绑定用户名
        holder.tvName.text = user.nick

        // 2. 绑定头像（两种方式可选）
        AvatarHelper.tryLoadAvatar(SdkGlobalData.context!!, user.icon, holder.ivAvatar,  user.gender, user.nick)

        // 3. 控制ivAdd显示/隐藏（默认隐藏，可根据业务调整）
        holder.ivAdd.visibility = View.GONE
        holder.ivRemove.visibility = View.GONE

        // 4. 绑定ivRemove点击事件
        holder.ivRemove.setOnClickListener {
            onRemoveClick?.invoke(position, user)
        }

        // 5. 绑定整个Item的点击事件
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(position, user)
        }
    }

    override fun getItemCount(): Int = userList.size

    /**
     * 更新数据源（Map变化时调用）
     */
    fun updateData(newUserList: List<User>) {

        this.userList.clear()
        this.userList.addAll(newUserList)
        notifyDataSetChanged()
    }

    /**
     * 移除指定位置的Item（可选）
     */
    fun removeItem(position: Int) {
        if (position in 0 until userList.size) {
            val removedUser = userList.removeAt(position)
            // 同步更新原Map（若需要）
//            userMap = userMap.filterKeys { it != removedUser.userId }
//            notifyItemRemoved(position)
//            notifyItemRangeChanged(position, userList.size)
        }
    }
}

/**
 * 网格布局间距装饰器
 * @param spanCount 列数
 * @param spacing 间距（px）
 * @param includeEdge 是否包含边缘
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view) // item position
        val column = position % spanCount // item column

        if (includeEdge) {
            // 包含边缘：左右间距均分
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            // 第一行添加顶部间距
            if (position < spanCount) {
                outRect.top = spacing
            }
            outRect.bottom = spacing // 所有Item添加底部间距
        } else {
            // 不包含边缘：左右间距仅中间有
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) {
                outRect.top = spacing // 非第一行添加顶部间距
            }
        }
    }
}