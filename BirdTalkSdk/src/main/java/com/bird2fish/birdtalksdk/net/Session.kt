package com.bird2fish.birdtalksdk.net

import android.util.Log
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.Msg
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okio.ByteString
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

    fun loginOk(){
        // sdk内部需要与服务器同步数据，然后通知界面跳转并刷新
        SdkGlobalData.userCallBack?.onEvent(MsgEventType.LOGIN_OK, 0,0L, 0L)
        updateState(Session.SessionState.READY)
    }

    fun loginFail(uid:String, detail:String){

    }

    fun loginNotifyCode(){

    }

}
