package com.bird2fish.birdtalkclient

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 全局初始化 SDK + WebSocket 服务
        GlobalData.init(this, "192.168.1.2:7817")
    }
}