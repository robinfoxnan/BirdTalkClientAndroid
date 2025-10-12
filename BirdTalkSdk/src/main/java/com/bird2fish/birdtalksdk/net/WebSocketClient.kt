package com.bird2fish.birdtalksdk.net

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.Msg
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketClient private constructor() {
    private val TAG = "WebSocketClient"
    private var client: OkHttpClient? = null
    private var socket: WebSocket? = null
    private var listener: ClientWebSocketListener? = null

    private val isRunning = AtomicBoolean(false)

    private val handlerThread: HandlerThread = HandlerThread("ws_send_thread")
    private var handler: Handler

    private var serverPath: String = "wss://192.168.1.2:7817/ws?"
    private var fileServerPath: String = "https://192.168.1.2:7817"
    private var reconnectDelaySeconds = 10

    private var manuallyClosed = false

    init {
        handlerThread.start()
        handler = Handler(handlerThread.looper)
    }

    companion object {
        @get:Synchronized
        var instance: WebSocketClient? = null
            get() {
                if (field == null) {
                    field = WebSocketClient()
                }
                return field
            }
            private set
    }

    fun setContext(context: Context?) {
        MsgEncocder.setContext(context)
    }

    fun setDomain(domain: String) {
        val formattedUrl = "wss://$domain/ws?"

        if (serverPath != formattedUrl) {
            serverPath = formattedUrl
            attemptReconnect()
        }
    }

    fun setFileServerDomain(domain: String){
        fileServerPath = "https://$domain"
    }

    fun setReconnectDelay(seconds: Int) {
        reconnectDelaySeconds = if (seconds > 5) seconds else 5
    }

    fun isRunning() = isRunning.get()

    @Synchronized
    fun connect() {
        Log.d("WebSocketClient", "准备链接....")
        if (isRunning.get()) return

        shutdownSock()
        isRunning.set(true)

        if (client == null) client = UnsafeOkHttpClient.getUnsafeOkHttpClient()
        if (listener == null) listener = ClientWebSocketListener(this)

        val request: Request = Request.Builder().url(serverPath).build()

        this.manuallyClosed = false
        socket = client!!.newWebSocket(request, listener!!)

        startSendTask()
    }

//    @Synchronized
//    fun shutdownSock() {
//        isRunning.set(false)
//        socket?.close(1000, "Goodbye!")
//        socket = null
//    }

    @Synchronized
    fun shutdownSock() {
        manuallyClosed = true
        if (socket != null) {
            socket!!.close(1000, "Goodbye")
            socket = null
        }
    }

    fun wasManuallyClosed(): Boolean {
        return manuallyClosed
    }

    fun enqueueMessage(msg: Msg) {
        SendQueue.instance.enqueue(msg)
    }

    private fun startSendTask() {
        handler.post(object : Runnable {
            override fun run() {
                if (!isRunning.get()) return

                var msg = SendQueue.instance.dequeue()
                while (msg != null) {
                    sendMsg(msg.toByteArray())
                    msg = SendQueue.instance.dequeue()
                }

                // 50ms 后再执行下一次循环
                handler.postDelayed(this, 50)
            }
        })
    }

    // 这个版本改为阻塞一直等待有消息为止，防止CPU空轮询
    // 但是需要在断开时候配合sendingThread.interrupt()使用，暂时不用这个版本
//    private fun startSendTask() {
//        handler.post(object : Runnable {
//            override fun run() {
//                if (!isRunning.get()) return
//
//                try {
//                    // 阻塞直到队列有消息
//                    val msg = SendQueue.instance.take()
//                    sendMsg(msg.toByteArray())
//                } catch (e: InterruptedException) {
//                    Thread.currentThread().interrupt()
//                    return
//                }
//
//                // 消息发送完后，立即尝试下一条
//                handler.post(this)
//            }
//        })
//    }

    @Synchronized
    private fun sendMsg(data: ByteArray): Boolean {
        if (socket != null && isRunning.get()) {
            // 在 Kotlin 里，这个 * 是 展开运算符（spread operator），用于把一个数组“拆开”成可变参数（vararg）。
            val strData: ByteString = ByteString.of(*data)
            return try {
                socket!!.send(strData)
            } catch (e: IOException) {
                e.printStackTrace()
                scheduleReconnect()
                false
            }
        }
        return false
    }




    private fun scheduleReconnect() {
        isRunning.set(false)
        Log.d(TAG, "10秒后重连接...")
        handler.postDelayed({ connect() }, reconnectDelaySeconds * 1000L)
    }

    fun attemptReconnect() {
        Log.d(TAG, "10秒后重连接.")
        if (isRunning.get()) {
            shutdownSock()
        }
        Log.d(TAG, "10秒后重连接..")
        scheduleReconnect()
    }

    // 根据上传返回的文件名，按照约定，计算路径
    fun getRemoteFilePath(remote: String): String {
        val baseUrl = if (fileServerPath.startsWith("https")) fileServerPath else "https://$fileServerPath"
        return "$baseUrl/filestore/${remote.trimStart('/')}"
    }

    fun clean() {
        shutdownSock()
        client?.dispatcher?.executorService?.shutdown()
        client = null
        handlerThread.quitSafely()
    }




}
