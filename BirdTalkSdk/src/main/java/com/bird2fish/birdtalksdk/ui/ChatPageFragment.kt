package com.bird2fish.birdtalksdk.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat
import androidx.core.view.ContentInfoCompat
import androidx.core.view.OnReceiveContentListener
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.model.ChatSession
import com.bird2fish.birdtalksdk.model.ChatSessionManager
import com.bird2fish.birdtalksdk.model.Drafty
import com.bird2fish.birdtalksdk.model.MessageContent
import com.bird2fish.birdtalksdk.model.MessageStatus
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.model.UserStatus
import com.bird2fish.birdtalksdk.pbmodel.User.GroupInfo
import com.bird2fish.birdtalksdk.uihelper.AvatarHelper
import com.bird2fish.birdtalksdk.uihelper.PermissionsHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import com.bird2fish.birdtalksdk.widgets.AudioSampler
import com.bird2fish.birdtalksdk.widgets.MovableActionButton
import com.bird2fish.birdtalksdk.widgets.WaveDrawable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.util.LinkedList

class ChatPageFragment : Fragment() , StatusCallback {

    private var root :View? =null
    private var parentView :ChatManagerFragment? = null
    private val SUPPORTED_MIME_TYPES: Array<String> = arrayOf("image/*")
    // Number of milliseconds between audio samples for recording visualization.
    private val AUDIO_SAMPLING: Int = 100
    // Minimum duration of an audio recording in milliseconds.
    private val MIN_DURATION: Int = 1000
    // Maximum duration of an audio recording in milliseconds.
    private val MAX_DURATION: Int = 30000


    private val ZONE_CANCEL: Int = 0
    private val ZONE_LOCK: Int = 1

    private lateinit var permissionsHelper: PermissionsHelper



    private var mMessageViewLayoutManager: LinearLayoutManager? = null
    private var mRecyclerView: RecyclerView? = null
    private var mMessagesAdapter: ChatPageAdapter? = null
    private var mRefresher: SwipeRefreshLayout? = null
    //private var mFailureListener: PromisedReply.FailureListener<ServerMessage>? = null

    private var mGoToLatest: FloatingActionButton? = null

    private lateinit var chatId: String
    private var mChatIdLong :Long = 0L
    private var mPeerFriend :User? = null
    private var mPeerGroup : User? = null  //TODO:

    private val mTopicName: String? = null
    private val mMessageToSend: String? = null
    private val mChatInvitationShown = false

    private val mCurrentPhotoFile: String? = null
    private val mCurrentPhotoUri: Uri? = null

    private var mReplySeqID = -1
//    private val mReply: Drafty? = null
    private var mContentToForward: MessageContent? = null
    private var mForwardSender: MessageContent? = null

    private var mAudioRecorder: MediaRecorder? = null // 录音类，系统提供的
    private var mAudioRecord: File? = null     // 录音的文件

    // Timestamp when the recording was started.
    private var mRecordingStarted: Long = 0

    // Duration of audio recording.
    private var mAudioRecordDuration = 0
    private var mEchoCanceler: AcousticEchoCanceler? = null
    private var mNoiseSuppressor: NoiseSuppressor? = null
    private var mGainControl: AutomaticGainControl? = null

    // Playback of audio recording.
    private var mAudioPlayer: MediaPlayer? = null

    // Preview or audio amplitudes.
    private var mAudioSampler: AudioSampler? = null

    private val mAudioSamplingHandler = Handler(Looper.getMainLooper())

    private var mVisibleSendPanel = R.id.sendMessagePanel

    private var mReply : MessageContent? = null

    private var chatSession: ChatSession? = null
    private var isScrollByCode :Boolean = false


    // 临时的一个消息列表
    //var dataList = LinkedList<MessageContent>()

    // 通知界面事件
    override fun onError(code : InterErrorType, lastAction:String, errType:String, detail:String){

    }

    // 上传或下载事件
    // 这里是回调函数，无法操作界面
    override fun onEvent(eventType: MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>){
        if (eventType == MsgEventType.MSG_UPLOAD_OK){


        }
        else if (eventType == MsgEventType.MSG_UPLOAD_FAIL){
            TextHelper.showDialogInCallback(this.requireContext(), "上传头像失败")
        }
        // 或者回执来了
        else if ( eventType == MsgEventType.MSG_SEND_OK
            || eventType == MsgEventType.MSG_RECV_OK || eventType == MsgEventType.MSG_READ_OK) {
            refreshData(true)

        }
        // 新消息来了,
        else if (eventType == MsgEventType.MSG_COMING){
            refreshData(true)
        }
        // 历史数据
        else if (eventType == MsgEventType.MSG_HISTORY){
            refreshData(false)
            onLoadMessageOk()
            // 这里不滚动
        }

        // 上传文件结束了
        else if (eventType == MsgEventType.MSG_UPLOAD_OK){
            refreshData(true)
        }

        // 下载文件
        else if (eventType == MsgEventType.MSG_DOWNLOAD_OK ||
            eventType == MsgEventType.MSG_DOWNLOAD_PROCESS ||
            eventType == MsgEventType.MSG_DOWNLOAD_FAIL){
            refreshData(true)
        }

        else if (eventType == MsgEventType.MSG_UPLOAD_PROCESS ||
            eventType == MsgEventType.MSG_UPLOAD_OK ||
            eventType == MsgEventType.MSG_UPLOAD_FAIL){
            refreshData(true)
        }

        else if (eventType == MsgEventType.MSG_SEND_ERROR){
            val detail = params["detail"]
            if (fid == this.mChatIdLong){
                if (detail == "not friend")
                {
                    val info = getString(R.string.not_friend)
                    showMesage(info)
                }
            }
            refreshData(true)
        }

    }

