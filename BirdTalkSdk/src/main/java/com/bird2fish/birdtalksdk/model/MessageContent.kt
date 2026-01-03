package com.bird2fish.birdtalksdk.model

import android.net.Uri
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatMsgType
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatType
import com.bird2fish.birdtalksdk.uihelper.TextHelper

// 解析后的用于界面所显示的类的
class MessageContent(
    var session: ChatSession,
    var msgId : Long,
    var sendId:Long,
    var msgStatus: MessageStatus,
    val inOut :Boolean,   // in: true, out: false
    var tm1:Long,
    var tm2:Long,
    var tm3:Long,
    val text :String,
    var content :Drafty?,
    var msgType:ChatMsgType,
    var fileUri: Uri? = null,  // 上传文件使用的，本地路径
    var mime : String? = "",
    var fileSz:Long = 0L,
    var fileName:String = "",
) {
    var tId:Long   = 0L            // 这里是对方的好友的ID，或者群组的ID
    var userId: Long =0L           // 这里是发言的人
    var nick: String = ""          // 用户的昵称
    var iconUrl: String = ""       // 用户图标的URL，可以为空


    var fileHashCode:String = ""
    var isP2p :Boolean = true
    var tm:Long = System.currentTimeMillis()
    var tmResend :Long = 0L

    var msgRefId:Long = 0L
    var contentOut:String = ""

    var detail :String = ""  // 失败的原因
    // 开始时候为0，服务器应答后，这里改为自己发送的ID
    //    val userStatus: UserStatus  = UserStatus.ONLINE    // 用户的状态，枚举类型

    val isRead: Boolean
        get() = tm3 > 0

    val isRecv: Boolean
        get() = tm2 > 0

    val isSent: Boolean
        get() = tm1 > 0


    init {
        this.tId = session.tid
        // in: true, out: false
        if (inOut){
            this.iconUrl = session.icon
            this.nick = session.title
            this.userId = session.tid
        }else{
            this.iconUrl = SdkGlobalData.selfUserinfo.icon
            this.nick = SdkGlobalData.selfUserinfo.nick
            this.userId = SdkGlobalData.selfUserinfo.id
        }
        this.isP2p = session.isP2pChat()

    }
    // 是否在处理
    fun isPending(): Boolean {
        if (msgStatus == MessageStatus.UPLOADING) {
            return true
        }

        if (msgStatus == MessageStatus.DOWNLOADING) {
            return true
        }

        return false
    }
}

// 定义用户状态的枚举
enum class UserStatus {
    ONLINE,      // 在线
    OFFLINE,     // 离线
    BUSY,        // 忙碌
    AWAY         // 离开
}

// 消息的几种状态
enum class MessageStatus {
    DRAFT,
    QUEUED,
    UPLOADING,      //
    UPLOADED,     //
    SENDING,
    FAIL,        //
    DOWNLOADING,        //
    DOWNLOADED,
    OK,
    RECV,
    SEEN,
    BACKWARD,   // 前侧边界，需要加载
    FORWARD,    // 后侧边界，需要加载
    CONTINUOUS,  // 加载整块消息的中间部分
}