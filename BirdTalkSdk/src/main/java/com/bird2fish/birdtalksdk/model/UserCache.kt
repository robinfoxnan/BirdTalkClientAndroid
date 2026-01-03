package com.bird2fish.birdtalksdk.model

import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.db.UserDbHelper
import com.bird2fish.birdtalksdk.net.MsgEncocder
import com.bird2fish.birdtalksdk.uihelper.UserHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

// 单例 UserCache
object UserCache {

    // 所有可能用到的用户，都在这里缓存，如果找不到就加载数据，如果没有就同步网络
    private val userMap = ConcurrentHashMap<Long, User>()

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

    /**
     * 根据 fid 获取 User（同步加载）
     * 优先缓存 -> 数据库 -> 网络
     */
    fun findUserSync(fid: Long): User {
        // 1️⃣ 优先查缓存
        userMap[fid]?.let { return it }

        // 保证线程安全
        synchronized(userMap) {
            // 再查一次，防止并发
            userMap[fid]?.let { return it }

            // 2️⃣ 查数据库
            val dbUser = UserDbHelper.getUserById(fid)
            if (dbUser != null) {
                userMap[fid] = dbUser
                return dbUser
            }

            // 3️⃣ 数据库没找到，同步调用网络接口

            val newUser = User().apply {
                id = fid
                name = "loading"
                nick = "loading"
                icon = ""
            }

            // 更新缓存和数据库
            userMap[fid] = newUser
            DataLoadHelper.findUser(fid)

            return newUser
        }
    }

    // 对方不再关注自己了
    private fun setNotFan(fid:Long){
        val f = findUserSync(fid)
        f.isFan = false
    }

    // 自己取消关注
    private fun setNotFollowHim(friend:User){
        val f = findUserSync(friend.id)
        f.isFollow = false
    }

    // 开始关注
    private fun setFollowHim(friend:User){
        val f = findUserSync(friend.id)
        f.isFollow = true
    }

    // 启动时候初始化时候
    private fun loadFollowingList(){
        // 重连时候不加载数据库
        synchronized(followingList) {
            if (followingList.isEmpty())
            {
                val follows = UserDbHelper.queryFollowsFromView(SdkGlobalData.selfUserinfo.id, 1000)
                for (f in follows) {
                    followingList[f.id] = f
                    if (!userMap.containsKey(f.id)){
                        userMap[f.id] = f
                    }
                }
            }
        }

    }

    private fun loadFanList(){
        // 重连时候不加载数据库
        synchronized(fanList) {
            if (fanList.isEmpty())
            {
                val fans = UserDbHelper.queryFansFromView(SdkGlobalData.selfUserinfo.id, 1000)
                for (f in fans) {
                    fanList[f.id] = f
                    if (!userMap.containsKey(f.id)){
                        userMap[f.id] = f
                    }
                }
            }
        }
    }

    private fun loadMutualList(){
        // 重连时候不加载数据库
        synchronized(mutualFollowingList) {
            if (mutualFollowingList.isEmpty())
            {
                val mutuals = UserDbHelper.queryMutualFromView(SdkGlobalData.selfUserinfo.id, 1000)
                for (f in mutuals) {
                    mutualFollowingList[f.id] = f
                    // 添加到全局库中
                    if (!userMap.containsKey(f.id)){
                        userMap[f.id] = f
                    }
                }
            }
        }
    }

    // 这里先加载互粉的，意思是直接插入到缓存，后面都是更新
    fun initLoadFriends(){
        loadMutualList()
        loadFollowingList()
        loadFanList()
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

    // 设置当前搜索的结果，有可能是界面搜索好友，也有可能是缺少信息而加载
    fun onReplySearchFriendRet(lst:LinkedList<User>, result:String, status:String){
        // 使用同步代码块，锁定当前对象
        synchronized(this) {
            this.searchFriendList = lst
        }

        // 同步缓存和数据库句
        updateUsers(lst)

        // 通知界面更新
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(
            MsgEventType.SEARCH_FRIEND_RET,
            0, 0, 0L, mapOf("result"  to result, "status" to status ) )

    }

    // 返回不可变视图（次优）
    fun getSearchFriendRet(): List<User> {
        synchronized(searchFriendList) {
            return searchFriendList.toList()
        }
    }

    fun onNoticeDeleteFollow(fid:Long){
        synchronized(followingList) {
            if (followingList.containsKey(fid)) {
                followingList.remove(fid)
            }

            // 更新自己关注数据库表
            UserDbHelper.deleteFromFollows(fid)
        }
        // 通知Session 不能会话了

    }


    // 从关注列表中点击了“取消关注”
    fun onSetDeleteFollow(fid:Long){

        synchronized(followingList) {
            if (followingList.containsKey(fid)) {
                followingList.remove(fid)
            }

            // 更新自己关注数据库表
            UserDbHelper.deleteFromFollows(fid)
            //setNotFollowHim(fid)
            // 通知会话列表
            //ChatSessionManager.updateDeleteFollow(friend)
        }


        synchronized(mutualFollowingList){
            mutualFollowingList.remove(fid)
        }

        // 提交服务
        MsgEncocder.sendRemoveFollow(fid)

        // 通知界面更新
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_LIST_FOLLOW,
            0, 0, 0L, mapOf("type" to "follow" ) )
    }

