package com.bird2fish.birdtalksdk.model

import java.util.LinkedList


/**
 * 群组会话
 */
class Group : Topic {
    var owner: User? = null
    private var admins: MutableMap<Long, User> = LinkedHashMap()
    //群成员 uid 列表
    private var members: MutableMap<Long, User> = LinkedHashMap()


    // 是否全员禁言
    var isMuteAll: Boolean = false

    var ownerId:Long = 0
    var brief:String = ""
    // 群人数（可冗余，便于快速展示）
    var membersCount: Int = 0
    // chat channel map
    var chatType:String = "chat"
    // direct auth question invite
    var joinType:String = "direct"
    // public private
    var visibleType :String = "public"
    var tags :String = ""

    // 进群的问题
    var question:String = ""
    var answer:String = ""


    constructor() : super() {

    }

    constructor(
        tid: Long,
        syncId: Long,
        readId: Long,
        type: Int,
        data: Int,
        title: String?,
        icon: String?
    ) : super(tid, syncId, readId, type, data, title, icon) {
    }

    /* ---------------- admins ---------------- */
    fun getAdminIds(): List<User> {
        return admins.values.toList()
    }

    fun setAdminIds(adminIds: List<Long>?) {

    }

    fun isAdmin(uid: Long): Boolean {
        return true
    }

    /* ---------------- members ---------------- */
    fun getMembers(): List<User> {
        return members.values.toList()
    }

    fun setMemberIds(memberIds: List<Long>?) {

    }

    fun isMember(uid: Long): Boolean {
        return members.containsKey(uid)
    }

    /* ---------------- helper ---------------- */
    /**
     * 是否有群管理权限
     */
    fun canManage(uid: Long): Boolean {
        return true;
    }

    override fun toString(): String {
        return "Group{" +
                "tid=" + tid +
                ", title='" + title + '\'' +

                ", members=" + membersCount +
                ", muteAll=" + isMuteAll +
                '}'
    }
}
