package com.bird2fish.birdtalksdk.model

// 解析后的用于界面所显示的类的
class MessageContent(
    val msgId : Long,
    val userId: Long,         // 用户的唯一ID
    val nick: String,           // 用户的昵称
    val iconUrl: String?,       // 用户图标的URL，可以为空
    val userStatus: UserStatus,      // 用户的状态，枚举类型
    val msgStatus: MessageStatus,
    val inOut :Boolean,
    var bRead :Boolean,
    var bRecv:Boolean,

    val text :String,
    val content :Drafty?

) {

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
    SENDING,
    UPLOADING,      //
    UPLOADED,     //
    FAIL,        //
    DOWNLOADING,        //
    DOWNLOADED,
    OK,

}