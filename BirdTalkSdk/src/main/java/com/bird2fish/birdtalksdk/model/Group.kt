package com.bird2fish.birdtalksdk.model

import java.util.LinkedList


/**
 * 群组实体
 * 核心信息用于本地表存储
 * 扩展信息按需异步加载
 */
class Group(

    /* ================= 基础标识 ================= */

    var gid: Long = 0L,                 // 群 ID
    var ownerId: Long = 0L,             // 群主 UID

    /* ================= 展示信息 ================= */

    var name: String = "-",             // 群名称
    var brief: String = "...",           // 群简介
    var icon: String = "",               // 群头像 URL
    var tags: String = "",               // 标签（| 分割）

    /* ================= 群状态 ================= */

    var membersCount: Int = 0,           // 群人数（可冗余）
    var mask: Int = 0,                   // 群位掩码（扩展用）

    /* ================= 群类型 ================= */

    var chatType: String = CHAT_TYPE_CHAT,       // chat / channel
    var joinType: String = JOIN_TYPE_DIRECT,     // direct / auth / question / invite
    var visibleType: String = VISIBLE_PUBLIC,    // public / private

    /* ================= 入群验证 ================= */

    var question: String = "",           // 入群问题
    var answer: String = "",             // 入群答案

    /* ================= 群控制 ================= */

    var isMuteAll: Boolean = false        // 是否全员禁言
) {

    /* ================= 扩展对象（非表字段） ================= */

    var owner: User? = null              // 群主对象（延迟加载）

    /** 管理员列表 uid -> User */
    private val admins: MutableMap<Long, User> = LinkedHashMap()

    /** 群成员 uid -> User */
    private val members: MutableMap<Long, User> = LinkedHashMap()

    /* ================= 业务方法 ================= */

    fun isOwner(uid: Long): Boolean = ownerId == uid

    fun isAdmin(uid: Long): Boolean =
        uid == ownerId || admins.containsKey(uid)

    fun isMember(uid: Long): Boolean =
        members.containsKey(uid)

    fun addAdmin(user: User) {
        admins[user.id] = user
    }

    fun removeAdmin(uid: Long) {
        admins.remove(uid)
    }

    // 通知用户添加到组的时候需要，查询成员列表时候也需要
    fun addUsers(members:List<User>){
        for (u in members){
            if (u.role.contains('o')){
                this.addAdmin(u)
                this.addMember(u)
                this.ownerId = u.id
                this.owner = u
            }else if (u.role.contains('a')){
                this.addAdmin(u)
            }else{
                this.addMember(u)
            }
        }
    }

    fun addMember(user: User) {
        members[user.id] = user
    }

    fun removeMember(uid: Long) {
        members.remove(uid)
    }

    fun getAdmins(): MutableList<User>{
        return this.admins.values.toMutableList()
    }

    fun getMembers(): MutableList<User> {
        return this.members.values.toMutableList()
    }

    fun clearMembers() {
        members.clear()
        admins.clear()
    }

    fun findUser(uid:Long):User?{
        if (members.containsKey(uid)){
            return members[uid]
        }
        if (admins.containsKey(uid)){
            return admins[uid]
        }
        return UserCache.findUserSync(uid)
    }

    // 这里是网络返回的数据，所有只有基本的属性
    fun update(other:Group){
        if (other === this) return
        this.name = other.name
        this.brief = other.brief
        this.icon = other.icon
        this.tags = other.tags
        this.membersCount = other.membersCount
        this.mask = other.mask
        this.visibleType = other.visibleType
        this.chatType = other.chatType
        this.joinType = other.joinType
        this.question = other.question
        this.answer = other.answer
    }

    companion object {

        /* ========== chatType ========== */
        const val CHAT_TYPE_CHAT = "chat"
        const val CHAT_TYPE_CHANNEL = "channel"

        /* ========== joinType ========== */
        const val JOIN_TYPE_DIRECT = "direct"
        const val JOIN_TYPE_AUTH = "auth"
        const val JOIN_TYPE_QUESTION = "question"
        const val JOIN_TYPE_INVITE = "invite"

        /* ========== visibleType ========== */
        const val VISIBLE_PUBLIC = "public"
        const val VISIBLE_PRIVATE = "private"
    }
}
