package com.bird2fish.birdtalksdk

import android.content.Context
import android.os.Build
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.model.Topic
import com.bird2fish.birdtalksdk.net.CRC64
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.net.UnsafeOkHttpClient
import com.bird2fish.birdtalksdk.uihelper.UserHelper
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import java.util.LinkedList
import java.util.TreeMap
import java.util.concurrent.atomic.AtomicLong


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




class SdkGlobalData {

    companion object{

        var context :Context? = null
        // 个人信息
        val selfUserinfo :com.bird2fish.birdtalksdk.model.User = User()
        private val msgId  = AtomicLong(1)


        // 这里是一个回调的列表
        var userCallBackManager = CallbackManager()

        // 互相关注
        var mutualFollowingList : MutableMap<Long, User> = LinkedHashMap()
        // 关注
        var followingList : MutableMap<Long, User> = LinkedHashMap()

        // 粉丝
        var fanList : MutableMap<Long, User> = LinkedHashMap()

        // 系统推荐
        var recommendedList : LinkedList<User> = LinkedList<User>()

        // 搜索用户返回的结果
        private var searchFriendList : LinkedList<User> = LinkedList<User>()

        // 当前会话列表
        var chatSessionList :LinkedList<Topic> = LinkedList<Topic>()

        // 当前显示的消息界面，如果是负数，就是群组
        var currentChatFid = 0L

        fun  invokeOnEventCallbacks(eventType: MsgEventType, msgType: Int, msgId: Long, fid: Long, params:Map<String, String>){
            this.userCallBackManager.invokeOnEventCallbacks(eventType, msgType, msgId, fid, params)
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

        fun setSearchFriendRet(ret:LinkedList<User>){
            // 使用同步代码块，锁定当前对象
            synchronized(this) {
                this.searchFriendList = ret
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

        fun updateAddNewFan(f:com.bird2fish.birdtalksdk.pbmodel.User.UserInfo){
            val friend = UserHelper.pbUserInfo2LocalUser(f)

            synchronized(fanList) {
                if (!fanList.containsKey(f.userId)){
                    fanList.set(f.userId, friend)
                }
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

        // 尝试加载用户的好友和粉丝等各种预加载信息
        fun initLoad(){
            MsgEncocder.sendListFriend("follows")
            MsgEncocder.sendListFriend("fans")
        }

        fun nextId():Long{

            return msgId.addAndGet(1)
        }

        // TODO: 设备唯一编码，用于同一个账户的不同终端
        fun generateUniqueDeviceId(): String {
            // 获取设备的唯一信息
            val deviceInfo = StringBuilder()

            // 添加设备信息
            deviceInfo.append("Manufacturer: ${Build.MANUFACTURER}, ")
            deviceInfo.append("Model: ${Build.MODEL}, ")
            deviceInfo.append("Fingerprint: ${Build.FINGERPRINT}, ")



            // 获取 Android ID
//            val androidId = Settings.Secure.getString(context!!.contentResolver, Settings.Secure.ANDROID_ID)

            // 操作系统版本
            deviceInfo.append("OS Version: ${Build.VERSION.RELEASE}, ")
            deviceInfo.append("SDK Version: ${Build.VERSION.SDK_INT}, ")

            // 使用 CRC64 计算设备的唯一 ID
            val deviceInfoBytes = deviceInfo.toString().toByteArray()
            val crc64Value = CRC64.crc64(deviceInfoBytes)

            // 将 CRC64 值转为十六进制字符串
            return crc64ToHex(crc64Value)
        }

        fun getDeviceFingerprint(): String {
            val deviceInfo = StringBuilder()

            // 提取设备的硬件特征码
            //deviceInfo.append("Brand: ${Build.BRAND}, ")
            //deviceInfo.append("Model: ${Build.MODEL}, ")
            deviceInfo.append("Manufacturer: ${Build.MANUFACTURER}, ")
            deviceInfo.append("Device: ${Build.DEVICE}, ")
            //deviceInfo.append("Product: ${Build.PRODUCT}, ")
            //deviceInfo.append("Board: ${Build.BOARD}, ")
            //deviceInfo.append("Version: ${VERSION.RELEASE}, ")
            //deviceInfo.append("SDK: ${VERSION.SDK_INT}, ")
            //deviceInfo.append("Fingerprint: ${Build.FINGERPRINT}")

            return deviceInfo.toString()
        }



        fun crc64ToHex(crc64Value: Long): String {
            return String.format("%016X", crc64Value)
        }

        // OS Version: 10, SDK Version: 29, OS Code Name: Q, OS: google, Build ID: QKQ1.190828.002, Security Patch Level: 2020-10-05
        fun getOSInfo(): String {
            val osInfo = StringBuilder()

            // Android 版本
            osInfo.append("OS Version: ${Build.VERSION.RELEASE}, ")

            // SDK 版本
            osInfo.append("SDK Version: ${Build.VERSION.SDK_INT}, ")

            // 系统版本号
            osInfo.append("OS Code Name: ${Build.VERSION.CODENAME}, ")

            // 操作系统类型（比如 `android`）
            osInfo.append("OS: ${Build.BRAND}, ")

            // 系统构建版本
            osInfo.append("Build ID: ${Build.ID}, ")

            // 系统版本的安全补丁等级
            osInfo.append("Security Patch Level: ${Build.VERSION.SECURITY_PATCH}")

            return osInfo.toString()
        }
    }
}