package com.bird2fish.birdtalksdk.model
import android.util.Base64

class User() {
    var id: Long = 0
    var name: String = ""
    var nick: String = ""
    var nick1: String = ""
    var nick2: String = ""
    var nick3: String = ""
    var pwd: String = ""
    var gid: Long = 0
    var age: Int = 0
    var gender: String = ""
    var region: String = ""
    var icon: String = ""
    var follows: Int = 0
    var fans: Int = 0
    var isFollow: Boolean = false
    var isFan: Boolean = false
    var introduction: String = ""
    var lastLoginTime: String = ""
    var isOnline: Boolean = false
    var mask: Int = 0
    var cryptKey: String = ""
    var cryptType: String = ""
    var sharedPrint: Long = 0
    var sharedKey: ByteArray? = null
    var phone: String = ""
    var email: String = ""
    var role:String = ""
    /**
     * 更新除 id 外的字段
     */
    fun update(other: User) {
        if (other === this) return
        this.name = other.name
        this.nick = other.nick
        this.nick1 = other.nick1
        this.nick2 = other.nick2
        this.nick3 = other.nick3
        this.gid = other.gid
        this.age = other.age
        this.gender = other.gender
        this.region = other.region
        this.icon = other.icon
        this.introduction = other.introduction
        this.phone = other.phone
        this.email = other.email

//        this.mask = other.mask
//        this.pwd = other.pwd
//        this.follows = other.follows
//        this.fans = other.fans
//        this.isFollow = other.isFollow
//        this.isFan = other.isFan
//        this.cryptKey = other.cryptKey
//        this.cryptType = other.cryptType
//        this.sharedPrint = other.sharedPrint
//        this.sharedKey = other.sharedKey?.clone() // 避免引用共享
//        this.lastLoginTime = other.lastLoginTime
//        this.isOnline = other.isOnline
    }

    /**
     * 安全转换 null 字符串
     */
    private fun nullToText(value: String?): String = value ?: "null"

    override fun toString(): String {
        val sharedKeyBase64 = sharedKey?.let { Base64.encodeToString(it, Base64.DEFAULT) } ?: ""
        return """
            User{
              id=$id, name=${nullToText(name)}, nick=${nullToText(nick)},
              nick1=${nullToText(nick1)}, nick2=${nullToText(nick2)}, nick3=${nullToText(nick3)},
              pwd=${nullToText(pwd)}, gid=$gid, age=$age, gender=${nullToText(gender)},
              region=${nullToText(region)}, icon=${nullToText(icon)}, follows=$follows, fans=$fans,
              isFollow=$isFollow, isFan=$isFan, introduction=${nullToText(introduction)},
              lastLoginTime=${nullToText(lastLoginTime)}, isOnline=$isOnline, mask=$mask,
              cryptKey=${nullToText(cryptKey)}, cryptType=${nullToText(cryptType)},
              sharedPrint=$sharedPrint, sharedKey=${nullToText(sharedKeyBase64)},
              phone=${nullToText(phone)}, email=${nullToText(email)}
            }
        """.trimIndent()
    }
}

