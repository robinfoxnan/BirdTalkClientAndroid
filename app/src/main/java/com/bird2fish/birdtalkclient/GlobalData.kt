package com.bird2fish.birdtalkclient
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bird2fish.birdtalksdk.InterErrorType
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.MsgEventType.*
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.StatusCallback
import com.bird2fish.birdtalksdk.net.CRC64
import com.bird2fish.birdtalksdk.net.WebSocketClient
import com.bird2fish.birdtalksdk.net.WebSocketService


class GlobalData  {

    companion object {
        // 引用SDK中的
        val eventListener : ActionListener = ActionListener()
        var loginActivity: AppCompatActivity? = null
        var mainActivity :AppCompatActivity?=null
        var userStatus:String = ""

        fun init(ctx: Context, domain:String){
            // 设置与sdk的通知接口
            SdkGlobalData.addCallback(eventListener)
            SdkGlobalData.init(ctx)

            val appContext = ctx.applicationContext
            WebSocketClient.instance!!.setDomain(domain)
            WebSocketClient.instance!!.setFileServerDomain(domain)
            WebSocketClient.instance!!.setContext(appContext)

            // 启动后台服务（前台服务）
            val intent = Intent(appContext, WebSocketService::class.java)
            appContext.startService(intent)
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

    private fun doNothing(){

    }

    override fun onEvent(eventType: MsgEventType, msgType: Int, msgId: Long, fid: Long, params:Map<String, String>) {
        // 处理事件逻辑
        Log.d("GlobalData", "Event: $eventType, MsgType: $msgType, MsgId: $msgId, Fid: $fid")

        // 可以添加其他事件处理逻辑，如更新UI或发送通知
        when (eventType){
            // 跳转，加载用户信息，然后跳转到主界面
            LOGIN_OK ->{
                // 在后台组件中获取ViewModel实例并发送消息
                if (GlobalData.loginActivity != null){
                    val viewModel = ViewModelProvider(GlobalData.loginActivity!!).get(LoginViewMode::class.java)
                    viewModel.sendMessage("loginok")
                }
            }

            // 有好友申请，包括关注和
            FRIEND_REQUEST ->{
                if (GlobalData.mainActivity != null){
                    val  mainViewModel = ViewModelProvider(GlobalData.mainActivity!!).get(mainViewModel::class.java)
                }
            }
            FRIEND_REQ_REPLY ->doNothing()
            FRIEND_ONLINE ->doNothing()
            FRIEND_OFFLINE ->doNothing()
            FRIEND_UPDATE ->doNothing()
            MSG_COMING ->doNothing()
            MSG_SEND_ERROR ->doNothing()
            MSG_SEND_OK ->doNothing()
            MSG_RECV_OK ->doNothing()
            MSG_READ_OK ->doNothing()
            MSG_UPLOAD_OK -> doNothing()
            MSG_UPLOAD_FAIL -> doNothing()
            USR_UPDATEINFO_OK->doNothing()
            USR_UPDATEINFO_FAIL->doNothing()
            MSG_UPLOAD_PROCESS -> doNothing()
            SEARCH_FRIEND_RET -> doNothing()
            SEARCH_GROUP_RET -> doNothing()
            FRIEND_LIST_FOLLOW ->doNothing()
            FRIEND_LIST_FAN -> doNothing()
            LOGIN_CODE -> doNothing()
            APP_NOTIFY_SEND_MSG -> doNothing()
            APP_NOTIFY_REMOVE_SESSION -> doNothing()
            CONNECTING -> doNothing()
            RECONNECTING -> doNothing()
            CONNECTED -> doNothing()

            // end items
            MSG_DOWNLOAD_OK -> doNothing()
            MSG_DOWNLOAD_PROCESS -> doNothing()
            MSG_DOWNLOAD_FAIL -> doNothing()
            FRIEND_CHAT_SESSION -> doNothing()
        }
    }
}