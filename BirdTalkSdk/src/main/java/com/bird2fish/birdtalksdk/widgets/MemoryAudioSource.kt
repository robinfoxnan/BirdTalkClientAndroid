package com.bird2fish.birdtalksdk.widgets

import android.media.MediaDataSource
import java.io.IOException

// 将带内音频包装到 MediaDataSource 中，以使其可被 MediaPlayer 播放
class MemoryAudioSource(private val mData: ByteArray) : MediaDataSource() {

    @Throws(IOException::class)
    override fun readAt(position: Long, destination: ByteArray, offset: Int, size: Int): Int {
        val sizeToRead = minOf(mData.size - position.toInt(), size)
        System.arraycopy(mData, position.toInt(), destination, offset, sizeToRead)
        return sizeToRead
    }

    @Throws(IOException::class)
    override fun getSize(): Long {
        return mData.size.toLong()
    }

    @Throws(IOException::class)
    override fun close() {
        // 无需任何操作
    }
}
