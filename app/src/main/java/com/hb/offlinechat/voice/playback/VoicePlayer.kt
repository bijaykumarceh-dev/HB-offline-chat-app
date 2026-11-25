package com.hb.offlinechat.voice.playback

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.AudioManager

/** Simple one-at-a-time voice message player with proper resource cleanup. */
class VoicePlayer {

    private var player: MediaPlayer? = null
    var onCompletion: (() -> Unit)? = null

    val isPlaying: Boolean get() = player?.isPlaying == true

    fun play(path: String): Result<Unit> = try {
        stop()
        val mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                    .build()
            )
            setDataSource(path)
            prepare()
            setOnCompletionListener {
                onCompletion?.invoke()
                releaseResources()
            }
            start()
        }
        player = mediaPlayer
        Result.success(Unit)
    } catch (e: Exception) {
        releaseResources()
        Result.failure(e)
    }

    fun stop() = releaseResources()

    private fun releaseResources() {
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
    }
}
