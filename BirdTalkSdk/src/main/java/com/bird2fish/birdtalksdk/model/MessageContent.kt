package com.bird2fish.birdtalksdk.model

// 解析后的用于界面所显示的类的
data class MessageContent(
    val msgId : Long,
    val userId: Long,         // 用户的唯一ID
    val nick: String,           // 用户的昵称
    val iconUrl: String?,       // 用户图标的URL，可以为空
    val status: UserStatus,      // 用户的状态，枚举类型
    val inOut :Boolean,

    val text :String,
)

// 定义用户状态的枚举
enum class UserStatus {
    ONLINE,      // 在线
    OFFLINE,     // 离线
    BUSY,        // 忙碌
    AWAY         // 离开
}