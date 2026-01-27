package com.bird2fish.birdtalksdk.model

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.db.TopicDbHelper
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatMsgType
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import java.io.File
import java.util.LinkedList

enum class MessageInOut{
    IN,
    OUT
}

// 用来管理所有的会话信息，包括私聊以及群组
class ChatSession(
    topic: Topic
) : Topic(
    tid = topic.tid,
    syncId = topic.syncId,
    readId = topic.readId,
    type = topic.type,
    data = topic.getData(),
    title = topic.title,
    icon = topic.icon
)
{
    //////////////////////////////////////////////////////////////////////////
    // 好友、群组，或者系统通知
    private var friend: User? = null
    private var group: Group? = null
    private var sysFriend: User? = null
    override var title: String
        get(){
            if (type == Topic.CHAT_P2P){
                if (tid == SdkGlobalData.selfUserinfo.id){
                    val txt = SdkGlobalData.context!!.getString(R.string.me)
                    return txt
                }
                return if (friend != null && !TextUtils.isEmpty(friend!!.nick) ){
                    "${friend!!.nick} (${friend!!.id})"
                }else{
                    super.title
                }
            }else if (type == Topic.CHAT_GROUP){
                val txt = SdkGlobalData.context!!.getString(R.string.group_prefix)
                return if (group != null && !TextUtils.isEmpty(group!!.name)){
                    "$txt${group!!.name} (${group!!.gid})"
                }else{
                    txt + super.title
                }
            }else{
                return this.sysFriend?.nick ?: super.title
            }
        }
        set(value) {
            super.title = value
        }



    // 尽可能与user/group的信息同步
    override var icon :String
        get(){
            if (type == Topic.CHAT_P2P){
                return friend?.icon ?: super.icon
            }else if (type == Topic.CHAT_GROUP){
                return group?.icon ?: super.icon
            }else{
                return this.sysFriend?.icon ?: super.icon
            }
        }
        set(value) {
            super.icon = value
        }


    // 用于显示的消息列表
    var msgList: LinkedList<MessageContent> = LinkedList<MessageContent>()
    // 这个是一个标志，主要是在同步的时候会有重复
    var msgListMask : MutableMap<Long,MessageContent> = LinkedHashMap()
    // 正在发送的过程中的消息，如果消息超时了，则需要重发
    var msgSendingList: MutableMap<Long, MessageContent> = LinkedHashMap()

    var msgUnReadList : MutableMap<Long, MessageContent> = LinkedHashMap()

    //////////////////////////////////////////////////////////////////////////
    // 这里根据类型来初始化用户或者群组
    init {
        when (this.type){
            // 1
            Topic.CHAT_P2P -> {
                this.friend = UserCache.findUserSync(tid)
            }
            // 2
            Topic.CHAT_GROUP->{
                this.group = GroupCache.findGroupSync(tid)
            }
            // 0
            Topic.CHAT_SYSTEM->{
                this.sysFriend = User()
                val txt = SdkGlobalData.context!!.getString(R.string.sys_notice)
                this.sysFriend!!.name = txt
                this.sysFriend!!.nick = txt
                this.sysFriend!!.icon = "birdtalk128.png"
                this.sysFriend!!.id =100L
            }
            else -> {
                Log.e("ChatSession", "初始化发现了type = $type")
            }

        }
    }


    fun getNick():String{
        if (type == Topic.CHAT_P2P){
            return friend?.nick ?: super.title
        }else if (type == Topic.CHAT_GROUP){
            return group?.name ?: "${super.title}"
        }else{
            return this.sysFriend?.nick ?: super.title
        }
    }

    fun getGender():String{
        if (type == Topic.CHAT_P2P){
            return friend?.gender ?: ""
        }else{
            return ""
        }
    }

    // 会话的ID管理
    fun getSessionId():Long{
        when (this.type){
            // 1
            Topic.CHAT_P2P -> {
                return tid
            }
            // 2
            Topic.CHAT_GROUP->{
                return -tid
            }
            // 0
            Topic.CHAT_SYSTEM->{
                return 100
            }
            else -> {
                Log.e("ChatSession", "初始化发现了type = $type")
            }

        }
        return 0;
    }

    // 初始化时候用的
    fun loadMessageOnLogin(lst:  List<MessageData>, t:ChatSession):Long{
        // 如果没有收到服务器回执的
        val tempSendingList = LinkedList<MessageContent>()
        val tempUnReadList = LinkedList<MessageContent>()
        val tempUnRecvList = LinkedList<MessageContent>()
        var tm = 0L
        synchronized(msgList){
            var i = 0;
            for (msg in lst){
                val msgContent = if (t.isP2pChat()) TextHelper.MsgContentFromDbMessageP2p(msg, t) else TextHelper.MsgContentFromDbMessageGroup(msg, t)
                msgList.addFirst(msgContent)
                msgListMask[msgContent.msgId] = msgContent

                // 这里倒序，第一条是设置会话的最新消息
                if (i == 0){
                    // 单独设置会话
                    t.lastMsg = msgContent
                }
                i += 1

                // 发出的消息，重启后之后只有文本消息重发，其他的不重发
                if (msg.io == MessageInOut.OUT.ordinal && msg.tm1  == 0L){
                    // 文件类，无法重发，因为保存到消息不一样
                    if (msg.status == MessageStatus.UPLOADING.name)
                    {
                        msg.status = MessageStatus.FAIL.name
                    }
                    else if (msg.status == MessageStatus.SENDING.name)
                    {
                        if (msg.msgType == ChatMsgType.TEXT.name) {
                            tempSendingList.add(msgContent)
                        }
                        else{
                            // 其他类型涉及到上传，所以在重传时候可能没有数据，这里主要是比较麻烦，先这样写
                            msg.status = MessageStatus.FAIL.name
                            if (type == CHAT_P2P)
                                TopicDbHelper.insertPChatMsg(msg)
                            else if (type == CHAT_GROUP)
                                TopicDbHelper.insertGChatMsg(msg)
                        }

                    }

                }

                if (msg.io == MessageInOut.IN.ordinal && msg.tm2 == 0L){
                    tempUnRecvList.add(msgContent)
                }

                if (msg.io == MessageInOut.IN.ordinal && msg.tm3 == 0L){
                    tempUnReadList.add(msgContent)
                }

                if (msg.tm > tm){
                    tm = msg.tm
                }
            }
        }

        synchronized(msgUnReadList){
            for (item in tempUnReadList){
                msgUnReadList[item.msgId] = item
            }
            // 设置会话的未读数量
            t.unReadCount = tempUnReadList.size.toLong()
        }

        // 一会处理重新发送的
        synchronized(msgSendingList){
            for (item in tempSendingList){
                msgSendingList[item.msgId] = item
            }
        }

        // 提交回执，这种情况是刚收到消息没有提交回执就崩溃了，或者睡眠了
        for (item in tempUnRecvList) {
            MsgEncocder.sendChatReply(item.userId, item.sendId, item.msgId, false, "")
            TopicDbHelper.updatePChatReply(item.msgId, 0L, System.currentTimeMillis(), 0L, true)
        }


        return tm
    }

    // 加载的历史数据
    fun addP2PMessageToHeadOnDrag(lst:  List<MessageData>, topic:ChatSession, bRemote:Boolean) {
        val tempList = LinkedList<MessageContent>()

        synchronized(msgList) {
            // 遍历历史消息
            lst.forEach { msg ->
                // 如果 msgId 已存在，跳过，避免重复
                val message = if (topic.isP2pChat()) TextHelper.MsgContentFromDbMessageP2p(msg,topic) else TextHelper.MsgContentFromDbMessageGroup(msg,topic)
                if (!msgListMask.containsKey(message.msgId)) {
                    // 收到的消息提交回执
                    if (message.inOut && message.tm2 <= 0) {
                        MsgEncocder.sendChatReply(this.getSessionId(), msg.sendId, msg.id, true, "")
                        TopicDbHelper.updatePChatReply(message.msgId, 0, System.currentTimeMillis(), 0, true)
                        message.tm2 = System.currentTimeMillis()
                    }
                    if (message.inOut && message.tm3 <= 0){
                        tempList.add(message)
                    }

                    // 远端返回的，要写库
                    if (bRemote){
                        TopicDbHelper.insertPChatMsg(msg)
                    }

                    msgList.add(0, message)  // 插入到列表头部
                    msgListMask[message.msgId] = message

                }
            }
        }

        // 添加到未读
        synchronized(msgUnReadList){
            for (m in tempList){
                msgUnReadList[m.msgId] = m
            }
        }

    }

    //发送内嵌消息
    fun addMessageNewOut(message: MessageContent) {
        synchronized(msgList){
            msgList.add(message)
            msgListMask[message.msgId] = message
        }


        synchronized(msgSendingList){
            msgSendingList[message.msgId] =message
        }
        // 设置最新的消息
        this.lastMsg = message
        // 通知会话列表更新最新消息
        SdkGlobalData.invokeOnEventCallbacks(
            MsgEventType.MSG_COMING, message.msgType.number,
            message.msgId, message.userId, mapOf("msg" to "send") )

    }

    // 发送列表中的消息检查是否需要重发，预计不会很长，所以这里不再做拷贝
    val timeOutMili = 2 * 60 * 1000L

    fun checkResendOrFail(): Boolean {

        Log.d("ChatSession", "session=${getSessionId()}检查需要重发的条目n=${msgSendingList.size}")
        var list = LinkedList<Long>()
        var bUpdate = false
        synchronized(msgSendingList){
            for (sendId in msgSendingList.keys){
                val tmNow = System.currentTimeMillis()
                val msg = msgSendingList[sendId]!!
                if (msg.msgStatus == MessageStatus.UPLOADING){
                    // 如果在上传啥也不需要做
                    continue
                }

                if ( msg.msgStatus != MessageStatus.SENDING && msg.msgStatus != MessageStatus.UPLOADING){
                    msg.msgStatus = MessageStatus.FAIL
                    list.add(sendId)
                    continue
                }

                // 如果不相等，说明服务器给了回执
                if (msg.sendId != msg.msgId){
                    list.add(sendId)
                    msg.msgStatus = MessageStatus.FAIL
                    continue
                }

                if (tmNow - msg.tm > timeOutMili){
                    msg.msgStatus = MessageStatus.FAIL
                    bUpdate = true
                    list.add(sendId)
                    Log.d("ChatSession", "session=${getSessionId()},超时了.....  sendId=${msg.sendId}")
                }else{
                    msg.tmResend = System.currentTimeMillis()
                    ChatSessionManager.sendMsgContentToNet(msg, true)
                    Log.d("ChatSession", "session=${getSessionId()},重发送消息..... sendId= ${msg.sendId}")
                }
            }
        }
        // 尝试删除相关的KEY
        synchronized(msgSendingList){
            for (sendId in list){
                val msg = msgSendingList.remove(sendId)
                // 保存到数据库，否则下次还加载
                val data = TextHelper.MsgContentToDbMsg(msg!!)
                if (msg!!.isP2p()){

                    TopicDbHelper.insertPChatMsg(data)
                }else
                {
                    TopicDbHelper.insertGChatMsg(data)
                }

            }
        }

        return bUpdate
    }

    // 通过ID来找，这个函数没有加锁是因为仅仅为另一个函数setReply而存在
    // 这里没有枷锁，因为msgList已经枷锁了，用一个就可以了
    private fun FindMessageAndSetReply(msgid:Long, setToId:Long, tm1:Long,  tm2:Long, tm3:Long, bOk:Boolean) :MessageContent?{
        var msg: MessageContent? = null

        if (!msgListMask.containsKey(msgid)){
            return null
        }

        // 收到服务器的应答的时候，这里应该改变一下
        msg = msgListMask[msgid]
        if (msg!!.msgId != setToId) {
            msg.msgId = setToId

            msgListMask[setToId] = msg
            msgListMask.remove(msgid)

        }

        if (msg != null){
            if (msg!!.tm1 == 0L && tm1 >0){
                msg!!.tm1 = tm1
                if (msg!!.msgStatus.ordinal < MessageStatus.OK.ordinal)
                    msg!!.msgStatus = if (bOk) MessageStatus.OK else MessageStatus.FAIL
            }

            if (msg!!.tm2 == 0L && tm2 >0){

                msg!!.tm2 = tm2
                if (msg!!.msgStatus.ordinal < MessageStatus.RECV.ordinal)
                    msg!!.msgStatus = if (bOk) MessageStatus.RECV  else MessageStatus.FAIL
            }
            if (msg!!.tm3 == 0L && tm3 >0 ){
                msg!!.tm3 = tm3
                if (msg!!.msgStatus.ordinal < MessageStatus.SEEN.ordinal)
                    msg!!.msgStatus = if (bOk) MessageStatus.SEEN else MessageStatus.FAIL
            }
        }

        return msg
    }

    // 批量请求收到的的消息,是来自好友的消息
    fun onPRecvBatchMsg(messageLst: List<MessageContent>, isForward:Boolean){
        synchronized(msgList){
            for (msg in messageLst){
                if (isForward){

                    if (msgListMask.containsKey(msg.msgId)){
                        msgListMask[msg.msgId]!!.tm1 = msg.tm1
                        msgListMask[msg.msgId]!!.tm2 = msg.tm2
                        msgListMask[msg.msgId]!!.tm3 = msg.tm3
                    }else{
                        msgList.add(msg)
                    }

                }else{
                    if (msgListMask.containsKey(msg.msgId)){
                        msgListMask[msg.msgId]!!.tm1 = msg.tm1
                        msgListMask[msg.msgId]!!.tm2 = msg.tm2
                        msgListMask[msg.msgId]!!.tm3 = msg.tm3
                    }else{
                        msgList.addFirst(msg)
                    }
                }
            }
        }

        synchronized(msgUnReadList){
            for (msg in messageLst){
                msgUnReadList[msg.msgId] = msg
            }
            this.unReadCount = msgUnReadList.size.toLong()
        }

        // 聊天会话的列表中更新
        synchronized(msgList){
            this.lastMsg = msgList.last()
        }


    }


    // 这里的其实是sendId, 并不是服务器给的消息ID
    fun setReply(msgId:Long, sendId:Long, tm1:Long,  tm2:Long, tm3:Long, bOk:Boolean, detail:String) :MessageContent?{

        // 只要有回执了，就删除，不再重发了
        synchronized(msgSendingList){
            val v = msgSendingList.remove(sendId)
            if (v != null){
                Log.d("删除了程序列表中的都条目 sendId:", v.sendId.toString())
            }else{
                //Log.d("删除时候出错了，sendId=", sendId.toString())
            }

            val v1 = msgSendingList.remove(msgId)
            if (v1 != null){
                Log.d("删除了程序列表中的都条目 msgId:", msgId.toString())
            }else{
                //Log.d("删除时候出错了，msgId=", msgId.toString())
            }
            Log.d("剩余重发条目 n:", msgSendingList.size.toString())
        }
        // 这里必须使用一个原子操作，防止服务回执与用户回执顺序交错，或者同时到达
        synchronized(msgList) {
            // 如果是服务器回执，这时候需要用sendID去找，这个函数仅仅是设置内存没有保存数据库
            var msg = FindMessageAndSetReply(sendId, msgId, tm1, tm2, tm3, bOk)
            if (msg != null) {  // 通过SENDID找到的情况，是服务器应答的时候
                val msgData = TextHelper.MsgContentToDbMsg(msg)
                if (msg.isP2p()){
                    TopicDbHelper.insertPChatMsgAgain(msgData)
                }else{
                    TopicDbHelper.insertGChatMsgAgain(msgData)
                }


                return msg
            }


            // 内存已经同步了服务器分配的MSGID
            // 不过这里基本都是
            msg = FindMessageAndSetReply(msgId, msgId, tm1, tm2, tm3, bOk)
            if (msg != null) {
                if (msg.isP2p()){
                    TopicDbHelper.updatePChatReply(msgId, tm1, tm2, tm3, bOk)
                }else{
                    TopicDbHelper.updateGChatReply(msgId, tm1)
                }

            } else {
                // 这里出现错误了
                Log.e("Sdk", "can't find msg by msgid=" + msgId.toString())
            }
            return msg
        }
    }

    // 先发送上传的消息
//    fun sendUploading(message: MessageContent){
//        synchronized(msgSendingList){
//            msgSendingList[message.msgId] = message
//        }
//
//    }

    // 添加消息到会话尾部（收到新消息时）
    fun addMessageToTail(message: MessageContent) {
        synchronized(msgList) {
            msgList.add(message)
            msgListMask[message.msgId] = message
        }

        synchronized(msgUnReadList){
            msgUnReadList[message.msgId] = message
        }
        this.lastMsg = message
    }

    // 检查未读的消息，把已读的都发送回执
    fun checkUnRead(first:Int, last:Int){
        synchronized(msgUnReadList){
            if (msgUnReadList.isEmpty())
                return
        }
        var from = first
        if (from < 0)
            from = 0

        // 群组不处理
        if (type != Topic.CHAT_P2P)
            return

        val lst = LinkedList<MessageContent>()
        synchronized(msgList) {
            for (i in from..last) {
                if (last > msgList.size - 1)
                    break

                val msg = msgList[i]
                // 如果是出去的数据不监控的
                if (!msg.inOut)
                {
                    TopicDbHelper.deleteFromPChatUnread(msg.msgId)
                    continue
                }

                if (!msg.isRead){

                    msg.tm2 = System.currentTimeMillis()
                    msg.tm3 = System.currentTimeMillis()
                    lst.add(msg)
                }
            }
        }

        // 更新数据库
        for (msg in lst){
            MsgEncocder.sendChatReply(msg.userId, msg.sendId, msg.msgId, true, "")
            TopicDbHelper.updatePChatReply(msg.msgId, msg.tm1, msg.tm2, msg.tm3, true)
            // 删除这个监控条目
            TopicDbHelper.deleteFromPChatUnread(msg.msgId)
        }

        // 清除未读列表
        synchronized(msgUnReadList){
            for (msg in lst){
                msgUnReadList.remove(msg.msgId)
            }
        }
    }

    // 在头部插入消息（加载历史消息时）
    fun addMessageToHead(message: MessageContent) {
        synchronized(msgList) {
            msgList.addFirst(message)
            msgListMask[message.msgId] =message
        }
    }




    // 获取未读消息数量
    fun getUnreadCount(): Int {
        synchronized(msgList) {
            return msgList.count { it.tm3 == 0L }
        }
    }

    // 标记所有消息为已读
    fun markAllAsRead() {
        synchronized(msgList) {
            msgList.forEach { it.tm3 = System.currentTimeMillis();  }
        }
    }

    // 标记指定消息ID为已读
    fun markAsRead(messageId: Long, tm:Long) {
        synchronized(msgList) {
            msgList.find { it.msgId == messageId }?.tm3 = tm
        }
    }

    // 获取最后一条消息
    fun getLastMessage(): MessageContent? {
        synchronized(msgList) {
            return msgList.lastOrNull()
        }
    }

    // 获取消息总数
    fun getMessageCount(): Int {
        synchronized(msgList) {
            return msgList.size
        }
    }

    // 清除会话所有消息
    fun clearAllMessages() {
        synchronized(msgList) {
            msgList.clear()
        }
    }

    fun isP2pChat():Boolean{
        return (type == Topic.CHAT_P2P)
    }

    fun isGroupChat():Boolean{
        return (type == Topic.CHAT_GROUP)
    }

//    fun notice(){
//        // 收到消息了，自然要更新会话界面；
//        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_CHAT_SESSION, 0, msg.msgId, msg.userId, mapOf("msg" to "p2p"))
//
//        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_CHAT_SESSION, 0, 0, 0, mapOf("t" to "group"))
//
//        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_CHAT_SESSION, 0, 0, 0, mapOf("t" to "p2p"))
//    }

    // 根据消息ID查找消息
    fun findMessageById(messageId: Long): MessageContent? {
        synchronized(msgList) {
            return msgList.find { it.msgId == messageId }
        }
    }

    fun sendTextMessageOut(message:String, refMsgId:Long = 0L){
        if (this.type == Topic.CHAT_P2P || this.type == Topic.CHAT_GROUP)
            ChatSessionManager.sendTextMessageOut(this, message, refMsgId)
    }

    fun sendImageMessageUploading(context: Context, uri: Uri, refMsgId:Long = 0L):Long{
        if (this.type == Topic.CHAT_P2P || this.type == Topic.CHAT_GROUP)
            return ChatSessionManager.sendImageMessageUploading(this, context, uri, refMsgId)

        return 0L
    }

    fun sendAudioOut(sessionId:Long, context: Context, draft:Drafty, bits: ByteArray?, mAudioFile: File, refMsgId:Long = 0L):Long{
        if (this.type == Topic.CHAT_P2P || this.type == Topic.CHAT_GROUP)
            return ChatSessionManager.sendAudioOut(this, context, draft, bits, mAudioFile, refMsgId)
        return 0L
    }
}
////////////////////////////////////////////////////////////////////////////////
