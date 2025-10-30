package com.bird2fish.birdtalksdk

import android.content.Context
import android.os.Build
import com.bird2fish.birdtalksdk.db.BaseDb
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.model.Topic
import com.bird2fish.birdtalksdk.net.CRC64
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.net.UnsafeOkHttpClient
import com.bird2fish.birdtalksdk.uihelper.UserHelper
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicLong
import android.provider.Settings;
import android.util.Log
import com.bird2fish.birdtalksdk.db.SeqDbHelper
import com.bird2fish.birdtalksdk.db.TopicDbHelper
import com.bird2fish.birdtalksdk.db.UserDbHelper
import com.bird2fish.birdtalksdk.model.ChatSessionManager
import com.bird2fish.birdtalksdk.model.MessageContent
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


data class PlatformInfo(
    val platform: String,      // 平台名称，如 "Mobile", "Tablet"
    val os: String,            // 操作系统名称，如 "Android"
    val osVersion: String,     // 操作系统版本，如 "13" 或 "API 33"
    val programmingLang: String, // 编程语言，如 "Kotlin"
    val langVersion: String,   // 编程语言版本，如 "1.8.20"
    val deviceModel: String? = null, // 设备型号（可选）
    val appVersion: String? = null,  // 应用版本（可选）
    var deviceId: String? = "",
    var osInfo :String? = "",
    var sdkVersion :String = "1.0"
)


/*
备注：
1) 会话列表中，用户的chatSessionId = fid, 群组的chatSessionId = -gid;
   这是为了管理起来简单化；
2) 数据库存储的topic.tid 都是正数；
*/

class SdkGlobalData {

    companion object{

        private const val TAG = "SdkGlobalData"
        // 创建实例
        val basicInfo = PlatformInfo(
            platform = "${Build.MANUFACTURER}",
            os = "${Build.BRAND}",
            osVersion = android.os.Build.VERSION.RELEASE,
            programmingLang = "Kotlin",
            langVersion = "1.8.20",
            deviceModel = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            appVersion = "0.1",
            deviceId = ""

        )

        var context :Context? = null
        // 个人信息
        val selfUserinfo :com.bird2fish.birdtalksdk.model.User = User()
        private val msgId  = AtomicLong(1)


        // 这里是一个回调的列表
        var userCallBackManager = CallbackManager()

        // 互相关注
        var mutualFollowingList : MutableMap<Long, User> = LinkedHashMap()
        // 关注 保持插入顺序
        var followingList : MutableMap<Long, User> = LinkedHashMap()

        // 粉丝
        var fanList : MutableMap<Long, User> = LinkedHashMap()

        // 系统推荐
        var recommendedList : LinkedList<User> = LinkedList<User>()

        // 搜索用户返回的结果
        private var searchFriendList : LinkedList<User> = LinkedList<User>()

        // 当前会话列表
        var chatTopicList :LinkedHashMap<Long, Topic> = LinkedHashMap()

        // 当前显示的消息界面，如果是负数，就是群组
        var currentChatFid = 0L

        fun  invokeOnEventCallbacks(eventType: MsgEventType, msgType: Int, msgId: Long, fid: Long, params:Map<String, String>){
            this.userCallBackManager.invokeOnEventCallbacks(eventType, msgType, msgId, fid, params)
        }

        // 切换前需要更改当前的消息索引；
        fun beforeSwitchToChat(fid:Long, isP2p:Boolean){
            if (isP2p){
                SdkGlobalData.currentChatFid = fid
            }else{
                SdkGlobalData.currentChatFid = -fid
            }

        }

        ///////////////////////////////////////////////////////////////////////////////////////////
        // chatSession 与这个图标应该对应起来
        fun getTopic(friend:User):Topic {
            val sessionId = friend.id
            synchronized(chatTopicList){
                if (chatTopicList.containsKey(sessionId))
                    return chatTopicList[sessionId]!!
            }

            val t = addNewTopic(friend)
            ChatSessionManager.getSession(friend.id)
            return t
        }

        // 当多终端登录时候，某些消息可能找不到好友信息，已经不是好友了，那么消息就过时了
        // 在chatSession 中调用
        fun getPTopic(fid:Long):Topic?{
            //内存里不一定有这个用户信息，还需要请求服务端数据然后更新
            val friend = UserDbHelper.getUserById(fid)
            if (friend != null){
                return getTopic(friend)
            }
            else {
                // 如果内存中没有这个用户，肯定不是好友，也是不是粉丝了，那么重新请求一下数据
                val f = User()
                f.id = fid
                f.nick = "正在查询.."
                f.name = "正在查询.."
                val t = getTopic(f)
                MsgEncocder.sendFriendFindMessage("", "id")
                return t
            }

            ChatSessionManager.getSession(fid)
        }




        // 每次收到消息，都要设置最后的消息
        // 这里如果是允许提醒，发出声音
        fun updateTopic(msg:MessageContent){
            if (msg.isP2p){
                val fid = msg.userId
                synchronized(chatTopicList){
                    if (!chatTopicList.containsKey(fid)){
                        val t = Topic(fid, 0, 0, MsgOuterClass.ChatType.ChatTypeP2P.number, 1, msg.nick, msg.iconUrl)
                        chatTopicList[fid] = t
                        TopicDbHelper.insertOrReplacePTopic(t)
                    }
                    chatTopicList[fid]!!.lastMsg = msg
                }
            }else{
                val gid = msg.gid
            }

            // 收到消息了，自然要更新会话界面；
            userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_CHAT_SESSION, 0, msg.msgId, msg.userId, mapOf("msg" to "p2p"))
        }


