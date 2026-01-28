package com.bird2fish.birdtalksdk.uihelper

import com.bird2fish.birdtalksdk.model.ChatSession
import com.bird2fish.birdtalksdk.model.Drafty

object SystemMsgHelper {

    fun getButtonDrafty(chatSession: ChatSession, msgId:Long):Drafty{
        val txt = "飞鸟(10001)请求加入群组[群组](100002)，是否同意？"
        val draft = Drafty(txt)

        var btnInsertPos = draft.txt.length
        var title1 = "拒绝"
        draft.insertButtonForGroup(btnInsertPos, "同意", "invite", "ok", chatSession.getSessionId(), msgId, 1001L, 999L)

        draft.insertButtonForGroup(btnInsertPos, "拒绝", "join_req", "refuse", chatSession.getSessionId(), msgId, 1001L, 999L)
        draft.insertLineBreak(btnInsertPos)

        return draft
    }

    fun getResultDrafty(msgId: Long, gid:Long, fromUid:Long, actionType:String, actValue:String):Drafty{
        val txt1 = "飞鸟(10001)请求加入群组[群组](100002)，是否同意？"
        var txt2 = "设置为：" + actValue
        val pos = txt1.length
        val draft = Drafty(txt1 + txt2)


        draft.insertLineBreak(pos)

        return draft
    }
}