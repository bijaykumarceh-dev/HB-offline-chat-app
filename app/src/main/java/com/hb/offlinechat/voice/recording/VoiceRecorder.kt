package com.hb.offlinechat.voice.recording

import android.media.MediaRecorder
import java.io.File

/**
 * Voice message recorder using the platform MediaRecorder (AAC in MPEG-4).
 * The caller is responsible for holding RECORD_AUDIO permission.
 */
class VoiceRecorder {

    private var recorder: MediaRecorder? = null
    private var startedAt = 0L
    private var currentPath: String? = null

    val isRecording: Boolean get() = recorder != null

    val elapsedMillis: Long
        get() = if (isRecording) System.currentTimeMillis() - startedAt else 0L

    fun start(outputFile: File): Result<Unit> = try {
        outputFile.parentFile?.mkdirs()
        val mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(AAC_BITRATE)
            setAudioSamplingRate(SAMPLE_RATE)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        currentPath = outputFile.absolutePath
        startedAt = System.currentTimeMillis()
        Result.success(Unit)
    } catch (e: Exception) {
        recorder?.release()
        recorder = null
        Result.failure(e)
    }

    /** Stop recording and return the recorded file path. */
    fun stop(): String? {
        val path = currentPath
        try {
            recorder?.stop()
        } catch (e: Exception) {
            // Ignore invalid-state stop; the file may still contain audio.
        } finally {
            recorder?.release()
            recorder = null
            currentPath = null
        }
        return path
    }

    /** Discard the current recording without keeping the file. */
    fun cancel() {
        stop()?.let { runCatching { File(it).delete() } }
    }

    companion object {
        private const val AAC_BITRATE = 64_000
        private const val SAMPLE_RATE = 44_100
    }
}
