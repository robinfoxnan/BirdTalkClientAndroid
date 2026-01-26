package com.bird2fish.birdtalksdk.model
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatType

/**
 * 会话 / 话题实体
 * 数据库中有2个表,p_topic和g_topic
 * 分别存储;
 */
open class Topic(

        /* ================= 表字段 ================= */

        var tid: Long = 0L,
        var syncId: Long = 0L,
        var readId: Long = 0L,

        var type: Int = 0,
        private var data: Int = 1,            // flag 位（VISIBLE 默认 1）

        title: String = "",
        icon: String = DEFAULT_ICON
) {


    @Volatile
    private var _chatSession: ChatSession? = null

    fun asChatSession(): ChatSession {
        return _chatSession ?: synchronized(this) {
            _chatSession ?: ChatSession(this).also {
                _chatSession = it
            }
        }
    }

    /* ================= 展示字段 ================= */

    open var title: String = title.ifEmpty { "" }
    set(value) {
        field = value.ifEmpty { "" }
    }

    open var icon: String = icon.ifEmpty { DEFAULT_ICON }
    set(value) {
        field = value.ifEmpty { DEFAULT_ICON }
    }

    /* ================= 运行态字段（非表） ================= */

    var unReadCount: Long = 0
    var lastMsg: MessageContent? = null

    var mute: Boolean = false
        private set

    var showHide: Boolean = true
    private set

    var pinned: Boolean = false
    private set

    init {
        // 根据 data 同步状态位
        syncFlagsFromData()
    }

    /* ================= flag 同步 ================= */

    fun setData(mask: Int) {
        data = mask
        syncFlagsFromData()
    }

    fun getData(): Int = data

    private fun syncFlagsFromData() {
        mute = (data and TopicFlag.MUTE) != 0
        showHide = (data and TopicFlag.VISIBLE) != 0
        pinned = (data and TopicFlag.PINNED) != 0
    }

    private fun updateFlag(flag: Int, enable: Boolean) {
        data = if (enable) {
            data or flag
        } else {
            data and flag.inv()
        }
        syncFlagsFromData()
    }

    /* ================= 对外控制方法 ================= */

    fun setMute(enable: Boolean) {
        updateFlag(TopicFlag.MUTE, enable)
    }

    fun setPinned(enable: Boolean) {
        updateFlag(TopicFlag.PINNED, enable)
    }

    fun setShowHide(visible: Boolean) {
        updateFlag(TopicFlag.VISIBLE, visible)
    }

    /* ================= 便捷方法 ================= */

    fun getTm(): Long {
        return lastMsg?.tm ?: 0L
    }

    override fun toString(): String {
        return "Topic(" +
                "tid=$tid, " +
                "syncId=$syncId, " +
                "readId=$readId, " +
                "type=$type, " +
                "data=$data, " +
                "title='$title', " +
                "icon='$icon', " +
                "unReadCount=$unReadCount, " +
                "lastMsg=${lastMsg ?: "null"}, " +
                "mute=$mute, " +
                "pinned=$pinned, " +
                "showHide=$showHide" +
                ")"
    }

    // 默认的
    companion object {
        const val DEFAULT_ICON = ""
        val CHAT_P2P = ChatType.ChatTypeP2P.number
        val CHAT_GROUP = ChatType.ChatTypeGroup.number
        var CHAT_SYSTEM = 0
    }


}