    fun showMesage(str: String){
        (context as? Activity)?.runOnUiThread{
            TextHelper.showToast(requireContext(), str)
        }
    }
    // 需要在界面线程中处理
    fun refreshData(bEnd:Boolean){
        (context as? Activity)?.runOnUiThread {
            view?.post {
                if ( mMessagesAdapter!= null) {
                    mMessagesAdapter!!.notifyDataSetChanged()
                    // 数据更新
                    //mMessagesAdapter?.notifyItemChanged(index)
                    isScrollByCode = true
                    mRecyclerView!!.post {
                        if (!bEnd){
                            scrollToTop(true)
                        }else{
                            scrollToBottom(true)
                            }

                    }
                }
            }
        }

    }// end of fun

    // 申请录音权限
    private val audioRecorderPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            for ((permission, isGranted) in result) {
                if (!isGranted) {
                    // 有权限未被授予，禁用音频录制按钮。
                    val activity = requireActivity()
                    if (activity.isFinishing || activity.isDestroyed) {
                        return@registerForActivityResult
                    }
                    activity.findViewById<View>(R.id.audioRecorder).isEnabled = false
                    return@registerForActivityResult
                }
            }
        }

    // 图片权限的申请
    private val mImagePickerRequestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            for ((permission, granted) in result) {
                // Check if all required permissions are granted.
                if (!granted) {
                    return@registerForActivityResult
                }
            }
            // 重新打开图片浏览器
            openImageSelector(requireActivity())
        }

    // 文件浏览选择的权限申请，成功则重新
    private val mFileOpenerRequestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            // Check if permission is granted.
            if (isGranted) {
                openFileSelector(requireActivity())
            }
        }


    // 图片浏览器
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
               // 结果


                sendLoadImage(this.requireContext(), uri)
                //TextHelper.showToast(requireContext(), uri.toString())
            }
        }
    }

    // 文件浏览器
    private val mFilePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { content ->
            if (content == null) {
                return@registerForActivityResult
            }

            val activity = requireActivity() as? AppCompatActivity
            if (activity == null || activity.isFinishing || activity.isDestroyed) {
                return@registerForActivityResult
            }

            sendFile(content)
        }



    companion object {
        private const val ARG_CHAT_ID = "chat_id"

        fun newInstance(chatId: String, p: ChatManagerFragment): ChatPageFragment {
            val fragment = ChatPageFragment()
            fragment.setParent(p)
            fragment.setChatId(chatId.toLong())
            val args = Bundle()
            args.putString(ARG_CHAT_ID, chatId)
            fragment.arguments = args
            return fragment
        }
    }


    // 外部调用：设置RecyclerView的滚动监听，用于检测可见项
    fun setupScrollListener(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)

                // ✅ 当滚动停止时，标记可见消息为已读
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    // 手动滚动才能设置这个
                    if (false == isScrollByCode) {
                        markVisibleItemsAsRead(recyclerView)
                    }
                    isScrollByCode = false
                }

                // ✅ 检查是否需要加载更多（可按需启用）
                val adapter = mRecyclerView?.adapter ?: return
                val itemCount = adapter.itemCount
                val pos = mMessageViewLayoutManager?.findLastVisibleItemPosition()

            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                // ✅ 避免由代码触发的滚动重复处理
