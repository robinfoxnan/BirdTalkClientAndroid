package com.bird2fish.birdtalksdk.ui

import android.database.SQLException
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.db.BaseDb
import com.bird2fish.birdtalksdk.db.TopicDbHelper
import com.bird2fish.birdtalksdk.db.UserDbHelper
import com.bird2fish.birdtalksdk.model.MessageData
import com.bird2fish.birdtalksdk.model.Topic
import com.bird2fish.birdtalksdk.model.User


class FragmentTest : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_test, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 获取按钮视图
        val buttonExample: Button = view.findViewById(R.id.test_bnt1)

        // 设置点击监听器
        buttonExample.setOnClickListener {
            onClickTestBtn1()
        }
    }


    fun showInfo(str:String){
        // 执行操作，例如设置文本到 TextView
        val textView: TextView = requireView().findViewById(R.id.test_info_view)
        textView.text = textView.text.toString() + "\r\n" + str
    }

    // 必须在 Fragment 类中定义这个方法
    fun onClickTestBtn1() {
        // 执行操作，例如跳转到聊天页面
        //showInfo("Button clicked, text updated!")
        //testUserDb()
        testTopicDb()
    }

    fun addUser(){
        // 获取可写数据库
        for (i in 1..10) {
            val user = User()
            user.id = 1001 + i.toLong()
            user.name = "robin$i"
            user.nick = "测试$i"
            user.age = 30+ i
            user.gender = "男"
            user.region = "北京"
            user.icon = "path/to/icon"
            user.follows = 100+i
            user.fans = 150+i
            user.isFan = true
            user.isFollow = false
            user.introduction = "Hello, I'm John.$i"
            user.lastLoginTime = "2024-06-26 12:00:00"
            user.isOnline = true
            UserDbHelper.insertOrUpdateUser(user)
        }
    }

    fun testFindUser(){
        val lst =  UserDbHelper.findUserFrom(1010, 20)
        for (item in lst ){
            val str = item.toString()
            showInfo(str)
        }
    }

    fun testAddFuns(){
        UserDbHelper.insertFan(1001, "粉丝1")
        UserDbHelper.insertFan(1002, "粉丝2")
        UserDbHelper.insertFan(1003, "粉丝3")

        UserDbHelper.insertFan(1004, "粉丝4")
        UserDbHelper.deleteFromFans(1002)
        UserDbHelper.deleteFromFans(1006)

        UserDbHelper.insertFollow(1002, "关注2")
        UserDbHelper.insertFollow(1003, "关注2")
        UserDbHelper.deleteFromFollows(1003)
        UserDbHelper.deleteFromFollows(1004)
    }

    fun testFindFun(){
        val lst =  UserDbHelper.queryFansFromView(1000, 20)
        for (item in lst ){
            val str = item.toString()
            showInfo(str)
        }
    }

    fun testFindFollow(){
        val lst =  UserDbHelper.queryFollowsFromView(1000, 20)
        for (item in lst ){
            val str = item.toString()
            showInfo(str)
        }
    }

    fun testFindMutual(){
        val lst =  UserDbHelper.queryMutualFromView(1000, 20)
        for (item in lst ){
            val str = item.toString()
            showInfo(str)
        }
    }

    fun testAddAccount(){
        val user = User()
        user.id = 1234
        user.nick = "飞鸟"
        user.age = 30
        user.gender = "男"
        user.region = "北京"
        user.icon = "path/to/icon"
        user.follows = 100
        user.fans = 150
        user.isFan = true
        user.isFollow = false
        user.introduction = "Hello, I'm John."
        user.lastLoginTime = "2024-06-26 12:00:00"
        user.isOnline = true
        user.sharedPrint = 111
        UserDbHelper.insertOrReplaceAccount(user)

        var acc = UserDbHelper.getAccount(1234)
        showInfo(acc.toString())
    }

    fun testAddGroupMem(){

        try {
            UserDbHelper.insertOrReplaceGroupMember(201, 1001, 1, "群主")
            UserDbHelper.insertOrReplaceGroupMember(201, 1002, 2, "管理员")
            UserDbHelper.insertOrReplaceGroupMember(201, 1003, 3, "群众")

            UserDbHelper.insertOrReplaceGroupMember(301, 1001, 1, "群主")
            UserDbHelper.insertOrReplaceGroupMember(301, 1002, 2, "管理员")
            UserDbHelper.insertOrReplaceGroupMember(301, 1003, 3, "群众")

            UserDbHelper.deleteGroupMember(201, 1003)
            val lst = UserDbHelper.queryUsersFromGroupMemsView(201, 1002, 100)
            for (item in lst ){
                val str = item.toString()
                showInfo(str)
            }
        } catch (e: SQLException) {
            // 处理插入或替换过程中可能发生的异常
            e.printStackTrace()
            showInfo(e.toString())
            // 你可以在这里记录日志或通知用户出现了错误
        }


    }



    fun testUserDb(){
        // 获取数据库实例
        BaseDb.changeToDB(this.requireActivity(), "1234")
        //        addUser()
//        testAddFuns()
//       testFindFun()
//        testFindFollow()
//        BaseDb.changeToDB(this.requireActivity(), "12345")
//        testFindMutual()

//        testAddAccount()
        testAddGroupMem()

    }

    // 创建群组的会话信息以及
    fun testCreateTopic(){
        for (i in 1..10) {
            val topic = Topic(
                1000 + i.toLong(),  // tid
                1000,  // syncId
                2000,  // readId
                i % 5,  // type
                1,  // visible
                "测试 $i",  // title
                "Icon $i" // icon
            )
            TopicDbHelper.insertOrReplacePTopic(topic)
        }

        val lst = TopicDbHelper.getAllPTopics()
        for (item in lst){
            showInfo(item.toString())
        }

    }

    fun testCreateTopic1(){

            val topic = Topic(
                201,  // tid
                1000,  // syncId
                2000,  // readId
                1,  // type
                1,  // visible
                "群聊1",  // title
                "" // icon
            )

        TopicDbHelper.insertOrReplaceGTopic(topic)


        val lst = TopicDbHelper.getAllGTopics()
        for (item in lst){
            showInfo(item.toString())
        }

    }

    fun testInsertGChatData(){
        val name = Settings.Secure.getString(this.requireActivity().getContentResolver(), "bluetooth_name");
        for (i in 1..10){
            val msg = MessageData()
            msg.id = i.toLong();
            msg.sendId = msg.id
            msg.devId = name

            msg.tid = 201
            msg.uid = 1000 + i.toLong()
            msg.io = 1
            msg.msgType = "text"

            msg.data = "i love you".toByteArray()
            msg.tm = System.currentTimeMillis()
            msg.status = ""
            TopicDbHelper.insertGChatMsg(msg)
        }

        val count = TopicDbHelper.getGChatCount(201);
        showInfo("count: " + count)

        TopicDbHelper.updateGChatStatus(201, 10, "sending")

        val lst = TopicDbHelper.getGChatMessagesFromId(201, 9, 100, true)
        for (item in lst){
            showInfo(item.toString())
        }

    }

    fun testInsertP2PChatData(){
        val name = Settings.Secure.getString(this.requireActivity().getContentResolver(), "bluetooth_name");
        for (i in 11..20){
            val msg = MessageData()
            msg.id = i.toLong();
            msg.sendId = msg.id
            msg.devId = name

            msg.tid = 1001
            msg.uid = 1001
            msg.io = 0
            msg.msgType = "text"

            msg.data = "i love you".toByteArray()
            msg.tm = System.currentTimeMillis()
            msg.status = ""
            TopicDbHelper.insertPChatMsg(msg)
        }


        //TopicDbHelper.updateGChatStatus(201, 10, "sending")
        val msg = MessageData()
        msg.id = 6;
        msg.sendId = msg.id
        msg.devId = name

        msg.tid = 1001
        msg.uid = 1001
        msg.io = 1
        msg.msgType = "text"

        msg.data = "i love you".toByteArray()
        msg.tm = System.currentTimeMillis()
        msg.status = ""

        msg.tm1 =  System.currentTimeMillis()
        msg.tm2 = System.currentTimeMillis()

        TopicDbHelper.updatePChatReply(msg)

        msg.id = 5
        msg.status = "ok"
        TopicDbHelper.updatePChatStatus(msg)

//        val lst = TopicDbHelper.getMessagesFromTopic(1001, 6, 100, false)
//        for (item in lst){
//            showInfo(item.toString())
//        }

    }

    fun testUnread(){
        TopicDbHelper.deleteFromPChatUnread(2)
        val dataMap = TopicDbHelper.getUnreadCountsByUid()
        for (key in dataMap.keys) {
            val value: Int = dataMap.getValue(key)
            println("Key: $key, Value: $value")
            showInfo("fid=$key, count=$value")
        }
    }

    fun testUnreply(){
        val lst = TopicDbHelper.getMessagesUnReply()
        for (item in lst){
            showInfo(item.toString())
        }
    }

    fun testTopicDb(){

        BaseDb.changeToDB(this.requireActivity(), "1234")
        //TopicDbHelper.reCreateTable();

//        testCreateTopic()
        //testInsertGChatData()
        //testInsertP2PChatData()
        //testUnread()
        testInsertP2PChatData()
        testUnreply()
    }


}