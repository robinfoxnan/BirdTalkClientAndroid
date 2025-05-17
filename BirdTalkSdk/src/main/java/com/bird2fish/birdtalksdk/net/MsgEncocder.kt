package com.bird2fish.birdtalksdk.net
import android.content.Context
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.pbmodel.*
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.*
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ComMsgType.*
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ErrorMsgType.*
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.ErrorMsgType.UNRECOGNIZED
import com.bird2fish.birdtalksdk.pbmodel.User.FriendOpReq
import com.bird2fish.birdtalksdk.pbmodel.User.GroupInfo
import com.bird2fish.birdtalksdk.pbmodel.User.UserInfo
import com.bird2fish.birdtalksdk.pbmodel.User.UserOpReq
import com.bird2fish.birdtalksdk.pbmodel.User.UserOperationType.*
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import com.google.protobuf.ByteString

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
                MsgTChatMsg -> doNothing()
                MsgTChatReply -> doNothing()

                MsgTQueryResult -> doNothing()

                MsgTUploadReply -> doNothing()
                MsgTDownloadReply -> doNothing()

                MsgTUserOpRet -> onUserRet(msg)
                MsgTFriendOp -> doNothing()
                MsgTFriendOpRet -> doNothing()
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
        fun onHello(hello: MsgHello) {
            when (hello.stage) {
                "waitlogin" -> {  //  目前的服务端实现是要求必须执行密钥交换
                    createKeyExchange1Msg()
                }
                "needlogin"-> {  //  秘钥交换完成了，但是需要注册或者登录
                    // 这里界面不需要做啥
                    Session.updateState(Session.SessionState.WAIT)
                }
                "waitdata"->{    // 秘钥登录完毕，等待数据发送

                    Session.loginOk()

                }
                else->{

                }
            }
        }


        // 用户操作的服务器返回
        fun onUserRet(msg: MsgOuterClass.Msg){
            val opcode = msg.plainMsg.userOpRet.operation
            val result = msg.plainMsg.userOpRet.result
            var status = ""
            status = msg.plainMsg.userOpRet.getParamsOrDefault("status", "")

            if (msg.plainMsg.userOpRet.usersList.size <1){
                return
            }

            val user = msg.plainMsg.userOpRet.getUsers(0)
            when (opcode){
                Login -> {
                    if (result == "ok"){
                        Session.loginOk()
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
            SendQueue.getInstance().enqueue(retMsg)
        }

        // 发送普通的消息
        private  fun sendMsg(msg: Msg){
            SendQueue.getInstance().enqueue(msg)
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

            SendQueue.getInstance().enqueue(msg)

        }

        // 保存秘钥，同时设置状态
        private fun onExchangeMsg4(exMsg: MsgKeyExchange, tm :Long){

            if (exMsg.status == "waitdata"){   // 直接登录了
                // 保存key
                this.saveKeyPrint(this.keyExchange.getKeyPrint())
                this.saveShareKey(this.keyExchange.getSharedKey()!!)
                // 这里可以跳转了

            }else if (exMsg.status == "needlogin"){  // 注册或者登录
                // 保存key
                this.saveKeyPrint(this.keyExchange.getKeyPrint())
                this.saveShareKey(this.keyExchange.getSharedKey()!!)


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
                val paramsMap = userInfo.putParams("pwd", pwd)
            }

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
        fun sendListFriend(mode:String){
            val timestamp = System.currentTimeMillis()

            val regOpReq = FriendOpReq.newBuilder()
                .setOperation(ListFriends)
                //.setUser(userInfo)
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



    }
}