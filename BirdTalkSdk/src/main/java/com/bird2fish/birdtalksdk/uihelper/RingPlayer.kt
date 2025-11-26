package com.bird2fish.birdtalksdk.uihelper

object RingPlayer {
    private var lastPlayTime: Long = 0

    // 播放系统默认通知铃声（带 5 秒节流 + 同步）
    fun playNotifyRing(context: android.content.Context) {
        synchronized(this) {
            val now = System.currentTimeMillis()
            if (now - lastPlayTime < 1000) {
                return  // 5 秒内不重复播放
            }
            lastPlayTime = now

            try {
                val uri = android.media.RingtoneManager.getDefaultUri(
                    android.media.RingtoneManager.TYPE_NOTIFICATION
                )
                val ringtone = android.media.RingtoneManager.getRingtone(
                    context.applicationContext, uri
                )
                ringtone.play()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}