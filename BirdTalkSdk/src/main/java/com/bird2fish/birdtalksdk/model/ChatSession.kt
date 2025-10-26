package com.bird2fish.birdtalksdk.model

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.db.TopicDbHelper
import com.bird2fish.birdtalksdk.db.UserDbHelper
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.net.Session
import com.bird2fish.birdtalksdk.net.WebSocketClient
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatMsgType
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatType
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.net.URI
import java.util.HashMap
import java.util.LinkedList
import kotlin.math.max


// 会话类型枚举
enum class SessionType {
    PRIVATE_CHAT, // 私聊
    GROUP_CHAT // 群组聊天
}
enum class MessageInOut{
    IN,
    OUT
}

// 用来管理所有的会话信息，包括私聊以及群组
class ChatSession (val sessionId:Long)
{
    var sessionType: SessionType = SessionType.PRIVATE_CHAT  // 会话类型
    var sessionTitle:String = ""
    var sessionIcon :String = ""

    init {
        // 将TOPIC和用户的信息对应起来
        if (sessionId > 0){
           this.sessionType = SessionType.PRIVATE_CHAT

            // 这里的用户是数据库中搜索出来的，因为建立会话不一定就是好友，比如已经删除了好友，防止出错
            var friend = UserDbHelper.getUserById(sessionId)
            if (friend == null){

                SdkGlobalData.userCallBackManager.invokeOnErrorCallbacks(com.bird2fish.birdtalksdk.InterErrorType.FRIEND_INFO_ERROR,
                    "UserDbHelper.getUserById", "friend", "can't find user info in db")
            }else{
                // 确保这个Topic 应该有，这里图标与用户保持一致就可以了
                val topic = SdkGlobalData.getTopic(friend)

                if (topic != null){
                    this.sessionTitle = topic.title
                    this.sessionIcon = topic.icon
                    if (sessionTitle.isEmpty()){
                        sessionTitle = friend.id.toString()
                    }
                }else{
                    this.sessionTitle = friend.nick?: ""
                    this.sessionIcon = friend.icon?: ""
                    if (sessionTitle.isEmpty()){
                        sessionTitle = friend.id.toString()
                    }
                }
            }

        }else{
           this.sessionType = SessionType.GROUP_CHAT
            TODO()
        }
    }

    // 用于显示的消息列表
    var msgList: LinkedList<MessageContent> = LinkedList<MessageContent>()

    // 正在发送的过程中的消息，如果消息超时了，则需要重发
    var msgSendingList: MutableMap<Long, MessageContent> = LinkedHashMap()

    var msgUnReadList : MutableMap<Long, MessageContent> = LinkedHashMap()

