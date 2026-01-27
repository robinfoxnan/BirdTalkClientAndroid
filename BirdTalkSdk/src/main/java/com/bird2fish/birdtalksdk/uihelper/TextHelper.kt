package com.bird2fish.birdtalksdk.uihelper

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.model.ChatSession
import com.bird2fish.birdtalksdk.model.ChatSessionManager
import com.bird2fish.birdtalksdk.model.Drafty
import com.bird2fish.birdtalksdk.model.Group
import com.bird2fish.birdtalksdk.model.GroupCache
import com.bird2fish.birdtalksdk.model.MessageContent
import com.bird2fish.birdtalksdk.model.MessageData
import com.bird2fish.birdtalksdk.model.MessageInOut
import com.bird2fish.birdtalksdk.model.MessageStatus
import com.bird2fish.birdtalksdk.model.Topic
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.model.UserStatus
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatMsgType
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatMsgType.*
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ChatType
import com.bird2fish.birdtalksdk.pbmodel.User.GroupInfo
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.text.DecimalFormat
import kotlin.math.floor
import kotlin.math.pow
import com.google.i18n.phonenumbers.PhoneNumberUtil

import java.io.IOException
import java.io.InputStream
import java.security.MessageDigest
import java.security.SecureRandom
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale
import java.util.Date
import java.util.LinkedList

object  TextHelper {

    private val mAppName: String? = "BirdTalk"
    private val mOsVersion:String? = "6.0"

    // 群组成员的角色转为用户类型
    fun groupMember2User(mem: com.bird2fish.birdtalksdk.pbmodel.User.GroupMember): User {
        val u = User()
        u.id = mem.userId
        u.nick = mem.nick
        u.gid = mem.groupId
        u.icon = mem.icon
        u.role = mem.role

        return u
    }

    fun groupMembers2Users(mems: List<com.bird2fish.birdtalksdk.pbmodel.User.GroupMember>):List<User>{
        val userList = LinkedList<User>()
        for (m in mems){
            userList.add(groupMember2User(m))
        }
        return userList
    }

    // 从服务器返回的类型转换为本地类型
    fun groupInfo2Group(info:GroupInfo): Group {
        val groupId = info.groupId
        val groupName = info.groupName
        val groupType = info.groupType
        var groupIcon = ""
        var groupDes = ""
        var groupJoinType = ""
        var question = ""
        var answer = ""
        var groupVisible = "public"
        var tags = ""
        var membersCount = 0


        if (info.paramsMap!= null){
            if (info.paramsMap["icon"] != null){
                groupIcon = info.paramsMap["icon"]!!
            }
            if (info.paramsMap["jointype"] != null){
                groupJoinType = info.paramsMap["jointype"]!!
            }
            if (info.paramsMap["brief"] != null){
                groupDes = info.paramsMap["brief"]!!
            }

            if (info.paramsMap["visibility"] != null){
                groupVisible = info.paramsMap["visibility"]!!
            }

            if (info.paramsMap["question"] != null){
                question = info.paramsMap["question"]!!
            }

            if (info.paramsMap["answer"] != null){
                answer = info.paramsMap["answer"]!!
            }

            if (info.paramsMap["memberscount"] != null){
                membersCount = info.paramsMap["memberscount"]!!.toInt()
            }
        }
        if (info.tagsList != null && info.tagsList.size > 0)
        {
            tags = info.tagsList.joinToString(separator = "|")
        }

        val group = Group(groupId, 0, groupName,  groupDes, groupIcon,  tags, membersCount, 0,
            groupType,groupJoinType,  groupVisible, question, answer)


        return group
    }

    // 网络消息转换为本地的群组列表
    fun groupInfoList2Groups(infoList : List<GroupInfo>):List<Group>{
        val gList = LinkedList<Group>()
        for (info in infoList){
            val g = groupInfo2Group(info)
            gList.add(g)
        }
        return gList
    }

