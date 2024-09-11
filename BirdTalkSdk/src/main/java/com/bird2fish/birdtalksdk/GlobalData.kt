package com.bird2fish.birdtalksdk

import com.bird2fish.birdtalksdk.model.User
import com.bird2fish.birdtalksdk.model.Topic
import java.util.LinkedList

class GlobalData {

    companion object{
        // 互相关注
        var followedList : LinkedList<User> = LinkedList<User>()
        // 关注
        var followingList : LinkedList<User> = LinkedList<User>()

        // 粉丝
        var fanList : LinkedList<User> = LinkedList<User>()

        // 系统推荐
        var recommendedList : LinkedList<User> = LinkedList<User>()

        // 当前会话列表
        var chatSessionList :LinkedList<Topic> = LinkedList<Topic>()
    }
}