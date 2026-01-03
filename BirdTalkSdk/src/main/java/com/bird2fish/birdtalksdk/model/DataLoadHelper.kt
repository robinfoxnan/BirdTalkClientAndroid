package com.bird2fish.birdtalksdk.model

import com.bird2fish.birdtalksdk.net.MsgEncocder

object DataLoadHelper {

    // 查找好友
    fun findUser(fid:Long){
        MsgEncocder.sendFriendFindMessage("id", fid.toString())
    }

    // 尝试记载群信息
    fun findGroup(gid:Long){
        MsgEncocder.sendFindGroupMessage(gid.toString(), 0)
        MsgEncocder.sendListGroupMembers(gid)
    }

    fun findGroupMembers(groups:List<Group>){
        for (g in groups){
            MsgEncocder.sendListGroupMembers(g.gid)
        }

    }
}