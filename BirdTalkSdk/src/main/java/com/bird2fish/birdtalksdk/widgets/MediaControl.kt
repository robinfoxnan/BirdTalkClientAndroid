package com.bird2fish.birdtalksdk.widgets

import android.app.Activity
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import android.widget.Toast
import java.io.IOException
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.SdkGlobalData
import com.bird2fish.birdtalksdk.format.FullFormatter
import com.bird2fish.birdtalksdk.net.AudioDownloader
import com.bird2fish.birdtalksdk.uihelper.TextHelper

// Actions to take in setOnPreparedListener, when the player is ready.
enum class PlayerReadyAction {
    // Do nothing.
    NOOP,

    // Start playing.
    PLAY,

    // Seek without changing player state.
    SEEK,

    // Seek, then play when seek finishes.
    SEEKNPLAY
}

// 控制附件的音频播放
class MediaControl {
    private var mAudioManager: AudioManager? = null
    private var mAudioPlayer: MediaPlayer? = null
    private var mPlayingAudioSeq:Long = -1L
    private var mAudioControlCallback: FullFormatter.AudioControlCallback? = null
    private var mReadyAction = PlayerReadyAction.NOOP
    private var mSeekTo = -1f
    private var mActivity: Activity? = null

    fun setActivity(view:Activity){
        this.mActivity = view
    }

    @Synchronized
    @Throws(IOException::class)
    fun ensurePlayerReady(seq: Long, data: Map<String, Any>, control: FullFormatter.AudioControlCallback) {
        // 2025-11-28 修复，原本希望同一个一个消息不再重复的播放，但是重新加载后，序号不是固定的，这里应该改为消息号
        if (seq == 0L){
            // ChatPageAdapter  line728 中如果是0说明没有获取到ID，那么需要重新初始化，否则播放会错误
        }
        // 如果是需要播放的与上次播放的一样，那么不需要重新初始化了，否则这里是初始化
        else if (mAudioPlayer != null && mPlayingAudioSeq == seq) {
            mAudioControlCallback = control
            return
        }


        if (mPlayingAudioSeq > 0 && mAudioControlCallback != null) {
            mAudioControlCallback?.reset()
        }

        mPlayingAudioSeq = -1

        if (mAudioPlayer != null) {
            mAudioPlayer?.stop()
            mAudioPlayer?.reset()
        } else {
            mAudioPlayer = MediaPlayer()
        }

        if (mAudioManager == null) {
            mAudioManager = mActivity!!.getSystemService(Activity.AUDIO_SERVICE) as AudioManager
            mAudioManager?.mode = AudioManager.MODE_IN_CALL
            mAudioManager?.isSpeakerphoneOn = true
        }

        mAudioControlCallback = control
        mAudioPlayer?.apply {
            setOnPreparedListener { mp ->
                mPlayingAudioSeq = seq
                when (mReadyAction) {
                    PlayerReadyAction.PLAY -> {
                        mReadyAction = PlayerReadyAction.NOOP
                        mp.start()
                    }
                    PlayerReadyAction.SEEK, PlayerReadyAction.SEEKNPLAY -> seekTo(fractionToPos(mSeekTo))
                    else -> Unit
                }
                mSeekTo = -1f
            }
            setOnCompletionListener { mp ->
                if (mp.currentPosition > 0) {
                    mAudioControlCallback?.reset()
                }
            }
            setOnErrorListener { _, what, extra ->
                //Toast.makeText(mActivity, R.string.unable_to_play_audio, Toast.LENGTH_SHORT).show()
                Log.w("MediaControl", "Playback error $what/$extra")
                false
            }
            setOnSeekCompleteListener { mp ->
                if (mReadyAction == PlayerReadyAction.SEEKNPLAY) {
                    mReadyAction = PlayerReadyAction.NOOP
                    mp.start()
                }
            }
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)  // STREAM_VOICE_CALL
                    .build()
            )

            // 提取数据
            val valRef = data["ref"]
            val valData = data["val"]
            if (valRef == null){
                if (valData == null){
                    Log.w("MediaControl", "Unable to play audio: missing data")
                }
                else if (valData is String){
                    val source = Base64.decode(valData, Base64.DEFAULT)
                    setDataSource(MemoryAudioSource(source))
                    this.prepareAsync()
                }else if (valData is ByteArray){
                    setDataSource(MemoryAudioSource(valData))
                    this.prepareAsync()
                }
            }else{
                if (valRef is String){

                    //val url = tinode.toAbsoluteURL(valData)
                    val url = valRef
                    url?.let {

                        // 2025-12-18改动为下载后开始初始化
                        AudioDownloader.download(SdkGlobalData.context!!, url) { result ->
                            result.onSuccess { file ->
                                this.setDataSource(file.absolutePath)
                                this.prepareAsync()
                            }
                            result.onFailure { e->
                                Log.w("MediaControl", "Playback error $e")
                            }
                        }

//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            setDataSource(mActivity!!, Uri.parse(url.toString()), TextHelper.getRequestHeaders(), null)
//                        } else {
//                            val uri = Uri.parse(url.toString()).buildUpon()
//                                //.appendQueryParameter("apikey", tinode.apiKey)
//                                .appendQueryParameter("auth", "token")
//                                //.appendQueryParameter("secret", tinode.authToken)
//                                .build()
//                            setDataSource(mActivity!!, uri)
//                        }
                    }
                }
    }


            //prepareAsync()
        }
    }

    @Synchronized
    fun releasePlayer(seq: Long) {
        if ((seq != 0L && mPlayingAudioSeq != seq) || mPlayingAudioSeq == -1L) {
            return
        }

        mPlayingAudioSeq = -1
        mReadyAction = PlayerReadyAction.NOOP
        mSeekTo = -1f
        mAudioPlayer?.apply {
            stop()
            reset()
            release()
        }
        mAudioPlayer = null
        mAudioControlCallback?.reset()
    }

    fun playWhenReady() {
        if (mPlayingAudioSeq > 0) {
            mAudioPlayer?.start()
        } else {
            mReadyAction = PlayerReadyAction.PLAY
        }
    }

    @Synchronized
    fun pause() {
        mAudioPlayer?.takeIf { it.isPlaying }?.pause()
        mReadyAction = PlayerReadyAction.NOOP
        mSeekTo = -1f
    }

    @Synchronized
    fun seekToWhenReady(fraction: Float) {
        if (mPlayingAudioSeq > 0) {
            val pos = fractionToPos(fraction)
            if (mAudioPlayer?.currentPosition != pos) {
                mReadyAction = PlayerReadyAction.NOOP
                seekTo(pos)
            } else {
                mAudioPlayer?.start()
            }
        } else {
            mReadyAction = PlayerReadyAction.SEEK
            mSeekTo = fraction
        }
    }

    fun seekTo(pos: Int) {
        mAudioPlayer?.takeIf { mPlayingAudioSeq > 0 }?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.seekTo(pos as Long, MediaPlayer.SEEK_CLOSEST)
            } else {
                it.seekTo(pos)
            }
        }
    }

    private fun fractionToPos(fraction: Float): Int {
        return try {
            mAudioPlayer?.takeIf { mPlayingAudioSeq > 0 }?.let {
                val duration = it.duration
                if (duration > 0) (fraction * duration).toInt() else -1
            } ?: -1
        } catch (ex: IllegalStateException) {
            Log.w("MediaControl", "Not ready $mPlayingAudioSeq", ex)
            -1
        }
    }
}
