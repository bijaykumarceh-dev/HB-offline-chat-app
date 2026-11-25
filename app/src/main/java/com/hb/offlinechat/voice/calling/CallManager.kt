package com.hb.offlinechat.voice.calling

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import com.hb.offlinechat.connectivity.connection.ConnectionManager
import com.hb.offlinechat.connectivity.connection.WireKind
import com.hb.offlinechat.service.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Base64
import java.util.UUID

enum class CallState { IDLE, OUTGOING, INCOMING, ACTIVE }

data class CallPeer(val callId: String, val deviceId: String, val name: String)

/**
 * Local peer-to-peer voice calling.
 *
 * PCM frames are captured with AudioRecord (VOICE_COMMUNICATION enables device
 * echo cancellation where available), sent as encrypted wire payloads, buffered
 * in a small jitter queue and played through AudioTrack. No cloud infrastructure.
 * Call quality depends on the local radio and device manufacturer.
 */
class CallManager(
    private val context: Context,
    private val connection: ConnectionManager,
    private val scope: CoroutineScope
) {
    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_BYTES = 640 // 20 ms at 16 kHz / 16-bit mono
        private const val RS = "\u001E"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val jitter = Channel<ByteArray>(capacity = 40)

    private val _state = MutableStateFlow(CallState.IDLE)
    val state: StateFlow<CallState> = _state.asStateFlow()

    private val _peer = MutableStateFlow<CallPeer?>(null)
    val peer: StateFlow<CallPeer?> = _peer.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _muted = MutableStateFlow(false)
    val muted: StateFlow<Boolean> = _muted.asStateFlow()

    private val _speaker = MutableStateFlow(false)
    val speaker: StateFlow<Boolean> = _speaker.asStateFlow()

    private val _lastEvent = MutableStateFlow<String?>(null)
    val lastEvent: StateFlow<String?> = _lastEvent.asStateFlow()

    private var activeCallId: String? = null
    private var captureJob: Job? = null
    private var playbackJob: Job? = null
    private var timerJob: Job? = null
    private var startedAt = 0L
    private var hadAudioFocus = false

    init {
        scope.launch(Dispatchers.IO) {
            connection.incomingWire.collect { wire ->
                when (wire.kind) {
                    WireKind.CALL_INVITE -> onInvite(wire.senderId, wire.payload)
                    WireKind.CALL_ACCEPT -> onAcceptSignal(wire.payload)
                    WireKind.CALL_REJECT -> {
                        if (wire.payload == activeCallId && _state.value == CallState.OUTGOING) {
                            _lastEvent.value = "Call rejected"
                            cleanup()
                        }
                    }
                    WireKind.CALL_END -> {
                        if (wire.payload == activeCallId) {
                            _lastEvent.value = "Call ended"
                            cleanup()
                        }
                    }
                    WireKind.CALL_AUDIO -> onAudio(wire.payload)
                    else -> Unit
                }
            }
        }
    }

    /** Place an outgoing call to a connected device. */
    fun startCall(deviceId: String): Boolean {
        if (_state.value != CallState.IDLE) return false
        val callId = UUID.randomUUID().toString()
        activeCallId = callId
        _peer.value = CallPeer(callId, deviceId, deviceId.take(8))
        _state.value = CallState.OUTGOING
        val sent = connection.sendWireToDevice(deviceId, WireKind.CALL_INVITE, callId)
        if (!sent) {
            _lastEvent.value = "Device is not connected"
            cleanup()
            return false
        }
        return true
    }

    /** Accept an incoming call and start bidirectional audio. */
    fun accept() {
        val call = _peer.value ?: return
        if (_state.value != CallState.INCOMING) return
        connection.sendWireToDevice(call.deviceId, WireKind.CALL_ACCEPT, call.callId)
        startAudio()
    }

    fun reject() {
        val call = _peer.value ?: return
        connection.sendWireToDevice(call.deviceId, WireKind.CALL_REJECT, call.callId)
        cleanup()
    }

    fun end() {
        val call = _peer.value
        if (call != null) connection.sendWireToDevice(call.deviceId, WireKind.CALL_END, call.callId)
        cleanup()
    }

    fun toggleMute() {
        _muted.value = !_muted.value
    }

    fun toggleSpeaker() {
        val next = !_speaker.value
        _speaker.value = next
        audioManager.isSpeakerphoneOn = next
    }

    private fun onInvite(senderId: String, payload: String) {
        if (_state.value != CallState.IDLE) {
            connection.sendWireToDevice(senderId, WireKind.CALL_REJECT, payload)
            return
        }
        activeCallId = payload
        _peer.value = CallPeer(payload, senderId, senderId.take(8))
        _state.value = CallState.INCOMING
        NotificationHelper.post(context, "Incoming voice call", "From ${senderId.take(8)}", 2001)
    }

    private fun onAcceptSignal(payload: String) {
        if (payload == activeCallId && _state.value == CallState.OUTGOING) startAudio()
    }

    private fun onAudio(payload: String) {
        if (_state.value != CallState.ACTIVE) return
        val parts = payload.split(RS, limit = 2)
        if (parts.size != 2 || parts[0] != activeCallId) return
        val bytes = try {
            Base64.getDecoder().decode(parts[1])
        } catch (e: Exception) {
            return
        }
        if (jitter.trySend(bytes).isFailure) {
            // Bounded jitter buffer: drop the oldest frame when the peer is slow.
            jitter.tryReceive()
            jitter.trySend(bytes)
        }
    }

    private fun startAudio() {
        if (_state.value == CallState.ACTIVE) return
        _state.value = CallState.ACTIVE
        startedAt = System.currentTimeMillis()

        val focusResult = audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
        hadAudioFocus = focusResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

        timerJob = scope.launch {
            while (_state.value == CallState.ACTIVE) {
                _duration.value = (System.currentTimeMillis() - startedAt) / 1000
                kotlinx.coroutines.delay(500)
            }
        }

        captureJob = scope.launch(Dispatchers.IO) { captureLoop() }
        playbackJob = scope.launch(Dispatchers.IO) { playbackLoop() }
    }

    private fun captureLoop() {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, ENCODING)
        if (minBuffer <= 0) {
            _lastEvent.value = "Audio input unavailable on this device"
            return
        }
        val recorder = try {
            AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(ENCODING)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(CHANNEL_CONFIG)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer, FRAME_BYTES * 2))
                .build()
        } catch (e: Exception) {
            _lastEvent.value = "Microphone unavailable: ${e.message}"
            return
        }
        try {
            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                _lastEvent.value = "Could not initialise microphone"
                return
            }
            recorder.startRecording()
            val frame = ByteArray(FRAME_BYTES)
            val callId = activeCallId ?: return
            val peerId = _peer.value?.deviceId ?: return
            while (_state.value == CallState.ACTIVE) {
                val read = recorder.read(frame, 0, frame.size)
                if (read <= 0) continue
                if (_muted.value) continue
                val payload = listOf(
                    callId,
                    Base64.getEncoder().encodeToString(frame.copyOf(read))
                ).joinToString(RS)
                connection.sendWireToDevice(peerId, WireKind.CALL_AUDIO, payload)
            }
        } finally {
            recorder.stop()
            recorder.release()
        }
    }

    private suspend fun playbackLoop() {
        val minBuffer = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            ENCODING
        )
        if (minBuffer <= 0) {
            _lastEvent.value = "Audio output unavailable on this device"
            return
        }
        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(ENCODING)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(maxOf(minBuffer, FRAME_BYTES * 4))
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            _lastEvent.value = "Speaker unavailable: ${e.message}"
            return
        }
        try {
            track.play()
            while (_state.value == CallState.ACTIVE) {
                val frame = jitter.receiveCatching().getOrNull() ?: break
                track.write(frame, 0, frame.size, AudioTrack.WRITE_BLOCKING)
            }
        } finally {
            track.stop()
            track.release()
        }
    }

    /** Release all audio resources and reset state. */
    fun cleanup() {
        captureJob?.cancel()
        playbackJob?.cancel()
        timerJob?.cancel()
        captureJob = null
        playbackJob = null
        timerJob = null
        while (jitter.tryReceive().getOrNull() != null) { /* drain jitter buffer */ }
        if (hadAudioFocus) {
            audioManager.abandonAudioFocus(null)
            hadAudioFocus = false
        }
        audioManager.isSpeakerphoneOn = false
        _speaker.value = false
        _muted.value = false
        activeCallId = null
        _peer.value = null
        _duration.value = 0L
        _state.value = CallState.IDLE
    }

    /** Cleans up when the app/screen is torn down. */
    fun shutdown() = cleanup()
}
