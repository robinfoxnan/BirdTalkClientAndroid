package com.bird2fish.birdtalksdk.model

import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData.Companion.invokeOnEventCallbacks
import com.bird2fish.birdtalksdk.db.GroupDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap

// 单例 GroupCache
object GroupCache {

    // 缓存：gid -> Group
    private val groupMap = ConcurrentHashMap<Long, Group>()

    // 搜错群组返回的结果
    private var searchGroupList: LinkedList<Group> = LinkedList<Group>()

    // 这里的GROUP其实也来源于GroupCache的引用，
    private var userInGroupList = ConcurrentHashMap<Long, Group>()

    // 查找群组内的用户
    fun findGroupUser(gid:Long, uid:Long):User?{
        val g = groupMap[gid] ?: return null

        return  g.findUser(uid)
    }

    /**
     * 根据 gid 获取 Group（同步加载）
     * 优先缓存 -> 数据库 -> 网络
     */
    fun findGroupSync(gid: Long): Group {
        // 1️⃣ 优先查缓存
        groupMap[gid]?.let { return it }

        synchronized(groupMap) {
            // 再查一次，防止并发
            groupMap[gid]?.let { return it }

            // 2️⃣ 查数据库
            val dbGroup = GroupDbHelper.getGroupById(gid)
            if (dbGroup != null) {
                groupMap[gid] = dbGroup
                return dbGroup
            }

            // 3️⃣ 数据库没找到，创建占位 Group
            val newGroup = Group().apply {
                this.gid = gid
                this.name = "loading"
                this.brief = "loading"
                this.icon = ""
            }

            // 更新缓存和数据库
            groupMap[gid] = newGroup
            DataLoadHelper.findGroup(gid)

            return newGroup
        }
    }

    // 群组界面需要显示自己所在的群组列表，
    fun getInGroupList(): List<Group> {
        synchronized(userInGroupList) {
            return userInGroupList.values.toList()
        }
    }

    fun isInGroup(gid:Long):Boolean{
        synchronized(userInGroupList){
            return userInGroupList.containsKey(gid)
        }
    }


    // 搜索界面使用
    fun getSearchGroupRet(): List<Group> {
        synchronized(searchGroupList) {
            return searchGroupList.toList()
        }

    }

    // 从服务器返回结果s
    fun setSearchGroupRet(lst: List<Group>) {
        synchronized(searchGroupList) {
            searchGroupList.clear()
            searchGroupList.addAll(lst)
        }

        // 同步缓存与保存数据库
        updateGroups(lst, false)

        // 通知界面
        invokeOnEventCallbacks(MsgEventType.SEARCH_GROUP_RET, 0, 0, 0, mapOf(Pair("", "")))
    }
    // 列举自己所在的群信息
    fun onGroupListSelfInGroupRet(result:String, detail:String, sendId: Long, msgId: Long, groups:List<Group>){

        if (result != "ok"){
            return
        }

        // 保存到内存
        // 更新数据库
        updateGroups(groups, true)
        
        // 这里需要后期群的人员信息
        DataLoadHelper.findGroupMembers(groups)
    }

    fun initLoadGroups() {
        // 从数据库中加载群组信息
        if (groupMap.isEmpty()) {
            val gList = GroupDbHelper.findGroupsFrom(0, 1000);
            for (g in gList) {
                groupMap[g.gid] = g
            }
        }
    }

    /**
     * 更新缓存和数据库
     */
    fun updateGroup(group: Group):Group {
        var g :Group? = null
        synchronized(groupMap) {
            g = groupMap[group.gid]
            if (g != null) {
                g!!.update(group) // 需要你在 Group 类里实现 update 方法，更新除 gid 外的属性
            } else {
                groupMap[group.gid] = group
                g = group
            }

        }

        GlobalScope.launch(Dispatchers.IO) {
            GroupDbHelper.insertOrUpdateGroup(group)
        }

        return g!!
    }

    fun updateGroups(lst: List<Group>, isSelfIn:Boolean) {

        val retLst = LinkedList<Group>()
        for (g in lst) {
            val ret = updateGroup(g)
            retLst.add(ret)
        }
        // 同步到全局的组管理器中，后将同样的引用保存到这里，保证是同一个对象
        if (isSelfIn){
            synchronized(userInGroupList){
                userInGroupList.clear()
                for (g in retLst ){
                    userInGroupList[g.gid] = g
                }
            }
        }

    }

    /*
*  初始化时候加载用户所属的群组列表，会返回一组
 */
    fun addGroups(groupList: List<Group>) {
        for (g in groupList) {
            updateGroup(g)
        }
    }

    // 创建群组时候，这里会有一个添加动作
    fun addGroup(g: Group) {
        updateGroup(g)
    }

    /**
     * 清理缓存
     */
    fun clearCache() {
        groupMap.clear()
    }

    // 查询群组的用户返回的信息
    fun onRetGroupMembers(group:Group, users:List<User>){
        var g = groupMap[group.gid]
        if (g == null){
            synchronized(groupMap){
                g = group
                groupMap[group.gid] = group
                GroupDbHelper.insertOrUpdateGroup(group)
            }
        }
        for (u in users){
            if (u.role.contains('o')){
                g!!.addAdmin(u)
                g!!.ownerId = u.id
                g!!.owner = u
            }else if (u.role.contains('a')){
                g!!.addAdmin(u)
            }else{
                g!!.addMember(u)
            }
        }

    }

}
