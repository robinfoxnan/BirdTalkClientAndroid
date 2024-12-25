package com.bird2fish.birdtalksdk.net;


import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass;
import com.bird2fish.birdtalksdk.pbmodel.User;

// 消息引擎给上层界面的事件接口
interface SessionEventListener  {
    void onError(int code, String detail, MsgOuterClass.ComMsgType msgType);
    void onLoginResult(String ret, String detail, User.UserInfo userInfo);
    void onRegisterResult(String ret, String detail, User.UserInfo userInfo);


    void onReceiveSyncChatData(MsgOuterClass.MsgChat chatData);
    void onReceiveSyncFriendData(MsgOuterClass.MsgChat chatData);
    void onReceiveSynFriendOp(User.UserOpResult opRet);
    void onReceiveSynGroupOp(User.GroupOpResult opRet);
    void onReceiveSynGroupMessage(MsgOuterClass.MsgChat chatData);


    void onReceiveChatMessage(MsgOuterClass.MsgChat chatData);
    void onReceiveChatReply(MsgOuterClass.MsgChatReply reply);

}