        fun addNewTopic(f: User):Topic{
            val t = Topic(f.id, 0, 0, MsgOuterClass.ChatType.ChatTypeP2P.number, 1, f.nick, f.icon)
            addNewTopic(t, true)
            return t
        }

        // 确保对话时存在的
        fun addNewTopic(t:Topic, isP2p:Boolean){
            synchronized(chatTopicList){
                if (isP2p){
                    if (!chatTopicList.containsKey(t.tid)) {
                        chatTopicList[t.tid] = t
                        TopicDbHelper.insertOrReplacePTopic(t)
                        userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_CHAT_SESSION, 0, 0, 0, mapOf("t" to "p2p"))
                    }
                }
                else{
                    if (!chatTopicList.containsKey(-t.tid)) {
                        chatTopicList[-t.tid] = t
                        TopicDbHelper.insertOrReplaceGTopic(t)
                        userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_CHAT_SESSION, 0, 0, 0, mapOf("t" to "group"))
                    }
                }

            }
        }

        // 查询所有用户的信息的时候，返回数据已经保存数据库了，这个时候还是应该更新TOPIC里面的数据
        private fun updateTopicAndChatSession(friend :User){
            synchronized(chatTopicList){
                if (chatTopicList.containsKey(friend.id)){
                    val topic = chatTopicList[friend.id]
                    topic!!.icon = friend.icon
                    topic!!.title = friend. nick
                }
            }

            ChatSessionManager.updateSessionInfo(friend)
        }

        /////////////////////////////////////////////////////////////////////////
        // 给那个对话的界面使用的
        fun getChatSessionMap() :java.util.LinkedHashMap<Long, Topic>{
            synchronized(chatTopicList){
                return chatTopicList
            }
        }

        // 是否对方与自己双向关注
        fun isMutualfollowing(id: Long):Boolean{
            return mutualFollowingList.containsKey(id)
        }

        // 是否意境关注对方
        fun isFollowing(id: Long):Boolean{
            return followingList.containsKey(id)
        }

        // 是否对方是自己的粉丝
        fun isFan(id: Long):Boolean{
            return fanList.containsKey(id)

        }

        // 设置当前搜索的结果
        fun setSearchFriendRet(ret:LinkedList<User>, result:String, status:String){
            // 使用同步代码块，锁定当前对象
            synchronized(this) {
                this.searchFriendList = ret
            }
            // 通知界面更新
            userCallBackManager.invokeOnEventCallbacks(MsgEventType.SEARCH_FRIEND_RET,
                0, 0, 0L, mapOf("result"  to result, "status" to status ) )

            // 关键：用 IO 子线程执行编码操作，主线程不阻塞
            GlobalScope.launch(Dispatchers.IO) {
                for (user in ret) {
                    UserDbHelper.insertOrUpdateUser(user)
                }
            }
        }

        fun getSearchFriendRet(): LinkedList<User> {
            // 使用同步代码块，锁定当前对象
            synchronized(this) {
                return this.searchFriendList
            }
        }



        // 更新关注好友返回的信息
        fun updateAddNewFollow(f:com.bird2fish.birdtalksdk.pbmodel.User.UserInfo){
            val friend = UserHelper.pbUserInfo2LocalUser(f)

            synchronized(followingList) {
                if (!followingList.containsKey(f.userId)) {
                    followingList[f.userId] = friend
                }

                UserDbHelper.insertOrUpdateUser(friend)
                UserDbHelper.insertFollow(friend.id, friend.nick)
                updateTopicAndChatSession(friend)
            }

            var b  = false
            synchronized(fanList){
                if (fanList.containsKey(f.userId)){
                    b = true
                }
            }
            if (b){
                synchronized(mutualFollowingList){
                    mutualFollowingList[f.userId] = friend
                }
            }

        }

        // 服务端返回的粉丝
        fun updateAddNewFan(f:com.bird2fish.birdtalksdk.pbmodel.User.UserInfo){
            val friend = UserHelper.pbUserInfo2LocalUser(f)

            synchronized(fanList) {
                if (!fanList.containsKey(f.userId)){
                    fanList.set(f.userId, friend)
                }

                UserDbHelper.insertOrUpdateUser(friend)
                UserDbHelper.insertFan(friend.id, friend.nick)
                updateTopicAndChatSession(friend)
            }

            var b  = false
            synchronized(followingList){
                if (followingList.containsKey(f.userId)){
                    b = true
                }
            }
            if (b){
                synchronized(mutualFollowingList){
                    mutualFollowingList[f.userId] = friend
                }
            }

        }

        // 找本地双向的好友信息
        fun getMutualFriendLocal(fid:Long):User?{
            synchronized(mutualFollowingList){
                return mutualFollowingList[fid]
            }
        }

        fun getMutualFollowList():List<User>{
            synchronized(mutualFollowingList){
                return mutualFollowingList.values.toList()
            }
        }
        fun getFollowList(): List<User>{
            synchronized(followingList) {
                return followingList.values.toList()
            }
        }

        fun getFanList(): List<User>{
            synchronized(fanList){
                return fanList.values.toList()
            }
        }



        // 添加事件的跟踪器
        fun addCallback(callback: StatusCallback) {
            userCallBackManager.addCallback(callback)
        }

        // 删除事件的跟踪器
        fun removeCallback(callback: StatusCallback) {
            userCallBackManager.removeCallback(callback)
        }

        // 初始化
        fun init(context:Context){
            this.context = context
            basicInfo.deviceId = generateUniqueDeviceId()
            basicInfo.osInfo = getOSInfo()

            val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()

            // 配置 Picasso 使用自定义 OkHttpClient
            val builder = Picasso.Builder(context)
            builder.downloader(OkHttp3Downloader(client))
            val picasso = builder.build()
            Picasso.setSingletonInstance(picasso)

        }

        fun debugClean(){
            TopicDbHelper.deleteFromPTopic(0)
            TopicDbHelper.deleteFromPTopic(10006)
            TopicDbHelper.deleteFromPTopic()
        }

        // 登录成功后操作
        // 尝试加载用户的好友和粉丝等各种预加载信息
        fun initLoad(uid:Long){
            BaseDb.changeToDB(this.context, uid.toString())
            //debugClean()

            if (BaseDb.getInstance() == null) {
                Log.e(TAG, "BaseDb.changeToDB error")
                TextHelper.showToast(context!!, "BaseDb.changeToDB error")
            }

            val ret = UserDbHelper.insertOrReplaceAccount(selfUserinfo)
            if (!ret) {
                Log.e(TAG, "UserDbHelper.insertOrReplaceAccount FAIL")
            }

            // 重连时候不加载数据库
            synchronized(chatTopicList){
                if (chatTopicList.isEmpty()){
                    val p2pTopics = TopicDbHelper.getAllPTopics()
                    val gTopics = TopicDbHelper.getAllGTopics()
                    for (t in p2pTopics){
                        chatTopicList[t.tid] = t
                        if (SdkGlobalData.currentChatFid == 0L){
                            SdkGlobalData.currentChatFid = t.tid
                        }
                    }
                    for (t in gTopics){
                        chatTopicList[-t.tid] = t
                        if (SdkGlobalData.currentChatFid == 0L){
                            SdkGlobalData.currentChatFid = -t.tid
                        }
                    }
                }

            }

            // 重连时候不加载数据库
            synchronized(followingList) {
                if (followingList.isEmpty())
                {
                    val follows = UserDbHelper.queryFollowsFromView(uid, 1000)
                    for (f in follows) {
                        followingList[f.id] = f
                    }
                }
            }

            // 重连时候不加载数据库
            synchronized(fanList) {
                if (fanList.isEmpty())
                {
                    val fans = UserDbHelper.queryFansFromView(uid, 1000)
                    for (f in fans) {
                        fanList[f.id] = f
                    }
                }
            }

            // 重连时候不加载数据库
            synchronized(mutualFollowingList) {
                if (mutualFollowingList.isEmpty())
                {
                    val mutuals = UserDbHelper.queryMutualFromView(uid, 1000)
                    for (f in mutuals) {
                        mutualFollowingList[f.id] = f
                        if (SdkGlobalData.currentChatFid == 0L){
                            SdkGlobalData.currentChatFid = f.id
                        }
                    }
                }

            }



            // 向服务器申请重新更新好友列表
            MsgEncocder.sendListFriend("follows")
            MsgEncocder.sendListFriend("fans")


            // 申请同步数据消息
            try {
                // for Test
                //TopicDbHelper.clearPChatData(10006)
                //TopicDbHelper.clearPChatData()

                synchronized(chatTopicList){
                    ChatSessionManager.loadMessageOnLogin(chatTopicList)
                }
            }catch (e:Exception){
                Log.e("SdkGlobalData", e.toString())
            }


        }

        fun nextId():Long{
            if (BaseDb.getInstance() == null){
                return msgId.addAndGet(1)
            }
            else{
                return SeqDbHelper.getNextSeq()
            }
        }

        // TODO: 设备唯一编码，用于同一个账户的不同终端
        fun generateUniqueDeviceId(): String {
            // 获取设备的唯一信息
            val deviceInfo = StringBuilder()

            // 添加设备信息
            deviceInfo.append("Manufacturer: ${Build.MANUFACTURER}, ")
            deviceInfo.append("Model: ${Build.MODEL}, ")
            deviceInfo.append("Fingerprint: ${Build.FINGERPRINT}, ")
            val androidId = Settings.Secure.getString(context!!.contentResolver, Settings.Secure.ANDROID_ID)
            deviceInfo.append("AndroidId: $androidId, ")

            // 使用 CRC64 计算设备的唯一 ID
            val deviceInfoBytes = deviceInfo.toString().toByteArray()
            val crc64Value = CRC64.crc64(deviceInfoBytes)

            // 将 CRC64 值转为十六进制字符串
            return crc64ToHex(crc64Value)
        }

        fun crc64ToHex(crc64Value: Long): String {
            return String.format("%016X", crc64Value)
        }

        // OS Version: 10, SDK Version: 29, OS Code Name: Q, OS: google, Build ID: QKQ1.190828.002, Security Patch Level: 2020-10-05
        fun getOSInfo(): String {
            val osInfo = StringBuilder()

            // Android 版本
            osInfo.append("OSVersion: ${Build.VERSION.RELEASE}, ")

            // SDK 版本
            osInfo.append("SDKVersion: ${Build.VERSION.SDK_INT}, ")

            // 系统版本号
            osInfo.append("OSCodeName: ${Build.VERSION.CODENAME}, ")

            // 操作系统类型（比如 `android`）
            osInfo.append("OS: ${Build.BRAND}, ")

            // 系统构建版本
            osInfo.append("BuildID: ${Build.ID}, ")

            // 系统版本的安全补丁等级
            osInfo.append("SecurityPatchLevel: ${Build.VERSION.SECURITY_PATCH}")

            return osInfo.toString()
        }
    }
}