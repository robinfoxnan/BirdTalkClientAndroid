package com.bird2fish.birdtalksdk.net

import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.Msg
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okio.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WebSocketClient  // 私有构造器，防止外部实例化
private constructor() {
    private var client: OkHttpClient? = null
    private var socket: WebSocket? = null

    private var listener: ClientWebSocketListener? = null
    private val isRunning = AtomicBoolean(false)

    private val sendQueue: SendQueue = SendQueue.getInstance()
    private val sendExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var serverPath: String = "wss://192.168.1.2:7817/ws?"

    private var RECONNECT_DELAY_SECONDS = 5 // 重连延迟秒数

    fun setReconnectDelay(t: Int){
        if (t > 5)
            this.RECONNECT_DELAY_SECONDS = t
    }

    fun setDomain(domain:String ){
        //val domain = "example.com"
        val formattedUrl = "wss://$domain/ws?"
        if (!serverPath.equals(formattedUrl)){
            this.serverPath = formattedUrl

            this.attemptReconnect()
        }
    }

    fun isRunning(): Boolean {
        return isRunning.get()
    }

    fun setNotRunning() {
        isRunning.set(false)
        Session.getInstance().currentState = Session.SessionState.DISCONNECTED
    }

    // 启动异步发送任务
    private fun startSendTask() {
        sendExecutor.submit {
            while (isRunning.get()) {
                try {
                    val msg = sendQueue.dequeue()
                    if(msg === null){
                        Thread.sleep(50)
                        continue;
                    }
                    sendMsg(msg.toByteArray())
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }


    // connect 方法修改为同步，确保线程安全
    @Synchronized
    fun connect() {
        // 如果已经在运行，直接返回
        if (isRunning.get()) {
            return
        }
        shutdown()
        Session.getInstance().currentState = Session.SessionState.CONNECTING

        // 初始化 OkHttpClient 和 WebSocket
        if (this.client == null) {
            client = UnsafeOkHttpClient.getUnsafeOkHttpClient()
        }

        val request: Request = Request.Builder().url(this.serverPath).build()

        if (this.listener == null) {
            this.listener = ClientWebSocketListener(this)
        }

        socket = client!!.newWebSocket(request, listener!!)
        // 启动发送任务线程
        startSendTask()

        isRunning.set(true)
    }

    // shutdown 方法修改为同步，确保线程安全
    @Synchronized
    fun shutdown() {
        setNotRunning()
        if (!(sendExecutor.isShutdown || sendExecutor.isTerminated)) {
            // 停止发送线程
            sendExecutor.shutdownNow()
            try {
                // 如果一定时间后（比如设置超时时间）线程池还未关闭，可以再调用sendExecutor.shutdownNow()来强制关闭。
                if (!sendExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    // 如果超时还未关闭，再强制关闭
                    sendExecutor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                // 重新尝试强制关闭
                sendExecutor.shutdownNow()
            }

            // 安全地关闭 WebSocket 和客户端
            if (socket != null) {
                socket!!.close(1000, "Goodbye!")
                socket = null
            }
        }
    }

    // 清理资源
    fun clean() {
        this.shutdown()

        if (client != null) {
            client!!.dispatcher.executorService.shutdown()
            client = null
        }
    }

    // 添加消息到发送队列的方法
    fun enqueueMessage(msg: Msg?) {
        try {
            sendQueue.enqueue(msg)
        } catch (e: RuntimeException) {
            // 可以在这里记录日志或者进行合适的异常处理逻辑，比如提示用户入队失败等
            e.printStackTrace()
        }
    }

    @Synchronized
    private fun sendMsg(data: ByteArray): Boolean {
        if (socket != null  && isRunning.get()) {
            val strData: okio.ByteString = okio.ByteString.of(*data)
            try {
                val ret = socket?.send(strData)
                if (ret is Boolean)
                    return ret

                return false
            } catch (e: IOException) {
                // 可以在这里记录日志，以便后续排查问题
                e.printStackTrace()
                attemptReconnect()
            }
        }
        return false
    }

    // 尝试重连
    fun attemptReconnect() {
        setNotRunning()
        //        LogHelper.d("Attempting to reconnect in seconds: ", this.RECONNECT_DELAY_SECONDS);
        try {
            TimeUnit.SECONDS.sleep(RECONNECT_DELAY_SECONDS.toLong())
        } catch (e: InterruptedException) {
//            LogHelper.d("Reconnect delay interrupted: ", this.RECONNECT_DELAY_SECONDS);
            Thread.currentThread().interrupt()
        }
        connect()
    }

    companion object {
        //private const val WEBSOCKET_URL = "wss://127.0.0.1/ws?"
        // 单例实例的引用
        @get:Synchronized
        var instance: WebSocketClient? = null
            // 获取单例实例的静态方法
            get() {
                if (field == null) {
                    synchronized(WebSocketClient::class.java) {
                        if (field == null) {
                            field = WebSocketClient()
                        }
                    }
                }
                return field
            }
            private set

    }
}