//                if (!isScrollByCode) {
//                    super.onScrolled(recyclerView, dx, dy)
//                    // 若想实时标记已读可在此处调用 markVisibleItemsAsRead(recyclerView)
//                }
//
//                isScrollByCode = false
            }
        })
    }


    // 标记可见条目为已读
    private fun markVisibleItemsAsRead(recyclerView: RecyclerView) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
            ?: return  // 仅处理LinearLayoutManager（其他布局管理器类似）

        val linearManager = layoutManager as LinearLayoutManager
        // 获取可见范围（这里用“完全可见”作为判断，根据需求调整）
        val firstVisible = linearManager.findFirstCompletelyVisibleItemPosition()
        val lastVisible = linearManager.findLastCompletelyVisibleItemPosition()

        //val txt = "滚动的位置：" + firstVisible.toString() + " to " + lastVisible.toString()
        //TextHelper.showToast(requireContext(), txt)
        ChatSessionManager.markSessionReadItems(this.mChatIdLong, firstVisible, lastVisible)

        if (lastVisible == mMessagesAdapter!!.itemCount - 1){
            mGoToLatest?.hide()
        }else{
            mGoToLatest?.show()
        }

    }


    // 设置聊天的对方是谁，如果是正数就是私聊，如果是负数就是群组号码
    fun setParent(p:ChatManagerFragment){
        this.parentView = p
    }

    fun setChatId(id:Long){
        this.mChatIdLong = id
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取 arguments，注意这一步
        chatId = arguments?.getString(ARG_CHAT_ID) ?: ""  // 安全获取

    }


    // 代码触发滚动到最后一项
    private fun scrollToBottom(smooth: Boolean) {
        isScrollByCode = true
        if (mMessagesAdapter != null && mMessagesAdapter!!.itemCount > 0)
        {
            val pos = mMessagesAdapter!!.itemCount -1
            if (smooth) {
                mRecyclerView!!.smoothScrollToPosition(pos)
            } else {
                mRecyclerView!!.scrollToPosition(pos)
            }
        }

    }
    private fun scrollToTop(smooth: Boolean) {
        isScrollByCode = true
        if (mMessagesAdapter != null && mMessagesAdapter!!.itemCount > 0)
        {
            val pos = 0
            if (smooth) {
                mRecyclerView!!.smoothScrollToPosition(pos)
            } else {
                mRecyclerView!!.scrollToPosition(pos)
            }
        }

    }


    // 初始化
    fun testInitData(){

//        val msg = MessageContent(1, 1001, 1001,"飞鸟", "sys:3", UserStatus.ONLINE, MessageStatus.OK, true,
//            true, true, "昨天你去哪里了呢？", null)
//        chatSession?.addMessageToTail(msg)
//
//        val msg1 = MessageContent(2, 1002, 1002,"我", "sys:4", UserStatus.ONLINE, MessageStatus.SENDING, false,
//            false, false, "发送中，服务器还没有回执", null)
//        chatSession?.addMessageToTail(msg1)
//
//        val msg2 = MessageContent(3, 1002, "我", "sys:4", UserStatus.ONLINE, MessageStatus.OK, false,
//            false, false, "服务器给回执了", null)
//        chatSession?.addMessageToTail(msg2)
//
//        val msg3 = MessageContent(4, 1002, "我", "sys:4", UserStatus.OFFLINE, MessageStatus.OK, false,
//            false, true, "用户接收回执", null)
//        chatSession?.addMessageToTail(msg3)
//
//        val msg4 = MessageContent(5, 1002, "我", "sys:4", UserStatus.OFFLINE, MessageStatus.OK, false,
//            true, true, "用户阅读回执", null)
//        chatSession?.addMessageToTail(msg4)
//
//        val msg5 = MessageContent(6, 1002, "我", "sys:4", UserStatus.OFFLINE, MessageStatus.FAIL, false,
//            false, false, "发送失败的", null)
//        chatSession?.addMessageToTail(msg5)
    }

    // 手动刷新的时候
    fun setupRefresher(){
        // 初始化下拉刷新逻辑（加载更多旧消息）
        mRefresher?.apply {
            // 开启刷新功能
            isEnabled = true

            setOnRefreshListener {
                val firstVisible = mMessageViewLayoutManager?.findFirstVisibleItemPosition() ?: return@setOnRefreshListener
                Log.d("Swipe", "onRefresh triggered") // ← 测试是否执行
                Log.d("Swipe", "firstVisible=$firstVisible")

                // 仅当滑到顶部时才触发加载
                loadMoreOldMessages()
            }

            // 优化体验：防止滑动中误触下拉
            setOnChildScrollUpCallback { _, _ ->
                val firstVisible = mMessageViewLayoutManager?.findFirstVisibleItemPosition() ?: 0
                // ✅ 仅当在顶部时才允许 SwipeRefreshLayout 接管下拉
                firstVisible > 0
            }
        }

    }

    private fun loadMoreOldMessages() {
        var lastMsg :MessageContent? = null
        if (mMessagesAdapter!!.itemCount > 0){
            lastMsg = mMessagesAdapter!!.getFirst()
        }
        ChatSessionManager.onLoadHistoryMessage(this.mChatIdLong, lastMsg)
        // 模拟网络加载
        Handler(Looper.getMainLooper()).postDelayed({
            // 设置一个定时器，如果5秒都无法加载完成，也不能一直加载
            mRefresher?.isRefreshing = false

        }, 5000)
    }

    // 加载数据结束了
    fun onLoadMessageOk(){
        mRefresher?.isRefreshing = false

        // 记录当前第一个 item 的位置和高度（用于加载后不跳动）
        val firstPos = mMessageViewLayoutManager?.findFirstVisibleItemPosition() ?: 0
        val firstView = mRecyclerView?.getChildAt(0)
        val offset = firstView?.top ?: 0

        // 加载旧消息
        // (mRecyclerView?.adapter as? MessagesAdapter)?.loadPreviousPage()

        // 恢复原位置（不让界面跳动）
        //mMessageViewLayoutManager?.scrollToPositionWithOffset(firstPos + 新增消息数量, offset)
        //scrollToTop(true)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_page, container, false)
        this.root = view

        permissionsHelper = PermissionsHelper(requireActivity())
        // 一键滚动到底部的按钮
        mGoToLatest = view.findViewById(R.id.goToLatest)
        mGoToLatest?.setOnClickListener(View.OnClickListener { v: View? ->
            scrollToBottom(
                true
            )
        })

        mRefresher = view.findViewById(R.id.swipe_refresher)

        mMessageViewLayoutManager = object : LinearLayoutManager(activity) {
            override fun onLayoutChildren(recycler: Recycler, state: RecyclerView.State) {
                // This is a hack for IndexOutOfBoundsException:
                //  Inconsistency detected. Invalid view holder adapter positionViewHolder
                // It happens when two uploads are started at the same time.
                // See discussion here:
                // https://stackoverflow.com/questions/31759171/recyclerview-and-java-lang-indexoutofboundsexception-inconsistency-detected-in
                try {
                    super.onLayoutChildren(recycler, state)
                } catch (e: IndexOutOfBoundsException) {
                    Log.i(
                        "",
                        "meet a IOOBE in RecyclerView",
                        e
                    )
                }
            }
        }
        mMessageViewLayoutManager?.setReverseLayout(true)
        ////
        if (mChatIdLong == 0L){
            mChatIdLong = SdkGlobalData.currentChatFid
        }
        chatSession = ChatSessionManager.getSession(mChatIdLong)
        //testInitData()

        // 建立数据列表控件
        mRecyclerView = view.findViewById(R.id.messages_container)

        mMessagesAdapter = ChatPageAdapter(chatSession!!.msgList)
        mMessagesAdapter!!.setView(this)
        mMessagesAdapter!!.setSessionId(this.mChatIdLong)
        // 第三步：给listview设置适配器（view）

        mRecyclerView?.layoutManager = LinearLayoutManager(context)
        mRecyclerView?.setAdapter(mMessagesAdapter);
        setupScrollListener(mRecyclerView!!)
        // 刷新动作

        setupRefresher()


        val gestureDetector = GestureDetector(mRecyclerView!!.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapUp(e: MotionEvent): Boolean {
                Log.d("TouchEvent", "SingleTapUp detected")
                return false
            }

            override fun onDown(e: MotionEvent): Boolean {
                Log.d("TouchEvent", "onDown detected")
                return false
            }

            override fun onLongPress(e: MotionEvent) {
                Log.d("TouchEvent", "LongPress detected")
                Thread.dumpStack()
            }
        })
        gestureDetector.setIsLongpressEnabled(false)

        mRecyclerView!!.requestDisallowInterceptTouchEvent(true)
        mRecyclerView!!.parent?.requestDisallowInterceptTouchEvent(true)
        mRecyclerView!!.parent?.parent?.requestDisallowInterceptTouchEvent(true)
        // 新添加的
        mRecyclerView!!.addOnItemTouchListener(object : RecyclerView.OnItemTouchListener {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // 禁止父视图拦截触摸事件
                //rv.requestDisallowInterceptTouchEvent(true)
                 //rv.parent?.requestDisallowInterceptTouchEvent(true)

                when (e?.action) {
                    MotionEvent.ACTION_DOWN -> Log.d("TouchEvent", "ACTION_DOWN1")
                    MotionEvent.ACTION_UP -> Log.d("TouchEvent", "ACTION_UP1")
                    MotionEvent.ACTION_MOVE -> Log.d("TouchEvent", "ACTION_MOVE1")
                    MotionEvent.ACTION_CANCEL -> Log.d("TouchEven", "ACTION_CANCEL1")
                }
                return gestureDetector.onTouchEvent(e)

            }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                //rv.requestDisallowInterceptTouchEvent(true)
                //rv.parent?.requestDisallowInterceptTouchEvent(true)
                when (e?.action) {
                    MotionEvent.ACTION_DOWN -> Log.d("TouchEvent", "ACTION_DOWN2")
                    MotionEvent.ACTION_UP -> Log.d("TouchEvent", "ACTION_UP2")
                    MotionEvent.ACTION_MOVE -> Log.d("TouchEvent", "ACTION_MOVE2")
                    MotionEvent.ACTION_CANCEL -> Log.d("TouchEvent", "ACTION_CANCEL2")
                }
            }

            override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
        })


        // 麦克的音频的按钮启动后的面板
        val audio: AppCompatImageButton = setupAudioForms(activity as AppCompatActivity, view)

        // 发送消息的按钮
        val send = view.findViewById<AppCompatImageButton>(R.id.chatSendButton)
        send.setOnClickListener { v: View? ->
            sendText(
                activity
            )
        }

        // 转发面板中有个转发的按钮
        view.findViewById<View>(R.id.chatForwardButton).setOnClickListener { v: View? ->
            sendText(
                activity
            )
        }


        // 发送图片的按钮点击后：先浏览图片
        view.findViewById<View>(R.id.attachImage).setOnClickListener { v: View? ->
            openImageSelector(
                activity
            )
        }

        // 浏览选择发送文件
        view.findViewById<View>(R.id.attachFile).setOnClickListener { v: View? ->
            openFileSelector(
                activity
            )
        }


        // 取消回复预览按钮的点击
        view.findViewById<View>(R.id.cancelPreview).setOnClickListener { v: View? ->
            cancelPreview(
                activity
            )
        }

        // 取消转发预览
        view.findViewById<View>(R.id.cancelForwardingPreview).setOnClickListener { v: View? ->
            cancelPreview(
                activity
            )
        }

        // 设置文本框的接收内容控制
        val editor = view.findViewById<EditText>(R.id.editMessage)
        ViewCompat.setOnReceiveContentListener(
            editor,
            SUPPORTED_MIME_TYPES,
            StickerReceiver()
        )


        // 当编辑器被激活的时候，切换按钮
        editor.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
            }

            override fun onTextChanged(
                charSequence: CharSequence,
                start: Int,
                before: Int,
                count: Int
            ) {
                if (count > 0 || before > 0) {
                    //activity.sendKeyPress()
                }

                //切换按钮 [send] or [record audio] button.
                if (charSequence.length > 0) {
                    audio.visibility = View.INVISIBLE
                    send.visibility = View.VISIBLE
                } else {
                    audio.visibility = View.VISIBLE
                    send.visibility = View.INVISIBLE
                }
            }

            override fun afterTextChanged(editable: Editable) {
            }
        })

        setShowHide(true)

        scrollToBottom(true)
        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        setShowHide(false)
    }

    // 这里是发送对方正在输入的消息
    fun sendKeyPress() {
//        if (mTopic != null && mSendTypingNotifications) {
//            mTopic.noteKeyPress()
//        }
    }

    // 最下面的输入面板是根据状态来切换的
    // 切换发送面板为可见状态，需要根据当前是发送图片，文字，音频
    private fun setSendPanelVisible(activity: Activity, id: Int) {
        if (mVisibleSendPanel == id) {
            return
        }
        activity.findViewById<View>(id).visibility = View.VISIBLE
        activity.findViewById<View>(mVisibleSendPanel).visibility = View.GONE
        mVisibleSendPanel = id
    }

    // 音频相关操作的面板，这里执行各种设置
    @SuppressLint("ClickableViewAccessibility")
    private fun setupAudioForms(activity: AppCompatActivity, view: View): AppCompatImageButton {
        // 真正用于控制录音的浮动按钮
        val mab: MovableActionButton = view.findViewById(R.id.audioRecorder)
        // 浮动锁定
        val lockFab = view.findViewById<ImageView>(R.id.lockAudioRecording)
        // 浮动删除
        val deleteFab = view.findViewById<ImageView>(R.id.deleteAudioRecording)

        // 锁定的面板上的播放按钮
        val playButton = view.findViewById<AppCompatImageButton>(R.id.playRecording)
        // 暂停
        val pauseButton = view.findViewById<AppCompatImageButton>(R.id.pauseRecording)
        // 停止
        val stopButton = view.findViewById<AppCompatImageButton>(R.id.stopRecording)
        // ImageView 定制的带有波形的图片
        val wave = view.findViewById<ImageView>(R.id.audioWave)
        wave.background = WaveDrawable(resources, 5)
        wave.setOnTouchListener { v: View, event: MotionEvent ->
            if (mAudioRecordDuration > 0 && mAudioPlayer != null && event.action == MotionEvent.ACTION_DOWN) {
                val fraction = event.x / v.width
                mAudioPlayer!!.seekTo((fraction * mAudioRecordDuration).toInt())
                (v.background as WaveDrawable).seekTo(fraction)
                return@setOnTouchListener true
            }
            false
        }

        // 短音频的面板
        val waveShort = view.findViewById<ImageView>(R.id.audioWaveShort)
        waveShort.background = WaveDrawable(resources)

        // 时间的字符串
        val timerView = view.findViewById<TextView>(R.id.duration)
        val timerShortView = view.findViewById<TextView>(R.id.durationShort)
        // 加载面板的按钮
        val audio = view.findViewById<AppCompatImageButton>(R.id.chatAudioButton)
        val visualizer: Runnable = object : Runnable {
            override fun run() {
                if (mAudioRecorder != null) {
                    val x = mAudioRecorder!!.maxAmplitude
                    mAudioSampler?.put(x)
                    if (mVisibleSendPanel == R.id.recordAudioPanel) {
                        (wave.background as WaveDrawable).put(x)
                        timerView.setText(TextHelper.millisToTime((SystemClock.uptimeMillis() - mRecordingStarted).toInt()))
                    } else if (mVisibleSendPanel == R.id.recordAudioShortPanel) {
                        (waveShort.background as WaveDrawable).put(x)
                        timerShortView.setText(TextHelper.millisToTime((SystemClock.uptimeMillis() - mRecordingStarted).toInt()))
                    }
                    mAudioSamplingHandler.postDelayed(
                        this,
                        AUDIO_SAMPLING.toLong()
                    )
                }
            }
        }

        // 录音按钮设置调整位置的监测
        mab.setConstraintChecker(object : MovableActionButton.ConstraintChecker {
            override fun check(
                newPos: PointF,
                startPos: PointF,
                buttonRect: Rect,
                parentRect: Rect
            ): PointF {
                // Constrain button moves to strictly vertical UP or horizontal LEFT (no diagonal).
                val dX = minOf(0f, newPos.x - startPos.x)
                val dY = minOf(0f, newPos.y - startPos.y)

                if (kotlin.math.abs(dX) > kotlin.math.abs(dY)) {
                    // Horizontal move.
                    newPos.x = maxOf(parentRect.left.toFloat(), newPos.x)
                    newPos.y = startPos.y
                } else {
                    // Vertical move.
                    newPos.x = startPos.x
                    newPos.y = maxOf(parentRect.top.toFloat(), newPos.y)
                }
                return newPos
            }
        })


        // 录音按钮的点击
        mab.setOnActionListener(object : MovableActionButton.ActionListener() {
            override fun onUp(x: Float, y: Float): Boolean {

                onButtonUpStopRecord()
                return true
            }

            override fun onZoneReached(id: Int): Boolean {
                mab.performHapticFeedback(
                    if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                        if (id == ZONE_CANCEL) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.CONFIRM
                    } else {
                        HapticFeedbackConstants.CONTEXT_CLICK
                    }
                )
                mab.visibility = View.INVISIBLE
                lockFab.visibility = View.GONE
                deleteFab.visibility = View.GONE
                audio.visibility = View.VISIBLE

                if (id == ZONE_CANCEL) {
                    if (mAudioRecorder != null) {
                        releaseAudio(false)
                    }
                    setSendPanelVisible(activity, R.id.sendMessagePanel)
                    releaseAudio(false)
                } else {
                    playButton.visibility = View.GONE
                    stopButton.visibility = View.VISIBLE
                    setSendPanelVisible(activity, R.id.recordAudioPanel)
                }
                return true
            }
        })



        // 录音按钮的长按手势识别，
        val gd = GestureDetector(context, object : SimpleOnGestureListener() {
            // 长按的手势
            override fun onLongPress(e: MotionEvent) {
                if (!permissionsHelper.hasAudioRecordPermission()) {
                    audioRecorderPermissionLauncher.launch(
                        arrayOf<String>(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.MODIFY_AUDIO_SETTINGS
                        )
                    )
                    return
                }

                if (mAudioRecorder == null) {
                    initAudioRecorder(activity)
                }
                try {
                    mAudioRecorder!!.start()
                    mRecordingStarted = SystemClock.uptimeMillis()
                    visualizer.run()
                } catch (ex: RuntimeException) {
                    Log.e(
                        "long press",
                        "Failed to start audio recording",
                        ex
                    )
                    Toast.makeText(activity, R.string.audio_recording_failed, Toast.LENGTH_SHORT)
                        .show()
                    return
                }

                // 开始录音之后，显示4个音频按钮
                mab.setVisibility(View.VISIBLE)
                lockFab.visibility = View.VISIBLE
                deleteFab.visibility = View.VISIBLE
                audio.visibility = View.INVISIBLE
                mab.requestFocus()
                // 显示短音频面板
                setSendPanelVisible(activity, R.id.recordAudioShortPanel)

                // Cancel zone on the left.
                val x: Int = mab.getLeft()
                val y: Int = mab.getTop()
                val width: Int = mab.getWidth()
                val height: Int = mab.getHeight()
                mab.addActionZone(
                    ZONE_CANCEL, Rect(
                        x - (width * 1.5).toInt(), y,
                        x - (width * 0.5).toInt(), y + height
                    )
                )
                // Lock zone above.
                mab.addActionZone(
                    ZONE_LOCK, Rect(
                        x, y - (height * 1.5).toInt(),
                        x + width, y - (height * 0.5).toInt()
                    )
                )
                val motionEvent = MotionEvent.obtain(
                    e.downTime, e.eventTime,
                    MotionEvent.ACTION_DOWN,
                    e.rawX,
                    e.rawY,
                    0
                )
                mab.dispatchTouchEvent(motionEvent)
            }
        })
        // Ignore the warning: click detection is not needed here.
        audio.setOnTouchListener { v: View?, event: MotionEvent ->
            val action = event.action  // 从 event 对象中获取触摸事件的动作类型（如按下、移动、抬起等）
            if (mab.getVisibility() === View.VISIBLE) {
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    audio.isPressed = false
                }
                // 调用 mab.dispatchTouchEvent(event) 来将触摸事件转发给 mab 处理，并返回其结果。return@setOnTouchListener
                // 用于返回到 setOnTouchListener 的上下文中
                return@setOnTouchListener mab.dispatchTouchEvent(event)
            }
            gd.onTouchEvent(event)
        }

        // 如果点击了删除按钮，删除刚才录制的音频，不要了
        view.findViewById<View>(R.id.deleteRecording).setOnClickListener { v: View? ->
            val wd: WaveDrawable = wave.background as WaveDrawable
            wd.stop()
            releaseAudio(false)
            setSendPanelVisible(activity, R.id.sendMessagePanel)
        }

        // 播放按钮
        playButton.setOnClickListener { v: View? ->
            pauseButton.visibility = View.VISIBLE
            playButton.visibility = View.GONE
            val wd: WaveDrawable = wave.background as WaveDrawable
            wd.start()
            initAudioPlayer(wd, playButton, pauseButton)
            mAudioPlayer!!.start()
        }
        // 暂停播放按钮，可以暂停的
        pauseButton.setOnClickListener { v: View? ->
            playButton.visibility = View.VISIBLE
            pauseButton.visibility = View.GONE
            val wd: WaveDrawable = wave.background as WaveDrawable
            wd.stop()
            mAudioPlayer?.pause()
        }

        // 停止录制按钮
        stopButton.setOnClickListener { v: View ->
           onClickStopAudioRecord()
        }

        // 发送音频的按钮； 在录制的过程中，其实也是可以执行发送动作的
        view.findViewById<View>(R.id.chatSendAudio).setOnClickListener { v: View? ->
            releaseAudio(true)
            sendAudio(activity)
            // 切换到发送消息的面板
            setSendPanelVisible(activity, R.id.sendMessagePanel)
        }

        return audio
    }

    // 手动点击了停止录制按钮
    fun onClickStopAudioRecord(){

        // 锁定的面板上的播放按钮
        val playButton = root!!.findViewById<AppCompatImageButton>(R.id.playRecording)

        // 停止
        val stopButton = root!!.findViewById<AppCompatImageButton>(R.id.stopRecording)
        // ImageView 定制的带有波形的图片
        val wave = root!!.findViewById<ImageView>(R.id.audioWave)


        releaseAudio(true)      // 保留记录的临时文件
        val wd: WaveDrawable = wave.background as WaveDrawable
        wd.reset()
        wd.setDuration(mAudioRecordDuration)
        wd.put(mAudioSampler!!.obtain(96))
        wd.seekTo(0f)

        // 重新设置按钮
        stopButton.visibility = View.GONE
        playButton.visibility = View.VISIBLE
    }

    // 松开录制按钮的停止录制
    fun onButtonUpStopRecord(){
        // 如果时间很短，持续按住然后发送即可
        if (mAudioRecorder != null) {
            releaseAudio(true)
            // 执行发送
            sendAudio(requireActivity() as AppCompatActivity)
        }

        // 真正用于控制录音的浮动按钮
        val mab: MovableActionButton = root!!.findViewById(R.id.audioRecorder)
        // 浮动锁定
        val lockFab = root!!.findViewById<ImageView>(R.id.lockAudioRecording)
        // 浮动删除
        val deleteFab = root!!.findViewById<ImageView>(R.id.deleteAudioRecording)
        val audio = root!!.findViewById<AppCompatImageButton>(R.id.chatAudioButton)

        mab.visibility = View.INVISIBLE
        lockFab.visibility = View.GONE
        deleteFab.visibility = View.GONE
        audio.visibility = View.VISIBLE
        // 停止按时，切换为发送
        setSendPanelVisible(requireActivity(), R.id.sendMessagePanel)
    }

    // 录音最大限长被触发的时候，
    // 长声音使用录制面板；
    // 短声音使用短声音面板，
    fun onMaxDurationReachedStopRecord(){
        if (mVisibleSendPanel == R.id.recordAudioPanel) {
            onClickStopAudioRecord()
        } else if (mVisibleSendPanel == R.id.recordAudioShortPanel) {
            onButtonUpStopRecord()
        }
    }


    // 当 Fragment 被隐藏或显示时调用
    fun setShowHide(b: Boolean){
        if (b){
            SdkGlobalData.userCallBackManager.addCallback(this)
        }else{
            SdkGlobalData.userCallBackManager.removeCallback(this)
        }
    }

    override fun onResume() {
        super.onResume()
        setShowHide(true)
        Log.d("MyFragment", "onResume called")
    }

    // 初始化录音按钮
    private fun initAudioRecorder(activity: Activity) {
        if (mAudioRecord != null) {
            mAudioRecord!!.delete()
            mAudioRecord = null
        }


        //  Android 13（API 级别 33） 开始
        if (Build.VERSION.SDK_INT >= 33) {
            mAudioRecorder = MediaRecorder(requireActivity())
        }else{
            mAudioRecorder = MediaRecorder()
        }

        mAudioRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        mAudioRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mAudioRecorder!!.setAudioEncodingBitRate(24000)
        mAudioRecorder!!.setAudioSamplingRate(16000)
        mAudioRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

        // 监听录音的状态变化，包括最大时长结束
        mAudioRecorder!!.setMaxDuration(MAX_DURATION) // 10 minutes.
        mAudioRecorder!!.setOnInfoListener { mr, what, _ ->
            if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                // 录音达到最大时长时的处理逻辑
                Log.d("Recorder", "录音已达到最大时长")
                onMaxDurationReachedStopRecord()
            }
        }

        if (AcousticEchoCanceler.isAvailable()) {
            mEchoCanceler = AcousticEchoCanceler.create(MediaRecorder.AudioSource.MIC)
        }
        if (NoiseSuppressor.isAvailable()) {
            mNoiseSuppressor = NoiseSuppressor.create(MediaRecorder.AudioSource.MIC)
        }
        if (AutomaticGainControl.isAvailable()) {
            mGainControl = AutomaticGainControl.create(MediaRecorder.AudioSource.MIC)
        }

        try {
            mAudioRecord = File.createTempFile("audio", ".m4a", activity.cacheDir)
            mAudioRecorder!!.setOutputFile(mAudioRecord!!.absolutePath)
            mAudioRecorder!!.prepare()
            mAudioSampler = AudioSampler()
        } catch (ex: IOException) {
            Log.w(
                "AudioRecorder",
                "Failed to initialize audio recording",
                ex
            )
            Toast.makeText(activity, R.string.audio_recording_failed, Toast.LENGTH_SHORT).show()
            mAudioRecorder?.release()
            mAudioRecorder = null
            mAudioSampler = null
            mAudioRecord = null
        }
    }

    // 初始化音乐播放器
    private fun initAudioPlayer(waveDrawable: WaveDrawable, play: View, pause: View) {
        if (mAudioPlayer != null) {
            return
        }


        val audioManager = requireActivity().getSystemService(Activity.AUDIO_SERVICE) as? AudioManager
        audioManager?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // 设置模式为通话模式
                //it.mode = AudioManager.MODE_IN_CALL
                it.mode = AudioManager.MODE_NORMAL

                // 打开扬声器
                it.isSpeakerphoneOn = true
            } else {
                // 处理低版本逻辑（如果需要）
                // 例如，你可以记录日志或提供兼容性说明
            }
        } ?: run {
            // 处理 audioManager 为 null 的情况
            Log.e("AudioManager", "Unable to get AudioManager service")
            TextHelper.showToast(requireContext(), "can't get audio manager")
            return
        }

        mAudioPlayer = MediaPlayer()
        mAudioPlayer!!.setOnCompletionListener(OnCompletionListener { mp: MediaPlayer? ->
            waveDrawable.reset()
            pause.visibility = View.GONE
            play.visibility = View.VISIBLE
        })
        mAudioPlayer!!.setAudioAttributes(
            AudioAttributes.Builder().setLegacyStreamType(AudioManager.STREAM_VOICE_CALL).build()
        )
        try {
            mAudioPlayer!!.setDataSource(mAudioRecord!!.absolutePath)
            mAudioPlayer!!.prepare()
            if (AutomaticGainControl.isAvailable()) {
                val agc = AutomaticGainControl.create(mAudioPlayer!!.audioSessionId)
                agc?.setEnabled(true)
            }
        } catch (ex: IOException) {
            Log.e("audio player", "Unable to play recording", ex)
            Toast.makeText(requireActivity(), "can't play audio", Toast.LENGTH_SHORT).show()
            mAudioPlayer = null
        } catch (ex: IllegalStateException) {
            Log.e("audio player", "Unable to play recording", ex)
            Toast.makeText(requireActivity(), "can't play audio", Toast.LENGTH_SHORT).show()
            mAudioPlayer = null
        }
    }

    // 释放录音资源
    private fun releaseAudio(keepRecord: Boolean) {

        val playButton = root!!.findViewById<AppCompatImageButton>(R.id.playRecording)
        // 暂停
        val pauseButton = root!!.findViewById<AppCompatImageButton>(R.id.pauseRecording)
        // 停止
        val stopButton = root!!.findViewById<AppCompatImageButton>(R.id.stopRecording)

        playButton.visibility = View.GONE
        pauseButton.visibility = View.GONE
        stopButton.visibility = View.GONE


        if (!keepRecord && mAudioRecord != null) {
            mAudioRecord?.delete()
            mAudioRecord = null
            mAudioRecordDuration = 0
        } else if (mRecordingStarted != 0L) {
            mAudioRecordDuration = (SystemClock.uptimeMillis() - mRecordingStarted).toInt()
        }
        mRecordingStarted = 0

        if (mAudioPlayer != null) {
            mAudioPlayer!!.stop()
            mAudioPlayer!!.reset()
            mAudioPlayer!!.release()
            mAudioPlayer = null
        }

        if (mAudioRecorder != null) {
            try {
                mAudioRecorder!!.stop()
            } catch (ignored: java.lang.RuntimeException) {
            }
            mAudioRecorder!!.release()
            mAudioRecorder = null
        }

        if (mEchoCanceler != null) {
            mEchoCanceler!!.release()
            mEchoCanceler = null
        }

        if (mNoiseSuppressor != null) {
            mNoiseSuppressor!!.release()
            mNoiseSuppressor = null
        }

        if (mGainControl != null) {
            mGainControl!!.release()
            mGainControl = null
        }
    }

    // 请求权限
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (!permissionsHelper.hasCameraPermission()) {
            permissions.add(Manifest.permission.CAMERA)
        }
        if (!permissionsHelper.hasGalleryPermission()) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (!permissionsHelper.hasLocationPermission()) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (!permissionsHelper.hasAudioRecordPermission()){
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }

        if (!permissionsHelper.hasWritePermission()){
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this.requireActivity(), permissions.toTypedArray(), PermissionsHelper.PERMISSION_REQUEST_CODE)
        }
    }



    fun testSendImage(){
        val contentResolver: ContentResolver = requireContext().contentResolver
        val details = mutableMapOf<String, Any?>()

        val draft = Drafty("")
        val url = "https://bkimg.cdn.bcebos.com/pic/adaf2edda3cc7cd98d1032641457363fb80e7bec3b4a?x-bce-process=image/format,f_auto/watermark,image_d2F0ZXIvYmFpa2UyNzI,g_7,xp_5,yp_5,P_20/resize,m_lfit,limit_1,h_1080"
        // 网络的
        //draft.insertImage(0,"image/jpeg", null, 884, 535, "",
//        draft.insertImage(0,"image/jpeg", null, 0, 0, "",
//            URI(url), 417737)
//        draft.insertImage(0,"image/jpeg", null, 0, 0, "",
//            URI(url), 0)

        //二进制方式

//            val t = draft.insertLocalImage(this.requireContext(),contentResolver, uri, "测试.jpg")
//            if (t == null){
//                return;
//            }

        Log.d("文件内容", "draft: ${draft.toPlainText()}")
        val msg2 = MessageContent(2, 1002, 10006, 10001,"我", "sys:4",
            UserStatus.ONLINE, MessageStatus.UPLOADING, false, false, false, "", draft)
        chatSession!!.addMessageToTail(msg2)

        mMessagesAdapter?.notifyDataSetChanged()
    }

    // 发送浏览选择的图片
    private fun sendLoadImage(context: Context, uri: Uri){

        val msgId = ChatSessionManager.sendImageMessageUploading(this.mChatIdLong, context, uri)
        if (msgId ==0L){
            Toast.makeText(requireContext(), "send image fail", Toast.LENGTH_LONG)
        }
        mMessagesAdapter?.notifyDataSetChanged()
    }

    fun hideSoftKeyboard() {
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        // 隐藏软键盘
        imm.hideSoftInputFromWindow(this.root?.windowToken, 0)
    }

    // 发送文本
    private fun sendText(activity: Activity?) {

        //testSendImage()

        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return
        }
        val inputField = activity.findViewById<EditText>(R.id.editMessage) ?: return

        if (mContentToForward != null) {
//            if (sendMessage(mForwardSender.appendLineBreak().append(mContentToForward), -1)) {
//                mForwardSender = null
//                mContentToForward = null
//            }
            activity.findViewById<View>(R.id.forwardMessagePanel).visibility = View.GONE
            activity.findViewById<View>(R.id.sendMessagePanel).visibility = View.VISIBLE
            return
        }

        val message = inputField.text.toString().trim { it <= ' ' }
        if (message != "") {
            // 提交消息，然后刷新显示
            ChatSessionManager.sendTextMessageOut(this.mChatIdLong, message)
            inputField.text.clear()
            hideSoftKeyboard()
            this.mMessagesAdapter!!.notifyDataSetChanged()
        }
    }

    // 发送语音
    private fun sendAudio(activity: AppCompatActivity?) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return
        }

        val args = arguments ?: return

        if (mAudioRecordDuration < MIN_DURATION) {
            return
        }

        val preview = mAudioSampler!!.obtain(96)


        val bits = mAudioRecord?.readBytes()
        var sz = 0
        if (bits ==null)
        {
            sz = 0
        }
        else{
            sz = bits.size
        }
        // 测试添加音乐
       val draft = Drafty()
        draft.insertAudio(
            0,
            "audio/aac",
            bits,
            preview,
            mAudioRecordDuration,
            "",
            null, // URI("https://lx-sycdn.kuwo.cn/c7aff93e02882b90b34e8f45387b4436/6755728e/resource/n2/3/57/2049851017.mp3?")
            sz.toLong())
        ChatSessionManager.sendAudioOut(this.mChatIdLong, requireContext(), draft)

    }

    // 浏览文件后发送
    private fun sendFile(uri:Uri){
        TextHelper.showToast(requireContext(), uri.toString())

        ChatSessionManager.sendFileMessageUploading(this.mChatIdLong, requireContext(), uri)
        this.mMessagesAdapter?.notifyDataSetChanged()
    }

    // 打开图片浏览器
    private fun openImageSelector(activity: Activity?) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return
        }

        // 检查外部数据读取权限
        if (permissionsHelper.hasGalleryPermission()){

            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)  // 这个加载器返回时候处理数据
            return
        }

        // 申请图片浏览权限，申请之后重新进入此函数
        val missing: LinkedList<String> = LinkedList<String>()
        missing.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        mImagePickerRequestPermissionLauncher.launch(missing.toTypedArray())
        return
    }

    // 打开文件的浏览器，
    private fun openFileSelector(activity: Activity?) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return
        }

        if (permissionsHelper.hasReadPermission()){
            mFilePickerLauncher.launch("*/*")
            return
        }
        // 申请权限之后，重新进入这个函数
        mFileOpenerRequestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
    }


    private fun cancelPreview(activity: Activity?) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return
        }

        mReplySeqID = -1
        mReply = null
        mContentToForward = null
        mForwardSender = null

        activity.findViewById<View>(R.id.replyPreviewWrapper).visibility = View.GONE
        activity.findViewById<View>(R.id.forwardMessagePanel).visibility = View.GONE
        activity.findViewById<View>(R.id.sendMessagePanel).visibility = View.VISIBLE
    }

