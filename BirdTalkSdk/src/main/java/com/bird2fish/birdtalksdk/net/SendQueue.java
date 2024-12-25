package com.bird2fish.birdtalksdk.net;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import com.bird2fish.birdtalksdk.pbmodel.*;

// 单件模式
public class SendQueue {
    private static volatile SendQueue instance;
    // LinkedBlockingQueue内部已经通过锁机制来保证线程安全。
    private final BlockingQueue<MsgOuterClass.Msg> queue = new LinkedBlockingQueue<>();

    private static class SingletonHelper {
        private static final SendQueue INSTANCE = new SendQueue();
    }

    private SendQueue() {}

    public static SendQueue getInstance() {
        return SingletonHelper.INSTANCE;
    }

    // 添加到队列的方法
    public void enqueue(MsgOuterClass.Msg data) {
        try {
            queue.put(data);
        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    // 从队列中获取数据的方法
    public MsgOuterClass.Msg dequeue() throws InterruptedException {
        return queue.poll();
    }

    // 检查队列是否为空
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
