package com.bird2fish.birdtalksdk.model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.net.Session
import com.bird2fish.birdtalksdk.net.WebSocketClient
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatMsgType
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatType
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import java.io.FileNotFoundException
import java.net.URI
import java.util.LinkedList


// 会话类型枚举
enum class SessionType {
    PRIVATE_CHAT, // 私聊
    GROUP_CHAT // 群组聊天
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
            val friend = SdkGlobalData.getMutualFriendLocal(sessionId)
            if (friend != null){
                // 确保这个Topic 应该有，这里图标与用户保持一致就可以了
                val topic = SdkGlobalData.getTopic(friend!!)

                this.sessionTitle = friend.nick ?: ""
                this.sessionIcon = friend.icon?: ""
                if (sessionTitle.isEmpty()){
                    sessionTitle = friend.id.toString()
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

    //发送内嵌消息
    fun addMessageNewOut(message: MessageContent) {
        synchronized(msgList){
            msgList.add(message)
        }


        synchronized(msgSendingList){
            msgSendingList[message.msgId] =message
        }
    }

    fun setReply(msgId:Long,tm:Long,  tm1:Long, tm2:Long) :Int{

        synchronized(msgSendingList){
            if (msgSendingList.containsKey(msgId)){
                //val msg = msgSendingList[msgId]
                msgSendingList.remove(msgId)
            }
        }

        synchronized(msgList){
            for (i in msgList.indices) {
                if(msgList[i].msgId == msgId){
                    msgList[i].msgStatus = MessageStatus.OK
                    if (tm > 0){
                        msgList[i].tm = tm
                    }
                    if (tm1 > 0){
                        msgList[i].bRecv = true
                    }
                    if (tm2 > 0){
                        msgList[i].bRead = true
                    }

                    return i
                }
            }

        }
        return 0
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

    // 发送文本消息，添加到列表中，同时发送消息
    fun sendTextMessageOut(sessionId:Long, message:String, refMsgId:Long = 0L):Long{
        val msgId = SdkGlobalData.nextId()
        val chatSession = getSession(sessionId)

        val uid = if (sessionId > 0) sessionId else -sessionId
        val msg = MessageContent(msgId, chatSession.sessionId, chatSession.sessionTitle, chatSession.sessionIcon,
            UserStatus.ONLINE,
            MessageStatus.SENDING,
            false,
            false,
            false,
            message,
            null)

        // 1)写入数据库

        // 2）加入列表
        chatSession?.addMessageNewOut(msg)

        // 更新最新消息
        SdkGlobalData.updateSession(msg)

        // 3）发送到服务器
        val chatType = if (sessionId > 0) ChatType.ChatTypeP2P else ChatType.ChatTypeGroup
        MsgEncocder.sendChatMsg(msgId, uid, chatType, ChatMsgType.TEXT, message, refMsgId)

        // 4 设置最新消息
        SdkGlobalData.updateSession(msg)
        return  msgId
    }

    // 使用嵌入图片的消息先放到内存消息中占位，然后上传图片，等待上传结束
    fun sendImageMessageUploading(sessionId:Long, context: Context, uri: Uri):Long{
        val contentResolver: ContentResolver = context.contentResolver

        val msgId = SdkGlobalData.nextId()
        val chatSession = getSession(sessionId)


        val uId = if (sessionId > 0) sessionId else -sessionId


        val draft = Drafty("")
        try {

            //二进制方式
            var fileName = TextHelper.getFileNameFromUri(context, uri)
            if (fileName == null) {
                fileName = ""
            }
            val t = draft.insertLocalImage(context, contentResolver, uri, fileName) ?: return 0

            //Log.d("文件内容", "draft: ${draft.toPlainText()}")
            val mime = TextHelper.getMimeTypeFromUri(context, uri)
            val msg = MessageContent(msgId,
                chatSession.sessionId, chatSession.sessionTitle, chatSession.sessionIcon,
                UserStatus.ONLINE, MessageStatus.UPLOADING, false, false, false,"", draft,  0, uri, mime)

            val sz = TextHelper.getFileSize(context, uri)
            msg.fileSz = sz

            // 得保存到临时列表，等待上传结束
            synchronized(this.uploadingMap){
                this.uploadingMap[msgId] = msg
            }
            // 异步上传
            Session.uploadSmallFile(context, uri,  uId, msgId)
            // 添加到界面列表
            chatSession!!.addMessageNewOut(msg)

        } catch (e: FileNotFoundException) {
            Log.e("ImageDetails", "File not found: $e")
            return  0
        } catch (e: Exception) {
            Log.e("ImageDetails", "Error while fetching image details: $e")
            return 0
        }

        return msgId
    }

    // 将文件上传后发送消息，与那个图片不一样在于处理draft方式不同
    fun sendFileMessageUploading(sessionId:Long, context: Context, uri: Uri):Long{
        val contentResolver: ContentResolver = context.contentResolver

        val msgId = SdkGlobalData.nextId()
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
        draft.attachFile(mime, fileName, uri.toString(), sz, msgId, sessionId);

        val msg = MessageContent(msgId,
            chatSession.sessionId, chatSession.sessionTitle, chatSession.sessionIcon,
            UserStatus.ONLINE, MessageStatus.UPLOADING, false, false, false,"", draft,  0, uri, mime)
        msg.fileSz = sz

        val (hasAttach, sz1) = hasAttachment(draft)
        Log.d("SendFile", "文件大小为：${sz1}")

        // 得保存到临时列表，等待上传结束
        synchronized(this.uploadingMap){
            this.uploadingMap[msgId] = msg
        }
        // 异步上传
        //Session.uploadSmallFile(context, uri,  uId, msgId)
        msg.fileHashCode = Session.uploadFileChunk(context, uri,  uId, msgId, 0, "")
        // 添加到界面列表
        chatSession!!.addMessageNewOut(msg)

        return msgId
    }

    // 计算上传进度
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

        val msgId = SdkGlobalData.nextId()
        val chatSession = getSession(sessionId)


        val uId = if (sessionId > 0) sessionId else -sessionId

        val msg = MessageContent(msgId, SdkGlobalData.selfUserinfo.id, "", "",
            UserStatus.ONLINE, MessageStatus.SENDING,false, false, false, "", draft)

        // 1)写入数据库

        // 2）加入列表
        chatSession?.addMessageNewOut(msg)

        // 3）发送到服务器
        val chatType = if (sessionId > 0) ChatType.ChatTypeP2P else ChatType.ChatTypeGroup

        val txt = TextHelper.serializeDrafty(draft)
        Log.d("send audio drafty", txt)

        MsgEncocder.sendChatMsg(msgId, uId, chatType, ChatMsgType.VOICE, txt, 0L)

        // 4 设置最新消息
        SdkGlobalData.updateSession(msg)
        return  msgId
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
        val chatType = if (msg!!.userId > 0) ChatType.ChatTypeP2P else ChatType.ChatTypeGroup

        val draft = Drafty("")

        // "image/jpeg"
        val fullUrl = WebSocketClient.instance!!.getRemoteFilePath(uuidName)
        var draftMsg = msg!!.content

        // 如果是图片
        if (hasImage(draftMsg)){
            //draft.insertImage(0,"image/jpeg", null, 884, 535, "",
            draft.insertImage(0, msg!!.mime, null, 0, 0, "",
                URI(fullUrl), 0)

            val txt = TextHelper.serializeDrafty(draft)
            Log.d("send image or file drafty", txt)

            MsgEncocder.sendChatMsg(msg!!.msgId, msg!!.userId, chatType, ChatMsgType.IMAGE, txt, 0L)

            // 4 设置最新消息
            SdkGlobalData.updateSession(msg!!)
            return
        }

        // 解构返回值
        val (hasAttach, sz) = hasAttachment(draftMsg)

        if (hasAttach) {

            setAttachmentProcess(draftMsg, fileName, 100)

            // 调用 attachFile 时传入文件大小
            draft.attachFile(msg!!.mime, fileName, fullUrl, sz ?: 0L, msgId, SdkGlobalData.selfUserinfo.id)

            // 序列化 Drafty
            val txt = TextHelper.serializeDrafty(draft)
            Log.d("send image or file drafty", txt)

            // 发送消息
            MsgEncocder.sendChatMsg(
                msg!!.msgId,
                msg!!.userId,
                chatType,
                ChatMsgType.FILE,
                txt,
                0L
            )

            // 4 设置最新消息
            SdkGlobalData.updateSession(msg!!)

            return
        }

        // 如果是大的语音片

    }

    // 如果上传文件错误了
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

        setAttachmentProcess(draftMsg, fileName, -1)
    }

    // 处理收到的数据，
    // 1）保存数据库，2）回执，回执后更新数据库，3）添加到会话的消息列表中
    fun onRecvChatMsg(chatMsg: com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.MsgChat):MessageContent{

        //2）接受回执，回执后更新数据库
        val fid = chatMsg.fromId
        MsgEncocder.sendChatReply(fid, chatMsg.sendId, chatMsg.msgId, false)


        //3）添加到界面的列表里
        var sId = chatMsg.fromId
        if (chatMsg.chatType == MsgOuterClass.ChatType.ChatTypeP2P){

        }else{
            sId = -chatMsg.fromId
        }

        val chatSession = getSession(sId)

        var msg : MessageContent? = null

        if (chatMsg.msgType == MsgOuterClass.ChatMsgType.TEXT){
            val utf8String = chatMsg.data.toString(Charsets.UTF_8)

            msg = MessageContent(chatMsg.msgId,
                sId, chatSession.sessionTitle, chatSession.sessionIcon,
                UserStatus.ONLINE, MessageStatus.OK, true, false, false, utf8String, null, chatMsg.tm)

        }else if (chatMsg.msgType == MsgOuterClass.ChatMsgType.IMAGE){
            val txt = chatMsg.data.toString(Charsets.UTF_8)


            val draft = TextHelper.deserializeDrafty(txt)
            Log.d("Image Draty", txt)

            msg = MessageContent(chatMsg.msgId,
                sId, chatSession.sessionTitle, chatSession.sessionIcon,
                UserStatus.ONLINE, MessageStatus.OK, true, false, false, "", draft, chatMsg.tm)
        }
        else if (chatMsg.msgType == MsgOuterClass.ChatMsgType.VOICE){

            val txt = chatMsg.data.toString(Charsets.UTF_8)

            val draft = TextHelper.deserializeDrafty(txt)
            Log.d("Audio Draty", txt)

            msg = MessageContent(chatMsg.msgId,
                sId, chatSession.sessionTitle, chatSession.sessionIcon,
                UserStatus.ONLINE, MessageStatus.OK, true, false, false, "", draft, chatMsg.tm)
        }
        else if (chatMsg.msgType == MsgOuterClass.ChatMsgType.FILE){
            val txt = chatMsg.data.toString(Charsets.UTF_8)

            val draft = TextHelper.deserializeDrafty(txt)
            Log.d("File Draty", txt)

            msg = MessageContent(chatMsg.msgId,
                sId, chatSession.sessionTitle, chatSession.sessionIcon,
                UserStatus.ONLINE, MessageStatus.OK, true, false, false, "", draft, chatMsg.tm)
        }

        chatSession!!.addMessageToTail(msg!!)

        // 需要创建topic 并设置最新消息
        SdkGlobalData.updateSession(msg!!)
        return msg!!
    }

    // 回执, 先写到数据库，然后同步到内存，区别私聊和群聊，群聊只有一个回执
    fun onChatMsgReply(fid:Long, params:Map<String, String> ?, msgId:Long, tm:Long, tm1:Long, tm2:Long):Int{
        val chatSession = getSession(fid)
        val index = chatSession.setReply(msgId, tm, tm1, tm2)
        return index
    }


    // 点击时候直接设置进度了
//    fun notifyDownloadProcess(sessionId:Long, msgid:Long, percent:Int, fname:String, url:String){
//
//    }
}