//    fun showReply(activity: Activity, reply: Drafty, seq: Int) {
//        mReply = reply
//        mReplySeqID = seq
//        mContentToForward = null
//        mForwardSender = null
//
//        activity.findViewById<View>(R.id.forwardMessagePanel).visibility = View.GONE
//        activity.findViewById<View>(R.id.sendMessagePanel).visibility = View.VISIBLE
//        activity.findViewById<View>(R.id.replyPreviewWrapper).visibility = View.VISIBLE
//        val replyHolder = activity.findViewById<TextView>(R.id.contentPreview)
//        replyHolder.setText(reply.format(SendReplyFormatter(replyHolder)))
//    }
//
//    private fun showContentToForward(activity: Activity, sender: Drafty, content: Drafty) {
//        var content: Drafty = content
//        mReplySeqID = -1
//        mReply = null
//
//        activity.findViewById<View>(R.id.sendMessagePanel).visibility = View.GONE
//        val previewHolder = activity.findViewById<TextView>(R.id.forwardedContentPreview)
//        content = Drafty().append(sender).appendLineBreak()
//            .append(content.preview(UiUtils.QUOTED_REPLY_LENGTH))
//        previewHolder.setText(content.format(SendForwardedFormatter(previewHolder)))
//        activity.findViewById<View>(R.id.forwardMessagePanel).visibility = View.VISIBLE
//    }

    private inner class StickerReceiver : OnReceiveContentListener {
        @Nullable
        override fun onReceiveContent(view: View, payload: ContentInfoCompat): ContentInfoCompat? {
            val split = payload.partition { item -> item.uri != null }

            val activity = requireActivity() as? AppCompatActivity ?: return split.second
            if (split.first != null) {
                // Handle posted URIs.
                val data = split.first.clip
                if (data.itemCount > 0) {
                    val stickerUri = data.getItemAt(0).uri
                    val args = Bundle().apply {
//                        putParcelable(AttachmentHandler.ARG_LOCAL_URI, stickerUri)
//                        putString(AttachmentHandler.ARG_OPERATION, AttachmentHandler.ARG_OPERATION_IMAGE)
//                        putString(AttachmentHandler.ARG_TOPIC_NAME, mTopicName)
                    }

//                    val op = AttachmentHandler.enqueueMsgAttachmentUploadRequest(activity,
//                        AttachmentHandler.ARG_OPERATION_IMAGE, args)
//                    op?.result?.addListener({
//                        if (activity.isFinishing || activity.isDestroyed) {
//                            return@addListener
//                        }
//                        activity.syncAllMessages(true)
//                        notifyDataSetChanged(false)
//                    }, ContextCompat.getMainExecutor(activity))
                }
            }

            // Return content we did not handle.
            return split.second
        }
    }


}
