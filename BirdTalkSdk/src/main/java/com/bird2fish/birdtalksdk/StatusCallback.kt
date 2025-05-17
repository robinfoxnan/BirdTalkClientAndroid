package com.bird2fish.birdtalksdk


// 通知界面错误消息
enum class InterErrorType {

    CONNECT_ERROR,
    SERVER_ERROR,
    USER_NAME_ERROR,
    USER_PHONE_ERROR,
    USER_EMAIL_ERROR,
    USER_PWD_ERROR,

}

// sdk通知界面的操作
enum class MsgEventType {
    LOGIN_OK,  // 跳转

    FRIEND_REQUEST,   // 有好友申请，包括关注和
    FRIEND_REQ_REPLY, // 你申请好友的应答
    FRIEND_ONLINE,    // 好友上线
    FRIEND_OFFLINE,   // 好友下线
    FRIEND_UPDATE,    // 好友更新信息


    MSG_COMING,          // 新消息
    MSG_DOWNLOAD_ERROR,  // 下载失败

    MSG_SEND_ERROR,      // 发送消息失败
    MSG_SEND_OK,         // 发送消息成功
    MSG_RECV_OK,         // 对方接受消息
    MSG_READ_OK,         // 对方阅读消息

}

interface StatusCallback {

    // 比如 "", "", ""
    fun onError(code :InterErrorType, lastAction:String, errType:String, detail:String)


    fun onEvent(eventType:MsgEventType, msgType:Int, msgId:Long, fid:Long)
}