    // 服务器通知移除这个粉丝，应该对方删除了自己
    fun onNoticeDeleteFan(fid:Long){
        var b = false

        synchronized(fanList){
            if (fanList.containsKey(fid)){
                fanList.remove(fid)
                b = true
            }
        }

        if (b){
            synchronized(mutualFollowingList){
                mutualFollowingList.remove(fid)
            }
        }

        // 从粉丝表中删除对方
        UserDbHelper.deleteFromFans(fid)
        setNotFan(fid)
        ChatSessionManager.setNotFan(fid)
    }

    // 服务器通知对方不再关注自己了
    fun onSetDeleteFan(fid: Long){
        var b = false

        synchronized(fanList){
            if (fanList.containsKey(fid)){
                fanList.remove(fid)
                b = true
            }
        }

        if (b){
            synchronized(mutualFollowingList){
                mutualFollowingList.remove(fid)
            }
        }

        // 从粉丝表中删除对方
        UserDbHelper.deleteFromFans(fid)
        setNotFan(fid)
        ChatSessionManager.setNotFan(fid)

        // 通知界面更新
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_LIST_FAN,
            0, 0, 0L, mapOf("type" to "follow" ) )
    }

    // 服务器返回的双向关注
    fun onReplyMutual( lst: List<com.bird2fish.birdtalksdk.pbmodel.User.UserInfo>){
        synchronized(mutualFollowingList){
            for (f in lst){
                val friend = UserHelper.pbUserInfo2LocalUser(f)
                if (!mutualFollowingList.containsKey(f.userId)){

                    // 更新并保存
                    updateUser(friend)
                    mutualFollowingList[f.userId] = findUserSync(f.userId)
                }
            }
        }

    }

    // 初始化请求的枚举粉丝，
    fun onReplyListFollows(lst: List<com.bird2fish.birdtalksdk.pbmodel.User.UserInfo>){
        for (f in lst){
            onAddNewFollow(f)
        }

    }
    // 更新关注好友返回的信息
    fun onAddNewFollow(f:com.bird2fish.birdtalksdk.pbmodel.User.UserInfo){
        val friend = UserHelper.pbUserInfo2LocalUser(f)

        synchronized(followingList) {
            if (!followingList.containsKey(f.userId)) {
                followingList[f.userId] = friend
            }

            UserDbHelper.insertFollow(friend.id, friend.nick)

            // 更新与保存
            updateUser(friend)
        }

        var b  = false
        synchronized(fanList){
            if (fanList.containsKey(f.userId)){
                b = true
            }
        }
        if (b){
            synchronized(mutualFollowingList){
                // 要保证幂等，如果有了，就不要覆盖了
                if (!mutualFollowingList.containsKey(f.userId))
                    mutualFollowingList[f.userId] = friend
            }
        }
        // 通知界面更新
        SdkGlobalData.userCallBackManager.invokeOnEventCallbacks(MsgEventType.FRIEND_LIST_FOLLOW,
            0, 0, 0L, mapOf("type" to "follow" ) )
    }

    // 初始化列举自己的粉丝都时候，这里返回了
    fun onReplyListFans(lst: List<com.bird2fish.birdtalksdk.pbmodel.User.UserInfo>){
        for (f in lst){
            onAddNewFan(f)
        }

    }

    // 服务端返回的粉丝
    // 别人点击了关注自己，服务器会发一个回执过来这样可以通知我们添加粉丝，当然丢失了消息，在初始化时候还是可以知道的；
    fun onAddNewFan(f:com.bird2fish.birdtalksdk.pbmodel.User.UserInfo){
        val friend = UserHelper.pbUserInfo2LocalUser(f)

        synchronized(fanList) {
            if (!fanList.containsKey(f.userId)){
                fanList.set(f.userId, friend)
            }

            updateUser(friend)
            UserDbHelper.insertFan(friend.id, friend.nick)
        }

        var b  = false
        synchronized(followingList){
            if (followingList.containsKey(f.userId)){
                b = true
            }
        }
        if (b){
            synchronized(mutualFollowingList){
                if (!mutualFollowingList.containsKey(f.userId))
                    mutualFollowingList[f.userId] = friend
            }
        }

        // 通知界面更新
        SdkGlobalData.invokeOnEventCallbacks(MsgEventType.FRIEND_LIST_FAN,
            0, 0, 0L, mapOf("type" to "fan" ) )
    }

    // TODO:
    // 目前如果能从服务器返回粉丝列表，那么这里应该是用服务器的列表，保证已经删除了多余的；
    fun clearFans(){
        synchronized(fanList) {
            fanList.clear()
        }
        synchronized(mutualFollowingList){
            mutualFollowingList.clear()
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

    /**
     * 更新缓存和数据库
     */
    fun updateUser(user: User) {
        synchronized(userMap) {
            val u = userMap[user.id]
            if (u != null) {
                u.update(user)
            } else {
                userMap[user.id] = user
            }
        }
        GlobalScope.launch(Dispatchers.IO) {
            UserDbHelper.insertOrUpdateUser(user)
        }
    }

    // 尽量少重复，重复越多越容易出错
    fun updateUsers(lst :LinkedList<User>){
        for (user in lst){
            updateUser(user)
        }
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        userMap.clear()
    }
}
