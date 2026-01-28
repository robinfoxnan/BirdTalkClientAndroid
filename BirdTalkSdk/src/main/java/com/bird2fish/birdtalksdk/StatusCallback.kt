package com.bird2fish.birdtalksdk


// 通知界面错误消息
enum class InterErrorType {

    CONNECT_ERROR,
    SERVER_ERROR,
    USER_NAME_ERROR,
    USER_PHONE_ERROR,
    USER_EMAIL_ERROR,
    USER_PWD_ERROR,
    UPLOAD_FAIL,
    FRIEND_INFO_ERROR,

}

// sdk通知界面的操作
enum class MsgEventType {
    CONNECTING,
    RECONNECTING,
    CONNECTED,

    LOGIN_OK,  // 跳转
    LOGIN_CODE, // 需要用户填写验证码

    FRIEND_REQUEST,   // 有好友申请，包括关注和
    FRIEND_REQ_REPLY, // 你申请好友的应答
    FRIEND_ADD_NOTICE,
    FRIEND_REMOVE_NOTICE,
    FRIEND_ONLINE,    // 好友上线
    FRIEND_OFFLINE,   // 好友下线
    FRIEND_UPDATE,    // 好友更新信息
    FRIEND_LIST_FAN,    // 更新界面
    FRIEND_LIST_FOLLOW,
    FRIEND_CHAT_SESSION,


    MSG_COMING,          // 新消息
    MSG_HISTORY,         // 历史数据加载完成



    MSG_SEND_ERROR,      // 发送消息失败
    MSG_SEND_OK,         // 发送消息成功
    MSG_RECV_OK,         // 对方接受消息
    MSG_READ_OK,         // 对方阅读消息

    MSG_UPLOAD_OK,
    MSG_UPLOAD_PROCESS,
    MSG_UPLOAD_FAIL,

    MSG_DOWNLOAD_OK,
    MSG_DOWNLOAD_PROCESS,
    MSG_DOWNLOAD_FAIL,  // 下载失败

    USR_UPDATEINFO_OK,
    USR_UPDATEINFO_FAIL,

    SEARCH_FRIEND_RET,
    SEARCH_GROUP_RET,


    APP_NOTIFY_SEND_MSG,  // SDK通知界面
    APP_NOTIFY_CHANGE_SESSION,

    GROUP_CREATE_OK,
    GROUP_CREATE_FAIL,
    GROUP_JOIN_FAIL,
    GROUP_JOIN_OK,
    GROUP_MEMBER_JOIN,
    GROUP_MEMBER_LEAVE,
    GROUP_MEMBER_NEED_AUTH,
    GROUP_MEMBER_INVITE,
    GROUP_DISMESS,
    GROUP_OWNER_CHANGE,
    GROUP_UPDATE_INFO_OK,
    GROUP_UPDATE_INFO_FAIL,
    GROUP_MANAGER_CHANGE,

}

interface StatusCallback {

    // 比如 "", "", ""
    fun onError(code :InterErrorType, lastAction:String, errType:String, detail:String)


    fun onEvent(eventType:MsgEventType, msgType:Int, msgId:Long, fid:Long, params:Map<String, String>)
}

class CallbackManager {
    private val callbacks = mutableListOf<StatusCallback>()
    private val lock = Any()

    fun addCallback(callback: StatusCallback) {
        synchronized(lock) {
            if (!callbacks.contains(callback)) {
                callbacks.add(callback)
            }
        }
    }

    fun removeCallback(callback: StatusCallback) {
        synchronized(lock) {
            callbacks.remove(callback)
        }
    }

    fun invokeOnErrorCallbacks(code: InterErrorType, lastAction: String, errType: String, detail: String) {
        val copy = synchronized(lock) { callbacks.toList() }
        copy.forEach { it.onError(code, lastAction, errType, detail) }
    }

    fun invokeOnEventCallbacks(eventType: MsgEventType, msgType: Int, msgId: Long, fid: Long, params:Map<String, String>) {
        val copy = synchronized(lock) { callbacks.toList() }
        copy.forEach { it.onEvent(eventType, msgType, msgId, fid, params) }
    }
}