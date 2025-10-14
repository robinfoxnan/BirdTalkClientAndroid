package com.bird2fish.birdtalksdk.net

import android.util.Log
import com.bird2fish.birdtalksdk.MsgEventType
import com.bird2fish.birdtalksdk.SdkGlobalData.Companion.invokeOnEventCallbacks
import com.bird2fish.birdtalksdk.net.Session.dispatchMsg
import com.bird2fish.birdtalksdk.net.Session.startHello
import com.bird2fish.birdtalksdk.net.Session.updateState
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class ClientWebSocketListener(client: WebSocketClient?) : WebSocketListener() {
    private var ws: WebSocketClient? = null

    init {
        this.ws = client
    }

    override fun onOpen(webSocket: WebSocket, response: Response) {
        //LogHelper.d("open websocket");
        Log.d("WebSocketClient", "连接成功")
        val resultMap = mapOf("reason" to "")
        invokeOnEventCallbacks(MsgEventType.CONNECTED, 0, 0, 0, resultMap)
        updateState(Session.SessionState.CONNECTED)
        startHello()
    }

    // 文本消息，主要是调试初期使用
    override fun onMessage(webSocket: WebSocket, text: String) {
        //LogHelper.d( "Received message: {0}", text);
    }

    // 二进制消息
    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        //LogHelper.d( "Received message: {0}", bytes.hex());

        dispatchMsg(bytes)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        //LogHelper.d( "WebSocket closing: {0} / {1}", new Object[]{code, reason});
        Log.i("WebSocket", "Closing: $code / $reason")

        // 判断关闭类型
        when (code) {
            1000 -> {
                // 正常关闭（可能用户手动退出）
                Log.d("WebSocketClient", "本地正常关闭，不重连")
                updateState(Session.SessionState.DISCONNECTED)
            }
            1001 -> {
                // 远端关闭或App中断
                Log.d("WebSocketClient", "远端主动关闭，准备重连")
                updateState(Session.SessionState.CONNECTING)
                invokeOnEventCallbacks(MsgEventType.RECONNECTING, 3, 0, 0, mapOf("reason" to "remote closed"))
                ws?.attemptReconnect()
            }
            else -> {
                // 其他非正常关闭，执行重连
                Log.d("WebSocketClient", "异常关闭 code=$code，执行重连")
                updateState(Session.SessionState.CONNECTING)
                invokeOnEventCallbacks(MsgEventType.RECONNECTING, 2, 0, 0, mapOf("reason" to "abnormal close"))
                ws?.attemptReconnect()
            }
        }
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        //LogHelper.d( "WebSocket error: ", t);

        Log.d("WebSocketClient", "$t")
        val resultMap = mapOf("reason" to "failure")
        when (t) {
            is java.net.SocketTimeoutException -> {
                Log.d("WebSocketClient", "链接超时")
                // 可以触发重连或提示用户
                invokeOnEventCallbacks(MsgEventType.RECONNECTING, 0, 0, 0, resultMap)
            }
            is java.net.ConnectException -> {
                Log.d("WebSocketClient", "无法链接服务器")
                invokeOnEventCallbacks(MsgEventType.RECONNECTING, 1, 0, 0, resultMap)
            }
            else -> {
                Log.d("WebSocketClient", "网络异常")
                invokeOnEventCallbacks(MsgEventType.RECONNECTING, 2, 0, 0, resultMap)
            }
        }

        updateState(Session.SessionState.CONNECTING)
        ws!!.attemptReconnect()
    }
}
