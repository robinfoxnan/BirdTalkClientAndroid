package com.bird2fish.birdtalksdk.model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.Settings.Global.getString
import android.text.TextUtils
import android.util.Log
import androidx.core.net.toUri
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.db.GroupDbHelper
import com.bird2fish.birdtalksdk.db.TopicDbHelper
import com.bird2fish.birdtalksdk.model.Drafty.Entity
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.net.Session
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatMsgType
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatType
import com.bird2fish.birdtalksdk.uihelper.ImagesHelper
import com.bird2fish.birdtalksdk.uihelper.RingPlayer
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileNotFoundException
import java.util.HashMap
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

// 会话管理工具类
// 备注：系统通知使用100号，私聊号
object ChatSessionManager {

    var bInit = false
    private val sessions = ConcurrentHashMap<Long, ChatSession>() // 会话ID到会话的映射

    // 给那个对话的界面使用的
    private var displayChatList: MutableList<ChatSession> = ArrayList()

    // 如果正在上传，应该保存临时信息
    var uploadingMap: MutableMap<Long, MessageContent> = LinkedHashMap()


    // 这里不要加锁了
    // 这个函数是收到新的消息，或者打开界面的的时候需要创建一个新的会话
    private fun createSession(sessionId: Long):ChatSession{
        // 单独处理那个系统消息的会话
        if (sessionId == 100L || sessionId == 0L){
            return if (sessions.containsKey(0L)){
                sessions[0]!!
            }else{
                createSystemSession()
            }
        }


        // 一对一私聊
        if (sessionId > 0){
            val friend = UserCache.findUserSync(sessionId)
            val t = Topic(sessionId, 0, 0, Topic.CHAT_P2P, 1, friend.nick, friend.icon)
            val s = t.asChatSession()
            sessions[sessionId] = s
            TopicDbHelper.insertOrReplacePTopic(t)
            return s
        }
        // 群聊
        val group = GroupCache.findGroupSync(-sessionId)
        val t = Topic(-sessionId, 0, 0, Topic.CHAT_GROUP, 1, group.name, group.icon)
        val s = t.asChatSession()
        sessions[sessionId] = s
        TopicDbHelper.insertOrReplaceGTopic(t)
        return s
    }

    // 获取会话，有可能是空的
    fun getSession(sessionId: Long): ChatSession{
        synchronized(sessions){
            if (sessions.containsKey(sessionId))
                return sessions[sessionId]!!
            else{
                return createSession(sessionId)
            }
        }
    }

    fun getSession(friend: User):ChatSession{
        return getSession(friend.id)
    }

    fun getSession(group:Group):ChatSession{
        return getSession(-group.gid)
    }


    // 重建会话列表显示的部分，因为隐藏了，或者显示了一部分
    fun rebuildDisplayList(): MutableList<ChatSession>{
        synchronized(sessions){
            displayChatList.clear()
            for (t in sessions.values) {
                if (t.showHide) {
                    displayChatList.add(t)
                }
            }
        }

        resortDisplayChatList()

        return displayChatList
    }

    // 每次收到消息都需要排序
    private fun resortDisplayChatList(){
        // 这里执行一个排序，优先考虑置顶，然后考虑有新消息的
        // lambda 必须返回 Int：负数 表示 a 在 b 之前，正数 表示 a 在 b 之后，0 表示相等（保持原顺序若排序稳定
        displayChatList.sortWith { a, b ->
            // 1️⃣ 置顶优先
            if (a.pinned != b.pinned) {
                return@sortWith if (a.pinned) -1 else 1
            }

            var aTm = 0L
            var bTm = 0L
            b.lastMsg?.apply { bTm = tm }
            a.lastMsg?.let { aTm = it.tm }


            // 2️⃣ 时间倒序（最新在前）
            // 这里，如果是a大，则需要返回负数，所以这里参数反着用
            return@sortWith bTm.compareTo(aTm)
        }
    }

    // 启动时候加载，登录时候不用; 这个与远程也不同步
    fun tryLoadTopicsFromDb(){
        if (bInit){
            return
        }
        // 这里会构造函数之后init自己加载用户或者群组内容，
        synchronized(sessions){
            if (sessions.isEmpty()){
                // 私聊的会话
                val p2pTopics = TopicDbHelper.getAllPTopics()
                for (t in p2pTopics){
                    if (t.type != Topic.CHAT_P2P){
                        Log.e("tryLoadTopicsFromDb()", "load p2p topics "+ t.toString())
                        continue
                    }
                    sessions[t.tid] = t.asChatSession()
                }
                // 群聊的会话
                val gTopics = TopicDbHelper.getAllGTopics()
                for (t in gTopics){
                    if (t.type != Topic.CHAT_GROUP){
                        Log.e("load group topics", t.toString())
                        continue
                    }
                    sessions[-t.tid] = t.asChatSession()
                }

                // 系统会话
                sessions[0L] = createSystemSession()
            }
        }

    }

