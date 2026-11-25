package com.hb.offlinechat.presentation.viewmodels

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hb.offlinechat.di.AppContainer
import com.hb.offlinechat.domain.model.Device
import com.hb.offlinechat.voice.playback.VoicePlayer
import com.hb.offlinechat.voice.recording.VoiceRecorder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

/** Received voice note available for playback. */
data class PlayableVoice(val path: String, val name: String, val peerId: String, val size: Long)

/** ViewModel for real voice message recording, transfer and playback. */
class VoiceMessagesViewModel(private val app: Application) : AndroidViewModel(app) {
    private val container = AppContainer.getInstance(app)
    private val recorder = VoiceRecorder()
    private val player = VoicePlayer()
    private var timerJob: Job? = null
    private var recordingPath: String? = null

    private val _isRecording = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _elapsed = kotlinx.coroutines.flow.MutableStateFlow(0L)
    val elapsed: StateFlow<Long> = _elapsed

    private val _nowPlaying = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val nowPlaying: StateFlow<String?> = _nowPlaying

    private val _lastEvent = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val lastEvent: StateFlow<String?> = _lastEvent

    val connectedDevices: StateFlow<List<Device>> = combine(
        container.connectionManager.discoveredDevices,
        container.connectionManager.connectedEndpoints
    ) { devices, endpoints ->
        devices.filter { container.connectionManager.endpointIdForDevice(it.deviceId) in endpoints }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val receivedVoices: StateFlow<List<PlayableVoice>> = container.fileTransferManager.transfers
        .map { list ->
            list.filter { it.direction == "IN" && it.status == "COMPLETED" && it.fileName.endsWith(".m4a", true) }
                .mapNotNull { transfer ->
                    val path = transfer.localUri
                    if (path != null && File(path).exists()) {
                        PlayableVoice(path, transfer.fileName, transfer.peerId, transfer.fileSize)
                    } else null
                }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        player.onCompletion = { _nowPlaying.value = null }
    }

    fun startRecording(): Boolean {
        if (recorder.isRecording) return true
        val dir = File(app.cacheDir, "voice")
        val name = "voice_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT).format(java.util.Date())}.m4a"
        val result = recorder.start(File(dir, name))
        if (result.isSuccess) {
            _isRecording.value = true
            _elapsed.value = 0L
            timerJob = viewModelScope.launch {
                while (recorder.isRecording) {
                    _elapsed.value = recorder.elapsedMillis
                    delay(200)
                }
            }
        } else {
            _lastEvent.value = "Could not start recording: ${result.exceptionOrNull()?.message}"
        }
        return result.isSuccess
    }

    /** Stop recording and keep the file ready to send. */
    fun stopRecording() {
        if (!recorder.isRecording) return
        recordingPath = recorder.stop()
        _isRecording.value = false
        timerJob?.cancel()
        _elapsed.value = 0L
        _lastEvent.value = if (recordingPath != null) "Recording ready to send" else "Recording failed"
    }

    fun cancelRecording() {
        if (!recorder.isRecording) return
        recorder.cancel()
        recordingPath = null
        _isRecording.value = false
        timerJob?.cancel()
        _lastEvent.value = "Recording discarded"
    }

    /** Send the last recording to a connected peer through the verified file pipeline. */
    fun sendRecording(recipientDeviceId: String) {
        val path = recordingPath
        if (path == null) {
            _lastEvent.value = "Record a voice message first"
            return
        }
        val name = File(path).name
        viewModelScope.launch {
            container.fileTransferManager.sendFileFromPath(path, name, "audio/mp4", recipientDeviceId)
            recordingPath = null
        }
    }

    fun play(voice: PlayableVoice) {
        val result = player.play(voice.path)
        if (result.isSuccess) _nowPlaying.value = voice.path
        else Toast.makeText(app, "Cannot play: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
    }

    fun stopPlayback() {
        player.stop()
        _nowPlaying.value = null
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        recorder.cancel()
        player.stop()
    }
}