    // 初始化时候用的
    fun loadMessage(lst:  List<MessageData>, t:Topic):Long{
        // 如果没有收到服务器回执的
        val tempSendingList = LinkedList<MessageContent>()
        val tempUnReadList = LinkedList<MessageContent>()
        val tempUnRecvList = LinkedList<MessageContent>()
        var tm = 0L
        synchronized(msgList){
            var i = 0;
            for (msg in lst){
                val msgContent = TextHelper.MsgContentFromDbMessage(msg, t)
                msgList.add(msgContent)

                // 这里是设置会话的最新消息
                if (i == lst.size - 1){
                    t.lastMsg = msgContent
                }
                i += 1


                if (msg.io == MessageInOut.OUT.ordinal && msg.tm1  == 0L){
                    tempSendingList.add(msgContent)
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
            TopicDbHelper.updatePChatReply(item.msgId, 0L, System.currentTimeMillis(), 0L)
        }


        return tm
    }

    //发送内嵌消息
    fun addMessageNewOut(message: MessageContent) {
        synchronized(msgList){
            msgList.add(message)
        }


        synchronized(msgSendingList){
            msgSendingList[message.msgId] =message
        }
    }

    // 发送列表中的消息检查是否需要重发，预计不会很长，所以这里不再做拷贝
    val timeOutMili = 2 * 60 * 1000L

    fun checkResendOrFail(): Boolean {

        var list = LinkedList<Long>()
        var bUpdate = false
        synchronized(msgSendingList){
            for (sid in msgSendingList.keys){
                val tmNow = System.currentTimeMillis()
                val msg = msgSendingList[sid]!!
                if ( msg.msgStatus != MessageStatus.SENDING
                    && msg.msgStatus != MessageStatus.UPLOADING){
                    list.add(sid)
                    continue
                }

                if (tmNow - msg.tm > timeOutMili){
                    msg.msgStatus = MessageStatus.FAIL
                    bUpdate = true
                    list.add(sid)
                    Log.d("ChatSession", "timeout msg.....${msg.msgId}")
                }else{
                    msg.tmResend = System.currentTimeMillis()
                    ChatSessionManager.sendMsgContent(sid, msg)
                    Log.d("ChatSession", "resend msg.....${msg.msgId}")
                }
            }
        }
        // 尝试删除相关的KEY
        synchronized(msgSendingList){
            for (sid in list){
                msgSendingList.remove(sid)
            }
        }

        return bUpdate
    }

    // 通过ID来找，这个函数没有加锁是因为仅仅为另一个函数setReply而存在
    private fun FindMessageAndSetReply(msgid:Long, setToId:Long, tm1:Long,  tm2:Long, tm3:Long) :MessageContent?{
        var msg: MessageContent? = null

            for (item in msgList) {
                if(item.msgId == msgid){
                    msg = item
                    // 在这里设置，防止接收回执太快
                    if (item.msgId != setToId){
                        item.msgId = setToId
                    }
                    break
                }
            }


        if (msg != null){

            if (msg!!.tm1 == 0L && tm1 >0){
                msg!!.tm1 = tm1
                if (msg!!.msgStatus.ordinal < MessageStatus.OK.ordinal)
                    msg!!.msgStatus = MessageStatus.OK
            }

            if (msg!!.tm2 == 0L && tm2 >0){
                msg!!.bRecv = true
                msg!!.tm2 = tm2
                if (msg!!.msgStatus.ordinal < MessageStatus.RECV.ordinal)
                    msg!!.msgStatus = MessageStatus.RECV
            }
            if (msg!!.tm3 == 0L && tm3 >0 ){
                msg!!.bRead = true
                msg!!.tm3 = tm3
                if (msg!!.msgStatus.ordinal < MessageStatus.SEEN.ordinal)
                    msg!!.msgStatus = MessageStatus.SEEN
            }
        }

        return msg
    }

    // 批量请求收到的的消息,是来自好友的消息
    fun onPRecvBatchMsg(tid:Long, messageLst: List<MessageContent>, isForward:Boolean){

        val topic = SdkGlobalData.getPTopic(tid)
        if (topic == null){
            return
        }

        synchronized(msgList){
            var i = 0
            for (msg in messageLst){
                if (isForward){
                    msgList.add(msg)
                    if (i == messageLst.size -1){
                        topic.lastMsg = msg
                    }
                }else{
                    msgList.addFirst(msg)
                }
            }
        }

        synchronized(msgUnReadList){
            for (msg in messageLst){
                msgUnReadList[msg.msgId] = msg
            }
            topic.unReadCount = msgUnReadList.size.toLong()
        }

    }

    // 这里的其实是sendId, 并不是服务器给的消息ID
    fun setReply(msgId:Long, sendId:Long, tm1:Long,  tm2:Long, tm3:Long) :MessageContent?{

        // 只要有回执了，就删除，不再重发了
        synchronized(msgSendingList){
            msgSendingList.remove(sendId)
        }
        // 这里必须使用一个原子操作，防止服务回执与用户回执顺序交错，或者同时到达
        synchronized(msgList) {
            // 如果是服务器回执，这时候需要用sendID去找
            var msg = FindMessageAndSetReply(sendId, msgId, tm1, tm2, tm3)
            if (msg != null) {  // 通过SENDID找到的情况，是服务器应答的时候
                val msgData = TextHelper.MsgContentToDbMsg(msg, this.sessionId)
                TopicDbHelper.insertPChatMsgAgain(msgData)

                return msg
            }


            // 内存已经同步了服务器分配的MSGID
            msg = FindMessageAndSetReply(msgId, msgId, tm1, tm2, tm3)
            if (msg != null) {
                TopicDbHelper.updatePChatReply(msgId, tm1, tm2, tm3)
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
        }

        synchronized(msgUnReadList){
            msgUnReadList[message.msgId] = message
        }
    }

    // 检查未读的消息
    fun checkUnRead(first:Int, last:Int){
        synchronized(msgUnReadList){
            if (msgUnReadList.isEmpty())
                return
        }
        var from = first
        if (from < 0)
            from = 0

        // 群组不处理
        if (sessionId < 0)
            return

        val lst = LinkedList<MessageContent>()
        synchronized(msgList) {
            for (i in from..last) {
                if (last > msgList.size - 1)
                    break

                val msg = msgList[i]
                if (!msg.inOut)
                    continue

                if (msg.bRead == false){

                    msg.bRead =true
                    msg.bRecv = true
                    msg.tm3 = System.currentTimeMillis()

                    lst.add(msg)
                }
            }
        }

        // 更新数据库
        for (msg in lst){
            MsgEncocder.sendChatReply(msg.userId, msg.sendId, msg.msgId, true, "")
            TopicDbHelper.updatePChatReply(msg.msgId, msg.tm1, msg.tm2, msg.tm3)
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
        }
    }

    // 批量添加历史消息
    fun addHistoricalMessages(messages: List<MessageContent>) {
        // 假设消息是按时间正序排列的，需要逆序插入到头部
        synchronized(msgList) {
            messages.reversed().forEach { msgList.addFirst(it) }
        }
    }

    // 获取未读消息数量
    fun getUnreadCount(): Int {
        synchronized(msgList) {
            return msgList.count { !it.bRead }
        }
    }

    // 标记所有消息为已读
    fun markAllAsRead() {
        synchronized(msgList) {
            msgList.forEach { it.bRead = true }
        }
    }

    // 标记指定消息ID为已读
    fun markAsRead(messageId: Long) {
        synchronized(msgList) {
            msgList.find { it.msgId == messageId }?.bRead = true
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

    // 根据消息ID查找消息
    fun findMessageById(messageId: Long): MessageContent? {
        synchronized(msgList) {
            return msgList.find { it.msgId == messageId }
        }
    }

}
////////////////////////////////////////////////////////////////////////////////
// 会话管理工具类
object ChatSessionManager {
    private val sessions = mutableMapOf<Long, ChatSession>() // 会话ID到会话的映射

    // 保存上传的图片的基础信息
    var uploadingMap: MutableMap<Long, MessageContent> = LinkedHashMap()
    var bInit = false


    // 获取会话
    fun getSession(sessionId: Long): ChatSession {
        synchronized(sessions){
            if (sessions.containsKey(sessionId))
                return sessions[sessionId]!!
            else{
                val s = ChatSession(sessionId)
                sessions[sessionId] = s
                return s
            }
        }

    }

    // 删除会话
    fun deleteSession(sessionId: Long) {
        synchronized(sessions) {
            sessions.remove(sessionId)
        }
    }

    // 获取所有会话列表
    fun getAllSessions(): List<ChatSession> {
        // 按最后一条消息时间排序，最新的在前面
        synchronized(sessions){
            return sessions.values.sortedByDescending { it.getLastMessage()?.tm ?: 0 }
        }

    }

    // 获取未读消息总数
    fun getTotalUnreadCount(): Int {
        synchronized(sessions) {
            return sessions.values.sumOf { it.getUnreadCount() }
        }
    }


    // 检查所有没有收到回执的消息，如果超时了就设置发送失败
    fun checkResendOrFail(){
        val lst = getAllSessions()
        var bUpdate = false
        for (s in lst){
            val b = s.checkResendOrFail()
            bUpdate = b && bUpdate
        }

        // 通知界面刷新超时的消息
        if (bUpdate){
            val params = mapOf("send" to "fail")
            SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.MSG_SEND_ERROR, 0, 0L, 0L, params)
        }
    }

    // 提交消息到网络层，重发也在这里
    fun sendMsgContent(sessionId:Long, msg:MessageContent){

        GlobalScope.launch(Dispatchers.IO) {

            // 1) 先写数据库,更新到发送表
            val msgData = TextHelper.MsgContentToDbMsg(msg, sessionId)

            if (msg.isP2p){
                TopicDbHelper.insertPChatMsg(msgData)
            }else{
                TopicDbHelper.insertGChatMsg(msgData)
            }

            val chatType = if (sessionId > 0) ChatType.ChatTypeP2P else ChatType.ChatTypeGroup

            // 2) 提交到网络
            if (msg.msgType == ChatMsgType.TEXT) {
                MsgEncocder.sendChatMsg(
                    msg.msgId,
                    msg.userId,
                    chatType,
                    ChatMsgType.TEXT,
                    msg.text,
                    msg.msgRefId
                )
            } else {
                MsgEncocder.sendChatMsg(
                    msg.msgId,
                    msg.userId,
                    chatType,
                    msg.msgType,
                    msg.contentOut,
                    msg.msgRefId
                )
            }
        }

    }

    // 发送文本消息，添加到列表中，同时发送消息
    fun sendTextMessageOut(sessionId:Long, message:String, refMsgId:Long = 0L){
        val sendId = SdkGlobalData.nextId()
        val chatSession = getSession(sessionId)

        val uid = if (sessionId > 0) sessionId else -sessionId
        val msg = MessageContent(sendId, sendId, uid, chatSession.sessionTitle, chatSession.sessionIcon,
            UserStatus.ONLINE,
            MessageStatus.SENDING,
            false,
            false,
            false,
            message,
            null)
        msg.msgType = ChatMsgType.TEXT

        // 1)写入数据库

        // 2）加入列表
        chatSession?.addMessageNewOut(msg)

        // 更新最新消息
        SdkGlobalData.updateSession(msg)

        // 3）发送到服务器
        this.sendMsgContent(sessionId, msg)

        // 4 设置最新消息
        SdkGlobalData.updateSession(msg)
        return
    }

    // 使用嵌入图片的消息先放到内存消息中占位，然后上传图片，等待上传结束
    fun sendImageMessageUploading(sessionId:Long, context: Context, uri: Uri):Long{
        val contentResolver: ContentResolver = context.contentResolver

        val sendId = SdkGlobalData.nextId()
        val chatSession = getSession(sessionId)


        val uId = if (sessionId > 0) sessionId else -sessionId


        val draft = Drafty("")
        try {

            //二进制方式
            var fileName = TextHelper.getFileNameFromUri(context, uri)
            if (fileName == null) {
                fileName = ""
            }

            val mime = TextHelper.getMimeTypeFromUri(context, uri)
            draft.insertLocalImage(context, contentResolver, uri, fileName, mime) ?: return 0L

            //Log.d("文件内容", "draft: ${draft.toPlainText()}")

            val msg = MessageContent(sendId, sendId,
                uId, chatSession.sessionTitle, chatSession.sessionIcon,
                UserStatus.ONLINE, MessageStatus.UPLOADING, false, false, false,"", draft,  System.currentTimeMillis(), uri, mime)
            msg.msgType = ChatMsgType.IMAGE

            val sz = TextHelper.getFileSize(context, uri)
            msg.fileSz = sz

            // 得保存到临时列表，等待上传结束
            synchronized(this.uploadingMap){
                this.uploadingMap[sendId] = msg
            }
            // 异步上传
            Session.uploadSmallFile(context, uri,  uId, sendId)

            // 先写数据库,更新到发送表
            val msgData = TextHelper.MsgContentToDbMsg(msg, sessionId)

            if (msg.isP2p){
                TopicDbHelper.insertPChatMsg(msgData)
            }else{
                TopicDbHelper.insertGChatMsg(msgData)
            }
            // 添加到界面列表
            chatSession!!.addMessageNewOut(msg)

        } catch (e: FileNotFoundException) {
            Log.e("ImageDetails", "File not found: $e")
            return 0L
        } catch (e: Exception) {
            Log.e("ImageDetails", "Error while fetching image details: $e")
            return 0L
        }

        return sendId
    }

    // 将文件上传后发送消息，与那个图片不一样在于处理draft方式不同
    fun sendFileMessageUploading(sessionId:Long, context: Context, uri: Uri):Long{
        val contentResolver: ContentResolver = context.contentResolver

        val sendId = SdkGlobalData.nextId()
        val chatSession = getSession(sessionId)


        //二进制方式
        var fileName = TextHelper.getFileNameFromUri(context, uri)
        if (TextUtils.isEmpty(fileName)) {
            fileName = "file"
        }

        //Log.d("文件内容", "draft: ${draft.toPlainText()}")
        val mime = TextHelper.getMimeTypeFromUri(context, uri)
        val sz = TextHelper.getFileSize(context, uri)

        val uId = if (sessionId > 0) sessionId else -sessionId



        val draft = Drafty("")
        //draft.attachFile(mime, bits, fileName);
        draft.attachFile(mime, fileName, uri.toString(), sz,sendId, sessionId);

        val msg = MessageContent(sendId, sendId,
            chatSession.sessionId, chatSession.sessionTitle, chatSession.sessionIcon,
            UserStatus.ONLINE, MessageStatus.UPLOADING, false, false, false,"", draft,  System.currentTimeMillis(), uri, mime)
        msg.fileSz = sz
        msg.msgType = ChatMsgType.FILE

        val (hasAttach, sz1) = hasAttachment(draft)
        Log.d("SendFile", "文件大小为：${sz1}")

        // 得保存到临时列表，等待上传结束
        synchronized(this.uploadingMap){
            this.uploadingMap[sendId] = msg
        }
        // 异步上传
        //Session.uploadSmallFile(context, uri,  uId, msgId)
        msg.fileHashCode = Session.uploadFileChunk(context, uri,  uId, sendId, 0, "")

        // 先写数据库,更新到发送表
        val msgData = TextHelper.MsgContentToDbMsg(msg, chatSession.sessionId)

        if (msg.isP2p){
            TopicDbHelper.insertPChatMsg(msgData)
        }else{
            TopicDbHelper.insertGChatMsg(msgData)
        }

        // 添加到界面列表
        chatSession!!.addMessageNewOut(msg)

        return sendId
    }

    // 计算上传进度, 收到上一片的回执再发送下一片，这里其实是支持并发多个文件的
    fun onUploadFileProcess(msgId:Long, fileName:String, uuid:String, index:Int, params:Map<String, String>){
        var msg:MessageContent? = null
        synchronized(this.uploadingMap){
            msg = this.uploadingMap[msgId]
        }

        if (msg != null){
            val sessionId = msg!!.userId
            val sz = msg!!.fileSz.toFloat()
            val percent:Float = (index+1).toFloat() * 1024*1024 * 100   /sz;

            val draft = msg!!.content
            setAttachmentProcess(draft, fileName, percent.toInt())
            Session.uploadFileChunk(SdkGlobalData.context!!, msg!!.fileUri, msg!!.userId, msgId, index + 1, msg!!.fileHashCode)

            SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.MSG_UPLOAD_PROCESS, percent.toInt(), msgId, 0L, params)
        }

    }

    // 发送语音
    fun sendAudioOut(sessionId:Long, context: Context, draft:Drafty):Long{

        val sendId = SdkGlobalData.nextId()
        val chatSession = getSession(sessionId)


        val uId = if (sessionId > 0) sessionId else -sessionId

        val msg = MessageContent(sendId, sendId,
            uId, chatSession.sessionTitle, chatSession.sessionIcon,
            UserStatus.ONLINE, MessageStatus.SENDING,false, false, false, "", draft)
        msg.msgType = ChatMsgType.VOICE
        val txt = TextHelper.serializeDrafty(draft)
        msg.contentOut = txt
        Log.d("send audio drafty", txt)
        // 1)写入数据库

        // 2）加入列表
        chatSession?.addMessageNewOut(msg)

        // 3）发送到服务器
        this.sendMsgContent(sessionId, msg)

        // 4 设置最新消息
        SdkGlobalData.updateSession(msg)
        return  sendId
    }

    fun hasAttachment(drafty: Drafty?, filename: String? = null): Pair<Boolean, Long?> {
        if (drafty?.ent.isNullOrEmpty()) return Pair(false, null)

        for (entity in drafty!!.ent!!) {
            if (entity?.tp == null || entity.data == null) continue

            // 只检测附件类型
            if (entity.tp == "EX" || entity.tp == "FI") {
                val name = entity.data["name"] as? String
                val sizeObj = entity.data["size"]
                val size: Long? = when (sizeObj) {
                    is Number -> sizeObj.toLong()
                    is String -> sizeObj.toLongOrNull()
                    else -> null
                }

                if (!filename.isNullOrEmpty()) {
                    if (name != null && name == filename) {
                        return Pair(true, size)
                    }
                } else {
                    // 不指定文件名，只要存在附件就返回第一个附件的大小
                    return Pair(true, size)
                }
            }
        }

        return Pair(false, null)
    }

    // 设置本地的附件上传进度
    fun setAttachmentProcess(drafty: Drafty?, filename: String? = null, percent:Int):Boolean{
        if (drafty?.ent.isNullOrEmpty()) return false
        for (entity in drafty!!.ent!!) {
            if (entity?.tp == null || entity.data == null) continue

            // 只检测附件类型
            if (entity.tp == "EX") {
                val name = entity.data["name"] as? String
                if (name == filename){
                    entity.data["cur"] = percent
                    return true
                }
            }
        }
        return false
    }

    fun hasImage(drafty: Drafty?): Boolean {
        if (drafty?.ent == null) {
            return false
        }

        for (entity in drafty.ent) {
            if (entity?.tp == null) continue
            if (entity.tp.equals("IM") ) {
                return true
            }
        }
        return false
    }

    // 上传结束了，这个时候需要发送消息
    fun onUploadFileFinish(msgId:Long, result:String, detail:String, fileName:String, uuidName:String){

        var msg : MessageContent? = null
        synchronized(this.uploadingMap){
            msg = this.uploadingMap[msgId]
            this.uploadingMap.remove(msgId)
        }

        if (msg == null){
            Log.e("onUploadFileReply", "can't find message in queue")
            return
        }

        // 这里的userId就是sessionId
        val chatSession = getSession(msg!!.userId)

        msg!!.msgStatus = MessageStatus.SENDING
        //msg!!.content = draft
        //val chatType = if (msg!!.userId > 0) ChatType.ChatTypeP2P else ChatType.ChatTypeGroup

        val draft = Drafty("")

        // "image/jpeg"
        val fullUrl = WebSocketClient.instance!!.getRemoteFilePath(uuidName)
        var draftInMsg = msg!!.content

        // 如果是图片
        if (hasImage(draftInMsg)){
            //draft.insertImage(0,"image/jpeg", null, 884, 535, "",
            draft.insertImage(0, msg!!.mime, null, 0, 0, "",
                URI(fullUrl), URI(fullUrl), 0)

            val txt = TextHelper.serializeDrafty(draft)
            Log.d("send image or file drafty", txt)
            msg!!.contentOut = txt

            this.sendMsgContent(chatSession.sessionId, msg!!)

            // 4 设置最新消息
            SdkGlobalData.updateSession(msg!!)
            return
        }

        // 解构返回值
        val (hasAttach, sz) = hasAttachment(draftInMsg)

        if (hasAttach) {

            setAttachmentProcess(draftInMsg, fileName, 100)

            // 调用 attachFile 时传入文件大小
            draft.attachFile(msg!!.mime, fileName, fullUrl, sz ?: 0L, msgId, SdkGlobalData.selfUserinfo.id)

            // 序列化 Drafty
            val txt = TextHelper.serializeDrafty(draft)
            Log.d("send image or file drafty", txt)

            // 发送消息
            msg!!.contentOut = txt
            this.sendMsgContent(chatSession.sessionId, msg!!)

            // 4 设置最新消息
            SdkGlobalData.updateSession(msg!!)

            return
        }

        // 如果是大的语音片

    }

    // 如果上传文件错误了, 这个函数还没有测试过
    fun onUploadFileError(msgId:Long, result:String, detail:String, fileName:String, uuidName:String){
        var msg : MessageContent? = null
        synchronized(this.uploadingMap){
            msg = this.uploadingMap[msgId]
        }

        if (msg == null){
            Log.e("onUploadFileReply", "can't find message in queue")
            return
        }

        // 这里的userId就是sessionId
        val chatSession = getSession(msg!!.userId)

        msg!!.msgStatus = MessageStatus.FAIL
        //msg!!.content = draft
        val chatType = if (msg!!.userId > 0) ChatType.ChatTypeP2P else ChatType.ChatTypeGroup
        var draftMsg = msg!!.content

        // 先写数据库,更新到发送表
        val msgData = TextHelper.MsgContentToDbMsg(msg!!, chatSession.sessionId)

        if (msg!!.isP2p){
            TopicDbHelper.insertPChatMsg(msgData)
        }else{
            TopicDbHelper.insertGChatMsg(msgData)
        }

        setAttachmentProcess(draftMsg, fileName, -1)
    }

    // 处理收到的数据，
    // 1）保存数据库，2）回执，回执后更新数据库，3）添加到会话的消息列表中
    fun onRecvChatMsg(chatMsg: MsgOuterClass.MsgChat):MessageContent{
        // 1) 写数据库
        val fid = chatMsg.fromId
        val msgData = TextHelper.pbMsgToDbMsg(fid, chatMsg)

        if (chatMsg.chatType == ChatType.ChatTypeP2P){
            TopicDbHelper.insertPChatMsg(msgData)

        }else{
            TopicDbHelper.insertGChatMsg(msgData)
        }


        //2）接受回执，回执后更新数据库

        MsgEncocder.sendChatReply(fid, chatMsg.sendId, chatMsg.msgId, false, "")

        // 更新第3个选项
        val tm2= System.currentTimeMillis()
        TopicDbHelper.updatePChatReply(chatMsg.msgId, 0L, tm2, 0L)


        //3）添加到界面的列表里
        var sId = chatMsg.fromId
        if (chatMsg.chatType == MsgOuterClass.ChatType.ChatTypeP2P){

        }else{
            sId = -chatMsg.fromId
        }

        val chatSession = ChatSessionManager.getSession(sId)
        val msg = TextHelper.pbMsg2MessageContent(sId, chatMsg)
        chatSession.addMessageToTail(msg!!)

        // 需要创建topic 并设置最新消息
        SdkGlobalData.updateSession(msg!!)

        val resultMap = mapOf(
            "status" to "coming",
        )

        // 通知界面更新消息，已经保存处理完了
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.MSG_COMING, chatMsg.msgType.number,
            chatMsg.msgId, chatMsg.fromId, resultMap)
        return msg!!
    }

    // 回执, 先写到数据库，然后同步到内存，区别私聊和群聊，群聊只有一个回执
    fun onChatMsgReply(msgId:Long, sendId:Long, fid:Long, params:Map<String, String>, tmServer:Long, tmRecv:Long,
                       tmRead:Long, result:String?){
        // 这里更新回执
        val detail = params?.get("detail")
        val gidStr = params["gid"]

        // 群聊，只有服务器应答，私聊才有用户应答
        if (gidStr != "0"){
            // 群组的服务器应答
        }else{
            if (result == "ok")
            {
                val chatSession = getSession(fid)
                chatSession.setReply(msgId, sendId, tmServer, tmRecv, tmRead)
            }
        }

    }

    // 这里是全局数据的索引
    fun loadMessageOnLogin(chatTopicList :LinkedHashMap<Long, Topic>){
        var lstTm = 0L
        if (!bInit){
            lstTm = loadPMessageFormDb(chatTopicList)
            bInit = true
        }
    }
    // 启动时候需要从数据库加载消息
    fun loadPMessageFormDb(chatTopicList :LinkedHashMap<Long, Topic>):Long{
        val max = Long.MAX_VALUE
        var lastTm = 0L

        var lst : List<MessageData>
        for (p in chatTopicList){
            val sid = p.key
            val t = p.value
            val chatSession = getSession(sid)

            if (t.type == ChatType.ChatTypeP2P.number){
                lst = TopicDbHelper.getPChatMessagesById(t.tid, max, 200, false)
            }else{
                lst = TopicDbHelper.getGChatMessagesById(t.tid, max, 200, false)
            }
            // 将数据库里的数据插入到列表中
            val tm = chatSession.loadMessage(lst, t)
            if (tm > lastTm){
                lastTm = tm
            }
        }

        loadPMessageFromServer(lastTm)

        return lastTm
    }


    // 重链接成功后需要与服务器
    fun loadPMessageFromServer(lastId:Long){

        // 找出当前私聊或者群聊中最后一条记录
        val maxId = TopicDbHelper.getPChatLastId()
        if (maxId > 0){
            // 说明此时表中有数据，应该正向查找
            MsgEncocder.sendSynPChatDataForward(maxId)
        }else{
            // 没有数据说明是多终端登录或者的清理过数据
            MsgEncocder.sendSynPChatDataBackward(Long.MAX_VALUE)
        }

    }

    // 向服务器发送已读报告
    fun markSessionReadItems(sid:Long, firstVisible:Int, lastVisible:Int){
        val chatSession = getSession(sid)
        chatSession.checkUnRead(firstVisible, lastVisible)
    }

    /* 同步方法备注：
    1. 私聊的本地数据是连续，保证从后向前一直存在，加载时候整体加载，因为服务器端不区分私聊会话。
    2. 群聊的数据不一定是连续的，因为客户可能长时间未登录，群内消息太多，全部加载不现实；
       所以每次与服务器只同步最新的数据；
       更新的整块的数据肯定是连续的，除了最前一条需要检测是否已经覆盖了以前的消息，那么后边的部分都标记为“连续”，
       返回的第1条默认设置为需要向前同步，但是如果第1条单独插入时候发现已经存在了，那么证明已经有完整的消息块了。
     */
    // 个别时候，多终端登录，会遇到这样的情况，
    fun onQueryPChatDataReplyBackward(littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){


    }

    // 一般连续使用，都是需要正向同步
    // 正常，这里不会很多
    fun onQueryPChatDataReplyForward(littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){
        // 1）保存到数据库
        val dbMessageList = pbChatDataList2DBMessageList(lst)
        val tempDbList = LinkedList<MessageData>()
        val ret = TopicDbHelper.insertPChatMsgBatch(dbMessageList, tempDbList)
        if (!ret){
            Log.e("Sdk", "batch insert db p2p chatmsg error")
        }

        // 这里可能有很多个会话的消息，需要单独的处理
        val msgList = pbChatDataList2MessageContentList(lst)
        // 逐个提交接收回执
        for (msg in msgList){
            if (msg.inOut && msg.tm2 == 0L)
            {
                // 如果多终端登录的自己的发的消息，不给回执
                MsgEncocder.sendChatReply(msg.userId, msg.sendId, msg.msgId, false, "")
                msg.tm2 = System.currentTimeMillis()
                msg.bRecv = true
            }

        }

        // 添加到内存
        val userMsgMap = pMessageListSortByTid(msgList)
        for (data in userMsgMap){
            val fid = data.key
            val userMsgList = data.value
            val chatSession = getSession(fid)
            chatSession.onPRecvBatchMsg(fid, userMsgList, true)
        }


        // 4) 数据库标记已经发送了接收回执
        TopicDbHelper.updatePChatReplyBatch(msgList)

        // 通知界面更新消息，已经保存处理完了
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.MSG_COMING, 0,
            0L, 0L, mapOf("msg" to "batch forward") )

        // 数据刚好为100,说明可能后续还有数据需要同步
        if (lst.size == 100){
            loadPMessageFromServer(0L)
        }

    }

    fun onQueryPChatDataReplyBetween(littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){
        // 用不到
    }

    // 将收到私聊的数据列表按照会话的ID分类
    fun pMessageListSortByTid(msgList: List<MessageContent>): Map<Long, MutableList<MessageContent>>{
        val userMsgMap = HashMap<Long, MutableList<MessageContent>>()
        for (msg in msgList){
            if (userMsgMap.containsKey(msg.userId)){
                userMsgMap[msg.userId]!!.add(msg)
            }else{
                val lst = LinkedList<MessageContent>()
                lst.add(msg)
                userMsgMap[msg.userId] =  lst
            }
        }
        return userMsgMap
    }

    fun pbChatDataList2DBMessageList( lst: List<MsgOuterClass.MsgChat>):List<MessageData>{
        val ret = LinkedList<MessageData>()
        for (item in lst){
            val msg = TextHelper.pbMsgToDbMsg(item.fromId, item)
            ret.add(msg)
        }
        return ret
    }
    fun pbChatDataList2MessageContentList(lst: List<MsgOuterClass.MsgChat>):List<MessageContent>{
        val ret = LinkedList<MessageContent>()
        for (item in lst){
            val msg = TextHelper.pbMsg2MessageContent(item.fromId, item)
            ret.add(msg)
        }
        return ret
    }


    // 群组一直都是反向同步，打开时候，仅仅先同步近期的数据
    fun onQueryGChatDataReplyBackward(gid:Long, littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){

    }

    fun onQueryGChatDataReplyForward(gid:Long, littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){
        // 用不到
    }

    fun onQueryGChatDataReplyBetween(gid:Long, littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){
        // 用不到
    }


} // end of class