package com.bird2fish.birdtalksdk.widgets

import android.app.Activity
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.io.IOException
import com.bird2fish.birdtalksdk.R
import com.bird2fish.birdtalksdk.format.FullFormatter

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
    private var mPlayingAudioSeq = -1
    private var mAudioControlCallback: FullFormatter.AudioControlCallback? = null
    private var mReadyAction = PlayerReadyAction.NOOP
    private var mSeekTo = -1f
    private var mActivity: Activity? = null

    @Synchronized
    @Throws(IOException::class)
    fun ensurePlayerReady(seq: Int, data: Map<String, Any>, control: FullFormatter.AudioControlCallback) {
        if (mAudioPlayer != null && mPlayingAudioSeq == seq) {
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

            when (val valData = data["ref"] as? String ?: data["val"]) {
                is String -> {
//                    val tinode = Cache.getTinode()
//                    val url = tinode.toAbsoluteURL(valData)
//                    url?.let {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                            setDataSource(mActivity, Uri.parse(url.toString()), tinode.getRequestHeaders(), null)
//                        } else {
//                            val uri = Uri.parse(url.toString()).buildUpon()
//                                .appendQueryParameter("apikey", tinode.apiKey)
//                                .appendQueryParameter("auth", "token")
//                                .appendQueryParameter("secret", tinode.authToken)
//                                .build()
//                            setDataSource(mActivity, uri)
//                        }
//                    }
                }
                is String -> {
                    //val source = Base64.decode(valData, Base64.DEFAULT)
                    //setDataSource(MemoryAudioSource(source))
                }
                else -> Log.w("MediaControl", "Unable to play audio: missing data")
            }

            prepareAsync()
        }
    }

    @Synchronized
    fun releasePlayer(seq: Int) {
        if ((seq != 0 && mPlayingAudioSeq != seq) || mPlayingAudioSeq == -1) {
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
