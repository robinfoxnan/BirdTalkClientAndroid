package com.bird2fish.birdtalksdk.net

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.content.ContextCompat
import com.bird2fish.birdtalksdk.R


class WebSocketService : Service() {

    // 获取此类的引用
    inner class WebSocketServiceBinder : Binder() {
        fun getService(): WebSocketService? {
            return this@WebSocketService
        }
    }

    private val wsClient = WebSocketClient.instance!!
    private val binder = WebSocketServiceBinder()

    override fun onBind(intent: Intent?): IBinder? {
        Log.i("HttpService", "HttpService -> onBind, Thread: " + Thread.currentThread().name)
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.i("HttpService", "HttpService -> onUnbind, from:" + intent.getStringExtra("from"))
        return false
    }

///////////////////////////////////////////////////////////////////////////////////////////////////////
    override fun onCreate() {
    Log.i("HttpService", "HttpService -> onCreate, Thread: " + Thread.currentThread().name)
        super.onCreate()
        startForeground(1, createNotification())
        wsClient.connect()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(
            "HttpService",
            "HttpService -> onStartCommand, startId: " + startId + ", Thread: " + Thread.currentThread().name
        )
        return START_STICKY
    }

    override fun onDestroy() {
        Log.i("DemoLog", "TestService -> onDestroy, Thread: " + Thread.currentThread().name)
        super.onDestroy()
        wsClient.clean()
    }

    private fun createNotification(): Notification {
        val channelId = "ws_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "WebSocket Service", NotificationManager.IMPORTANCE_LOW)
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("鸣聊")
            .setContentText("后台服务工作中...")
            //.setSmallIcon(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) R.drawable.talkbird24 else R.drawable.talkbird_legacy) // 为旧版本准备的备用图标
            //.setColor(ContextCompat.getColor(this, R.color.notification_icon_color)) // 设置图标背景色（Android 5.0+）
            .build()
    }



}