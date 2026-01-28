package com.bird2fish.birdtalksdk

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.bird2fish.birdtalksdk.db.BaseDb
import com.bird2fish.birdtalksdk.db.GroupDbHelper
import com.bird2fish.birdtalksdk.db.SeqDbHelper
import com.bird2fish.birdtalksdk.db.TopicDbHelper
import com.bird2fish.birdtalksdk.model.TopicFlag
import com.bird2fish.birdtalksdk.db.UserDbHelper
import com.bird2fish.birdtalksdk.model.ChatSession
import com.bird2fish.birdtalksdk.model.ChatSessionManager
import com.bird2fish.birdtalksdk.model.Group
import com.bird2fish.birdtalksdk.model.GroupCache
import com.bird2fish.birdtalksdk.model.MessageContent
import com.bird2fish.birdtalksdk.model.Platform
import com.bird2fish.birdtalksdk.model.Topic
import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.model.UserCache
import com.bird2fish.birdtalksdk.net.CRC64
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.net.UnsafeOkHttpClient
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass
import com.bird2fish.birdtalksdk.uihelper.TextHelper
import com.bird2fish.birdtalksdk.uihelper.UserHelper
import com.squareup.picasso.OkHttp3Downloader
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/*
备注：
1) 会话列表中，用户的chatSessionId = fid, 群组的chatSessionId = -gid;
   这是为了管理起来简单化；
2) 数据库存储的topic.tid 都是正数；
*/

class SdkGlobalData {

    companion object{

        private const val TAG = "SdkGlobalData"
        // 一次从数据库加载几条
        const val LOAD_MSG_BATCH = 10
        const val LOAD_MSG_BATCH_SERVER = 100
        // 创建实例
        val basicInfo = Platform.getBasic()
        // 默认是使用扬声器播放的
        var useLoudSpeaker = true
        var context :Context? = null
        // 个人信息
        val selfUserinfo :com.bird2fish.birdtalksdk.model.User = User()
        private val msgId  = AtomicLong(1)

        var currentChatSession :ChatSession? = null

        // 这里是一个回调的列表
        var userCallBackManager = CallbackManager()

        fun  invokeOnEventCallbacks(eventType: MsgEventType, msgType: Int, msgId: Long, fid: Long, params:Map<String, String>){
            this.userCallBackManager.invokeOnEventCallbacks(eventType, msgType, msgId, fid, params)
        }


        // 发送消息时候，提供sendId的编号
        fun nextId():Long{
            if (BaseDb.getInstance() == null){
                return msgId.addAndGet(1)
            }
            else{
                return SeqDbHelper.getNextSeq()
            }
        }

        fun switchToChatSession(sid:Long):Boolean{
            if (sid == 0L){
                return false
            }
            currentChatSession = ChatSessionManager.getSession(sid)
            return true
        }

        ///////////////////////////////////////////////////////////////////////////////////////////
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
            basicInfo.deviceId = Platform.generateUniqueDeviceId()
            basicInfo.osInfo = Platform.getOSInfo()

            val client = UnsafeOkHttpClient.getUnsafeOkHttpClient()

            // 配置 Picasso 使用自定义 OkHttpClient
            val builder = Picasso.Builder(context)
            builder.downloader(OkHttp3Downloader(client))
            val picasso = builder.build()
            Picasso.setSingletonInstance(picasso)

        }

        fun debugClean(){
            try {
                //GroupDbHelper.resetGroupTable()

                //TopicDbHelper.dropPChatTopic(1000)

                //TopicDbHelper.deleteFromPTopic(10001)
                //TopicDbHelper.dropPChatTopic(10001)
                //TopicDbHelper.dropPChatTable()
                //TopicDbHelper.dropGChatTopic(10006)
                //TopicDbHelper.deleteFromGTopic(10006)

            }catch (e:Exception){
                Log.e("Sdk", e.toString())
            }

        }


        // 登录成功后操作
        // 尝试加载用户的好友和粉丝等各种预加载信息
        fun initLoad(uid:Long){
            try {
                //1) 切换数据到当前用户
                BaseDb.changeToDB(this.context, uid.toString())

                debugClean()

                if (BaseDb.getInstance() == null) {
                    Log.e(TAG, "BaseDb.changeToDB error")
                    TextHelper.showToast(context!!, "BaseDb.changeToDB error")
                }

                val ret = UserDbHelper.insertOrReplaceAccount(selfUserinfo)
                if (!ret) {
                    Log.e(TAG, "UserDbHelper.insertOrReplaceAccount FAIL")
                }

                // 2）加载粉丝，好友，和互粉信息,   先尽可能的多加载用户，后面会用到
                UserCache.initLoadFriends()

                // 3) 启动时候加载数据中存储的群组信息, 在前面
                GroupCache.initLoadGroups()

                // 4) 加载本地的会话列表，会话列表会引用前面的群信息和用户信息
                ChatSessionManager.tryLoadTopicsFromDb()

                // 向服务器申请重新更新好友列表
                MsgEncocder.sendListFriend("follows", 10000)
                MsgEncocder.sendListFriend("fans", 10000)
                // 如果关注的太多才使用服务器同步，因为数量少直接本地计算一下就好了
                //MsgEncocder.sendListFriend("friends", 10000)
                // 向服务器请求同步所在群的信息，最多返回100条
                MsgEncocder.sendListSelfInGroup()

                // 申请同步数据消息
                ChatSessionManager.loadMessageOnLogin()

            }catch (e:Exception){
                Log.e("SdkGlobalData", e.toString())
            }

        }


    }
}