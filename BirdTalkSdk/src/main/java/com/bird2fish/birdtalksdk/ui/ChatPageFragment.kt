package com.bird2fish.birdtalksdk.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.NonNull
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
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.model.MessageContent
import com.bird2fish.birdtalksdk.model.UserStatus
import com.bird2fish.birdtalksdk.uihelper.PermissionsHelper
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import com.bird2fish.birdtalksdk.widgets.AudioSampler
import com.bird2fish.birdtalksdk.widgets.MovableActionButton
import com.bird2fish.birdtalksdk.widgets.WaveDrawable
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import java.io.IOException
import java.util.LinkedList

class ChatPageFragment : Fragment() {

    private var parentView :ChatManagerFragment? = null
    private val SUPPORTED_MIME_TYPES: Array<String> = arrayOf("image/*")
    // Number of milliseconds between audio samples for recording visualization.
    private val AUDIO_SAMPLING: Int = 100
    // Minimum duration of an audio recording in milliseconds.
    private val MIN_DURATION: Int = 1000
    // Maximum duration of an audio recording in milliseconds.
    private val MAX_DURATION: Int = 600000


    private val ZONE_CANCEL: Int = 0
    private val ZONE_LOCK: Int = 1

    private lateinit var permissionsHelper: PermissionsHelper

    private lateinit var chatId: String

    private var mMessageViewLayoutManager: LinearLayoutManager? = null
    private var mRecyclerView: RecyclerView? = null
    private var mMessagesAdapter: ChatPageAdapter? = null
    private var mRefresher: SwipeRefreshLayout? = null
    //private var mFailureListener: PromisedReply.FailureListener<ServerMessage>? = null

    private var mGoToLatest: FloatingActionButton? = null

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
                TextHelper.showToast(requireContext(), uri.toString())

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


