package com.bird2fish.birdtalksdk.net
import android.content.Context
import android.provider.Settings.Global
import android.telephony.mbms.FileInfo
import android.text.TextUtils
import android.util.Log
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.db.TopicDbHelper
import com.bird2fish.birdtalksdk.model.ChatSessionManager
import com.bird2fish.birdtalksdk.model.Drafty
import com.bird2fish.birdtalksdk.pbmodel.*
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.*
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ComMsgType.*
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ErrorMsgType.*
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ErrorMsgType.UNRECOGNIZED
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.QueryDataType.*
import com.bird2fish.birdtalksdk.pbmodel.User.FriendOpReq
import com.bird2fish.birdtalksdk.pbmodel.User.GroupInfo
import com.bird2fish.birdtalksdk.pbmodel.User.UserInfo
import com.bird2fish.birdtalksdk.pbmodel.User.UserOpReq
import com.bird2fish.birdtalksdk.pbmodel.User.UserOpResult
import com.bird2fish.birdtalksdk.pbmodel.User.UserOperationType.*
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import com.bird2fish.birdtalksdk.uihelper.UserHelper
import com.google.protobuf.ByteString
import java.util.LinkedHashMap
import java.util.LinkedList

// 按照格式编码，用于产生各种消息
class MsgEncocder {