    // 系统通知
    private fun  createSystemSession():ChatSession{
        val txt = SdkGlobalData.context!!.getString(R.string.sys)
        val temp = Topic(0, 0, 0, 0, 1, txt, "")
        val sysTopic = temp.asChatSession()
        return sysTopic
    }

    // 删除会话，暂时没有用过
    fun deleteSession(sessionId: Long) {
        synchronized(sessions) {
            sessions.remove(sessionId)
        }
    }



    // 获取未读消息总数
    fun getTotalUnreadCount(): Int {
        synchronized(sessions) {
            return sessions.values.sumOf { it.getUnreadCount() }
        }
    }

     //获取所有会话列表，在连接管理器的线程里，检查需要重发的消息需要调用这个
    private fun getAllSessions(): List<ChatSession> {
        // 按最后一条消息时间排序，最新的在前面
//        synchronized(sessions){
//            return sessions.values.sortedByDescending { it.getLastMessage()?.tm ?: 0 }
//        }
        synchronized(sessions){
            return sessions.values.toList()
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
    // 如果是重启后重发，那么这里可能会没有msg.contentOut, 所以从数据库加载的没有发送晚点消息直接发送失败
    fun sendMsgContentToNet(msg:MessageContent, bResend:Boolean = false){

        GlobalScope.launch(Dispatchers.IO) {

            // 1) 先写数据库,更新到发送表
            if (!bResend){
                val msgData = TextHelper.MsgContentToDbMsg(msg)
                if (msg.isP2p()){
                    TopicDbHelper.insertPChatMsg(msgData)
                }else{
                    TopicDbHelper.insertGChatMsg(msgData)
                }
            }


            val chatType = if (msg.isP2p()) ChatType.ChatTypeP2P else ChatType.ChatTypeGroup

            // 2) 提交到网络
            if (msg.msgType == ChatMsgType.TEXT) {
                MsgEncocder.sendChatMsg(
                    msg.msgId,
                    msg.sendId,
                    msg.tId,
                    chatType,
                    ChatMsgType.TEXT,
                    msg.text,
                    msg.msgRefId
                )
            } else {
                MsgEncocder.sendChatMsg(
                    msg.msgId,
                    msg.sendId,
                    msg.tId,
                    chatType,
                    msg.msgType,
                    msg.contentOut,
                    msg.msgRefId
                )
            }
        }

    }

    // 发送文本消息，添加到列表中，同时发送消息
    fun sendTextMessageOut(session:ChatSession, str:String, refMsgId:Long = 0L){
        val sendId = SdkGlobalData.nextId()

        val msg = MessageContent(session, sendId, sendId, MessageStatus.SENDING,
            false, 0L, 0L, 0L, str, null, ChatMsgType.TEXT)

        // 2）加入列表
        session.addMessageNewOut(msg)

        // 3）发送到服务器
        this.sendMsgContentToNet(msg, false)

        return
    }

    // 使用嵌入图片的消息先放到内存消息中占位，然后上传图片，等待上传结束
    fun sendImageMessageUploading(session:ChatSession, context: Context, uri: Uri, refMsgId:Long = 0L):Long{
        val contentResolver: ContentResolver = context.contentResolver

        val sendId = SdkGlobalData.nextId()

        val tid = session.tid
        val me = SdkGlobalData.selfUserinfo.id


        // 检测是否需要缩放到1024大小
        var (resizeUri, needResize) = ImagesHelper.resizeImageIfNeeded(SdkGlobalData.context!!, uri, "upload")
        if (!needResize){
            resizeUri = uri
        }

        val draft = Drafty("")
        try {

            //二进制方式
            var fileName = TextHelper.getFileNameFromUri(context, uri)
            if (fileName == null) {
                fileName = ""
            }else{
                fileName = toJpgFileName(fileName!!)
            }

            val mime = TextHelper.getMimeTypeFromUri(context, resizeUri)
            // 插入10K预览图实在是太小了
            draft.insertLocalImage(context, contentResolver, resizeUri, fileName, mime) ?: return 0L

            //Log.d("文件内容", "draft: ${draft.toPlainText()}")

            val sz = TextHelper.getFileSize(context, resizeUri)
            val msg = MessageContent(session, sendId, sendId, MessageStatus.UPLOADING,
                false, 0L, 0L, 0L, "", draft, ChatMsgType.IMAGE, resizeUri, mime, sz, fileName)


            // 得保存到临时列表，等待上传结束
            synchronized(this.uploadingMap){
                this.uploadingMap[sendId] = msg
            }
            // 异步上传
            msg.fileHashCode = Session.uploadFileChunk(context, resizeUri,  tid,
                sendId, 0, "", msg.fileName)

            // 先写数据库,更新到发送表
            val msgData = TextHelper.MsgContentToDbMsg(msg)

            if (session.isP2pChat()){
                TopicDbHelper.insertPChatMsg(msgData)
            }else{
                TopicDbHelper.insertGChatMsg(msgData)
            }
            // 添加到界面列表
            session.addMessageNewOut(msg)

        } catch (e: FileNotFoundException) {
            Log.e("ImageDetails", "File not found: $e")
            return 0L
        } catch (e: Exception) {
            Log.e("ImageDetails", "Error while fetching image details: $e")
            return 0L
        }

        return sendId
    }

    fun toJpgFileName(fileName: String): String {
        // 去掉后缀
        val nameWithoutExt = fileName.substringBeforeLast('.', fileName)
        // 拼接 .jpg
        return "$nameWithoutExt.jpg"
    }

    // 将文件上传后发送消息，与那个图片不一样在于处理draft方式不同
    fun sendFileMessageUploading(session:ChatSession, context: Context, uri: Uri):Long{
        //val contentResolver: ContentResolver = context.contentResolver

        val sendId = SdkGlobalData.nextId()

        //二进制方式
        var fileName = TextHelper.getFileNameFromUri(context, uri)
        if (TextUtils.isEmpty(fileName)) {
            fileName = "file"
        }

        //Log.d("文件内容", "draft: ${draft.toPlainText()}")
        val mime = TextHelper.getMimeTypeFromUri(context, uri)
        val sz = TextHelper.getFileSize(context, uri)

        val tid = session.tid
        val me = SdkGlobalData.selfUserinfo.id



        val draft = Drafty("")
        //draft.attachFile(mime, bits, fileName);
        draft.attachFile(mime, fileName, uri.toString(), sz,sendId, session.getSessionId());

        val msg = MessageContent(session, sendId, sendId, MessageStatus.UPLOADING,
            false, 0L, 0L, 0L, "", draft, ChatMsgType.FILE, uri, mime, sz, fileName!!)

        //Log.d("SendFile", "文件大小为：${sz1}")

        // 得保存到临时列表，等待上传结束
        synchronized(this.uploadingMap){
            this.uploadingMap[sendId] = msg
        }
        // 异步上传
        //Session.uploadSmallFile(context, uri,  uId, msgId)
        msg.fileHashCode = Session.uploadFileChunk(context, uri,  tid, sendId, 0, "", fileName)

        // 先写数据库,更新到发送表
        val msgData = TextHelper.MsgContentToDbMsg(msg)

        if (session.isP2pChat()){
            TopicDbHelper.insertPChatMsg(msgData)
        }else{
            TopicDbHelper.insertGChatMsg(msgData)
        }

        // 添加到界面列表
       session.addMessageNewOut(msg)

        return sendId
    }

    // 计算上传进度, 收到上一片的回执再发送下一片，这里其实是支持并发多个文件的
    fun onUploadFileProcess(msgId:Long, fileName:String, uuid:String, index:Int, params:Map<String, String>){
        var msg:MessageContent? = null
        synchronized(this.uploadingMap){
            msg = this.uploadingMap[msgId]
        }

        if (msg != null){
            //val sessionId = if (msg.isP2p) msg!!.tId else -msg!!.tId
            val sz = msg!!.fileSz.toFloat()
            val percent:Float = (index+1).toFloat() * 1024*1024 * 100   /sz;

            val draft = msg!!.content
            setAttachmentProcess(draft, fileName, percent.toInt())
            Session.uploadFileChunk(
                SdkGlobalData.context!!, msg!!.fileUri, msg!!.userId,
                msgId, index + 1, msg!!.fileHashCode,
                msg!!.fileName)

            SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.MSG_UPLOAD_PROCESS, percent.toInt(), msgId, 0L, params)
        }

    }

    // 检测音频数据BASE64是否是超过10K
    fun isBase64Over10KB(audioBytes: ByteArray?): Boolean {
        if (audioBytes == null) return false

        // Base64 length = ceil(n / 3) * 4
        val base64Length = ((audioBytes.size + 2) / 3) * 4

        return base64Length > 10 * 1024
    }

    // 发送语音
    fun sendAudioOut(session:ChatSession, context: Context, draft:Drafty, bits: ByteArray?, mAudioFile: File, refMsgId:Long):Long{

        val sendId = SdkGlobalData.nextId()
        val tid = session.tid
        val me = SdkGlobalData.selfUserinfo.id
        // 如果是太长，远端服务器无法写库，所以大一点需要上传文件
        if (isBase64Over10KB(bits)){

            val msg = MessageContent(session, sendId, sendId, MessageStatus.UPLOADING,
                false, 0L, 0L, 0L, "",
                draft, ChatMsgType.VOICE, mAudioFile.toUri(), "audio/mp4", bits!!.size.toLong(), "")


            // 得保存到临时列表，等待上传结束
            synchronized(this.uploadingMap){
                this.uploadingMap[sendId] = msg
            }
            // 异步上传
            //Session.uploadSmallFile(context, uri,  uId, msgId)
            msg.fileHashCode = Session.uploadFileChunk(context, mAudioFile.toUri(),
                tid, sendId, 0, "", "")

            // 先写数据库,更新到发送表
            val msgData = TextHelper.MsgContentToDbMsg(msg)

            if (session.isP2pChat()){
                TopicDbHelper.insertPChatMsg(msgData)
            }else{
                TopicDbHelper.insertGChatMsg(msgData)
            }

            // 添加到界面列表
            session.addMessageNewOut(msg)
        }else{

            val txt = TextHelper.serializeDrafty(draft)

            val msg = MessageContent(session, sendId, sendId, MessageStatus.SENDING,
                false, 0L, 0L, 0L, "",
                draft, ChatMsgType.VOICE, null, "audio/mp4", bits!!.size.toLong(), "")
            msg.contentOut = txt

            //Log.d("send audio drafty", txt)
            //
            // 2）加入列表
            session.addMessageNewOut(msg)

            // 3）发送到服务器
            // 3.1)写入数据库
            this.sendMsgContentToNet(msg)

        }
        return  sendId
    }

    fun hasAttachment(drafty: Drafty?, filename: String?, uuidName: String): Pair<Boolean, Long?> {
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
                        entity.data["ref"] = uuidName
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

    // 将上传的结果补充进去
    fun hasImage(drafty: Drafty?, fileName: String, uuidName: String): Boolean {
        if (drafty?.ent == null) {
            return false
        }

        for (entity in drafty.ent) {
            if (entity?.tp == null) continue
            if (entity.tp.equals("IM") ) {
                entity.data["ref"] = uuidName
                return true
            }
        }
        return false
    }

    fun hasAudio(drafty: Drafty?, fileName: String, uuidName: String) : Pair<Boolean, Entity?>{
        if (drafty?.ent == null) {
            return Pair(false, null)
        }

        for (entity in drafty.ent) {
            if (entity?.tp == null) continue
            if (entity.tp.equals("AU") ) {
                entity.data["ref"] = uuidName
                return Pair(true, entity)
            }
        }
        return Pair(false, null)
    }


    // 上传结束了，这个时候需要发送消息
    fun onUploadFileFinish(msgId:Long, fileName:String, uuidName:String){

        var msg : MessageContent? = null
        synchronized(this.uploadingMap){
            msg = this.uploadingMap[msgId]
            this.uploadingMap.remove(msgId)
        }

        if (msg == null){
            Log.e("onUploadFileReply", "can't find message in queue")
            return
        }

        val chatSession = msg!!.session

        msg!!.msgStatus = MessageStatus.SENDING
        //msg!!.content = draft
        //val chatType = if (msg!!.userId > 0) ChatType.ChatTypeP2P else ChatType.ChatTypeGroup

        val draft = Drafty("")

        // "image/jpeg"
        //val fullUrl = WebSocketClient.instance!!.getRemoteFilePath(uuidName)

        //val fullUrl = CryptHelper.getUrl(uuidName)
        var draftInMsg = msg!!.content

        // 如果是图片
        if (hasImage(draftInMsg, fileName, uuidName)){
            //draft.insertImage(0,"image/jpeg", null, 884, 535, "",
            draft.insertImage(0, msg!!.mime, null, 0, 0, fileName,
                uuidName, null, 0)

            val txt = TextHelper.serializeDrafty(draft)
            Log.d("send image or file drafty", txt)
            msg!!.contentOut = txt

            this.sendMsgContentToNet(msg!!)
            chatSession.lastMsg = msg!!

            return
        }
        // 如果是大的语音片
        else{
            val (hasAudio, entity) = hasAudio(draftInMsg, fileName, uuidName)
            if (hasAudio)
            {
                val duration = entity?.data?.get("duration") as? Int ?: 0
                val preview = entity?.data?.get("preview") as?  ByteArray ?:null
                draft.insertAudio(
                    0,
                    "audio/mp4",
                    null,
                    preview,
                    duration,
                    uuidName,
                    uuidName, // URI("https://lx-sycdn.kuwo.cn/c7aff93e02882b90b34e8f45387b4436/6755728e/resource/n2/3/57/2049851017.mp3?")
                    msg!!.fileSz)

                // 序列化 Drafty
                val txt = TextHelper.serializeDrafty(draft)
                Log.d("send audio with file drafty", txt)

                // 发送消息
                msg!!.contentOut = txt
                this.sendMsgContentToNet(msg!!)
                chatSession.lastMsg = msg!!
                return
            }


            // 文件类型的

            // 解构返回值
            val (hasAttach, sz) = hasAttachment(draftInMsg, fileName, uuidName)
            if (hasAttach) {

                setAttachmentProcess(draftInMsg, fileName, 100)

                // 调用 attachFile 时传入文件大小
                draft.attachFile(msg!!.mime, fileName, uuidName, sz ?: 0L, msgId, SdkGlobalData.selfUserinfo.id)

                // 序列化 Drafty
                val txt = TextHelper.serializeDrafty(draft)
                Log.d("send image or file drafty", txt)

                // 发送消息
                msg!!.contentOut = txt
                this.sendMsgContentToNet(msg!!)
                chatSession.lastMsg = msg!!
                return
            }
        }
    }

    // 如果上传文件错误了, 这个函数还没有测试过
    fun onUploadFileError(msgId:Long, detail:String, fileName:String, uuidName:String){
        var msg : MessageContent? = null
        synchronized(this.uploadingMap){
            msg = this.uploadingMap[msgId]
            this.uploadingMap.remove(msgId)
        }

        if (msg == null){
            Log.e("onUploadFileReply", "can't find message in queue")
            return
        }

        //
        val sessionId = if (msg!!.isP2p()) msg!!.tId else -msg!!.tId
        val chatSession = getSession(sessionId)

        msg!!.msgStatus = MessageStatus.FAIL
        //msg!!.content = draft
        val chatType = if (msg!!.userId > 0) ChatType.ChatTypeP2P else ChatType.ChatTypeGroup
        var draftMsg = msg!!.content

        // 先写数据库,更新到发送表
        val msgData = TextHelper.MsgContentToDbMsg(msg!!)

        if (msg!!.isP2p()){
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
        var tid = chatMsg.fromId

        // 当前消息状态为RECV
        val msgData = TextHelper.pbMsgToDbMsg(chatMsg)
        if (chatMsg.chatType == ChatType.ChatTypeP2P){
            tid = chatMsg.fromId
            // 1.1 写库
            TopicDbHelper.insertPChatMsg(msgData)
            // 1.2 发送接收回执
            MsgEncocder.sendChatReply(tid, chatMsg.sendId, chatMsg.msgId, false, "")

            // 1.3 更新数据库的接收时间
            val tm2= System.currentTimeMillis()
            TopicDbHelper.updatePChatReply(chatMsg.msgId, 0L, tm2, 0L, true)

        }else{
            tid = chatMsg.toId
            TopicDbHelper.insertGChatMsg(msgData)
            // 群组消息不用给回执，因为不在意
        }


        //2）添加到界面的列表里
        val msg = TextHelper.pbMsg2MessageContent(chatMsg)

        var sId = tid
        if (chatMsg.chatType == MsgOuterClass.ChatType.ChatTypeP2P){
            sId = chatMsg.fromId
        }else{
            sId = -chatMsg.toId
        }
        val chatSession = getSession(sId)
        chatSession.addMessageToTail(msg!!)

        val resultMap = mapOf(
            "status" to "coming",
        )

        // 通知界面更新消息，已经保存处理完了
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(
            MsgEventType.MSG_COMING, chatMsg.msgType.number,
            chatMsg.msgId, chatMsg.fromId, resultMap)

        // 播放铃声，需要检测对应的topic是否需要响
        if (chatSession.mute != null && ! chatSession.mute){
            RingPlayer.playNotifyRing(SdkGlobalData.context!!)
        }

        return msg!!
    }

    // 回执, 先写到数据库，然后同步到内存，区别私聊和群聊，群聊只有一个回执
    fun onChatMsgReplyOk(msgId:Long, sendId:Long, fid:Long, params:Map<String, String>, tmServer:Long, tmRecv:Long,
                         tmRead:Long, result:String?){
        // 这里更新回执
        var detail = params?.get("detail")
        var gidStr = params["gid"]
        if (detail == null){
            detail = ""
        }
        if (gidStr == null){
            gidStr = "0"
        }

        // 群聊，只有服务器应答，私聊才有用户应答
        if (gidStr != "0"){
            // 群组的服务器应答
            val chatSession = getSession(-fid)
            chatSession.setReply(msgId, sendId, tmServer, tmRecv, tmRead, true, "")
        }else{

            val chatSession = getSession(fid)
            chatSession.setReply(msgId, sendId, tmServer, tmRecv, tmRead, true, "")

        }

    }

    // 某个消息错误，不能发
    fun onPChatMsgReplyError(fid:Long, msgId:Long, sendId:Long, detail:String, params:Map<String, String>){

        val chatSession = getSession(fid)
        chatSession.setReply(msgId, sendId, System.currentTimeMillis(), 0, 0, false, detail)
    }

    fun onGChatMsgReplyError(fid:Long, msgId:Long, sendId:Long, detail:String, params:Map<String, String>){

        val chatSession = getSession(-fid)
        chatSession.setReply(msgId, sendId, System.currentTimeMillis(), 0, 0, false, detail)
    }

    // 这里是全局数据的索引
    fun loadMessageOnLogin(){
        var lstTm = 0L
        // 如果是启动登录就加载数据库的历史消息，然后与服务器同步，
        if (!bInit){
            loadPMessageFormDbOnLogin()
            bInit = true
        }
        // 如果是断开后的链接后登录，直接同步消息
        else{
            loadP2pMessageFromServerOnLogin()
        }
    }
    // 启动时候需要从数据库加载消息
    fun loadPMessageFormDbOnLogin():Long{
        val max = Long.MAX_VALUE
        var lastTm = 0L

        var lst : List<MessageData>? = null
        synchronized(sessions){
            // 这里的TID 也是有正负之分的
            for (p in sessions){
                val sid = p.key
                val t = p.value
                // 如果不显示的会话，不加载数据，也不显示
                if (!t.showHide){
                    continue
                }
                val chatSession = t
                Log.d("loadPMessageFormDbOnLogin()", t.toString())

                if (t.type == Topic.CHAT_P2P){
                    lst = TopicDbHelper.getPChatMessagesById(t.tid, max, SdkGlobalData.LOAD_MSG_BATCH, false)
                }else if (t.type == Topic.CHAT_GROUP){
                    lst = TopicDbHelper.getGChatMessagesById(t.tid, max, SdkGlobalData.LOAD_MSG_BATCH, false)
                }else{
                    // 加载系统的通知事件
                    continue
                }
                if (lst != null){
                    // 将数据库里的数据插入到列表中, 这是反向加载，所以是倒序的
                    val tm = chatSession.loadMessageOnLogin(lst!!, t)
                    if (tm > lastTm){
                        lastTm = tm
                    }
                }

            }
        }


        // 同步远程的消息
        loadP2pMessageFromServerOnLogin()

        return lastTm
    }


    // 重链接成功后需要与服务器, 这里是一组消息
    // 重要：如果这里不加1，会返回重复的数据，造成隐藏的会话项重新设置为显示，后续如果清理数据，也至少需要保留一条！！！
    fun loadP2pMessageFromServerOnLogin(){

        // 找出当前私聊或者群聊中最后一条记录, +1 是为了防止重复
        val maxId = TopicDbHelper.getPChatLastId()
        if (maxId > 0){

            // 说明此时表中有数据，应该正向查找
            MsgEncocder.sendSynPChatDataForward(maxId + 1)
        }else{
            // 没有数据说明是多终端登录或者的清理过数据,
            // 设置fid==0，就是加载所有的数据，默认返回100条
            MsgEncocder.sendSynChatDataBackward(Long.MAX_VALUE, 0, 0)
        }

    }


    /* 同步方法备注：
    1. 私聊的本地数据是连续，保证从后向前一直存在，加载时候整体加载，因为服务器端不区分私聊会话。
       但是服务器有个物化视图保存了一份会话的数据，这里可以使用fid指定反向加载某些历史数据。
    2. 群聊的数据不一定是连续的，因为客户可能长时间未登录，群内消息太多，全部加载不现实；
       所以每次与服务器只同步最新的数据；
       更新的整块的数据肯定是连续的，除了最前一条需要检测是否已经覆盖了以前的消息，那么后边的部分都标记为“连续”，
       返回的第1条默认设置为需要向前同步，但是如果第1条单独插入时候发现已经存在了，那么证明已经有完整的消息块了。
     */
    // 1）个别时候，多终端登录，会遇到这样的情况；2）单个会话清理了本地数据，拖动界面会触发反向加载
    fun onQueryPChatDataReplyBackward(fid:Long, littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){
        if (fid == 0L || fid == SdkGlobalData.selfUserinfo.id){
            onQueryPChatDataReplyBackward(littleId, bigId, lst)
            return
        }
        // 这里开始处理一个会话的返回的消息

        // 1）保存到数据库
        val dbMessageList = pbChatDataList2DBMessageList(lst)
        val tempDbList = LinkedList<MessageData>()
        val ret = TopicDbHelper.insertPChatMsgBatch(dbMessageList, tempDbList)
        if (!ret){
            Log.e("Sdk", "batch insert db p2p chatmsg error")
        }

        // 这里可能有很多个会话的消息，需要单独的处理
        val msgList = pbChatDataList2MessageContentList(lst)
        // 这里不需要再检查回执了


        // 添加到内存
        val userMsgMap = pMessageListSortByTid(msgList)
        for (data in userMsgMap){
            val fid = data.key
            val userMsgList = data.value
            val chatSession = getSession(fid)
            chatSession.onPRecvBatchMsg(userMsgList, false)
        }

        // 通知界面更新消息，已经保存处理完了
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(
            MsgEventType.MSG_HISTORY, 0,
            0L, 0L, mapOf("msg" to "batch forward") )

    }

    // 这里与正向加载一样混合了多个用户的会话的数据
    fun onQueryPChatDataReplyBackward(littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){

        // 1）保存到数据库
        val dbMessageList = pbChatDataList2DBMessageList(lst)
        val tempDbList = LinkedList<MessageData>()
        val ret = TopicDbHelper.insertPChatMsgBatch(dbMessageList, tempDbList)
        if (!ret){
            Log.e("Sdk", "batch insert db p2p chatmsg error")
        }

        // 这里可能有很多个会话的消息，需要单独的处理
        val msgList = pbChatDataList2MessageContentList(lst)
        // 这里不需要再检查回执了


        // 添加到内存
        val userMsgMap = pMessageListSortByTid(msgList)
        for (data in userMsgMap){
            val fid = data.key
            val userMsgList = data.value
            val chatSession = getSession(fid)
            chatSession.onPRecvBatchMsg(userMsgList, false)
        }

        // 通知界面更新消息，已经保存处理完了
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(
            MsgEventType.MSG_HISTORY, 0,
            0L, 0L, mapOf("msg" to "batch forward") )

    }

    // 一般连续使用，都是需要正向同步
    // 正常，这里不会很多
    fun onQueryPChatDataReplyForward(littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){

        // 1）保存到数据库
        val dbMessageList = pbChatDataList2DBMessageList(lst)

        // 先发送回执
        for (msg in dbMessageList){
            // 必须是别人发给自己的，而且没有提交过，因为可以多终端登录
            if (msg.io == MessageInOut.IN.ordinal && msg.tm2 == 0L)
            {
                // 如果多终端登录的自己的发的消息，不给回执
                MsgEncocder.sendChatReply(msg.tid, msg.sendId, msg.id, false, "")
                msg.tm2 = System.currentTimeMillis()
            }
        }

        // 临时的保存失败的条目
        val tempDbList = LinkedList<MessageData>()
        val ret = TopicDbHelper.insertPChatMsgBatch(dbMessageList, tempDbList)
        if (!ret){
            Log.e("Sdk", "batch insert db p2p chatmsg error")
        }

        // 2)  分组后添加到内存
        // 这里是转换格式
        // 这里可能有很多个会话的消息，需要单独的处理
        val msgList = pbChatDataList2MessageContentList(lst)
        // 这里虽然不太一致，也问题不大，保证界面上不再二次提交回执
//        for (m in msgList){
//            if (m.inOut  && m.tm2 == 0L){
//                m.tm2 = System.currentTimeMillis()
//                m.bRecv = true
//            }
//        }

        // 添加到内存
        val userMsgMap = pMessageListSortByTid(msgList)
        for (data in userMsgMap){
            val fid = data.key
            val userMsgList = data.value
            val chatSession = getSession(fid)
            chatSession.onPRecvBatchMsg(userMsgList, true)
        }


        // 4) 数据库标记已经发送了接收回执
        TopicDbHelper.updatePChatReplyBatch(msgList)

        // 通知界面更新消息，已经保存处理完了
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(
            MsgEventType.MSG_COMING, 0,
            0L, 0L, mapOf("msg" to "batch forward") )

        // 数据刚好为100,说明可能后续还有数据需要同步
        if (lst.size == SdkGlobalData.LOAD_MSG_BATCH_SERVER){
            loadP2pMessageFromServerOnLogin()
        }

    }

    fun onQueryPChatDataReplyBetween(fid:Long, littleId:Long, bigId:Long,  lst: List<MsgOuterClass.MsgChat>){
        // 用不到
    }

    // 将收到私聊的数据列表按照会话的ID分类
    fun pMessageListSortByTid(msgList: List<MessageContent>): Map<Long, MutableList<MessageContent>>{
        val userMsgMap = HashMap<Long, MutableList<MessageContent>>()
        for (msg in msgList){
            var tid = msg.tId
            if (userMsgMap.containsKey(tid)){
                userMsgMap[tid]!!.add(msg)
            }else{
                val lst = LinkedList<MessageContent>()
                lst.add(msg)
                userMsgMap[tid] =  lst
            }
        }
        return userMsgMap
    }

    fun pbChatDataList2DBMessageList( lst: List<MsgOuterClass.MsgChat>):List<MessageData>{
        val ret = LinkedList<MessageData>()
        for (item in lst){
            val msg = TextHelper.pbMsgToDbMsg(item)
            ret.add(msg)
        }
        return ret
    }
    fun pbChatDataList2MessageContentList(lst: List<MsgOuterClass.MsgChat>):List<MessageContent>{
        val ret = LinkedList<MessageContent>()
        for (item in lst){
            val msg = TextHelper.pbMsg2MessageContent(item)
            if (msg != null){
                ret.add(msg)
            }else{
                Log.e("chatSessionManager", "pbChatDataList2MessageContentList find null msg")
            }

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


    // 页面上下拉控件，触发事件， 加载历史数据
    fun onLoadHistoryMessageOnDrag(session:ChatSession, firstMsg:MessageContent?){
        if (session.isP2pChat()){
            onLoadP2pHistoryMessageOnDrag(session, firstMsg)
        }else{

        }
    }

    // 页面上下拉控件，触发事件， 加载历史数据:  {私聊的数据，先尝试本地加载，这里是从向前边的历史数据开始加载}
    fun onLoadP2pHistoryMessageOnDrag(session:ChatSession, firstMsg:MessageContent?){
        var firstId = Long.MAX_VALUE
        if (firstMsg != null){
            firstId= firstMsg.msgId
        }
        // 向前加载
        val lst = TopicDbHelper.getPChatMessagesById(session.tid, firstId, SdkGlobalData.LOAD_MSG_BATCH, false)
        if (lst != null && lst.size > 0){
            firstId = lst.first().id
        }

        val chatSession = session

        // 添加到界面消息列表中
        chatSession.addP2PMessageToHeadOnDrag(lst, session, false)
        SdkGlobalData.invokeOnEventCallbacks(MsgEventType.MSG_HISTORY, 0, 0, session.getSessionId(), mapOf("from" to "db"))

        // 如果数据库里面的消息太少，这里还需要去服务器找
        if (lst == null || lst.size < SdkGlobalData.LOAD_MSG_BATCH){
            MsgEncocder.sendSynChatDataBackward(firstId, session.getSessionId(), 0)
        }else{
            // 如果是刚刚好，那么等待拖动界面下一次加载，这里啥也不做了
        }
    }
    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // 创建群组结束后
    fun onCreateGroupRet(result:String, detail:String, sendId: Long, msgId: Long, group:Group){
        if (result == "ok"){
            // 写库，群组
            GroupCache.updateGroup(group)

            // 刷新会话界面
            rebuildDisplayList()
            SdkGlobalData.invokeOnEventCallbacks(
                MsgEventType.APP_NOTIFY_CHANGE_SESSION, 0, msgId, group.gid,
                mapOf("group" to group.name))

            // 通知创建成功
            SdkGlobalData.invokeOnEventCallbacks(
                MsgEventType.GROUP_CREATE_OK, 0, msgId, group.gid,
                mapOf("group" to group.name))
        }else{
            //通知创建失败
            SdkGlobalData.invokeOnEventCallbacks(
                MsgEventType.GROUP_CREATE_FAIL,0, msgId, group.gid,
                mapOf("error" to detail))
        }
    }

    // 收到有人加入的应答
    fun onJoinAnswer(result:String, detail:String, sendId: Long, msgId: Long, group:Group, members:List<User>){
        for (m in members){
            if (m.id == SdkGlobalData.selfUserinfo.id){
                if (result == "ok"){
                    // 写库，群组
                    GroupCache.updateGroup(group)

                    // 刷新会话界面
                    rebuildDisplayList()
                    SdkGlobalData.invokeOnEventCallbacks(
                        MsgEventType.APP_NOTIFY_CHANGE_SESSION, 0, msgId, group.gid,
                        mapOf("group" to group.name))

                    // 通知创建成功
                    SdkGlobalData.invokeOnEventCallbacks(
                        MsgEventType.GROUP_JOIN_OK, 0, msgId, group.gid,
                        mapOf("group" to group.name))
                }else{
                    //通知创建失败
                    SdkGlobalData.invokeOnEventCallbacks(
                        MsgEventType.GROUP_JOIN_FAIL,0, msgId, group.gid,
                        mapOf("error" to detail))
                }
                continue
            }

            // 其他人加入了群，这里了需要转换为系统通知
        }

    }


} // end of class