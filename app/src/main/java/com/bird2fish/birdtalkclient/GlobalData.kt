package com.bird2fish.birdtalkclient
import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.net.CRC64
import com.bird2fish.birdtalksdk.net.WebSocketClient


class GlobalData  {

    companion object {
        // 引用SDK中的
        var client : WebSocketClient = WebSocketClient.instance!!
        val eventListener : ActionListener = ActionListener()
        var loginActivity: AppCompatActivity? = null
        var mainActivity :AppCompatActivity?=null


        fun init(ctx: Context){
            // 设置与sdk的通知接口
            SdkGlobalData.userCallBack = eventListener
            SdkGlobalData.init()
            client.setDomain("192.168.1.2:7817")
            client.setContext(ctx)
            client.connect()
        }

    }

}

// 负责处理SDK通知的各种事件
class ActionListener :StatusCallback{
    // 实现StatusCallback接口的方法
    override fun onError(code: InterErrorType, lastAction: String, errType: String, detail: String) {
        // 处理错误逻辑
        Log.e("GlobalData", "Error: $code, Action: $lastAction, Type: $errType, Detail: $detail")
        // 可以添加其他错误处理逻辑，如通知UI
    }

    override fun onEvent(eventType: MsgEventType, msgType: Int, msgId: Long, fid: Long) {
        // 处理事件逻辑
        Log.d("GlobalData", "Event: $eventType, MsgType: $msgType, MsgId: $msgId, Fid: $fid")
        // 可以添加其他事件处理逻辑，如更新UI或发送通知
        when (eventType){
            // 跳转，加载用户信息，然后跳转到主界面
            MsgEventType.LOGIN_OK->{
                // 在后台组件中获取ViewModel实例并发送消息
                if (GlobalData.loginActivity != null){
                    val viewModel = ViewModelProvider(GlobalData.loginActivity!!).get(LoginViewMode::class.java)
                    viewModel.sendMessage("loginok")
                }
            }

            // 有好友申请，包括关注和
            MsgEventType.FRIEND_REQUEST ->{

            }
            MsgEventType.FRIEND_REQ_REPLY ->{

            }
            MsgEventType.FRIEND_ONLINE->{

            }
            MsgEventType.FRIEND_OFFLINE->{

            }
            MsgEventType.FRIEND_UPDATE->{

            }


            MsgEventType.MSG_COMING->{

            }
            MsgEventType.MSG_DOWNLOAD_ERROR->{

            }

            MsgEventType.MSG_SEND_ERROR->{

            }
            MsgEventType.MSG_SEND_OK->{

            }
            MsgEventType.MSG_RECV_OK->{

            }
            MsgEventType.MSG_READ_OK->{

            }
            // end items
        }
    }
}