    companion object {

        // 读写文件的默认路径（可根据实际需求调整路径）
        private const val KEYPRINT_FILE_NAME = "keyPrint.bin"
        private const val SHAREKEY_FILE_NAME = "shareKey.bin"
        private  var context: Context? =null
        private  val keyExchange  =  ECDHKeyExchange()

        fun setContext(ctx: Context?){
            context = ctx
        }
        fun longToByteArray(value: Long): ByteArray {
            val byteArray = ByteArray(Long.SIZE_BYTES) // Long.SIZE_BYTES = 8
            for (i in byteArray.indices) {
                byteArray[i] = (value shr (8 * i) and 0xFF).toByte()
            }
            return byteArray
        }

        fun byteArrayToLong(bytes: ByteArray): Long {
            require(bytes.size == Long.SIZE_BYTES) { "ByteArray size must be ${Long.SIZE_BYTES}" }
            var value: Long = 0
            for (i in bytes.indices) {
                value = value or ((bytes[i].toLong() and 0xFF) shl (8 * i))
            }
            return value
        }


        // 写入 keyPrint
        fun saveKeyPrint( keyPrint: Long) {

            val data = longToByteArray(keyPrint)
            TextHelper.writeToBinaryFile(context!!, data, KEYPRINT_FILE_NAME)
        }

        // 读取 keyPrint
        fun loadKeyPrint(): Long {
            val data = TextHelper.readFromBinaryFile(context!!, KEYPRINT_FILE_NAME) ?: return 0
            return byteArrayToLong(data)
        }

        fun deleteKeyFiles() {
            TextHelper.deleteBinaryFile(context!!, KEYPRINT_FILE_NAME)
            TextHelper.deleteBinaryFile(context!!, SHAREKEY_FILE_NAME)
        }

        // 写入 shareKey
        fun saveShareKey(shareKey: ByteArray) {
            TextHelper.writeToBinaryFile(context!!, shareKey, SHAREKEY_FILE_NAME)
        }

        // 读取 shareKey
        fun loadShareKey(): ByteArray? {
            return TextHelper.readFromBinaryFile(context!!, SHAREKEY_FILE_NAME)
        }

        // 最外层的封装
        fun wrapMsg(plainMsg: MsgOuterClass.MsgPlain. Builder, tm :Long, t: ComMsgType):MsgOuterClass.Msg{
            val msg = Msg.newBuilder().setMsgType(t)
                .setPlainMsg(plainMsg)
                .setVersion(1)
                .setTm(tm)
                .build()

            return msg
        }

        // 消息分发
        fun onMsg(msg: MsgOuterClass.Msg){
            when (msg.msgType) {

                // 初次应答，可能是秘钥交换
                MsgTHello -> {
                    onHello(msg.plainMsg.hello)
                }

                MsgTError -> {
                    onError(msg)
                }
                MsgTKeyExchange -> {
                    onExchangeMsg(msg)
                }
                MsgTChatMsg -> onRecvChatMsg(msg)      // 聊天消息
                MsgTChatReply -> onRecvChatReply(msg)  // 聊天的回执

                MsgTQueryResult -> onQueryReply(msg)   // 查询消息，查询回执，各种查询

                MsgTUploadReply -> onUploadReply(msg)
                MsgTDownloadReply -> doNothing()

                MsgTUserOpRet -> onUserRet(msg)         // 登录等操做，也在这里来
                MsgTFriendOp -> doNothing()
                MsgTFriendOpRet -> onFriendOpRet(msg)   // 所有好友操作相关的处理
                MsgTGroupOp -> doNothing()
                MsgTGroupOpRet -> doNothing()

                MsgTOther -> doNothing()
                ComMsgType.UNRECOGNIZED -> doNothing()
                MsgTQuery -> doNothing()
                MsgTUpload -> doNothing()
                MsgTDownload -> doNothing()
                MsgTUserOp -> doNothing()
                MsgTUnused -> doNothing()
                MsgTHeartBeat -> doNothing()
            }
        }

        fun doNothing(){

        }

        // 查询消息等返回的结果
        fun onQueryReply(msg: MsgOuterClass.Msg){
            val reply = msg.plainMsg.commonQueryRet
            when (reply.queryType){
                QueryDataTypeChatData -> onQueryChatDataReply(reply)
                QueryDataTypeChatReply -> doNothing()
                QueryDataTypeFriendOP -> doNothing()
                QueryDataTypeGroupOP -> doNothing()
                QueryDataType.UNRECOGNIZED -> doNothing()
            }
        }

        // 查询消息返回的结果
        fun onQueryChatDataReply(reply: MsgOuterClass. MsgQueryResult){
            if (reply.synType == SynType.SynTypeForward){
                if (reply.chatType == ChatType.ChatTypeP2P){
                   ChatSessionManager.onQueryPChatDataReplyForward(reply.littleId, reply.bigId, reply.chatDataListList)
                }else{
                    ChatSessionManager.onQueryGChatDataReplyForward(reply.groupId, reply.littleId, reply.bigId, reply.chatDataListList)
                }

            }else if (reply.synType == SynType.SynTypeBackward){
                if (reply.chatType == ChatType.ChatTypeP2P) {
                    ChatSessionManager.onQueryPChatDataReplyBackward(reply.littleId, reply.bigId, reply.chatDataListList)
                }else{
                    ChatSessionManager.onQueryGChatDataReplyBackward(reply.groupId, reply.littleId, reply.bigId, reply.chatDataListList)
                }
            }else{
                if (reply.chatType == ChatType.ChatTypeP2P) {
                    ChatSessionManager.onQueryPChatDataReplyBetween(reply.littleId, reply.bigId, reply.chatDataListList)
                }else{
                    ChatSessionManager.onQueryGChatDataReplyBetween(reply.groupId, reply.littleId, reply.bigId, reply.chatDataListList)
                }
            }

        }

        // 当收到消息的时候
        fun onRecvChatMsg(msg: MsgOuterClass.Msg){
            val chatMsg = msg.plainMsg.chatData

            // 添加到各个对话
            ChatSessionManager.onRecvChatMsg(chatMsg)

        }

        // 聊天的回执，
        fun onRecvChatReply(msg: MsgOuterClass.Msg){
            val reply = msg.plainMsg.chatReply

            ChatSessionManager.onChatMsgReply(reply.msgId, reply.sendId, reply.fromId,  reply.paramsMap,
                reply.sendOk,reply.recvOk, reply.readOk, reply.extraMsg)

            val result = reply.extraMsg
            var detail = reply.paramsMap["detail"]
            var gid = reply.paramsMap["gid"]   // 私聊这里是“0”
            val fid = reply.fromId

            if (detail == null)
                detail = ""
            if (gid == null){
                gid = "0"
            }

            val resultMap = mapOf(
                "result" to result,
                "detail" to detail!!,
                "gid" to gid!!,
                "fid" to fid.toString(),
            )
            if (result == "ok"){
                // 通知界面更新消息，已经保存处理完了
                SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.MSG_SEND_OK, 0,
                    reply.msgId, reply.fromId, resultMap)
            }else{
                // "fail"
                // 如果发送消息失败，说明对方已经删除了自己
                if (gid == "0" && detail == "not friend")
                {
                    SdkGlobalData.updateDeleteFan(fid)
                    ChatSessionManager.onChatMsgReplyError(reply.fromId, reply.msgId, reply.sendId, detail)
                }
                SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.MSG_SEND_ERROR, 0,
                    reply.msgId, reply.fromId, resultMap)
            }

        }


        // 处理错误
        fun onError(msg: MsgOuterClass.Msg) {
            when (msg.plainMsg.errorMsg.code) {
                ErrTNone.number -> {
                    // 无错误，通常不需要处理
                }
                ErrTVersion.number -> {
                    onVersionError()
                }
                ErrTKeyPrint.number -> {
                    onKeyPrintError()
                }
                ErrTRedirect.number -> {
                    //onRedirectError()
                }
                ErrTWrongPwd.number -> {
                    onWrongPasswordError()
                }
                ErrTWrongCode.number -> {
                    //onWrongVerificationCodeError()
                }
                ErrTRsaPrint.number -> {
                    //onRsaKeyError()
                }
                ErrTTempKey.number -> {
                    //onTemporaryKeyError()
                }
                ErrTEncType.number -> {
                    // onEncryptionTypeNotSupportedError()
                }
                ErrTServerInside.number -> {
                    //onServerInternalError()
                }
                ErrTStage.number -> {
                    // onStageFieldError()
                }
                ErrTPublicKey.number -> {
                    // onPublicKeyError()
                }
                ErrTKeyConflict.number -> {
                    //onKeyConflictError()
                }
                ErrTCheckData.number -> {
                    //onVerificationDataError()
                }
                ErrTMsgContent.number -> {
                    //onMessageContentError()
                }
                ErrTNotLogin.number -> {
                    //onNotLoggedInError()
                }
                ErrTNotPermission.number -> {
                    //onPermissionDeniedError()
                }
                ErrTDisabled.number -> {
                    //onUserOrPostDisabledError()
                }
                ErrTDeleted.number -> {
                    //onUserOrMessageDeletedError()
                }
                ErrTEmail.number -> {
                    // onEmailVerificationError()
                }
                ErrTPhone.number -> {
                    // onPhoneVerificationError()
                }
                ErrTNotFriend.number -> {
                    //onNotFriendError()
                }
                UNRECOGNIZED.number -> {
                //onUnrecognizedError()
                }
                else->{

                }
            }
        }

        // 应答了上传的文件的结果
        private fun onUploadReply(msg: MsgOuterClass.Msg){
            val plain = msg.plainMsg
            val ret = msg.plainMsg.uploadReply
            if (ret == null){
                return
            }

            val uploadResult = ret.result
            val fileName = ret.fileName
            val uuidName = ret.uuidName
            val detail = ret.detail
            val index = ret.chunkIndex

            val resultMap = mapOf(
                "uploadResult" to uploadResult.toString(),
                "fileName" to fileName,
                "uuidName" to uuidName,
                "detail" to detail
            )

            // result:"chunkok" detail:"wait next truck"
            // result:"fileok" detail:"finish"}

            if (uploadResult == "fileok" || uploadResult == "sameok"){

                // 通知消息管理器，文件传完了，需要发送消息了
                ChatSessionManager.onUploadFileFinish(ret.sendId, uploadResult.toString(), detail, fileName, uuidName)

                val msgType = ChatMsgType.IMAGE_VALUE
                SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.MSG_UPLOAD_OK, msgType, ret.sendId, 0L, resultMap)
            }
            else if (uploadResult == "chunkok")
            {
                // 这个函数中通知消息
                ChatSessionManager.onUploadFileProcess(ret.sendId, fileName, uuidName, index, resultMap)
                return
            }
            else{
                // 遇到错误
                ChatSessionManager.onUploadFileFinish(ret.sendId, uploadResult.toString(), detail, fileName, uuidName)
                SdkGlobalData.userCallBackManager.invokeOnErrorCallbacks(InterErrorType.UPLOAD_FAIL,fileName,uploadResult, detail)
            }
        }

        // 示例错误处理函数（你需要根据实际需求实现这些函数）
        private fun onVersionError() {
            // 处理版本不兼容错误
        }

        private fun onWrongPasswordError() {
            // 处理密码错误
        }

        // 先删除秘钥文件，然后重新发送hello
        fun onKeyPrintError(){
            deleteKeyFiles()
            createHelloMsg()
        }

        // 关于hello消息应答的效果
        // 如果登录成功，不在这里返回，而是收到一个用户登陆成功的消息
        fun onHello(hello: MsgHello) {
            when (hello.stage) {
                "waitlogin" -> {  //  目前的服务端实现是要求必须执行密钥交换
                    createKeyExchange1Msg()
                }
                "needlogin"-> {  //  秘钥交换完成了，但是需要注册或者登录
                    // 这里界面不需要做啥
                    Session.updateState(Session.SessionState.WAIT)
                }
                else->{

                }
            }
        }

        // 登录成功后返回的数据，解析自己的个人信息
        fun UserInfo2DbUser(ui: User.UserInfo) : com.bird2fish.birdtalksdk.model.User{

            SdkGlobalData.selfUserinfo.id = ui.userId
            SdkGlobalData.selfUserinfo.name = ui.userName
            SdkGlobalData.selfUserinfo.nick = ui.nickName
            SdkGlobalData.selfUserinfo.icon = ui.icon
            SdkGlobalData.selfUserinfo.age = ui.age

            SdkGlobalData.selfUserinfo.email = ui.email
            SdkGlobalData.selfUserinfo.phone = ui.phone
            SdkGlobalData.selfUserinfo.gender = ui.gender
            SdkGlobalData.selfUserinfo.introduction = ui.intro
            return SdkGlobalData.selfUserinfo
        }

        // 用户操作的服务器返回
        fun onUserRet(msg: MsgOuterClass.Msg){
            val opcode = msg.plainMsg.userOpRet.operation
            val result = msg.plainMsg.userOpRet.result
            var status = msg.plainMsg.userOpRet.getParamsOrDefault("status", "")



            val user = msg.plainMsg.userOpRet.getUsers(0)
            when (opcode){
                Login -> {
                    if (result == "ok"){
                        if (status == "waitcode"){
                            Session.loginNotifyCode()
                        }else if (status == "loginok"){
                            // 解析自己的个人信息
                            UserInfo2DbUser(user)
                            // 设置状态，并跳转
                            Session.loginOk(user.userId)
                        }else{
                            //
                            Session.loginFail(user.userId.toString(), status)
                        }

                    }else {
                        Session.loginFail("", "")
                    }
                }

                UserNoneAction -> doNothing()
                RegisterUser -> {
                    if (result == "ok" || status == "waitcode"){
                        Session.loginNotifyCode()
                    }
                }
                UnregisterUser -> {

                }
                DisableUser -> {

                }
                RecoverUser -> {

                }
                SetUserInfo -> {
                    // 设置个人信息完毕
                    if (result == "ok"){
                        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.USR_UPDATEINFO_OK,
                            0, 0, 0L, mapOf("status" to status ) )
                    }else{
                        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.USR_UPDATEINFO_FAIL,
                            0, 0, 0L, mapOf("status" to status ) )
                    }

                }
                RealNameVerification -> {

                }
                Logout -> {

                }
                FindUser -> {

                }
                AddFriend -> {

                }
                ApproveFriend -> {

                }
                RemoveFriend -> {

                }
                BlockFriend -> {

                }
                UnBlockFriend -> {

                }
                SetFriendPermission -> {

                }
                SetFriendMemo -> {

                }
                ListFriends -> {

                }
                User.UserOperationType.UNRECOGNIZED -> {

                }
            }

        }

        // 好友操作的返回结果
        private fun onFriendOpRet(msg: MsgOuterClass.Msg){
            val opcode = msg.plainMsg.friendOpRet.operation
            val result = msg.plainMsg.friendOpRet.result
            var status = ""
            status = msg.plainMsg.friendOpRet.getParamsOrDefault("status", "")

//            if (msg.plainMsg.friendOpRet.usersList.size <1){
//                return
//            }

            when (opcode){
                FindUser -> {
                    onFindFriendRet(msg.plainMsg.friendOpRet, result, status)
                }
                AddFriend -> {
                    onFriendAddRet(msg.plainMsg.friendOpRet, result, status)
                }
                ApproveFriend -> {

                }
                RemoveFriend -> {
                    onFriendRemoveRet(msg.plainMsg.friendOpRet, result, status)
                }
                BlockFriend -> {

                }
                UnBlockFriend -> {

                }
                SetFriendPermission -> {

                }
                SetFriendMemo -> {

                }
                ListFriends -> {
                    val mode = msg.plainMsg.friendOpRet.paramsMap["mode"]
                    if (mode == "fans"){
                        onFriendListFans(msg.plainMsg.friendOpRet, result, status)
                    }else if (mode == "follows"){
                        onFriendListFollows(msg.plainMsg.friendOpRet, result, status)
                    }else{
                        onFriendListMutual(msg.plainMsg.friendOpRet, result, status)
                    }
                }

                UserNoneAction -> doNothing()
                RegisterUser -> doNothing()
                UnregisterUser -> doNothing()
                DisableUser -> doNothing()
                RecoverUser -> doNothing()
                SetUserInfo -> doNothing()
                RealNameVerification -> doNothing()
                Login -> doNothing()
                Logout -> doNothing()
                User.UserOperationType.UNRECOGNIZED -> doNothing()
            }

        }

        // 好友列表返回
        fun onFriendListFans(retMsg: User.FriendOpResult, result:String, status:String){

            if (retMsg.usersList != null && retMsg.usersList.size > 0){
                //val lst = LinkedList<com.bird2fish.birdtalksdk.model.User>()
                // TODO: 以后再修复这里
                SdkGlobalData.clearFans()
                for (f in retMsg.usersList){
                    SdkGlobalData.updateAddNewFan(f)
                }

                // 还有需要加载的
                if (retMsg.usersList.size >= 100){
                    val f = retMsg.usersList.last()
                    val fromId = f.userId
                    sendListFriend("fans", fromId)
                }
            }
            SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_REQ_REPLY,
                0, 0, 0, mapOf("result"  to result, "status" to status ) )
        }

        // 关注列表返回
        fun onFriendListFollows(retMsg: User.FriendOpResult, result:String, status:String){
            if (retMsg.usersList != null && retMsg.usersList.size > 0){

                for (f in retMsg.usersList){
                    SdkGlobalData.updateAddNewFollow(f)
                }


                // 还有需要加载的
                if (retMsg.usersList.size >= 100){
                    val f = retMsg.usersList.last()
                    val fromId = f.userId
                    sendListFriend("follows", fromId)
                }
            }

            SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_REQ_REPLY,
                0, 0, 0, mapOf("result"  to result, "status" to status ) )
        }

        // 从服务器返回好友的列表
        fun onFriendListMutual(retMsg: User.FriendOpResult, result:String, status:String){

            if (retMsg.usersList != null && retMsg.usersList.size > 0){

                SdkGlobalData.updateAddMutual(retMsg.usersList)

                // 还有需要加载的
                if (retMsg.usersList.size >= 100){
                    val f = retMsg.usersList.last()
                    val fromId = f.userId
                    sendListFriend("friends", fromId)
                }
            }

            SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_REQ_REPLY,
                0, 0, 0, mapOf("result"  to result, "status" to status ) )
        }

        // 添加好友返回结果，结果中user是申请者，列表中的第一个元素是被申请的对象
        fun onFriendAddRet(retMsg: User.FriendOpResult, result:String, status:String){
            var fid = 0L

            val uinfo = retMsg.user
            if (uinfo == null){
                Log.e("onFriendAddRet", "user info in friend reply is null")
                return
            }
            // 如果是自己关注别人的返回，包括多终端登录的返回
            if (uinfo.userId == SdkGlobalData.selfUserinfo.id){
                if (retMsg.usersList != null && retMsg.usersList.size > 0){
                    fid = retMsg.usersList[0].userId
                    if (fid > 0){
                        // 更新好友列表
                        if (result == "ok" || result == "notice"){
                            SdkGlobalData.updateAddNewFollow(retMsg.usersList[0])
                        }else{
                            // 添加好友失败了
                        }

                    }else{
                        Log.e("onFriendAddRet", "friend id  is 0")
                        return
                    }
                }else{
                    Log.e("onFriendAddRet", "user list  is null")
                    return
                }

                SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_REQ_REPLY,
                    0, 0, fid, mapOf("result"  to result, "status" to status ) )
            }
            // 这里是别人关注了自己，这里是一个通知
            else{
                fid = uinfo.userId
                if (retMsg.usersList != null && retMsg.usersList.size > 0){
                    val uid = retMsg.usersList[0].userId
                    if (uid == SdkGlobalData.selfUserinfo.id){
                        // 更新好友列表
                        if (result == "ok" || result == "notice"){
                            SdkGlobalData.updateAddNewFan(uinfo)
                        }else{
                            // 别的应该不会有
                        }

                    }else{
                        Log.e("onFriendAddRet", "friend id  is 0")
                        return
                    }
                }else{
                    Log.e("onFriendAddRet", "user list  is null")
                    return
                }

                SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_ADD_NOTICE,
                    0, 0, fid, mapOf("result"  to result, "status" to status ) )
            }// end notice

        }

        // 删除好友的结果；如果是自己的其他终端删除了关注，这里接收到ok，当对方删除好友，这里也知道notice
        fun onFriendRemoveRet(retMsg: User.FriendOpResult, result:String, status:String){
            var fid = 0L

            val uinfo = retMsg.user
            if (uinfo == null){
                Log.e("onFriendAddRet", "user info in friend reply is null")
                return
            }

            // 不是自己发过来的，别人删除了关注自己
            if (retMsg.user.userId != SdkGlobalData.selfUserinfo.id){

                if (result == "notice")
                {
                    if (retMsg.usersList != null && retMsg.usersList.size > 0) {
                        val uid = retMsg.usersList[0].userId
                        if (uid == SdkGlobalData.selfUserinfo.id){
                            SdkGlobalData.updateDeleteFan(retMsg.user.userId)
                        }
                    }else{
                        Log.e("onFriendAddRet", "user info in friend reply is null")
                        return
                    }

//                    SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_REMOVE_NOTICE,
//                        0, 0, fid, mapOf("result"  to result, "status" to status ) )
                }else{
                    // 不应该有其他的类型
                }
                return

            }else{
                // 自己获取其他的终端删除了关注
                if (result == "ok" || result == "notice") {
                    if (retMsg.usersList != null && retMsg.usersList.size > 0) {
                        val fid = retMsg.usersList[0].userId
                        if (fid > 0L){
                            val friend = UserHelper.pbUserInfo2LocalUser(retMsg.usersList[0])
                            SdkGlobalData.updateDeleteFollow(friend)
                        }
                    }else{
                        Log.e("onFriendAddRet", "user list in friend reply is null")
                        return
                    }

                }else{
                    // 不应该有其他的类型
                }
            }// end of remove follow

            // end of onFriendRemoveRet
        }

        // 好友搜索结果
        private fun onFindFriendRet(retMsg: User.FriendOpResult, result:String, status:String){
            // 设置个人信息完毕
            val lst = LinkedList<com.bird2fish.birdtalksdk.model.User>()
//            if (retMsg.usersList.size <1){
//                return
//            }

            if (retMsg.usersList != null){
                // 遍历返回的列表
                for (f in retMsg.usersList){
                    val friend = UserHelper.pbUserInfo2LocalUser(f)
                    lst.add(friend)
                }
            }

            SdkGlobalData.setSearchFriendRet(lst, result, status)
        }

        /*
        message MsgError{
          int32  code = 1;
          string detail = 2;
          int64  sendId = 3;
          int64  msgId = 4;
          ErrorMsgType msgType = 5;
          map<string, string> params = 9;
        }
         */
        // 生成错误信息
        private fun createErrMsg(code:ErrorMsgType, detail:String) :Msg{
            val errMsg = MsgError.newBuilder()
                .setMsgType(code)
                .setDetail(detail)
                .build()

            val plainMsg = MsgPlain.newBuilder().setErrorMsg(errMsg)

            val timestamp = System.currentTimeMillis()
            return wrapMsg(plainMsg, timestamp, MsgTError)
        }


        // 发给服务器消息
        private fun sendBackErr(code:ErrorMsgType, detail:String){
            val retMsg = createErrMsg(code, detail)
            SendQueue.instance.enqueue(retMsg)
        }

        // 发送普通的消息
        private  fun sendMsg(msg: Msg){
            SendQueue.instance.enqueue(msg)
        }



        // hello 或者直接秘钥快速登录
        private  fun createHelloData(clientId: String, version: String, platform: String, aesFingerPrint: String, tm:Long): MsgHello {

            var keyPrint = loadKeyPrint()
            var keyShare = loadShareKey()

            val builder = MsgHello.newBuilder()
                .setClientId(clientId)  // 客户端唯一标识
                .setVersion(version)    // 客户端版本
                .setPlatform(platform)  // 平台类型 (如 ANDROID, IOS 等）
                .setStage("clienthello")

            // 检查是否有共享密钥
            if (!keyPrint.equals(0) && keyShare != null){
                // 初始化加解密工具
                this.keyExchange.setLocalShare(keyPrint, keyShare)

                // 这里与秘钥交换不一样，这里是STRING，所以需要BASE64编码
                builder.setKeyPrint(keyPrint)
                val tmStr = tm.toString()
                val checkData = this.keyExchange.encryptAESCTR_Str2Base64(tmStr)
                builder.putParams("checkTokenData", checkData)

            }else{
                this.keyExchange.clear()
                keyPrint = 0
                keyShare = null
            }


            return builder.build()
        }

        // 阶段1的握手数据
        private fun  createKeyExchange1Msg() : MsgOuterClass.Msg {
            this.keyExchange.generateKeyPair()
            val pubKey64 = this.keyExchange.exportPublicKeyToPem()
            val pubKeyG = ByteString.copyFrom(pubKey64)

            val exMsg = MsgKeyExchange.newBuilder()
                .setPubKey(pubKeyG)
                .setStage(1)            // PEM格式编码的字节流UTF-8
                .setEncType("AES-CTR")  // 加密算法
                .setKeyPrint(0)
                .setRsaPrint(0).build()

            val plainMsg = MsgPlain.newBuilder().setKeyEx(exMsg)

            val timestamp = System.currentTimeMillis()
            val msg = wrapMsg(plainMsg, timestamp, MsgTKeyExchange)
            sendMsg(msg)
            Session.updateState(Session.SessionState.KEY_EXCHANGE_1)
            return msg
        }

        // 阶段3的握手数据
        private fun  createKeyExchange3Msg() : MsgOuterClass.Msg {
            this.keyExchange.generateKeyPair()

            val timestamp = System.currentTimeMillis()
            val tmStr = timestamp.toString()
            val keyPrint = this.keyExchange.getKeyPrint()
            val tempKey = this.keyExchange.encryptAESCTR(tmStr.toByteArray(Charsets.UTF_8))


            val exMsg = MsgKeyExchange.newBuilder()
                .setStage(3)            // PEM格式编码的字节流UTF-8
                .setEncType("AES-CTR")  // 加密算法
                .setKeyPrint(keyPrint)
                .setTempKey( ByteString.copyFrom(tempKey) )
                .setStatus("ready")
                .setRsaPrint(0).build()

            val plainMsg = MsgPlain.newBuilder().setKeyEx(exMsg)

            val msg = wrapMsg(plainMsg, timestamp, MsgTKeyExchange)
            sendMsg(msg)

            Session.updateState(Session.SessionState.KEY_EXCHANGE_3)
            return msg
        }

        // 收到服务端的秘钥交换消息
        private fun onExchangeMsg(msg: MsgOuterClass.Msg){
            val exMsg = msg.plainMsg.keyEx
            if (exMsg.stage == 2){
                onExchangeMsg2(exMsg, msg.tm)
            }else if (exMsg.stage == 4){
                onExchangeMsg4(exMsg, msg.tm)
            }else{
                // 消息错误
              sendBackErr(ErrTWrongCode, "exchagne stage is error")
            }
        }

        // 收到服务端的公钥，这里做秘钥交换
        private fun onExchangeMsg2(exMsg: MsgKeyExchange, tm :Long){
            val remoteKeyPrint = exMsg.keyPrint
            val remotePubKeyData = exMsg.pubKey
            val remotePubKey = this.keyExchange.importPublicKeyFromPem(remotePubKeyData.toByteArray())
            val tempKey = exMsg.tempKey   // 这里是验证双方加密算法是否一致的测试数据，

            // 开始交换
            val ret = this.keyExchange.exchangeKeys(remotePubKey)
            if (!ret){
                // 秘钥交换错误
                sendBackErr(ErrTMsgContent, "exchange data error")
                return
            }

            // 开始验证秘钥
            val keyPrint = this.keyExchange.calculateKeyPrint()
            if (keyPrint != remoteKeyPrint){
                // 秘钥的指纹错误
                sendBackErr(ErrTKeyPrint, "exchange key error")
                return
            }
            // 秘钥签名一致了，使用小端编码，测试一下解密
            val tmStr = tm.toString()
            //val localTmStr = this.keyExchange.encryptAESCTR(tmStr.toByteArray())

            val tempKeyData = tempKey.toByteArray()
            val plain = this.keyExchange.decryptAESCTR(tempKeyData)
            val calTmStr  = String(plain, Charsets.UTF_8)
            if (tmStr != calTmStr){
                // 测试密文解密遇到错误
                sendBackErr(ErrTKeyPrint, "exchange key, decrypt temp data error")
                return
            }

            // 生成阶段三，然后发送
            val msg = createKeyExchange3Msg()

            SendQueue.instance.enqueue(msg)

        }

        // 保存秘钥，同时设置状态
        private fun onExchangeMsg4(exMsg: MsgKeyExchange, tm :Long){

            if (exMsg.status == "waitdata"){   // 直接登录了
                // 保存key
                this.saveKeyPrint(this.keyExchange.getKeyPrint())
                this.saveShareKey(this.keyExchange.getSharedKey()!!)
                // 这里可以跳转了
                Session.updateState(Session.SessionState.WAIT)

            }else if (exMsg.status == "needlogin"){  // 注册或者登录
                // 保存key
                this.saveKeyPrint(this.keyExchange.getKeyPrint())
                this.saveShareKey(this.keyExchange.getSharedKey()!!)
                Session.updateState(Session.SessionState.WAIT)

            }else{
                sendBackErr(ErrTMsgContent, "exchange4 status is error")
                return
            }

        }



        fun createHelloMsg() {

            // todo:这里可以使用一个UUID，执行本地存储，每次都带着，用于服务端区分设备
            val clientId = SdkGlobalData.basicInfo.deviceId
            val version = SdkGlobalData.basicInfo.sdkVersion
            val platform = SdkGlobalData.basicInfo.platform
            val aesFingerPrint = ""

            val timestamp = System.currentTimeMillis()
            // 创建 MsgHello 消息
            val helloMessage = createHelloData(clientId!!, version, platform!!, aesFingerPrint, timestamp)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setHello(helloMessage)

            val msg = wrapMsg(plainMsg, timestamp, MsgTHello)
            sendMsg(msg)
        }

        // 心跳的消息
        fun sendHeartBeat(){
            val timestamp = System.currentTimeMillis()

            val heart = MsgHeartBeat.newBuilder().setTm(timestamp).setUserId(0)

            val plainMsg = MsgPlain.newBuilder().setHeartBeat(heart)
            val msg = wrapMsg(plainMsg, timestamp, MsgTHeartBeat)
            sendMsg(msg)
        }


        // 发出注册申请
        fun sendRegister(name:String, pwd:String, email:String, mode :String){

            val timestamp = System.currentTimeMillis()

            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(0)
            userInfo.setUserName(name)
            userInfo.setEmail(email)
            userInfo.putParams("pwd", pwd)


            val regOpReq = UserOpReq.newBuilder()
                .setOperation(RegisterUser)
                .setUser(userInfo)
                .putParams("regmode", mode)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setUserOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTUserOp)
            sendMsg(msg)
        }

        // 发出
        fun sendLogin(mode :String, id:String, pwd:String){

            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            if (mode == "phone"){
                userInfo.setPhone(id)
            }else if (mode == "email"){
                userInfo.setEmail(id);
            }else{
                userInfo.setUserId(id.toLong())
            }

            val paramsMap = userInfo.putParams("pwd", pwd)

            val regOpReq = UserOpReq.newBuilder()
                .setOperation(Login)
                .setUser(userInfo)
                .putParams("loginmode", mode)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setUserOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTUserOp)
            sendMsg(msg)
        }

        // 发送验证码，注册，或者登录
        fun sendCodeMessage(mode:String, id:String, code:String){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()

            if (mode == "phone"){
                userInfo.setPhone(id)
            }else if (mode == "email"){
                userInfo.setEmail(id);
            }else {
                userInfo.setUserId(id.toLong())
            }


            val regOpReq = UserOpReq.newBuilder()
                .setOperation(RealNameVerification)
                .setUser(userInfo)
                .putParams("regmode", mode)
                .putParams("code", code)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setUserOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTUserOp)
            sendMsg(msg)
        }

        /*
        const paramsMap =  regOpReq.getParamsMap();
        paramsMap.set("UserName", "Robin.fox");
        paramsMap.set("NickName", "飞鸟真人");
        paramsMap.set("Age", "35");
        paramsMap.set("Intro", "我是一个爱运动的博主>_<...");
        paramsMap.set("Gender", "男");
        paramsMap.set("Region", "北京");
        paramsMap.set("Icon", "飞鸟真人");
        paramsMap.set("Params.title", "经理")
         */
        // 设置用户的信息
        fun setUserInfo(id:Long, data:Map<String, String>){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(id)

            val regOpReq = UserOpReq.newBuilder()
                .setOperation(SetUserInfo)
                .setUser(userInfo)

            regOpReq.putAllParams(data)

            val plainMsg = MsgPlain.newBuilder().setUserOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTUserOp)
            sendMsg(msg)
        }

        // 设置用户的手机和邮箱的时候，实名验证的验证码
        fun sendUserInfoCodeMessage(id:Long, code:String){

            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(id)

            val regOpReq = UserOpReq.newBuilder()
                .setOperation(RealNameVerification)
                .setUser(userInfo)
                .putParams("code", code)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setUserOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTUserOp)
            sendMsg(msg)
        }

        //禁用和解禁用户，普通客户端不需要实现

        // 注销用户
        fun sendUserUnregMessage(id:Long){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(id)

            val regOpReq = UserOpReq.newBuilder()
                .setOperation(UnregisterUser)
                .setUser(userInfo)


            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setUserOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTUserOp)
            sendMsg(msg)
        }

        // 9 退出登录
        fun sendUserLogoutMessage(id:Long){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(id)

            val regOpReq = UserOpReq.newBuilder()
                .setOperation(Logout)
                .setUser(userInfo)


            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setUserOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTUserOp)
            sendMsg(msg)
        }

        // 10 查询好友，通过params设置查询的信息
        fun sendFriendFindMessage(mode:String, key:String){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()

            val regOpReq = FriendOpReq.newBuilder()
                .setOperation(FindUser)
                .setUser(userInfo)
                .putParams("mode", mode)
                .putParams("value", key)


            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setFriendOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTFriendOp)
            sendMsg(msg)
        }

        // 11 请求添加好友, 当前的模式是双向关注即是好友
        fun sendFriendAddMessage(fid:Long){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(fid)

            val regOpReq = FriendOpReq.newBuilder()
                .setOperation(AddFriend)
                .setUser(userInfo)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setFriendOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTFriendOp)
            sendMsg(msg)
        }

        // 同意或者拒绝好友申请，暂时不需要

        //13 删除关注
        fun sendRemoveFollow(fid:Long){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(fid)

            val regOpReq = FriendOpReq.newBuilder()
                .setOperation(RemoveFriend)
                .setUser(userInfo)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setFriendOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTFriendOp)
            sendMsg(msg)
        }

        // 拉黑
        fun sendBlockFollow(fid:Long){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(fid)

            val regOpReq = FriendOpReq.newBuilder()
                .setOperation(BlockFriend)
                .setUser(userInfo)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setFriendOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTFriendOp)
            sendMsg(msg)
        }

        // 解除拉黑
        fun sendUnBlockFollow(fid:Long){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(fid)

            val regOpReq = FriendOpReq.newBuilder()
                .setOperation(UnBlockFriend)
                .setUser(userInfo)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setFriendOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTFriendOp)
            sendMsg(msg)
        }

        // 设置给对方的权限
        fun sendSetPermission(fid:Long, mask:String){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(fid)

            val regOpReq = FriendOpReq.newBuilder()
                .setOperation(SetFriendPermission)
                .setUser(userInfo)
                .putParams("permission", mask)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setFriendOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTFriendOp)
            sendMsg(msg)
        }

        // 设置备注，这里包括了关注的备注和粉丝的备注两种
        // "fans"
        fun sendSetFriendMemo(fid:Long, mode:String, nick:String){
            val timestamp = System.currentTimeMillis()
            val userInfo = UserInfo.newBuilder()
            userInfo.setUserId(fid)
            userInfo.setNickName(nick)

            val regOpReq = FriendOpReq.newBuilder()
                .setOperation(SetFriendMemo)
                .setUser(userInfo)
                .putParams("mode", mode)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setFriendOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTFriendOp)
            sendMsg(msg)
        }

        // 设置列出好友
        fun sendListFriend(mode:String, fromId: Long){
            val timestamp = System.currentTimeMillis()

            // TODO: 用户量大了以后，得支持多页查找，目前一次能返回1000个
            // 从这个用户开始查找
            val uinfo = UserInfo.newBuilder()
            uinfo.userId = fromId


            val regOpReq = FriendOpReq.newBuilder()
                .setOperation(ListFriends)
                .setUser(uinfo)
                .putParams("mode", mode)

            // 如果 sharedKeyPrint 存在，则执行相应的操作
            val plainMsg = MsgPlain.newBuilder().setFriendOp(regOpReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTFriendOp)
            sendMsg(msg)
        }

        //////////////////////////////////////////////////////
        // 群操作
        // 创建群  "public"  "private"
        fun sendCrateGroupMessage(name:String, visibility:String){
            val timestamp = System.currentTimeMillis()

            val group = GroupInfo.newBuilder()
                .setGroupName(name)
                .putParams("visibility", visibility)

            val opReq = User.GroupOpReq.newBuilder()
            opReq.setOperation(User.GroupOperationType.GroupCreate);
            opReq.setGroup(group)

            val plainMsg = MsgPlain.newBuilder().setGroupOp(opReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTGroupOp)
            sendMsg(msg)

        }

        // 查找群
        fun sendFindGroupMessage(keyword:String){
            val timestamp = System.currentTimeMillis()




            val opReq = User.GroupOpReq.newBuilder()
            opReq.setOperation(User.GroupOperationType.GroupSearch)
            opReq.putParams("keyword", keyword)

            val plainMsg = MsgPlain.newBuilder().setGroupOp(opReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTGroupOp)
            sendMsg(msg)
        }

        // 设置群信息
        fun sendSetgroupMemo(id:Long, name:String, tags:Array<String>, brief:String, icon:String){
            val timestamp = System.currentTimeMillis()

            val group = GroupInfo.newBuilder()
                .setGroupId(id)
                .setGroupName(name)
                .clearTags()

            for (i in 0 until tags.size) {
                val element = tags[i]
                group.addTags(element)
            }

            group.putParams("icon", icon)
            group.putParams("brief", brief)
            group.putParams("jointype", "any")

            val opReq = User.GroupOpReq.newBuilder()
            opReq.setOperation(User.GroupOperationType.GroupSetInfo);
            opReq.setGroup(group)

            val plainMsg = MsgPlain.newBuilder().setGroupOp(opReq)
            val msg = wrapMsg(plainMsg, timestamp, MsgTGroupOp)
            sendMsg(msg)
        }


        // 上传文件
        fun sendFileChunk(name:String, sz:Long, index:Int, chunkSz:Int, chunkCount:Int, hash:String, data:ByteArray, fileType:String?,
                          gId:Long, msgId:Long){
            val timestamp = System.currentTimeMillis()
            val upload = MsgUploadReq.newBuilder()

            upload.setSendId(msgId)
            upload.setGroupId(gId)
            upload.setFileName(name)
            upload.setChunkIndex(index)
            upload.setChunkSize(chunkSz)
            upload.setFileSize(sz)
            upload.setChunkCount(chunkCount)

            upload.setHashType("md5")
            upload.setHashCode(hash)
            upload.setFileData(ByteString.copyFrom(data))
            upload.setFileType(fileType ?:"file")


            val plainMsg = MsgPlain.newBuilder().setUploadReq(upload)
            val msg = wrapMsg(plainMsg, timestamp, MsgTUpload)
            sendMsg(msg)
        }

        // 发送消息
        fun sendChatMsg(msgId:Long, fid: Long, chatType:ChatType, dataType:ChatMsgType, txt:String, refMsgId:Long){
            val timestamp = System.currentTimeMillis()
            val chatMsg = MsgChat.newBuilder()


            // 注意，这里的msgId 在服务器应答时候会变为雪花算法指定的消息，sendId还是自己指定的编号
            chatMsg.msgId = msgId   // 这个会变
            chatMsg.sendId = msgId  // 这个回来时候不变

            chatMsg.chatType = chatType
            chatMsg.msgType = dataType
            chatMsg.data =  com.google.protobuf.ByteString.copyFrom(txt.toByteArray(Charsets.UTF_8))

            chatMsg.fromId = SdkGlobalData.selfUserinfo.id
            chatMsg.userId = SdkGlobalData.selfUserinfo.id
            chatMsg.toId = fid

            chatMsg.tm = timestamp
            chatMsg.devId = SdkGlobalData.basicInfo.deviceId
            chatMsg.refMessageId = refMsgId


            val plainMsg = MsgPlain.newBuilder().setChatData(chatMsg)
            val msg = wrapMsg(plainMsg, timestamp, MsgTChatMsg)
            sendMsg(msg)
        }

        // 发送回执
        fun sendChatReply(fid:Long, sendId:Long, refMsgId:Long, bRead:Boolean, batch:String){
            val timestamp = System.currentTimeMillis()
            var reply = MsgChatReply.newBuilder()

            reply.extraMsg = "ok"
            reply.msgId = refMsgId
            reply.sendId = sendId
            reply.userId = fid                              // 发给谁
            reply.fromId = SdkGlobalData.selfUserinfo.id    // 从哪来， 这个用户发的的
            reply.recvOk = timestamp

            if (bRead){
                reply.readOk = timestamp
            }else{
                reply.readOk = 0L
            }
            reply.putParams("gid", "0")

            // 支持批量的回执
            if (!TextUtils.isEmpty(batch)){
                reply.putParams("batch", batch)
            }

            val plainMsg = MsgPlain.newBuilder().setChatReply(reply)
            val msg = wrapMsg(plainMsg, timestamp, MsgTChatReply)
            sendMsg(msg)
        }

        // 发送申请，同步那个私聊的数据
        fun sendSynPChatDataForward(msgId:Long ){
            val timestamp = System.currentTimeMillis()
            val msgQ = MsgQuery.newBuilder()

            msgQ.userId = SdkGlobalData.selfUserinfo.id
            msgQ.groupId = 0
            msgQ.littleId = msgId
            msgQ.bigId = Long.MAX_VALUE
            msgQ.synType = SynType.SynTypeForward

            msgQ.queryType = QueryDataTypeChatData
            msgQ.chatType = ChatType.ChatTypeP2P

            val plainMsg = MsgPlain.newBuilder().setCommonQuery(msgQ)
            val msg = wrapMsg(plainMsg, timestamp, MsgTQuery)
            sendMsg(msg)
        }

        fun sendSynPChatDataBackward(msgId:Long ){
            val timestamp = System.currentTimeMillis()
            val msgQ = MsgQuery.newBuilder()

            msgQ.userId = SdkGlobalData.selfUserinfo.id
            msgQ.groupId = 0
            msgQ.littleId = msgId
            msgQ.bigId = Long.MAX_VALUE
            msgQ.synType = SynType.SynTypeBackward

            msgQ.queryType = QueryDataTypeChatData
            msgQ.chatType = ChatType.ChatTypeP2P

            val plainMsg = MsgPlain.newBuilder().setCommonQuery(msgQ)
            val msg = wrapMsg(plainMsg, timestamp, MsgTQuery)
            sendMsg(msg)
        }


    }
}