    fun splitTags(tagString: String?): List<String> {
        if (tagString.isNullOrBlank()) return emptyList()

        // 正则匹配所有分隔符：空格、逗号、中英文逗号、顿号、|、/
        val regex = "[ ,，、|/]".toRegex()

        return tagString
            .split(regex)
            .map { it.trim() }       // 去掉首尾空格
            .filter { it.isNotEmpty() } // 去掉空字符串
            .distinct()               // 可选：去重
    }

    /**
     * 适配 API 24+，获取当前日期（年月日，格式：yyyy-MM-dd）
     * 例：2025-10-18
     * 核心：用 Calendar 类（API 1 就支持）+ SimpleDateFormat 实现，无版本限制
     */
    fun getCurrentDateString(): String {
        try {
            // 1. 获取当前系统时间的 Calendar 实例（API 1 兼容）
            val calendar = Calendar.getInstance()

            // 2. 定义日期格式：yyyy-MM-dd，指定 Locale.CHINA 避免系统语言影响（如英文月份）
            // SimpleDateFormat 虽有线程安全问题，但此处是方法内创建局部对象，无风险
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)

            // 3. 将 Calendar 转成 Date 对象，再格式化为字符串
            return dateFormatter.format(calendar.time)
        } catch (e: Exception) {
            // 异常兜底：避免系统时间异常导致崩溃，返回默认日期（可按需修改）
            e.printStackTrace()
            return "1970-01-01"
        }
    }

    // 收到的消息也有可能是远端的多端登录发来的啊，
    fun pbMsg2MessageContent(chatMsg :MsgOuterClass.MsgChat):MessageContent? {
        var msg : MessageContent? = null
        var inOut  = true
        var tid = 0L
        var sid = 0L

        // 如果是群聊数据，这里要考虑是多端登录的情况
        if (chatMsg.chatType == ChatType.ChatTypeGroup){
            tid = chatMsg.toId
            sid = -tid
            if (chatMsg.fromId == SdkGlobalData.selfUserinfo.id) {
                inOut = false
            }else{
                inOut = true
            }
        }else{
            // 这里是私聊的情况
            if (chatMsg.fromId == SdkGlobalData.selfUserinfo.id){
                inOut = false
                tid = chatMsg.toId
            }else{
                inOut = true
                tid = chatMsg.fromId
            }
            sid = tid
        }


        // 这里一定保证返回一个合法的数据
        val chatSession = ChatSessionManager.getSession(sid)
        msg = when (chatMsg.msgType) {
            TEXT -> {
                val utf8String = chatMsg.data.toString(Charsets.UTF_8)

                MessageContent(chatSession, chatMsg.msgId, chatMsg.sendId, MessageStatus.OK, inOut,
                    chatMsg.sendReply, chatMsg.recvReply, chatMsg.readReply, utf8String, null, chatMsg.msgType)
            }

            IMAGE -> {
                val txt = chatMsg.data.toString(Charsets.UTF_8)
                val draft = deserializeDrafty(txt)
                Log.d("Image Drafty", txt)

                MessageContent(chatSession, chatMsg.msgId, chatMsg.sendId, MessageStatus.OK, inOut,
                    chatMsg.sendReply, chatMsg.recvReply, chatMsg.readReply, "", draft, chatMsg.msgType)
            }

            VOICE -> {
                val txt = chatMsg.data.toString(Charsets.UTF_8)
                val draft = deserializeDrafty(txt)
                Log.d("Audio Drafty", txt)

                MessageContent(chatSession, chatMsg.msgId, chatMsg.sendId, MessageStatus.OK, inOut,
                    chatMsg.sendReply, chatMsg.recvReply, chatMsg.readReply, "", draft, chatMsg.msgType)
            }

            FILE -> {
                val txt = chatMsg.data.toString(Charsets.UTF_8)
                val draft = deserializeDrafty(txt)
                Log.d("File Drafty", txt)

                MessageContent(chatSession, chatMsg.msgId, chatMsg.sendId, MessageStatus.OK, inOut,
                    chatMsg.sendReply, chatMsg.recvReply, chatMsg.readReply, "", draft, chatMsg.msgType)
            }

            else-> null
        }

        // 群组显示的图标不一样呢
        if (chatMsg.chatType == ChatType.ChatTypeGroup){
            val fid = chatMsg.fromId
            val f = GroupCache.findGroupUser(tid, fid)
            if (f != null){
                msg?.iconUrl = f.icon
                msg?.nick = f.nick
            }
            msg?.userId = fid
        }


        return msg
    }

    // 网络数据包转数据库类型
    fun pbMsgToDbMsg(chatMsg: MsgOuterClass.MsgChat):MessageData{
        var inOut :Int
        var tid:Long
        if (chatMsg.chatType == ChatType.ChatTypeGroup){
            // toId是接受者，这里是群ID
            tid = chatMsg.toId
            if (chatMsg.fromId == SdkGlobalData.selfUserinfo.id){
                inOut = MessageInOut.OUT.ordinal
            }else{
                inOut = MessageInOut.IN.ordinal
            }

        }else{
            // 私聊可能收到的就是互相发的数据，这里要考虑多终端的情况
            if (chatMsg.fromId == SdkGlobalData.selfUserinfo.id){
                inOut = MessageInOut.OUT.ordinal
                tid = chatMsg.toId
            }else{
                inOut = MessageInOut.IN.ordinal
                tid = chatMsg.fromId
            }
        }

        val data = chatMsg.data.toByteArray()
        val isPlain = if (chatMsg.msgType == TEXT) 1 else 0

        var status = MessageStatus.OK.name
        if (chatMsg.readReply > 0){
            status = MessageStatus.SEEN.name
        }else if (chatMsg.recvReply > 0){
            status = MessageStatus.RECV.name
        }else if (chatMsg.sendReply > 0){
            status = MessageStatus.OK.name
        }


        // 多终端的情况下，收到的消息，只是同步过来的，不需要回执
        val msg = MessageData(chatMsg.msgId, tid, chatMsg.fromId, chatMsg.sendId,
            chatMsg.devId,
            inOut, chatMsg.msgType.name, data, isPlain,
            chatMsg.tm, chatMsg.sendReply, chatMsg.recvReply, chatMsg.readReply,
            chatMsg.encType.name, chatMsg.keyPrint.toInt(), status)

        return msg
    }

    //从给控件用的，转换到数据库用的类，
    // 由于这里并没有全局的msgid, 这里仅仅使用sendId 代替，
    fun MsgContentToDbMsg(msg:MessageContent):MessageData{
        val isPlain = if (msg.msgType == TEXT) 1 else 0
        val data =  if (msg.msgType == TEXT) msg.text.toByteArray(Charsets.UTF_8) else serializeDrafty(msg.content!!).toByteArray(Charsets.UTF_8)
        val inout:Int = if (msg.inOut) MessageInOut.IN.ordinal  else MessageInOut.OUT.ordinal


        val tid = msg.tId
        val msg = MessageData(msg.msgId, tid, msg.userId, msg.sendId,
            SdkGlobalData.basicInfo.deviceId,
            inout, msg.msgType.name, data, isPlain,
            msg.tm, msg.tm1, msg.tm2, msg.tm3,
            "", 0, msg.msgStatus.name)
        return msg
    }

    // 数据库加载数据后
    fun MsgContentFromDbMessageP2p(msg: MessageData, session: ChatSession): MessageContent{

        var txt = ""
        var content:Drafty? = null

        val plain = String(msg.data, Charsets.UTF_8)
        if (msg.msgType  == TEXT.name){
            txt =  plain
        }else{
            try {
                content = deserializeDrafty(plain)
            }
            catch (e:Exception){
                Log.e("Sdk", e.toString())
                txt = "解析错误"
                Log.e("Sdk 解析消息错误", plain)
            }finally {

            }

        }

        var inOut = (msg.io == MessageInOut.IN.ordinal)

        var msgStatus = MessageStatus.SENDING
        try {
            msgStatus = MessageStatus.valueOf(msg.status)
        }catch (e:Exception){
            Log.e("Sdk", e.toString())
        }

        val msgContent = MessageContent(
            session,
            msg.id,
            msg.sendId,
            msgStatus,
            inOut,
            msg.tm1,
            msg.tm2,
            msg.tm3,
            txt,
            content,
            ChatMsgType.valueOf(msg.msgType)
        )
        msgContent.tm = msg.tm

        return msgContent
    }

    fun MsgContentFromDbMessageGroup(msg: MessageData, session: ChatSession): MessageContent{

        var txt = ""
        var content:Drafty? = null

        val plain = String(msg.data, Charsets.UTF_8)
        if (msg.msgType  == TEXT.name){
            txt =  plain
        }else{
            try {
                content = deserializeDrafty(plain)
            }
            catch (e:Exception){
                Log.e("Sdk", e.toString())
                txt = "解析错误"
                Log.e("Sdk 解析消息错误", plain)
            }finally {

            }

        }

        var inOut = (msg.io == MessageInOut.IN.ordinal)

        var msgStatus = MessageStatus.SENDING
        try {
            msgStatus = MessageStatus.valueOf(msg.status)
        }catch (e:Exception){
            Log.e("Sdk", e.toString())
        }

        val msgContent = MessageContent(
            session,
            msg.id,
            msg.sendId,
            msgStatus,
            inOut,
            msg.tm1,
            msg.tm2,
            msg.tm3,
            txt,
            content,
            ChatMsgType.valueOf(msg.msgType)
        )
        msgContent.tm = msg.tm

        val fid = msg.uid
        val f = GroupCache.findGroupUser(msg.tid, fid)
        if (f != null){
            msgContent.iconUrl = f.icon
            msgContent.nick = f.nick
        }
        msgContent.userId = fid

        return msgContent
    }
    ///////////////////////////////////////////////////////////////////////////////////////

    fun getFileNameFromUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val lastSegment = uri.lastPathSegment
            if (!lastSegment.isNullOrEmpty()) lastSegment else "download_${System.currentTimeMillis()}"
        } catch (e: Exception) {
            "download_${System.currentTimeMillis()}"
        }
    }


    fun hasDownloadFile(filename:String): Boolean{
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val outFile = File(downloadsDir, filename)

        // ✅ 已存在则直接返回
        if (outFile.exists() && outFile.length() > 0) {
            //FileDownloader.showToast(context, "文件已存在，直接打开")
            return true
        }

        return false
    }
    /**
     * 将Drafty对象序列化为JSON字符串
     * @param drafty 要序列化的Drafty对象
     * @return 序列化后的JSON字符串
     * @throws Exception 序列化过程中可能抛出的异常
     */
    @Throws(Exception::class)
    fun serializeDrafty(drafty: Drafty): String {
        val mapper = ObjectMapper()
        // Jackson会自动识别Drafty类中的Jackson注解并应用相应规则
        return mapper.writeValueAsString(drafty)
    }

    /**
     * 将JSON字符串反序列化为Drafty对象
     * @param json 要反序列化的JSON字符串
     * @return 反序列化后的Drafty对象
     * @throws Exception 反序列化过程中可能抛出的异常
     */
    @Throws(Exception::class)
    fun deserializeDrafty(json: String): Drafty {
        val mapper = ObjectMapper()
        return mapper.readValue(json, Drafty::class.java)
    }


    fun showDialogInCallback(context: Context, message: String) {
        // 假设这是你的回调
        (context as? Activity)?.runOnUiThread {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }


    /**
     * 常见文件扩展名与 MIME 类型对应表：
     *
     * 文件类型              扩展名                      MIME 类型
     * ---------------------------------------------------------------
     * PDF 文档              .pdf                        application/pdf
     * Word 文档（旧版）     .doc                        application/msword
     * Word 文档（新版）     .docx                       application/vnd.openxmlformats-officedocument.wordprocessingml.document
     * Excel 表格（旧版）    .xls                        application/vnd.ms-excel
     * Excel 表格（新版）    .xlsx                       application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     * PowerPoint（旧版）    .ppt                        application/vnd.ms-powerpoint
     * PowerPoint（新版）    .pptx                       application/vnd.openxmlformats-officedocument.presentationml.presentation
     * MP3 音频              .mp3                        audio/mpeg
     * MP4 视频              .mp4                        video/mp4
     * PNG 图片              .png                        image/png
     * JPG 图片              .jpg / .jpeg                image/jpeg
     * ZIP 压缩包            .zip                        application/zip
     * 纯文本                .txt                        text/plain
     */

    /**
    * 根据 Uri 获取文件的 MIME 类型
     *
     * @param context 应用上下文
     * @param uri 文件 Uri
     * @return MIME 类型字符串，如果无法确定则返回 null
     */
    fun getMimeTypeFromUri(context: Context, uri: Uri?): String? {
        if (uri == null) return null

        val mime =  when {
            // 处理 content:// Uri
            ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true) -> {
                try {
                    // 尝试通过 ContentResolver 获取 MIME 类型
                    context.contentResolver.getType(uri) ?: run {
                        // 如果 ContentResolver 无法确定，尝试从路径扩展名推断
                        val path = getFilePathFromUri(context, uri)
                        path?.let { getMimeTypeFromExtension(it) }
                    }
                } catch (e: Exception) {
                    // 出错时尝试从路径扩展名推断
                    val path = getFilePathFromUri(context, uri)
                    path?.let { getMimeTypeFromExtension(it) }
                }
            }

            // 处理 file:// Uri
            ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true) -> {
                uri.path?.let { getMimeTypeFromExtension(it) }
            }

            // 处理其他类型的 Uri（如 http://、ftp:// 等）
            else -> {
                uri.path?.let { getMimeTypeFromExtension(it) } ?: uri.lastPathSegment?.let { getMimeTypeFromExtension(it) }
            }
        }

        return mime ?: "application/octet-stream"
    }

    /**
     * 从文件路径或文件名推断 MIME 类型
     */
    private fun getMimeTypeFromExtension(filePath: String): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(filePath)
        return if (extension.isNotEmpty()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
        } else {
            null
        }
    }

    /**
     * 从 Uri 获取文件路径（辅助函数）
     * 注意：此方法在某些情况下可能返回 null，建议结合 getMimeTypeFromUri 使用
     */
    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
                path = it.getString(columnIndex)
            }
        }

        return path
    }

    /**
     * 生成指定长度的随机字符串（使用安全随机数生成器）
     *
     * @param length 生成的字节数（注意：Base64编码后字符串长度会增加约33%）
     * @return 编码后的随机字符串
     */
    fun generateRandomByteString(length: Int = 10): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)

        // 使用Base64编码确保生成的字符串可打印且长度固定
        // 使用URL安全变体，避免包含'+'和'/'字符
        //return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        // 使用Android提供的Base64类（URL安全模式，不含填充）
        return Base64.encodeToString(bytes, Base64.NO_WRAP or Base64.URL_SAFE or Base64.NO_PADDING)
    }


    private fun queryFileNameFromMediaStore(context: Context, contentUri: Uri, id: String): String? {
        val selection = "_id=?"
        val selectionArgs = arrayOf(id)
        return queryFileName(context, contentUri, selection, selectionArgs)
    }


    /**
     * 从 Uri 中提取文件名
     *
     * @param context 应用上下文
     * @param uri 要处理的 Uri
     * @return 提取的文件名，如果无法提取则返回 null
     */
    fun getFileNameFromUri(context: Context, uri: Uri?): String? {
        if (uri == null) return null

        return when {
            // 处理 content:// Uri
            ContentResolver.SCHEME_CONTENT.equals(uri.scheme, ignoreCase = true) -> {
                // 对于 Android 4.4 (API 19) 及以上版本的 DocumentProvider
                val isDocumentUri = try {
                    DocumentsContract.isDocumentUri(context, uri)
                } catch (e: Exception) {
                    false
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && isDocumentUri) {
                    // ExternalStorageProvider
                    if (isExternalStorageDocument(uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val split = docId.split(":").toTypedArray()
                        if (split.size >= 2) {
                            return split[1]
                        }
                    }
                    // DownloadsProvider
                    else if (isDownloadsDocument(uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)

                        // 1️ raw: 前缀，直接用路径
                        if (docId.startsWith("raw:")) {
                            return File(docId.removePrefix("raw:")).name
                        }

                        // 2️  对非 raw: 文件，直接 SAF 查询 Display Name
                        return queryFileName(context, uri)
                    }
                    // MediaProvider
                    else if (isMediaDocument(uri)) {
                        val docId = DocumentsContract.getDocumentId(uri)
                        val parts = docId.split(":").toTypedArray()
                        if (parts.size < 2) return null

                        val type = parts[0]
                        val id = parts[1]
                        val volumeName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaStore.getExternalVolumeNames(context).firstOrNull() ?: "external"
                        } else "external"

                        val contentUri = when (type) {
                            "image" -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                            "video" -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                            "audio" -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

                            else -> {
                                return queryFileName(context, uri)
                            }
                        }

                        val selection = "_id=?"
                        val selectionArgs = arrayOf(id)

                        return queryFileName(context, contentUri, selection, selectionArgs)
                    }
                }

                // 普通的 ContentProvider Uri
                return queryFileName(context, uri)
            }

            // 处理 file:// Uri
            ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true) -> {
                uri.path?.let { path ->
                    return File(path).name
                }
                null
            }
            else -> {  // 其他情况
                uri.lastPathSegment ?: uri.path?.let { File(it).name }
            }
        }
    }


    /*
    实测结论（基于 A12, A13, MIUI, ColorOS）
    来源	URI 示例	正确 Provider
    Chrome 下载	content://com.android.providers.downloads.documents/document/531	✅ public_downloads
    微信/QQ 文件	content://com.android.providers.media.documents/document/document:115843	✅ my_downloads / all_downloads
    文件管理器复制的文件	content://media/external/file/1234	✅ MediaStore.Files
    系统相册选择	content://media/external/images/media/1024	✅ MediaStore.Images.Media
     */
    private fun resolveDownloadsFileName(context: Context, id: String): String? {
        val downloadUris = listOf(
            "content://downloads/public_downloads",
            "content://downloads/my_downloads",
            "content://downloads/all_downloads"
        )

        for (base in downloadUris) {
            val contentUri = ContentUris.withAppendedId(Uri.parse(base), id.toLongOrNull() ?: continue)
            queryFileName(context, contentUri)?.let { return it }
        }

        // 尝试 raw: 或 uuid 场景
        if (id.startsWith("raw:")) {
            return File(id.removePrefix("raw:")).name
        }

        return null
    }


    // 实现一个小工具函数检测某个 ID 是否存在
    private fun existsInMediaStore(context: Context, contentUri: Uri, id: String): Boolean {
        return context.contentResolver.query(
            contentUri,
            arrayOf("_id"),
            "_id=?",
            arrayOf(id),
            null
        )?.use { it.moveToFirst() } ?: false
    }

    /**
     * 四、简化版核心结论
     * 文件来源	URI 类型	正确获取方式
     * 系统图库、相机	content://media/external/images/media/xxx	MediaStore 查询
     * 下载文件	content://downloads/...	DownloadsProvider 查询
     * 自定义目录（例如 /000/xxx.pdf）	content://com.android.providers.media.documents/document/document:xxxx	✅
     * 直接 queryFileName(context, uri)
     * 第三方文件管理器 / SAF 选择	任意 content://document/ 形式	✅ 直接 OpenableColumns.DISPLAY_NAME 查询
     */
    private fun queryFileName(
        context: Context,
        uri: Uri,
        selection: String? = null,
        selectionArgs: Array<String>? = null
    ): String? {
        var cursor: Cursor? = null
        val projection = arrayOf(MediaStore.Files.FileColumns.DISPLAY_NAME)

        try {
            cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                null
            )

            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                // 确保文件名非空
                val fileName = cursor.getString(index)
                if (!TextUtils.isEmpty(fileName)) {
                    return fileName
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        return ""
        //return uri.lastPathSegment?.takeIf { it.isNotEmpty() }
    }

    /**
     * 判断 Uri 是否为 ExternalStorageProvider
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * 判断 Uri 是否为 DownloadsProvider
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * 判断 Uri 是否为 MediaProvider
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }


    /**
     * 计算 Uri 对应文件的大小（字节）
     * @param uri 文件的 Uri，可为空
     * @return 文件大小（字节），若出错或无法获取则返回 -1
     */
    fun getFileSize(context: Context, uri: Uri?): Long {
        // 输入验证
        if (uri == null) return -1

        // 处理 content:// 类型的 Uri
        if (ContentResolver.SCHEME_CONTENT == uri.scheme) {
            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(
                    uri,
                    arrayOf(OpenableColumns.SIZE),
                    null,
                    null,
                    null
                )

                if (cursor != null && cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        return cursor.getLong(sizeIndex)
                    }
                }
            } catch (e: Exception) {
                // 忽略异常，尝试其他方法
            } finally {
                cursor?.close()
            }
        }

        // 处理 file:// 类型的 Uri 或 content Uri 无法获取大小时
        try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { descriptor ->
                return descriptor.statSize
            }
        } catch (e: IOException) {
            // 文件访问失败
        }

        return -1
    }


    fun calculateMD5FromUri(context: Context, uri: Uri?): String {
        if (uri == null) return ""

        var inputStream: InputStream? = null
        return try {
            // 打开 Uri 的输入流
            inputStream = context.contentResolver.openInputStream(uri) ?: return ""

            // 创建 MD5 消息摘要
            val md5Digest = MessageDigest.getInstance("MD5")

            // 使用缓冲区循环读取数据
            val buffer = ByteArray(1024 *1024) // 8KB 缓冲区
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md5Digest.update(buffer, 0, bytesRead)
            }

            // 将 MD5 字节数组转换为十六进制字符串
            md5Digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // 打印异常日志
            e.printStackTrace()
            ""
        } finally {
            // 确保关闭流
            try {
                inputStream?.close()
            } catch (closeException: Exception) {
                closeException.printStackTrace()
            }
        }
    }


    /**
     * 删除外部存储目录中的指定文件。
     *
     * @param context 应用上下文，用于获取外部文件目录
     * @param fileName 要删除的文件名
     * @return 删除操作的结果：1表示成功，0表示失败
     */
    fun deleteBinaryFile(context: Context, fileName: String): Int {
        try {
            // 构建文件路径，与写入函数使用相同的逻辑
            val file = File(context.getExternalFilesDir(null), fileName)

            // 检查文件是否存在
            if (file.exists()) {
                // 执行删除操作
                val isDeleted = file.delete()
                return if (isDeleted) 1 else 0
            } else {
                // 文件不存在，返回失败
                return 0
            }
        } catch (e: Exception) {
            // 异常处理
            e.printStackTrace()
            return 0
        }
    }
    // 写入二进制文件
    fun writeToBinaryFile(context: Context, data: ByteArray, fileName: String):Int {
        try {
            val uri = File(context.getExternalFilesDir(null), fileName).toUri()
            val file: File = uri.toFile()
            //file.writeText(text)
            return file.writeBytes(data) as Int

        } catch (e: Exception) {
            e.printStackTrace()
            //throw RuntimeException("Failed to write to file: $fileName", e)
            return 0
        }
    }

    // 从二进制文件读取
    fun readFromBinaryFile(context: Context, fileName: String): ByteArray? {
        return try {
            val uri = File(context.getExternalFilesDir(null), fileName).toUri()
            val file: File = uri.toFile()
            if (file.exists()) file.readBytes() else null
        } catch (e: Exception) {
            e.printStackTrace()
            //throw RuntimeException("Failed to read from file: $fileName", e)
            return null
        }
    }

    // 从二进制文件读取
    fun readFromBinaryFile(context: Context, uri: Uri): ByteArray? {
        return try {
            // 使用 ContentResolver 打开输入流
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // 将输入流转换为字节数组
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
//    fun readFromBinaryFile(context: Context, uri: Uri): ByteArray? {
//        return try {
//            val file: File = uri.toFile()
//            if (file.exists()) file.readBytes() else null
//        } catch (e: Exception) {
//            e.printStackTrace()
//            //throw RuntimeException("Failed to read from file: $fileName", e)
//            return null
//        }
//    }

    fun getRequestHeaders(): Map<String, String> {
        val headers = HashMap<String, String>()
//        if (mApiKey != null) {
//            headers["X-Tinode-APIKey"] = mApiKey
//        }
//        if (mAuthToken != null) {
//            headers["X-Tinode-Auth"] = "Token $mAuthToken"
//        }
        headers["User-Agent"] = makeUserAgent()
        return headers
    }

    private fun makeUserAgent(): String {
        return (mAppName + " (Android " + mOsVersion + "; "
                + Locale.getDefault() + "); " + "bird_talk_sdk")
    }

    // 显示简单的 Toast 消息
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, message, duration).show()
    }

    // 显示简单的 Toast 消息，使用默认的短时间
    fun showToast(context: Context, messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(context, messageResId, duration).show()
    }

    fun bytesToHumanSize(bytes: Long): String {
        if (bytes <= 0) {
            // 0x202F - narrow non-breaking space.
            return "0\u202FBytes"
        }
        val k = 1024.0

        val sizes = arrayOf("Bytes", "KB", "MB", "GB", "TB")
        val bucket = (63 - java.lang.Long.numberOfLeadingZeros(bytes)) / 10
        val count: Double = bytes / k.pow(bucket.toDouble())
        val roundTo = if (bucket > 0) (if (count < 3) 2 else (if (count < 30) 1 else 0)) else 0
        val fmt = DecimalFormat.getInstance()
        fmt.maximumFractionDigits = roundTo
        return fmt.format(count) + "\u202F" + sizes[bucket]
    }

    fun millisToTime1(millis: Long): String {
        val msgDate = Calendar.getInstance().apply { timeInMillis = millis }
        val now = Calendar.getInstance()

        return if (msgDate.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            msgDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        ) {
            // 今天，显示 HH:mm:ss
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            sdf.format(Date(millis))
        } else {
            // 之前的日期，显示 yyyy-MM-dd
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(Date(millis))
        }
    }

    fun millisToTime(millis: Int): String {
        val sb = StringBuilder()
        val duration = millis / 1000f
        val min = floor((duration / 60f).toDouble()).toInt()
        if (min < 10) {
            sb.append("0")
        }
        sb.append(min).append(":")
        val sec = (duration % 60f).toInt()
        if (sec < 10) {
            sb.append("0")
        }
        return sb.append(sec).toString()
    }

    // 拼接服务器的基础地址和给出的部分字符串，如果出错了，给出一个默认的地址
    fun toAbsoluteURL(value:String) : URL {
        val uri = Uri.parse(value)
        if (uri.isAbsolute){

            try {
                return URL(value) // 使用 Uri 的字符串表示创建 URL
            } catch (ignored: MalformedURLException) {
                return URL("https://127.0.0.1")
            }

        }

        var fullUrl = CryptHelper.getUrl(value)

        var url: URL? = URL("https://127.0.0.1")
        try {
            url = URL(fullUrl)
        } catch (ignored: MalformedURLException) {
        }
        return url!!
    }

    fun isUriAbsolute(uriString: String): Boolean {
        val uri = Uri.parse(uriString)
        return uri.isAbsolute // 或者手动检查 scheme 是否为空： uri.scheme != null
    }

    /**
     * 使用 libphonenumber 验证国际手机号
     */
    private fun isValidInternationalPhone(phone: String): Boolean {
        return try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val number = phoneUtil.parse(phone, "ZZ") // ZZ表示未知国家代码
            phoneUtil.isValidNumber(number)
        } catch (e: Exception) {
            false
        }
    }

    // 检查输入是否合法
    fun checkStringType(input: String): String {
        // 检查是否为纯数字
        if (input.all { it.isDigit() }) {
            return "id"
        }

        // 检查是否为电子邮件
        val emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        if (input.matches(emailPattern.toRegex())) {
            return "email"
        }

        // 检查是否为手机号（以中国手机号为例，11位数字，以1开头）
//        val phonePattern = "^1\\d{10}$"
//        if (input.matches(phonePattern.toRegex())) {
//            return "phone"
//        }
        if (isValidInternationalPhone(input)){
            return "phone"
        }

        // 不符合任何类型
        return "invalid"
    }
}