            TextHelper.showToast(requireContext(), content.toString())
//            val args = Bundle().apply {
//                putParcelable(AttachmentHandler.ARG_LOCAL_URI, content)
//                putString(AttachmentHandler.ARG_OPERATION, AttachmentHandler.ARG_OPERATION_FILE)
//                putString(AttachmentHandler.ARG_TOPIC_NAME, mTopicName)
//            }
            // Show attachment preview.
            //activity.showFragment(MessageActivity.FRAGMENT_FILE_PREVIEW, args, true)

        }



    companion object {
        private const val ARG_CHAT_ID = "chat_id"

        fun newInstance(chatId: String, p: ChatManagerFragment): ChatPageFragment {
            val fragment = ChatPageFragment()
            fragment.setParent(p)
            val args = Bundle()
            args.putString(ARG_CHAT_ID, chatId)
            fragment.arguments = args
            return fragment
        }
    }

    fun setParent(p:ChatManagerFragment){
        this.parentView = p
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 获取 arguments，注意这一步
        chatId = arguments?.getString(ARG_CHAT_ID) ?: ""  // 安全获取



    }

    private fun scrollToBottom(smooth: Boolean) {
        if (smooth) {
            mRecyclerView!!.smoothScrollToPosition(0)
        } else {
            mRecyclerView!!.scrollToPosition(0)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_chat_page, container, false)

        permissionsHelper = PermissionsHelper(requireActivity())
        // 一键滚动到底部的按钮
        mGoToLatest = view.findViewById(R.id.goToLatest)
        mGoToLatest?.setOnClickListener(View.OnClickListener { v: View? ->
            scrollToBottom(
                true
            )
        })

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
        var dataList = LinkedList<MessageContent>()

        val msg = MessageContent(1, 1001, "飞鸟", "sys:3", UserStatus.ONLINE, true, "昨天你去哪里了呢？")
        dataList += msg

        val msg1 = MessageContent(2, 1002, "我", "sys:4", UserStatus.ONLINE, false, "西单啊，还去了奥森")
        dataList += msg1
        // 建立数据列表控件
        mRecyclerView = view.findViewById(R.id.messages_container)
        mRecyclerView?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val adapter = mRecyclerView?.getAdapter() ?: return
                val itemCount = adapter.itemCount
                val pos = mMessageViewLayoutManager?.findLastVisibleItemPosition()
//                if (itemCount - pos < 4) {
//                    (adapter as MessagesAdapter).loadNextPage()
//                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val pos = mMessageViewLayoutManager?.findFirstVisibleItemPosition()
//                if (dy > 5 && pos > 2) {
//                    mGoToLatest.show()
//                } else if (dy < -5 || pos == 0) {
//                    mGoToLatest.hide()
//                }
            }
        })

        mMessagesAdapter = ChatPageAdapter(dataList)
        mMessagesAdapter!!.setView(this)
        // 第三步：给listview设置适配器（view）

        mRecyclerView?.layoutManager = LinearLayoutManager(context)
        mRecyclerView?.setAdapter(mMessagesAdapter);
        // 刷新动作



        mRefresher?.setOnRefreshListener(OnRefreshListener {
//            if (!mMessagesAdapter.loadNextPage() && !StoredTopic.isAllDataLoaded(mTopic)) {
//                try {
//                    mTopic.getMeta(
//                        mTopic.getMetaGetBuilder()
//                            .withEarlierData(co.tinode.tindroid.MessagesFragment.MESSAGES_TO_LOAD)
//                            .build()
//                    )
//                        .thenApply(
//                            object : SuccessListener<ServerMessage?>() {
//                                fun onSuccess(result: ServerMessage?): PromisedReply<ServerMessage> {
//                                    activity!!.runOnUiThread { mRefresher.setRefreshing(false) }
//                                    return@setOnRefreshListener null
//                                }
//                            },
//                            object : FailureListener<ServerMessage?>() {
//                                fun onFailure(err: Exception?): PromisedReply<ServerMessage> {
//                                    activity!!.runOnUiThread { mRefresher.setRefreshing(false) }
//                                    return@setOnRefreshListener null
//                                }
//                            }
//                        )
//                } catch (e: Exception) {
//                    mRefresher?.setRefreshing(false)
//                }
//            } else {
//                mRefresher?.setRefreshing(false)
//            }
        })

        //mFailureListener = ToastFailureListener(activity)

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

        return view
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

                // 如果时间很短，持续按住然后发送即可
                if (mAudioRecorder != null) {
                    releaseAudio(true)
                    sendAudio(activity)
                }

                mab.visibility = View.INVISIBLE
                lockFab.visibility = View.GONE
                deleteFab.visibility = View.GONE
                audio.visibility = View.VISIBLE
                // 停止按时，切换为发送
                setSendPanelVisible(activity, R.id.sendMessagePanel)
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
            mAudioPlayer!!.pause()
        }

        // 停止录制按钮
        stopButton.setOnClickListener { v: View ->
            playButton.visibility = View.VISIBLE
            v.visibility = View.GONE
            releaseAudio(true)
            val wd: WaveDrawable = wave.background as WaveDrawable
            wd.reset()
            wd.setDuration(mAudioRecordDuration)
            wd.put(mAudioSampler!!.obtain(96))
            wd.seekTo(0f)
        }

        // 发送音频的按钮
        view.findViewById<View>(R.id.chatSendAudio).setOnClickListener { v: View? ->
            releaseAudio(true)
            sendAudio(activity)
            // 切换到发送消息的面板
            setSendPanelVisible(activity, R.id.sendMessagePanel)
        }

        return audio
    }


    override fun onResume() {
        super.onResume()
        Log.d("MyFragment", "onResume called")
    }

    override fun onPause() {
        super.onPause()
        Log.d("MyFragment", "onPause called")
    }

    override fun onStop() {
        super.onStop()
        Log.d("MyFragment", "onStop called")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("MyFragment", "onDestroyView called")
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
        mAudioRecorder!!.setMaxDuration(MAX_DURATION) // 10 minutes.
        mAudioRecorder!!.setAudioEncodingBitRate(24000)
        mAudioRecorder!!.setAudioSamplingRate(16000)
        mAudioRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

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
                it.mode = AudioManager.MODE_IN_CALL

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


    // 真正发送消息函数，执行发送消息
    private fun sendMessage(content: MessageContent, replyTo: Int): Boolean {

        if (activity != null) {
            val done: Boolean = true
            if (done) {
                scrollToBottom(false)
            }
            return done
        }
        return false
    }

    // 发送文本
    private fun sendText(activity: Activity?) {
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
//            var msg: Drafty = Drafty.parse(message)
//            if (mReply != null) {
//                msg = mReply.append(msg)
//            }
//            if (sendMessage(msg, mReplySeqID)) {
//                // Message is successfully queued, clear text from the input field and redraw the list.
//                inputField.text.clear()
//                if (mReplySeqID > 0) {
//                    mReplySeqID = -1
//                    mReply = null
//                    activity.findViewById<View>(R.id.replyPreviewWrapper).visibility =
//                        View.GONE
//                }
//            }
        }
    }

    private fun sendAudio(activity: AppCompatActivity?) {
        if (activity == null || activity.isFinishing || activity.isDestroyed) {
            return
        }

        val args = arguments ?: return

        if (mAudioRecordDuration < MIN_DURATION) {
            return
        }

        val preview = mAudioSampler!!.obtain(96)
//        args.putByteArray(AttachmentHandler.ARG_AUDIO_PREVIEW, preview)
//        args.putParcelable(AttachmentHandler.ARG_LOCAL_URI, Uri.fromFile(mAudioRecord))
//        args.putString(AttachmentHandler.ARG_FILE_PATH, mAudioRecord!!.absolutePath)
//        args.putInt(AttachmentHandler.ARG_AUDIO_DURATION, mAudioRecordDuration)
//        args.putString(AttachmentHandler.ARG_OPERATION, AttachmentHandler.ARG_OPERATION_AUDIO)
//        args.putString(AttachmentHandler.ARG_TOPIC_NAME, mTopicName)
//        AttachmentHandler.enqueueMsgAttachmentUploadRequest(
//            activity,
//            AttachmentHandler.ARG_OPERATION_AUDIO,
//            args
//        )
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

            val activity = activity as? AppCompatActivity ?: return split.second
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
