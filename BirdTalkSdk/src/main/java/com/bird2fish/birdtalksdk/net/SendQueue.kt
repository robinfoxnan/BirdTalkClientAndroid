package com.bird2fish.birdtalksdk.net

import com.bird2fish.birdtalksdk.pbmodel.MsgOuterClass.Msg
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class SendQueue private constructor() {
    // LinkedBlockingQueue 是 JDK 提供的线程安全阻塞队列
    private val queue: BlockingQueue<Msg> = LinkedBlockingQueue()

    // 添加到队列
    fun enqueue(data: Msg) {
        try {
            queue.put(data)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    // 非阻塞获取
    fun dequeue(): Msg? {
        return queue.poll()
    }

    fun take(): Msg {
        return queue.take()  // 阻塞直到有消息
    }

    val isEmpty: Boolean
        get() = queue.isEmpty()

    //by lazy 默认是 线程安全模式 (LazyThreadSafetyMode.SYNCHRONIZED)。
    //
    //第一次访问 SendQueue.instance 时会自动加锁初始化，之后直接返回单例引用
    companion object {
        val instance: SendQueue by lazy { SendQueue() }
    }
}

