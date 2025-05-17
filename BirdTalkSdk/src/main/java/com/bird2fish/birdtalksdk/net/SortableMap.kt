package com.bird2fish.birdtalksdk.net

import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.Msg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.TimeUnit

class SortableMap {
    // 使用安全的跳表实现的类
    private val messageMap = ConcurrentSkipListMap<Long, Msg>()
    private var job: Job? = null
    // 将消息添加到
    fun addMessage(id: Long, message: Msg) {
        messageMap[id] = message
    }

    // Method to remove a message with a timestamp
    fun removeMessage(id: Long) {
        messageMap.remove(id)
    }

    // 执行消息检查和重发
    private suspend fun checkAndResendMessages() {
        val currentTimestamp = System.currentTimeMillis()
        synchronized(this) {
            for ((id, message) in messageMap) {
                // 执行消息超时检查
                Session.checkMsgTimeout(id, message)
            }
        }
    }

    // 启动周期性任务
    fun startChecking(period: Long) {
        job = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                delay(TimeUnit.SECONDS.toMillis(period))  // 等待指定时间
                checkAndResendMessages()  // 执行任务
            }
        }
    }

    // 停止任务
    fun stopChecking() {
        job?.cancel()  // 取消协程任务
    }
}


