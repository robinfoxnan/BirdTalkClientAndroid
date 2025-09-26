package com.bird2fish.birdtalksdk.net

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toFile
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.Msg
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import okio.ByteString
import java.io.File
import java.io.FileInputStream
import java.util.concurrent.atomic.AtomicReference

object Session  {
    // Define session states
    enum class SessionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        HELLO,
        KEY_EXCHANGE_1,
        KEY_EXCHANGE_3,
        WAIT,
        LOGGING_IN,
        REGISTERING,
        READY,
    }

    enum class SessionStateWorking {
        SENDING_CODE,
        WAITING_SYN,
        SENDING_MSG,
    }


    private val _sessionState: MutableStateFlow<SessionState > = MutableStateFlow(SessionState.DISCONNECTED)
    val sessionState: StateFlow<SessionState> get() = _sessionState

    // 更新状态
    fun updateState(state: SessionState) {
        _sessionState.value = state
    }


    private var msgWaitQueque: SortableMap? = null

    // Private constructor for singleton pattern
    init {
        // Initialize any necessary variables
        this.msgWaitQueque = SortableMap()
    }


    fun startChecking() {
        msgWaitQueque!!.startChecking(5)
    }

    fun stopChecking() {
        msgWaitQueque!!.stopChecking()
    }

    // 开始发送消息
    fun startHello() {
        MsgEncocder.createHelloMsg()
    }

    // 检查消息是否过期
    fun checkMsgTimeout(id: Long, msg: Msg?) {
    }

    // Method to dispatch messages
    fun dispatchMsg(bytes: ByteString?) {
        try {
            // 将 Okio ByteString 转换
            val inputStream = bytes!!.toByteArray()
            val msg = Msg.parseFrom(inputStream)

            MsgEncocder.onMsg(msg)

        }catch (e:Exception){
            // 解析消息出错了，需要通知界面
            Log.e("Session class", e.toString())
        }


    }

    ///////////////////////////////////////////////////////////////////////////
    // 注册
    fun startRegister(mode:String, uid:String, email:String, pwd:String): Boolean {
        Session.updateState(SessionState.REGISTERING)
        MsgEncocder.sendRegister(uid, pwd, email, mode)
        return true
    }

    fun sendRegisterCode(mode:String, uid:String, code:String){
        MsgEncocder.sendCodeMessage(mode, uid, code)
    }

    fun startLogin(mode :String, id:String, pwd:String): Boolean {
        Session.updateState(Session.SessionState.LOGGING_IN)
        MsgEncocder.sendLogin(mode, id, pwd)
        return true
    }

    fun sendLoginCode(mode:String, uid:String, code:String){
        MsgEncocder.sendCodeMessage(mode, uid, code)
    }

    fun queryFriendInfo(id: Long): Boolean {
        return true
    }

    // 查询自己所在的各个组
    fun queryUserInGroups(): Boolean {
        return true
    }

    // 加载私聊会话列表，从本地数据库
    fun loadP2PChats() {
    }

    // 加载群聊列表，从本地数据库
    fun loadGroupChats() {
    }

    // 加载某个会话的消息列表，这里应该是历史消息列表
    fun loadChatMessages(id: String?) {
    }

    // Explicit getInstance function
    fun getInstance(): Session {
        return this
    }

    // 加载自己的个人信息
    // 1） 同步最新的消息，2)同步好友列表  3） 同步所在群  4）同步群消息
    fun loadOnLogin(){
        SdkGlobalData.initLoad()
    }

    // 登录成功，通知界面更改页面
    fun loginOk(){

        val resultMap = mapOf(
            "result" to "ok"
        )
        // sdk内部需要与服务器同步数据，然后通知界面跳转并刷新
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.LOGIN_OK, 0,0L, 0L, resultMap)
        updateState(Session.SessionState.READY)

        // 这里还需要与服务器同步数据
        loadOnLogin()

    }

    // 服务器应答登录错误
    fun loginFail(uid:String, detail:String){

        val resultMap = mapOf(
            "result" to "ok"
        )
        SdkGlobalData.userCallBackManager.invokeOnErrorCallbacks(InterErrorType.USER_PWD_ERROR, "", "", "")
    }

    // 服务器发送完毕，通知跳转到输入验证码的页面
    // 如果不输入口令的，需要填写验证码
    fun loginNotifyCode(){

        val resultMap = mapOf(
            "result" to "ok"
        )
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.LOGIN_CODE, 0,0L, 0L, resultMap)
    }

    fun uploadSmallFile(ctx: Context, uri: Uri?) {
        if (uri == null) {
            println("URI is null, cannot upload file.")
            return
        }

        try {
            // 获取文件名
            var name = TextHelper.getFileNameFromUri(ctx, uri)
            if (name.isNullOrEmpty()) {
                name = TextHelper.generateRandomByteString()
            }

            // 计算文件的 MD5 哈希值
            val hashCode = TextHelper.calculateMD5FromUri(ctx, uri)

            // 定义分块大小和文件大小
            val chunkSz = 1024 * 1024 // 1MB
            val fileSize = TextHelper.getFileSize(ctx, uri)
            val chunks = ((fileSize + chunkSz - 1) / chunkSz).toInt()

            // 获取文件的 MIME 类型
            val type = TextHelper.getMimeTypeFromUri(ctx, uri)

            // 打开输入流
            val inputStream = ctx.contentResolver.openInputStream(uri) ?: throw IllegalArgumentException("Unable to open InputStream for URI")

            val buffer = ByteArray(chunkSz)
            var index = 0
            var bytesRead: Int

            // 循环读取文件，分块上传
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {

                if (bytesRead == chunkSz){
                    MsgEncocder.sendFileChunk(name, fileSize, index, chunkSz, chunks, hashCode, buffer, type)
                }else{
                    val chunkData = buffer.copyOf(bytesRead) // 确保最后一块数据大小正确
                    MsgEncocder.sendFileChunk(name, fileSize, index, chunkSz, chunks, hashCode, chunkData, type)
                }

                index++
            }

            // 关闭流
            inputStream.close()
            println("File upload complete. Total chunks: $chunks")
        } catch (e: Exception) {
            // 打印异常信息
            e.printStackTrace()
        }
    }


    fun uploadBigFile(ctx: Context, uri: Uri?){

    }


}
