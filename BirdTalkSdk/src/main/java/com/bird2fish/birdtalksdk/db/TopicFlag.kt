package com.bird2fish.birdtalksdk.db

object TopicFlag {
    /** 可见  */
    const val VISIBLE: Int = 1 shl 0 // 1  (0001)

    /** 静音  */
    const val MUTE: Int = 1 shl 1 // 2  (0010)

    /** 置顶  */
    const val PINNED: Int = 1 shl 2 // 4  (0100)

    /** 不可见（通常用 0 表示无任何标志）  */
    const val INVISIBLE: Int = 0
}
