package com.bird2fish.birdtalksdk.net

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
        // Implement message dispatch logic here
    }

    ///////////////////////////////////////////////////////////////////////////
    // 注册
    fun startRegister(): Boolean {
        Session.updateState(SessionState.REGISTERING)
        return true
    }

    fun startLogin(mode :String, id:String, pwd:String): Boolean {
        Session.updateState(Session.SessionState.LOGGING_IN)
        MsgEncocder.createLogin(mode, id, pwd)
        return true
    }

    fun sendCode(code:String){

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

}
